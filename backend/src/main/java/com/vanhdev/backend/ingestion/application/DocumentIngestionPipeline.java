package com.vanhdev.backend.ingestion.application;

import com.vanhdev.backend.ingestion.domain.ChunkingStrategy;
import com.vanhdev.backend.ingestion.domain.TextChunk;
import com.vanhdev.backend.ingestion.infrastructure.embedding.EmbeddingPort;
import com.vanhdev.backend.ingestion.infrastructure.extraction.TextExtractorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Async pipeline that drives the document from PENDING → PROCESSING → INDEXED (or FAILED).
 * Deliberately not @Transactional at the class level — each stage commits independently
 * via IngestionTransactions to avoid holding a DB connection across OpenAI API calls.
 * The @Async annotation causes Spring to dispatch this on the `ingestion-` thread pool
 * (configured in application.yml), freeing the HTTP thread immediately after upload.
 */
@Component
public class DocumentIngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionPipeline.class);

    private final IngestionTransactions transactions;
    private final TextExtractorPort textExtractor;
    private final ChunkingStrategy chunkingStrategy;
    private final EmbeddingPort embeddingPort;

    public DocumentIngestionPipeline(IngestionTransactions transactions,
                                     TextExtractorPort textExtractor,
                                     ChunkingStrategy chunkingStrategy,
                                     EmbeddingPort embeddingPort) {
        this.transactions = transactions;
        this.textExtractor = textExtractor;
        this.chunkingStrategy = chunkingStrategy;
        this.embeddingPort = embeddingPort;
    }

    @Async
    public void process(UUID tenantId, UUID documentId, String storagePath) {
        log.info("Ingestion started [documentId={}]", documentId);

        try {
            // Stage 1 — Transition: PENDING → PROCESSING (commit)
            var doc = transactions.markProcessing(tenantId, documentId);

            // Stage 2 — Extract (CPU-bound, no DB connection held)
            String rawText = textExtractor.extract(doc.getStoragePath());

            if (rawText.isBlank()) {
                transactions.markFailed(tenantId, documentId, "Extracted text is empty — document may be scanned or corrupt");
                log.warn("Empty extraction result [documentId={}]", documentId);
                return;
            }

            // Stage 3 — Chunk (in-memory, fast)
            List<TextChunk> chunks = chunkingStrategy.chunk(rawText);

            if (chunks.isEmpty()) {
                transactions.markFailed(tenantId, documentId, "Chunking produced no segments");
                return;
            }

            // Stage 4 — Embed (network I/O — no DB connection held)
            List<String> texts = chunks.stream().map(TextChunk::content).toList();
            List<float[]> embeddings = embeddingPort.embedBatch(texts);

            // Stage 5 — Persist chunks + transition: PROCESSING → INDEXED (commit)
            transactions.persistChunksAndMarkIndexed(tenantId, documentId, chunks, embeddings);

            log.info("Ingestion completed [documentId={}, chunks={}]", documentId, chunks.size());

        } catch (Exception ex) {
            // Catch-all: transition to FAILED so the document is never stuck in PROCESSING.
            // Error message is stored for operator visibility — not exposed to clients.
            log.error("Ingestion failed [documentId={}]: {}", documentId, ex.getMessage(), ex);
            transactions.markFailed(tenantId, documentId, truncate(ex.getMessage()));
        }
    }

    private String truncate(String message) {
        if (message == null) return "Unknown error";
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}