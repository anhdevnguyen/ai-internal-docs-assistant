package com.vanhdev.backend.admin.application;

import com.vanhdev.backend.admin.api.dto.AdminDtos.AdminDocumentResponse;
import com.vanhdev.backend.document.application.DocumentDeletionService;
import com.vanhdev.backend.document.domain.Document;
import com.vanhdev.backend.document.domain.DocumentStatus;
import com.vanhdev.backend.document.infrastructure.DocumentJpaRepository;
import com.vanhdev.backend.ingestion.application.DocumentIngestionPipeline;
import com.vanhdev.backend.shared.api.PagedResponse;
import com.vanhdev.backend.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminDocumentService {

    private static final Logger log = LoggerFactory.getLogger(AdminDocumentService.class);

    private final DocumentJpaRepository documentRepository;
    private final DocumentDeletionService documentDeletionService;
    private final DocumentIngestionPipeline ingestionPipeline;

    public AdminDocumentService(DocumentJpaRepository documentRepository,
                                DocumentDeletionService documentDeletionService,
                                DocumentIngestionPipeline ingestionPipeline) {
        this.documentRepository = documentRepository;
        this.documentDeletionService = documentDeletionService;
        this.ingestionPipeline = ingestionPipeline;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminDocumentResponse> listDocuments(UUID tenantId, Pageable pageable) {
        return PagedResponse.from(
                documentRepository.findByTenantId(tenantId, pageable),
                this::toResponse
        );
    }

    /**
     * Hard delete in dependency order: storage file → vector chunks → document record.
     * Delegation to DocumentDeletionService keeps storage and chunk cleanup
     * inside the document module — admin module has no business knowing those internals.
     */
    @Transactional
    public void deleteDocument(UUID documentId, UUID tenantId) {
        Document document = requireDocument(documentId, tenantId);
        documentDeletionService.hardDelete(document);
        log.info("Document hard-deleted by admin [id={}, tenantId={}]", documentId, tenantId);
    }

    /**
     * Resets a FAILED document back to PENDING and fires the ingestion pipeline.
     * Only allowed for FAILED documents — re-triggering INDEXED or PROCESSING
     * would cause duplicate chunk insertion.
     */
    @Transactional
    public AdminDocumentResponse forceReindex(UUID documentId, UUID tenantId) {
        Document document = requireDocument(documentId, tenantId);

        if (document.getStatus() != DocumentStatus.FAILED) {
            throw new IllegalStateException(
                    "Force re-index is only allowed for FAILED documents. Current status: " + document.getStatus());
        }

        // Purge stale chunks before re-ingesting to prevent duplicate embeddings
        documentDeletionService.deleteChunks(documentId);

        document.resetToPending();
        documentRepository.save(document);

        // Fire async — does not block this HTTP thread
        ingestionPipeline.process(tenantId, documentId, document.getStoragePath());

        log.info("Force re-index triggered [documentId={}, tenantId={}]", documentId, tenantId);
        return toResponse(document);
    }

    private Document requireDocument(UUID documentId, UUID tenantId) {
        return documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DOCUMENT_NOT_FOUND", "Document not found: " + documentId));
    }

    private AdminDocumentResponse toResponse(Document doc) {
        return new AdminDocumentResponse(
                doc.getId(),
                doc.getUploadedBy(),
                doc.getTitle(),
                doc.getOriginalFilename(),
                doc.getMimeType(),
                doc.getStatus(),
                doc.getErrorMessage(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}