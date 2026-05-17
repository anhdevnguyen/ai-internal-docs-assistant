package com.vanhdev.backend.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Runs before every request (including unauthenticated ones) to establish a
 * correlation context in SLF4J MDC. All log statements downstream automatically
 * inherit [requestId][tenantId][userId] without any caller involvement.
 * Filter ordering: must run AFTER JwtAuthenticationFilter so SecurityContext
 * and TenantContext are already populated when this filter reads them.
 * @Order(2) achieves this — JwtAuthenticationFilter is registered at @Order(1)
 * via SecurityFilterChain ordering.
 * Why a separate filter instead of enriching JwtAuthenticationFilter:
 * JwtAuthenticationFilter is a Spring Security filter registered inside the
 * security filter chain. This filter is a servlet filter registered in the
 * standard filter chain, giving us hook points for both pre- and post-processing
 * without coupling MDC lifecycle to security concerns.
 */
@Component
@Order(2)
public class MdcRequestFilter extends OncePerRequestFilter {

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_TENANT_ID  = "tenantId";
    private static final String MDC_USER_ID    = "userId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            MDC.put(MDC_REQUEST_ID, UUID.randomUUID().toString());

            // TenantContext and SecurityContext are populated by JwtAuthenticationFilter
            // before this filter runs (security filter chain executes first in Spring Boot).
            TenantContext.getTenantId().ifPresent(id -> MDC.put(MDC_TENANT_ID, id.toString()));

            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
                MDC.put(MDC_USER_ID, principal.userId().toString());
            }

            // Propagate the requestId to the response so clients can correlate errors
            response.setHeader("X-Request-Id", MDC.get(MDC_REQUEST_ID));

            filterChain.doFilter(request, response);

        } finally {
            // MDC is thread-local — must clear regardless of outcome to prevent
            // context bleed across requests in pooled threads
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
        }
    }
}