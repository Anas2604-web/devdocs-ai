# Tasks — DevDocs AI build plan

Track your progress here. Check off items as you complete them.

---

## Week 1 — Auth, multi-tenancy, database

### Backend
- [ ] Init Spring Boot project with correct dependencies
- [ ] Create `application.properties` profiles (local, prod)
- [ ] Write Flyway migration V1 — create all tables
- [ ] Implement `TenantContext` (ThreadLocal)
- [ ] Implement `JwtService` — generate + validate tokens
- [ ] Implement `JwtAuthFilter` — Spring Security filter chain
- [ ] Implement `AuthController` — register, login, refresh, logout
- [ ] Implement `UserService` — BCrypt password, save user + tenant
- [ ] Configure Spring Security — permit /auth/**, secure everything else
- [ ] Add `GlobalExceptionHandler` with standard error response format
- [ ] Write `JwtServiceTest` — all token cases
- [ ] Write `AuthControllerIT` — full auth flow integration test

### Frontend
- [ ] Init Next.js 14 with TypeScript + Tailwind + ShadCN
- [ ] Install dependencies: axios, zustand, react-hook-form, zod
- [ ] Create auth store (Zustand) — token, user, tenant
- [ ] Build `/register` page with form validation
- [ ] Build `/login` page
- [ ] Build `/dashboard` protected layout (redirect if no token)
- [ ] Build dashboard home — tenant name, plan, stats placeholders
- [ ] Add Axios interceptor — attach Bearer token, handle 401 refresh

### Week 1 done when:
User can register, login, see their dashboard, logout. JWT refresh works. Push to GitHub with clear commit messages.

---

## Week 2 — Ingestion pipeline, S3, embeddings

### Backend
- [ ] Add AWS SDK + OpenAI dependencies to pom.xml
- [ ] Configure S3Client bean with credentials from properties
- [ ] Implement `S3Service` — upload file, generate presigned URL
- [ ] Add swagger-parser dependency
- [ ] Implement `SpecParserService` — parse OpenAPI YAML/JSON
- [ ] Implement `ChunkingService` — extract endpoints as chunks
- [ ] Implement `EmbeddingService` — call OpenAI Ada-002 per chunk
- [ ] Implement `PineconeService` — upsert vectors with tenant namespace
- [ ] Implement `IngestionService` — orchestrate full pipeline (async @Async)
- [ ] Implement `SpecController` — upload, list, get status, delete
- [ ] Add status polling: `GET /api/specs/{id}/status`
- [ ] Write `ChunkingServiceTest` — correct chunk count, edge cases
- [ ] Write `IngestionServiceIT` — status transitions end-to-end

### Frontend
- [ ] Build `/dashboard/specs` page — list all specs
- [ ] Build spec upload component (react-dropzone)
- [ ] Add polling for ingestion status (useInterval hook)
- [ ] Show status badge: PENDING / PROCESSING / READY / FAILED
- [ ] Add delete spec with confirmation dialog

### Week 2 done when:
Upload Petstore YAML → wait for READY → verify chunks in Pinecone console → verify chunk rows in PostgreSQL.

---

## Week 3 — Agentic RAG engine, chat UI, SSE streaming

### Backend
- [ ] Add LangChain4j or Spring AI dependency
- [ ] Define tool interfaces: `SearchDocsTool`, `FetchSchemaTool`, `GenerateExampleTool`, `ClarifyTool`
- [ ] Implement `SearchDocsTool` — embed query → search Pinecone → return top-5 chunks
- [ ] Implement `FetchSchemaTool` — query PostgreSQL by path+method
- [ ] Implement `GenerateExampleTool` — secondary LLM call for code snippets
- [ ] Implement `RagOrchestrator` — agent loop with tool calling
- [ ] Implement `ConversationHistoryService` — Redis store, 10-message window
- [ ] Implement `RagCacheService` — check/store by queryHash+tenantId
- [ ] Implement `RateLimitService` — Redis counter, 60/hr per tenant
- [ ] Implement `ChatController` — SSE endpoint (TEXT_EVENT_STREAM_VALUE)
- [ ] Write `RagCacheServiceTest`
- [ ] Write `RateLimitServiceTest`
- [ ] Write `ToolExecutorTest` for each tool
- [ ] Write `TenantIsolationIT` — verify cross-tenant data leakage is impossible

### Frontend
- [ ] Build `/dashboard/chat` page
- [ ] Implement SSE client (EventSource) for streaming response
- [ ] Build message list component — user messages + streamed assistant response
- [ ] Add tool-call indicator: "🔍 Searching docs..."
- [ ] Render code blocks with react-syntax-highlighter
- [ ] Add copy button on code blocks
- [ ] Add session reset button

### Week 3 done when:
Ask "how do I list all pets?" and get a streaming answer that cites the correct endpoint. Verify cache works on second identical question (check Redis).

---

## Week 4 — AWS deploy, CI/CD, observability, security

### Infrastructure
- [ ] Write `Dockerfile` for Spring Boot (multi-stage)
- [ ] Write `Dockerfile` for Next.js (multi-stage)
- [ ] Update `docker-compose.yml` for production (no dev volumes)
- [ ] Write Nginx config: proxy rules, gzip, security headers
- [ ] Set up EC2 t3.medium — security groups (80, 443, 22 only)
- [ ] Assign Elastic IP
- [ ] Set up RDS PostgreSQL (free tier)
- [ ] Set up ElastiCache Redis (or EC2 Redis)
- [ ] Run Certbot for SSL on your domain
- [ ] Add domain to Cloudflare, proxy enabled
- [ ] Store all secrets in AWS Secrets Manager
- [ ] Configure Spring Boot to read from Secrets Manager on startup

### CI/CD
- [ ] Write `.github/workflows/deploy.yml`
- [ ] Add EC2 SSH key to GitHub Secrets
- [ ] Test: push to main → auto deploy → health check passes
- [ ] Add deploy status badge to README

### Observability
- [ ] Add Micrometer + Prometheus dependency
- [ ] Expose `/actuator/health` and `/actuator/prometheus`
- [ ] Add custom metrics: RAG latency, cache hit rate
- [ ] Configure structured JSON logging (Logback)
- [ ] Install Prometheus + Grafana on EC2 (or use Grafana Cloud free)

### Security hardening
- [ ] Add CORS config — only allow your frontend domain
- [ ] Add security headers in Nginx (HSTS, X-Frame-Options)
- [ ] Add request size limits (5MB max upload)
- [ ] Verify .gitignore — no secrets, no .env files committed
- [ ] Enable Cloudflare WAF (free tier)

### Week 4 done when:
`git push main` → GitHub Actions deploys → app is live at your domain → health check green → metrics visible.

---

## Week 5 — Polish and recruiter-ready

- [ ] Build embeddable widget (`/embed/{slug}/widget.js`)
- [ ] Build analytics dashboard page (Recharts)
- [ ] Write detailed README with architecture diagram
- [ ] Record 3-minute Loom demo video
- [ ] Create demo tenant "AcmePay API" with real-looking spec
- [ ] Build landing page at root domain
- [ ] Add "Request access" email capture
- [ ] Update resume with project + live link
- [ ] Post on LinkedIn with demo video

---

## Ongoing — every day

- [ ] Write tests for every new feature before considering it done
- [ ] Meaningful commit messages: `feat: add JWT refresh token rotation`
- [ ] Keep README updated as features are added
- [ ] Update `IMPLEMENTATION_STATUS.md` daily
