package com.vanhdev.backend.ingestion.application;

import com.vanhdev.backend.document.domain.Document;
import com.vanhdev.backend.document.infrastructure.DocumentJpaRepository;
import com.vanhdev.backend.ingestion.application.DocumentIngestionPipeline;
import com.vanhdev.backend.ingestion.infrastructure.storage.StoragePort;
import com.vanhdev.backend.shared.config.StorageProperties;
import com.vanhdev.backend.shared.exception.InvalidFileException;
import com.vanhdev.backend.shared.exception.ResourceNotFoundException;
import com.vanhdev.backend.shared.exception.StorageException;
import com.vanhdev.backend.shared.security.TenantContext;
import org.apache.tika.Tika;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class DocumentUploadService {

    private final DocumentJpaRepository documentRepository;
    private final StoragePort storagePort;
    private final DocumentIngestionPipeline ingestionPipeline;
    private final StorageProperties storageProperties;
    private final Tika tika = new Tika();

    public DocumentUploadService(DocumentJpaRepository documentRepository,
                                  StoragePort storagePort,
                                  DocumentIngestionPipeline ingestionPipeline,
                                  StorageProperties storageProperties) {
        this.documentRepository = documentRepository;
        this.storagePort = storagePort;
        this.ingestionPipeline = ingestionPipeline;
        this.storageProperties = storageProperties;
    }

    public Document upload(MultipartFile file, String titleOverride, UUID uploadedBy) {
        byte[] fileBytes = readFileBytes(file);
        String detectedMime = detectMimeType(fileBytes, file.getOriginalFilename());

        validateMimeType(detectedMime);
        validateFileSize(fileBytes.length);

        UUID tenantId = TenantContext.requireTenantId();
        UUID storageKey = UUID.randomUUID();
        String storagePath = storagePort.store(
                new ByteArrayInputStream(fileBytes),
                file.getOriginalFilename(),
                tenantId,
                storageKey);

        String title = (titleOverride != null && !titleOverride.isBlank())
                ? titleOverride.strip()
                : file.getOriginalFilename();

        Document document = documentRepository.save(
                Document.create(tenantId, uploadedBy, title,
                        file.getOriginalFilename(), storagePath, detectedMime));

        // Sync ingestion in Phase 2 — blocks until INDEXED or FAILED.
        // Phase 5: replace with async dispatch (Kafka/queue) so upload returns immediately.
        ingestionPipeline.ingest(document.getId(), fileBytes, detectedMime);

        // Reload to reflect status updated by pipeline
        return documentRepository.findById(document.getId()).orElseThrow();
    }

    public Page<Document> listForCurrentTenant(Pageable pageable) {
        return documentRepository.findByTenantId(TenantContext.requireTenantId(), pageable);
    }

    public Document getForCurrentTenant(UUID documentId) {
        return documentRepository.findByIdAndTenantId(documentId, TenantContext.requireTenantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: ", documentId));
    }

    @Transactional
    public void delete(UUID documentId, UUID requestingUserId) {
        UUID tenantId = TenantContext.requireTenantId();
        Document document = documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: ", documentId));

        String storagePath = document.getStoragePath();
        documentRepository.delete(document); // CASCADE deletes document_chunks

        // Storage delete is best-effort after DB delete. Orphan file is preferable to
        // a state where the DB record is gone but storage delete failed mid-transaction.
        try {
            storagePort.delete(storagePath);
        } catch (StorageException e) {
            // Phase 5: schedule cleanup job for orphaned storage paths
        }
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                throw new InvalidFileException("Uploaded file is empty");
            }
            return bytes;
        } catch (IOException e) {
            throw new InvalidFileException("Could not read uploaded file");
        }
    }

    private String detectMimeType(byte[] bytes, String filename) {
        try {
            // Tika detection uses file content (magic bytes), not the filename extension.
            // Filename is a hint only — actual content signature takes precedence.
            return tika.detect(bytes, filename);
        } catch (Exception e) {
            throw new InvalidFileException("Could not determine file type");
        }
    }

    private void validateMimeType(String detectedMime) {
        boolean allowed = storageProperties.allowedMimeTypes().stream()
                .anyMatch(detectedMime::startsWith);
        if (!allowed) {
            throw new InvalidFileException(
                    "File type '%s' is not supported. Allowed: PDF, DOCX, TXT".formatted(detectedMime));
        }
    }

    private void validateFileSize(int bytes) {
        long limitBytes = (long) storageProperties.maxFileSizeMb() * 1024 * 1024;
        if (bytes > limitBytes) {
            throw new InvalidFileException(
                    "File size exceeds the %dMB limit".formatted(storageProperties.maxFileSizeMb()));
        }
    }
}
