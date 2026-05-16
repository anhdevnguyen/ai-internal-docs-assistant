package com.vanhdev.backend.ingestion.infrastructure.extraction;

import com.vanhdev.backend.shared.exception.TextExtractionException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class TikaTextExtractorAdapter implements TextExtractorPort {

    // Minimum meaningful content — guards against scanned PDFs and empty files
    private static final int MIN_CONTENT_LENGTH = 50;
    // 5MB character limit prevents OOM on pathological inputs
    private static final int MAX_CONTENT_CHARS = 5_000_000;

    private final Tika tika = new Tika();

    @Override
    public String extract(byte[] content, String mimeType) {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.CONTENT_TYPE_OVERRIDE, mimeType);

        try {
            String text = tika.parseToString(new ByteArrayInputStream(content), metadata, MAX_CONTENT_CHARS);
            String trimmed = text.strip();

            if (trimmed.length() < MIN_CONTENT_LENGTH) {
                throw new TextExtractionException(
                        "Extracted text is too short (%d chars) — document may be a scanned image or empty"
                                .formatted(trimmed.length()));
            }

            return trimmed;
        } catch (TikaException | SAXException e) {
            throw new TextExtractionException("Document is corrupt or format is unsupported: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new TextExtractionException("IO error during text extraction", e);
        }
    }
}
