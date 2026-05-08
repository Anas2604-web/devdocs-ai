# Testing guide — DevDocs AI

This project follows a **three-layer testing strategy**: unit tests, integration tests, and end-to-end tests. You should write tests as you build each feature — not after.

---

## Testing philosophy

```
         ▲
         │  E2E tests (few, slow, test user flows)
         │  Integration tests (medium, test real DB/Redis)
         │  Unit tests (many, fast, test logic in isolation)
         ▼
```

- **Unit tests** — test one class in isolation, mock all dependencies. Fast (<100ms each).
- **Integration tests** — test a full slice (controller → service → real DB). Use Testcontainers.
- **End-to-end tests** — test full user flows via the real running app. Use Playwright.

---

## Dependencies to add to `pom.xml`

```xml
<!-- Testing framework -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers — real Postgres + Redis in tests -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mocking -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Unit tests — what to test and how

### JwtServiceTest.java

```java
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // inject test secret via reflection or @TestPropertySource
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "test-secret-256-bits-long-enough-here");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiry", 900000L);
    }

    @Test
    void shouldGenerateValidAccessToken() {
        String token = jwtService.generateAccessToken("user-id-123", "tenant-id-456", "ADMIN");
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void shouldExtractCorrectClaims() {
        String token = jwtService.generateAccessToken("user-123", "tenant-456", "MEMBER");
        assertEquals("user-123", jwtService.extractUserId(token));
        assertEquals("tenant-456", jwtService.extractTenantId(token));
        assertEquals("MEMBER", jwtService.extractRole(token));
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiry", 1L); // 1ms
        String token = jwtService.generateAccessToken("user-123", "tenant-456", "ADMIN");
        Thread.sleep(10);
        assertFalse(jwtService.validateToken(token));
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateAccessToken("user-123", "tenant-456", "ADMIN");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtService.validateToken(tampered));
    }
}
```

### TenantContextTest.java

```java
class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear(); // always clean up ThreadLocal
    }

    @Test
    void shouldStoreAndRetrieveTenantId() {
        TenantContext.setCurrentTenant("tenant-123");
        assertEquals("tenant-123", TenantContext.getCurrentTenant());
    }

    @Test
    void shouldReturnNullWhenNotSet() {
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void shouldIsolateBetweenThreads() throws InterruptedException {
        TenantContext.setCurrentTenant("tenant-main");
        AtomicReference<String> childTenantId = new AtomicReference<>();

        Thread child = new Thread(() -> {
            // child thread has its own ThreadLocal — should be null
            childTenantId.set(TenantContext.getCurrentTenant());
        });
        child.start();
        child.join();

        assertNull(childTenantId.get()); // isolation confirmed
        assertEquals("tenant-main", TenantContext.getCurrentTenant()); // main unaffected
    }
}
```

### ChunkingServiceTest.java

```java
@ExtendWith(MockitoExtension.class)
class ChunkingServiceTest {

    @InjectMocks
    private ChunkingService chunkingService;

    @Test
    void shouldCreateOneChunkPerEndpoint() throws Exception {
        String spec = loadTestSpec("petstore.yaml"); // load from test/resources
        List<ApiChunk> chunks = chunkingService.parseAndChunk(spec);

        // PetStore has 3 endpoints — verify correct count
        assertEquals(3, chunks.size());
    }

    @Test
    void shouldExtractMethodAndPath() throws Exception {
        String spec = loadTestSpec("simple-spec.yaml");
        List<ApiChunk> chunks = chunkingService.parseAndChunk(spec);

        ApiChunk getUsers = chunks.stream()
            .filter(c -> c.getEndpointPath().equals("/users"))
            .findFirst().orElseThrow();

        assertEquals("GET", getUsers.getEndpointMethod());
        assertTrue(getUsers.getChunkText().contains("/users"));
    }

    @Test
    void shouldHandleEmptySpecGracefully() {
        assertThrows(InvalidSpecException.class,
            () -> chunkingService.parseAndChunk(""));
    }

    private String loadTestSpec(String filename) throws IOException {
        return Files.readString(
            Path.of("src/test/resources/specs/" + filename)
        );
    }
}
```

### RagCacheServiceTest.java

```java
@ExtendWith(MockitoExtension.class)
class RagCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private RagCacheService cacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void shouldReturnCachedAnswerOnHit() {
        when(valueOps.get("rag:cache:tenant-123:abc123")).thenReturn("cached answer");
        Optional<String> result = cacheService.get("tenant-123", "abc123");
        assertTrue(result.isPresent());
        assertEquals("cached answer", result.get());
    }

    @Test
    void shouldReturnEmptyOnCacheMiss() {
        when(valueOps.get(anyString())).thenReturn(null);
        Optional<String> result = cacheService.get("tenant-123", "xyz");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldStoreWithCorrectTtl() {
        cacheService.put("tenant-123", "hash456", "answer text");
        verify(valueOps).set(
            eq("rag:cache:tenant-123:hash456"),
            eq("answer text"),
            eq(1L),
            eq(TimeUnit.HOURS)
        );
    }
}
```

---

## Integration tests — full slice with Testcontainers

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class AuthControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("devdocsai_test")
        .withUsername("postgres")
        .withPassword("postgres");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRegisterAndLoginSuccessfully() {
        // Register
        RegisterRequest registerReq = new RegisterRequest("TestCo", "test@testco.com", "Pass@1234");
        ResponseEntity<Void> registerResp = restTemplate.postForEntity(
            "/api/auth/register", registerReq, Void.class);
        assertEquals(201, registerResp.getStatusCode().value());

        // Login
        LoginRequest loginReq = new LoginRequest("test@testco.com", "Pass@1234");
        ResponseEntity<LoginResponse> loginResp = restTemplate.postForEntity(
            "/api/auth/login", loginReq, LoginResponse.class);

        assertEquals(200, loginResp.getStatusCode().value());
        assertNotNull(loginResp.getBody().getAccessToken());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        RegisterRequest req = new RegisterRequest("TestCo2", "dup@test.com", "Pass@1234");
        restTemplate.postForEntity("/api/auth/register", req, Void.class);

        // Second registration with same email
        ResponseEntity<ErrorResponse> resp = restTemplate.postForEntity(
            "/api/auth/register", req, ErrorResponse.class);

        assertEquals(409, resp.getStatusCode().value());
        assertEquals("EMAIL_ALREADY_EXISTS", resp.getBody().getCode());
    }

    @Test
    void shouldRejectRequestsWithoutToken() {
        ResponseEntity<Void> resp = restTemplate.getForEntity("/api/specs", Void.class);
        assertEquals(401, resp.getStatusCode().value());
    }
}
```

---

## Running tests

```bash
# All tests
./mvnw test

# Only unit tests (fast — no Docker needed)
./mvnw test -Dgroups="unit"

# Only integration tests (requires Docker)
./mvnw test -Dgroups="integration"

# Specific test class
./mvnw test -Dtest=JwtServiceTest

# With coverage report
./mvnw test jacoco:report
# Report at: target/site/jacoco/index.html

# Skip tests (for fast builds)
./mvnw package -DskipTests
```

---

## What to test (priority order)

Build these tests as you build the features:

### Must have (Week 1)
- [ ] `JwtServiceTest` — token generation, validation, expiry, tamper detection
- [ ] `TenantContextTest` — ThreadLocal isolation
- [ ] `AuthControllerIT` — register, login, refresh, logout flows
- [ ] `PasswordValidationTest` — strong password rules

### Must have (Week 2)
- [ ] `ChunkingServiceTest` — parse spec, correct chunk count, handle bad input
- [ ] `IngestionServiceTest` — status transitions (PENDING → PROCESSING → READY)
- [ ] `S3ServiceTest` — mock AWS SDK, verify correct bucket/key
- [ ] `SpecControllerIT` — upload, list, delete specs

### Must have (Week 3)
- [ ] `RagCacheServiceTest` — cache hit, miss, TTL, invalidation
- [ ] `RateLimitServiceTest` — allows 60/hr, blocks at 61, resets after window
- [ ] `ToolExecutorTest` — each tool returns expected structure
- [ ] `TenantIsolationIT` — verify tenant A cannot access tenant B's data

### Nice to have (Week 4+)
- [ ] `MetricsTest` — verify Prometheus counters increment
- [ ] `ErrorHandlingIT` — all error codes return correct HTTP status
- [ ] Playwright E2E — register → upload spec → ask question → get answer

---

## Coverage targets

| Package | Target |
|---|---|
| `auth` | 90%+ (security is critical) |
| `rag` | 80%+ (core business logic) |
| `ingestion` | 80%+ |
| `tenant` | 85%+ |
| `common` | 70%+ |

Run coverage report and check with:
```bash
./mvnw test jacoco:report
open target/site/jacoco/index.html
```
