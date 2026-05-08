# Architecture — DevDocs AI

## 1. System overview

DevDocs AI is a multi-tenant B2B SaaS platform. Each "tenant" is a company that uploads their API documentation and gets a hosted AI chatbot for their developers.

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                             │
│           Next.js 14 (App Router) — Vercel / EC2               │
└─────────────────────────┬───────────────────────────────────────┘
                          │ HTTPS
┌─────────────────────────▼───────────────────────────────────────┐
│                    EDGE / GATEWAY LAYER                         │
│    Cloudflare CDN · WAF · Rate limiting · DDoS protection       │
│              SSL termination (Let's Encrypt)                    │
└─────────────────────────┬───────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                    AWS EC2 (t3.medium)                          │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Nginx Reverse Proxy                         │   │
│  │  /api/* → :8080 (Spring Boot)  /* → :3000 (Next.js)     │   │
│  └──────────┬─────────────────────────────┬─────────────────┘   │
│             │                             │                     │
│  ┌──────────▼──────────┐     ┌────────────▼─────────────────┐   │
│  │   Spring Boot API   │     │      Next.js Frontend        │   │
│  │   (Port 8080)       │     │      (Port 3000 / PM2)       │   │
│  │                     │     └──────────────────────────────┘   │
│  │  ┌───────────────┐  │                                        │
│  │  │ Auth Service  │  │                                        │
│  │  │ JWT + RBAC    │  │                                        │
│  │  └───────┬───────┘  │                                        │
│  │  ┌───────▼───────┐  │                                        │
│  │  │Tenant Service │  │                                        │
│  │  │ThreadLocal ctx│  │                                        │
│  │  └───────┬───────┘  │                                        │
│  │  ┌───────▼────────┐ │                                        │
│  │  │Ingestion Svc   │ │                                        │
│  │  │parse→chunk→emb │ │                                        │
│  │  └───────┬────────┘ │                                        │
│  │  ┌───────▼────────┐ │                                        │
│  │  │ Agentic RAG    │ │  ←── the brain of the system           │
│  │  │ Engine         │ │                                        │
│  │  └───────┬────────┘ │                                        │
│  └──────────┼──────────┘                                        │
└─────────────┼─────────────────────────────────────────────────--┘
              │
    ┌─────────┼─────────────────────────────────┐
    │         │     Data & AI Services           │
    │   ┌─────▼──────┐  ┌──────────┐  ┌──────┐  │
    │   │ PostgreSQL  │  │  Redis   │  │  S3  │  │
    │   │ (AWS RDS)   │  │(ElastiC.)│  │      │  │
    │   └─────────────┘  └──────────┘  └──────┘  │
    │   ┌─────────────┐  ┌──────────────────────┐ │
    │   │  Pinecone   │  │  OpenAI API          │ │
    │   │ (Vector DB) │  │  GPT-4o-mini + Ada   │ │
    │   └─────────────┘  └──────────────────────┘ │
    └────────────────────────────────────────────--┘
```

---

## 2. Agentic RAG engine — detailed flow

This is the most important part of the system. It is what makes this project stand out.

```
User question
     │
     ▼
┌────────────────────────────────────────────────┐
│          RAG Orchestrator (Spring)             │
│                                                │
│  1. Check Redis cache (query_hash + tenant)    │
│     └── HIT  → return cached answer           │
│     └── MISS → continue                       │
│                                                │
│  2. Build LLM prompt with:                    │
│     - System prompt (tenant context)           │
│     - Conversation history (last 10 msgs)      │
│     - User question                            │
│     - Tool definitions (4 tools)               │
│                                                │
│  3. Call OpenAI API                            │
│     └── Response: tool_call?                   │
│           YES → execute tool, append result    │
│                 → loop back to step 3          │
│           NO  → stream final answer via SSE    │
│                 → cache result in Redis        │
└────────────────────────────────────────────────┘

4 Tools the LLM can call:
┌─────────────────────────────────────────────────────────────────┐
│ search_docs(query: string)                                      │
│   → embed query → search Pinecone (tenant namespace)           │
│   → return top-5 chunks with similarity scores                 │
├─────────────────────────────────────────────────────────────────┤
│ fetch_endpoint_schema(path: string, method: string)             │
│   → query PostgreSQL for full endpoint definition               │
│   → return request params, headers, response schema            │
├─────────────────────────────────────────────────────────────────┤
│ generate_code_example(endpoint: string, language: string)       │
│   → secondary LLM call to generate curl / JS / Python snippet  │
│   → return formatted code block                                │
├─────────────────────────────────────────────────────────────────┤
│ clarify_question(ambiguous_part: string)                        │
│   → return a follow-up question for the user                   │
│   → agent pauses and waits for user response                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Multi-tenant isolation strategy

Multi-tenancy is enforced at **four layers**:

```
Layer 1 — JWT token
  Every JWT contains: { sub: userId, tenantId: "acme-corp", role: "ADMIN" }
  JwtFilter extracts tenantId on every request.

Layer 2 — ThreadLocal context
  TenantContext.setCurrentTenant(tenantId)
  All downstream services call TenantContext.getCurrentTenant()
  Never pass tenantId as a parameter — it always comes from context.

Layer 3 — PostgreSQL row-level filtering
  Every table has a tenant_id column.
  All repository queries add: WHERE tenant_id = :tenantId
  BaseRepository enforces this automatically via @Query.

Layer 4 — Pinecone namespace isolation
  Each tenant's embeddings live in a separate Pinecone namespace.
  Namespace = tenantId. Cross-tenant search is structurally impossible.
```

---

## 4. Ingestion pipeline — sequence

```
POST /api/specs/upload (multipart file)
    │
    ▼
┌──────────────────────────────────────────────────────┐
│  IngestionController                                  │
│  1. Validate file (YAML/JSON, max 5MB)               │
│  2. Save metadata to PostgreSQL (status: PENDING)    │
│  3. Upload raw file to S3 (tenant-id/spec-id/raw)   │
│  4. Return 202 Accepted + specId                     │
└──────────────────┬───────────────────────────────────┘
                   │ @Async (background thread)
                   ▼
┌──────────────────────────────────────────────────────┐
│  IngestionService                                     │
│  1. Update status: PROCESSING                        │
│  2. Parse OpenAPI spec (swagger-parser)              │
│  3. Extract endpoints → create chunks:               │
│     chunk = { method, path, description,             │
│               params, requestBody, responses }       │
│  4. For each chunk:                                  │
│     a. Call OpenAI text-embedding-ada-002            │
│     b. Store embedding in Pinecone (namespace=tenant)│
│     c. Store chunk text in PostgreSQL                │
│  5. Update status: READY                             │
└──────────────────────────────────────────────────────┘
```

---

## 5. Database schema

```sql
-- Tenants (companies using the platform)
CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) UNIQUE NOT NULL,      -- used in embed script
    plan        VARCHAR(50) DEFAULT 'FREE',         -- FREE, PRO, ENTERPRISE
    api_key     VARCHAR(255) UNIQUE NOT NULL,       -- for embed widget auth
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Users (employees of a tenant)
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50) DEFAULT 'MEMBER',     -- ADMIN, MEMBER
    created_at    TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, email)
);

-- API specifications uploaded by tenants
CREATE TABLE api_specs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    version     VARCHAR(50),
    s3_key      VARCHAR(500) NOT NULL,             -- raw file location
    status      VARCHAR(50) DEFAULT 'PENDING',     -- PENDING, PROCESSING, READY, FAILED
    chunk_count INTEGER DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Individual chunks from parsed specs
CREATE TABLE api_chunks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    spec_id         UUID NOT NULL REFERENCES api_specs(id) ON DELETE CASCADE,
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    endpoint_method VARCHAR(10),                   -- GET, POST, etc.
    endpoint_path   VARCHAR(500),                  -- /users/{id}
    chunk_text      TEXT NOT NULL,                 -- full text for retrieval
    pinecone_id     VARCHAR(255),                  -- ID in Pinecone
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Chat sessions
CREATE TABLE chat_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    session_key VARCHAR(255) NOT NULL,             -- anonymous users get a key
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Usage analytics
CREATE TABLE query_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    session_id      UUID REFERENCES chat_sessions(id),
    question_hash   VARCHAR(64),                   -- SHA-256 of question (no PII)
    tool_calls      VARCHAR(500),                  -- which tools were used
    cache_hit       BOOLEAN DEFAULT FALSE,
    latency_ms      INTEGER,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_specs_tenant ON api_specs(tenant_id);
CREATE INDEX idx_chunks_tenant ON api_chunks(tenant_id);
CREATE INDEX idx_chunks_spec ON api_chunks(spec_id);
CREATE INDEX idx_logs_tenant ON query_logs(tenant_id);
CREATE INDEX idx_logs_created ON query_logs(created_at);
```

---

## 6. API contracts

### Auth endpoints

```
POST   /api/auth/register          Register new tenant + admin user
POST   /api/auth/login             Login, returns JWT in httpOnly cookie
POST   /api/auth/refresh           Refresh access token
POST   /api/auth/logout            Invalidate refresh token
```

### Tenant endpoints (requires auth)

```
GET    /api/tenant/me              Current tenant info
PUT    /api/tenant/me              Update tenant settings
GET    /api/tenant/me/stats        Usage stats (questions, cache rate, etc.)
```

### Spec management

```
POST   /api/specs/upload           Upload OpenAPI spec file (multipart)
GET    /api/specs                  List all specs for current tenant
GET    /api/specs/{id}             Get spec details + status
DELETE /api/specs/{id}             Delete spec + all chunks + Pinecone vectors
GET    /api/specs/{id}/status      Poll ingestion status (PENDING/PROCESSING/READY)
```

### Chat / RAG

```
POST   /api/chat/start             Start new chat session, returns sessionId
GET    /api/chat/{sessionId}/ask?q=...   SSE endpoint — streams response tokens
POST   /api/chat/{sessionId}/reset       Clear conversation history
```

### Public embed widget

```
GET    /embed/{tenantSlug}/widget.js     Embeddable script (public, no auth)
POST   /embed/{tenantSlug}/chat          Widget chat endpoint (uses api_key auth)
```

---

## 7. Caching strategy

```
Redis key patterns:
  rag:cache:{tenantId}:{queryHash}     → cached RAG answer (TTL: 1 hour)
  session:{sessionId}:history          → conversation messages (TTL: 24 hours)
  ratelimit:{tenantId}:queries:{hour}  → counter for rate limiting (TTL: 1 hour)
  tenant:{tenantId}:stats              → pre-computed stats (TTL: 5 min)

Cache invalidation:
  - New spec uploaded → clear all rag:cache:{tenantId}:* keys
  - TTL-based expiry for everything else (no manual invalidation complexity)
```

---

## 8. CI/CD pipeline

```
Developer pushes to main
         │
         ▼
GitHub Actions workflow
  Step 1: Run unit tests (mvn test)
  Step 2: Run integration tests (Testcontainers)
  Step 3: Build Docker image
  Step 4: Push to AWS ECR
  Step 5: SSH into EC2
  Step 6: docker-compose pull && docker-compose up -d
  Step 7: Health check GET /actuator/health
  Step 8: Notify (Slack or email) on success/failure
```

---

## 9. Security architecture

```
1. Authentication
   - JWT access tokens (15 min expiry) in memory
   - Refresh tokens (7 days) in httpOnly cookie
   - Refresh token rotation on every use
   - Token blacklist in Redis on logout

2. Authorization
   - Spring Security method-level @PreAuthorize
   - Roles: ADMIN (full access), MEMBER (chat only)
   - Every service layer validates tenant ownership

3. Transport
   - HTTPS everywhere (Certbot + Nginx)
   - HSTS, X-Frame-Options, X-Content-Type-Options headers
   - Cloudflare WAF for bot and injection protection

4. Input validation
   - @Valid + @NotBlank on all request DTOs
   - Max file size enforced (5MB spec uploads)
   - SQL injection: JPA parameterized queries only
   - XSS: sanitize all user text before LLM prompt injection

5. Secrets management
   - All secrets (DB password, OpenAI key, Redis password) in AWS Secrets Manager
   - Spring Boot fetches at startup via AWS SDK
   - Zero secrets in code, .env files, or Docker images
   - .gitignore includes application-secrets.properties

6. Rate limiting
   - 60 RAG queries/hour per tenant (Redis counter)
   - 429 response with Retry-After header
   - 10 spec uploads/day per tenant
```

---

## 10. Observability

```
Metrics (Prometheus + Grafana):
  - rag.query.latency        → histogram (p50, p95, p99)
  - rag.cache.hit_rate       → gauge
  - ingestion.duration       → histogram
  - active.tenants           → gauge
  - questions.per.minute     → counter

Logging (Logback JSON):
  - Every request: tenant_id, endpoint, latency, status
  - Every RAG call: tools_called, cache_hit, latency
  - Never log: passwords, tokens, user question text (privacy)

Health checks:
  - GET /actuator/health → { db, redis, pinecone, openai }
  - GitHub Actions pings this after every deploy
```
