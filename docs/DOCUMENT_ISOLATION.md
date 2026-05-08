# Document isolation — multi-tenant data security

This document explains exactly how DevDocs AI ensures one tenant can NEVER access another tenant's data. This is the most critical security property of the system.

---

## The four isolation layers

### Layer 1 — JWT contains tenantId

Every authenticated request carries a JWT with the tenant's ID embedded:

```json
{
  "sub": "user-uuid-here",
  "tenantId": "acmepay-uuid-here",
  "role": "ADMIN",
  "iat": 1700000000,
  "exp": 1700000900
}
```

The `JwtAuthFilter` extracts this on every request before any controller runs.

### Layer 2 — ThreadLocal context (never pass tenantId as a parameter)

```java
// TenantContext.java
public class TenantContext {
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String getCurrentTenant() {
        return TENANT.get();
    }

    public static void clear() {
        TENANT.remove(); // CRITICAL — always clear after request
    }
}

// JwtAuthFilter.java — sets context on every request
@Override
protected void doFilterInternal(HttpServletRequest request, ...) {
    try {
        String token = extractToken(request);
        if (token != null && jwtService.validateToken(token)) {
            String tenantId = jwtService.extractTenantId(token);
            TenantContext.setCurrentTenant(tenantId); // set here
            // ... set SecurityContext
        }
        filterChain.doFilter(request, response);
    } finally {
        TenantContext.clear(); // ALWAYS clear — even on exception
    }
}
```

### Layer 3 — Every database query filters by tenantId

```java
// BaseRepository pattern — all repositories extend this
public interface SpecRepository extends JpaRepository<ApiSpec, UUID> {

    // CORRECT — always filter by tenantId from context
    @Query("SELECT s FROM ApiSpec s WHERE s.id = :id AND s.tenantId = :tenantId")
    Optional<ApiSpec> findByIdAndTenantId(UUID id, String tenantId);

    // WRONG — never do this (exposes all tenants' data)
    // Optional<ApiSpec> findById(UUID id); -- DO NOT USE
}

// Service layer always passes tenantId from context
public ApiSpec getSpec(UUID specId) {
    String tenantId = TenantContext.getCurrentTenant();
    return specRepository.findByIdAndTenantId(specId, tenantId)
        .orElseThrow(() -> new DevDocsException("SPEC_NOT_FOUND", 404));
}
```

The key insight: if a tenant tries to access `GET /api/specs/some-other-tenants-spec-id`, the query returns empty because the `AND tenant_id = :tenantId` clause filters it out. They get a 404, not the other tenant's data.

### Layer 4 — Pinecone namespace isolation

```java
// Every vector upsert uses tenantId as namespace
public void upsertChunk(String tenantId, String chunkId, float[] embedding, Map<String, String> metadata) {
    UpsertRequest request = UpsertRequest.newBuilder()
        .setNamespace(tenantId)  // <-- namespace = tenantId
        .addVectors(Vector.newBuilder()
            .setId(chunkId)
            .addAllValues(...)
            .setMetadata(metadata)
            .build())
        .build();
    pineconeStub.upsert(request);
}

// Every search is scoped to the tenant's namespace
public List<ScoredVector> search(String tenantId, float[] queryEmbedding, int topK) {
    QueryRequest request = QueryRequest.newBuilder()
        .setNamespace(tenantId)  // <-- only searches this tenant's vectors
        .addAllVector(...)
        .setTopK(topK)
        .setIncludeMetadata(true)
        .build();
    return pineconeStub.query(request).getMatchesList();
}
```

Pinecone namespaces are fully isolated at the index level. A query on namespace `acmepay` cannot return results from namespace `stripe-inc` — this is guaranteed by Pinecone's architecture.

---

## Testing isolation

This integration test verifies that cross-tenant access is impossible:

```java
@Test
void tenantA_cannotAccessTenantB_specs() {
    // Create tenant A and upload a spec
    String tokenA = registerAndLogin("CompanyA", "a@a.com", "Pass@1234");
    UUID specId = uploadSpec(tokenA, "spec-a.yaml");

    // Create tenant B
    String tokenB = registerAndLogin("CompanyB", "b@b.com", "Pass@1234");

    // Tenant B tries to access tenant A's spec — must get 404, not the spec
    ResponseEntity<ApiSpecResponse> resp = restTemplate.exchange(
        "/api/specs/" + specId,
        HttpMethod.GET,
        new HttpEntity<>(headersWithToken(tokenB)),
        ApiSpecResponse.class
    );

    assertEquals(404, resp.getStatusCode().value()); // not 200, not 403 — 404
    // 404 is correct: we don't want to reveal the spec exists at all
}

@Test
void tenantA_ragSearch_cannotReturnTenantB_vectors() {
    // Upload specs for both tenants
    String tokenA = registerAndLogin("CompanyA", "a@a.com", "Pass@1234");
    uploadAndWaitReady(tokenA, "acmepay-api.yaml");

    String tokenB = registerAndLogin("CompanyB", "b@b.com", "Pass@1234");
    uploadAndWaitReady(tokenB, "stripe-api.yaml");

    // Ask tenant A's chatbot a question about tenant B's API content
    String answer = askChat(tokenA, "How does Stripe handle webhooks?");

    // Answer should say it doesn't know (not return Stripe docs from tenant B)
    assertFalse(answer.toLowerCase().contains("stripe"));
}
```

---

## What NOT to do

```java
// WRONG — bypasses tenant isolation
@GetMapping("/admin/specs/{id}")
public ApiSpec getSpec(@PathVariable UUID id) {
    return specRepository.findById(id).orElseThrow(); // no tenant filter!
}

// WRONG — tenantId from request path (can be spoofed)
@GetMapping("/tenants/{tenantId}/specs")
public List<ApiSpec> listSpecs(@PathVariable String tenantId) {
    return specRepository.findByTenantId(tenantId); // user controls tenantId!
}

// CORRECT — tenantId always from JWT via TenantContext
@GetMapping("/api/specs")
public List<ApiSpec> listSpecs() {
    String tenantId = TenantContext.getCurrentTenant(); // from JWT, not user
    return specRepository.findAllByTenantId(tenantId);
}
```
