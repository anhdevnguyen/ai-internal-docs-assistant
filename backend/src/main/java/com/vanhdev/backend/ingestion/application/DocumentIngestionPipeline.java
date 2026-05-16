package com.vanhdev.backend.ingestion.application;

import com.vanhdev.backend.document.domain.Document;
import com.vanhdev.backend.document.domain.DocumentStatus;
import com.vanhdev.backend.document.infrastructure.DocumentJpaRepository;
import com.vanhdev.backend.document.infrastructure.VectorChunkRepository;
import com.vanhdev.backend.ingestion.domain.ChunkingStrategy;
import com.vanhdev.backend.ingestion.domain.TextChunk;
import com.vanhdev.backend.ingestion.infrastructure.embedding.EmbeddingPort;
import com.vanhdev.backend.ingestion.infrastructure.extraction.TextExtractorPort;
import com.vanhdev.backend.shared.exception.EmbeddingApiException;
import com.vanhdev.backend.shared.exception.TextExtractionException;
import com.vanhdev.backend.shared.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentIngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionPipeline.class);

    private final DocumentJpaRepository documentRepository;
    private final VectorChunkRepository vectorChunkRepository;
    private final TextExtractorPort textExtractor;
    private final ChunkingStrategy chunkingStrategy;
    private final EmbeddingPort embeddingPort;
    // Self-reference for transactional dispatch — avoids Spring AOP self-invocation bypass
    private final IngestionTransactions tx;

    public DocumentIngestionPipeline(DocumentJpaRepository documentRepository,
                                      VectorChunkRepository vectorChunkRepository,
                                      TextExtractorPort textExtractor,
                                      ChunkingStrategy chunkingStrategy,
                                      EmbeddingPort embeddingPort,
                                      IngestionTransactions tx) {
        this.documentRepository = documentRepository;
        this.vectorChunkRepository = vectorChunkRepository;
        this.textExtractor = textExtractor;
        this.chunkingStrategy = chunkingStrategy;
        this.embeddingPort = embeddingPort;
        this.tx = tx;
    }

    /**
     * Orchestrates the ingestion pipeline. Not @Transactional — holding a DB connection
     * open during text extraction and embedding API calls would exhaust the connection pool.
     * Transactional boundaries are explicit in IngestionTransactions.
     */
    public void ingest(UUID documentId, byte[] fileContent, String mimeType) {
        log.info("Starting ingestion for document {}", documentId);
        tx.updateStatus(documentId, DocumentStatus.PROCESSING, null);

        try {
            String text = textExtractor.extract(fileContent, mimeType);
            List<TextChunk> chunks = chunkingStrategy.chunk(text);

            if (chunks.isEmpty()) {
                tx.updateStatus(documentId, DocumentStatus.FAILED,
                        "No indexable content produced from document");
                return;
            }

            List<float[]> embeddings = embeddingPort.embedBatch(
                    chunks.stream().map(TextChunk::content).toList());

            UUID tenantId = TenantContext.requireTenantId();
            tx.persistChunksAndMarkIndexed(documentId, tenantId, chunks, embeddings);

            log.info("Ingestion complete for document {} — {} chunks indexed", documentId, chunks.size());

        } catch (TextExtractionException e) {
            log.warn("Text extraction failed for document {}: {}", documentId, e.getMessage());
            tx.updateStatus(documentId, DocumentStatus.FAILED, "Text extraction failed: " + e.getMessage());
        } catch (EmbeddingApiException e) {
            log.error("Embedding API failed for document {}: {}", documentId, e.getMessage());
            tx.updateStatus(documentId, DocumentStatus.FAILED, "Embedding service error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected ingestion failure for document {}", documentId, e);
            tx.updateStatus(documentId, DocumentStatus.FAILED, "Unexpected processing error");
        }
    }
}
