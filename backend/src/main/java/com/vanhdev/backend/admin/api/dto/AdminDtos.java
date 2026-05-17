package com.vanhdev.backend.admin.api.dto;

import com.vanhdev.backend.document.domain.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * All admin-facing DTOs in one file — these are presentation-layer contracts only.
 * None of these leak domain entity internals (no JPA annotations, no lazy collections).
 */
public final class AdminDtos {

    private AdminDtos() {}

    // ── Dashboard ────────────────────────────────────────────────────────────

    public record DashboardOverviewResponse(
            Map<DocumentStatus, Long> documentCountByStatus,
            long totalUsers,
            long activeUsersToday,
            long chatSessionsToday,
            List<TopDocumentEntry> topDocumentsByRetrieval
    ) {}

    public record TopDocumentEntry(
            UUID documentId,
            String title,
            long hitCount
    ) {}

    // ── Document management ──────────────────────────────────────────────────

    public record AdminDocumentResponse(
            UUID id,
            UUID uploadedBy,
            String title,
            String originalFilename,
            String mimeType,
            DocumentStatus status,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {}

    // ── User management ──────────────────────────────────────────────────────

    public record AdminUserResponse(
            UUID id,
            String email,
            String role,
            boolean isActive,
            Instant createdAt
    ) {}

    public record ToggleUserActiveResponse(
            UUID userId,
            boolean isActive
    ) {}
}