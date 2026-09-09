package com.example.carteirainvestimento.integration.adapter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.enums.FonteCotacao;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class BrapiCotacaoAdapter implements CotacaoAdapter {

    private final RestClient client;
    private final String token;

    public BrapiCotacaoAdapter(IntegrationProperties properties, RestClient.Builder restClientBuilder) {
        this.client = restClientBuilder.clone().baseUrl(properties.brapi().baseUrl()).build();
        this.token = properties.brapi().token();
    }

    @Override
    public boolean suporta(Mercado mercado) {
        // A BRAPI também cobre símbolos internacionais sem exigir token em
        // vários casos. A validação do mercado é feita após ler a moeda/bolsa.
        return mercado == Mercado.BRASIL || mercado == Mercado.EUA;
    }

    @Override
    public CotacaoConsulta buscarCotacao(String ticker) {
        try {
            BrapiResponse response = client.get()
                    .uri(builder -> builder.path("/api/v2/stocks/quote").queryParam("symbols", ticker).build())
                    .headers(headers -> {
                        if (StringUtils.hasText(token)) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    .body(BrapiResponse.class);
            if (response == null) {
                throw new ExternalIntegrationException("BRAPI returned an empty response");
            }
            if (Boolean.TRUE.equals(response.error())) {
                throw new ExternalIntegrationException("BRAPI rejected the quotation request");
            }
            if (response.results() == null || response.results().isEmpty()) {
                throw new ResourceNotFoundException("Ticker nÃ£o encontrado na BRAPI");
            }

            BrapiQuote quote = response.results().getFirst();
            String longName = quote.data() == null ? quote.longName() : quote.data().longName();
            String currency = quote.data() == null ? quote.currency() : quote.data().currency();
            BigDecimal price = quote.data() == null ? quote.regularMarketPrice() : quote.data().regularMarketPrice();
            OffsetDateTime marketTime = quote.data() == null ? quote.regularMarketTime() : quote.data().regularMarketTime();
            validarRespostaCompleta(quote.symbol(), longName, currency, price, marketTime);
            Moeda quoteCurrency = moeda(currency);
            Mercado detectedMarket = quoteCurrency == Moeda.BRL ? Mercado.BRASIL : Mercado.EUA;
            String exchange = quote.exchangeName() != null ? quote.exchangeName() : quote.exchange();
            return new CotacaoConsulta(quote.symbol(), longName, detectedMarket, quoteCurrency, price, marketTime,
                    logoHttps(quote.logoUrl()), FonteCotacao.BRAPI,
                    detectedMarket == Mercado.BRASIL ? "BR" : "US", exchange, quote.exchangeMic());
        } catch (RestClientException | HttpMessageConversionException exception) {
            String reason = exception.getMessage() == null ? "" : exception.getMessage();
            if (reason.contains("502") || reason.contains("500") || reason.contains("503")) return buscarLegado(ticker);
            throw new ExternalIntegrationException("Falha ao consultar a BRAPI");
        }
    }

    private CotacaoConsulta buscarLegado(String ticker) {
        try {
            JsonNode root = client.get().uri("/api/quote/" + ticker).retrieve().body(JsonNode.class);
            JsonNode quote = root == null ? null : root.path("results").path(0);
            String symbol = quote == null ? null : quote.path("symbol").asText(null);
            String name = quote == null ? null : quote.path("longName").asText(null);
            String currency = quote == null ? null : quote.path("currency").asText(null);
            BigDecimal price = quote != null && quote.path("regularMarketPrice").isNumber() ? quote.path("regularMarketPrice").decimalValue() : null;
            OffsetDateTime time = quote != null && quote.path("regularMarketTime").isTextual() ? OffsetDateTime.parse(quote.path("regularMarketTime").asText()) : null;
            validarRespostaCompleta(symbol, name, currency, price, time);
            Moeda quoteCurrency = moeda(currency);
            Mercado detectedMarket = quoteCurrency == Moeda.BRL ? Mercado.BRASIL : Mercado.EUA;
            String exchange = quote.path("exchangeName").asText(quote.path("exchange").asText(null));
            return new CotacaoConsulta(symbol, name, detectedMarket, quoteCurrency, price, time,
                    logoHttps(quote.path("logourl").asText(null)), FonteCotacao.BRAPI,
                    detectedMarket == Mercado.BRASIL ? "BR" : "US", exchange, quote.path("exchangeMic").asText(null));
        } catch (RuntimeException ex) {
            throw new ExternalIntegrationException("Falha ao consultar a BRAPI");
        }
    }

    private Moeda moeda(String currency) {
        try {
            return Moeda.valueOf(currency);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ExternalIntegrationException("BRAPI retornou uma moeda invÃ¡lida");
        }
    }

    private void validarRespostaCompleta(String symbol, String longName, String currency, BigDecimal price,
                                         OffsetDateTime marketTime) {
        if (!StringUtils.hasText(symbol) || !StringUtils.hasText(longName) || !StringUtils.hasText(currency)
                || price == null || marketTime == null) {
            throw new ExternalIntegrationException("BRAPI returned incomplete quotation data");
        }
    }

    private String logoHttps(String logoUrl) {
        return StringUtils.hasText(logoUrl) && logoUrl.trim().toLowerCase(java.util.Locale.ROOT).startsWith("https://")
                ? logoUrl.trim() : null;
    }

    private record BrapiResponse(List<BrapiQuote> results, Boolean error) {
    }

    private record BrapiQuote(String symbol, String longName, String currency, String logourl, BigDecimal regularMarketPrice,
                              OffsetDateTime regularMarketTime, String exchange, String exchangeName, String exchangeMic,
                              BrapiMarketData data) {
        String logoUrl() { return logourl; }
    }

    private record BrapiMarketData(String longName, String currency, BigDecimal regularMarketPrice,
                                   OffsetDateTime regularMarketTime) {
    }
}
