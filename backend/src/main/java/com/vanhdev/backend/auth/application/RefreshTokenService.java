package com.vanhdev.backend.auth.application;

import com.vanhdev.backend.auth.domain.RefreshToken;
import com.vanhdev.backend.auth.infrastructure.RefreshTokenRepository;
import com.vanhdev.backend.shared.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-ttl}") Duration refreshTokenTtl
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /**
     * Creates a new refresh token for the given user.
     * Returns the raw token to be sent to the client — only the hash is persisted.
     */
    @Transactional
    public String issue(UUID userId, UUID tenantId) {
        String rawToken = generateSecureToken();
        String tokenHash = hash(rawToken);

        RefreshToken entity = new RefreshToken(
                userId,
                tenantId,
                tokenHash,
                Instant.now().plus(refreshTokenTtl)
        );
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    /**
     * Validates and rotates a refresh token.
     * Rotation invalidates the presented token and issues a fresh one —
     * this limits the window of exploitation if a token is stolen.
     * userId/tenantId are read from the stored token, not trusted from the caller.
     */
    @Transactional
    public RotationResult rotate(String rawToken) {
        String tokenHash = hash(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(UnauthorizedException::refreshTokenInvalid);

        if (!stored.isValid()) {
            throw UnauthorizedException.refreshTokenInvalid();
        }

        UUID userId = stored.getUserId();
        UUID tenantId = stored.getTenantId();

        stored.revoke();
        refreshTokenRepository.save(stored);

        String newRawToken = issue(userId, tenantId);
        return new RotationResult(userId, tenantId, newRawToken);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in any JVM
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RotationResult(UUID userId, UUID tenantId, String newRawToken) {}
}