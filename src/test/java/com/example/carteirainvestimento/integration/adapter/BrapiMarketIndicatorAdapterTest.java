package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.dto.insight.DisponibilidadeInsight;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrapiMarketIndicatorAdapterTest {
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
    void missingTokenProducesUnavailableWithoutCallingProvider() {
        RestClient.Builder builder = RestClient.builder();
        var adapter = new BrapiMarketIndicatorAdapter(properties(""), builder);
        assertThat(adapter.buscarIndicadores()).allMatch(item -> item.disponibilidade() == DisponibilidadeInsight.UNAVAILABLE);
    }

    @Test
    void unauthorizedResponseIsUnavailableAndDoesNotExposeCredentials() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrapiMarketIndicatorAdapter(properties("secret"), builder);
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=%5EBVSP"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("https://brapi.test/api/v2/macro?symbols=cdi"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("https://brapi.test/api/v2/currency?currency=USD-BRL"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThat(adapter.buscarIndicadores()).allMatch(item -> item.disponibilidade() == DisponibilidadeInsight.UNAVAILABLE);
        server.verify();
    }

    @Test
    void incompleteOrZeroProviderValuesAreMarkedUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrapiMarketIndicatorAdapter(properties("token"), builder);
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=%5EBVSP"))
                .andRespond(withSuccess("{\"results\":[{\"data\":{\"regularMarketPrice\":142350,\"regularMarketChangePercent\":1.2,\"regularMarketTime\":1725450000}}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://brapi.test/api/v2/macro?symbols=cdi"))
                .andRespond(withSuccess("{\"results\":[{\"data\":{\"value\":0,\"date\":\"2026-09-08\"}}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://brapi.test/api/v2/currency?currency=USD-BRL"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        var indicators = adapter.buscarIndicadores();
        assertThat(indicators.getFirst().disponibilidade()).isEqualTo(DisponibilidadeInsight.UNAVAILABLE);
        assertThat(indicators.get(1).disponibilidade()).isEqualTo(DisponibilidadeInsight.UNAVAILABLE);
        assertThat(indicators.get(2).disponibilidade()).isEqualTo(DisponibilidadeInsight.UNAVAILABLE);
        server.verify();
    }

    @Test
    void mapsAvailableIndicatorsAndSupportsNestedOrDirectPayloads() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrapiMarketIndicatorAdapter(properties("token"), builder);
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=%5EBVSP"))
                .andRespond(withSuccess("""
                        {"results":[{"data":{"regularMarketPrice":142350,
                        "regularMarketChangePercent":1.2,"regularMarketTime":1725450000}}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://brapi.test/api/v2/macro?symbols=cdi"))
                .andRespond(withSuccess("{\"results\":[{\"data\":{\"value\":10.5,\"date\":\"2026-09-08\"}}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://brapi.test/api/v2/currency?currency=USD-BRL"))
                .andRespond(withSuccess("""
                        {"currency":[{"data":{"bidPrice":5.42,
                        "percentageChange":0.2,"updatedAtTimestamp":1725796800}}]}
                        """, MediaType.APPLICATION_JSON));

        var result = adapter.buscarIndicadores();

        assertThat(result).allMatch(item -> item.disponibilidade() == DisponibilidadeInsight.AVAILABLE);
        assertThat(result.get(0).valor()).isEqualByComparingTo("142350");
        assertThat(result.get(1).valor()).isEqualByComparingTo("10.5");
        assertThat(result.get(2).valor()).isEqualByComparingTo("5.42");
        server.verify();
    }

    @Test
    void marksMalformedTimestampAndEmptyResponseUnavailable() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrapiMarketIndicatorAdapter(properties("token"), builder);
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=%5EBVSP"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://brapi.test/api/v2/macro?symbols=cdi"))
                .andRespond(withSuccess("{\"results\":[{\"data\":{\"value\":9.1,\"date\":\"not-a-date\"}}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://brapi.test/api/v2/currency?currency=USD-BRL"))
                .andRespond(withSuccess("{\"currency\":[]}", MediaType.APPLICATION_JSON));

        var result = adapter.buscarIndicadores();

        assertThat(result.get(0).disponibilidade()).isEqualTo(DisponibilidadeInsight.UNAVAILABLE);
        assertThat(result.get(1).disponibilidade()).isEqualTo(DisponibilidadeInsight.AVAILABLE);
        assertThat(result.get(1).referenciaEm()).isEqualTo(java.time.OffsetDateTime.parse("1970-01-01T00:00:00Z"));
        assertThat(result.get(2).disponibilidade()).isEqualTo(DisponibilidadeInsight.UNAVAILABLE);
        server.verify();
    }
}
