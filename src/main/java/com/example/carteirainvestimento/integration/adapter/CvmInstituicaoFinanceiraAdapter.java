package com.example.carteirainvestimento.integration.adapter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.dto.InstituicaoFinanceiraConsulta;
import com.example.carteirainvestimento.validation.DocumentoNormalizer;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CvmInstituicaoFinanceiraAdapter implements InstituicaoFinanceiraAdapter {

    private static final String FONTE = "CVM";

    private final RestClient client;

    public CvmInstituicaoFinanceiraAdapter(IntegrationProperties properties, RestClient.Builder restClientBuilder) {
        this.client = restClientBuilder.clone().baseUrl(properties.financialInstitution().baseUrl()).build();
    }

    @Override
    public InstituicaoFinanceiraConsulta buscarPorCnpj(String cnpj) {
        String cnpjNormalizado = DocumentoNormalizer.cnpj(cnpj);
        try {
            CvmInstituicaoResponse response = client.get()
                    .uri("/instituicoes/{cnpj}", cnpjNormalizado)
                    .retrieve()
                    .body(CvmInstituicaoResponse.class);
            if (response == null || response.autorizada() == null) {
                throw new ExternalIntegrationException("CVM returned an incomplete authorization response");
            }
            return new InstituicaoFinanceiraConsulta(response.autorizada(), FONTE,
                    OffsetDateTime.now(ZoneOffset.UTC));
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Instituicao financeira nao encontrada na CVM");
            }
            throw new ExternalIntegrationException("Falha ao consultar a CVM");
        } catch (RestClientException | HttpMessageConversionException exception) {
            throw new ExternalIntegrationException("Falha ao consultar a CVM");
        }
    }

    private record CvmInstituicaoResponse(Boolean autorizada) {
    }
}
