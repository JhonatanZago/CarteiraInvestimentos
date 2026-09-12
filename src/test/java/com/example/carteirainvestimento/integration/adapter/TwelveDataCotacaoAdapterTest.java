package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
}
