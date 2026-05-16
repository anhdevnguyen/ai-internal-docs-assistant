package com.vanhdev.backend.document.application;

import com.vanhdev.backend.document.domain.Document;
import com.vanhdev.backend.document.infrastructure.DocumentJpaRepository;
import com.vanhdev.backend.shared.exception.ResourceNotFoundException;
import com.vanhdev.backend.shared.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Owns all read-side queries for the document module.
 * Tenant scoping is enforced here — not in the controller — so that any
 * future caller (scheduled job, admin endpoint, internal service) that goes
 * through this service gets the same isolation guarantee automatically.
 * Controllers must never call DocumentJpaRepository directly.
 */
@Service
@Transactional(readOnly = true)
public class DocumentQueryService {

    private final DocumentJpaRepository documentRepository;

    public DocumentQueryService(DocumentJpaRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Page<Document> listForCurrentTenant(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return documentRepository.findByTenantId(tenantId, pageable);
    }

    public Document getByIdForCurrentTenant(UUID documentId) {
        UUID tenantId = TenantContext.requireTenantId();
        return documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: ", documentId));
    }

    public void verifyOwnership(UUID documentId) {
        UUID tenantId = TenantContext.requireTenantId();
        if (!documentRepository.existsByIdAndTenantId(documentId, tenantId)) {
            throw new ResourceNotFoundException("Document not found: ", documentId);
        }
    }
}