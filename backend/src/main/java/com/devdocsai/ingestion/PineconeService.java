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
public class PineconeService {

    private static final Logger log = LoggerFactory.getLogger(PineconeService.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${pinecone.api.key}")
    private String pineconeApiKey;

    @Value("${pinecone.index.host}")
    private String indexHost; // e.g. https://devdocsai-abc123.svc.aped-4627-b74a.pinecone.io

    public PineconeService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Upserts a single vector into Pinecone.
     * namespace = tenantId — this is the multi-tenant isolation in the vector DB.
     */
    public void upsert(String vectorId, List<Float> embedding,
                       String chunkText, String method, String path,
                       String specId, String tenantId) {
        try {
            ObjectNode vector = objectMapper.createObjectNode();
            vector.put("id", vectorId);

            ArrayNode values = vector.putArray("values");
            for (float v : embedding) values.add(v);

            // Metadata — stored alongside vector for retrieval
            ObjectNode metadata = vector.putObject("metadata");
            metadata.put("chunk_text", chunkText.length() > 1000 ? chunkText.substring(0, 1000) : chunkText);
            metadata.put("endpoint_method", method != null ? method : "");
            metadata.put("endpoint_path",   path   != null ? path   : "");
            metadata.put("spec_id",  specId);
            metadata.put("tenant_id", tenantId);

            ObjectNode body = objectMapper.createObjectNode();
            body.putArray("vectors").add(vector);
            body.put("namespace", tenantId); // KEY: isolate by tenant

            String json = objectMapper.writeValueAsString(body);
            Request request = new Request.Builder()
                    .url(indexHost + "/vectors/upsert")
                    .post(RequestBody.create(json, MediaType.get("application/json")))
                    .addHeader("Api-Key", pineconeApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String respBody = response.body() != null ? response.body().string() : "";
                    log.error("Pinecone upsert failed {}: {}", response.code(), respBody);
                    throw new DevDocsException("PINECONE_UPSERT_FAILED",
                        "Failed to store embedding.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

        } catch (DevDocsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Pinecone upsert error for vectorId={}", vectorId, e);
            throw new DevDocsException("PINECONE_UPSERT_FAILED",
                "Failed to store embedding.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Queries Pinecone for the top-K most similar chunks.
     * Always scoped to the tenant's namespace — cross-tenant access is structurally impossible.
     */
    public List<PineconeMatch> query(List<Float> queryEmbedding, String tenantId, int topK) {
        try {
            ObjectNode body = objectMapper.createObjectNode();

            ArrayNode vector = body.putArray("vector");
            for (float v : queryEmbedding) vector.add(v);

            body.put("topK", topK);
            body.put("namespace", tenantId); // always scoped to tenant
            body.put("includeMetadata", true);

            String json = objectMapper.writeValueAsString(body);
            Request request = new Request.Builder()
                    .url(indexHost + "/query")
                    .post(RequestBody.create(json, MediaType.get("application/json")))
                    .addHeader("Api-Key", pineconeApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("Pinecone query failed: {}", respBody);
                    return List.of();
                }

                JsonNode root = objectMapper.readTree(respBody);
                JsonNode matches = root.path("matches");
                List<PineconeMatch> results = new ArrayList<>();

                for (JsonNode match : matches) {
                    String chunkText = match.path("metadata").path("chunk_text").asText("");
                    String method    = match.path("metadata").path("endpoint_method").asText("");
                    String path      = match.path("metadata").path("endpoint_path").asText("");
                    float score      = (float) match.path("score").asDouble();
                    results.add(new PineconeMatch(chunkText, method, path, score));
                }

                return results;
            }

        } catch (Exception e) {
            log.error("Pinecone query error", e);
            return List.of();
        }
    }

    /**
     * Deletes all vectors for a spec (when spec is deleted).
     */
    public void deleteBySpecId(String specId, String tenantId) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode filter = body.putObject("filter");
            filter.put("spec_id", specId);
            body.put("namespace", tenantId);
            body.put("deleteAll", false);

            String json = objectMapper.writeValueAsString(body);
            Request request = new Request.Builder()
                    .url(indexHost + "/vectors/delete")
                    .post(RequestBody.create(json, MediaType.get("application/json")))
                    .addHeader("Api-Key", pineconeApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            httpClient.newCall(request).execute().close();
            log.info("Deleted Pinecone vectors for specId={}", specId);
        } catch (Exception e) {
            log.warn("Failed to delete Pinecone vectors for specId={}: {}", specId, e.getMessage());
        }
    }

    // Simple result record
    public record PineconeMatch(String chunkText, String endpointMethod, String endpointPath, float score) {}
}
