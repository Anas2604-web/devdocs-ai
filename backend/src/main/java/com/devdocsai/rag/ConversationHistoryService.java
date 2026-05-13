package com.devdocsai.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationHistoryService.class);
    private static final int MAX_HISTORY = 10;
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ConversationHistoryService(StringRedisTemplate redis) {
        this.redis = redis;
        this.objectMapper = new ObjectMapper();
    }

    public record Message(String role, String content) {}

    public List<Message> getHistory(String sessionId) {
        try {
            String key = "session:" + sessionId + ":history";
            String json = redis.opsForValue().get(key);
            if (json == null) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<List<Message>>() {});
        } catch (Exception e) {
            log.warn("Failed to get history for session {}: {}", sessionId, e.getMessage());
            return new ArrayList<>();
        }
    }

    public void addMessage(String sessionId, String role, String content) {
        try {
            String key = "session:" + sessionId + ":history";
            List<Message> history = getHistory(sessionId);
            history.add(new Message(role, content));
            // Keep only last MAX_HISTORY messages
            if (history.size() > MAX_HISTORY) {
                history = history.subList(history.size() - MAX_HISTORY, history.size());
            }
            redis.opsForValue().set(key, objectMapper.writeValueAsString(history), TTL);
        } catch (Exception e) {
            log.warn("Failed to save message for session {}: {}", sessionId, e.getMessage());
        }
    }

    public void clearHistory(String sessionId) {
        redis.delete("session:" + sessionId + ":history");
    }
}
