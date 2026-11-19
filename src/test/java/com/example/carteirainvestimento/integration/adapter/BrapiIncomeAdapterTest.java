package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrapiIncomeAdapterTest {
    private RestClient.Builder builder() {
        var converter = new MappingJackson2HttpMessageConverter(new ObjectMapper()) {
            @Override public boolean canRead(Class<?> clazz, MediaType mediaType) {
                return JsonNode.class.isAssignableFrom(clazz) && super.canRead(clazz, mediaType);
            }
        };
        return RestClient.builder().messageConverters(converters -> converters.add(0, converter));
    }

    private IntegrationProperties properties(String token) {
        return new IntegrationProperties(new IntegrationProperties.Brapi("https://brapi.test", token), null, null, null, null);
    }

    @Test
    void parsesLegacyDividendEventsAndSupportsMissingSymbolWithTickerFallback() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrapiIncomeAdapter(properties("token"), builder);
        server.expect(requestTo("https://brapi.test/api/quote/PETR4?dividends=true"))
                .andRespond(withSuccess("""
                        {"results":[{"dividendsData":{"cashDividends":[
                          {"rate":1.23,"paymentDate":"2026-09-01T00:00:00Z"},
                          {"rate":0,"paymentDate":"2026-09-02T00:00:00Z"},
                          {"rate":2.00,"paymentDate":"not-a-date"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        var events = adapter.buscarDividendos("PETR4");

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().ticker()).isEqualTo("PETR4");
        assertThat(events.getFirst().valorPorAcao()).isEqualByComparingTo("1.23");
        server.verify();
    }

    @Test
    void usesV2ResponseWhenLegacyHasNoEvents() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrapiIncomeAdapter(properties("token"), builder);
        server.expect(requestTo("https://brapi.test/api/quote/VALE3?dividends=true"))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://brapi.test/api/v2/stocks/dividends?symbols=VALE3"))
                .andRespond(withSuccess("""
                        {"results":[{"symbol":"VALE3","data":{"cashDividends":[
                          {"rate":0.75,"paymentDate":"2026-09-03T00:00:00Z"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        var events = adapter.buscarDividendos(List.of("VALE3"));

        assertThat(events).containsKey("VALE3");
        assertThat(events.get("VALE3")).hasSize(1);
        assertThat(events.get("VALE3").getFirst().dataPagamento()).isNotNull();
        server.verify();
    }

    @Test
    void fallsBackToLegacyWhenV2ProviderFails() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrapiIncomeAdapter(properties("token"), builder);
        server.expect(requestTo("https://brapi.test/api/quote/ITUB4?dividends=true"))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://brapi.test/api/v2/stocks/dividends?symbols=ITUB4"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo("https://brapi.test/api/quote/ITUB4?dividends=true"))
                .andRespond(withSuccess("""
                        {"results":[{"symbol":"ITUB4","dividendsData":{"cashDividends":[
                          {"rate":0.40,"paymentDate":"2026-09-04T00:00:00Z"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        var events = adapter.buscarDividendos("ITUB4");

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().valorPorAcao()).isEqualByComparingTo("0.40");
        server.verify();
    }

    @Test
    void emptyTickerListDoesNotCallProvider() {
        RestClient.Builder builder = builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrapiIncomeAdapter(properties("token"), builder);

        assertThat(adapter.buscarDividendos(List.of())).isEmpty();
        server.verify();
    }
}
