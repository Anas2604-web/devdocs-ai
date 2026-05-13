package com.devdocsai.rag;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class RateLimitService {

    private static final int MAX_QUERIES_PER_HOUR = 60;
    private final StringRedisTemplate redis;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean isAllowed(String tenantId) {
        String key = "ratelimit:" + tenantId + ":hour:" + currentHour();
        Long count = redis.opsForValue().increment(key);
        if (count == 1) {
            redis.expire(key, Duration.ofHours(1));
        }
        return count <= MAX_QUERIES_PER_HOUR;
    }

    public long getRemaining(String tenantId) {
        String key = "ratelimit:" + tenantId + ":hour:" + currentHour();
        String val = redis.opsForValue().get(key);
        long used = val != null ? Long.parseLong(val) : 0;
        return Math.max(0, MAX_QUERIES_PER_HOUR - used);
    }

    private long currentHour() {
        return Instant.now().getEpochSecond() / 3600;
    }
}
