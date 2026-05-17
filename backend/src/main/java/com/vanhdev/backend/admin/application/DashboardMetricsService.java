package com.vanhdev.backend.admin.application;

import com.vanhdev.backend.admin.api.dto.AdminDtos.DashboardOverviewResponse;
import com.vanhdev.backend.admin.api.dto.AdminDtos.TopDocumentEntry;
import com.vanhdev.backend.admin.infrastructure.AdminMetricsRepository;
import com.vanhdev.backend.document.domain.DocumentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardMetricsService {

    private static final int TOP_DOCUMENTS_LIMIT = 5;

    private final AdminMetricsRepository metricsRepository;

    public DashboardMetricsService(AdminMetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    public DashboardOverviewResponse getOverview(UUID tenantId) {
        Map<DocumentStatus, Long> countByStatus = buildStatusCountMap(tenantId);
        long totalUsers = metricsRepository.countUsers(tenantId);
        long activeUsersToday = metricsRepository.countActiveUsersToday(tenantId);
        long chatSessionsToday = metricsRepository.countChatSessionsToday(tenantId);
        List<TopDocumentEntry> topDocuments = metricsRepository.findTopRetrievedDocuments(tenantId, TOP_DOCUMENTS_LIMIT);

        return new DashboardOverviewResponse(
                countByStatus,
                totalUsers,
                activeUsersToday,
                chatSessionsToday,
                topDocuments
        );
    }

    private Map<DocumentStatus, Long> buildStatusCountMap(UUID tenantId) {
        // Initialize all statuses to 0 so the frontend always receives a complete map —
        // avoids NPE/missing-key bugs when a status has no documents yet.
        Map<DocumentStatus, Long> result = new EnumMap<>(DocumentStatus.class);
        for (DocumentStatus s : DocumentStatus.values()) {
            result.put(s, 0L);
        }

        List<Object[]> rows = metricsRepository.countDocumentsByStatus(tenantId);
        for (Object[] row : rows) {
            DocumentStatus status = DocumentStatus.valueOf((String) row[0]);
            long count = ((Number) row[1]).longValue();
            result.put(status, count);
        }
        return result;
    }
}