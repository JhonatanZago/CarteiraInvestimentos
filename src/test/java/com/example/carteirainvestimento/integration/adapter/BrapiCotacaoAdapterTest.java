package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrapiCotacaoAdapterTest {

    private MockRestServiceServer server;
    private BrapiCotacaoAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BrapiCotacaoAdapter(new IntegrationProperties(
                new IntegrationProperties.Brapi("https://brapi.test", "token"), null, null, null, null), builder);
    }

    @Test
    void translatesRateLimitAndIncompleteQuotation() {
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=PETR4"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> adapter.buscarCotacao("PETR4"))
                .isInstanceOf(ExternalIntegrationException.class);

        server.reset();
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=PETR4"))
                .andRespond(withSuccess("""
                        {"results":[{"symbol":"PETR4","data":{"longName":"Petrobras","currency":"BRL",
                        "regularMarketPrice":38.50}}]}
                        """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.buscarCotacao("PETR4"))
                .isInstanceOf(ExternalIntegrationException.class);
    }
}
