package com.vanhdev.backend.document.infrastructure;

import com.vanhdev.backend.document.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentJpaRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Document> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}
