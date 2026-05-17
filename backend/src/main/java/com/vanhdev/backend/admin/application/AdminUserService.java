package com.vanhdev.backend.admin.application;

import com.vanhdev.backend.admin.api.dto.AdminDtos.AdminUserResponse;
import com.vanhdev.backend.admin.api.dto.AdminDtos.ToggleUserActiveResponse;
import com.vanhdev.backend.admin.infrastructure.AdminUserRepository;
import com.vanhdev.backend.auth.domain.User;
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
                adminUserRepository.findByTenantId(tenantId, pageable),
                this::toResponse
        );
    }

    /**
     * Uses domain methods activate()/deactivate() rather than a direct UPDATE query.
     * Reason: User.updatedAt must be kept consistent, and domain methods own that invariant.
     * The tenant-scoped lookup is the authorization check — if a malicious admin sends
     * another tenant's userId, findByIdAndTenantId returns empty → 404, leaking nothing.
     */
    @Transactional
    public ToggleUserActiveResponse setUserActive(UUID userId, UUID tenantId, boolean active) {
        User user = adminUserRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found: " + userId));

        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }

        return new ToggleUserActiveResponse(userId, user.isActive());
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}