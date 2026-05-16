package com.vanhdev.backend.ingestion.infrastructure.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vanhdev.backend.shared.config.AiProperties;
import com.vanhdev.backend.shared.exception.EmbeddingApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class OpenAiEmbeddingAdapter implements EmbeddingPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingAdapter.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1_000;

    private final RestClient restClient;
    private final AiProperties aiProperties;

    public OpenAiEmbeddingAdapter(RestClient.Builder restClientBuilder, AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = restClientBuilder
                .baseUrl(aiProperties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + aiProperties.openaiApiKey())
                .build();
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        int batchSize = aiProperties.embeddingBatchSize();
        List<float[]> result = new ArrayList<>(texts.size());

        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            result.addAll(callWithRetry(batch));
        }

        return result;
    }

    private List<float[]> callWithRetry(List<String> batch) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return callApi(batch);
            } catch (EmbeddingApiException e) {
                if (e.isRateLimited() && attempt < MAX_RETRIES - 1) {
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << attempt); // exponential: 1s, 2s, 4s
                    log.warn("OpenAI rate limit hit, backing off {}ms (attempt {}/{})", backoffMs, attempt + 1, MAX_RETRIES);
                    sleep(Duration.ofMillis(backoffMs));
                } else {
                    throw e;
                }
            }
        }
        throw new EmbeddingApiException("Max retries exceeded calling OpenAI embeddings API", false);
    }

    private List<float[]> callApi(List<String> texts) {
        try {
            EmbeddingApiResponse response = restClient.post()
                    .uri("/embeddings")
                    .body(new EmbeddingApiRequest(aiProperties.embeddingModel(), texts))
                    .retrieve()
                    .body(EmbeddingApiResponse.class);

            if (response == null || response.data() == null) {
                throw new EmbeddingApiException("Empty response from OpenAI embeddings API", false);
            }

            // Sort by index to guarantee ordering matches input order
            return response.data().stream()
                    .sorted(Comparator.comparingInt(EmbeddingDataItem::index))
                    .map(item -> toFloatArray(item.embedding()))
                    .toList();

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new EmbeddingApiException("OpenAI rate limit (429)", true);
            }
            throw new EmbeddingApiException(
                    "OpenAI API error %d: %s".formatted(e.getStatusCode().value(), e.getResponseBodyAsString()),
                    false);
        }
    }

    private float[] toFloatArray(List<Double> doubles) {
        float[] arr = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            arr[i] = doubles.get(i).floatValue();
        }
        return arr;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingApiException("Interrupted during retry backoff", false);
        }
    }

    // --- API contract types (private — not part of the domain) ---

    private record EmbeddingApiRequest(String model, List<String> input) {}

    private record EmbeddingDataItem(
            @JsonProperty("embedding") List<Double> embedding,
            @JsonProperty("index") int index) {}

    private record EmbeddingApiResponse(@JsonProperty("data") List<EmbeddingDataItem> data) {}
}