package com.vanhdev.backend.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(

        @NotBlank(message = "refreshToken is required")
        String refreshToken
) {}