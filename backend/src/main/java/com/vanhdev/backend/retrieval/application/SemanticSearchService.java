package com.vanhdev.backend.retrieval.application;

import com.vanhdev.backend.ingestion.infrastructure.embedding.EmbeddingPort;
import com.vanhdev.backend.retrieval.domain.RetrievedChunk;
import com.vanhdev.backend.retrieval.domain.RetrievalQuery;
import com.vanhdev.backend.retrieval.infrastructure.VectorSearchRepository;
import com.vanhdev.backend.shared.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Translates a natural-language question into a list of relevant document chunks.
 * This service does NOT know about chat sessions or prompt construction —
 * it is a pure semantic search component consumed by ChatOrchestrationService.
 * Keeping retrieval isolated means we can swap strategies (hybrid search, re-ranking)
 * without touching the chat module.
 */
@Service
public class SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);

    private final EmbeddingPort embeddingPort;
    private final VectorSearchRepository vectorSearchRepository;
    private final AiProperties aiProperties;

    public SemanticSearchService(EmbeddingPort embeddingPort,
                                 VectorSearchRepository vectorSearchRepository,
                                 AiProperties aiProperties) {
        this.embeddingPort = embeddingPort;
        this.vectorSearchRepository = vectorSearchRepository;
        this.aiProperties = aiProperties;
    }

    /**
     * Embeds the question and retrieves the most semantically relevant chunks
     * scoped strictly to the given tenant.
     * Returns an empty list (not exception) when no chunks meet the similarity threshold —
     * the chat orchestrator handles the "no context found" case gracefully.
     */
    public List<RetrievedChunk> findRelevantChunks(String question, UUID tenantId) {
        float[] questionEmbedding = embedQuestion(question);

        RetrievalQuery query = new RetrievalQuery(
                tenantId,
                questionEmbedding,
                aiProperties.retrievalTopK(),
                aiProperties.retrievalMinSimilarity()
        );

        List<RetrievedChunk> chunks = vectorSearchRepository.findSimilarChunks(
                query.tenantId(),
                query.questionEmbedding(),
                query.topK(),
                query.minSimilarity()
        );

        log.debug("Retrieval [tenantId={}, question_length={}, chunks_found={}]",
                tenantId, question.length(), chunks.size());

        return chunks;
    }

    private float[] embedQuestion(String question) {
        // EmbeddingPort.embedBatch preserves order — index 0 is our question
        List<float[]> embeddings = embeddingPort.embedBatch(List.of(question));
        return embeddings.get(0);
    }
}