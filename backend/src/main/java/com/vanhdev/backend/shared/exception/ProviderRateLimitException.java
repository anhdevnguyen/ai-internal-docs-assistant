package com.vanhdev.backend.shared.exception;

/**
 * Thrown when the upstream AI provider (OpenAI/Gemini) responds with HTTP 429.
 * Translated to 503 Service Unavailable at the API boundary — the client knows
 * to retry later, without leaking provider-specific details.
 */
public class ProviderRateLimitException extends RuntimeException {

    public ProviderRateLimitException(String provider) {
        super(provider + " rate limit exceeded — retry after a short delay");
    }

    public ProviderRateLimitException(String provider, Throwable cause) {
        super(provider + " rate limit exceeded — retry after a short delay", cause);
    }
}