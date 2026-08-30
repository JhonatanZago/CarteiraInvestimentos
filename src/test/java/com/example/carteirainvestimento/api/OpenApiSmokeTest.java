package com.example.carteirainvestimento.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSmokeTest {
    @Autowired private MockMvc mockMvc;
    @Test void exposesAllVersionedResourcesInOpenApi() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/corretoras']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/acoes']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/carteiras']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/dashboard/carteiras/{carteiraId}']").exists())
                .andExpect(jsonPath("$.components.schemas.AtivoCarteiraResponse.properties.ticker.example").value("PETR4"))
                .andExpect(jsonPath("$.components.schemas.AtivoCarteiraResponse.properties.valorAtual.example").value(3125.0))
                .andExpect(jsonPath("$.components.schemas.DashboardCarteiraResponse.properties.quantidadeAtivos.example").value(3))
                .andExpect(jsonPath("$.components.schemas.DashboardCarteiraResponse.properties.composicao").exists())
                .andExpect(jsonPath("$.components.schemas.ComposicaoCarteiraResponse.properties.ticker.example").value("PETR4"));
    }
}
