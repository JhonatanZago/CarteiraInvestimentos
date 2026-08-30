package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.integration.dto.InstituicaoFinanceiraConsulta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CvmInstituicaoFinanceiraAdapterTest {

    private MockRestServiceServer server;
    private CvmInstituicaoFinanceiraAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new CvmInstituicaoFinanceiraAdapter(new IntegrationProperties(null, null, null, null,
                new IntegrationProperties.FinancialInstitution("https://cvm.test")), builder);
    }

    @Test
    void normalizesCnpjAndRecordsCvmSourceAndValidationTime() {
        server.expect(requestTo("https://cvm.test/instituicoes/12345678000195"))
                .andRespond(withSuccess("{\"autorizada\":true}", MediaType.APPLICATION_JSON));

        InstituicaoFinanceiraConsulta result = adapter.buscarPorCnpj("12.345.678/0001-95");

        assertThat(result.autorizada()).isTrue();
        assertThat(result.fonte()).isEqualTo("CVM");
        assertThat(result.dataHoraValidacao()).isNotNull();
        server.verify();
    }
}
