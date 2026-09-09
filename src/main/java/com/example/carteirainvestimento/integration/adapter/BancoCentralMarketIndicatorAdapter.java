package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.dto.insight.DisponibilidadeInsight;
import com.example.carteirainvestimento.dto.insight.IndicadorMercadoResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Uses public Banco Central SGS series: CDI 4389 and USD commercial selling rate 1. */
@Component
public class BancoCentralMarketIndicatorAdapter implements MarketIndicatorAdapter {
    private static final Logger log = LoggerFactory.getLogger(BancoCentralMarketIndicatorAdapter.class);
    private final RestClient client;
    private final RestClient yahooClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BancoCentralMarketIndicatorAdapter(RestClient.Builder builder) {
        this.client = builder.clone().baseUrl("https://api.bcb.gov.br").build();
        this.yahooClient = builder.clone().baseUrl("https://query1.finance.yahoo.com").build();
    }

    @Override
    public List<IndicadorMercadoResponse> buscarIndicadores() {
        return List.of(ibovespa(), serie("CDI", "CDI", 4389), serie("USD", "Dólar comercial", 1));
    }

    private IndicadorMercadoResponse ibovespa() {
        try {
            JsonNode meta = yahooClient.get().uri("/v8/finance/chart/%5EBVSP?range=1mo&interval=1d")
                    .retrieve().body(JsonNode.class).path("chart").path("result").path(0).path("meta");
            BigDecimal value = meta.path("regularMarketPrice").decimalValue();
            BigDecimal previous = meta.path("previousClose").decimalValue();
            if (value.signum() == 0 || previous.signum() == 0) throw new IllegalStateException("Índice sem cotação válida");
            BigDecimal change = value.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 4, java.math.RoundingMode.HALF_UP);
            return new IndicadorMercadoResponse("IBOV", "Ibovespa", value, change,
                    OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(meta.path("regularMarketTime").asLong()), ZoneOffset.UTC), DisponibilidadeInsight.AVAILABLE);
        } catch (Exception exception) { return indisponivel("IBOV", "Ibovespa"); }
    }

    private IndicadorMercadoResponse serie(String codigo, String descricao, int serie) {
        try {
            String body = client.get().uri("/dados/serie/bcdata.sgs.{serie}/dados/ultimos/1?formato=json", serie).retrieve().body(String.class);
            JsonNode values = body == null ? null : objectMapper.readTree(body);
            if (values == null || !values.isArray() || values.isEmpty()) throw new IllegalStateException("Série sem observação");
            JsonNode value = values.get(0); BigDecimal numero = new BigDecimal(value.path("valor").asText().replace(',', '.'));
            if (numero.signum() == 0) throw new IllegalStateException("Valor zero não é cotação válida");
            LocalDate data = LocalDate.parse(value.path("data").asText(), DateTimeFormatter.ofPattern("dd/MM/uuuu"));
            return new IndicadorMercadoResponse(codigo, descricao, numero, null, OffsetDateTime.of(data.atStartOfDay(), ZoneOffset.ofHours(-3)), DisponibilidadeInsight.AVAILABLE);
        } catch (Exception exception) {
            log.warn("Indicador {} temporariamente indisponível: {}", codigo, exception.getClass().getSimpleName());
            return indisponivel(codigo, descricao);
        }
    }
    private IndicadorMercadoResponse indisponivel(String codigo, String descricao) { return new IndicadorMercadoResponse(codigo, descricao, null, null, null, DisponibilidadeInsight.UNAVAILABLE); }
}
