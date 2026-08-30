package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.integration.dto.EmpresaConsulta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrasilApiAdapterTest {

    private MockRestServiceServer server;
    private BrasilApiAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BrasilApiAdapter(properties(), builder);
    }

    @Test
    void normalizesCnpjAndMapsCompanyResponse() {
        server.expect(requestTo("https://brasil.test/api/v1/cnpj/12345678000195"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"razao_social":"Empresa S.A.","nome_fantasia":"Empresa","email":"contato@empresa.com",
                        "ddd_telefone_1":"1133334444","descricao_situacao_cadastral":"ATIVA"}
                        """, MediaType.APPLICATION_JSON));

        EmpresaConsulta result = adapter.buscarPorCnpj("12.345.678/0001-95");

        assertThat(result.cnpj()).isEqualTo("12345678000195");
        assertThat(result.razaoSocial()).isEqualTo("Empresa S.A.");
        assertThat(result.situacaoCadastral()).isEqualTo("ATIVA");
        server.verify();
    }

    @Test
    void translatesNotFound() {
        server.expect(requestTo("https://brasil.test/api/v1/cnpj/12345678000195"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.buscarPorCnpj("12345678000195"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void translatesMalformedResponseAndUnavailableProvider() {
        server.expect(requestTo("https://brasil.test/api/v1/cnpj/12345678000195"))
                .andRespond(withSuccess("{invalido", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.buscarPorCnpj("12345678000195"))
                .isInstanceOf(ExternalIntegrationException.class);

        server.reset();
        server.expect(requestTo("https://brasil.test/api/v1/cnpj/12345678000195"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> adapter.buscarPorCnpj("12345678000195"))
                .isInstanceOf(ExternalIntegrationException.class);
    }

    private IntegrationProperties properties() {
        return new IntegrationProperties(null, null, new IntegrationProperties.BrasilApi("https://brasil.test"),
                null, null);
    }
}
