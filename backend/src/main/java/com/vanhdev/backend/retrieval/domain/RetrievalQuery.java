package com.vanhdev.backend.retrieval.domain;

import java.util.UUID;

/**
 * Encapsulates all parameters for a semantic search operation.
 * Using a value object here instead of raw parameters makes
 * SemanticSearchService's interface stable as retrieval evolves
 * (e.g. adding document-scope filtering, date range, etc.).
 */
public record RetrievalQuery(
        UUID tenantId,
        float[] questionEmbedding,
        int topK,
        double minSimilarity
) {}