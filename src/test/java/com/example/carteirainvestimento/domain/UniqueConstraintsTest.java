package com.example.carteirainvestimento.domain;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UniqueConstraintsTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rejectsDuplicateTickers() {
        insertAcao("UNIQ-TICKER");

        assertThatThrownBy(() -> insertAcao("UNIQ-TICKER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateCnpjs() {
        jdbcTemplate.update("insert into corretora (cnpj, razao_social, validada_mercado_financeiro, data_cadastro) values (?, ?, ?, ?)",
                "12345678000190", "Corretora Única", true, Timestamp.from(Instant.now()));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into corretora (cnpj, razao_social, validada_mercado_financeiro, data_cadastro) values (?, ?, ?, ?)",
                "12345678000190", "Outra Corretora", true, Timestamp.from(Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateQuotationPoints() {
        insertAcao("UNIQ-HISTORICO");
        Long acaoId = jdbcTemplate.queryForObject("select id from acao where ticker = ?", Long.class, "UNIQ-HISTORICO");
        Timestamp quotationTime = Timestamp.from(Instant.parse("2026-08-25T12:00:00Z"));
        jdbcTemplate.update(
                "insert into historico_cotacao (acao_id, valor, data_hora_cotacao, data_hora_registro, fonte) values (?, ?, ?, ?, ?)",
                acaoId, 10.50, quotationTime, Timestamp.from(Instant.now()), "BRAPI");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into historico_cotacao (acao_id, valor, data_hora_cotacao, data_hora_registro, fonte) values (?, ?, ?, ?, ?)",
                acaoId, 11.00, quotationTime, Timestamp.from(Instant.now()), "BRAPI"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicatePortfolioPositions() {
        insertAcao("UNIQ-POSICAO");
        Long acaoId = jdbcTemplate.queryForObject("select id from acao where ticker = ?", Long.class, "UNIQ-POSICAO");
        Long carteiraId = insertCarteira();
        Long corretoraId = insertCorretora();
        jdbcTemplate.update(
                "insert into ativo_carteira (carteira_id, acao_id, corretora_id, quantidade, preco_medio, data_primeira_compra) values (?, ?, ?, ?, ?, current_date)",
                carteiraId, acaoId, corretoraId, 10, 25.00);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into ativo_carteira (carteira_id, acao_id, corretora_id, quantidade, preco_medio, data_primeira_compra) values (?, ?, ?, ?, ?, current_date)",
                carteiraId, acaoId, corretoraId, 20, 30.00))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertAcao(String ticker) {
        jdbcTemplate.update(
                "insert into acao (ticker, nome_empresa, mercado, moeda, cotacao_atual, data_hora_cotacao) values (?, ?, ?, ?, ?, ?)",
                ticker, "Empresa de Teste", "BRASIL", "BRL", 10.00, Timestamp.from(Instant.now()));
    }

    private Long insertCarteira() {
        jdbcTemplate.update("insert into carteira (nome, data_criacao) values (?, ?)",
                "Carteira de Teste", Timestamp.from(Instant.now()));
        return jdbcTemplate.queryForObject("select max(id) from carteira", Long.class);
    }

    private Long insertCorretora() {
        jdbcTemplate.update("insert into corretora (cnpj, razao_social, validada_mercado_financeiro, data_cadastro) values (?, ?, ?, ?)",
                "98765432000199", "Corretora da Posição", true, Timestamp.from(Instant.now()));
        return jdbcTemplate.queryForObject("select max(id) from corretora", Long.class);
    }
}
