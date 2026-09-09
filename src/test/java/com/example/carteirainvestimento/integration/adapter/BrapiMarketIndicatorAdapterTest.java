package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.dto.insight.DisponibilidadeInsight;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrapiMarketIndicatorAdapterTest {
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
}
