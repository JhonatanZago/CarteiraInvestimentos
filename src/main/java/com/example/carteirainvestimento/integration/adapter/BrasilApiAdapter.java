package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.dto.EmpresaConsulta;
import com.example.carteirainvestimento.validation.CnpjValidator;
import com.example.carteirainvestimento.validation.DocumentoNormalizer;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BrasilApiAdapter implements EmpresaAdapter {

    private final RestClient client;

    public BrasilApiAdapter(IntegrationProperties properties, RestClient.Builder restClientBuilder) {
        this.client = restClientBuilder.clone().baseUrl(properties.brasilApi().baseUrl()).build();
    }

    @Override
    public EmpresaConsulta buscarPorCnpj(String cnpj) {
        String cnpjNormalizado = DocumentoNormalizer.cnpj(cnpj);
        if (!CnpjValidator.isValid(cnpjNormalizado)) {
            throw new com.example.carteirainvestimento.exception.BusinessRuleException("CNPJ invalido");
        }
        try {
            BrasilApiEmpresaResponse response = client.get()
                    .uri("/api/v1/cnpj/{cnpj}", cnpjNormalizado)
                    .retrieve()
                    .body(BrasilApiEmpresaResponse.class);
            if (response == null) {
                throw new ExternalIntegrationException("BrasilAPI retornou uma resposta vazia");
            }
            return new EmpresaConsulta(cnpjNormalizado, response.razaoSocial(), response.nomeFantasia(), response.email(),
                    response.dddTelefone1(), response.descricaoSituacaoCadastral());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("CNPJ não encontrado na BrasilAPI");
            }
            throw new ExternalIntegrationException("Falha ao consultar a BrasilAPI");
        } catch (RestClientException exception) {
            throw new ExternalIntegrationException("Falha ao consultar a BrasilAPI");
        }
    }

    private record BrasilApiEmpresaResponse(
            String cnpj,
            @JsonProperty("razao_social") String razaoSocial,
            @JsonProperty("nome_fantasia") String nomeFantasia,
            String email,
            @JsonProperty("ddd_telefone_1") String dddTelefone1,
            @JsonProperty("descricao_situacao_cadastral") String descricaoSituacaoCadastral) {
    }
}
