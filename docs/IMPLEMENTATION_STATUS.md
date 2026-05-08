# Implementation status

Update this file daily. Be honest — it helps you track real progress.

Last updated: May 2026

---

## Overall progress

```
Week 1 — Auth + DB          ░░░░░░░░░░  0%
Week 2 — Ingestion          ░░░░░░░░░░  0%
Week 3 — Agentic RAG        ░░░░░░░░░░  0%
Week 4 — Deploy + CI/CD     ░░░░░░░░░░  0%
Week 5 — Polish             ░░░░░░░░░░  0%
```

---

## What's working

Nothing yet — project just initialized. Update this as you build.

---

## In progress

- [ ] Spring Boot project initialization
- [ ] Next.js project initialization

---

## Blocked on

Nothing blocked yet. Document blockers here as they come up with:
- What's blocked
- Why it's blocked
- What you tried
- What you need to unblock it

---

## Decisions made

| Decision | What we chose | Why |
|---|---|---|
| LLM | GPT-4o-mini | Cheaper than GPT-4, good enough for RAG |
| Vector DB | Pinecone | Free tier, simple API, good Java SDK |
| Embedding model | text-embedding-ada-002 | Best price/performance for semantic search |
| Multi-tenancy | ThreadLocal + row-level filter | Simple, no schema-per-tenant overhead |
| Caching | Redis TTL-based | Simple, no manual invalidation bugs |
| Frontend deploy | Vercel (later EC2) | Fast to start, easy CI |

---

## Known issues

Document bugs here as you find them. Don't try to fix everything immediately.

| Issue | Severity | Status |
|---|---|---|
| — | — | — |

---

## Performance baseline (fill after Week 4)

| Metric | Target | Actual |
|---|---|---|
| RAG response (cache miss) | < 3s | TBD |
| RAG response (cache hit) | < 100ms | TBD |
| Ingestion (50-endpoint spec) | < 30s | TBD |
| API p95 latency | < 500ms | TBD |
| Cache hit rate | > 60% | TBD |
