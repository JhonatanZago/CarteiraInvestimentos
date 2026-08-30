package com.example.carteirainvestimento.api;

import com.example.carteirainvestimento.dto.common.PageResponse;
import com.example.carteirainvestimento.dto.corretora.CorretoraResponse;
import com.example.carteirainvestimento.dto.corretora.CorretoraCreateRequest;
import com.example.carteirainvestimento.mapper.CorretoraMapper;
import com.example.carteirainvestimento.service.CorretoraRegistrationService;
import com.example.carteirainvestimento.service.CorretoraQueryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Corretoras", description = "Cadastro e consulta de corretoras validadas")
@RequestMapping("/api/v1/corretoras")
public class CorretoraController {

    private final CorretoraQueryService service;
    private final CorretoraRegistrationService registrationService;

    public CorretoraController(CorretoraQueryService service, CorretoraRegistrationService registrationService) {
        this.service = service;
        this.registrationService = registrationService;
    }

    @GetMapping
    public PageResponse<CorretoraResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.list(PageRequest.of(page, size, Sort.by("razaoSocial").ascending())),
                CorretoraMapper::toResponse);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CorretoraResponse create(@jakarta.validation.Valid @RequestBody CorretoraCreateRequest request) {
        return CorretoraMapper.toResponse(registrationService.registrar(request));
    }

    @GetMapping("/{id}")
    public CorretoraResponse getById(@PathVariable Long id) {
        return CorretoraMapper.toResponse(service.findById(id));
    }

    @GetMapping("/cnpj/{cnpj}")
    public CorretoraResponse getByCnpj(@PathVariable String cnpj) {
        return CorretoraMapper.toResponse(service.findByCnpj(cnpj.replaceAll("\\D", "")));
    }
}
