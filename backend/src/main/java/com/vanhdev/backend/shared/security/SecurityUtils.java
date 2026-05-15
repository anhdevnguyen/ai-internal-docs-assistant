package com.vanhdev.backend.shared.security;

import com.vanhdev.backend.shared.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Returns the current authenticated principal.
     * Throws if the request is unauthenticated — use only inside endpoints
     * that require authentication (i.e. behind Spring Security's auth checks).
     */
    public static UserPrincipal requireAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw UnauthorizedException.invalidToken();
        }
        return principal;
    }

    /**
     * Returns the current principal without throwing — useful for optional auth scenarios
     * (e.g. public endpoints that behave differently for authenticated users).
     */
    public static java.util.Optional<UserPrincipal> getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(principal);
    }
}