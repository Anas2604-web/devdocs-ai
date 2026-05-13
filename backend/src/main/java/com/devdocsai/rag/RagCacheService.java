package com.devdocsai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;

@Service
public class RagCacheService {

    private static final Logger log = LoggerFactory.getLogger(RagCacheService.class);
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redis;

    public RagCacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Optional<String> get(String tenantId, String question) {
        try {
            String key = buildKey(tenantId, question);
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache HIT for tenant {}", tenantId);
                return Optional.of(cached);
            }
            log.debug("Cache MISS for tenant {}", tenantId);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Cache get failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String tenantId, String question, String answer) {
        try {
            String key = buildKey(tenantId, question);
            redis.opsForValue().set(key, answer, TTL);
        } catch (Exception e) {
            log.warn("Cache put failed: {}", e.getMessage());
        }
    }

    public void invalidateForTenant(String tenantId) {
        // Called when tenant uploads a new spec
        try {
            var keys = redis.keys("rag:cache:" + tenantId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
                log.info("Invalidated {} cache entries for tenant {}", keys.size(), tenantId);
            }
        } catch (Exception e) {
            log.warn("Cache invalidation failed: {}", e.getMessage());
        }
    }

    private String buildKey(String tenantId, String question) {
        String hash = sha256(question.toLowerCase().trim());
        return "rag:cache:" + tenantId + ":" + hash;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString().substring(0, 16); // first 16 chars is enough
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
