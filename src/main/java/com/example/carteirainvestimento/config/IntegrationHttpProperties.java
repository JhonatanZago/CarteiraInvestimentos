package com.example.carteirainvestimento.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.http")
public record IntegrationHttpProperties(Duration connectTimeout, Duration readTimeout) {
}
