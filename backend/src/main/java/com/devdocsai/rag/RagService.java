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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final String GROQ_CHAT_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String SYSTEM_PROMPT = """
        You are a helpful API documentation assistant.
        You answer developer questions about the API based ONLY on the provided documentation context.
        Always be specific and include code examples when relevant.
        Format code blocks with the appropriate language tag.
        If the context doesn't contain enough information to answer the question, say so clearly.
        Always cite which endpoint you're referencing.
        Be concise and technical.
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
        this.pineconeService = pineconeService;
        this.historyService = historyService;
        this.cacheService = cacheService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Main RAG flow:
     * 1. Check Redis cache
     * 2. Embed the question
     * 3. Search Pinecone for relevant chunks (top-20)
     * 4. Rerank to get top-5
     * 5. Build prompt with context + history
     * 6. Stream response via SSE
     */
    public void streamAnswer(String question, String tenantId, String sessionId,
                             SseEmitter emitter) {
        try {
            // 1. Check cache
            var cached = cacheService.get(tenantId, question);
            if (cached.isPresent()) {
                emitter.send(SseEmitter.event().name("token").data(cached.get()));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
                return;
            }

            // 2. Embed question
            log.info("Embedding question for tenant {}", tenantId);
            List<Float> queryEmbedding = embeddingService.embed(question);

            // 3. Search Pinecone — get top 20
            List<PineconeService.PineconeMatch> matches =
                pineconeService.query(queryEmbedding, tenantId, 20);

            if (matches.isEmpty()) {
                String noContext = "I couldn't find any relevant documentation to answer your question. " +
                    "Please make sure you've uploaded your API spec and it's been processed successfully.";
                emitter.send(SseEmitter.event().name("token").data(noContext));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
                return;
            }

            // 4. Rerank — take top 5 by score
            List<PineconeService.PineconeMatch> topMatches = matches.stream()
                .sorted((a, b) -> Float.compare(b.score(), a.score()))
                .limit(5)
                .toList();

            // Signal to frontend that we found relevant docs
            emitter.send(SseEmitter.event().name("searching").data(
                "Found " + topMatches.size() + " relevant sections"));

            // 5. Build context from top chunks
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < topMatches.size(); i++) {
                var match = topMatches.get(i);
                context.append("--- Documentation section ").append(i + 1).append(" ---\n");
                if (match.endpointMethod() != null && !match.endpointMethod().isEmpty()) {
                    context.append("Endpoint: ").append(match.endpointMethod())
                           .append(" ").append(match.endpointPath()).append("\n");
                }
                context.append(match.chunkText()).append("\n\n");
            }

            // 6. Get conversation history
            List<ConversationHistoryService.Message> history = historyService.getHistory(sessionId);

            // 7. Build request
            String prompt = buildPrompt(question, context.toString(), history);
            String requestJson = buildGroqRequest(prompt);

            // 8. Stream from Gemini
            log.info("Streaming Gemini response for tenant {}", tenantId);
            StringBuilder fullAnswer = new StringBuilder();



            Request request = new Request.Builder()
                    .url(GROQ_CHAT_URL)
                    .post(RequestBody.create(requestJson, MediaType.get("application/json")))
                    .addHeader("Authorization", "Bearer " + groqApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("Groq error {}: {}", response.code(), responseBody);
                    throw new DevDocsException("LLM_ERROR",
                            "AI error: " + response.code(), HttpStatus.SERVICE_UNAVAILABLE);
                }
                JsonNode root = objectMapper.readTree(responseBody);
                String answer = root.path("choices").get(0)
                        .path("message").path("content").asText("No answer generated");

                fullAnswer.append(answer);
                emitter.send(SseEmitter.event().name("token").data(answer));
            }

            // 9. Cache the full answer
            if (!fullAnswer.isEmpty()) {
                cacheService.put(tenantId, question, fullAnswer.toString());
                historyService.addMessage(sessionId, "user", question);
                historyService.addMessage(sessionId, "assistant", fullAnswer.toString());
            }

            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
            emitter.complete();

        } catch (Exception e) {
            log.error("RAG stream failed for tenant {}", tenantId, e);
            try {
                emitter.send(SseEmitter.event().name("error")
                    .data("Something went wrong. Please try again."));
                emitter.complete();
            } catch (Exception ignored) {}
        }
    }

    private String buildPrompt(String question, String context,
                                List<ConversationHistoryService.Message> history) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT).append("\n\n");
        prompt.append("DOCUMENTATION CONTEXT:\n").append(context).append("\n");

        if (!history.isEmpty()) {
            prompt.append("CONVERSATION HISTORY:\n");
            for (var msg : history) {
                prompt.append(msg.role().toUpperCase()).append(": ").append(msg.content()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("USER QUESTION: ").append(question);
        return prompt.toString();
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
