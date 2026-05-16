package com.vanhdev.backend.ingestion.infrastructure.extraction;

import com.vanhdev.backend.shared.exception.TextExtractionException;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class TikaTextExtractorAdapter implements TextExtractorPort {

    // AutoDetectParser is thread-safe and expensive to construct — singleton per Tika docs.
    // Constructed via TikaConfig.getDefaultConfig() rather than new AutoDetectParser()
    // because Tika 3.2.2+ applies XXE-safe defaults in TikaConfig (fix for CVE-2025-66516).
    // Bypassing TikaConfig re-exposes the vulnerability even on the patched version.
    private static final AutoDetectParser PARSER;

    static {
        TikaConfig config;
        try {
            config = TikaConfig.getDefaultConfig();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to initialize Tika: " + e.getMessage());
        }
        PARSER = new AutoDetectParser(config);
    }

    @Override
    public String extract(String storagePath) {
        Path filePath = Paths.get(storagePath);

        try (InputStream stream = Files.newInputStream(filePath)) {
            // -1 disables the character limit — bounded upstream by StorageProperties.maxFileSizeMb
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            PARSER.parse(stream, handler, metadata, context);

            return normalise(handler.toString());
        } catch (IOException | SAXException | TikaException e) {
            // storagePath intentionally omitted from exception message — it contains tenant/id info
            throw new TextExtractionException("Text extraction failed: " + e.getMessage(), e);
        }
    }

    // Collapse excessive blank lines; double newlines serve as paragraph boundaries for chunking
    private String normalise(String raw) {
        return raw
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .strip();
    }
}