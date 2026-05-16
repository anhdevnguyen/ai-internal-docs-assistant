package com.vanhdev.backend.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai")
public record AiProperties(
        String openaiApiKey,
        String embeddingModel,
        int embeddingBatchSize,
        String baseUrl
) {}