package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.integration.dto.EmpresaConsulta;

public interface EmpresaAdapter {

    EmpresaConsulta buscarPorCnpj(String cnpj);
}
