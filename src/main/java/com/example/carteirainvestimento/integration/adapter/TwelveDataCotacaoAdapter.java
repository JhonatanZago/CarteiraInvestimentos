package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.enums.FonteCotacao;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.exception.ProviderUnauthorizedException;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.core.annotation.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

/** Fonte prioritária de ações EUA, com token mantido somente no servidor. */
@Component
@Order(-20)
public class TwelveDataCotacaoAdapter implements CotacaoAdapter {
    private static final Logger log = LoggerFactory.getLogger(TwelveDataCotacaoAdapter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RestClient client;
    private final String apiKey;
    public TwelveDataCotacaoAdapter(IntegrationProperties properties, RestClient.Builder builder) {
        client = builder.clone().baseUrl(properties.twelveData().baseUrl()).build();
        apiKey = properties.twelveData().apiKey();
    }
    @Override public boolean suporta(Mercado mercado) { return mercado == Mercado.EUA && StringUtils.hasText(apiKey); }
    @Override public CotacaoConsulta buscarCotacao(String ticker) {
        try {
            String rawBody = client.get().uri(uri -> uri.path("/quote").queryParam("symbol", ticker).queryParam("apikey", apiKey).build()).retrieve().body(String.class);
            JsonNode body = rawBody == null ? null : OBJECT_MAPPER.readTree(rawBody);
            if (body == null || body.has("code") || body.has("status")) throw new ResourceNotFoundException("Ticker não encontrado na Twelve Data");
            String symbol = body.path("symbol").asText(); String name = body.path("name").asText(); String currency = body.path("currency").asText(); String exchange = body.path("exchange").asText();
            String close = body.path("close").asText();
            BigDecimal price = StringUtils.hasText(close) ? new BigDecimal(close) : BigDecimal.ZERO;
            if (!StringUtils.hasText(symbol) || !StringUtils.hasText(name) || !"USD".equals(currency) || price.signum() <= 0) throw new ExternalIntegrationException("Twelve Data retornou dados incompletos para " + ticker);
            return new CotacaoConsulta(symbol, name, Mercado.EUA, Moeda.USD, price, OffsetDateTime.now(), null, FonteCotacao.TWELVE_DATA, "US", exchange, null);
        } catch (ResourceNotFoundException | ProviderUnauthorizedException exception) { throw exception; }
        catch (HttpClientErrorException.Unauthorized exception) { throw new ProviderUnauthorizedException("A chave da Twelve Data foi rejeitada pela fonte."); }
        catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
                throw new ProviderUnauthorizedException("A chave da Twelve Data foi rejeitada pela fonte.");
            }
            throw new ExternalIntegrationException("A Twelve Data respondeu HTTP " + exception.getStatusCode().value());
        }
        catch (Exception exception) {
            // Nunca incluir a URL da requisi\u00e7\u00e3o aqui: ela cont\u00e9m a chave da fonte.
            log.warn("Falha da Twelve Data para ticker={} tipo={}", ticker, exception.getClass().getSimpleName());
            throw new ExternalIntegrationException("Falha ao consultar a Twelve Data");
        }
    }
}
