package com.vanhdev.backend.chat.infrastructure;

import com.vanhdev.backend.chat.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findByUserIdAndTenantIdOrderByCreatedAtDesc(UUID userId, UUID tenantId);

    Optional<ChatSession> findByIdAndTenantId(UUID id, UUID tenantId);
}