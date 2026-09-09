package com.example.carteirainvestimento.dto.acao;

import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;

public record RevalidacaoDetalhe(Long id, String ticker, boolean alterado,
        Moeda moedaAnterior, Moeda moedaAtual, Mercado mercadoAnterior, Mercado mercadoAtual,
        String logoAnterior, String logoAtual, String erro) { }
