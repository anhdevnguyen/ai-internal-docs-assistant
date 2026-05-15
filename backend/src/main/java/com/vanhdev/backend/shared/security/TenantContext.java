package com.vanhdev.backend.shared.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local carrier for the current request's tenant identity.
 * Set once by JwtAuthenticationFilter at the start of every authenticated request.
 * Cleared in the same filter's finally block — thread pools reuse threads,
 * so forgetting to clear causes tenant data to bleed across unrelated requests.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * Returns the tenant ID, or throws if none is set.
     * Use this inside service/repository code that must always run within a tenant scope.
     */
    public static UUID requireTenantId() {
        UUID tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context available on current thread — " +
                    "ensure request passed through JwtAuthenticationFilter");
        }
        return tenantId;
    }

    public static Optional<UUID> getTenantId() {
        return Optional.ofNullable(TENANT_ID.get());
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}