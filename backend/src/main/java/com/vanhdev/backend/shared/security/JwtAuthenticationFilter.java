package com.vanhdev.backend.shared.security;

import com.vanhdev.backend.auth.infrastructure.JwtProvider;
import com.vanhdev.backend.shared.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(BEARER_PREFIX.length());
            JwtProvider.ParsedClaims claims = jwtProvider.validateAndParse(token);

            SecurityContextHolder.getContext().setAuthentication(buildAuthentication(claims));

            // Set TenantContext — enforced at repository layer for data isolation
            TenantContext.setTenantId(claims.tenantId());

            filterChain.doFilter(request, response);

        } catch (UnauthorizedException e) {
            // Let the request proceed without authentication; Spring Security will reject
            // it if the endpoint requires auth — we don't short-circuit here to keep
            // the security decision in SecurityConfig, not scattered across filters.
            filterChain.doFilter(request, response);
        } finally {
            // Thread pool reuse means we MUST clear ThreadLocal state after every request
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private static UsernamePasswordAuthenticationToken buildAuthentication(JwtProvider.ParsedClaims claims) {
        UserPrincipal principal = new UserPrincipal(
                claims.userId(),
                claims.tenantId(),
                claims.role()
        );
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
        );
    }
}