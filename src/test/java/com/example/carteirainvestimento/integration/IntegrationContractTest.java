package com.example.carteirainvestimento.integration;

import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.integration.adapter.CotacaoAdapter;
import com.example.carteirainvestimento.integration.adapter.EmpresaAdapter;
import com.example.carteirainvestimento.integration.adapter.EnderecoAdapter;
import com.example.carteirainvestimento.integration.adapter.InstituicaoFinanceiraAdapter;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import com.example.carteirainvestimento.integration.dto.EmpresaConsulta;
import com.example.carteirainvestimento.integration.dto.EnderecoConsulta;
import com.example.carteirainvestimento.integration.dto.InstituicaoFinanceiraConsulta;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationContractTest {

    @Test
    void exposesOnlyInternalLookupDtosAtTheAdapterBoundary() throws Exception {
        assertThat(EmpresaAdapter.class.getMethod("buscarPorCnpj", String.class).getReturnType())
                .isEqualTo(EmpresaConsulta.class);
        assertThat(EnderecoAdapter.class.getMethod("buscarPorCep", String.class).getReturnType())
                .isEqualTo(EnderecoConsulta.class);
        assertThat(InstituicaoFinanceiraAdapter.class.getMethod("buscarPorCnpj", String.class).getReturnType())
                .isEqualTo(InstituicaoFinanceiraConsulta.class);
        assertThat(CotacaoAdapter.class.getMethod("buscarCotacao", String.class).getReturnType())
                .isEqualTo(CotacaoConsulta.class);
        assertThat(CotacaoAdapter.class.getMethod("suporta", Mercado.class).getReturnType()).isEqualTo(boolean.class);
    }
}
