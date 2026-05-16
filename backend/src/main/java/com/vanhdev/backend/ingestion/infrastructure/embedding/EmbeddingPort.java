package com.vanhdev.backend.ingestion.infrastructure.embedding;

import java.util.List;

public interface EmbeddingPort {
    /**
     * Returns embeddings for the given texts, preserving input order.
     * Implementations handle batching and retry internally.
     */
    List<float[]> embedBatch(List<String> texts);
}
