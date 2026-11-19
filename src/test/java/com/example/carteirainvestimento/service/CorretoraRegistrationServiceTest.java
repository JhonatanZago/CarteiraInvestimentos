package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.domain.StatusValidacaoCorretora;
import com.example.carteirainvestimento.dto.corretora.CorretoraCreateRequest;
import com.example.carteirainvestimento.integration.adapter.EmpresaAdapter;
import com.example.carteirainvestimento.integration.adapter.EnderecoAdapter;
import com.example.carteirainvestimento.integration.dto.EmpresaConsulta;
import com.example.carteirainvestimento.integration.dto.EnderecoConsulta;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.exception.AssetInUseException;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.exception.ExternalIntegrationException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorretoraRegistrationServiceTest {
    @Test
    void persistsOfficialDataAfterAllValidations() {
        CorretoraRepository repository = mock(CorretoraRepository.class);
        EmpresaAdapter empresa = cnpj -> new EmpresaConsulta(cnpj, "Corretora Oficial", "Oficial", "a@b.com", "11999999999", "ATIVA");
        EnderecoAdapter endereco = cep -> new EnderecoConsulta(cep, "Rua A", "Centro", "Sao Paulo", "SP");
        when(repository.existsByCnpj("12345678000195")).thenReturn(false);
        when(repository.save(any(Corretora.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Corretora result = new CorretoraRegistrationService(repository, empresa, endereco)
                .registrar(new CorretoraCreateRequest("12.345.678/0001-95", "01001-000", "10", "Sala 1"));

        assertThat(result.getCnpj()).isEqualTo("12345678000195");
        assertThat(result.getRazaoSocial()).isEqualTo("Corretora Oficial");
        assertThat(result.getFonteValidacaoMercado()).isEqualTo("BRASIL_API");
        assertThat(result.isValidadaMercadoFinanceiro()).isTrue();
        assertThat(result.getCep()).isEqualTo("01001000");
    }

    @Test
    void revalidatesAnActiveCompanyUsingBrasilApi() {
        CorretoraRepository repository = mock(CorretoraRepository.class);
        Corretora corretora = new Corretora();
        corretora.setCnpj("12345678000195");
        EmpresaAdapter empresa = cnpj -> new EmpresaConsulta(cnpj, "Corretora Atualizada", "Atualizada", null, null, "ATIVA");
        EnderecoAdapter endereco = cep -> new EnderecoConsulta(cep, "Rua A", "Centro", "Sao Paulo", "SP");
        when(repository.findById(7L)).thenReturn(Optional.of(corretora));
        when(repository.save(corretora)).thenReturn(corretora);

        Corretora result = new CorretoraRegistrationService(repository, empresa, endereco).revalidar(7L);

        assertThat(result.getStatusValidacao()).isEqualTo(StatusValidacaoCorretora.VALIDADA);
        assertThat(result.isValidadaMercadoFinanceiro()).isTrue();
        assertThat(result.getFonteValidacaoMercado()).isEqualTo("BRASIL_API");
    }

    @Test
    void keepsRegistrationPendingWhenExternalCompanyProviderIsUnavailable() {
        CorretoraRepository repository = mock(CorretoraRepository.class);
        EmpresaAdapter empresa = cnpj -> { throw new ExternalIntegrationException("provider unavailable"); };
        EnderecoAdapter endereco = cep -> { throw new ExternalIntegrationException("cep unavailable"); };
        when(repository.existsByCnpj("12345678000195")).thenReturn(false);
        when(repository.save(any(Corretora.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Corretora result = new CorretoraRegistrationService(repository, empresa, endereco)
                .registrar(new CorretoraCreateRequest("12.345.678/0001-95", "01001-000", "10", null));

        assertThat(result.getStatusValidacao()).isEqualTo(StatusValidacaoCorretora.FONTE_INDISPONIVEL);
        assertThat(result.isValidadaMercadoFinanceiro()).isFalse();
        assertThat(result.getCep()).isEqualTo("01001000");
    }

    @Test
    void rejectsInactiveCompanyAndDuplicateCnpj() {
        CorretoraRepository repository = mock(CorretoraRepository.class);
        EmpresaAdapter inactive = cnpj -> new EmpresaConsulta(cnpj, "Inativa", null, null, null, "INAPTA");
        EnderecoAdapter endereco = cep -> new EnderecoConsulta(cep, null, null, null, null);
        when(repository.existsByCnpj("12345678000195")).thenReturn(false);

        assertThatThrownBy(() -> new CorretoraRegistrationService(repository, inactive, endereco)
                .registrar(new CorretoraCreateRequest("12.345.678/0001-95", "01001-000", "10", null)))
                .isInstanceOf(BusinessRuleException.class);

        when(repository.existsByCnpj("12345678000195")).thenReturn(true);
        assertThatThrownBy(() -> new CorretoraRegistrationService(repository, inactive, endereco)
                .registrar(new CorretoraCreateRequest("12.345.678/0001-95", "01001-000", "10", null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void refusesDeletionWhenBrokerHasPositions() {
        CorretoraRepository repository = mock(CorretoraRepository.class);
        AtivoCarteiraRepository ativos = mock(AtivoCarteiraRepository.class);
        Corretora corretora = new Corretora();
        when(repository.findById(3L)).thenReturn(Optional.of(corretora));
        when(ativos.existsByCorretoraId(3L)).thenReturn(true);

        assertThatThrownBy(() -> new CorretoraRegistrationService(repository, cnpj -> null, cep -> null, ativos).excluir(3L))
                .isInstanceOf(AssetInUseException.class);
    }
}
