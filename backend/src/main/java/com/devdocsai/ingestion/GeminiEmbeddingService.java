package com.devdocsai.ingestion;

import com.devdocsai.common.DevDocsException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class GeminiEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingService.class);
    public static final int EMBEDDING_DIMENSIONS = 2048;
    private static final String PINECONE_EMBED_URL = "https://api.pinecone.io/embed";
    private static final String MODEL = "llama-text-embed-v2";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${pinecone.api.key}")
    private String pineconeApiKey;

    public GeminiEmbeddingService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<Float> embed(String text) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", MODEL);
            ObjectNode parameters = body.putObject("parameters");
            parameters.put("input_type", "passage");
            parameters.put("truncate", "END");
            ArrayNode inputs = body.putArray("inputs");
            inputs.addObject().put("text", truncate(text));

            String json = objectMapper.writeValueAsString(body);
            Request request = new Request.Builder()
                    .url(PINECONE_EMBED_URL)
                    .post(RequestBody.create(json, MediaType.get("application/json")))
                    .addHeader("Api-Key", pineconeApiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Pinecone-API-Version", "2024-10")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("Pinecone embed error {}: {}", response.code(), responseBody);
                    throw new DevDocsException("EMBEDDING_FAILED",
                            "Embedding error: " + response.code(), HttpStatus.SERVICE_UNAVAILABLE);
                }
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode values = root.path("data").get(0).path("values");
                List<Float> embedding = new ArrayList<>();
                for (JsonNode val : values) embedding.add(val.floatValue());
                log.debug("Embedding dimensions: {}", embedding.size());
                return embedding;
            }
        } catch (DevDocsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding failed", e);
            throw new DevDocsException("EMBEDDING_FAILED",
                    "Failed to generate embeddings.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String truncate(String text) {
        return text.length() > 7000 ? text.substring(0, 7000) : text;
    }
}