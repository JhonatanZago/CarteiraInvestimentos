package com.example.carteirainvestimento.api;

import com.example.carteirainvestimento.dto.acao.AcaoResponse;
import com.example.carteirainvestimento.dto.common.PageResponse;
import com.example.carteirainvestimento.mapper.AcaoMapper;
import com.example.carteirainvestimento.service.AcaoQueryService;
import com.example.carteirainvestimento.service.AcaoRegistrationService;
import com.example.carteirainvestimento.service.CotacaoRefreshService;
import com.example.carteirainvestimento.repository.HistoricoCotacaoRepository;
import com.example.carteirainvestimento.dto.acao.AcaoCreateRequest;
import com.example.carteirainvestimento.dto.historico.HistoricoCotacaoResponse;
import com.example.carteirainvestimento.mapper.HistoricoCotacaoMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Acoes", description = "Cadastro, cotacao e historico de acoes")
@RequestMapping("/api/v1/acoes")
public class AcaoController {

    private final AcaoQueryService service;
    private final AcaoRegistrationService registration; private final CotacaoRefreshService refresh; private final HistoricoCotacaoRepository historicos;

    public AcaoController(AcaoQueryService service, AcaoRegistrationService registration, CotacaoRefreshService refresh, HistoricoCotacaoRepository historicos) {
        this.service = service; this.registration = registration; this.refresh = refresh; this.historicos = historicos;
    }

    @GetMapping
    public PageResponse<AcaoResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.list(PageRequest.of(page, size, Sort.by("ticker").ascending())), AcaoMapper::toResponse);
    }
    @PostMapping public AcaoResponse create(@jakarta.validation.Valid @RequestBody AcaoCreateRequest request) { return AcaoMapper.toResponse(registration.registrar(request)); }
    @GetMapping("/{id}") public AcaoResponse get(@PathVariable Long id) { return AcaoMapper.toResponse(service.findById(id)); }
    @GetMapping("/ticker/{ticker}") public AcaoResponse getTicker(@PathVariable String ticker) { return AcaoMapper.toResponse(service.findByTicker(ticker)); }
    @PostMapping("/{id}/atualizar-cotacao") public AcaoResponse refresh(@PathVariable Long id) { return AcaoMapper.toResponse(refresh.atualizar(id)); }
    @GetMapping("/{id}/historico") public List<HistoricoCotacaoResponse> history(@PathVariable Long id) { service.findById(id); return historicos.findByAcaoIdOrderByDataHoraCotacaoDesc(id).stream().map(HistoricoCotacaoMapper::toResponse).toList(); }
}
