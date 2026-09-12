## Why

O projeto atualmente não possui um fluxo de autenticação completo, embora a referência `IMAGEM_DE_LOGIN.png` e a especificação `SPEC_Carteira_Investimentos.md` definam uma experiência de login, cadastro e recuperação de senha. É necessário proteger os dados financeiros por usuário e oferecer uma tela de acesso coerente com a identidade visual verde premium demonstrada na imagem.

## What Changes

- Adicionar cadastro de usuários com e-mail normalizado, perfil padrão e senha protegida por BCrypt.
- Adicionar login, logout, consulta de sessão e renovação segura por refresh token em cookie HttpOnly.
- Proteger rotas e endpoints financeiros, isolando carteiras, posições e operações pelo usuário autenticado.
- Adicionar telas Angular standalone de login, cadastro, recuperação e redefinição de senha, com validação, loading, mensagens e redirecionamento.
- Reproduzir visualmente a referência: fundo financeiro desfocado, card translúcido central, tema claro/escuro e controles acessíveis.
- Preservar APIs e funcionalidades existentes, introduzindo migração segura para dados legados.

## Capabilities

### New Capabilities

- `identity/authentication`: cadastro, login, sessão, refresh, logout e recuperação de senha.
- `identity/login-experience`: telas, rotas, guards, interceptor e apresentação visual responsiva da autenticação.

### Modified Capabilities

- `investment-portfolios`: exigir autenticação e filtrar dados pela carteira do usuário proprietário.
- `investment-platform-api`: restringir endpoints financeiros e padronizar respostas de autenticação/autorização.

## Impact

- Backend Spring Boot: entidade e repositório de usuário, DTOs, serviços, controllers, segurança stateless, cookies, exceções e migração de relacionamentos.
- Frontend Angular: `app.routes.ts`, configuração global, feature de autenticação, serviço/interceptor/guards e estilos da tela de login.
- Persistência: nova tabela de usuários e associação segura das carteiras legadas, sem apagar dados.
- Configuração: segredo JWT, expirações, cookie e origem CORS via variáveis de ambiente.
- Testes unitários, integração, segurança, responsividade e validação visual da rota `/login`.
