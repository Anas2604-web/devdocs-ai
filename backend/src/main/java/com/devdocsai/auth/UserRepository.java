package com.devdocsai.auth;

import com.devdocsai.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Always scope queries to tenantId — never query across tenants
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    // Used only during login (before tenantId is known — find by email first, then verify tenant)
    Optional<User> findByEmail(String email);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);
}
