package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.enums.Mercado;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YahooFinanceIncomeAdapterTest {
    private RestClient.Builder builder() {
        var converter = new MappingJackson2HttpMessageConverter(new ObjectMapper()) {
            @Override public boolean canRead(Class<?> clazz, MediaType mediaType) {
                return JsonNode.class.isAssignableFrom(clazz) && super.canRead(clazz, mediaType);
            }
        };
        return RestClient.builder().messageConverters(converters -> converters.add(0, converter));
    }

    @Test
    void parsesOnlyPositiveDividendEventsWithValidTimestamps() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new YahooFinanceIncomeAdapter(builder);
        server.expect(request -> {
                    assertThat(request.getURI().getPath()).isEqualTo("/v8/finance/chart/PETR4.SA");
                    assertThat(request.getURI().getQuery()).contains("events=div");
                })
                .andRespond(withSuccess("""
                        {"chart":{"result":[{"events":{"dividends":{
                          "a":{"amount":0.50,"date":1725450000},
                          "b":{"amount":0,"date":1725450000},
                          "c":{"amount":1.20,"date":0},
                          "d":{"amount":-1,"date":1725450000}}}}]}}
                        """, MediaType.APPLICATION_JSON));

        var events = adapter.buscarDividendos("PETR4", Mercado.BRASIL,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().ticker()).isEqualTo("PETR4");
        assertThat(events.getFirst().valorPorAcao()).isEqualByComparingTo("0.50");
        server.verify();
    }

    @Test
    void returnsEmptyWhenChartOrDividendsAreMissing() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new YahooFinanceIncomeAdapter(builder);
        server.expect(request -> request.getURI().getPath().endsWith("/AAPL"))
                .andRespond(withSuccess("{\"chart\":{\"result\":[]}}", MediaType.APPLICATION_JSON));

        assertThat(adapter.buscarDividendos("AAPL", Mercado.EUA,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).isEmpty();
        server.verify();
    }
}
