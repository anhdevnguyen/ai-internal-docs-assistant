package com.vanhdev.backend.shared.exception;

import com.vanhdev.backend.shared.api.ApiResponse;
import com.vanhdev.backend.shared.exception.ProviderRateLimitException;
import com.vanhdev.backend.shared.exception.LlmCompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(ResourceNotFoundException ex) {
        return ApiResponse.fail("RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleConflict(ConflictException ex) {
        return ApiResponse.fail("CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleUnauthorized(UnauthorizedException ex) {
        return ApiResponse.fail("UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler(InvalidFileException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResponse<Void> handleInvalidFile(InvalidFileException ex) {
        return ApiResponse.fail("INVALID_FILE", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse<Void> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return ApiResponse.fail("FILE_TOO_LARGE", "Uploaded file exceeds the maximum allowed size");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ApiResponse.fail("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception ex) {
        // Log full stacktrace for unexpected errors — never leak internals to client
        log.error("Unhandled exception", ex);
        return ApiResponse.fail("INTERNAL_ERROR", "An unexpected error occurred");
    }

    @ExceptionHandler(ProviderRateLimitException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleProviderRateLimit(ProviderRateLimitException ex) {
        log.warn("AI provider rate limit hit: {}", ex.getMessage());
        return ApiResponse.fail("PROVIDER_RATE_LIMIT", "AI service is temporarily unavailable. Please try again in a moment.");
    }

    @ExceptionHandler(LlmCompletionException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiResponse<Void> handleLlmCompletion(LlmCompletionException ex) {
        log.error("LLM completion failed: {}", ex.getMessage(), ex);
        return ApiResponse.fail("LLM_COMPLETION_FAILED", "Failed to generate AI response. Please try again.");
    }
}