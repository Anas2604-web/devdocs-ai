package com.devdocsai.auth.dto;

import java.util.UUID;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UUID userId,
    UUID tenantId,
    String role
) {
    public static LoginResponse of(String token, long expiresIn, UUID userId, UUID tenantId, String role) {
        return new LoginResponse(token, "Bearer", expiresIn, userId, tenantId, role);
    }
}
