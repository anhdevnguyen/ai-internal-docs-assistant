package com.vanhdev.backend.chat.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatSession() {}

    public static ChatSession create(UUID userId, UUID tenantId) {
        ChatSession session = new ChatSession();
        session.userId = userId;
        session.tenantId = tenantId;
        session.createdAt = Instant.now();
        return session;
    }

    public void assignTitle(String title) {
        // Title is set asynchronously after the first message — not in the constructor
        this.title = title;
    }
}