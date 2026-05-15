package com.vanhdev.backend.auth.infrastructure;

import com.vanhdev.backend.shared.exception.UnauthorizedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;

    public JwtProvider(
            @Value("${app.jwt.access-token-secret}") String base64Secret,
            @Value("${app.jwt.access-token-ttl}") Duration accessTokenTtl
    ) {
        // Keys.hmacShaKeyFor validates the key is strong enough for HS256
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessTokenTtl = accessTokenTtl;
    }

    public String issueAccessToken(UUID userId, UUID tenantId, String role) {
        long nowMs = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TENANT_ID, tenantId.toString())
                .claim(CLAIM_ROLE, role)
                .issuedAt(new Date(nowMs))
                .expiration(new Date(nowMs + accessTokenTtl.toMillis()))
                .signWith(signingKey)
                .compact();
    }

    public ParsedClaims validateAndParse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new ParsedClaims(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class)),
                    claims.get(CLAIM_ROLE, String.class)
            );
        } catch (ExpiredJwtException e) {
            throw UnauthorizedException.tokenExpired();
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw UnauthorizedException.invalidToken();
        }
    }

    public record ParsedClaims(UUID userId, UUID tenantId, String role) {}
}