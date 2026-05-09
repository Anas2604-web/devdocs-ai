package com.devdocsai.ingestion.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_specs")
public class ApiSpec {

    public enum Status { PENDING, PROCESSING, READY, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "chunk_count")
    private int chunkCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    // Constructors
    public ApiSpec() {}

    public ApiSpec(UUID tenantId, String name, String originalFilename, String s3Key) {
        this.tenantId = tenantId;
        this.name = name;
        this.originalFilename = originalFilename;
        this.s3Key = s3Key;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getOriginalFilename() { return originalFilename; }
    public String getS3Key() { return s3Key; }
    public Status getStatus() { return status; }
    public int getChunkCount() { return chunkCount; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setStatus(Status status) { this.status = status; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
