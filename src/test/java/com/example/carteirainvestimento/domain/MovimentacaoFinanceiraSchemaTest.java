package com.example.carteirainvestimento.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.enums.TipoMovimentacao;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MovimentacaoFinanceiraSchemaTest {

    @Test
    void storesTransactionValuesWithDecimalTypesAndCurrencyMetadata() throws Exception {
        assertThat(field("precoUnitario").getType()).isEqualTo(BigDecimal.class);
        assertThat(field("cambioParaBrl").getType()).isEqualTo(BigDecimal.class);
        assertThat(field("valorOriginal").getType()).isEqualTo(BigDecimal.class);
        assertThat(field("valorBrl").getType()).isEqualTo(BigDecimal.class);
        assertThat(field("custosBrl").getType()).isEqualTo(BigDecimal.class);
        assertThat(field("moeda").getType()).isEqualTo(Moeda.class);
        assertThat(field("tipo").getType()).isEqualTo(TipoMovimentacao.class);
    }

    private Field field(String name) throws NoSuchFieldException {
        return MovimentacaoFinanceira.class.getDeclaredField(name);
    }
}
