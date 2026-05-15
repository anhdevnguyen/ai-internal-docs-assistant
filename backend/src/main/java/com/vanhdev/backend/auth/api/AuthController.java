package com.vanhdev.backend.auth.api;

import com.vanhdev.backend.auth.api.dto.*;
import com.vanhdev.backend.auth.application.AuthService;
import com.vanhdev.backend.shared.api.ApiResponse;
import com.vanhdev.backend.shared.security.SecurityUtils;
import com.vanhdev.backend.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthTokens tokens = authService.login(
                request.tenantId(), request.email(), request.password()
        );
        return ApiResponse.ok(AuthResponse.from(tokens.accessToken(), tokens.refreshToken(), tokens.user()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        // Access token may be expired when client calls this — userId/tenantId come from the stored refresh token
        AuthService.AuthTokens tokens = authService.refresh(request.refreshToken());
        return ApiResponse.ok(AuthResponse.from(tokens.accessToken(), tokens.refreshToken(), tokens.user()));
    }

    @DeleteMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        UserPrincipal principal = SecurityUtils.requireAuthenticatedUser();
        authService.logout(principal.userId());
    }
}