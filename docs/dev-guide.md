# Developer guide — DevDocs AI

## Git workflow

### Branch naming
```
feature/week1-jwt-auth
feature/week2-ingestion-pipeline
fix/refresh-token-rotation-bug
test/rag-cache-service-tests
docs/update-architecture
```

### Commit message format
Follow Conventional Commits:
```
feat: add JWT refresh token rotation
fix: correct tenant isolation in spec deletion
test: add integration tests for auth controller
docs: update architecture with ingestion flow
refactor: extract ChunkingService from IngestionService
chore: add Testcontainers dependency
```

### PR process (even if you're solo)
1. Create a branch for each feature
2. Make small commits (one logical change each)
3. Self-review your diff before merging
4. Merge to main only when feature works + tests pass

## Code standards

### No magic strings — use constants
```java
// Bad
if (spec.getStatus().equals("READY")) { }

// Good
if (spec.getStatus() == IngestionStatus.READY) { }
```

### Always validate input at the controller layer
```java
@PostMapping("/register")
public ResponseEntity<ApiResponse<Void>> register(
        @RequestBody @Valid RegisterRequest request) { // @Valid triggers validation
    authService.register(request);
    return ResponseEntity.status(201).body(ApiResponse.ok(null));
}
```

### Never catch generic Exception and swallow it
```java
// Bad
try {
    doSomething();
} catch (Exception e) {
    log.error("Error"); // no stack trace, no rethrow — bug lost forever
}

// Good
try {
    doSomething();
} catch (OpenAiException e) {
    log.error("OpenAI call failed for tenant {}", tenantId, e);
    throw new DevDocsException("OPENAI_ERROR", HttpStatus.SERVICE_UNAVAILABLE);
}
```

### Always clear ThreadLocal in finally block
```java
try {
    TenantContext.setCurrentTenant(tenantId);
    // ... do work
} finally {
    TenantContext.clear(); // runs even if exception thrown
}
```

## Local dev tips

- Use `application-local.properties` for all local secrets — it's git-ignored
- Run `docker compose up -d` once, leave it running all day
- Use IntelliJ's HTTP Client (`.http` files) to test endpoints without Postman
- Check `http://localhost:8080/actuator/health` after every restart
- `CTRL+C` stops Spring Boot — Redis and Postgres stay running
