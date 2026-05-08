# DevDocs AI 🤖

> **B2B SaaS platform** that turns any OpenAPI/Swagger spec into an intelligent, agentic chatbot — deployed on your docs site in minutes.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-14-black)](https://nextjs.org/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![CI](https://github.com/YOUR_USERNAME/devdocs-ai/actions/workflows/deploy.yml/badge.svg)](https://github.com/YOUR_USERNAME/devdocs-ai/actions)

---

## What is DevDocs AI?

DevDocs AI is a **multi-tenant SaaS platform** where companies upload their API documentation (OpenAPI 3.0 spec) and instantly get an AI-powered chatbot that answers developer questions about their API — with code examples, schema details, and real citations.

### Key highlights

- **Agentic RAG pipeline** — the LLM decides which tool to call: search docs, fetch schemas, generate code examples, or ask for clarification
- **Multi-tenant isolation** — each company's data is fully isolated at the vector DB, database, and application layer
- **Streaming responses** — answers stream token-by-token via SSE, ChatGPT-style
- **Embeddable widget** — a `<script>` tag companies paste into their existing docs site
- **Production-grade** — Redis caching, CI/CD, Prometheus metrics, AWS Secrets Manager, rate limiting

---

## Live Demo

🌐 **[devdocsai.yourdomain.com](https://devdocsai.yourdomain.com)**

Demo tenant: **AcmePay API** — ask it anything about authentication, endpoints, or request formats.

---

## Architecture overview

```
Client (Next.js) → Cloudflare CDN → Nginx → Spring Boot API
                                              ├── Auth Service (JWT)
                                              ├── Tenant Service (multi-tenancy)
                                              ├── Ingestion Service (parse → chunk → embed)
                                              └── Agentic RAG Engine
                                                    ├── Tool: search_docs      → Pinecone
                                                    ├── Tool: fetch_schema     → PostgreSQL
                                                    ├── Tool: generate_example → LLM
                                                    └── Cache layer            → Redis
```

See [`docs/architecture.md`](docs/architecture.md) for the full architecture with diagrams.

---

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 14, TypeScript, Tailwind CSS, ShadCN UI |
| Backend | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA |
| AI / RAG | OpenAI GPT-4o-mini, text-embedding-ada-002, LangChain4j |
| Vector DB | Pinecone (multi-tenant namespaces) |
| Database | PostgreSQL (AWS RDS) |
| Cache | Redis (AWS ElastiCache) |
| Storage | AWS S3 (raw spec files) |
| Infra | AWS EC2, Nginx, Certbot SSL, Cloudflare |
| CI/CD | GitHub Actions → Docker → EC2 |
| Observability | Spring Actuator, Prometheus, Grafana, Logback JSON |

---

## Quick start

See [`docs/QUICK_START.md`](docs/QUICK_START.md) for full local setup instructions.

```bash
# Clone
git clone https://github.com/YOUR_USERNAME/devdocs-ai.git
cd devdocs-ai

# Start dependencies (Postgres + Redis)
docker-compose up -d postgres redis

# Backend
cd backend
./mvnw spring-boot:run

# Frontend
cd frontend
npm install && npm run dev
```

Open [http://localhost:3000](http://localhost:3000)

---

## Project structure

```
devdocs-ai/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/devdocsai/
│   │   ├── auth/               # JWT, refresh tokens, security filter
│   │   ├── tenant/             # Multi-tenancy, tenant context
│   │   ├── ingestion/          # Parse, chunk, embed pipeline
│   │   ├── rag/                # Agentic RAG engine, tool definitions
│   │   ├── chat/               # SSE streaming, conversation history
│   │   ├── config/             # Spring Security, Redis, AWS config
│   │   └── common/             # Exception handling, response wrappers
│   └── src/test/               # Unit + integration tests
├── frontend/                   # Next.js application
│   ├── app/                    # App Router pages
│   ├── components/             # Reusable UI components
│   └── lib/                    # API client, auth helpers, types
├── docs/                       # All project documentation
├── infra/                      # Nginx config, docker-compose, GitHub Actions
├── docker-compose.yml          # Local dev environment
└── README.md
```

---

## Built by

**Anas Khan** — Final-year CS student, full stack engineer  
[LinkedIn](https://linkedin.com/in/YOUR_PROFILE) · [GitHub](https://github.com/YOUR_USERNAME) · [Portfolio](https://yourportfolio.com)
