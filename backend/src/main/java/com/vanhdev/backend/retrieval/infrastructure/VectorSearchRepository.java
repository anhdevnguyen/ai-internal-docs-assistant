package com.vanhdev.backend.retrieval.infrastructure;

import com.vanhdev.backend.retrieval.domain.RetrievedChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Performs ANN (approximate nearest neighbor) vector search against pgvector.
 * Kept separate from VectorChunkRepository (write side) intentionally:
 * ingestion writes chunks; retrieval reads them. Different responsibilities,
 * different lifecycles. CQRS boundary at the infrastructure level.
 * tenant_id filter is enforced here — NOT in the service layer — as defense-in-depth.
 * A bug in the service cannot cause cross-tenant data leakage.
 */
@Repository
public class VectorSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    public VectorSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns the top-K most similar chunks for a given tenant, ordered by cosine similarity descending.
     * The HNSW index on embedding (vector_cosine_ops) makes this sub-millisecond at typical corpus sizes.
     *
     * @param tenantId       enforced at query level — no cross-tenant results possible
     * @param queryEmbedding vector of the user's question (must match training dimensions: 1536)
     * @param topK           maximum number of chunks to return
     * @param minSimilarity  cosine similarity floor — chunks below this are excluded before returning
     */
    public List<RetrievedChunk> findSimilarChunks(
            UUID tenantId,
            float[] queryEmbedding,
            int topK,
            double minSimilarity
    ) {
        // Fetch slightly more than topK, then filter by minSimilarity in Java.
        // Reason: pgvector's ORDER BY embedding <=> ... returns cosine *distance* (0=identical, 1=orthogonal),
        // but we expose similarity (1 - distance). Filtering in DB with a WHERE on computed column
        // would prevent index usage; filtering after fetch is safe at these result set sizes.
        String sql = """
                SELECT
                    dc.id              AS chunk_id,
                    dc.document_id,
                    d.title            AS document_title,
                    dc.content,
                    1 - (dc.embedding <=> CAST(? AS vector)) AS similarity_score
                FROM document_chunks dc
                JOIN documents d ON d.id = dc.document_id
                WHERE dc.tenant_id = ?
                ORDER BY dc.embedding <=> CAST(? AS vector)
                LIMIT ?
                """;

        String vectorLiteral = toVectorLiteral(queryEmbedding);

        List<RetrievedChunk> candidates = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new RetrievedChunk(
                        rs.getObject("chunk_id", UUID.class),
                        rs.getObject("document_id", UUID.class),
                        rs.getString("document_title"),
                        rs.getString("content"),
                        rs.getDouble("similarity_score")
                ),
                vectorLiteral,
                tenantId,
                vectorLiteral,
                topK
        );

        // Filter out low-quality matches that would pollute the prompt context
        return candidates.stream()
                .filter(chunk -> chunk.similarityScore() >= minSimilarity)
                .toList();
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        return sb.append("]").toString();
    }
}