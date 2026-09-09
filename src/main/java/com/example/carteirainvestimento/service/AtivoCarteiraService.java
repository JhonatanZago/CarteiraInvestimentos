package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.Acao;
import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.domain.Carteira;
import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.dto.carteira.AtivoCarteiraRequest;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.repository.AcaoRepository;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.CorretoraRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Comparator;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.dto.insight.ClassificacaoAlocacao;
import com.example.carteirainvestimento.enums.OrigemSnapshotCarteira;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtivoCarteiraService {

    private final CarteiraRepository carteiras;
    private final AcaoRepository acoes;
    private final CorretoraRepository corretoras;
    private final AtivoCarteiraRepository ativos;
    private final PortfolioSnapshotService snapshots;

    public AtivoCarteiraService(CarteiraRepository carteiras, AcaoRepository acoes,
            CorretoraRepository corretoras, AtivoCarteiraRepository ativos, PortfolioSnapshotService snapshots) {
        this.carteiras = carteiras;
        this.acoes = acoes;
        this.corretoras = corretoras;
        this.ativos = ativos;
        this.snapshots = snapshots;
    }

    @Transactional
    public AtivoCarteira criar(Long carteiraId, AtivoCarteiraRequest request) {
        validarDados(request);
        Carteira carteira = buscarCarteira(carteiraId);
        validarPosicaoUnica(carteiraId, request.acaoId(), request.corretoraId());

        AtivoCarteira ativo = new AtivoCarteira();
        preencher(ativo, carteira, request);
        AtivoCarteira salvo = ativos.save(ativo); snapshots.registrarEvento(carteiraId, OrigemSnapshotCarteira.POSITION_CREATED); return salvo;
    }

    @Transactional(readOnly = true)
    public List<AtivoCarteira> listar(Long carteiraId) {
        buscarCarteira(carteiraId);
        return ativos.findByCarteiraId(carteiraId);
    }

    @Transactional(readOnly = true)
    public List<AtivoCarteira> listar(Long carteiraId, Mercado mercado, ClassificacaoAlocacao classificacao,
            String busca, String ordenarPor) {
        Comparator<AtivoCarteira> ordem = switch (ordenarPor == null ? "ticker" : ordenarPor) {
            case "valorAtual" -> Comparator.comparing(a -> CalculadoraFinanceira.calcular(a.getQuantidade(), a.getPrecoMedio(), a.getAcao().getCotacaoAtual()).valorAtual());
            case "resultado" -> Comparator.comparing(a -> CalculadoraFinanceira.calcular(a.getQuantidade(), a.getPrecoMedio(), a.getAcao().getCotacaoAtual()).resultado());
            case "rentabilidade" -> Comparator.comparing(a -> CalculadoraFinanceira.calcular(a.getQuantidade(), a.getPrecoMedio(), a.getAcao().getCotacaoAtual()).rentabilidadePercentual());
            default -> Comparator.comparing(a -> a.getAcao().getTicker());
        };
        return listar(carteiraId).stream()
                .filter(a -> mercado == null || a.getAcao().getMercado() == mercado)
                .filter(a -> classificacao == null || ClassificadorAlocacao.para(a.getAcao().getMercado()) == classificacao)
                .filter(a -> busca == null || busca.isBlank() || a.getAcao().getTicker().toLowerCase().contains(busca.toLowerCase()))
                .sorted(ordem).toList();
    }

    @Transactional
    public AtivoCarteira atualizar(Long carteiraId, Long ativoId, AtivoCarteiraRequest request) {
        validarDados(request);
        AtivoCarteira ativo = buscarAtivoDaCarteira(carteiraId, ativoId);
        if (ativos.existsByCarteiraIdAndAcaoIdAndCorretoraIdAndIdNot(
                carteiraId, request.acaoId(), request.corretoraId(), ativoId)) {
            throw new DuplicateResourceException("Posicao ja cadastrada para esta acao e corretora");
        }

        preencher(ativo, ativo.getCarteira(), request);
        AtivoCarteira salvo = ativos.save(ativo); snapshots.registrarEvento(carteiraId, OrigemSnapshotCarteira.POSITION_UPDATED); return salvo;
    }

    @Transactional
    public void excluir(Long carteiraId, Long ativoId) {
        ativos.delete(buscarAtivoDaCarteira(carteiraId, ativoId)); ativos.flush(); snapshots.registrarEvento(carteiraId, OrigemSnapshotCarteira.POSITION_DELETED);
    }

    private void preencher(AtivoCarteira ativo, Carteira carteira, AtivoCarteiraRequest request) {
        ativo.setCarteira(carteira);
        ativo.setAcao(buscarAcao(request.acaoId()));
        ativo.setCorretora(buscarCorretora(request.corretoraId()));
        ativo.setQuantidade(request.quantidade());
        ativo.setPrecoMedio(request.precoMedio());
        ativo.setDataPrimeiraCompra(request.dataPrimeiraCompra());
        ativo.setCambioHistoricoComprovado(ativo.getAcao().getMoeda() == com.example.carteirainvestimento.enums.Moeda.BRL);
    }

    private void validarDados(AtivoCarteiraRequest request) {
        if (request.quantidade() == null || request.quantidade().signum() <= 0) {
            throw new BusinessRuleException("Quantidade deve ser maior que zero");
        }
        if (request.precoMedio() == null || request.precoMedio().signum() <= 0) {
            throw new BusinessRuleException("Preco medio deve ser maior que zero");
        }
        if (request.dataPrimeiraCompra() == null || request.dataPrimeiraCompra().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Data da primeira compra nao pode estar no futuro");
        }
    }

    private void validarPosicaoUnica(Long carteiraId, Long acaoId, Long corretoraId) {
        if (ativos.existsByCarteiraIdAndAcaoIdAndCorretoraId(carteiraId, acaoId, corretoraId)) {
            throw new DuplicateResourceException("Posicao ja cadastrada para esta acao e corretora");
        }
    }

    private Carteira buscarCarteira(Long id) {
        return carteiras.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira nao encontrada"));
    }

    private Acao buscarAcao(Long id) {
        return acoes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Acao nao encontrada"));
    }

    private Corretora buscarCorretora(Long id) {
        return corretoras.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Corretora nao encontrada"));
    }

    private AtivoCarteira buscarAtivoDaCarteira(Long carteiraId, Long ativoId) {
        AtivoCarteira ativo = ativos.findById(ativoId)
                .orElseThrow(() -> new ResourceNotFoundException("Posicao nao encontrada"));
        if (!ativo.getCarteira().getId().equals(carteiraId)) {
            throw new ResourceNotFoundException("Posicao nao encontrada nesta carteira");
        }
        return ativo;
    }
}
