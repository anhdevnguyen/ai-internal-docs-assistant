package com.vanhdev.backend.chat.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MessageRole role;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String content;

    /**
     * Chunk IDs used to generate this assistant message.
     * Empty for USER role messages — persisted as '{}' per schema default.
     * This field is the foundation for citation display, analytics, and audit.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "retrieved_chunk_ids", columnDefinition = "uuid[]")
    private UUID[] retrievedChunkIds;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {}

    public static ChatMessage userMessage(UUID sessionId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.sessionId = sessionId;
        msg.role = MessageRole.USER;
        msg.content = content;
        msg.retrievedChunkIds = new UUID[0];
        msg.createdAt = Instant.now();
        return msg;
    }

    public static ChatMessage assistantMessage(
            UUID sessionId,
            String content,
            List<UUID> retrievedChunkIds,
            Integer promptTokens,
            Integer completionTokens
    ) {
        ChatMessage msg = new ChatMessage();
        msg.sessionId = sessionId;
        msg.role = MessageRole.ASSISTANT;
        msg.content = content;
        msg.retrievedChunkIds = retrievedChunkIds.toArray(new UUID[0]);
        msg.promptTokens = promptTokens;
        msg.completionTokens = completionTokens;
        msg.createdAt = Instant.now();
        return msg;
    }
}