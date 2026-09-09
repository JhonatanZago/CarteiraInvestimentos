package com.example.carteirainvestimento.integration.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.carteirainvestimento.enums.Mercado;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Public Yahoo Finance boundary used only by the backend for historical dividend events. */
@Component
public class YahooFinanceIncomeAdapter {
    private final RestClient client;

    public YahooFinanceIncomeAdapter(RestClient.Builder builder) {
        this.client = builder.clone().baseUrl("https://query1.finance.yahoo.com").build();
    }

    public List<DividendEvent> buscarDividendos(String ticker, Mercado mercado, LocalDate inicio, LocalDate fim) {
        String symbol = mercado == Mercado.BRASIL && !ticker.endsWith(".SA") ? ticker + ".SA" : ticker;
        JsonNode root = client.get().uri(uri -> uri.path("/v8/finance/chart/{ticker}")
                .queryParam("period1", inicio.atStartOfDay().toEpochSecond(ZoneOffset.UTC))
                .queryParam("period2", fim.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC))
                .queryParam("events", "div").build(symbol)).retrieve().body(JsonNode.class);
        JsonNode dividends = root == null ? null : root.path("chart").path("result").path(0).path("events").path("dividends");
        if (dividends == null || !dividends.isObject()) return List.of();
        List<DividendEvent> events = new ArrayList<>();
        dividends.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            BigDecimal amount = value.path("amount").isNumber() ? value.path("amount").decimalValue() : null;
            long timestamp = value.path("date").asLong(0);
            if (amount != null && amount.signum() > 0 && timestamp > 0) {
                events.add(new DividendEvent(ticker, amount, OffsetDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC)));
            }
        });
        return events;
    }

    public record DividendEvent(String ticker, BigDecimal valorPorAcao, OffsetDateTime dataPagamento) { }
}
