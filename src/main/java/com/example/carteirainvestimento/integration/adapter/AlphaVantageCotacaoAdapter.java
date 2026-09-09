package com.example.carteirainvestimento.integration.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.enums.FonteCotacao;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AlphaVantageCotacaoAdapter implements CotacaoAdapter {

    private final RestClient client;
    private final String apiKey;

    public AlphaVantageCotacaoAdapter(IntegrationProperties properties, RestClient.Builder restClientBuilder) {
        this.client = restClientBuilder.clone().baseUrl(properties.alphaVantage().baseUrl()).build();
        this.apiKey = properties.alphaVantage().apiKey();
    }

    @Override
    public boolean suporta(Mercado mercado) {
        return Mercado.EUA == mercado;
    }

    @Override
    public CotacaoConsulta buscarCotacao(String ticker) {
        try {
            AlphaVantageQuoteResponse quoteResponse = consultar("GLOBAL_QUOTE", ticker, AlphaVantageQuoteResponse.class);
            if (quoteResponse == null) {
                throw new ExternalIntegrationException("Alpha Vantage returned an empty response");
            }
            if (quoteResponse.hasRateLimit()) {
                throw new ExternalIntegrationException("Alpha Vantage rate limit reached");
            }
            if (quoteResponse.globalQuote() == null) {
                throw new ResourceNotFoundException("Ticker nÃ£o encontrado na Alpha Vantage");
            }
            AlphaVantageGlobalQuote quote = quoteResponse.globalQuote();
            validarCotacaoCompleta(quote.symbol(), quote.price(), quote.latestTradingDay());
            AlphaVantageOverviewResponse overviewResponse = consultar("OVERVIEW", ticker, AlphaVantageOverviewResponse.class);
            if (overviewResponse == null || overviewResponse.hasRateLimit()) {
                throw new ExternalIntegrationException("Alpha Vantage returned incomplete quotation data");
            }
            validarVisaoGeralCompleta(overviewResponse.name(), overviewResponse.currency());
            return new CotacaoConsulta(quote.symbol(), overviewResponse.name(), Mercado.EUA, moeda(overviewResponse.currency()),
                    new BigDecimal(quote.price()), LocalDate.parse(quote.latestTradingDay()).atStartOfDay().atOffset(ZoneOffset.UTC),
                    null, FonteCotacao.ALPHA_VANTAGE);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new ExternalIntegrationException("Falha ao consultar a Alpha Vantage");
        }
    }

    private <T> T consultar(String function, String ticker, Class<T> responseType) {
        return client.get()
                .uri(builder -> builder.path("/query")
                        .queryParam("function", function)
                        .queryParam("symbol", ticker)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(responseType);
    }

    private Moeda moeda(String currency) {
        try {
            return Moeda.valueOf(currency);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ExternalIntegrationException("Alpha Vantage retornou uma moeda invÃ¡lida");
        }
    }

    private void validarCotacaoCompleta(String symbol, String price, String latestTradingDay) {
        if (!StringUtils.hasText(symbol) || !StringUtils.hasText(price) || !StringUtils.hasText(latestTradingDay)) {
            throw new ExternalIntegrationException("Alpha Vantage returned incomplete quotation data");
        }
    }

    private void validarVisaoGeralCompleta(String name, String currency) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(currency)) {
            throw new ExternalIntegrationException("Alpha Vantage returned incomplete quotation data");
        }
    }

    private record AlphaVantageQuoteResponse(@JsonProperty("Global Quote") AlphaVantageGlobalQuote globalQuote,
                                             @JsonProperty("Information") String information,
                                             @JsonProperty("Note") String note) {
        boolean hasRateLimit() {
            return StringUtils.hasText(information) || StringUtils.hasText(note);
        }
    }

    private record AlphaVantageGlobalQuote(
            @JsonProperty("01. symbol") String symbol,
            @JsonProperty("05. price") String price,
            @JsonProperty("07. latest trading day") String latestTradingDay) {
    }

    private record AlphaVantageOverviewResponse(@JsonProperty("Name") String name,
                                                @JsonProperty("Currency") String currency,
                                                @JsonProperty("Information") String information,
                                                @JsonProperty("Note") String note) {
        boolean hasRateLimit() {
            return StringUtils.hasText(information) || StringUtils.hasText(note);
        }
    }
}
