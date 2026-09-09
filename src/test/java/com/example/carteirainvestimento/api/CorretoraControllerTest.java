package com.example.carteirainvestimento.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carteirainvestimento.api.error.GlobalExceptionHandler;
import com.example.carteirainvestimento.domain.Corretora;
import com.example.carteirainvestimento.exception.BusinessRuleException;
import com.example.carteirainvestimento.exception.DuplicateResourceException;
import com.example.carteirainvestimento.exception.ResourceNotFoundException;
import com.example.carteirainvestimento.service.CorretoraQueryService;
import com.example.carteirainvestimento.service.CorretoraRegistrationService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CorretoraControllerTest {

    private CorretoraQueryService queryService;
    private CorretoraRegistrationService registrationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        queryService = org.mockito.Mockito.mock(CorretoraQueryService.class);
        registrationService = org.mockito.Mockito.mock(CorretoraRegistrationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CorretoraController(queryService, registrationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createsListsAndRetrievesBroker() throws Exception {
        Corretora corretora = corretora();
        when(registrationService.registrar(any())).thenReturn(corretora);
        when(queryService.list(any())).thenReturn(new PageImpl<>(List.of(corretora)));
        when(queryService.findById(1L)).thenReturn(corretora);
        when(queryService.findByCnpj("12345678000195")).thenReturn(corretora);

        mockMvc.perform(post("/api/v1/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").value("12345678000195"));

        mockMvc.perform(get("/api/v1/corretoras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].razaoSocial").value("Corretora Oficial S.A."));

        mockMvc.perform(get("/api/v1/corretoras/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        mockMvc.perform(get("/api/v1/corretoras/cnpj/12345678000195"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("12345678000195"));
    }

    @Test
    void returnsConflictForDuplicateCnpj() throws Exception {
        when(registrationService.registrar(any()))
                .thenThrow(new DuplicateResourceException("CNPJ ja cadastrado"));

        postBroker().andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void returnsNotFoundForUnknownCompany() throws Exception {
        when(registrationService.registrar(any()))
                .thenThrow(new ResourceNotFoundException("CNPJ nao encontrado na BrasilAPI"));

        postBroker().andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void returnsConflictForInactiveCompany() throws Exception {
        when(registrationService.registrar(any()))
                .thenThrow(new BusinessRuleException("Empresa nao esta ativa"));

        postBroker().andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void returnsConflictForUnauthorizedFinancialInstitution() throws Exception {
        when(registrationService.registrar(any()))
                .thenThrow(new BusinessRuleException("Instituicao financeira nao autorizada pela CVM"));

        postBroker().andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void returnsNotFoundForUnknownCep() throws Exception {
        when(registrationService.registrar(any()))
                .thenThrow(new ResourceNotFoundException("CEP nao encontrado no ViaCEP"));

        postBroker().andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private org.springframework.test.web.servlet.ResultActions postBroker() throws Exception {
        return mockMvc.perform(post("/api/v1/corretoras")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson()));
    }

    private String requestJson() {
        return """
                {"cnpj":"12.345.678/0001-95","cep":"01001-000","numero":"10","complemento":"Sala 1"}
                """;
    }

    private Corretora corretora() {
        Corretora corretora = new Corretora();
        corretora.setId(1L);
        corretora.setCnpj("12345678000195");
        corretora.setRazaoSocial("Corretora Oficial S.A.");
        corretora.setCep("01001000");
        corretora.setValidadaMercadoFinanceiro(true);
        corretora.setDataCadastro(OffsetDateTime.of(2026, 8, 30, 12, 0, 0, 0, ZoneOffset.UTC));
        return corretora;
    }
}
