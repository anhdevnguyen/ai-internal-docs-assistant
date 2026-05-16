package com.vanhdev.backend.ingestion.infrastructure.extraction;

public interface TextExtractorPort {
    /**
     * Reads file bytes from the given storage path and returns extracted plain text.
     * Implementations must not log raw file content.
     */
    String extract(String storagePath);
}