package com.vanhdev.backend.admin.api;

import com.vanhdev.backend.admin.api.dto.AdminDtos.*;
import com.vanhdev.backend.admin.application.AdminDocumentService;
import com.vanhdev.backend.admin.application.AdminUserService;
import com.vanhdev.backend.admin.application.DashboardMetricsService;
import com.vanhdev.backend.shared.api.ApiResponse;
import com.vanhdev.backend.shared.api.PagedResponse;
import com.vanhdev.backend.shared.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * All routes under /admin/** are already protected at the SecurityFilterChain level
 * by `.requestMatchers("/admin/**").hasRole("ADMIN")`.
 * The @PreAuthorize annotation here is defense-in-depth at the method level —
 * it documents intent and ensures protection holds even if route config changes.
 * Both checks use the same token claim so there is no extra DB cost.
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DashboardMetricsService metricsService;
    private final AdminDocumentService documentService;
    private final AdminUserService userService;

    public AdminController(DashboardMetricsService metricsService,
                           AdminDocumentService documentService,
                           AdminUserService userService) {
        this.metricsService = metricsService;
        this.documentService = documentService;
        this.userService = userService;
    }

    // ── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/metrics/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview() {
        UUID tenantId = SecurityUtils.requireCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.ok(metricsService.getOverview(tenantId)));
    }

    // ── Document management ──────────────────────────────────────────────────

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<PagedResponse<AdminDocumentResponse>>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID tenantId = SecurityUtils.requireCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(documentService.listDocuments(tenantId, pageable)));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID documentId) {
        UUID tenantId = SecurityUtils.requireCurrentTenantId();
        documentService.deleteDocument(documentId, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/documents/{documentId}/reindex")
    public ResponseEntity<ApiResponse<AdminDocumentResponse>> forceReindex(@PathVariable UUID documentId) {
        UUID tenantId = SecurityUtils.requireCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.ok(documentService.forceReindex(documentId, tenantId)));
    }

    // ── User management ──────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID tenantId = SecurityUtils.requireCurrentTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(userService.listUsers(tenantId, pageable)));
    }

    /**
     * PATCH semantics: partial update of a single field.
     * Using /activate and /deactivate as sub-resources rather than a generic
     * `PATCH /users/{id}` with body — clearer intent, idempotent, easier to audit-log.
     */
    @PatchMapping("/users/{userId}/activate")
    public ResponseEntity<ApiResponse<ToggleUserActiveResponse>> activateUser(@PathVariable UUID userId) {
        UUID tenantId = SecurityUtils.requireCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.ok(userService.setUserActive(userId, tenantId, true)));
    }

    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<ApiResponse<ToggleUserActiveResponse>> deactivateUser(@PathVariable UUID userId) {
        UUID tenantId = SecurityUtils.requireCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.ok(userService.setUserActive(userId, tenantId, false)));
    }
}