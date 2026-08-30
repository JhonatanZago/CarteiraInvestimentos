package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.dto.insight.ClassificacaoAlocacao;
import com.example.carteirainvestimento.enums.Mercado;

public final class ClassificadorAlocacao {
    private ClassificadorAlocacao() { }

    public static ClassificacaoAlocacao para(Mercado mercado) {
        return mercado == Mercado.EUA ? ClassificacaoAlocacao.ACOES_EXTERIOR : ClassificacaoAlocacao.ACOES_BRASIL;
    }
}
