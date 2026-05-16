package com.vanhdev.backend.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.chunking")
public record ChunkingProperties(int chunkSizeTokens, int overlapTokens) {}
