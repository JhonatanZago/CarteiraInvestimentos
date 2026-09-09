package com.example.carteirainvestimento.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.ManyToOne;
import java.lang.reflect.Field;
import org.hibernate.annotations.OnDelete;
import org.junit.jupiter.api.Test;

class RelationshipIntegrityTest {
    @Test
    void portfolioSnapshotsDoNotCascadeDeleteTheirPortfolio() throws Exception {
        Field carteira = PortfolioSnapshot.class.getDeclaredField("carteira");
        assertThat(carteira.isAnnotationPresent(ManyToOne.class)).isTrue();
        assertThat(carteira.isAnnotationPresent(OnDelete.class)).isFalse();
    }
}
