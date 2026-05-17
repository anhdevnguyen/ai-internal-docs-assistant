package com.vanhdev.backend.document.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;

    @Column(nullable = false)
    private String title;

    @Column(name = "original_filename", nullable = false, updatable = false)
    private String originalFilename;

    // Internal server path — never returned to clients directly
    @Column(name = "storage_path", nullable = false, updatable = false)
    private String storagePath;

    @Column(name = "mime_type", nullable = false, updatable = false)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Document() {}

    public static Document create(UUID tenantId, UUID uploadedBy, String title,
                                   String originalFilename, String storagePath, String mimeType) {
        Document doc = new Document();
        doc.tenantId = tenantId;
        doc.uploadedBy = uploadedBy;
        doc.title = title;
        doc.originalFilename = originalFilename;
        doc.storagePath = storagePath;
        doc.mimeType = mimeType;
        doc.status = DocumentStatus.PENDING;
        doc.createdAt = Instant.now();
        doc.updatedAt = doc.createdAt;
        return doc;
    }

    // --- Domain behaviours ---

    public void markAsProcessing() {
        this.status = DocumentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markAsIndexed() {
        this.status = DocumentStatus.INDEXED;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void markAsFailed(String reason) {
        this.status = DocumentStatus.FAILED;
        this.errorMessage = reason;
        this.updatedAt = Instant.now();
    }

    /**
     * Resets a FAILED document back to PENDING so the ingestion pipeline can retry.
     * Only called by AdminDocumentService.forceReindex() after stale chunks are purged.
     * Not exposed as a general-purpose method — the only valid transition to PENDING
     * after creation is a retry from FAILED.
     */
    public void resetToPending() {
        this.status = DocumentStatus.PENDING;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }
}
