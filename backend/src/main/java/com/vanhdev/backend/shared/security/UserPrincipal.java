package com.vanhdev.backend.shared.security;

import java.util.UUID;

/**
 * Immutable identity object stored in Spring Security context for the duration of a request.
 * Contains only the data encoded in the JWT — no DB round-trips needed for auth checks.
 */
public record UserPrincipal(UUID userId, UUID tenantId, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean belongsToTenant(UUID targetTenantId) {
        return this.tenantId.equals(targetTenantId);
    }
}