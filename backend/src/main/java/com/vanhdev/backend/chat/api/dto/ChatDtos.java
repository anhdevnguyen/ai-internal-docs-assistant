package com.vanhdev.backend.chat.api.dto;

import com.vanhdev.backend.chat.domain.CitedSource;
import com.vanhdev.backend.chat.domain.MessageRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// === Requests ===

public final class ChatDtos {

    private ChatDtos() {}

    public record SendMessageRequest(
            @NotBlank(message = "Message content must not be blank")
            @Size(max = 4000, message = "Message must not exceed 4000 characters")
            String content
    ) {}

    // === Responses ===

    public record SessionResponse(
            UUID id,
            String title,
            Instant createdAt
    ) {}

    public record MessageResponse(
            UUID id,
            MessageRole role,
            String content,
            List<CitationResponse> citations,
            Instant createdAt
    ) {}

    public record CitationResponse(
            UUID documentId,
            String documentTitle,
            String chunkExcerpt
    ) {
        public static CitationResponse from(CitedSource source) {
            return new CitationResponse(
                    source.documentId(),
                    source.documentTitle(),
                    source.chunkExcerpt()
            );
        }
    }

    public record SendMessageResponse(
            UUID messageId,
            String content,
            List<CitationResponse> citations
    ) {}
}