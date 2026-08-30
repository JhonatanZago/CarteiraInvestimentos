package com.example.carteirainvestimento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrations")
public record IntegrationProperties(
        Brapi brapi,
        AlphaVantage alphaVantage,
        BrasilApi brasilApi,
        ViaCep viaCep,
        FinancialInstitution financialInstitution) {

    public record Brapi(String baseUrl, String token) {
    }

    public record AlphaVantage(String baseUrl, String apiKey) {
    }

    public record BrasilApi(String baseUrl) {
    }

    public record ViaCep(String baseUrl) {
    }

    public record FinancialInstitution(String baseUrl) {
    }
}
