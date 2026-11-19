package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TwelveDataCotacaoAdapterTest {
    @Test
    void mapsProviderLogoWhenLogoEndpointReturnsHttpsUrl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var properties = new IntegrationProperties(null, null,
                new IntegrationProperties.TwelveData("https://twelvedata.test", "secret"), null, null, null);
        var adapter = new TwelveDataCotacaoAdapter(properties, builder);

        server.expect(requestTo("https://twelvedata.test/quote?symbol=AAPL&apikey=secret"))
                .andRespond(withSuccess("""
                        {"symbol":"AAPL","name":"Apple Inc.","currency":"USD","exchange":"NASDAQ","close":"200.00"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(request -> {
                    var uri = request.getURI();
                    assertThat(uri.getPath()).isEqualTo("/logo");
                    assertThat(uri.getQuery()).contains("symbol=AAPL", "exchange=NASDAQ", "apikey=secret");
                })
                .andRespond(withSuccess("""
                        {"meta":{"symbol":"AAPL"},"url":"https://api.twelvedata.com/logo/apple.com"}
                        """, MediaType.APPLICATION_JSON));

        var quote = adapter.buscarCotacao("AAPL");
        assertThat(quote.logoUrl()).isEqualTo("https://api.twelvedata.com/logo/apple.com");
        server.verify();
    }

    @Test
    void keepsValidQuoteWhenLogoEndpointFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var properties = new IntegrationProperties(null, null,
                new IntegrationProperties.TwelveData("https://twelvedata.test", "secret"), null, null, null);
        var adapter = new TwelveDataCotacaoAdapter(properties, builder);
        server.expect(requestTo("https://twelvedata.test/quote?symbol=NVDA&apikey=secret"))
                .andRespond(withSuccess("""
                        {"symbol":"NVDA","name":"NVIDIA Corporation","currency":"USD","exchange":"NASDAQ","close":"120.00"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://twelvedata.test/logo?symbol=NVDA&exchange=NASDAQ&apikey=secret"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        var quote = adapter.buscarCotacao("NVDA");
        assertThat(quote.valor()).isEqualByComparingTo("120.00");
        assertThat(quote.logoUrl()).isNull();
    }

    @Test
    void mapsUnsupportedMarketAndMissingKeyWithoutCallingProvider() {
        RestClient.Builder builder = RestClient.builder();
        var noKey = new TwelveDataCotacaoAdapter(new IntegrationProperties(null, null,
                new IntegrationProperties.TwelveData("https://twelvedata.test", ""), null, null, null), builder);
        assertThat(noKey.suporta(com.example.carteirainvestimento.enums.Mercado.EUA)).isFalse();
        assertThat(noKey.suporta(com.example.carteirainvestimento.enums.Mercado.BRASIL)).isFalse();
    }

    @Test
    void translatesUnauthorizedAndInvalidQuotationResponses() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var properties = new IntegrationProperties(null, null,
                new IntegrationProperties.TwelveData("https://twelvedata.test", "secret"), null, null, null);
        var adapter = new TwelveDataCotacaoAdapter(properties, builder);
        server.expect(request -> request.getURI().getPath().equals("/quote"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> adapter.buscarCotacao("AAPL"))
                .isInstanceOf(com.example.carteirainvestimento.exception.ProviderUnauthorizedException.class);

        server.reset();
        server.expect(request -> request.getURI().getPath().equals("/quote"))
                .andRespond(withSuccess("{\"symbol\":\"AAPL\",\"name\":\"Apple\",\"currency\":\"EUR\",\"close\":\"0\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.buscarCotacao("AAPL"))
                .isInstanceOf(com.example.carteirainvestimento.exception.ExternalIntegrationException.class);
    }

    @Test
    void ignoresNonHttpsLogoUrl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var properties = new IntegrationProperties(null, null,
                new IntegrationProperties.TwelveData("https://twelvedata.test", "secret"), null, null, null);
        var adapter = new TwelveDataCotacaoAdapter(properties, builder);
        server.expect(request -> request.getURI().getPath().equals("/quote"))
                .andRespond(withSuccess("{\"symbol\":\"MSFT\",\"name\":\"Microsoft\",\"currency\":\"USD\",\"exchange\":\"NASDAQ\",\"close\":\"300\"}", MediaType.APPLICATION_JSON));
        server.expect(request -> request.getURI().getPath().equals("/logo"))
                .andRespond(withSuccess("{\"url\":\"http://insecure.example/logo\"}", MediaType.APPLICATION_JSON));

        assertThat(adapter.buscarCotacao("MSFT").logoUrl()).isNull();
        server.verify();
    }

    @Test
    void translatesProviderServerFailureAndMalformedJson() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var properties = new IntegrationProperties(null, null,
                new IntegrationProperties.TwelveData("https://twelvedata.test", "secret"), null, null, null);
        var adapter = new TwelveDataCotacaoAdapter(properties, builder);

        server.expect(request -> request.getURI().getPath().equals("/quote"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatThrownBy(() -> adapter.buscarCotacao("AAPL"))
                .isInstanceOf(com.example.carteirainvestimento.exception.ExternalIntegrationException.class)
                .hasMessage("A Twelve Data respondeu HTTP 500");

        server.reset();
        server.expect(request -> request.getURI().getPath().equals("/quote"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.buscarCotacao("AAPL"))
                .isInstanceOf(com.example.carteirainvestimento.exception.ExternalIntegrationException.class)
                .hasMessage("Falha ao consultar a Twelve Data");
    }

    @Test
    void supportsNoConfiguredPropertiesWithoutCallingProvider() {
        var adapter = new TwelveDataCotacaoAdapter(new IntegrationProperties(null, null, null, null, null, null), RestClient.builder());

        assertThat(adapter.suporta(com.example.carteirainvestimento.enums.Mercado.EUA)).isFalse();
        assertThat(adapter.suporta(null)).isFalse();
    }
}
