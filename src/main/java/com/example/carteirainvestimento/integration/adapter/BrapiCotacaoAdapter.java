package com.example.carteirainvestimento.integration.adapter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.enums.FonteCotacao;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BrapiCotacaoAdapter implements CotacaoAdapter {

    private final RestClient client;
    private final String token;

    public BrapiCotacaoAdapter(IntegrationProperties properties, RestClient.Builder restClientBuilder) {
        this.client = restClientBuilder.clone().baseUrl(properties.brapi().baseUrl()).build();
        this.token = properties.brapi().token();
    }

    @Override
    public boolean suporta(Mercado mercado) {
        return Mercado.BRASIL == mercado;
    }

    @Override
    public CotacaoConsulta buscarCotacao(String ticker) {
        try {
            BrapiResponse response = client.get()
                    .uri(builder -> builder.path("/api/v2/stocks/quote").queryParam("symbols", ticker).build())
                    .headers(headers -> {
                        if (StringUtils.hasText(token)) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    .body(BrapiResponse.class);
            if (response == null) {
                throw new ExternalIntegrationException("BRAPI returned an empty response");
            }
            if (Boolean.TRUE.equals(response.error())) {
                throw new ExternalIntegrationException("BRAPI rejected the quotation request");
            }
            if (response.results() == null || response.results().isEmpty()) {
                throw new ResourceNotFoundException("Ticker nÃ£o encontrado na BRAPI");
            }

            BrapiQuote quote = response.results().getFirst();
            String longName = quote.data() == null ? quote.longName() : quote.data().longName();
            String currency = quote.data() == null ? quote.currency() : quote.data().currency();
            BigDecimal price = quote.data() == null ? quote.regularMarketPrice() : quote.data().regularMarketPrice();
            OffsetDateTime marketTime = quote.data() == null ? quote.regularMarketTime() : quote.data().regularMarketTime();
            validarRespostaCompleta(quote.symbol(), longName, currency, price, marketTime);
            return new CotacaoConsulta(quote.symbol(), longName, Mercado.BRASIL, moeda(currency), price, marketTime,
                    FonteCotacao.BRAPI);
        } catch (RestClientException | HttpMessageConversionException exception) {
            throw new ExternalIntegrationException("Falha ao consultar a BRAPI");
        }
    }

    private Moeda moeda(String currency) {
        try {
            return Moeda.valueOf(currency);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ExternalIntegrationException("BRAPI retornou uma moeda invÃ¡lida");
        }
    }

    private void validarRespostaCompleta(String symbol, String longName, String currency, BigDecimal price,
                                         OffsetDateTime marketTime) {
        if (!StringUtils.hasText(symbol) || !StringUtils.hasText(longName) || !StringUtils.hasText(currency)
                || price == null || marketTime == null) {
            throw new ExternalIntegrationException("BRAPI returned incomplete quotation data");
        }
    }

    private record BrapiResponse(List<BrapiQuote> results, Boolean error) {
    }

    private record BrapiQuote(String symbol, String longName, String currency, BigDecimal regularMarketPrice,
                              OffsetDateTime regularMarketTime, BrapiMarketData data) {
    }

    private record BrapiMarketData(String longName, String currency, BigDecimal regularMarketPrice,
                                   OffsetDateTime regularMarketTime) {
    }
}
