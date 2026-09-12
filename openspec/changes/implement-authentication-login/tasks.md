## 1. Diagnóstico e modelo de dados

- [x] 1.1 Inventariar entidades, controllers, rotas, CORS e vínculos de carteira existentes; registrar no design qualquer incompatibilidade antes de codificar.
- [x] 1.2 Criar entidade `Usuario`, enum de perfil, repository e migração idempotente; verificar que o schema existente e registros legados permanecem íntegros.
- [ ] 1.3 Implementar associação segura de carteiras a usuários e estratégia para legados; cobrir acesso cruzado com teste de integração.

## 2. Autenticação backend

- [x] 2.1 Criar DTOs validados de cadastro, login, refresh, usuário autenticado e recuperação; verificar que respostas nunca contêm senha, hash ou refresh token.
- [x] 2.2 Implementar cadastro com e-mail normalizado, unicidade, perfil USER e BCrypt; executar testes de cadastro válido, duplicidade e hash.
- [x] 2.3 Implementar emissão, validação, rotação e invalidação de access/refresh tokens com segredo e expirações configuráveis por ambiente; testar tokens válidos, expirados e reutilizados.
- [x] 2.4 Implementar endpoints `/api/v1/auth/*`, handlers JSON de 401/403 e logout 204; validar contratos e códigos HTTP com MockMvc.
- [x] 2.5 Implementar recuperação/redefinição com token aleatório somente-hash, expiração e adapter de e-mail dev seguro; testar resposta genérica, expiração e uso único.

## 3. Segurança e API existente

- [x] 3.1 Configurar `SecurityFilterChain`, CORS restrito, cookie HttpOnly/SameSite/Path e preflight; verificar que apenas endpoints públicos documentados permanecem anônimos.
- [ ] 3.2 Aplicar filtros de proprietário em carteiras, ações relacionadas, posições, vendas, históricos, proventos e dashboard; testar que IDs de outro usuário não vazam dados.
- [ ] 3.3 Padronizar erros de autenticação, autorização e validação sem expor detalhes sensíveis; executar suíte completa de integração da API.

## 4. Frontend de autenticação

- [x] 4.1 Criar modelos e `AuthService` centralizado com estado reativo, restauração inicial e logout; verificar comportamento após reload e limpeza de sessão.
- [x] 4.2 Implementar interceptor funcional com header de acesso, uma tentativa de refresh e exclusão de login/refresh do ciclo; cobrir 401 sem loop.
- [x] 4.3 Implementar guards para rotas privadas e para impedir login de usuário autenticado, preservando URL de retorno; testar redirecionamentos.
- [x] 4.4 Criar páginas standalone `/login`, `/cadastro`, `/esqueci-minha-senha` e `/redefinir-senha` com Reactive Forms, validações, loading, mensagens e prevenção de duplo envio; executar testes de componente.
- [x] 4.5 Reproduzir a referência visual em HTML/SCSS responsivo: fundo financeiro desfocado, card translúcido, branding, ícones, tema global, contraste e breakpoints 1920×1080, 1366×768, 768×1024 e 390×844; validar visualmente sem rolagem horizontal.
- [x] 4.6 Integrar nome/e-mail/avatar e logout no menu/cabeçalho existente sem alterar o layout aprovado das telas internas; verificar navegação após login e saída.

## 5. Validação final

- [x] 5.1 Executar `./gradlew test` e `./gradlew build`, corrigindo falhas sem desabilitar segurança.
- [x] 5.2 Executar `npm test` e `npm run build` no frontend, corrigindo erros de TypeScript, template e estilos.
- [ ] 5.3 Subir backend e frontend juntos e validar cadastro, login, reload, rota privada, logout, senha incorreta, refresh expirado, recuperação, isolamento entre usuários e temas.
- [ ] 5.4 Capturar a rota `/login` nos quatro viewports e comparar com `IMAGEM_DE_LOGIN.png`; documentar diferenças residuais e configuração final do `.env`.
