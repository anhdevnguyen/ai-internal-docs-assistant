package com.vanhdev.backend.ingestion.infrastructure.storage;

import com.vanhdev.backend.shared.config.StorageProperties;
import com.vanhdev.backend.shared.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalFileStorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageAdapter.class);

    private final Path baseDir;

    public LocalFileStorageAdapter(StorageProperties props) {
        this.baseDir = Paths.get(props.basePath()).toAbsolutePath().normalize();
    }

    @Override
    public String store(InputStream content, String originalFilename, UUID tenantId, UUID storageKey) {
        String extension = extractExtension(originalFilename);
        // Tenant-scoped path prevents cross-tenant file access via UUID guessing.
        // storageKey (document UUID) replaces original filename to avoid path traversal
        // and filename collision — the original name is preserved only in the DB record.
        Path destination = baseDir
                .resolve(tenantId.toString())
                .resolve(storageKey + extension)
                .normalize();

        // Guard: ensure resolved path is still inside baseDir after normalization
        if (!destination.startsWith(baseDir)) {
            throw new StorageException("Resolved storage path escapes base directory — possible path traversal");
        }

        try {
            Files.createDirectories(destination.getParent());
            Files.copy(content, destination, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file at {}", destination);
            return destination.toString();
        } catch (IOException e) {
            throw new StorageException("Failed to persist uploaded file: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storagePath) {
        Path target = Paths.get(storagePath).normalize();
        if (!target.startsWith(baseDir)) {
            throw new StorageException("Delete target escapes storage base directory");
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // Log and continue — a failed delete should not block domain operations
            log.warn("Could not delete file at {}: {}", storagePath, e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot).toLowerCase() : "";
    }
}