package com.example.carteirainvestimento.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI investmentPortfolioOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Carteira de Investimentos API")
                .version("v1")
                .description("API v1 para corretoras, acoes, carteiras, posicoes, historico e dashboard."))
                .tags(java.util.List.of(new Tag().name("Corretoras"), new Tag().name("Acoes"),
                        new Tag().name("Carteiras"), new Tag().name("Dashboard")))
                .components(new Components().addResponses("ValidationError", new ApiResponse().description("VALIDATION_ERROR: dados invalidos"))
                        .addResponses("DuplicateResource", new ApiResponse().description("DUPLICATE_RESOURCE: recurso ja cadastrado"))
                        .addResponses("BusinessRule", new ApiResponse().description("BUSINESS_RULE_VIOLATION: regra de negocio violada"))
                        .addResponses("NotFound", new ApiResponse().description("RESOURCE_NOT_FOUND: recurso inexistente"))
                        .addResponses("ExternalIntegration", new ApiResponse().description("EXTERNAL_INTEGRATION_ERROR: fonte externa indisponivel")));
    }
}
