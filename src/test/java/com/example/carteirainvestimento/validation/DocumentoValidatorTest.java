package com.example.carteirainvestimento.validation;

import com.example.carteirainvestimento.config.IntegrationProperties;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.integration.adapter.BrasilApiAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentoValidatorTest {

    @Test
    void normalizesFormattedCnpjAndCep() {
        assertThat(DocumentoNormalizer.cnpj("12.345.678/0001-95")).isEqualTo("12345678000195");
        assertThat(DocumentoNormalizer.cep("01001-000")).isEqualTo("01001000");
        assertThat(CnpjValidator.isValid("12345678000195")).isTrue();
    }

    @Test
    void rejectsInvalidCnpjBeforeCallingCompanyProvider() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrasilApiAdapter adapter = new BrasilApiAdapter(new IntegrationProperties(null, null,
                new IntegrationProperties.BrasilApi("https://brasil.test"), null, null), builder);

        assertThatThrownBy(() -> adapter.buscarPorCnpj("12.345.678/0001-90"))
                .isInstanceOf(BusinessRuleException.class);
        server.verify();
    }

    @Test
    void rejectsCnpjWithInvalidLengthOrCheckDigits() {
        assertThatThrownBy(() -> DocumentoNormalizer.cnpj("123"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(CnpjValidator.isValid("12345678000190")).isFalse();
        assertThat(CnpjValidator.isValid("11111111111111")).isFalse();
    }
}
