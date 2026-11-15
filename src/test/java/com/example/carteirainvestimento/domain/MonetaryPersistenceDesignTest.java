package com.example.carteirainvestimento.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MonetaryPersistenceDesignTest {
    private static final Path DESIGN = Path.of(
            "openspec/changes/standardize-multicurrency-financial-consistency/migration-design.md");

    @Test
    void migrationDesignIsAdditiveAndPreservesUnresolvedRows() throws IOException {
        String design = Files.readString(DESIGN);

        assertThat(design).contains("ADD COLUMN IF NOT EXISTS moeda_base");
        assertThat(design).contains("CREATE TABLE IF NOT EXISTS portfolio_snapshot_cambio");
        assertThat(design).contains("UNRESOLVED");
        assertThat(design).contains("No default is assigned");
        assertThat(design).doesNotContain("DROP TABLE");
        assertThat(design).doesNotContain("DROP COLUMN");
        assertThat(design).doesNotContain("DELETE FROM");
    }
}
