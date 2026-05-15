package com.vanhdev.backend.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/**
 * Uniform API envelope. Every endpoint returns this shape so clients have a single
 * contract to code against — success/failure discrimination is always via the
 * top-level `success` field, never via HTTP status alone.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorPayload error,
        Meta meta
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Meta.generate());
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorPayload(code, message), Meta.generate());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorPayload(String code, String message) {}

    public record Meta(String requestId, Instant timestamp) {
        static Meta generate() {
            return new Meta(UUID.randomUUID().toString(), Instant.now());
        }
    }
}