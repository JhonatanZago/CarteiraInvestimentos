package com.example.carteirainvestimento.service;

import java.time.OffsetDateTime;

import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.dto.corretora.CorretoraCreateRequest;
import com.example.carteirainvestimento.integration.adapter.EmpresaAdapter;
import com.example.carteirainvestimento.integration.adapter.EnderecoAdapter;
import com.example.carteirainvestimento.integration.dto.EmpresaConsulta;
import com.example.carteirainvestimento.integration.dto.EnderecoConsulta;
import com.example.carteirainvestimento.integration.dto.InstituicaoFinanceiraConsulta;
import com.example.carteirainvestimento.integration.facade.InstituicaoFinanceiraFacade;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorretoraRegistrationServiceTest {
    @Test
    void persistsOfficialDataAfterAllValidations() {
        CorretoraRepository repository = mock(CorretoraRepository.class);
        EmpresaAdapter empresa = cnpj -> new EmpresaConsulta(cnpj, "Corretora Oficial", "Oficial", "a@b.com", "11999999999", "ATIVA");
        EnderecoAdapter endereco = cep -> new EnderecoConsulta(cep, "Rua A", "Centro", "Sao Paulo", "SP");
        InstituicaoFinanceiraFacade financeiro = mock(InstituicaoFinanceiraFacade.class);
        when(repository.existsByCnpj("12345678000195")).thenReturn(false);
        when(financeiro.validarPorCnpj("12345678000195"))
                .thenReturn(new InstituicaoFinanceiraConsulta(true, "CVM", OffsetDateTime.now()));
        when(repository.save(any(Corretora.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Corretora result = new CorretoraRegistrationService(repository, empresa, endereco, financeiro)
                .registrar(new CorretoraCreateRequest("12.345.678/0001-95", "01001-000", "10", "Sala 1"));

        assertThat(result.getCnpj()).isEqualTo("12345678000195");
        assertThat(result.getRazaoSocial()).isEqualTo("Corretora Oficial");
        assertThat(result.getFonteValidacaoMercado()).isEqualTo("CVM");
        assertThat(result.getCep()).isEqualTo("01001000");
    }
}
