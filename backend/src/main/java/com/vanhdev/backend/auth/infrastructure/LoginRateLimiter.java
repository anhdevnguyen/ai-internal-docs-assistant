package com.vanhdev.backend.auth.infrastructure;

import com.vanhdev.backend.shared.exception.ProviderRateLimitException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiter for authentication endpoints.
 * Design decisions:
 * - Keyed by IP address, not email — prevents username enumeration attacks where
 *   an attacker probes whether an email exists by observing lockout behavior per email.
 * - In-memory ConcurrentHashMap is sufficient for phase 1 (single instance).
 *   When horizontally scaled, migrate key storage to Redis using Bucket4j's
 *   ProxyManager with RedissonBasedProxyManager.
 * - Sliding window via Bucket4j's token bucket algorithm — fairer than fixed windows
 *   which can be gamed by bursting at window boundaries.
 * - Limit: 10 attempts per 15 minutes per IP. Conservative for legitimate users
 *   (a person mis-typing their password 10 times in 15 minutes is unusual),
 *   but blocks automated credential stuffing immediately.
 * - No cleanup of stale buckets: ConcurrentHashMap will grow unbounded under a
 *   DDoS with many source IPs. Acceptable for phase 1; add Caffeine eviction when needed.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS   = 10;
    private static final Duration WINDOW    = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Consumes one token for the given IP.
     * Throws ProviderRateLimitException (reused as 429 carrier) if the bucket is exhausted.
     */
    public void checkAndConsume(String clientIp) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::newBucket);

        if (!bucket.tryConsume(1)) {
            throw new LoginRateLimitException(
                    "Too many login attempts from this IP. Please try again in " + WINDOW.toMinutes() + " minutes.");
        }
    }

    private Bucket newBucket(String ignored) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(MAX_ATTEMPTS)
                        .refillGreedy(MAX_ATTEMPTS, WINDOW)
                        .build())
                .build();
    }

    /**
     * Domain-specific exception for login rate limit breaches.
     * Mapped to 429 Too Many Requests in GlobalExceptionHandler.
     */
    public static class LoginRateLimitException extends RuntimeException {
        public LoginRateLimitException(String message) {
            super(message);
        }
    }
}