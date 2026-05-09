package com.devdocsai.ingestion;

import com.devdocsai.common.DevDocsException;
import com.devdocsai.ingestion.model.ApiChunk;
import com.devdocsai.ingestion.model.ApiSpec;
import com.devdocsai.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final ApiSpecRepository specRepository;
    private final ApiChunkRepository chunkRepository;
    private final S3Service s3Service;
    private final ChunkingService chunkingService;
    private final GeminiEmbeddingService embeddingService;
    private final PineconeService pineconeService;

    public IngestionService(ApiSpecRepository specRepository, ApiChunkRepository chunkRepository,
                            S3Service s3Service, ChunkingService chunkingService,
                            GeminiEmbeddingService embeddingService, PineconeService pineconeService) {
        this.specRepository   = specRepository;
        this.chunkRepository  = chunkRepository;
        this.s3Service        = s3Service;
        this.chunkingService  = chunkingService;
        this.embeddingService = embeddingService;
        this.pineconeService  = pineconeService;
    }

    /**
     * Step 1: Validate, save to S3, save metadata to DB, return immediately (202).
     * Step 2: Async processing happens in processIngestion().
     */
    @Transactional
    public ApiSpec initiateUpload(MultipartFile file, String specName, UUID tenantId) {
        // Validate file
        if (file.isEmpty()) {
            throw DevDocsException.badRequest("EMPTY_FILE", "File is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw DevDocsException.badRequest("FILE_TOO_LARGE", "File exceeds 5MB limit.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".yaml")
                && !originalFilename.endsWith(".yml") && !originalFilename.endsWith(".json"))) {
            throw DevDocsException.badRequest("INVALID_FILE_TYPE", "Only YAML or JSON files are supported.");
        }

        // Upload raw file to S3
        String s3Key = s3Service.uploadSpecFile(tenantId.toString(), file);

        // Save spec metadata with PENDING status
        ApiSpec spec = new ApiSpec(tenantId, specName, originalFilename, s3Key);
        spec = specRepository.save(spec);

        log.info("Spec {} saved for tenant {} — starting async ingestion", spec.getId(), tenantId);

        // Trigger async processing (runs in a separate thread)
        processIngestionAsync(spec.getId(), tenantId.toString());

        return spec;
    }

    /**
     * Async: parse → chunk → embed → store in Pinecone.
     * Runs in background thread pool so the HTTP response returns immediately.
     * We pass tenantId as a string because ThreadLocal doesn't carry across threads.
     */
    @Async
    public void processIngestionAsync(UUID specId, String tenantId) {
        // Must set TenantContext in new thread since ThreadLocal doesn't cross threads
        TenantContext.setCurrentTenant(tenantId);
        try {
            processIngestion(specId, UUID.fromString(tenantId));
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public void processIngestion(UUID specId, UUID tenantId) {
        ApiSpec spec = specRepository.findById(specId)
                .orElseThrow(() -> new RuntimeException("Spec not found: " + specId));

        try {
            // Update status → PROCESSING
            spec.setStatus(ApiSpec.Status.PROCESSING);
            specRepository.save(spec);

            // Read raw file from S3
            log.info("Reading spec {} from S3 key: {}", specId, spec.getS3Key());
            String specContent = s3Service.readFileAsString(spec.getS3Key());

            // Parse + chunk
            log.info("Parsing and chunking spec {}", specId);
            List<ApiChunk> chunks = chunkingService.parseAndChunk(specContent, specId, tenantId);

            if (chunks.isEmpty()) {
                throw new RuntimeException("No chunks produced from spec — check file content");
            }

            // Embed each chunk and upsert to Pinecone
            log.info("Embedding {} chunks for spec {}", chunks.size(), specId);
            int processed = 0;
            for (ApiChunk chunk : chunks) {
                List<Float> embedding = embeddingService.embed(chunk.getChunkText());

                String vectorId = specId + "_" + processed;
                pineconeService.upsert(
                    vectorId,
                    embedding,
                    chunk.getChunkText(),
                    chunk.getEndpointMethod(),
                    chunk.getEndpointPath(),
                    specId.toString(),
                    tenantId.toString()
                );

                chunk.setPineconeId(vectorId);
                chunkRepository.save(chunk);
                processed++;

                // Small delay to respect Gemini rate limits on free tier
                if (processed % 10 == 0) {
                    Thread.sleep(500);
                }
            }

            // Update status → READY
            spec.setStatus(ApiSpec.Status.READY);
            spec.setChunkCount(processed);
            specRepository.save(spec);

            log.info("Ingestion complete for spec {} — {} chunks embedded", specId, processed);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markFailed(spec, "Processing interrupted");
        } catch (Exception e) {
            log.error("Ingestion failed for spec {}", specId, e);
            markFailed(spec, e.getMessage());
        }
    }

    @Transactional
    public void deleteSpec(UUID specId, UUID tenantId) {
        ApiSpec spec = specRepository.findByIdAndTenantId(specId, tenantId)
                .orElseThrow(() -> DevDocsException.notFound("Spec not found."));

        // Delete from Pinecone
        pineconeService.deleteBySpecId(specId.toString(), tenantId.toString());

        // Delete chunks from DB
        chunkRepository.deleteAllBySpecId(specId);

        // Delete from S3
        s3Service.deleteFile(spec.getS3Key());

        // Delete spec record
        specRepository.delete(spec);

        log.info("Deleted spec {} for tenant {}", specId, tenantId);
    }

    public List<ApiSpec> listSpecs(UUID tenantId) {
        return specRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public ApiSpec getSpec(UUID specId, UUID tenantId) {
        return specRepository.findByIdAndTenantId(specId, tenantId)
                .orElseThrow(() -> DevDocsException.notFound("Spec not found."));
    }

    private void markFailed(ApiSpec spec, String reason) {
        spec.setStatus(ApiSpec.Status.FAILED);
        spec.setErrorMessage(reason);
        specRepository.save(spec);
    }
}
