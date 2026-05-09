package com.devdocsai.ingestion;

import com.devdocsai.ingestion.model.ApiSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiSpecRepository extends JpaRepository<ApiSpec, UUID> {
    List<ApiSpec> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<ApiSpec> findByIdAndTenantId(UUID id, UUID tenantId);
    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}
