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
        var configured = properties.twelveData();
        client = builder.clone().baseUrl(configured == null ? "https://api.twelvedata.com" : configured.baseUrl()).build();
        apiKey = configured == null ? "" : configured.apiKey();
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
            return new CotacaoConsulta(symbol, name, Mercado.EUA, Moeda.USD, price, OffsetDateTime.now(), buscarLogo(symbol, exchange), FonteCotacao.TWELVE_DATA, "US", exchange, null);
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

    /**
     * A cotação e o logotipo são recursos independentes na Twelve Data. Uma
     * falha no endpoint de logo nunca invalida uma cotação válida; nesse caso
     * o componente compartilhado usa o fallback seguro.
     */
    private String buscarLogo(String symbol, String exchange) {
        try {
            String rawBody = client.get().uri(uri -> uri.path("/logo")
                    .queryParam("symbol", symbol)
                    .queryParamIfPresent("exchange", StringUtils.hasText(exchange) ? java.util.Optional.of(exchange) : java.util.Optional.empty())
                    .queryParam("apikey", apiKey).build()).retrieve().body(String.class);
            JsonNode body = rawBody == null ? null : OBJECT_MAPPER.readTree(rawBody);
            String url = body == null ? null : body.path("url").asText(null);
            return StringUtils.hasText(url) && url.trim().toLowerCase(java.util.Locale.ROOT).startsWith("https://")
                    ? url.trim() : null;
        } catch (Exception exception) {
            log.debug("Logo indisponível na Twelve Data para ticker={} tipo={}", symbol, exception.getClass().getSimpleName());
            return null;
        }
    }
}
