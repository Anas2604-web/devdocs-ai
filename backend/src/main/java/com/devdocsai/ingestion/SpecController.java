package com.devdocsai.ingestion;

import com.devdocsai.common.ApiResponse;
import com.devdocsai.ingestion.model.ApiSpec;
import com.devdocsai.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/specs")
public class SpecController {

    private final IngestionService ingestionService;

    public SpecController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Upload a new API spec.
     * Returns 202 Accepted immediately — processing happens async.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<SpecResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) {

        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        String specName = (name != null && !name.isBlank())
            ? name : file.getOriginalFilename();

        ApiSpec spec = ingestionService.initiateUpload(file, specName, tenantId);
        return ResponseEntity.accepted().body(ApiResponse.ok(SpecResponse.from(spec)));
    }

    /**
     * List all specs for the current tenant.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SpecResponse>>> list() {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        List<SpecResponse> specs = ingestionService.listSpecs(tenantId)
                .stream().map(SpecResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(specs));
    }

    /**
     * Get a single spec — used for status polling.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SpecResponse>> get(@PathVariable UUID id) {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        ApiSpec spec = ingestionService.getSpec(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(SpecResponse.from(spec)));
    }

    /**
     * Delete a spec and all its data (S3 + Pinecone + DB).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        UUID tenantId = UUID.fromString(TenantContext.getCurrentTenant());
        ingestionService.deleteSpec(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ─── Response DTO ──────────────────────────────────────────────────────────
    public record SpecResponse(
        String id,
        String name,
        String originalFilename,
        String status,
        int chunkCount,
        String errorMessage,
        String createdAt,
        String updatedAt
    ) {
        public static SpecResponse from(ApiSpec spec) {
            return new SpecResponse(
                spec.getId().toString(),
                spec.getName(),
                spec.getOriginalFilename(),
                spec.getStatus().name(),
                spec.getChunkCount(),
                spec.getErrorMessage(),
                spec.getCreatedAt().toString(),
                spec.getUpdatedAt().toString()
            );
        }
    }
}
