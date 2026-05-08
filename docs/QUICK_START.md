# Quick start — DevDocs AI

Get the full stack running locally in under 10 minutes.

---

## Prerequisites

Install these before starting:

| Tool | Version | Check |
|---|---|---|
| Java JDK | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 18+ | `node -version` |
| Docker | 24+ | `docker -version` |
| Docker Compose | 2.x | `docker compose version` |

You also need accounts (free tiers work):
- **OpenAI** — [platform.openai.com](https://platform.openai.com) (for embeddings + GPT-4o-mini)
- **Pinecone** — [pinecone.io](https://pinecone.io) (free tier: 1 index)
- **AWS** — S3 bucket + IAM user with S3 access

---

## Step 1 — Clone and configure

```bash
git clone https://github.com/YOUR_USERNAME/devdocs-ai.git
cd devdocs-ai
```

Copy the example environment file:

```bash
cp backend/src/main/resources/application-local.properties.example \
   backend/src/main/resources/application-local.properties
```

Edit `application-local.properties` and fill in your keys:

```properties
# Database (Docker handles this locally — no change needed)
spring.datasource.url=jdbc:postgresql://localhost:5432/devdocsai
spring.datasource.username=postgres
spring.datasource.password=postgres

# Redis (Docker handles this locally)
spring.data.redis.host=localhost
spring.data.redis.port=6379

# OpenAI
openai.api.key=sk-YOUR_KEY_HERE
openai.chat.model=gpt-4o-mini
openai.embedding.model=text-embedding-ada-002

# Pinecone
pinecone.api.key=YOUR_PINECONE_KEY
pinecone.index.name=devdocsai
pinecone.environment=YOUR_ENV

# AWS S3
aws.s3.bucket=devdocsai-specs
aws.region=ap-south-1
aws.access-key=YOUR_ACCESS_KEY
aws.secret-key=YOUR_SECRET_KEY

# JWT
jwt.secret=your-256-bit-secret-here-make-it-long-and-random
jwt.access-token.expiry=900000
jwt.refresh-token.expiry=604800000
```

---

## Step 2 — Start infrastructure

```bash
# From project root
docker compose up -d postgres redis
```

This starts:
- PostgreSQL on `localhost:5432` (db: `devdocsai`, user: `postgres`, pass: `postgres`)
- Redis on `localhost:6379`

Verify they're running:

```bash
docker compose ps
```

---

## Step 3 — Run database migrations

```bash
cd backend
./mvnw flyway:migrate -Dspring.profiles.active=local
```

This creates all tables. Check `src/main/resources/db/migration/` for the SQL files.

---

## Step 4 — Start the backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Backend starts on `http://localhost:8080`

Verify: `curl http://localhost:8080/actuator/health`

Expected: `{"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}`

---

## Step 5 — Start the frontend

```bash
cd frontend
npm install
cp .env.example .env.local
```

Edit `.env.local`:

```
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

```bash
npm run dev
```

Frontend starts on `http://localhost:3000`

---

## Step 6 — Create your first tenant

Open `http://localhost:3000/register` and create an account.

Or use the API directly:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "AcmePay",
    "email": "admin@acmepay.com",
    "password": "Test@1234"
  }'
```

---

## Step 7 — Upload a test API spec

Download a sample OpenAPI spec:

```bash
curl -o sample-spec.yaml \
  https://raw.githubusercontent.com/OAI/OpenAPI-Specification/main/examples/v3.0/petstore.yaml
```

Upload via the dashboard or API:

```bash
# First login to get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@acmepay.com","password":"Test@1234"}' \
  | jq -r '.accessToken')

# Upload spec
curl -X POST http://localhost:8080/api/specs/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@sample-spec.yaml" \
  -F "name=PetStore API"
```

---

## Step 8 — Ask the chatbot

Once ingestion status is `READY`, open the dashboard and ask:

> "How do I list all pets?"
> "What authentication does this API use?"
> "Show me a Python example for creating a pet"

---

## Useful commands

```bash
# View backend logs
./mvnw spring-boot:run 2>&1 | tee backend.log

# Run all tests
cd backend && ./mvnw test

# Run only unit tests
./mvnw test -Dtest="*Test" -DfailIfNoTests=false

# Run only integration tests
./mvnw test -Dtest="*IT" -DfailIfNoTests=false

# Reset database (careful!)
docker compose down -v && docker compose up -d postgres redis
./mvnw flyway:migrate -Dspring.profiles.active=local

# Check Redis
docker exec -it devdocsai-redis redis-cli KEYS "*"

# Check Postgres
docker exec -it devdocsai-postgres psql -U postgres -d devdocsai -c "\dt"
```

---

## Common issues

See [`DEPENDENCY_FIX.md`](DEPENDENCY_FIX.md) for known dependency issues.

| Symptom | Likely cause | Fix |
|---|---|---|
| `Connection refused 5432` | Postgres not started | `docker compose up -d postgres` |
| `401 Unauthorized` on all requests | JWT secret mismatch | Check `jwt.secret` in properties |
| Ingestion stuck at PROCESSING | OpenAI API key invalid | Check key + quota |
| Pinecone 404 error | Wrong index name or env | Check pinecone settings |
| Redis `WRONGTYPE` error | Key type conflict from old run | `docker exec redis redis-cli FLUSHDB` |
