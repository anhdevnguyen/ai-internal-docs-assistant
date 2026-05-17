package com.vanhdev.backend.ingestion.application;

import com.vanhdev.backend.document.domain.Document;
import com.vanhdev.backend.document.infrastructure.DocumentJpaRepository;
import com.vanhdev.backend.ingestion.infrastructure.extraction.FileSignatureValidator;
import com.vanhdev.backend.ingestion.infrastructure.storage.StoragePort;
import com.vanhdev.backend.shared.config.StorageProperties;
import com.vanhdev.backend.shared.exception.InvalidFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class DocumentUploadService {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);

    private final StoragePort storagePort;
    private final DocumentJpaRepository documentRepository;
    private final DocumentIngestionPipeline ingestionPipeline;
    private final StorageProperties storageProperties;
    private final FileSignatureValidator fileSignatureValidator;

    public DocumentUploadService(StoragePort storagePort,
                                 DocumentJpaRepository documentRepository,
                                 DocumentIngestionPipeline ingestionPipeline,
                                 StorageProperties storageProperties,
                                 FileSignatureValidator fileSignatureValidator) {
        this.storagePort = storagePort;
        this.documentRepository = documentRepository;
        this.ingestionPipeline = ingestionPipeline;
        this.storageProperties = storageProperties;
        this.fileSignatureValidator = fileSignatureValidator;
    }

    /**
     * Validates, stores, and registers a document, then fires the async ingestion pipeline.
     * This method commits a single short transaction (file record only).
     * The pipeline runs entirely outside this transaction on a separate thread.
     */
    @Transactional
    public Document acceptUpload(UUID tenantId, UUID uploadedBy,
                                 String title, MultipartFile file) {
        validateSize(file);

        // Detect real MIME type from magic bytes — client-declared Content-Type is untrusted.
        // This is the primary security gate; StorageProperties.allowedMimeTypes provides
        // a secondary configuration-level filter for the stored record.
        String detectedMimeType = fileSignatureValidator.detectAndValidate(file);

        UUID documentId = UUID.randomUUID();

        String storagePath;
        try {
            storagePath = storagePort.store(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    tenantId,
                    documentId
            );
        } catch (IOException e) {
            throw new InvalidFileException("Could not read uploaded file stream: " + e.getMessage());
        }

        Document document = Document.create(
                tenantId,
                uploadedBy,
                title,
                file.getOriginalFilename(),
                storagePath,
                detectedMimeType  // Store detected type, not the client-declared one
        );
        Document saved = documentRepository.save(document);

        log.info("Document accepted [documentId={}, tenantId={}, mimeType={}]",
                saved.getId(), tenantId, detectedMimeType);

        ingestionPipeline.process(tenantId, saved.getId(), storagePath);

        return saved;
    }

    private void validateSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }

        long maxBytes = (long) storageProperties.maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new InvalidFileException(
                    "File exceeds maximum size of " + storageProperties.maxFileSizeMb() + "MB");
        }
    }
}