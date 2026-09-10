package com.example.carteirainvestimento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrations")
public record IntegrationProperties(
        Brapi brapi,
        AlphaVantage alphaVantage,
        TwelveData twelveData,
        BrasilApi brasilApi,
        ViaCep viaCep,
        FinancialInstitution financialInstitution) {

    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public IntegrationProperties(Brapi brapi, AlphaVantage alphaVantage, TwelveData twelveData,
            BrasilApi brasilApi, ViaCep viaCep, FinancialInstitution financialInstitution) {
        this.brapi = brapi;
        this.alphaVantage = alphaVantage;
        this.twelveData = twelveData;
        this.brasilApi = brasilApi;
        this.viaCep = viaCep;
        this.financialInstitution = financialInstitution;
    }

    public record Brapi(String baseUrl, String token) {
    }

    public record AlphaVantage(String baseUrl, String apiKey) {
    }
    public record TwelveData(String baseUrl, String apiKey) {
    }

    /** Compatibilidade com fixtures e integrações que ainda não precisam da Twelve Data. */
    public IntegrationProperties(Brapi brapi, AlphaVantage alphaVantage, BrasilApi brasilApi,
            ViaCep viaCep, FinancialInstitution financialInstitution) {
        this(brapi, alphaVantage, null, brasilApi, viaCep, financialInstitution);
    }

    public record BrasilApi(String baseUrl) {
    }

    public record ViaCep(String baseUrl) {
    }

    public record FinancialInstitution(String baseUrl) {
    }
}
