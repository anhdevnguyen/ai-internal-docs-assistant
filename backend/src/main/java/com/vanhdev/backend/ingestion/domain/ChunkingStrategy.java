package com.vanhdev.backend.ingestion.domain;

import java.util.List;

/**
 * Strategy interface for splitting document text into embeddable chunks.
 * Pluggable by design — chunk size directly impacts RAG retrieval quality,
 * and switching strategies without code changes is expected.
 */
public interface ChunkingStrategy {
    List<TextChunk> chunk(String text);
}