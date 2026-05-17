package com.vanhdev.backend.admin.infrastructure;

import com.vanhdev.backend.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<User, UUID> {

    Page<User> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Tenant-scoped lookup — prevents an admin of Tenant A from
     * mutating a user in Tenant B via URL manipulation.
     */
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);
}