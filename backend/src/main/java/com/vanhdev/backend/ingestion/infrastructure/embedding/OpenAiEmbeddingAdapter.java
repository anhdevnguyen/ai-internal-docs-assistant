package com.vanhdev.backend.ingestion.infrastructure.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vanhdev.backend.shared.config.AiProperties;
import com.vanhdev.backend.shared.exception.EmbeddingApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAiEmbeddingAdapter implements EmbeddingPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingAdapter.class);

    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 1_000L;

    private final RestClient restClient;
    private final String embeddingModel;
    private final int batchSize;

    public OpenAiEmbeddingAdapter(AiProperties props) {
        this.embeddingModel = props.embeddingModel();
        this.batchSize = props.embeddingBatchSize();
        this.restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.openaiApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>(texts.size());

        // Batch into fixed-size windows to respect OpenAI rate limits
        for (int offset = 0; offset < texts.size(); offset += batchSize) {
            List<String> batch = texts.subList(offset, Math.min(offset + batchSize, texts.size()));
            List<float[]> batchResult = embedWithRetry(batch, 0);
            results.addAll(batchResult);
        }

        return results;
    }

    private List<float[]> embedWithRetry(List<String> batch, int attempt) {
        try {
            EmbeddingApiResponse response = restClient.post()
                    .uri("/embeddings")
                    .body(new EmbeddingApiRequest(batch, embeddingModel))
                    .retrieve()
                    .body(EmbeddingApiResponse.class);

            if (response == null || response.data() == null || response.data().size() != batch.size()) {
                throw new EmbeddingApiException("OpenAI returned unexpected embedding response shape");
            }

            // OpenAI returns items ordered by their `index` field, not insertion order.
            // Sort by index to guarantee alignment with the input list.
            return response.data().stream()
                    .sorted(java.util.Comparator.comparingInt(EmbeddingDataItem::index))
                    .map(EmbeddingDataItem::embedding)
                    .toList();

        } catch (EmbeddingApiException e) {
            throw e;
        } catch (Exception e) {
            if (attempt >= MAX_RETRIES) {
                throw new EmbeddingApiException("Embedding API failed after " + MAX_RETRIES + " retries: " + e.getMessage(), e);
            }
            long backoff = BASE_BACKOFF_MS * (1L << attempt); // exponential: 1s, 2s, 4s
            log.warn("Embedding API attempt {} failed, retrying in {}ms: {}", attempt + 1, backoff, e.getMessage());
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new EmbeddingApiException("Embedding retry interrupted", ie);
            }
            return embedWithRetry(batch, attempt + 1);
        }
    }

    record EmbeddingApiRequest(List<String> input, String model) {}

    record EmbeddingApiResponse(List<EmbeddingDataItem> data) {}

    record EmbeddingDataItem(int index, @JsonProperty("embedding") float[] embedding) {}
}