package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.dto.insight.DisponibilidadeInsight;
import com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** BRAPI boundary: credentials remain server-side and provider payloads are normalized here. */
@Component
@ConditionalOnProperty(name = "integrations.brapi.indicators-enabled", havingValue = "true")
public class BrapiMarketIndicatorAdapter implements MarketIndicatorAdapter {
    private static final Logger log = LoggerFactory.getLogger(BrapiMarketIndicatorAdapter.class);
    private final RestClient client;
    private final String token;
    private final boolean configured;

    public BrapiMarketIndicatorAdapter(IntegrationProperties properties, RestClient.Builder builder) {
        this.token = properties.brapi().token();
        this.configured = StringUtils.hasText(properties.brapi().baseUrl()) && StringUtils.hasText(token);
        this.client = builder.clone().baseUrl(properties.brapi().baseUrl()).build();
    }

    @Override
    public List<IndicadorMercadoResponse> buscarIndicadores() {
        if (!configured) return List.of(indisponivel("IBOV", "Ibovespa"), indisponivel("CDI", "CDI"), indisponivel("USD", "Dólar comercial"));
        return List.of(ibovespa(), cdi(), dolar());
    }

    private IndicadorMercadoResponse ibovespa() {
        try {
            JsonNode root = get(uri -> uri.path("/api/v2/stocks/quote").queryParam("symbols", "^BVSP").build());
            JsonNode quote = first(root, "results");
            return disponivel("IBOV", "Ibovespa", decimal(quote, "regularMarketPrice"), decimal(quote, "regularMarketChangePercent"), timestamp(quote, "regularMarketTime"));
        } catch (RuntimeException exception) { return falhou("IBOV", "Ibovespa", exception); }
    }

    private IndicadorMercadoResponse dolar() {
        try {
            JsonNode root = get(uri -> uri.path("/api/v2/currency").queryParam("currency", "USD-BRL").build());
            JsonNode currency = first(root, "currency");
            return disponivel("USD", "Dólar comercial", decimal(currency, "bidPrice"), decimal(currency, "percentageChange"), timestamp(currency, "updatedAtTimestamp"));
        } catch (RuntimeException exception) { return falhou("USD", "Dólar comercial", exception); }
    }

    private IndicadorMercadoResponse cdi() {
        try {
            JsonNode root = get(uri -> uri.path("/api/v2/macro").queryParam("symbols", "cdi").build());
            JsonNode cdi = first(root, "results");
            if (cdi.isMissingNode()) cdi = root;
            return disponivel("CDI", "CDI", decimal(cdi, "value"), null, timestamp(cdi, "date"));
        } catch (RuntimeException exception) { return falhou("CDI", "CDI", exception); }
    }

    private JsonNode get(java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uri) {
        JsonNode response = client.get().uri(uri::apply).headers(headers -> headers.setBearerAuth(token)).retrieve().body(JsonNode.class);
        if (response == null) throw new IllegalStateException("Resposta vazia do provedor");
        return response;
    }

    private IndicadorMercadoResponse disponivel(String codigo, String descricao, BigDecimal valor, BigDecimal variacao, OffsetDateTime referencia) {
        if (valor == null || valor.signum() == 0) throw new IllegalStateException("Valor inválido do provedor");
        return new IndicadorMercadoResponse(codigo, descricao, valor, variacao, referencia, DisponibilidadeInsight.AVAILABLE);
    }
    private IndicadorMercadoResponse falhou(String codigo, String descricao, RuntimeException exception) {
        log.warn("Indicador {} temporariamente indisponível: {}", codigo, exception.getClass().getSimpleName());
        return indisponivel(codigo, descricao);
    }
    private IndicadorMercadoResponse indisponivel(String codigo, String descricao) { return new IndicadorMercadoResponse(codigo, descricao, null, null, null, DisponibilidadeInsight.UNAVAILABLE); }
    private JsonNode first(JsonNode root, String field) { JsonNode node = root.path(field); JsonNode item = node.isArray() && !node.isEmpty() ? node.get(0) : node; JsonNode data = item.path("data"); return data.isObject() ? data : item; }
    private BigDecimal decimal(JsonNode node, String field) { JsonNode value = node.path(field); return value.isNumber() || value.isTextual() ? value.decimalValue() : null; }
    private OffsetDateTime timestamp(JsonNode node, String field) { JsonNode value = node.path(field); if (value.isNumber() || value.isTextual()) { try { return OffsetDateTime.ofInstant(Instant.ofEpochSecond(value.asLong()), ZoneOffset.UTC); } catch (RuntimeException ignored) { try { return OffsetDateTime.parse(value.asText()); } catch (RuntimeException ignoredAgain) { return null; } } } return null; }
}
