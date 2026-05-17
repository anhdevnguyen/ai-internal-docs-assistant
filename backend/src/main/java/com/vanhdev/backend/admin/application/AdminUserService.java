package com.vanhdev.backend.admin.application;

import com.vanhdev.backend.admin.api.dto.AdminDtos.AdminUserResponse;
import com.vanhdev.backend.admin.api.dto.AdminDtos.ToggleUserActiveResponse;
import com.vanhdev.backend.admin.infrastructure.AdminUserRepository;
import com.vanhdev.backend.shared.api.PagedResponse;
import com.vanhdev.backend.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;

    public AdminUserService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> listUsers(UUID tenantId, Pageable pageable) {
        return PagedResponse.from(
                adminUserRepository.findByTenantId(tenantId, pageable).map(this::toResponse)
        );
    }

    /**
     * Toggles the active state of a user.
     * Uses a direct UPDATE query (not load-then-save) for two reasons:
     * 1. Avoids an unnecessary SELECT for a single boolean flip.
     * 2. The WHERE clause includes tenant_id — this is the authorization check.
     *    If a malicious admin from Tenant A sends Tenant B's userId, the query
     *    matches 0 rows, and we throw ResourceNotFoundException, leaking nothing.
     */
    @Transactional
    public ToggleUserActiveResponse setUserActive(UUID userId, UUID tenantId, boolean active) {
        int updated = adminUserRepository.setActiveByIdAndTenantId(userId, tenantId, active);
        if (updated == 0) {
            throw new ResourceNotFoundException("USER_NOT_FOUND", "User not found: " + userId);
        }
        return new ToggleUserActiveResponse(userId, active);
    }

    private AdminUserResponse toResponse(com.vanhdev.backend.auth.domain.User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}