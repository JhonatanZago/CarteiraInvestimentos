package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.dto.EnderecoConsulta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViaCepAdapterTest {

    private MockRestServiceServer server;
    private ViaCepAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new ViaCepAdapter(properties(), builder);
    }

    @Test
    void normalizesCepAndMapsAddressResponse() {
        server.expect(requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withSuccess("""
                        {"cep":"01001-000","logradouro":"Praça da Sé","bairro":"Sé","localidade":"São Paulo","uf":"SP"}
                        """, MediaType.APPLICATION_JSON));

        EnderecoConsulta result = adapter.buscarPorCep("01001-000");

        assertThat(result.cep()).isEqualTo("01001000");
        assertThat(result.cidade()).isEqualTo("São Paulo");
        server.verify();
    }

    @Test
    void translatesMissingCepAndTimeout() {
        server.expect(requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withSuccess("{\"erro\":true}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.buscarPorCep("01001000"))
                .isInstanceOf(ResourceNotFoundException.class);

        server.reset();
        server.expect(requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(request -> { throw new ResourceAccessException("timeout"); });
        assertThatThrownBy(() -> adapter.buscarPorCep("01001000"))
                .isInstanceOf(ExternalIntegrationException.class);
    }

    private IntegrationProperties properties() {
        return new IntegrationProperties(null, null, null, new IntegrationProperties.ViaCep("https://viacep.test"), null);
    }
}
