# Design — DevDocs AI

## Component responsibilities

| Component | Responsibility | Does NOT do |
|---|---|---|
| `AuthController` | Handle HTTP auth requests | Business logic |
| `AuthService` | Register, login, token management | DB access |
| `UserRepository` | DB queries for users | Business logic |
| `TenantContext` | Store tenantId for current thread | Validation |
| `JwtService` | Generate + validate JWT | Auth decisions |
| `IngestionService` | Orchestrate parse → chunk → embed | Parsing logic |
| `ChunkingService` | Parse OpenAPI → chunks | Embedding |
| `EmbeddingService` | Call OpenAI embeddings API | Vector storage |
| `PineconeService` | Upsert + query vectors | Business logic |
| `RagOrchestrator` | Agent loop, tool calling | Individual tools |
| `RagCacheService` | Redis get/put for answers | Cache strategy |
| `RateLimitService` | Count + enforce query limits | Auth |

## Package structure

```
com.devdocsai/
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── JwtService.java
│   ├── JwtAuthFilter.java
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   └── LoginResponse.java
│   └── model/
│       └── User.java
│
├── tenant/
│   ├── TenantContext.java
│   ├── TenantService.java
│   └── model/
│       └── Tenant.java
│
├── ingestion/
│   ├── IngestionController.java
│   ├── IngestionService.java       ← orchestrates
│   ├── ChunkingService.java        ← parses OpenAPI
│   ├── EmbeddingService.java       ← calls OpenAI
│   ├── PineconeService.java        ← vector storage
│   ├── S3Service.java
│   └── model/
│       ├── ApiSpec.java
│       └── ApiChunk.java
│
├── rag/
│   ├── RagOrchestrator.java        ← agent loop
│   ├── RagCacheService.java
│   ├── RateLimitService.java
│   ├── ConversationHistoryService.java
│   └── tools/
│       ├── SearchDocsTool.java
│       ├── FetchSchemaTool.java
│       ├── GenerateExampleTool.java
│       └── ClarifyTool.java
│
├── chat/
│   └── ChatController.java         ← SSE endpoint
│
├── config/
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   ├── AwsConfig.java
│   └── OpenAiConfig.java
│
└── common/
    ├── GlobalExceptionHandler.java
    ├── ErrorResponse.java
    ├── DevDocsException.java
    └── ApiResponse.java
```

## Standard API response wrapper

All successful responses use this wrapper:

```java
// Success
{
  "success": true,
  "data": { ... }
}

// Error (see ERROR_HANDLING.md)
{
  "success": false,
  "error": { "code": "...", "message": "..." }
}
```

```java
public class ApiResponse<T> {
    private boolean success;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }
}
```
