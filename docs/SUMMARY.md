# DevDocs AI — project summary

**One line:** B2B SaaS that turns any OpenAPI spec into an AI chatbot developers can embed on their docs site.

**Built by:** Anas Khan, final-year CS student, Jabalpur

**Status:** In development — May 2026

**Live at:** devdocsai.yourdomain.com

---

## What it does

1. Company signs up and uploads their OpenAPI spec
2. System parses, chunks, and embeds it into a vector database
3. Company gets a chatbot that answers developer questions — with code examples and endpoint citations
4. Embed it on their docs site with one script tag

## Why it's interesting technically

- **Agentic RAG** — not just retrieve-and-answer; the LLM decides which tools to call: search, fetch schema, generate code, clarify
- **Multi-tenant isolation** — enforced at four independent layers (JWT, ThreadLocal, SQL, Pinecone namespace)
- **Production-grade** — Redis caching, rate limiting, CI/CD, Prometheus metrics, AWS Secrets Manager

## Stack

Java 17, Spring Boot 3.2, Next.js 14, PostgreSQL, Redis, Pinecone, OpenAI, AWS (EC2 + RDS + S3 + ElastiCache), Docker, Nginx, GitHub Actions

---
