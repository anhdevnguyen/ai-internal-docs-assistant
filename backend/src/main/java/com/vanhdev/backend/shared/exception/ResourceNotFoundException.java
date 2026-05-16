package com.vanhdev.backend.shared.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String errorCode;

    public ResourceNotFoundException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    // Convenience constructor for the common "entity X with id Y not found" pattern
    public ResourceNotFoundException(String entityName, UUID id) {
        this(entityName.toUpperCase() + "_NOT_FOUND",
                entityName + " with id " + id + " not found");
    }
}