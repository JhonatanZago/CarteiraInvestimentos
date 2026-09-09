package com.example.carteirainvestimento.api;

import com.example.carteirainvestimento.domain.AtivoCarteira;
import com.example.carteirainvestimento.dto.carteira.AtivoCarteiraRequest;
import com.example.carteirainvestimento.dto.carteira.AtivoCarteiraResponse;
import com.example.carteirainvestimento.dto.carteira.CarteiraRequest;
import com.example.carteirainvestimento.dto.carteira.CarteiraResponse;
import com.example.carteirainvestimento.dto.common.PageResponse;
import com.example.carteirainvestimento.mapper.AtivoCarteiraMapper;
import com.example.carteirainvestimento.mapper.CarteiraMapper;
import com.example.carteirainvestimento.service.AtivoCarteiraService;
import com.example.carteirainvestimento.service.CarteiraService;
import jakarta.validation.Valid;
import java.util.List;
import com.example.carteirainvestimento.enums.Mercado;
import com.example.carteirainvestimento.dto.insight.ClassificacaoAlocacao;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.carteirainvestimento.service.ExchangeRateService;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Carteiras", description = "Carteiras e posicoes de investimento")
@RequestMapping("/api/v1/carteiras")
public class CarteiraController {

    private final CarteiraService carteiras;
    private final AtivoCarteiraService ativos;
    private final ExchangeRateService exchangeRates;

    public CarteiraController(CarteiraService carteiras, AtivoCarteiraService ativos) {
        this(carteiras, ativos, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CarteiraController(CarteiraService carteiras, AtivoCarteiraService ativos, ExchangeRateService exchangeRates) {
        this.carteiras = carteiras;
        this.ativos = ativos;
        this.exchangeRates = exchangeRates;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarteiraResponse criar(@Valid @RequestBody CarteiraRequest request) {
        return CarteiraMapper.toResponse(carteiras.criar(request));
    }

    @GetMapping
    public PageResponse<CarteiraResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(carteiras.listar(PageRequest.of(page, size, Sort.by("nome").ascending())),
                CarteiraMapper::toResponse);
    }

    @GetMapping("/{id}")
    public CarteiraResponse buscar(@PathVariable Long id) {
        return CarteiraMapper.toResponse(carteiras.buscar(id));
    }

    @PutMapping("/{id}")
    public CarteiraResponse atualizar(@PathVariable Long id, @Valid @RequestBody CarteiraRequest request) {
        return CarteiraMapper.toResponse(carteiras.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        carteiras.excluir(id);
    }

    @PostMapping("/{carteiraId}/posicoes")
    @ResponseStatus(HttpStatus.CREATED)
    public AtivoCarteiraResponse criarPosicao(
            @PathVariable Long carteiraId, @Valid @RequestBody AtivoCarteiraRequest request) {
        return map(ativos.criar(carteiraId, request));
    }

    @GetMapping("/{carteiraId}/posicoes")
    public List<AtivoCarteiraResponse> listarPosicoes(@PathVariable Long carteiraId,
            @RequestParam(required = false) Mercado mercado,
            @RequestParam(required = false) ClassificacaoAlocacao classificacao,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String ordenarPor) {
        return ativos.listar(carteiraId, mercado, classificacao, busca, ordenarPor).stream().map(this::map).toList();
    }

    @GetMapping("/{carteiraId}/posicoes/paginadas")
    public PageResponse<AtivoCarteiraResponse> listarPosicoesPaginadas(@PathVariable Long carteiraId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Mercado mercado,
            @RequestParam(required = false) ClassificacaoAlocacao classificacao,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String ordenarPor) {
        List<AtivoCarteiraResponse> filtradas = ativos.listar(carteiraId, mercado, classificacao, busca, ordenarPor)
                .stream().map(this::map).toList();
        int inicio = Math.min(page * size, filtradas.size());
        int fim = Math.min(inicio + size, filtradas.size());
        return new PageResponse<>(filtradas.subList(inicio, fim), page, size, filtradas.size(),
                (int) Math.ceil((double) filtradas.size() / size));
    }

    @PutMapping("/{carteiraId}/posicoes/{posicaoId}")
    public AtivoCarteiraResponse atualizarPosicao(
            @PathVariable Long carteiraId, @PathVariable Long posicaoId,
            @Valid @RequestBody AtivoCarteiraRequest request) {
        return map(ativos.atualizar(carteiraId, posicaoId, request));
    }

    @DeleteMapping("/{carteiraId}/posicoes/{posicaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirPosicao(@PathVariable Long carteiraId, @PathVariable Long posicaoId) {
        ativos.excluir(carteiraId, posicaoId);
    }

    private AtivoCarteiraResponse map(AtivoCarteira ativo) {
        var cambio = exchangeRates == null ? null : exchangeRates.usdToBrl();
        return AtivoCarteiraMapper.toResponse(ativo, cambio, null, cambio == null ? null : "MARKET_INDICATOR");
    }
}
