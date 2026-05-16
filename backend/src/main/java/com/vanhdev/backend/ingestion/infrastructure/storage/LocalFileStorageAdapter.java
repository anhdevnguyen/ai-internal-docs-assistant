package com.vanhdev.backend.ingestion.infrastructure.storage;

import com.vanhdev.backend.shared.config.StorageProperties;
import com.vanhdev.backend.shared.exception.StorageException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalFileStorageAdapter implements StoragePort {

    private final StorageProperties storageProperties;

    public LocalFileStorageAdapter(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String store(InputStream content, String originalFilename, UUID tenantId, UUID storageKey) {
        Path targetPath = resolvePath(tenantId, storageKey, extractExtension(originalFilename));
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(content, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            throw new StorageException("Failed to persist file to local storage", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path path = Path.of(storagePath);
            Files.deleteIfExists(path);
            // Remove empty parent directory (the storageKey-named dir) to avoid accumulation
            Path parent = path.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                try (var entries = Files.list(parent)) {
                    if (entries.findAny().isEmpty()) {
                        Files.delete(parent);
                    }
                }
            }
        } catch (IOException e) {
            // Log but don't fail — orphan file cleanup is acceptable for now.
            // Phase 5: scheduled cleanup job for orphan storage paths.
            throw new StorageException("Failed to delete file at path: " + storagePath, e);
        }
    }

    // {base}/{tenantId}/{storageKey}/content.{ext}
    // storageKey is a random UUID — not documentId, not user-controlled, not guessable
    private Path resolvePath(UUID tenantId, UUID storageKey, String extension) {
        return Path.of(storageProperties.basePath(),
                tenantId.toString(),
                storageKey.toString(),
                "content" + extension);
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }
}
