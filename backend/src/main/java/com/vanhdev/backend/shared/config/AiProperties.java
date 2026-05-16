package com.vanhdev.backend.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai")
public record AiProperties(
        String openaiApiKey,
        String baseUrl,

        // Embedding
        String embeddingModel,
        int embeddingBatchSize,

        // Chat completion
        String chatModel,
        int chatMaxTokens,
        double chatTemperature,
        int retrievalTopK,
        double retrievalMinSimilarity
) {}