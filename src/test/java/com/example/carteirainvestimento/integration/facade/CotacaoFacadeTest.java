package com.example.carteirainvestimento.integration.facade;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.example.carteirainvestimento.enums.FonteCotacao;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.integration.adapter.CotacaoAdapter;
import com.example.carteirainvestimento.integration.dto.CotacaoConsulta;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CotacaoFacadeTest {

    @Test
    void selectsBrapiForBrazilAndTwelveDataForUsa() {
        CotacaoAdapter brapi = adapter(Mercado.BRASIL, FonteCotacao.BRAPI);
        CotacaoAdapter alphaVantage = adapter(Mercado.EUA, FonteCotacao.TWELVE_DATA);
        CotacaoFacade facade = new CotacaoFacade(List.of(brapi, alphaVantage));

        assertThat(facade.buscarCotacao("PETR4", Mercado.BRASIL).fonte()).isEqualTo(FonteCotacao.BRAPI);
        assertThat(facade.buscarCotacao("MSFT", Mercado.EUA).fonte()).isEqualTo(FonteCotacao.TWELVE_DATA);
    }

    @Test
    void rejectsUnsupportedMarket() {
        CotacaoFacade facade = new CotacaoFacade(List.of());

        assertThatThrownBy(() -> facade.buscarCotacao("PETR4", Mercado.BRASIL))
                .isInstanceOf(BusinessRuleException.class);
    }

    private CotacaoAdapter adapter(Mercado supportedMarket, FonteCotacao source) {
        return new CotacaoAdapter() {
            @Override
            public boolean suporta(Mercado mercado) {
                return supportedMarket == mercado;
            }

            @Override
            public CotacaoConsulta buscarCotacao(String ticker) {
                return new CotacaoConsulta(ticker, ticker, supportedMarket, Moeda.BRL, BigDecimal.ONE,
                        OffsetDateTime.now(), null, source);
            }
        };
    }
}
