package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** BRAPI corporate-actions adapter. The token is optional for publicly covered tickers. */
@Component
public class BrapiIncomeAdapter {
    private final RestClient client;
    private final String token;

    public BrapiIncomeAdapter(IntegrationProperties properties, RestClient.Builder builder) {
        String base = StringUtils.hasText(properties.brapi().baseUrl()) ? properties.brapi().baseUrl() : "https://brapi.dev";
        this.client = builder.clone().baseUrl(base).build();
        this.token = properties.brapi().token();
    }

    public List<YahooFinanceIncomeAdapter.DividendEvent> buscarDividendos(String ticker) {
        return buscarDividendos(List.of(ticker)).getOrDefault(ticker, List.of());
    }

    public Map<String, List<YahooFinanceIncomeAdapter.DividendEvent>> buscarDividendos(List<String> tickers) {
        if (tickers.isEmpty()) return Collections.emptyMap();
        // A rota pública legada é gratuita para os tickers cobertos e não exige
        // token. Usá-la primeiro evita que uma consulta em lote v2 limitada
        // (429) esconda os proventos dos demais ativos da carteira.
        Map<String, List<YahooFinanceIncomeAdapter.DividendEvent>> publicResult = new LinkedHashMap<>();
        for (String ticker : tickers) {
            try {
                List<YahooFinanceIncomeAdapter.DividendEvent> events = buscarDividendosLegado(ticker);
                if (!events.isEmpty()) publicResult.put(ticker, events);
            } catch (RuntimeException ignored) { }
        }
        if (!publicResult.isEmpty()) return publicResult;

        JsonNode root;
        String symbols = String.join(",", tickers);
        try {
            root = consultar(symbols, StringUtils.hasText(token));
        } catch (RuntimeException erroFonte) {
            // A rota v2 pode responder 429 para consultas em lote. A rota pública
            // legada permite consultar tickers cobertos sem token e evita que um
            // ticker sem cobertura invalide os demais.
            Map<String, List<YahooFinanceIncomeAdapter.DividendEvent>> fallback = new LinkedHashMap<>();
            for (String ticker : tickers) {
                try {
                    List<YahooFinanceIncomeAdapter.DividendEvent> events = buscarDividendosLegado(ticker);
                    if (!events.isEmpty()) fallback.put(ticker, events);
                } catch (RuntimeException ignored) {
                    // Cobertura parcial é válida: os ativos sem dados continuam sem eventos.
                }
            }
            if (!fallback.isEmpty()) return fallback;
            // Se o token rejeitado foi a causa, ainda tenta a v2 sem autenticação
            // antes de propagar a indisponibilidade para a facade.
            root = consultar(symbols, false);
        }
        Map<String, List<YahooFinanceIncomeAdapter.DividendEvent>> result = new LinkedHashMap<>();
        result.putAll(parseEventos(root, false));
        if (!result.isEmpty()) return result;

        // Resposta válida, porém sem dados em lote: tenta a rota pública por ticker.
        for (String ticker : tickers) {
            try {
                List<YahooFinanceIncomeAdapter.DividendEvent> events = buscarDividendosLegado(ticker);
                if (!events.isEmpty()) result.put(ticker, events);
            } catch (RuntimeException ignored) { }
        }
        return result;
    }

    private Map<String, List<YahooFinanceIncomeAdapter.DividendEvent>> parseEventos(JsonNode root, boolean legado) {
        return parseEventos(root, legado, null);
    }

    private Map<String, List<YahooFinanceIncomeAdapter.DividendEvent>> parseEventos(JsonNode root, boolean legado, String tickerFallback) {
        Map<String, List<YahooFinanceIncomeAdapter.DividendEvent>> result = new LinkedHashMap<>();
        JsonNode results = root.path("results");
        if (!results.isArray()) return result;
        for (JsonNode entry : results) {
            String symbol = entry.path("symbol").asText(null);
            if (!StringUtils.hasText(symbol)) symbol = tickerFallback;
            JsonNode data = legado
                    ? entry.path("dividendsData").path("cashDividends")
                    : entry.path("data").path("cashDividends");
            if (symbol == null || !data.isArray()) continue;
            List<YahooFinanceIncomeAdapter.DividendEvent> events = new ArrayList<>();
            for (JsonNode item : data) {
                BigDecimal rate = item.path("rate").isNumber() ? item.path("rate").decimalValue() : null;
                String payment = item.path("paymentDate").asText(null);
                if (rate != null && rate.signum() > 0 && payment != null) {
                    try {
                        OffsetDateTime date;
                        try { date = OffsetDateTime.parse(payment); }
                        catch (RuntimeException ignored) { date = Instant.parse(payment).atOffset(ZoneOffset.UTC); }
                        events.add(new YahooFinanceIncomeAdapter.DividendEvent(symbol, rate, date));
                    } catch (RuntimeException ignored) { }
                }
            }
            result.put(symbol, events);
        }
        return result;
    }

    private List<YahooFinanceIncomeAdapter.DividendEvent> buscarDividendosLegado(String ticker) {
        JsonNode root = consultarLegado(ticker);
        Map<String, List<YahooFinanceIncomeAdapter.DividendEvent>> parsed = parseEventos(root, true);
        if (parsed.containsKey(ticker)) return parsed.get(ticker);
        // Algumas respostas legadas omitem `symbol`; nesse caso o ticker da
        // própria requisição é a identificação segura do evento.
        return parseEventos(root, true, ticker).getOrDefault(ticker, List.of());
    }

    private JsonNode consultar(String ticker, boolean autenticado) {
        RestClient.RequestHeadersSpec<?> request = client.get().uri(uri -> uri.path("/api/v2/stocks/dividends").queryParam("symbols", ticker).build());
        if (autenticado) request = request.headers(headers -> headers.setBearerAuth(token));
        JsonNode root = request.retrieve().body(JsonNode.class);
        if (root == null) throw new IllegalStateException("Resposta vazia da BRAPI");
        return root;
    }

    private JsonNode consultarLegado(String ticker) {
        JsonNode root = client.get().uri("/api/quote/" + ticker + "?dividends=true")
                .retrieve().body(JsonNode.class);
        if (root == null) throw new IllegalStateException("Resposta vazia da BRAPI");
        return root;
    }
}
