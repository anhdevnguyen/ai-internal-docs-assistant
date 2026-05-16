package com.vanhdev.backend.shared.exception;

import lombok.Getter;

@Getter
public class EmbeddingApiException extends RuntimeException {

    private final boolean rateLimited;

    public EmbeddingApiException(String message, boolean rateLimited) {
        super(message);
        this.rateLimited = rateLimited;
    }

}
