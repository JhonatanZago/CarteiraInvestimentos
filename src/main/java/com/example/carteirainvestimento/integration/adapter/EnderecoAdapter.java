package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.integration.dto.EnderecoConsulta;

public interface EnderecoAdapter {

    EnderecoConsulta buscarPorCep(String cep);
}
