package com.example.carteirainvestimento.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.carteirainvestimento.domain.Usuario;
import com.example.carteirainvestimento.enums.PerfilUsuario;
import com.example.carteirainvestimento.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.security.test-permit-all=false",
        "app.auth.refresh-cookie-secure=true",
        "app.auth.refresh-cookie-same-site=Strict"
})
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class UsuarioAdministracaoSecurityTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder encoder;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void enforcesAuthenticationAndAdminRoleForListing() throws Exception {
        mockMvc.perform(get("/api/v1/admin/usuarios"))
                .andExpect(status().isUnauthorized());

        String userToken = registerAndLogin("regular");
        mockMvc.perform(get("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String adminToken = adminToken();
        mockMvc.perform(get("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/usuarios/{id}", 999_999_999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanCreateAndUpdateUserWithoutReturningPasswordData() throws Exception {
        String adminToken = adminToken();
        String email = uniqueEmail("created");
        MvcResult created = mockMvc.perform(post("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Criado\",\"email\":\"" + email
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.perfil").value("USER"))
                .andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Atualizado\",\"email\":\"" + email
                                + "\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMIN"));
    }

    @Test
    void rejectsDuplicateEmailAndUserAdministrativeAttempts() throws Exception {
        String email = uniqueEmail("duplicate");
        registerAndLoginWithEmail(email);
        String adminToken = adminToken();

        mockMvc.perform(post("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Duplicado\",\"email\":\"" + email
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isConflict());

        String blockedEmail = uniqueEmail("blocked-admin");
        String userToken = registerAndLoginWithEmail(blockedEmail);
        long userId = usuarios.findByEmailIgnoreCase(blockedEmail).orElseThrow().getId();
        mockMvc.perform(post("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Nao\",\"email\":\"" + uniqueEmail("blocked")
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/admin/usuarios/{id}", userId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Nao\",\"email\":\"" + blockedEmail + "\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivationAndPasswordAdministrationAreSafe() throws Exception {
        String adminToken = adminToken();
        String email = uniqueEmail("managed");
        long id = createUser(adminToken, email, "Senha123!");

        mockMvc.perform(patch("/api/v1/admin/usuarios/{id}/status", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        login(email, "Senha123!").andExpect(status().isBadRequest());

        String activeEmail = uniqueEmail("password");
        long activeId = createUser(adminToken, activeEmail, "Senha123!");
        assertTrue(usuarios.findById(activeId).orElseThrow().getSenha().startsWith("$2"));
        mockMvc.perform(put("/api/v1/admin/usuarios/{id}/senha", activeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha\":\"NovaSenha123!\",\"confirmacaoSenha\":\"NovaSenha123!\"}"))
                .andExpect(status().isNoContent());
        assertTrue(encoder.matches("NovaSenha123!", usuarios.findById(activeId).orElseThrow().getSenha()));
        login(activeEmail, "Senha123!").andExpect(status().isBadRequest());
        login(activeEmail, "NovaSenha123!").andExpect(status().isOk());
    }

    @Test
    void cannotDeactivateLastActiveAdminOrAccessPrivatePortfolio() throws Exception {
        String adminEmail = uniqueEmail("last-admin");
        String adminToken = registerAndLoginWithEmail(adminEmail);
        Usuario admin = usuarios.findByEmailIgnoreCase(adminEmail).orElseThrow();
        admin.setPerfil(PerfilUsuario.ADMIN);
        usuarios.saveAndFlush(admin);
        var otherAdmins = usuarios.findAll().stream()
                .filter(user -> user.getPerfil() == PerfilUsuario.ADMIN && !user.getId().equals(admin.getId()))
                .peek(user -> user.setAtivo(false))
                .toList();
        usuarios.saveAllAndFlush(otherAdmins);
        assertTrue(usuarios.findById(admin.getId()).orElseThrow().getPerfil() == PerfilUsuario.ADMIN);
        assertTrue(usuarios.findById(admin.getId()).orElseThrow().getAtivo());
        adminToken = loginToken(adminEmail, "Senha123!");

        mockMvc.perform(patch("/api/v1/admin/usuarios/{id}/status", admin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isBadRequest());

        String ownerEmail = uniqueEmail("private-owner");
        String ownerToken = registerAndLoginWithEmail(ownerEmail);
        long portfolioId = createPortfolio(ownerToken);
        mockMvc.perform(get("/api/v1/carteiras/{id}", portfolioId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    private String adminToken() throws Exception {
        String email = uniqueEmail("admin");
        registerAndLoginWithEmail(email);
        Usuario admin = usuarios.findByEmailIgnoreCase(email).orElseThrow();
        admin.setPerfil(PerfilUsuario.ADMIN);
        usuarios.saveAndFlush(admin);
        return loginToken(email, "Senha123!");
    }

    private long createUser(String adminToken, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Gerenciado\",\"email\":\"" + email
                                + "\",\"senha\":\"" + password + "\",\"confirmacaoSenha\":\"" + password + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String registerAndLogin(String prefix) throws Exception {
        return registerAndLoginWithEmail(uniqueEmail(prefix));
    }

    private String registerAndLoginWithEmail(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Usuario\",\"email\":\"" + email
                                + "\",\"senha\":\"Senha123!\",\"confirmacaoSenha\":\"Senha123!\"}"))
                .andExpect(status().isCreated());
        return loginToken(email, "Senha123!");
    }

    private String loginToken(String email, String password) throws Exception {
        JsonNode json = mapper.readTree(login(email, password).andReturn().getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + password
                        + "\",\"manterConectado\":false}"));
    }

    private long createPortfolio(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/carteiras")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Privada\"}"))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID() + "@example.com";
    }
}
