package com.vanhdev.backend.shared.exception;

/**
 * Thrown when the LLM completion call fails for reasons other than rate limiting
 * (e.g. context window overflow, malformed response, network timeout).
 */
public class LlmCompletionException extends RuntimeException {

    public LlmCompletionException(String message) {
        super(message);
    }

    public LlmCompletionException(String message, Throwable cause) {
        super(message, cause);
    }
}