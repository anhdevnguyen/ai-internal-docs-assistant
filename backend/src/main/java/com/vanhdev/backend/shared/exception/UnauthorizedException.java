package com.vanhdev.backend.shared.exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {

    private final String errorCode;

    private UnauthorizedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    // Static factories keep error codes consistent across all call sites
    // and prevent callers from inventing ad-hoc codes

    public static UnauthorizedException invalidCredentials() {
        // Deliberately vague — never reveal whether email or password was wrong
        return new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password");
    }

    public static UnauthorizedException tokenExpired() {
        return new UnauthorizedException("TOKEN_EXPIRED", "Access token has expired");
    }

    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException("INVALID_TOKEN", "Access token is invalid");
    }

    public static UnauthorizedException refreshTokenInvalid() {
        return new UnauthorizedException("REFRESH_TOKEN_INVALID", "Refresh token is invalid or has been revoked");
    }
}