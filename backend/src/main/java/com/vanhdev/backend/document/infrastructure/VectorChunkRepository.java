package com.vanhdev.backend.document.infrastructure;

import com.vanhdev.backend.ingestion.domain.TextChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Handles document chunk persistence including the embedding (vector) column.
 * Spring Data JPA cannot handle the pgvector `vector` type natively, so we
 * use JdbcTemplate with CAST(? AS vector) for explicit type control.
 */
@Repository
public class VectorChunkRepository {

    private static final int INSERT_BATCH_SIZE = 50;

    private final JdbcTemplate jdbcTemplate;

    public VectorChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveAll(UUID tenantId, UUID documentId, List<TextChunk> chunks, List<float[]> embeddings) {
        String sql = """
                INSERT INTO document_chunks (id, document_id, tenant_id, chunk_index, content, embedding, token_count, created_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS vector), ?, NOW())
                """;

        jdbcTemplate.batchUpdate(sql, chunks, INSERT_BATCH_SIZE, (ps, chunk) -> {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, documentId);
            ps.setObject(3, tenantId);
            ps.setInt(4, chunk.index());
            ps.setString(5, chunk.content());
            ps.setString(6, toVectorLiteral(embeddings.get(chunk.index())));
            ps.setInt(7, chunk.estimatedTokenCount());
        });
    }

    // PostgreSQL vector literal format: [0.1,0.2,0.3]
    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        return sb.append("]").toString();
    }

    /**
     * Bulk-deletes all chunks belonging to a document.
     * Called before hard-deleting a document and before force re-indexing (to prevent
     * duplicate embeddings). Spring Data derives the DELETE query from the method name.
     */
    public void deleteByDocumentId(UUID documentId);
}
