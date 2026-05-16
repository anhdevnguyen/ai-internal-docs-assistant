package com.vanhdev.backend.shared.exception;

public class EmbeddingApiException extends RuntimeException {

    private final boolean rateLimited;

    public EmbeddingApiException(String message, boolean rateLimited) {
        super(message);
        this.rateLimited = rateLimited;
    }

    public boolean isRateLimited() { return rateLimited; }
}
