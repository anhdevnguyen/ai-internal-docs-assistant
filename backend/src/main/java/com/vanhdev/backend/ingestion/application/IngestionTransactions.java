package com.vanhdev.backend.ingestion.application;

import com.vanhdev.backend.document.domain.Document;
import com.vanhdev.backend.document.infrastructure.DocumentJpaRepository;
import com.vanhdev.backend.document.infrastructure.VectorChunkRepository;
import com.vanhdev.backend.ingestion.domain.TextChunk;
import com.vanhdev.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Isolates each pipeline stage into its own short transaction.
 * This prevents a single open DB connection from being held across
 * seconds-long network calls (Tika parsing, OpenAI API) which would
 * exhaust the connection pool under concurrent load.
 * The pipeline itself is NOT @Transactional — it orchestrates these
 * fine-grained commits sequentially.
 */
@Component
public class IngestionTransactions {

    private final DocumentJpaRepository documentRepository;
    private final VectorChunkRepository vectorChunkRepository;

    public IngestionTransactions(DocumentJpaRepository documentRepository,
                                 VectorChunkRepository vectorChunkRepository) {
        this.documentRepository = documentRepository;
        this.vectorChunkRepository = vectorChunkRepository;
    }

    @Transactional
    public Document markProcessing(UUID tenantId, UUID documentId) {
        Document doc = documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: ", documentId));
        doc.markAsProcessing();
        return documentRepository.save(doc);
    }

    @Transactional
    public void persistChunksAndMarkIndexed(UUID tenantId, UUID documentId,
                                            List<TextChunk> chunks, List<float[]> embeddings) {
        vectorChunkRepository.saveAll(tenantId, documentId, chunks, embeddings);

        Document doc = documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found during indexing: ", documentId));
        doc.markAsIndexed();
        documentRepository.save(doc);
    }

    @Transactional
    public void markFailed(UUID tenantId, UUID documentId, String reason) {
        documentRepository.findByIdAndTenantId(documentId, tenantId).ifPresent(doc -> {
            doc.markAsFailed(reason);
            documentRepository.save(doc);
        });
    }
}