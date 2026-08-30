package com.example.carteirainvestimento.integration.facade;

import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.integration.adapter.InstituicaoFinanceiraAdapter;
import com.example.carteirainvestimento.integration.dto.InstituicaoFinanceiraConsulta;
import org.springframework.stereotype.Component;

@Component
public class InstituicaoFinanceiraFacade {

    private final InstituicaoFinanceiraAdapter adapter;

    public InstituicaoFinanceiraFacade(InstituicaoFinanceiraAdapter adapter) {
        this.adapter = adapter;
    }

    public InstituicaoFinanceiraConsulta validarPorCnpj(String cnpj) {
        InstituicaoFinanceiraConsulta instituicao = adapter.buscarPorCnpj(cnpj);
        if (!instituicao.autorizada()) {
            throw new BusinessRuleException("Instituicao financeira nao autorizada pela " + instituicao.fonte());
        }
        return instituicao;
    }
}
