package com.vanhdev.backend.document.api;

import com.vanhdev.backend.document.api.dto.DocumentResponse;
import com.vanhdev.backend.document.api.dto.DocumentStatusResponse;
import com.vanhdev.backend.ingestion.application.DocumentUploadService;
import com.vanhdev.backend.shared.api.ApiResponse;
import com.vanhdev.backend.shared.api.PagedResponse;
import com.vanhdev.backend.shared.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentUploadService documentUploadService;

    public DocumentController(DocumentUploadService documentUploadService) {
        this.documentUploadService = documentUploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title) {

        UUID userId = SecurityUtils.requireCurrentUserId();
        DocumentResponse response = DocumentResponse.from(
                documentUploadService.upload(file, title, userId));
        return ApiResponse.ok(response);
    }

    @GetMapping
    public ApiResponse<PagedResponse<DocumentResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Caps page size to prevent abusive queries
        int cappedSize = Math.min(size, 100);
        PageRequest pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        return ApiResponse.ok(PagedResponse.from(
                documentUploadService.listForCurrentTenant(pageable),
                DocumentResponse::from));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(DocumentResponse.from(documentUploadService.getForCurrentTenant(id)));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<DocumentStatusResponse> getStatus(@PathVariable UUID id) {
        return ApiResponse.ok(DocumentStatusResponse.from(documentUploadService.getForCurrentTenant(id)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        documentUploadService.delete(id, userId);
    }
}