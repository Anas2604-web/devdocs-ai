package com.devdocsai.ingestion.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_chunks")
public class ApiChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "spec_id", nullable = false)
    private UUID specId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "endpoint_method")
    private String endpointMethod;

    @Column(name = "endpoint_path")
    private String endpointPath;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "pinecone_id")
    private String pineconeId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public ApiChunk() {}

    public ApiChunk(UUID specId, UUID tenantId, String method, String path, String chunkText) {
        this.specId = specId;
        this.tenantId = tenantId;
        this.endpointMethod = method;
        this.endpointPath = path;
        this.chunkText = chunkText;
    }

    public UUID getId() { return id; }
    public UUID getSpecId() { return specId; }
    public UUID getTenantId() { return tenantId; }
    public String getEndpointMethod() { return endpointMethod; }
    public String getEndpointPath() { return endpointPath; }
    public String getChunkText() { return chunkText; }
    public String getPineconeId() { return pineconeId; }
    public void setPineconeId(String pineconeId) { this.pineconeId = pineconeId; }
}
