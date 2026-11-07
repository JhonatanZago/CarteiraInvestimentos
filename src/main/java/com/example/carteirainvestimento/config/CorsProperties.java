package com.example.carteirainvestimento.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders) {

    public CorsProperties {
        allowedOrigins = normalized(allowedOrigins);
        allowedMethods = normalized(allowedMethods);
        allowedHeaders = normalized(allowedHeaders);
    }

    private static List<String> normalized(List<String> values) {
        return values == null ? List.of() : values.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
