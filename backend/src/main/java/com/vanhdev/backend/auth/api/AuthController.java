package com.vanhdev.backend.auth.api;

import com.vanhdev.backend.auth.api.dto.*;
import com.vanhdev.backend.auth.application.AuthService;
import com.vanhdev.backend.auth.infrastructure.LoginRateLimiter;
import com.vanhdev.backend.shared.api.ApiResponse;
import com.vanhdev.backend.shared.security.SecurityUtils;
import com.vanhdev.backend.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthTokens tokens = authService.register(
                request.tenantId(), request.email(), request.password()
        );
        return ApiResponse.ok(AuthResponse.from(tokens.accessToken(), tokens.refreshToken(), tokens.user()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        // Rate-limit before any credential validation — prevents brute force and
        // credential stuffing. Keyed by IP so attackers can't rotate emails to bypass.
        loginRateLimiter.checkAndConsume(resolveClientIp(httpRequest));

        AuthService.AuthTokens tokens = authService.login(
                request.tenantId(), request.email(), request.password()
        );
        return ApiResponse.ok(AuthResponse.from(tokens.accessToken(), tokens.refreshToken(), tokens.user()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        AuthService.AuthTokens tokens = authService.refresh(request.refreshToken());
        return ApiResponse.ok(AuthResponse.from(tokens.accessToken(), tokens.refreshToken(), tokens.user()));
    }

    @DeleteMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        UserPrincipal principal = SecurityUtils.requireAuthenticatedUser();
        authService.logout(principal.userId());
    }

    /**
     * Resolves the real client IP, respecting X-Forwarded-For set by trusted reverse proxies.
     * Only reads the first (leftmost) IP in the header chain — rightmost entries are
     * appended by each proxy and may include internal hop addresses, not the client origin.
     * Note: if this service is ever exposed without a trusted reverse proxy, X-Forwarded-For
     * becomes spoofable. Restrict this header to be settable only by the proxy at the
     * infrastructure level (Nginx: proxy_set_header X-Forwarded-For $remote_addr).
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}