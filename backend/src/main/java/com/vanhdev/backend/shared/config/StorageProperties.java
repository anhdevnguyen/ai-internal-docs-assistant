package com.vanhdev.backend.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties("app.storage")
public record StorageProperties(
        String basePath,
        int maxFileSizeMb,
        List<String> allowedMimeTypes
) {}