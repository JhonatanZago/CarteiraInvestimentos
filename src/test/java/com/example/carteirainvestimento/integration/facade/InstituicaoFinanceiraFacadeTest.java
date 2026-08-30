package com.example.carteirainvestimento.integration.facade;

import java.time.OffsetDateTime;

import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.integration.adapter.InstituicaoFinanceiraAdapter;
import com.example.carteirainvestimento.integration.dto.InstituicaoFinanceiraConsulta;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstituicaoFinanceiraFacadeTest {

    @Test
    void rejectsUnauthorizedInstitution() {
        InstituicaoFinanceiraAdapter adapter = cnpj -> new InstituicaoFinanceiraConsulta(false, "CVM", OffsetDateTime.now());
        InstituicaoFinanceiraFacade facade = new InstituicaoFinanceiraFacade(adapter);

        assertThatThrownBy(() -> facade.validarPorCnpj("12345678000195"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
