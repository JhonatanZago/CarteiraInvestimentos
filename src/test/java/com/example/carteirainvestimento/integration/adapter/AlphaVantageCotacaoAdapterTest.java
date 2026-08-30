package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AlphaVantageCotacaoAdapterTest {

    private MockRestServiceServer server;
    private AlphaVantageCotacaoAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new AlphaVantageCotacaoAdapter(new IntegrationProperties(null,
                new IntegrationProperties.AlphaVantage("https://alpha.test", "key"), null, null, null), builder);
    }

    @Test
    void translatesProviderRateLimitAndIncompleteQuotation() {
        server.expect(requestTo("https://alpha.test/query?function=GLOBAL_QUOTE&symbol=MSFT&apikey=key"))
                .andRespond(withSuccess("{\"Note\":\"API call frequency exceeded\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.buscarCotacao("MSFT"))
                .isInstanceOf(ExternalIntegrationException.class);

        server.reset();
        server.expect(requestTo("https://alpha.test/query?function=GLOBAL_QUOTE&symbol=MSFT&apikey=key"))
                .andRespond(withSuccess("{\"Global Quote\":{\"01. symbol\":\"MSFT\",\"05. price\":\"500.00\"}}",
                        MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.buscarCotacao("MSFT"))
                .isInstanceOf(ExternalIntegrationException.class);
    }
}
