package com.example.carteirainvestimento.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.enums.Moeda;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import com.example.carteirainvestimento.repository.PortfolioSnapshotRepository;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.HistoricoCotacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaginationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AcaoRepository acaoRepository;

    @Autowired
    private CorretoraRepository corretoraRepository;

    @Autowired private AtivoCarteiraRepository ativoCarteiraRepository;
    @Autowired private PortfolioSnapshotRepository portfolioSnapshotRepository;
    @Autowired private CarteiraRepository carteiraRepository;
    @Autowired private HistoricoCotacaoRepository historicoCotacaoRepository;

    @BeforeEach
    void setUp() {
        ativoCarteiraRepository.deleteAll();
        portfolioSnapshotRepository.deleteAll();
        carteiraRepository.deleteAll();
        historicoCotacaoRepository.deleteAll();
        acaoRepository.deleteAll();
        corretoraRepository.deleteAll();
    }

    @Test
    void returnsRequestedStockPageOrderedByTicker() throws Exception {
        saveAcao("ZZZZ3");
        saveAcao("AAAA3");
        saveAcao("MMMM3");

        mockMvc.perform(get("/api/v1/acoes?page=1&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].ticker").value("MMMM3"));
    }

    @Test
    void returnsRequestedBrokerPageOrderedByCorporateName() throws Exception {
        saveCorretora("Zeta Corretora", "10000000000100");
        saveCorretora("Alfa Corretora", "10000000000200");
        saveCorretora("Média Corretora", "10000000000300");

        mockMvc.perform(get("/api/v1/corretoras?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].razaoSocial").value("Alfa Corretora"))
                .andExpect(jsonPath("$.content[1].razaoSocial").value("Média Corretora"));
    }

    private void saveAcao(String ticker) {
        Acao acao = new Acao();
        acao.setTicker(ticker);
        acao.setNomeEmpresa(ticker + " S.A.");
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda(Moeda.BRL);
        acao.setCotacaoAtual(BigDecimal.TEN);
        acao.setDataHoraCotacao(OffsetDateTime.of(2026, 8, 25, 12, 0, 0, 0, ZoneOffset.UTC));
        acaoRepository.save(acao);
    }

    private void saveCorretora(String razaoSocial, String cnpj) {
        Corretora corretora = new Corretora();
        corretora.setCnpj(cnpj);
        corretora.setRazaoSocial(razaoSocial);
        corretora.setValidadaMercadoFinanceiro(true);
        corretora.setDataCadastro(OffsetDateTime.of(2026, 8, 25, 12, 0, 0, 0, ZoneOffset.UTC));
        corretoraRepository.save(corretora);
    }
}
