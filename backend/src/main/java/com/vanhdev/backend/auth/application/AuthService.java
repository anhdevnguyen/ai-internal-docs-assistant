package com.vanhdev.backend.auth.application;

import com.vanhdev.backend.auth.domain.Role;
import com.vanhdev.backend.auth.domain.User;
import com.vanhdev.backend.auth.infrastructure.TenantRepository;
import com.vanhdev.backend.auth.infrastructure.UserRepository;
import com.vanhdev.backend.shared.exception.ConflictException;
import com.vanhdev.backend.shared.exception.ResourceNotFoundException;
import com.vanhdev.backend.shared.exception.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final com.vanhdev.backend.auth.infrastructure.JwtProvider jwtProvider;

    public AuthService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            com.vanhdev.backend.auth.infrastructure.JwtProvider jwtProvider
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public AuthTokens register(UUID tenantId, String email, String rawPassword) {
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        if (userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new ConflictException("EMAIL_TAKEN", "Email is already registered in this organization");
        }

        User user = new User(
                tenantId,
                email.toLowerCase().strip(),
                passwordEncoder.encode(rawPassword),
                Role.USER
        );
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthTokens login(UUID tenantId, String email, String rawPassword) {
        User user = userRepository.findByTenantIdAndEmail(tenantId, email.toLowerCase().strip())
                // Use the same exception as wrong password — never hint which field was wrong
                .orElseThrow(UnauthorizedException::invalidCredentials);

        if (!user.isActive()) {
            // Still use the generic message — don't leak account status to attacker
            throw UnauthorizedException.invalidCredentials();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw UnauthorizedException.invalidCredentials();
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthTokens refresh(String rawRefreshToken) {
        // userId/tenantId come from the stored token — not from a (potentially expired) access token
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);

        User user = userRepository.findByIdAndTenantId(rotation.userId(), rotation.tenantId())
                .orElseThrow(UnauthorizedException::refreshTokenInvalid);

        if (!user.isActive()) {
            throw UnauthorizedException.refreshTokenInvalid();
        }

        String accessToken = jwtProvider.issueAccessToken(user.getId(), user.getTenantId(), user.getRole().name());
        return new AuthTokens(accessToken, rotation.newRawToken(), user);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthTokens issueTokens(User user) {
        String accessToken = jwtProvider.issueAccessToken(user.getId(), user.getTenantId(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user.getId(), user.getTenantId());
        return new AuthTokens(accessToken, refreshToken, user);
    }

    public record AuthTokens(String accessToken, String refreshToken, User user) {}
}