package com.vanhdev.backend.ingestion.infrastructure.extraction;

import com.vanhdev.backend.shared.exception.InvalidFileException;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Validates that the uploaded file's actual content matches a permitted MIME type
 * by inspecting the file's magic bytes via Tika — not the Content-Type header.
 * Why not trust Content-Type header:
 * The header is set by the client and trivially spoofed. An attacker can upload
 * a malicious binary with Content-Type: application/pdf and bypass any header-only check.
 * Tika reads the first N bytes of the stream (magic bytes / file signature) to determine
 * the real format independent of filename or declared content type.
 * Reuses TikaConfig.getDefaultConfig() to ensure XXE-safe Tika initialization
 * (same reasoning as TikaTextExtractorAdapter). Tika instance is lightweight and
 * thread-safe — safe as a singleton Spring bean.
 */
@Component
public class FileSignatureValidator {

    private static final Set<String> PERMITTED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private final Tika tika;

    public FileSignatureValidator() {
        try {
            this.tika = new Tika(TikaConfig.getDefaultConfig());
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to initialize Tika for MIME detection: " + e.getMessage());
        }
    }

    /**
     * Detects the real MIME type by reading the file's magic bytes.
     * Throws InvalidFileException if the detected type is not in the permitted set.
     * @param file the uploaded multipart file
     * @return detected MIME type string
     */
    public String detectAndValidate(MultipartFile file) {
        String detectedType = detect(file);

        if (!PERMITTED_MIME_TYPES.contains(detectedType)) {
            // Do not echo back the detected type — it may contain attacker-controlled content
            throw new InvalidFileException("File type not permitted. Accepted: PDF, DOCX, TXT");
        }

        return detectedType;
    }

    private String detect(MultipartFile file) {
        // Tika.detect(InputStream, String) uses both magic bytes and filename hint for
        // better accuracy on ambiguous types (e.g. distinguishing DOCX from ZIP).
        try (InputStream stream = file.getInputStream()) {
            return tika.detect(stream, file.getOriginalFilename());
        } catch (IOException e) {
            throw new InvalidFileException("Could not read file content for type validation");
        }
    }
}