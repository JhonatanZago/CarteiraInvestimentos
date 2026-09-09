package com.example.carteirainvestimento.service;

import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.dto.dashboard.ComposicaoCarteiraResponse;
import com.example.carteirainvestimento.dto.dashboard.DashboardCarteiraResponse;
import com.example.carteirainvestimento.dto.insight.ClassificacaoAlocacao;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.repository.AtivoCarteiraRepository;
import com.example.carteirainvestimento.repository.CarteiraRepository;
import com.example.carteirainvestimento.repository.MovimentacaoFinanceiraRepository;
import com.example.carteirainvestimento.domain.MovimentacaoFinanceira;
import com.example.carteirainvestimento.enums.TipoMovimentacao;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import com.example.carteirainvestimento.dto.dashboard.AnaliseMoedasResponse;
import com.example.carteirainvestimento.enums.Moeda;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardCarteiraService {

    private final CarteiraRepository carteiras;
    private final AtivoCarteiraRepository ativos;
    private final ExchangeRateService exchangeRates;
    private final MovimentacaoFinanceiraRepository movimentacoes;

    public DashboardCarteiraService(CarteiraRepository carteiras, AtivoCarteiraRepository ativos) {
        this(carteiras, ativos, null, null);
    }
    public DashboardCarteiraService(CarteiraRepository carteiras, AtivoCarteiraRepository ativos, ExchangeRateService exchangeRates) { this(carteiras, ativos, exchangeRates, null); }
    @org.springframework.beans.factory.annotation.Autowired
    public DashboardCarteiraService(CarteiraRepository carteiras, AtivoCarteiraRepository ativos, ExchangeRateService exchangeRates, MovimentacaoFinanceiraRepository movimentacoes) { this.carteiras = carteiras; this.ativos = ativos; this.exchangeRates = exchangeRates; this.movimentacoes = movimentacoes; }

    @Transactional(readOnly = true)
    public DashboardCarteiraResponse calcular(Long carteiraId) {
        if (!carteiras.existsById(carteiraId)) {
            throw new ResourceNotFoundException("Carteira nao encontrada");
        }

        BigDecimal taxaUsdBrl = exchangeRates == null ? null : exchangeRates.usdToBrl();
        List<AtivoCarteira> posicoes = ativos.findByCarteiraId(carteiraId);
        List<ComposicaoCarteiraResponse> composicao = consolidar(carteiraId, posicoes, taxaUsdBrl);
        BigDecimal valorInvestido = composicao.stream()
                .map(item -> convertido(item.valorInvestido(), item.moeda(), taxaUsdBrl))
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean cotacoesDisponiveis = composicao.stream().allMatch(item -> convertido(item.valorAtual(), item.moeda(), taxaUsdBrl) != null);
        BigDecimal valorAtual = cotacoesDisponiveis
                ? composicao.stream().map(item -> convertido(item.valorAtual(), item.moeda(), taxaUsdBrl)).reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;
        PosicaoFinanceira totais = composicao.isEmpty()
                ? new PosicaoFinanceira(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO)
                : CalculadoraFinanceira.calcular(BigDecimal.ONE, valorInvestido, valorAtual);
        OffsetDateTime ultimaAtualizacao = posicoes.stream()
                .map(posicao -> posicao.getAcao().getDataHoraCotacao())
                .filter(java.util.Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);

        return new DashboardCarteiraResponse(carteiraId,
                totais.valorInvestido(), totais.valorAtual(), totais.resultado(), totais.rentabilidadePercentual(),
                ultimaAtualizacao, composicao.size(), composicao);
    }

    private List<ComposicaoCarteiraResponse> consolidar(Long carteiraId, List<AtivoCarteira> posicoes, BigDecimal taxaUsdBrl) {
        Map<String, List<AtivoCarteira>> porAcao = posicoes.stream().collect(java.util.stream.Collectors.groupingBy(p -> p.getAcao().getId() == null ? "transient-" + System.identityHashCode(p.getAcao()) : "acao-" + p.getAcao().getId(), LinkedHashMap::new, java.util.stream.Collectors.toList()));
        return porAcao.values().stream().map(grupo -> composicao(carteiraId, grupo, taxaUsdBrl)).filter(java.util.Objects::nonNull).toList();
    }

    private ComposicaoCarteiraResponse composicao(Long carteiraId, List<AtivoCarteira> grupo, BigDecimal taxaUsdBrl) {
        AtivoCarteira posicao = grupo.getFirst();
        CustoAberto custo = custoAberto(carteiraId, posicao.getAcao().getId(), grupo);
        // A posicao encerrada por movimentacoes sai da tabela; a projeção legada de quantidade zero continua compatível.
        if (custo.quantidade().signum() <= 0 && movimentacoes != null
                && !movimentacoes.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscIdAsc(carteiraId, posicao.getAcao().getId()).isEmpty()) return null;
        PosicaoFinanceira valores = CalculadoraFinanceira.calcular(custo.quantidade(), custo.precoMedio(), posicao.getAcao().getCotacaoAtual());
        BigDecimal investidoBase = convertido(valores.valorInvestido(), posicao.getAcao().getMoeda(), taxaUsdBrl);
        BigDecimal atualBase = convertido(valores.valorAtual(), posicao.getAcao().getMoeda(), taxaUsdBrl);
        return new ComposicaoCarteiraResponse(posicao.getId(), posicao.getAcao().getId(), posicao.getAcao().getMercado(),
                ClassificadorAlocacao.para(posicao.getAcao().getMercado()), posicao.getAcao().getTicker(),
                posicao.getAcao().getNomeEmpresa(), posicao.getAcao().getLogoUrl(), custo.quantidade(), custo.precoMedio(), posicao.getAcao().getCotacaoAtual(),
                posicao.getAcao().getDataHoraCotacao(), valores.valorInvestido(), valores.valorAtual(), valores.resultado(),
                valores.rentabilidadePercentual(), posicao.getAcao().getMoeda(), posicao.isConversaoEstimada(),
                investidoBase, atualBase, posicao.getAcao().getMoeda() == Moeda.BRL ? BigDecimal.ONE : taxaUsdBrl);
    }

    /** Venda reduz o custo aberto proporcionalmente; seu preco nao altera o preco medio restante. */
    private CustoAberto custoAberto(Long carteiraId, Long acaoId, List<AtivoCarteira> legado) {
        List<MovimentacaoFinanceira> historico = movimentacoes == null ? List.of() : movimentacoes.findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscIdAsc(carteiraId, acaoId);
        if (historico.isEmpty()) {
            BigDecimal quantidade = legado.stream().map(AtivoCarteira::getQuantidade).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal custo = legado.stream().map(p -> p.getQuantidade().multiply(p.getPrecoMedio())).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CustoAberto(quantidade, quantidade.signum() == 0 ? BigDecimal.ZERO : custo.divide(quantidade, 8, RoundingMode.HALF_EVEN));
        }
        BigDecimal quantidade = BigDecimal.ZERO, custo = BigDecimal.ZERO;
        for (MovimentacaoFinanceira item : historico) {
            if (item.getTipo() == TipoMovimentacao.COMPRA) { quantidade = quantidade.add(item.getQuantidade()); custo = custo.add(item.getQuantidade().multiply(item.getPrecoUnitario())); }
            else if (quantidade.signum() > 0) { BigDecimal custoVendido = custo.multiply(item.getQuantidade()).divide(quantidade, 8, RoundingMode.HALF_EVEN); quantidade = quantidade.subtract(item.getQuantidade()); custo = custo.subtract(custoVendido); }
        }
        return new CustoAberto(quantidade, quantidade.signum() == 0 ? BigDecimal.ZERO : custo.divide(quantidade, 8, RoundingMode.HALF_EVEN));
    }
    private record CustoAberto(BigDecimal quantidade, BigDecimal precoMedio) { }

    @Transactional(readOnly = true)
    public AnaliseMoedasResponse analisarMoedas(Long carteiraId) {
        if (!carteiras.existsById(carteiraId)) throw new ResourceNotFoundException("Carteira nao encontrada");
        List<AtivoCarteira> posicoes = ativos.findByCarteiraId(carteiraId);
        BigDecimal taxaUsdBrl = exchangeRates == null ? null : exchangeRates.usdToBrl();
        var linhas = consolidar(carteiraId, posicoes, taxaUsdBrl);
        BigDecimal total = linhas.stream().map(x -> convertido(x, taxaUsdBrl)).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean conversaoDisponivel = linhas.stream().allMatch(x -> x.moeda() == Moeda.BRL || taxaUsdBrl != null);
        var exposicoes = java.util.Arrays.stream(Moeda.values()).map(m -> {
            BigDecimal valor = linhas.stream().filter(x -> x.moeda() == m).map(ComposicaoCarteiraResponse::valorAtual).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal convertido = m == Moeda.USD && taxaUsdBrl != null ? valor.multiply(taxaUsdBrl) : valor;
            BigDecimal percentual = total.signum() > 0 && conversaoDisponivel ? convertido.divide(total, 8, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : null;
            return new AnaliseMoedasResponse.ExposicaoMoeda(m, m == Moeda.BRL ? "Real brasileiro" : "Dolar americano", valor, convertido, percentual);
        }).filter(x -> x.valorOriginal().signum() > 0).toList();
        var resultados = java.util.Arrays.stream(Moeda.values()).map(m -> new AnaliseMoedasResponse.ResultadoMoeda(m, linhas.stream().filter(x -> x.moeda() == m).map(ComposicaoCarteiraResponse::resultado).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add))).filter(x -> x.resultado().signum() != 0).toList();
        var ativosResultado = linhas.stream().map(x -> new AnaliseMoedasResponse.ResultadoAtivo(x.acaoId(), x.ticker(), x.nomeEmpresa(), x.logoUrl(), x.moeda(), x.resultado(), x.rentabilidadePercentual())).toList();
        boolean possuiEstimativa = linhas.stream().anyMatch(ComposicaoCarteiraResponse::conversaoEstimada);
        String mensagem = !conversaoDisponivel ? "Conversao cambial indisponivel"
                : possuiEstimativa ? "Conversao cambial estimada: historico da compra nao comprovado" : null;
        return new AnaliseMoedasResponse(exposicoes, (int) ativosResultado.stream().filter(x -> x.resultado() != null && x.resultado().signum() > 0).count(), (int) ativosResultado.stream().filter(x -> x.resultado() != null && x.resultado().signum() < 0).count(), (int) ativosResultado.stream().filter(x -> x.resultado() != null && x.resultado().signum() == 0).count(), resultados, ativosResultado, Moeda.BRL, mensagem);
    }

    private BigDecimal convertido(ComposicaoCarteiraResponse item, BigDecimal taxaUsdBrl) {
        return convertido(item.valorAtual(), item.moeda(), taxaUsdBrl);
    }
    private BigDecimal convertido(BigDecimal valor, Moeda moeda, BigDecimal taxaUsdBrl) {
        if (valor == null) return null;
        if (moeda == Moeda.USD) return taxaUsdBrl == null ? null : valor.multiply(taxaUsdBrl);
        return valor;
    }
}
