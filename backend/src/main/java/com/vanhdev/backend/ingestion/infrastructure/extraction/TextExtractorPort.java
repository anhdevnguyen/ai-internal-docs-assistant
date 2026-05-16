package com.vanhdev.backend.ingestion.infrastructure.extraction;

public interface TextExtractorPort {
    /**
     * Extracts plain text from document bytes.
     * Throws TextExtractionException for scanned PDFs, corrupt files, or insufficient content.
     */
    String extract(byte[] content, String mimeType);
}