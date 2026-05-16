package com.vanhdev.backend.ingestion.application;

import com.vanhdev.backend.document.domain.Document;
import com.vanhdev.backend.document.domain.DocumentStatus;
import com.vanhdev.backend.document.infrastructure.DocumentJpaRepository;
import com.vanhdev.backend.document.infrastructure.VectorChunkRepository;
import com.vanhdev.backend.ingestion.domain.TextChunk;
import com.vanhdev.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Isolated transactional operations for the ingestion pipeline.
 * Extracted into a separate bean to avoid Spring AOP self-invocation — calling
 * @Transactional methods on 'this' within the same class bypasses the proxy.
 */
@Service
class IngestionTransactions {

    private final DocumentJpaRepository documentRepository;
    private final VectorChunkRepository vectorChunkRepository;

    IngestionTransactions(DocumentJpaRepository documentRepository,
                          VectorChunkRepository vectorChunkRepository) {
        this.documentRepository = documentRepository;
        this.vectorChunkRepository = vectorChunkRepository;
    }

    @Transactional
    public void updateStatus(UUID documentId, DocumentStatus status, String errorMessage) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        if (status == DocumentStatus.FAILED) {
            doc.markAsFailed(errorMessage);
        } else if (status == DocumentStatus.PROCESSING) {
            doc.markAsProcessing();
        }
        documentRepository.save(doc);
    }

    @Transactional
    public void persistChunksAndMarkIndexed(UUID documentId, UUID tenantId,
                                             List<TextChunk> chunks, List<float[]> embeddings) {
        vectorChunkRepository.saveAll(tenantId, documentId, chunks, embeddings);
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        doc.markAsIndexed();
        documentRepository.save(doc);
    }
}
