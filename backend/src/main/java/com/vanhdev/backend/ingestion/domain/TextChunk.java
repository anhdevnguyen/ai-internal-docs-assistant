package com.vanhdev.backend.ingestion.domain;

/**
 * Immutable value object representing one chunk of text ready for embedding.
 * index preserves original document order — critical for traceability.
 */
public record TextChunk(int index, String content, int estimatedTokenCount) {}
