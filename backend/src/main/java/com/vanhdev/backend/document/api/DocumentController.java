package com.vanhdev.backend.document.api;

import com.vanhdev.backend.document.api.dto.DocumentResponse;
import com.vanhdev.backend.document.application.DocumentQueryService;
import com.vanhdev.backend.ingestion.application.DocumentUploadService;
import com.vanhdev.backend.shared.api.ApiResponse;
import com.vanhdev.backend.shared.api.PagedResponse;
import com.vanhdev.backend.shared.security.SecurityUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Validated
public class DocumentController {

    private final DocumentUploadService uploadService;
    private final DocumentQueryService documentQueryService;

    public DocumentController(DocumentUploadService uploadService,
                              DocumentQueryService documentQueryService) {
        this.uploadService = uploadService;
        this.documentQueryService = documentQueryService;
    }

    /**
     * Accepts a document upload and immediately returns 202 Accepted.
     * Ingestion runs asynchronously — clients poll GET /documents/{id} for status.
     */
    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<DocumentResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("title")
            @NotBlank(message = "title is required")
            @Size(max = 500, message = "title must not exceed 500 characters")
            String title
    ) {
        var principal = SecurityUtils.requireAuthenticatedUser();
        var document = uploadService.acceptUpload(
                principal.tenantId(),
                principal.userId(),
                title.strip(),
                file
        );
        return ApiResponse.ok(DocumentResponse.from(document));
    }

    @GetMapping
    public ApiResponse<PagedResponse<DocumentResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Cap page size to prevent abusive queries
        int cappedSize = Math.min(size, 100);
        var pageResult = documentQueryService.listForCurrentTenant(
                PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ApiResponse.ok(PagedResponse.from(pageResult.map(DocumentResponse::from)));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentResponse> getById(@PathVariable UUID id) {
        var document = documentQueryService.getByIdForCurrentTenant(id);
        return ApiResponse.ok(DocumentResponse.from(document));
    }
}