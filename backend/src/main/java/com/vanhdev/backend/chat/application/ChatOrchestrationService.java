package com.vanhdev.backend.chat.application;

import com.vanhdev.backend.chat.domain.ChatMessage;
import com.vanhdev.backend.chat.domain.ChatSession;
import com.vanhdev.backend.chat.domain.CitedSource;
import com.vanhdev.backend.chat.infrastructure.ChatMessageRepository;
import com.vanhdev.backend.chat.infrastructure.ChatSessionRepository;
import com.vanhdev.backend.chat.infrastructure.LlmPort;
import com.vanhdev.backend.retrieval.application.SemanticSearchService;
import com.vanhdev.backend.retrieval.domain.RetrievedChunk;
import com.vanhdev.backend.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ChatOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestrationService.class);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final SemanticSearchService semanticSearchService;
    private final PromptBuilder promptBuilder;
    private final LlmPort llmPort;

    public ChatOrchestrationService(ChatSessionRepository sessionRepository,
                                    ChatMessageRepository messageRepository,
                                    SemanticSearchService semanticSearchService,
                                    PromptBuilder promptBuilder,
                                    LlmPort llmPort) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.semanticSearchService = semanticSearchService;
        this.promptBuilder = promptBuilder;
        this.llmPort = llmPort;
    }

    @Transactional
    public ChatSession createSession(UUID userId, UUID tenantId) {
        ChatSession session = ChatSession.create(userId, tenantId);
        return sessionRepository.save(session);
    }

    public List<ChatSession> listSessions(UUID userId, UUID tenantId) {
        return sessionRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId);
    }

    public List<ChatMessage> listMessages(UUID sessionId, UUID tenantId) {
        // Ownership check — tenant boundary enforced before returning any messages
        resolveSession(sessionId, tenantId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * Core RAG flow: embed → retrieve → prompt → complete → persist.
     *
     * @return the result carrying the assistant's answer and citation metadata
     */
    @Transactional
    public AssistantResponse handleUserMessage(UUID sessionId, UUID tenantId, String userQuestion) {
        ChatSession session = resolveSession(sessionId, tenantId);

        // Persist the user message immediately so it's visible in history even if LLM fails
        ChatMessage userMessage = ChatMessage.userMessage(sessionId, userQuestion);
        messageRepository.save(userMessage);

        // Retrieve relevant chunks (embedding + vector search)
        List<RetrievedChunk> relevantChunks = semanticSearchService.findRelevantChunks(userQuestion, tenantId);

        // Build prompt — PromptBuilder handles empty-context case gracefully
        String systemPrompt = promptBuilder.buildSystemPrompt(relevantChunks);

        // Call LLM — exceptions (rate limit, completion failure) propagate to GlobalExceptionHandler
        LlmPort.CompletionResult completion = llmPort.complete(List.of(
                LlmPort.Message.system(systemPrompt),
                LlmPort.Message.user(userQuestion)
        ));

        // Extract chunk IDs for traceability — stored in DB for analytics and audit
        List<UUID> chunkIds = relevantChunks.stream().map(RetrievedChunk::chunkId).toList();

        ChatMessage assistantMessage = ChatMessage.assistantMessage(
                sessionId,
                completion.content(),
                chunkIds,
                completion.promptTokens(),
                completion.completionTokens()
        );
        messageRepository.save(assistantMessage);

        // Auto-generate session title from first user question (async, non-blocking)
        if (session.getTitle() == null) {
            generateSessionTitleAsync(session, userQuestion);
        }

        List<CitedSource> citations = relevantChunks.stream()
                .map(c -> CitedSource.from(c.documentId(), c.documentTitle(), c.content()))
                .toList();

        log.info("RAG response [sessionId={}, chunks_used={}, prompt_tokens={}, completion_tokens={}]",
                sessionId, relevantChunks.size(), completion.promptTokens(), completion.completionTokens());

        return new AssistantResponse(assistantMessage.getId(), completion.content(), citations);
    }

    /**
     * Generates a short session title by asking the LLM to summarize the first question.
     * Runs async so the main response is not delayed. Failure is swallowed — a missing
     * title is cosmetic, not functional.
     */
    @Async
    @Transactional
    protected void generateSessionTitleAsync(ChatSession session, String firstQuestion) {
        try {
            LlmPort.CompletionResult result = llmPort.complete(List.of(
                    LlmPort.Message.system(
                            "Summarise the following question in 5 words or fewer. " +
                                    "Return only the title, no punctuation at the end."),
                    LlmPort.Message.user(firstQuestion)
            ));
            session.assignTitle(result.content().trim());
            sessionRepository.save(session);
        } catch (Exception e) {
            // Title generation is best-effort — do not surface this failure to the user
            log.warn("Session title generation failed [sessionId={}]: {}", session.getId(), e.getMessage());
        }
    }

    private ChatSession resolveSession(UUID sessionId, UUID tenantId) {
        return sessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chat session not found: ", sessionId));
    }

    /**
     * Carries the assistant's answer and citations back to the controller.
     * Not persisted — assembled at response time from retrieved chunks.
     */
    public record AssistantResponse(
            UUID messageId,
            String content,
            List<CitedSource> citations
    ) {}
}