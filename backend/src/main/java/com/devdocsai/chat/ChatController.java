package com.devdocsai.chat;

import com.devdocsai.common.ApiResponse;
import com.devdocsai.common.DevDocsException;
import com.devdocsai.rag.ConversationHistoryService;
import com.devdocsai.rag.RagService;
import com.devdocsai.rag.RateLimitService;
import com.devdocsai.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagService ragService;
    private final RateLimitService rateLimitService;
    private final ConversationHistoryService historyService;
    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    public ChatController(RagService ragService, RateLimitService rateLimitService,
                          ConversationHistoryService historyService) {
        this.ragService = ragService;
        this.rateLimitService = rateLimitService;
        this.historyService = historyService;
    }

    /**
     * Start a new chat session — returns a sessionId.
     * Frontend stores this and sends it with every message.
     */
    @PostMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, String>>> startSession() {
        String sessionId = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("sessionId", sessionId)));
    }

    /**
     * SSE streaming endpoint — streams RAG answer token by token.
     * Frontend connects with EventSource and receives tokens as they arrive.
     */
    @GetMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(
            @RequestParam String question,
            @RequestParam String sessionId) {

        String tenantId = TenantContext.getCurrentTenant();

        // Check rate limit
        if (!rateLimitService.isAllowed(tenantId)) {
            long remaining = rateLimitService.getRemaining(tenantId);
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error")
                    .data("Rate limit exceeded. " + remaining + " queries remaining this hour."));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        // Validate
        if (question == null || question.isBlank()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("Question cannot be empty."));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        // Create SSE emitter with 2-minute timeout
        SseEmitter emitter = new SseEmitter(120_000L);

        // Run in background thread so HTTP response returns immediately
        // We capture tenantId here because ThreadLocal won't carry to new thread
        String capturedTenantId = tenantId;
        executor.submit(() -> {
            TenantContext.setCurrentTenant(capturedTenantId);
            try {
                ragService.streamAnswer(question, capturedTenantId, sessionId, emitter);
            } finally {
                TenantContext.clear();
            }
        });

        return emitter;
    }

    /**
     * Clear conversation history for a session.
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> clearSession(@PathVariable String sessionId) {
        historyService.clearHistory(sessionId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
