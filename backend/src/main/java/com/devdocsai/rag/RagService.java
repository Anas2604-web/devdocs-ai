package com.devdocsai.rag;

import com.devdocsai.common.DevDocsException;
import com.devdocsai.ingestion.GeminiEmbeddingService;
import com.devdocsai.ingestion.PineconeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final String GROQ_CHAT_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String COHERE_RERANK_URL  = "https://api.cohere.com/v2/rerank";

    // Minimum similarity score to consider a chunk relevant.
    // Below this = LLM would be guessing = hallucination risk.
    private static final float CONFIDENCE_THRESHOLD = 0.30f;

    // Get top 20 from Pinecone, rerank, keep best 5 for LLM.
    private static final int RETRIEVAL_TOP_K = 20;
    private static final int RERANK_TOP_N    = 5;

    @Value("${groq.api.key}")
    private String groqApiKey;

    // Optional — if not set, falls back to similarity sort
    @Value("${cohere.api.key:}")
    private String cohereApiKey;

    private static final String SYSTEM_PROMPT = """
        You are a helpful API documentation assistant.
        Answer developer questions based ONLY on the provided documentation context.
        Always include specific code examples when relevant.
        Format code blocks with the appropriate language tag (bash, python, javascript).
        If the context doesn't contain enough information, say: "I don't have enough documentation to answer this."
        Always cite which endpoint you're referencing using [Section N] notation.
        Be concise, accurate, and technical.
        Never invent endpoints, parameters, or behaviors not present in the context.
        """;

    private final GeminiEmbeddingService embeddingService;
    private final PineconeService pineconeService;
    private final ConversationHistoryService historyService;
    private final RagCacheService cacheService;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RagService(GeminiEmbeddingService embeddingService,
                      PineconeService pineconeService,
                      ConversationHistoryService historyService,
                      RagCacheService cacheService) {
        this.embeddingService = embeddingService;
        this.pineconeService  = pineconeService;
        this.historyService   = historyService;
        this.cacheService     = cacheService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * The full RAG pipeline — 8 steps:
     *
     * 1. Cache check       — return instantly if same question asked before
     * 2. Embed question    — convert question to vector using same model as ingestion
     * 3. Pinecone search   — find top 20 similar chunks by cosine similarity
     * 4. Confidence filter — drop chunks below threshold (prevents hallucination)
     * 5. Rerank            — Cohere cross-encoder picks best 5 by actual relevance
     * 6. Build prompt      — context + conversation history + question
     * 7. Call Groq LLM     — generate answer, send via SSE
     * 8. Cache + history   — save answer for future calls
     */
    public void streamAnswer(String question, String tenantId,
                             String sessionId, SseEmitter emitter) {
        try {

            // ── 1. Cache check ─────────────────────────────────────────────────
            var cached = cacheService.get(tenantId, question);
            if (cached.isPresent()) {
                log.debug("Cache HIT for tenant {}", tenantId);
                emitter.send(SseEmitter.event().name("token").data(cached.get()));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
                return;
            }

            // ── 2. Embed question ──────────────────────────────────────────────
            log.info("Embedding question for tenant {}", tenantId);
            List<Float> queryEmbedding = embeddingService.embed(question);

            // ── 3. Pinecone search — top 20 ───────────────────────────────────
            List<PineconeService.PineconeMatch> matches =
                    pineconeService.query(queryEmbedding, tenantId, RETRIEVAL_TOP_K);

            if (matches.isEmpty()) {
                sendMessage(emitter,
                        "No documentation found. Make sure your API spec has been uploaded and processed.");
                return;
            }

            // ── 4. Confidence filtering ────────────────────────────────────────
            // Cosine similarity 0 = completely different, 1 = identical.
            // If the best match is below 0.45, the question is probably outside
            // the scope of the uploaded docs — better to say "I don't know"
            // than to let the LLM guess and hallucinate.
            List<PineconeService.PineconeMatch> confident = matches.stream()
                    .filter(m -> m.score() >= CONFIDENCE_THRESHOLD)
                    .collect(Collectors.toList());

            if (confident.isEmpty()) {
                float bestScore = matches.get(0).score();
                log.info("All chunks below confidence threshold — best score: {}", bestScore);
                sendMessage(emitter,
                        "I couldn't find documentation relevant to this question " +
                                "(best match confidence: " + String.format("%.0f%%", bestScore * 100) + "). " +
                                "This question may be outside the scope of the uploaded API spec.");
                return;
            }

            log.info("Confidence filter: {}/{} chunks passed", confident.size(), matches.size());

            // ── 5. Rerank ──────────────────────────────────────────────────────
            // Reranking uses a cross-encoder model that reads query + document
            // TOGETHER and scores "does this document answer this question?"
            // Much more accurate than cosine similarity alone.
            List<PineconeService.PineconeMatch> topMatches;
            if (cohereApiKey != null && !cohereApiKey.isBlank()) {
                topMatches = rerankWithCohere(question, confident, RERANK_TOP_N);
                log.info("Reranked {} chunks → top {}", confident.size(), topMatches.size());
            } else {
                topMatches = confident.stream()
                        .sorted((a, b) -> Float.compare(b.score(), a.score()))
                        .limit(RERANK_TOP_N)
                        .collect(Collectors.toList());
                log.info("No Cohere key — using top {} by cosine similarity", RERANK_TOP_N);
            }

            emitter.send(SseEmitter.event().name("searching")
                    .data("Found " + topMatches.size() + " relevant sections"));

            // ── 6. Build prompt ────────────────────────────────────────────────
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < topMatches.size(); i++) {
                var m = topMatches.get(i);
                context.append("--- Documentation section ").append(i + 1).append(" ---\n");
                if (m.endpointMethod() != null && !m.endpointMethod().isBlank()) {
                    context.append("Endpoint: ").append(m.endpointMethod())
                            .append(" ").append(m.endpointPath()).append("\n");
                }
                context.append(m.chunkText()).append("\n\n");
            }

            List<ConversationHistoryService.Message> history = historyService.getHistory(sessionId);
            String prompt      = buildPrompt(question, context.toString(), history);
            String requestJson = buildGroqRequest(prompt);

            // ── 7. Call Groq LLM ───────────────────────────────────────────────
            log.info("Calling Groq for tenant {}", tenantId);
            StringBuilder fullAnswer = new StringBuilder();

            Request request = new Request.Builder()
                    .url(GROQ_CHAT_URL)
                    .post(RequestBody.create(requestJson, MediaType.get("application/json")))
                    .addHeader("Authorization", "Bearer " + groqApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("Groq error {}: {}", response.code(), body);
                    throw new DevDocsException("LLM_ERROR",
                            "AI error: " + response.code(), HttpStatus.SERVICE_UNAVAILABLE);
                }
                JsonNode root = objectMapper.readTree(body);
                String answer  = root.path("choices").get(0)
                        .path("message").path("content").asText("No answer generated.");
                fullAnswer.append(answer);
                emitter.send(SseEmitter.event().name("token").data(answer));
            }

            // ── 8. Cache + history ─────────────────────────────────────────────
            if (!fullAnswer.isEmpty()) {
                cacheService.put(tenantId, question, fullAnswer.toString());
                historyService.addMessage(sessionId, "user", question);
                historyService.addMessage(sessionId, "assistant", fullAnswer.toString());
            }

            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
            emitter.complete();

        } catch (Exception e) {
            log.error("RAG failed for tenant {}", tenantId, e);
            try {
                emitter.send(SseEmitter.event().name("error")
                        .data("Something went wrong. Please try again."));
                emitter.complete();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Cohere Rerank API.
     * Sends the question + all candidate chunks to Cohere's cross-encoder.
     * Cross-encoder reads query + document TOGETHER (unlike bi-encoders that
     * encode them separately). This gives much better relevance scores.
     * Falls back to cosine similarity sort if Cohere call fails.
     */
    private List<PineconeService.PineconeMatch> rerankWithCohere(
            String question,
            List<PineconeService.PineconeMatch> candidates,
            int topN) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", "rerank-v3.5");
            body.put("query", question);
            body.put("top_n", topN);
            body.put("return_documents", false);

            ArrayNode docs = body.putArray("documents");
            for (var match : candidates) docs.add(match.chunkText());

            Request request = new Request.Builder()
                    .url(COHERE_RERANK_URL)
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(body),
                            MediaType.get("application/json")))
                    .addHeader("Authorization", "Bearer " + cohereApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.warn("Cohere rerank failed {} — falling back: {}", response.code(), respBody);
                    return fallbackSort(candidates, topN);
                }
                JsonNode root    = objectMapper.readTree(respBody);
                JsonNode results = root.path("results");
                List<PineconeService.PineconeMatch> reranked = new ArrayList<>();
                for (JsonNode r : results) {
                    int idx = r.path("index").asInt();
                    reranked.add(candidates.get(idx));
                }
                return reranked;
            }
        } catch (Exception e) {
            log.warn("Cohere rerank exception — falling back: {}", e.getMessage());
            return fallbackSort(candidates, topN);
        }
    }

    private List<PineconeService.PineconeMatch> fallbackSort(
            List<PineconeService.PineconeMatch> candidates, int topN) {
        return candidates.stream()
                .sorted((a, b) -> Float.compare(b.score(), a.score()))
                .limit(topN)
                .collect(Collectors.toList());
    }

    private void sendMessage(SseEmitter emitter, String message) throws Exception {
        emitter.send(SseEmitter.event().name("token").data(message));
        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
        emitter.complete();
    }

    private String buildPrompt(String question, String context,
                               List<ConversationHistoryService.Message> history) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n\n");
        sb.append("DOCUMENTATION CONTEXT:\n").append(context).append("\n");
        if (!history.isEmpty()) {
            sb.append("CONVERSATION HISTORY:\n");
            history.forEach(m -> sb.append(m.role().toUpperCase())
                    .append(": ").append(m.content()).append("\n"));
            sb.append("\n");
        }
        sb.append("USER QUESTION: ").append(question);
        return sb.toString();
    }

    private String buildGroqRequest(String prompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", "llama-3.1-8b-instant");
        root.put("temperature", 0.3);
        root.put("max_tokens", 1024);
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "user").put("content", prompt);
        return objectMapper.writeValueAsString(root);
    }
}
