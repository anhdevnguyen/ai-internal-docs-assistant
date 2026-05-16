package com.vanhdev.backend.chat.infrastructure;

import java.util.List;

/**
 * Port interface for LLM chat completion.
 * Decouples ChatOrchestrationService from provider specifics (OpenAI, Gemini, etc.).
 * Switching providers only requires a new adapter — no application-layer changes.
 */
public interface LlmPort {

    CompletionResult complete(List<Message> messages);

    record Message(String role, String content) {
        public static Message system(String content) { return new Message("system", content); }
        public static Message user(String content)   { return new Message("user", content); }
    }

    record CompletionResult(String content, int promptTokens, int completionTokens) {}
}