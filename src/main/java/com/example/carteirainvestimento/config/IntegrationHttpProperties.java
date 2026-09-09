package com.example.carteirainvestimento.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.http")
public record IntegrationHttpProperties(Duration connectTimeout, Duration readTimeout) {
    public IntegrationHttpProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(10) : readTimeout;
    }
}
