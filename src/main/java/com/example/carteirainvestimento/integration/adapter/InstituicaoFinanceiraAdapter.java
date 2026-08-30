package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.integration.dto.InstituicaoFinanceiraConsulta;

public interface InstituicaoFinanceiraAdapter {

    InstituicaoFinanceiraConsulta buscarPorCnpj(String cnpj);
}
