package com.vanhdev.backend.retrieval.domain;

import java.util.UUID;

/**
 * Value object representing a single chunk retrieved from semantic search.
 * Immutable by design — retrieval results are read-only within the chat flow.
 * similarityScore is cosine similarity (0..1), higher = more relevant.
 * Chunks below the configured minimum threshold are excluded before this object
 * is constructed, so consumers can trust that any instance is above the noise floor.
 */
public record RetrievedChunk(
        UUID chunkId,
        UUID documentId,
        String documentTitle,
        String content,
        double similarityScore
) {
    public RetrievedChunk {
        if (similarityScore < 0 || similarityScore > 1) {
            throw new IllegalArgumentException("similarityScore must be in [0, 1], got: " + similarityScore);
        }
    }
}