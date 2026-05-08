# What's working — verified features

Update this after manually testing each feature end-to-end. Don't mark something working until you've actually clicked through it.

---

## Week 1 — Auth

| Feature | Working? | Tested how |
|---|---|---|
| Register new tenant + user | ❌ Not built | — |
| Login returns JWT | ❌ Not built | — |
| JWT refresh | ❌ Not built | — |
| Logout invalidates token | ❌ Not built | — |
| Protected routes return 401 without token | ❌ Not built | — |
| Tenant isolation in context | ❌ Not built | — |

## Week 2 — Ingestion

| Feature | Working? | Tested how |
|---|---|---|
| Upload YAML spec | ❌ Not built | — |
| File stored in S3 | ❌ Not built | — |
| Spec parsed into chunks | ❌ Not built | — |
| Chunks embedded + stored in Pinecone | ❌ Not built | — |
| Status polling works | ❌ Not built | — |

## Week 3 — RAG

| Feature | Working? | Tested how |
|---|---|---|
| Basic question → answer | ❌ Not built | — |
| Tool: search_docs called correctly | ❌ Not built | — |
| Tool: fetch_schema returns schema | ❌ Not built | — |
| Tool: generate_example returns code | ❌ Not built | — |
| Response streams via SSE | ❌ Not built | — |
| Redis caching works | ❌ Not built | — |
| Rate limiting blocks at 61/hr | ❌ Not built | — |
| Cross-tenant isolation verified | ❌ Not built | — |

## Week 4 — Deploy

| Feature | Working? | Tested how |
|---|---|---|
| Deployed to EC2 | ❌ Not built | — |
| SSL working (https) | ❌ Not built | — |
| CI/CD deploys on push | ❌ Not built | — |
| Health check green | ❌ Not built | — |
| Metrics visible in Grafana | ❌ Not built | — |

---

*Update ❌ to ✅ as you complete each feature.*
