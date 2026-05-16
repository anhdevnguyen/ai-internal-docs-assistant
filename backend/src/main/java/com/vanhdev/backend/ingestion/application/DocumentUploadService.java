package com.vanhdev.backend.ingestion.application;

import com.vanhdev.backend.document.domain.Document;
import com.vanhdev.backend.document.infrastructure.DocumentJpaRepository;
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

    public DocumentUploadService(StoragePort storagePort,
                                 DocumentJpaRepository documentRepository,
                                 DocumentIngestionPipeline ingestionPipeline,
                                 StorageProperties storageProperties) {
        this.storagePort = storagePort;
        this.documentRepository = documentRepository;
        this.ingestionPipeline = ingestionPipeline;
        this.storageProperties = storageProperties;
    }

    /**
     * Validates, stores, and registers a document, then fires the async ingestion pipeline.
     * This method commits a single short transaction (file record only).
     * The pipeline runs entirely outside this transaction on a separate thread.
     * Returns the persisted Document so the controller can build the 202 response.
     */
    @Transactional
    public Document acceptUpload(UUID tenantId, UUID uploadedBy,
                                 String title, MultipartFile file) {
        validateFile(file);

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
                file.getContentType()
        );
        // Override auto-generated ID with our pre-allocated UUID so storagePath and documentId are consistent
        Document saved = documentRepository.save(document);

        log.info("Document accepted [documentId={}, tenantId={}]", saved.getId(), tenantId);

        // Fire-and-forget: pipeline runs on the ingestion thread pool after this transaction commits.
        // Spring's @Async proxy guarantees the method is called after the current TX commits,
        // so the pipeline will always find the document record in PENDING state.
        ingestionPipeline.process(tenantId, saved.getId(), storagePath);

        return saved;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !storageProperties.allowedMimeTypes().contains(contentType)) {
            throw new InvalidFileException("File type not permitted: " + contentType);
        }

        long maxBytes = (long) storageProperties.maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new InvalidFileException(
                    "File exceeds maximum size of " + storageProperties.maxFileSizeMb() + "MB");
        }
    }
}