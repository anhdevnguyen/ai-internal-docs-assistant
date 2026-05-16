package com.vanhdev.backend.chat.api;

import com.vanhdev.backend.chat.api.dto.ChatDtos;
import com.vanhdev.backend.chat.application.ChatOrchestrationService;
import com.vanhdev.backend.chat.domain.ChatMessage;
import com.vanhdev.backend.chat.domain.MessageRole;
import com.vanhdev.backend.shared.api.ApiResponse;
import com.vanhdev.backend.shared.security.SecurityUtils;
import com.vanhdev.backend.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatOrchestrationService orchestrationService;

    public ChatController(ChatOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatDtos.SessionResponse> createSession() {
        UserPrincipal principal = SecurityUtils.requireAuthenticatedUser();
        var session = orchestrationService.createSession(principal.userId(), principal.tenantId());
        return ApiResponse.ok(new ChatDtos.SessionResponse(session.getId(), session.getTitle(), session.getCreatedAt()));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatDtos.SessionResponse>> listSessions() {
        UserPrincipal principal = SecurityUtils.requireAuthenticatedUser();
        List<ChatDtos.SessionResponse> sessions = orchestrationService
                .listSessions(principal.userId(), principal.tenantId())
                .stream()
                .map(s -> new ChatDtos.SessionResponse(s.getId(), s.getTitle(), s.getCreatedAt()))
                .toList();
        return ApiResponse.ok(sessions);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatDtos.SendMessageResponse> sendMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatDtos.SendMessageRequest request
    ) {
        UserPrincipal principal = SecurityUtils.requireAuthenticatedUser();
        ChatOrchestrationService.AssistantResponse response = orchestrationService.handleUserMessage(
                sessionId,
                principal.tenantId(),
                request.content()
        );

        List<ChatDtos.CitationResponse> citations = response.citations().stream()
                .map(ChatDtos.CitationResponse::from)
                .toList();

        return ApiResponse.ok(new ChatDtos.SendMessageResponse(
                response.messageId(),
                response.content(),
                citations
        ));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatDtos.MessageResponse>> listMessages(@PathVariable UUID sessionId) {
        UserPrincipal principal = SecurityUtils.requireAuthenticatedUser();
        List<ChatDtos.MessageResponse> messages = orchestrationService
                .listMessages(sessionId, principal.tenantId())
                .stream()
                .map(msg -> toMessageResponse(msg))
                .toList();
        return ApiResponse.ok(messages);
    }

    private ChatDtos.MessageResponse toMessageResponse(ChatMessage msg) {
        // Citations are not re-fetched on history load — the excerpt was not persisted.
        // History view shows message content only; citation details require the send-message response.
        // This is an intentional trade-off: storing excerpts in DB is redundant (they live in chunks).
        List<ChatDtos.CitationResponse> emptyCitations = List.of();
        return new ChatDtos.MessageResponse(
                msg.getId(),
                msg.getRole(),
                msg.getContent(),
                emptyCitations,
                msg.getCreatedAt()
        );
    }
}