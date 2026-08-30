package com.example.carteirainvestimento.domain;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchemaGenerationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void generatesTheInvestmentDomainSchemaInH2() {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'PUBLIC'",
                String.class);

        assertThat(tables).contains("ACAO", "CORRETORA", "CARTEIRA", "ATIVO_CARTEIRA", "HISTORICO_COTACAO");
    }
}
