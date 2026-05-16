package com.vanhdev.backend.ingestion.infrastructure.storage;

import java.io.InputStream;
import java.util.UUID;

public interface StoragePort {

    /**
     * Persists file bytes and returns the internal opaque storage path.
     * The path must never be exposed directly to clients.
     */
    String store(InputStream content, String originalFilename, UUID tenantId, UUID storageKey);

    void delete(String storagePath);
}
