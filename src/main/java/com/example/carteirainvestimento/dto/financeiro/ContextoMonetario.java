package com.example.carteirainvestimento.dto.financeiro;

import com.example.carteirainvestimento.enums.Moeda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Contexto imutável de um valor financeiro, preservando origem e conversão.
 *
 * <p>Valores indisponíveis não recebem moeda ou taxa artificial. A validação
 * impede que uma conversão incompleta seja confundida com um valor definitivo.
 */
public record ContextoMonetario(
        Moeda moedaOriginal,
        Moeda moedaBase,
        BigDecimal valorOriginal,
        BigDecimal valorConvertidoBase,
        BigDecimal taxaCambio,
        OffsetDateTime cambioEfetivoEm,
        String fonteCambio,
        DisponibilidadeMonetaria disponibilidade) {

    public ContextoMonetario {
        Objects.requireNonNull(disponibilidade, "disponibilidade obrigatoria");
        validarTaxa(taxaCambio);

        if (disponibilidade == DisponibilidadeMonetaria.UNAVAILABLE) {
            if (valorConvertidoBase != null) {
                throw new IllegalArgumentException("valor convertido nao pode existir quando a conversao esta indisponivel");
            }
        } else {
            if (moedaOriginal == null || moedaBase == null || valorOriginal == null) {
                throw new IllegalArgumentException("moeda e valor original sao obrigatorios para um valor disponivel ou estimado");
            }
            if (disponibilidade == DisponibilidadeMonetaria.AVAILABLE && valorConvertidoBase == null) {
                throw new IllegalArgumentException("valor convertido obrigatorio para conversao disponivel");
            }
            if (disponibilidade == DisponibilidadeMonetaria.ESTIMATED && !temTexto(fonteCambio)) {
                throw new IllegalArgumentException("fonte obrigatoria para valor estimado");
            }
            if (moedaOriginal == moedaBase && valorConvertidoBase != null
                    && valorOriginal.compareTo(valorConvertidoBase) != 0) {
                throw new IllegalArgumentException("valor na mesma moeda nao pode ser alterado por conversao");
            }
            if (moedaOriginal != moedaBase
                    && (taxaCambio == null || cambioEfetivoEm == null || !temTexto(fonteCambio))) {
                throw new IllegalArgumentException("taxa, data e fonte obrigatorias para moedas diferentes");
            }
        }
    }

    public static ContextoMonetario available(Moeda moedaOriginal, Moeda moedaBase,
            BigDecimal valorOriginal, BigDecimal valorConvertidoBase, BigDecimal taxaCambio,
            OffsetDateTime cambioEfetivoEm, String fonteCambio) {
        return new ContextoMonetario(moedaOriginal, moedaBase, valorOriginal, valorConvertidoBase,
                taxaCambio, cambioEfetivoEm, fonteCambio, DisponibilidadeMonetaria.AVAILABLE);
    }

    public static ContextoMonetario estimated(Moeda moedaOriginal, Moeda moedaBase,
            BigDecimal valorOriginal, BigDecimal valorConvertidoBase, BigDecimal taxaCambio,
            OffsetDateTime cambioEfetivoEm, String fonteCambio) {
        return new ContextoMonetario(moedaOriginal, moedaBase, valorOriginal, valorConvertidoBase,
                taxaCambio, cambioEfetivoEm, fonteCambio, DisponibilidadeMonetaria.ESTIMATED);
    }

    public static ContextoMonetario unavailable(Moeda moedaOriginal, Moeda moedaBase,
            BigDecimal valorOriginal, String fonteCambio) {
        return new ContextoMonetario(moedaOriginal, moedaBase, valorOriginal, null, null, null,
                fonteCambio, DisponibilidadeMonetaria.UNAVAILABLE);
    }

    private static void validarTaxa(BigDecimal taxaCambio) {
        if (taxaCambio != null && taxaCambio.signum() <= 0) {
            throw new IllegalArgumentException("taxa de cambio deve ser positiva");
        }
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
