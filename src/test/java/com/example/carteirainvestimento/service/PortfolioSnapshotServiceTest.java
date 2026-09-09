package com.example.carteirainvestimento.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.domain.Carteira;
import com.example.carteirainvestimento.domain.PortfolioSnapshot;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.enums.OrigemSnapshotCarteira;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.PortfolioSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PortfolioSnapshotServiceTest {
    @Test
    void dailyCloseUpdatesExistingDayInsteadOfDuplicating() {
        Carteira carteira = new Carteira();
        carteira.setId(1L);
        Acao acao = new Acao();
        acao.setMoeda(Moeda.BRL);
        acao.setMercado(Mercado.BRASIL);
        acao.setCotacaoAtual(new BigDecimal("12.00"));
        AtivoCarteira posicao = new AtivoCarteira();
        posicao.setQuantidade(new BigDecimal("10"));
        posicao.setPrecoMedio(new BigDecimal("10"));
        posicao.setAcao(acao);

        CarteiraRepository carteiras = mock(CarteiraRepository.class);
        AtivoCarteiraRepository ativos = mock(AtivoCarteiraRepository.class);
        PortfolioSnapshotRepository snapshots = mock(PortfolioSnapshotRepository.class);
        when(carteiras.findAll()).thenReturn(List.of(carteira));
        when(carteiras.getReferenceById(1L)).thenReturn(carteira);
        when(ativos.findByCarteiraId(1L)).thenReturn(List.of(posicao));
        when(snapshots.findFirstByCarteiraIdAndOrigemAndDataHoraBetween(eq(1L), eq(OrigemSnapshotCarteira.DAILY_CLOSE), any(), any()))
                .thenReturn(Optional.empty());

        PortfolioSnapshotService service = new PortfolioSnapshotService(carteiras, ativos, snapshots);
        service.registrarFechamentoDiario(LocalDate.of(2026, 9, 8));
        verify(snapshots, times(1)).save(any(PortfolioSnapshot.class));

        PortfolioSnapshot existente = new PortfolioSnapshot();
        when(snapshots.findFirstByCarteiraIdAndOrigemAndDataHoraBetween(eq(1L), eq(OrigemSnapshotCarteira.DAILY_CLOSE), any(), any()))
                .thenReturn(Optional.of(existente));
        service.registrarFechamentoDiario(LocalDate.of(2026, 9, 8));
        verify(snapshots, times(2)).save(any(PortfolioSnapshot.class));
    }
}
