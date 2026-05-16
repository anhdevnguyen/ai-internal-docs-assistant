package com.vanhdev.backend.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({StorageProperties.class, ChunkingProperties.class, AiProperties.class})
public class AppPropertiesConfig {}
