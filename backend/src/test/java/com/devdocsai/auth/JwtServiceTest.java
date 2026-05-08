package com.devdocsai.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
            "test-secret-that-is-long-enough-for-hmac-sha256-algorithm-minimum");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiry", 900000L);
    }

    @Test
    void shouldGenerateValidToken() {
        String token = jwtService.generateAccessToken("user-1", "tenant-1", "ADMIN");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldValidateGeneratedToken() {
        String token = jwtService.generateAccessToken("user-1", "tenant-1", "ADMIN");
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void shouldExtractCorrectUserId() {
        String token = jwtService.generateAccessToken("user-123", "tenant-456", "MEMBER");
        assertEquals("user-123", jwtService.extractUserId(token));
    }

    @Test
    void shouldExtractCorrectTenantId() {
        String token = jwtService.generateAccessToken("user-123", "tenant-456", "MEMBER");
        assertEquals("tenant-456", jwtService.extractTenantId(token));
    }

    @Test
    void shouldExtractCorrectRole() {
        String token = jwtService.generateAccessToken("user-1", "tenant-1", "ADMIN");
        assertEquals("ADMIN", jwtService.extractRole(token));
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateAccessToken("user-1", "tenant-1", "ADMIN");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertFalse(jwtService.validateToken(tampered));
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiry", 1L);
        String token = jwtService.generateAccessToken("user-1", "tenant-1", "ADMIN");
        Thread.sleep(50);
        assertFalse(jwtService.validateToken(token));
    }

    @Test
    void shouldRejectNullToken() {
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    void shouldRejectEmptyToken() {
        assertFalse(jwtService.validateToken(""));
    }
}
