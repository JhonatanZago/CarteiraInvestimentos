## Context

O backend usa Spring Boot MVC/Data JPA com services, facades, repositories, DTOs e exceptions; o frontend usa Angular standalone e atualmente não possui rotas ou estado de autenticação identificados. A imagem `openspec/specs/IMAGEM_DE_LOGIN.png` define um card central translúcido sobre um cenário financeiro escuro, branding “CARTEIRA”, controles de e-mail/senha, tema e ações de acesso.

## Goals / Non-Goals

**Goals:**

- Implementar identidade, sessão e autorização sem expor credenciais.
- Associar carteiras e operações ao usuário autenticado, incluindo estratégia para registros legados.
- Criar telas Angular reais e responsivas que reproduzam a referência sem alterar o visual aprovado das telas internas.
- Manter respostas de erro estruturadas, CORS restrito e cobertura automatizada.

**Non-Goals:**

- Reescrever o domínio de investimentos ou alterar cálculos financeiros.
- Criar uma camada arquitetural genérica chamada `provider`.
- Enviar e-mail real sem um adapter/configuração de produção comprovada.
- Usar dados fictícios ou autenticação simulada para mascarar integrações ausentes.

## Decisions

1. **Sessão stateless com access token curto e refresh em cookie HttpOnly.** O access token ficará em memória no Angular; o refresh terá rotação, expiração e invalidação no logout. Isso reduz exposição a XSS em comparação com persistência primária em `localStorage`. Alternativas consideradas: sessão HTTP tradicional (incompatível com a fronteira atual) e apenas localStorage (risco maior).

2. **Spring Security com `SecurityFilterChain`, `PasswordEncoder` BCrypt e filtro JWT.** A configuração será explícita, com handlers JSON para 401/403 e endpoints públicos limitados. Não será usada API obsoleta. O segredo e tempos virão de propriedades de ambiente validadas no startup.

3. **Propriedade de carteira como limite de autorização.** Repositórios e services receberão o usuário autenticado do contexto, e consultas/alterações usarão métodos que filtram por proprietário. Registros legados serão associados por migração controlada ou fluxo de vinculação documentado; nunca será aceito `usuarioId` do cliente.

4. **Feature Angular de autenticação centralizada.** `AuthService`, interceptor funcional, guards e modelos ficarão em `core/auth`; páginas standalone ficarão em `features/auth`. O interceptor ignorará login/refresh e fará uma única tentativa de renovação para evitar loops.

5. **Tela reproduzida com CSS e decoração não interativa.** O fundo terá gradientes, formas e gráfico abstrato em HTML/CSS com `pointer-events: none`; nenhum screenshot com dados pessoais será embutido. O alternador de tema reutilizará o mecanismo global existente.

6. **Recuperação de senha com token somente-hash.** O adapter de e-mail terá implementação de desenvolvimento segura e contrato para produção. A resposta pública será sempre genérica para evitar enumeração de e-mails.

## Risks / Trade-offs

- [Dados legados sem proprietário] → executar migração idempotente em modo dev, bloquear acesso ambíguo e registrar itens que exigem decisão administrativa.
- [Cookie Secure em HTTP local] → habilitar `Secure` apenas em produção e documentar a configuração de desenvolvimento sem relaxar a produção.
- [Expiração/rotação concorrente de refresh] → armazenar hash, usar transação e invalidar o token anterior no mesmo fluxo.
- [Ausência de serviço de e-mail] → não expor token em produção; usar apenas adapter de desenvolvimento explicitamente configurado.
- [Diferença visual entre a referência e viewport] → validar 1920×1080, 1366×768, 768×1024 e 390×844 e manter decoração simplificada em telas estreitas.
