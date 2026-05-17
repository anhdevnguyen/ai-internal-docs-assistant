package com.vanhdev.backend.admin.infrastructure;

import com.vanhdev.backend.admin.api.dto.AdminDtos.TopDocumentEntry;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Dedicated read-model repository for admin analytics queries.
 * Kept separate from domain repositories intentionally — these are aggregate
 * read queries that cross module boundaries (chat_messages → document_chunks → documents)
 * and do not belong in any single domain's repository.
 * All queries are native SQL. JPQL cannot express unnest() or window functions,
 * and the performance characteristics of these queries must be explicit.
 */
@Repository
public class AdminMetricsRepository {

    private final EntityManager em;

    public AdminMetricsRepository(EntityManager em) {
        this.em = em;
    }

    /**
     * Counts documents grouped by status for the given tenant.
     * Returns List<Object[]> where [0]=status string, [1]=count long.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> countDocumentsByStatus(UUID tenantId) {
        return (List<Object[]>) em.createNativeQuery("""
                SELECT status, COUNT(id)
                FROM documents
                WHERE tenant_id = :tenantId
                GROUP BY status
                """)
                .setParameter("tenantId", tenantId)
                .getResultList();
    }

    /**
     * Counts total users for the tenant.
     */
    public long countUsers(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(id) FROM users WHERE tenant_id = :tenantId
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    /**
     * Counts distinct users who sent at least one chat message today (UTC).
     * Joins chat_sessions to scope by tenant — chat_messages itself is not tenant-keyed.
     */
    public long countActiveUsersToday(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(DISTINCT cs.user_id)
                FROM chat_sessions cs
                WHERE cs.tenant_id = :tenantId
                  AND cs.created_at >= current_date
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    /**
     * Counts chat sessions created today for the tenant.
     */
    public long countChatSessionsToday(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(id)
                FROM chat_sessions
                WHERE tenant_id = :tenantId
                  AND created_at >= current_date
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    /**
     * Returns the top-N documents by how many times their chunks appeared
     * in assistant message retrieval sets over the last 30 days.
     * The unnest() call expands uuid[] into individual rows — this is the
     * correct approach for array columns in PostgreSQL. No ORM can express this.
     * Scoped to tenant via documents.tenant_id — no cross-tenant leakage
     * even though chat_messages has no direct tenant_id column.
     */
    public List<TopDocumentEntry> findTopRetrievedDocuments(UUID tenantId, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                WITH unnested_chunks AS (
                    SELECT unnest(cm.retrieved_chunk_ids) AS chunk_id
                    FROM chat_messages cm
                    JOIN chat_sessions cs ON cm.session_id = cs.id
                    WHERE cs.tenant_id = :tenantId
                      AND cm.created_at > current_date - interval '30 days'
                      AND cm.retrieved_chunk_ids IS NOT NULL
                      AND cardinality(cm.retrieved_chunk_ids) > 0
                )
                SELECT d.id, d.title, COUNT(u.chunk_id) AS hit_count
                FROM unnested_chunks u
                JOIN document_chunks dc ON u.chunk_id = dc.id
                JOIN documents d ON dc.document_id = d.id
                WHERE d.tenant_id = :tenantId
                GROUP BY d.id, d.title
                ORDER BY hit_count DESC
                LIMIT :limit
                """)
                .setParameter("tenantId", tenantId)
                .setParameter("limit", limit)
                .getResultList();

        return rows.stream()
                .map(row -> new TopDocumentEntry(
                        (UUID) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }
}