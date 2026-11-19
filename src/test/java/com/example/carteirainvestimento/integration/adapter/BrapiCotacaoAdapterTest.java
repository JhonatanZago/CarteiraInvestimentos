package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
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

    @Test
    void translatesProviderUnauthorizedResponseWithoutLeakingConfiguredToken() {
        String fakeToken = "fake-brapi-token-for-test";
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BrapiCotacaoAdapter(new IntegrationProperties(
                new IntegrationProperties.Brapi("https://brapi.test", fakeToken), null, null, null, null), builder);
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=PETR4"))
                .andExpect(header("Authorization", "Bearer " + fakeToken))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("provider rejected fake-brapi-token"));

        assertThatThrownBy(() -> adapter.buscarCotacao("PETR4"))
                .isInstanceOf(ExternalIntegrationException.class)
                .hasMessage("Falha ao consultar a BRAPI")
                .hasMessageNotContaining(fakeToken);
    }

    @Test
    void mapsOnlyHttpsLogoUrlReturnedByBrapi() {
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=PETR4"))
                .andRespond(withSuccess("""
                        {"results":[{"symbol":"PETR4","longName":"Petrobras","currency":"BRL",
                        "regularMarketPrice":38.50,"regularMarketTime":"2026-09-04T15:30:00Z",
                        "logourl":"https://cdn.example.com/petr4.png"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.buscarCotacao("PETR4").logoUrl()).isEqualTo("https://cdn.example.com/petr4.png");
    }

    @Test
    void mapsLogoWhenBrapiReturnsItInsideMarketData() {
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=VALE3"))
                .andRespond(withSuccess("""
                        {"results":[{"symbol":"VALE3","longName":"Vale S.A.","currency":"BRL",
                        "regularMarketPrice":79.10,"regularMarketTime":"2026-09-04T15:30:00Z",
                        "data":{"longName":"Vale S.A.","currency":"BRL","regularMarketPrice":79.10,
                        "regularMarketTime":"2026-09-04T15:30:00Z","logourl":"https://cdn.example.com/vale3.png"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.buscarCotacao("VALE3").logoUrl()).isEqualTo("https://cdn.example.com/vale3.png");
    }

    @Test
    void rejectsEmptyResultsAndInvalidCurrency() {
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=EMPTY"))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.buscarCotacao("EMPTY"))
                .isInstanceOf(com.example.carteirainvestimento.exception.ResourceNotFoundException.class);

        server.reset();
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=BAD"))
                .andRespond(withSuccess("""
                        {"results":[{"symbol":"BAD","longName":"Bad Asset","currency":"ZZZ",
                        "regularMarketPrice":10,"regularMarketTime":"2026-09-04T15:30:00Z"}]}
                        """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.buscarCotacao("BAD"))
                .isInstanceOf(ExternalIntegrationException.class);
    }

    @Test
    void usesLegacyQuotationAfterServerFailureAndRejectsIncompleteLegacyPayload() {
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=PETR4"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(requestTo("https://brapi.test/api/quote/PETR4"))
                .andRespond(withSuccess("""
                        {"results":[{"symbol":"PETR4","longName":"Petrobras","currency":"BRL",
                        "regularMarketPrice":38.50,"regularMarketTime":"2026-09-04T15:30:00Z",
                        "exchange":"B3","logourl":"http://insecure.example/logo"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.buscarCotacao("PETR4"))
                .isInstanceOf(ExternalIntegrationException.class);

        server.reset();
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=VALE3"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        server.expect(requestTo("https://brapi.test/api/quote/VALE3"))
                .andRespond(withSuccess("{\"results\":[{\"symbol\":\"VALE3\"}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.buscarCotacao("VALE3"))
                .isInstanceOf(ExternalIntegrationException.class);
    }

    @Test
    void rejectsProviderErrorAndUsesTopLevelQuotationFields() {
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=ERROR"))
                .andRespond(withSuccess("{\"error\":true}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.buscarCotacao("ERROR"))
                .isInstanceOf(ExternalIntegrationException.class);

        server.reset();
        server.expect(requestTo("https://brapi.test/api/v2/stocks/quote?symbols=ITUB4"))
                .andRespond(withSuccess("""
                        {"results":[{"symbol":"ITUB4","longName":"Itau Unibanco","currency":"BRL",
                        "regularMarketPrice":30.10,"regularMarketTime":"2026-09-04T15:30:00Z",
                        "exchangeName":"B3","exchangeMic":"BVMF"}]}
                        """, MediaType.APPLICATION_JSON));

        var quote = adapter.buscarCotacao("ITUB4");
        assertThat(quote.nomeEmpresa()).isEqualTo("Itau Unibanco");
        assertThat(quote.exchange()).isEqualTo("B3");
        assertThat(quote.exchangeMic()).isEqualTo("BVMF");
    }

}
