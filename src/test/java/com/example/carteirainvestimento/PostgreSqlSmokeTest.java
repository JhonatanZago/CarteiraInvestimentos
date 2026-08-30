package com.example.carteirainvestimento;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@EnabledIfSystemProperty(named = "smoke.db.url", matches = ".+")
class PostgreSqlSmokeTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getProperty("smoke.db.url"));
        registry.add("spring.datasource.username", () -> System.getProperty("smoke.db.username"));
        registry.add("spring.datasource.password", () -> System.getProperty("smoke.db.password"));
        registry.add("spring.datasource.driver-class-name", () -> System.getProperty("smoke.db.driver"));
        registry.add("spring.h2.console.enabled", () -> false);
    }

    @Test
    void startsWithPostgreSqlAndCreatesTheSchemaConstraints() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }

        List<String> uniqueConstraints = jdbcTemplate.queryForList("""
                select lower(constraint_name)
                from information_schema.table_constraints
                where constraint_schema = current_schema()
                  and constraint_type = 'UNIQUE'
                """, String.class);

        assertThat(uniqueConstraints).contains(
                "uk_acao_ticker",
                "uk_corretora_cnpj",
                "uk_historico_cotacao_acao_data_hora",
                "uk_ativo_carteira_carteira_acao_corretora");
    }
}
