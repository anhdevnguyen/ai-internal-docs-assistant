package com.vanhdev.backend.auth.api.dto;

import com.vanhdev.backend.auth.domain.User;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserInfo user
) {

    public static AuthResponse from(String accessToken, String refreshToken, User user) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                new UserInfo(user.getId(), user.getTenantId(), user.getEmail(), user.getRole().name())
        );
    }

    // Embedded projection — only the fields the client legitimately needs
    public record UserInfo(UUID id, UUID tenantId, String email, String role) {}
}