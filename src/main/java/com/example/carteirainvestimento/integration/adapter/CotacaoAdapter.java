package com.example.carteirainvestimento.integration.adapter;

import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;

public interface CotacaoAdapter {

    boolean suporta(Mercado mercado);

    CotacaoConsulta buscarCotacao(String ticker);
}
