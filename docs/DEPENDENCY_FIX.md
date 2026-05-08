# Known dependency issues and fixes

Update this file whenever you hit a dependency conflict.

---

## Spring Boot 3.x + Java 17

Spring Boot 3.x requires Java 17+. If you see:
```
Unsupported class file major version 61
```
Fix: ensure `JAVA_HOME` points to JDK 17, not JDK 11.

```bash
java -version  # should show 17
export JAVA_HOME=/path/to/jdk17
```

---

## LangChain4j version compatibility

LangChain4j changes APIs frequently. Use this exact version in pom.xml:

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
    <version>0.31.0</version>
</dependency>
```

If using Spring AI instead:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M3</version>
</dependency>
```
Spring AI requires adding the Spring milestone repository to pom.xml.

---

## Testcontainers + M1/M2 Mac

If you're on Apple Silicon and Testcontainers fails:
```
Could not find a valid Docker environment
```
Fix: install Colima instead of Docker Desktop, or set:
```
export TESTCONTAINERS_RYUK_DISABLED=true
export DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock
```

---

## swagger-parser dependency

```xml
<dependency>
    <groupId>io.swagger.parser.v3</groupId>
    <artifactId>swagger-parser</artifactId>
    <version>2.1.20</version>
</dependency>
```

This may conflict with springdoc-openapi. If so, exclude:
```xml
<exclusion>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-models</artifactId>
</exclusion>
```

---

## Redis + Spring Boot 3.x

Use `spring-boot-starter-data-redis` (not `spring-boot-starter-redis` — deprecated).

If you get `ClassCastException` from Redis, ensure you're using `StringRedisTemplate` for string values, not the generic `RedisTemplate`.

---

## Pinecone Java SDK

The official Pinecone Java SDK (v2+) has a different API than older tutorials show. Use:

```xml
<dependency>
    <groupId>io.pinecone</groupId>
    <artifactId>pinecone-client</artifactId>
    <version>2.0.0</version>
</dependency>
```

Initialization:
```java
PineconeClient client = new PineconeClient(PineconeClientConfig.builder()
    .withApiKey(apiKey)
    .build());
```
