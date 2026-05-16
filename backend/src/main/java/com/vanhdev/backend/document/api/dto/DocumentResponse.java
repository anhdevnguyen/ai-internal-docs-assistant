package com.vanhdev.backend.document.api.dto;

import com.vanhdev.backend.document.domain.Document;
import com.vanhdev.backend.document.domain.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String originalFilename,
        String mimeType,
        DocumentStatus status,
        String errorMessage,
        UUID uploadedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getOriginalFilename(),
                doc.getMimeType(),
                doc.getStatus(),
                doc.getErrorMessage(),
                doc.getUploadedBy(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
