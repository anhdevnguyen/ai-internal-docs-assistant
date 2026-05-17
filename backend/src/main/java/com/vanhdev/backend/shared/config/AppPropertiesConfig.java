package com.vanhdev.backend.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableConfigurationProperties({
        AiProperties.class,
        ChunkingProperties.class,
        StorageProperties.class,
        CorsProperties.class
})
public class AppPropertiesConfig {}