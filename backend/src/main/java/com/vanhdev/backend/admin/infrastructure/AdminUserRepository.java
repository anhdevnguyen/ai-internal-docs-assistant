package com.vanhdev.backend.admin.infrastructure;

import com.vanhdev.backend.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<User, UUID> {

    Page<User> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Tenant-scoped lookup — prevents an admin of Tenant A from
     * accidentally (or maliciously) mutating a user in Tenant B via URL manipulation.
     */
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Direct update avoids loading the full entity just to flip a boolean.
     * @Modifying + @Transactional on the calling service handles the flush.
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = :active WHERE u.id = :userId AND u.tenantId = :tenantId")
    int setActiveByIdAndTenantId(@Param("userId") UUID userId,
                                 @Param("tenantId") UUID tenantId,
                                 @Param("active") boolean active);
}