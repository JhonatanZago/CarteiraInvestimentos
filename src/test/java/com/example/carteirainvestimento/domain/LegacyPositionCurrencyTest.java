package com.example.carteirainvestimento.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LegacyPositionCurrencyTest {
    @Test
    void marksForeignPositionWithoutHistoricalFxAsEstimated() {
        Acao acao = new Acao();
        acao.setMercado(Mercado.EUA);
        acao.setMoeda(Moeda.USD);
        AtivoCarteira posicao = new AtivoCarteira();
        posicao.setAcao(acao);
        posicao.setCambioHistoricoComprovado(false);

        assertThat(posicao.isConversaoEstimada()).isTrue();
    }

    @Test
    void doesNotMarkBrlPositionAsEstimated() {
        Acao acao = new Acao();
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda(Moeda.BRL);
        AtivoCarteira posicao = new AtivoCarteira();
        posicao.setAcao(acao);

        assertThat(posicao.isConversaoEstimada()).isFalse();
    }
}
