package com.example.carteirainvestimento.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.domain.Carteira;
import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.domain.HistoricoCotacao;
import com.example.carteirainvestimento.dto.acao.AcaoResponse;
import com.example.carteirainvestimento.dto.carteira.AtivoCarteiraResponse;
import com.example.carteirainvestimento.dto.carteira.CarteiraResponse;
import com.example.carteirainvestimento.dto.insight.ClassificacaoAlocacao;
import com.example.carteirainvestimento.dto.corretora.CorretoraResponse;
import com.example.carteirainvestimento.dto.historico.HistoricoCotacaoResponse;
import com.example.carteirainvestimento.enums.FonteCotacao;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapperTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 8, 25, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void mapsAcaoToResponse() {
        Acao acao = acao();

        assertThat(AcaoMapper.toResponse(acao)).isEqualTo(
                new AcaoResponse(10L, "PETR4", "Petrobras", Mercado.BRASIL, Moeda.BRL,
                        new BigDecimal("31.25"), NOW));
    }

    @Test
    void mapsCorretoraToResponse() {
        Corretora corretora = corretora();

        assertThat(CorretoraMapper.toResponse(corretora)).isEqualTo(new CorretoraResponse(
                20L, "12345678000190", "Corretora S.A.", "Corretora", "contato@corretora.com", "11999999999",
                "01001000", "Praça da Sé", "10", "Sala 1", "Sé", "São Paulo", "SP", "ATIVA", true,
                NOW, "CVM", NOW));
    }

    @Test
    void mapsCarteiraToResponse() {
        Carteira carteira = carteira();

        assertThat(CarteiraMapper.toResponse(carteira))
                .isEqualTo(new CarteiraResponse(30L, "Longo prazo", "Aposentadoria", NOW));
    }

    @Test
    void mapsAtivoCarteiraToResponse() {
        AtivoCarteira ativo = new AtivoCarteira();
        ativo.setId(40L);
        ativo.setCarteira(carteira());
        ativo.setAcao(acao());
        ativo.setCorretora(corretora());
        ativo.setQuantidade(new BigDecimal("100"));
        ativo.setPrecoMedio(new BigDecimal("28.50"));
        ativo.setDataPrimeiraCompra(LocalDate.of(2025, 1, 15));

        assertThat(AtivoCarteiraMapper.toResponse(ativo)).isEqualTo(
                new AtivoCarteiraResponse(40L, 30L, 10L, 20L, new BigDecimal("100"),
                        new BigDecimal("28.50"), LocalDate.of(2025, 1, 15), Mercado.BRASIL,
                        ClassificacaoAlocacao.ACOES_BRASIL, "PETR4", "Petrobras",
                        new BigDecimal("31.25"), NOW, new BigDecimal("2850.00"), new BigDecimal("3125.00"),
                        new BigDecimal("275.00"), new BigDecimal("9.6491")));
    }

    @Test
    void mapsHistoricoCotacaoToResponse() {
        HistoricoCotacao historico = new HistoricoCotacao();
        historico.setId(50L);
        historico.setAcao(acao());
        historico.setValor(new BigDecimal("31.25"));
        historico.setDataHoraCotacao(NOW);
        historico.setDataHoraRegistro(NOW.plusMinutes(1));
        historico.setFonte(FonteCotacao.BRAPI);

        assertThat(HistoricoCotacaoMapper.toResponse(historico)).isEqualTo(
                new HistoricoCotacaoResponse(50L, 10L, new BigDecimal("31.25"), NOW, NOW.plusMinutes(1), FonteCotacao.BRAPI));
    }

    private Acao acao() {
        Acao acao = new Acao();
        acao.setId(10L);
        acao.setTicker("PETR4");
        acao.setNomeEmpresa("Petrobras");
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda(Moeda.BRL);
        acao.setCotacaoAtual(new BigDecimal("31.25"));
        acao.setDataHoraCotacao(NOW);
        return acao;
    }

    private Corretora corretora() {
        Corretora corretora = new Corretora();
        corretora.setId(20L);
        corretora.setCnpj("12345678000190");
        corretora.setRazaoSocial("Corretora S.A.");
        corretora.setNomeFantasia("Corretora");
        corretora.setEmail("contato@corretora.com");
        corretora.setTelefone("11999999999");
        corretora.setCep("01001000");
        corretora.setLogradouro("Praça da Sé");
        corretora.setNumero("10");
        corretora.setComplemento("Sala 1");
        corretora.setBairro("Sé");
        corretora.setCidade("São Paulo");
        corretora.setUf("SP");
        corretora.setSituacaoCadastral("ATIVA");
        corretora.setValidadaMercadoFinanceiro(true);
        corretora.setDataValidacaoMercado(NOW);
        corretora.setFonteValidacaoMercado("CVM");
        corretora.setDataCadastro(NOW);
        return corretora;
    }

    private Carteira carteira() {
        Carteira carteira = new Carteira();
        carteira.setId(30L);
        carteira.setNome("Longo prazo");
        carteira.setDescricao("Aposentadoria");
        carteira.setDataCriacao(NOW);
        return carteira;
    }
}
