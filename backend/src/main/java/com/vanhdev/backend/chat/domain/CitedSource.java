package com.vanhdev.backend.chat.domain;

import java.util.UUID;

/**
 * Represents one document source cited in an assistant response.
 * This is a response-time construct built from RetrievedChunk — not persisted.
 * Clients use this to render citation chips with document title and excerpt.
 */
public record CitedSource(
        UUID documentId,
        String documentTitle,
        String chunkExcerpt
) {
    // Truncate excerpt for display — full content lives in document_chunks
    private static final int MAX_EXCERPT_LENGTH = 300;

    public static CitedSource from(UUID documentId, String documentTitle, String fullContent) {
        String excerpt = fullContent.length() > MAX_EXCERPT_LENGTH
                ? fullContent.substring(0, MAX_EXCERPT_LENGTH) + "…"
                : fullContent;
        return new CitedSource(documentId, documentTitle, excerpt);
    }
}