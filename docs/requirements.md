# Requirements — DevDocs AI

## Functional requirements

### Must have (MVP)
- FR-01: Companies can register and create a tenant account
- FR-02: Tenant admin can invite team members with roles (ADMIN, MEMBER)
- FR-03: Tenant can upload OpenAPI 3.0 YAML or JSON spec files
- FR-04: System processes uploaded spec: parse → chunk → embed → store
- FR-05: Tenant gets a chatbot that answers questions about their API
- FR-06: Chatbot uses agentic RAG — searches docs, fetches schemas, generates code examples
- FR-07: Chat responses stream token-by-token (SSE)
- FR-08: Tenant can embed the chatbot on their own docs site via script tag
- FR-09: Each tenant's data is fully isolated from all other tenants

### Nice to have (Week 5+)
- FR-10: Analytics dashboard showing question volume, cache hit rate, top questions
- FR-11: Multiple specs per tenant (e.g. v1 API + v2 API)
- FR-12: Chat history viewable by tenant admin
- FR-13: Custom system prompt per tenant ("Always respond in Spanish")
- FR-14: Usage-based billing via Stripe (FREE: 500 questions/mo, PRO: unlimited)

## Non-functional requirements

| Requirement | Target |
|---|---|
| RAG response latency (cache miss) | p95 < 3 seconds |
| RAG response latency (cache hit) | p95 < 150ms |
| API uptime | 99.5%+ |
| Max spec file size | 5MB |
| Max tenants supported (MVP) | 50 (single EC2) |
| Auth token expiry | Access: 15 min, Refresh: 7 days |
| Rate limit per tenant | 60 RAG queries/hour |
| Cache TTL | 1 hour |
| Data isolation | Zero cross-tenant leakage (verified by integration test) |
