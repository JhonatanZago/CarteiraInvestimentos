package com.example.carteirainvestimento.api;

import com.example.carteirainvestimento.dto.dashboard.AnaliseMoedasResponse;
import com.example.carteirainvestimento.service.DashboardCarteiraService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carteiras")
public class AnaliseMoedasController {
    private final DashboardCarteiraService service;
    public AnaliseMoedasController(DashboardCarteiraService service) { this.service = service; }
    @GetMapping("/{carteiraId}/analise-moedas")
    public AnaliseMoedasResponse consultar(@PathVariable Long carteiraId) { return service.analisarMoedas(carteiraId); }
}
