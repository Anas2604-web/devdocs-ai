package com.devdocsai.ingestion;

import com.devdocsai.ingestion.model.ApiChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiChunkRepository extends JpaRepository<ApiChunk, UUID> {
    List<ApiChunk> findAllBySpecId(UUID specId);
    List<ApiChunk> findAllByTenantId(UUID tenantId);
    void deleteAllBySpecId(UUID specId);
}
