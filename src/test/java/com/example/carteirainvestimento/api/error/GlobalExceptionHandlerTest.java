package com.example.carteirainvestimento.api.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.carteirainvestimento.exception.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ValidationController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void returnsDocumentedStableErrorForInvalidRequest() throws Exception {
        mockMvc.perform(post("/validation-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/validation-test"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void mapsApplicationErrorsToStableHttpStatusesWithoutSensitiveDetails() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/error/400"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/error/404"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/error/409"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/error/422"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ASSET_MARKET_MISMATCH"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/error/502"))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("EXTERNAL_INTEGRATION_ERROR"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/error/500"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @RestController
    static class ValidationController {

        @org.springframework.web.bind.annotation.GetMapping("/error/400")
        void badRequest() { throw new BusinessRuleException("invalid input"); }
        @org.springframework.web.bind.annotation.GetMapping("/error/404")
        void notFound() { throw new ResourceNotFoundException("missing"); }
        @org.springframework.web.bind.annotation.GetMapping("/error/409")
        void conflict() { throw new DuplicateResourceException("duplicate"); }
        @org.springframework.web.bind.annotation.GetMapping("/error/422")
        void mismatch() { throw new AssetMarketMismatchException("market mismatch"); }
        @org.springframework.web.bind.annotation.GetMapping("/error/502")
        void external() { throw new ExternalIntegrationException("provider failed"); }
        @org.springframework.web.bind.annotation.GetMapping("/error/500")
        void unexpected() { throw new RuntimeException("secret-token=do-not-leak"); }

        @PostMapping("/validation-test")
        void validate(@Valid @RequestBody ValidationRequest request) {
        }
    }

    record ValidationRequest(@NotBlank String name) {
    }
}
