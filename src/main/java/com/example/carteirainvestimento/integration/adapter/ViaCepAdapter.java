package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.dto.EnderecoConsulta;
import com.example.carteirainvestimento.validation.DocumentoNormalizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ViaCepAdapter implements EnderecoAdapter {

    private final RestClient client;

    public ViaCepAdapter(IntegrationProperties properties, RestClient.Builder restClientBuilder) {
        this.client = restClientBuilder.clone().baseUrl(properties.viaCep().baseUrl()).build();
    }

    @Override
    public EnderecoConsulta buscarPorCep(String cep) {
        String cepNormalizado = DocumentoNormalizer.cep(cep);
        try {
            ViaCepResponse response = client.get()
                    .uri("/ws/{cep}/json/", cepNormalizado)
                    .retrieve()
                    .body(ViaCepResponse.class);
            if (response == null) {
                throw new ExternalIntegrationException("ViaCEP retornou uma resposta vazia");
            }
            if (Boolean.TRUE.equals(response.erro())) {
                throw new ResourceNotFoundException("CEP não encontrado no ViaCEP");
            }
            return new EnderecoConsulta(cepNormalizado, response.logradouro(), response.bairro(), response.localidade(), response.uf());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("CEP não encontrado no ViaCEP");
            }
            throw new ExternalIntegrationException("Falha ao consultar o ViaCEP");
        } catch (RestClientException exception) {
            throw new ExternalIntegrationException("Falha ao consultar o ViaCEP");
        }
    }

    private record ViaCepResponse(String cep, String logradouro, String bairro, String localidade, String uf, Boolean erro) {
    }
}
