package com.vanhdev.backend.document.api.dto;

import com.vanhdev.backend.document.domain.Document;
import com.vanhdev.backend.document.domain.DocumentStatus;

import java.util.UUID;

// Lightweight projection for frontend polling — avoids sending full payload every 2s
public record DocumentStatusResponse(UUID id, DocumentStatus status, String errorMessage) {
    public static DocumentStatusResponse from(Document doc) {
        return new DocumentStatusResponse(doc.getId(), doc.getStatus(), doc.getErrorMessage());
    }
}
