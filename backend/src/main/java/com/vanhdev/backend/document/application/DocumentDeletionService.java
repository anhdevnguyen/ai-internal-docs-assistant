package com.vanhdev.backend.document.application;

import com.vanhdev.backend.document.domain.Document;
import com.vanhdev.backend.document.infrastructure.DocumentJpaRepository;
import com.vanhdev.backend.document.infrastructure.VectorChunkRepository;
import com.vanhdev.backend.ingestion.infrastructure.storage.StoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Owns all destruction logic for a Document and its dependents.
 * Keeping this inside the document module is intentional — AdminDocumentService
 * should not reach into VectorChunkRepository or StoragePort directly, as those
 * are document-module internals. This service is the public API for deletion.
 * Deletion order: storage file → vector chunks → document record.
 * File is deleted first: if the DB commit fails afterward, we have a missing file
 * but a traceable DB record the operator can act on. The inverse (record deleted,
 * file survives) creates an untrackable storage leak — harder to recover from.
 */
@Service
public class DocumentDeletionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentDeletionService.class);

    private final DocumentJpaRepository documentRepository;
    private final VectorChunkRepository vectorChunkRepository;
    private final StoragePort storagePort;

    public DocumentDeletionService(DocumentJpaRepository documentRepository,
                                   VectorChunkRepository vectorChunkRepository,
                                   StoragePort storagePort) {
        this.documentRepository = documentRepository;
        this.vectorChunkRepository = vectorChunkRepository;
        this.storagePort = storagePort;
    }

    @Transactional
    public void hardDelete(Document document) {
        try {
            storagePort.delete(document.getStoragePath());
        } catch (Exception ex) {
            // A missing or already-deleted file must not block DB cleanup.
            // Storage may be inconsistent due to a prior partial failure.
            log.warn("Storage deletion failed [documentId={}], proceeding with DB cleanup: {}",
                    document.getId(), ex.getMessage());
        }

        vectorChunkRepository.deleteByDocumentId(document.getId());
        documentRepository.delete(document);
    }

    /**
     * Deletes only the vector chunks for a document, leaving the document record intact.
     * Used by force re-index to purge stale embeddings before re-running the pipeline.
     */
    @Transactional
    public void deleteChunks(UUID documentId) {
        vectorChunkRepository.deleteByDocumentId(documentId);
    }
}