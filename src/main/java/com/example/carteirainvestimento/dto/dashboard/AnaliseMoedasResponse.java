package com.example.carteirainvestimento.dto.dashboard;

import com.example.carteirainvestimento.enums.Moeda;
import java.math.BigDecimal;
import java.util.List;

public record AnaliseMoedasResponse(
        List<ExposicaoMoeda> exposicoes,
        int quantidadeEmLucro,
        int quantidadeEmPrejuizo,
        int quantidadeNeutra,
        List<ResultadoMoeda> resultadosPorMoeda,
        List<ResultadoAtivo> ativos,
        Moeda moedaBase,
        String mensagemCambio) {
    public record ExposicaoMoeda(Moeda moeda, String nome, BigDecimal valorOriginal, BigDecimal valorConvertidoBase, BigDecimal percentual) {
        public ExposicaoMoeda(Moeda moeda, String nome, BigDecimal valorOriginal, BigDecimal percentual) { this(moeda, nome, valorOriginal, valorOriginal, percentual); }
    }
    public record ResultadoMoeda(Moeda moeda, BigDecimal resultado) {}
    public record ResultadoAtivo(Long acaoId, String ticker, String nomeEmpresa, String logoUrl, Moeda moeda,
                                 BigDecimal resultado, BigDecimal rentabilidade) {}
}
