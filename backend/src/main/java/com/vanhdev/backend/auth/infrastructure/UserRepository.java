package com.vanhdev.backend.auth.infrastructure;

import com.vanhdev.backend.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    // Used during session restoration after token rotation — avoids a separate tenant lookup
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}