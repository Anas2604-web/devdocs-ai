package com.devdocsai.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, ErrorDetail error) {

    public record ErrorDetail(String code, String message, String requestId, Instant timestamp) {}

    public static ErrorResponse of(String code, String message, HttpServletRequest request) {
        return new ErrorResponse(false,
            new ErrorDetail(code, message, UUID.randomUUID().toString(), Instant.now()));
    }
}
