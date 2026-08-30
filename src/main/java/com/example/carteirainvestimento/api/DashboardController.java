package com.example.carteirainvestimento.api;

import com.example.carteirainvestimento.dto.dashboard.DashboardCarteiraResponse;
import com.example.carteirainvestimento.service.DashboardCarteiraService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Dashboard", description = "Indicadores consolidados de carteira")
@RequestMapping("/api/v1/dashboard/carteiras")
public class DashboardController {

    private final DashboardCarteiraService dashboard;

    public DashboardController(DashboardCarteiraService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/{carteiraId}")
    public DashboardCarteiraResponse consultar(@PathVariable Long carteiraId) {
        return dashboard.calcular(carteiraId);
    }
}
