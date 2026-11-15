# Auditoria final de encerramento — Modulo 11

Data: 2026-11-08  
Escopo: auditoria tecnica do change OpenSpec `harden-application-security-module-11`.  
Resultado da auditoria: **nao recomendado encerrar o modulo ainda**.

## 1. Resumo executivo

O modulo possui 46 tarefas OpenSpec, 38 concluidas e 8 pendentes. As camadas SAST, SCA, SBOM, SpotBugs/FindSecBugs e DAST foram implementadas e executadas com evidencia preservada. O ZAP final cobriu 13 URLs locais, com 63 PASS, 0 FAIL e 6 entradas WARN residuais; o Security Gate permanece corretamente **REPROVADO por `zap:alerts`**.

O alerta DOMPurify foi corrigido com Swagger UI 5.34.1 / DOMPurify 3.4.17. Tambem foi removido `script-src 'unsafe-inline'` apos confirmar que o Swagger usa somente tres scripts externos. Permanecem `style-src 'unsafe-inline'`, COEP/CORP, timestamps e alertas informativos.

O risco mais importante fora do ZAP continua sendo a credencial BRAPI antiga exposta no historico Git. A revogacao nao foi confirmada e nao foi executada automaticamente.

## 2. Dez tarefas pendentes

| Tarefa | Status atual | Classificacao | Evidencia existente | Evidencia ausente / acao necessaria |
|---|---|---|---|---|
| 1.1 | `[ ]` | A — implementada, mas sem validacao completa | `AuthTokenService` rejeita segredo ausente, curto e placeholder fora de teste; existem testes unitarios | Falta teste de inicializacao de contexto nao-teste para todos os cenarios e verificacao explicita de ausencia de segredo em logs; adicionar testes antes de marcar |
| 1.2 | `[x]` | B — implementada e comprovada | HMAC-SHA256, expiração, comparação constante, rotação, hash persistido e concorrência real validados com MockMvc/H2; 30 execuções concorrentes sem falha | Manter monitoramento de lock transacional em banco de produção; os demais requisitos das tasks 1.1–3.2 continuam fora do escopo |
| 1.3 | `[ ]` | A — implementada, mas sem validacao completa | Cookie usa HttpOnly, Secure configuravel, SameSite, Path e Max-Age; teste seguro verifica headers | Falta validar local/secure, logout/limpeza, max-age e compatibilidade real Angular; adicionar testes de `Set-Cookie` |
| 2.1 | `[ ]` | B — implementada e parcialmente comprovada | CORS usa origens, metodos e headers configurados; testes aprovam origem Angular e rejeitam origem desconhecida | Falta matriz completa de preflight e requisicao real com credenciais em todos os ambientes; confirmar antes de marcar |
| 2.2 | `[ ]` | B — implementada e parcialmente comprovada | CSRF desabilitado para fronteira bearer-only; cookie de refresh sozinho nao muta endpoint financeiro; bearer valido permanece funcional | Falta documentacao/teste completo de todos os endpoints mutaveis e comportamento de refresh/logout; completar evidencia |
| 2.3 | `[ ]` | A — implementada, mas sem validacao completa | H2 console desabilitado em dev/test e permitido somente em local; `web-allow-others=false`; teste local existente | Falta teste de perfil deployment-like/producao e confirmacao de rejeicao do console fora de local |
| 3.1 | `[ ]` | A — implementada, mas sem validacao completa | Servicos usam `CurrentUser` e consultas por `findByIdAndUsuarioId`; teste de isolamento de carteira existe | Falta auditoria/teste abrangente de posicoes, vendas, dashboard, evolucao, rendas e analise cambial; cobrir todos os caminhos |
| 3.2 | `[ ]` | A — implementada, mas sem validacao completa | `SecurityRegressionTest` cobre 401, token invalido/expirado, cookie-only, isolamento, CORS e `/me` | Falta cobertura explicita de 403, vazamento entre usuarios em todos os recursos, entradas invalidas e endpoints financeiros restantes |
| 6.4 | `[ ]` | D — dependente de aprovacao | Historico sanitizado identifica um achado `generic-api-key` associado a BRAPI; worktree atual sem achados | Responsavel deve confirmar validade, revogar/rotacionar e autorizar eventual tratamento historico; manter aberto |
| 7.5 | `[ ]` | E — dependente de fator externo/condicao futura | Excecao CSRF e limitacao do detector CORS estao documentadas; estado atual e bearer-only | Reavaliar se cookie autorizar mutacoes ou quando surgir FindSecBugs compativel; nao ha acao segura adicional agora |

O checkbox da task 1.3 foi alterado somente após a execução dos testes descritos no Command 23. Os demais checkboxes permanecem inalterados. A classificação acima não substitui a evidência exigida pelo workflow OpenSpec.

## 3. Auditoria dos controles

- **JWT:** HMAC-SHA256, TTL, expiracao, comparacao constante e segredo externo fora de teste. A politica de segredo esta implementada, mas requer testes de contexto adicionais.
- **Autorizacao/isolation:** filtros criam autenticacao a partir do bearer e servicos usam o usuario atual em consultas de carteira. A cobertura transversal ainda nao e completa.
- **Validacao e erros:** DTOs usam Bean Validation e o handler retorna erros JSON sem stack trace; a regressao existente passou.
- **CORS:** origens exatas configuraveis, metodos/headers explicitos e credenciais habilitadas. Nao ha wildcard no valor padrao.
- **CSRF/cookies:** CSRF esta desabilitado para a fronteira bearer-only; refresh token e HttpOnly, com SameSite/Path/Max-Age. O teste cookie-only passou.
- **H2:** console desabilitado em dev/test e local-only por configuracao; falta prova deployment-like completa.
- **Headers:** CSP, COOP, COEP, CORP, Permissions-Policy, frame options e cache-control foram observados no backend local sem duplicidade.
- **Dados financeiros:** testes e servicos preservam isolamento, compras, vendas e calculos; a cobertura de autorizacao ainda precisa ser ampliada.
- **Segredos:** `BRAPI_TOKEN` e referenciado por variavel externa; nenhum valor foi impresso. O historico continua comprometido ate revogacao confirmada.

## 4. Security Gate

O script `scripts/security-gate.ps1` executa:

1. Semgrep com falha em findings/execucao.
2. Gitleaks nos escopos atuais, registrando cada falha.
3. CycloneDX backend/frontend.
4. OSV, distinguindo vulnerabilidade de erro de execucao.
5. SpotBugs + FindSecBugs.
6. ZAP somente com `-RunZap`.

O gate preserva os relatorios, falha quando scanners retornam erro ou achados bloqueantes e trata erro de execucao ZAP como `zap:execution`. Nao existe bypass generico. A analise historica Gitleaks e mantida separada do gate atual; portanto, Gitleaks atual 0 nao resolve o historico BRAPI.

## 5. Alertas ZAP residuais

| Alerta | Tratamento de auditoria |
|---|---|
| `10055 style-src unsafe-inline`, Medium | Correcao recomendada: avaliar nonce/hash ou politica especifica Swagger; nao aprovado automaticamente |
| `90004` COEP `unsafe-none`, Low | Aceitacao proposta: valor deliberado para compatibilidade; reavaliar se isolamento cross-origin for requisito |
| `90004` CORP `same-site`, Low | Aceitacao proposta: testar `same-origin` por rota antes de alterar; evitar mudanca apenas para satisfazer ZAP |
| `10096` Timestamp Disclosure, Low | Informativo de baixo impacto em bundle estatico; confirmar ausencia de metadado da aplicacao |
| `10109` Modern Web Application, Informational | Sem acao necessaria |
| `10049` Non-Storable Content, Informational | `no-store` e controle conservador; nao relaxar respostas autenticadas/financeiras |

Os seis itens permanecem `PROPOSTO` em [zap-risk-acceptance.md](zap-risk-acceptance.md). Nenhum risco foi aprovado.

## 6. Segredo BRAPI — procedimento pendente

1. Responsavel revoga o token antigo diretamente no provedor BRAPI.
2. Responsavel gera uma nova credencial.
3. Nova credencial e configurada somente por ambiente/secret manager local, nunca em arquivo rastreado.
4. Integração e validada com ambiente autorizado e testes/mocks sem imprimir o valor.
5. Gitleaks atual e verificacao de logs sao executados sem exibir o segredo.
6. Tratamento do historico remoto e avaliado separadamente, com autorizacao explicita; nenhuma reescrita foi executada.

O risco permanece **ABERTO** ate confirmação de revogacao.

## 7. Evidencias de regressao

- `./gradlew.bat clean test --console=plain`: aprovado.
- `./gradlew.bat build --console=plain`: aprovado, incluindo SpotBugs.
- `./gradlew.bat spotbugsMain`: aprovado, 0 achados.
- `npm run build`: aprovado, com warnings de budget/`DecimalPipe` ja existentes.
- `npm test -- --watch=false`: aprovado; 15 arquivos e 63 testes.
- OpenSpec strict: aprovado.
- Security Gate com `-RunZap`: reprovado por `zap:alerts`.

## Atualizacao de cobertura — Command 20

Foram adicionados `SecurityCoverageTest`, `H2ConsoleDeploymentSecurityTest` e `frontend/src/app/core/auth/auth.service.spec.ts`.

Nova evidencia:

- Backend: 107 testes aprovados.
- Frontend: 16 arquivos e 65 testes aprovados.
- JWT: assinatura adulterada, ausencia de eco da credencial e escopo do SecurityContext cobertos.
- Refresh: hash persistido, rotacao, rejeicao de reuso e logout/revogacao cobertos.
- Cookies: `withCredentials`, logout e ausencia de refresh token em local/sessionStorage cobertos; Secure/SameSite/Path ja eram verificados no backend.
- CORS: origem, metodo e header nao permitidos cobertos.
- H2: perfil dev com console desabilitado coberto, alem do perfil local existente.
- Isolamento: GET/PUT/DELETE de carteira e GET/POST de posicao entre dois usuarios cobertos.

As tarefas 1.1 a 3.2 permanecem sem checkbox alterado porque ainda faltam: concorrencia na rotacao, matriz HTTPS/local completa, teste cross-site de todos os endpoints mutaveis, cobertura de vendas/proventos/indicadores/corretoras e um endpoint real com 403. A evidencia melhorou, mas nao satisfaz integralmente os criterios originais.

## 8. Critérios para encerramento

O encerramento tecnico exige, no minimo:

- completar ou obter aprovacao formal para as 10 tarefas pendentes;
- concluir a matriz de testes de autenticacao, cookies, CORS, CSRF e isolamento;
- confirmar o estado deployment-like do H2;
- decidir formalmente os riscos ZAP residuais, sem aprovacao automatica;
- revogar/rotacionar a credencial BRAPI e registrar a decisao sobre o historico;
- repetir o gate com resultado compatível com a politica aprovada;
- executar nova validacao OpenSpec strict sem marcar tarefas por inferencia.

## 9. Recomendacao final

**Nao encerrar o Modulo 11 neste momento.** A task 1.2 foi comprovada neste comando, mas o gate permanece reprovado, ainda existem tarefas pendentes e a credencial historica BRAPI nao possui revogacao confirmada. A proxima decisao deve ser do responsavel do projeto sobre as tarefas restantes, a credencial comprometida e os riscos ZAP propostos.

## Atualizacao de concorrencia de refresh — Command 22 — 2026-11-08

A verificacao do repositorio nao encontrou commit, arquivo, marcador ou diff atribuivel ao Command 21. A documentacao anterior registrava explicitamente a concorrencia de refresh como lacuna; portanto, o relatorio anterior nao foi tratado como evidencia de execucao do Command 21.

Foi corrigida a condicao de corrida em `AuthService.refresh`: a busca do refresh token agora usa `findByTokenHashForUpdate`, com `PESSIMISTIC_WRITE`, dentro da transacao existente. A segunda transacao aguarda a primeira, observa `usado=true` e rejeita o token anterior. Nao houve migration, alteracao de contrato REST ou modificacao de regra financeira.

O teste `RefreshTokenConcurrencyTest` usa duas requisicoes reais via MockMvc, H2, `ExecutorService` e `CountDownLatch`. Cada invocacao JUnit repete o cenario 10 vezes; foram feitas 3 invocacoes externas, totalizando 30 cenarios: 30 sucessos unicos, 30 rejeicoes unicas, um unico sucessor por cenario e reuso do token anterior rejeitado. Nao foi usado `Thread.sleep` como sincronizacao.

Resultados: `clean test` aprovado; 117 testes backend, 0 falhas e 1 ignorado; `build` aprovado com SpotBugs/FindSecBugs sem achados bloqueantes. O OpenSpec strict foi validado apos marcar somente a task 1.2. O gate/ZAP nao foi reexecutado, pois este comando foi deliberadamente restrito a concorrencia de refresh.
## Testes de cookies e sessão — Command 23 — 2026-11-08

A task 1.3 foi validada sem alteração de produção: `AuthProperties` controla `Secure`, `SameSite` e `Path`, e `AuthController` emite/limpa o cookie `refresh_token` com `HttpOnly`, `Max-Age` e atributos compatíveis. O refresh token não aparece no JSON e não é armazenado em `localStorage` ou `sessionStorage` pelo Angular.

Matriz comprovada:

| Ambiente | Secure | SameSite | Path | Cobertura |
|---|---|---|---|---|
| Local HTTP explícito | ausente | Lax | `/api/v1/auth` | login, refresh, logout, rotação, limpeza, ausente, adulterado e expirado |
| Deployment-like/HTTPS | presente | Strict | `/api/v1/auth` | login, logout, `Max-Age`, JSON sem refresh token e `X-Forwarded-Proto=https` |

Foram adicionados três testes backend e dois testes Angular. O backend executou 120 testes, 0 falhas e 1 ignorado; o frontend executou 16 arquivos e 67 testes, todos aprovados. O teste ignorado é `PostgreSqlSmokeTest`, condicionado por `@EnabledIfSystemProperty(smoke.db.url)`, porque não havia banco PostgreSQL de smoke configurado; ele não foi removido nem alterado.

SameSite/CORS não foram tratados como substitutos de CSRF. A análise de cross-site e endpoints autorizados por cookie permanece no escopo da task 2.2. O Security Gate e os riscos ZAP não foram alterados ou aprovados automaticamente.

## Validação completa de CORS — Command 24 — 2026-11-08

A task 2.1 foi comprovada sem alteração da configuração de produção. `CorsConfig` mantém origens exatas, métodos `GET,POST,PUT,DELETE,OPTIONS`, headers `Authorization,Content-Type,Accept,X-Requested-With`, `allowCredentials=true`, `maxAge=3600` e nenhum `exposedHeaders` ou wildcard.

| Cenário | Origem/método/headers | Esperado | Observado |
|---|---|---|---|
| Preflight permitido | `http://localhost:4200`, POST, Authorization/Content-Type | 200, origem exata, métodos/headers explícitos, credentials=true, Vary | Aprovado |
| Segundo frontend permitido | `http://127.0.0.1:4200`, GET, Accept | 200 e concessão exata | Aprovado |
| Preflight negado | origem externa | Sem concessão CORS | Aprovado, 403 |
| Método não permitido | PATCH | Sem concessão CORS | Aprovado, 403 |
| Header não permitido | X-Not-Allowed | Sem concessão CORS | Aprovado, 403 |
| Origin ausente | sem Origin | Sem headers CORS; autenticação normal | Aprovado, 401 |
| Origem com porta/protocolo diferentes | `localhost:4201`, HTTPS | Sem concessão CORS | Aprovado |
| `Origin: null` ou `*` | null/wildcard | Sem reflexão ou wildcard | Aprovado |
| Requisição permitida sem credencial | GET sem Bearer | CORS concedido, autenticação negada | Aprovado, 401 |
| Requisição permitida com Bearer | GET com Authorization | CORS concedido e endpoint protegido acessível | Aprovado, 200 |
| Rotas reais | GET, POST, PUT, DELETE | CORS correto sem bypass de auth | Aprovado; sem Bearer, 401 |

O teste `CorsComprehensiveSecurityTest` executou 9 cenários. O Angular usa API relativa `/api/v1`, `withCredentials` e `Authorization` Bearer pelo interceptor, mantendo compatibilidade com a política sem ampliar origens.

Resultados: backend com 129 testes, 0 falhas e 1 ignorado; build e `spotbugsMain --rerun-tasks` aprovados. Frontend com 16 arquivos e 67 testes aprovados; build aprovado. OpenSpec strict aprovado. A task 2.2 continua responsável pela análise CSRF/cross-site; o Security Gate e os riscos ZAP não foram alterados.
\n+## Auditoria CSRF e fronteira Bearer/cookie — Command 25 — 2026-11-08
\n+A task 2.2 foi validada exclusivamente com `CsrfBoundarySecurityTest` (5 testes MockMvc, banco H2 real). Cookie de refresh isolado não lê nem altera carteiras; sem Origin o resultado é 401, e com origem não confiável ou `Origin: null` o filtro CORS rejeita com 403. Bearer válido continua funcional em origem permitida.
\n+Resultados reais: `clean test` e `build` aprovados com 134 testes backend, 0 falhas, 0 erros e 1 ignorado (`PostgreSqlSmokeTest`, sem `smoke.db.url`); `spotbugsMain --rerun-tasks` aprovado sem achados. Frontend: build aprovado e 16 arquivos/67 testes aprovados. OpenSpec strict foi validado e a contagem passou para 40/46. Security Gate, ZAP e riscos BRAPI não foram alterados.
## CSRF boundary audit - Command 25 - 2026-11-08

Task 2.2 was validated with CsrfBoundarySecurityTest: 5 real MockMvc/H2 tests. Refresh-cookie alone cannot read or mutate financial endpoints. Without Origin the protected request is 401; with an untrusted or null Origin the CORS filter returns 403 without Access-Control-Allow-Origin. A valid Bearer remains functional from an allowed origin.

Observed matrix: financial GET/POST/PUT/DELETE with cookie only rejected; refresh/logout succeed only from the configured origin; invalid form/plain login content returns 415 without Set-Cookie; trusted preflight returns explicit origin, credentials, methods and headers.

Results: backend clean test/build/spotbugsMain rerun passed with 134 tests, 0 failures, 0 errors and 1 skipped PostgreSqlSmokeTest due to missing smoke.db.url. Frontend build passed and 16 files/67 tests passed. OpenSpec strict passed at 40/46. Security Gate, ZAP and BRAPI risk were not changed.
## Recuperacao Gradle e validacao JWT - Command 28 - 2026-11-08

Diagnostico: `java -version` confirmou Java 21.0.12.1; o `java` do PATH aponta para `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`, enquanto `JAVA_HOME` persistia apontando para JDK 17. O Wrapper e Gradle 9.5.1. Nao existe `gradle.properties` no repositorio; `settings.gradle` usa somente o nome do projeto e `build.gradle` usa `mavenCentral()`.

O cache global `C:\.gradle` falha por permissao. O perfil de usuario sugerido tambem nao pode ser criado nesta sessao. Foi usado cache dentro do workspace, com `JAVA_HOME` corrigido apenas na sessao. Os daemons Gradle dos tres caches do projeto foram parados; nenhum daemon do projeto permaneceu ativo. O artefato `byte-buddy-agent-1.17.8.jar` foi removido apenas do caminho isolado apos a parada dos daemons e restaurado de uma copia local com hash SHA-256 identico.

A rede continuou bloqueada mesmo com tentativa escalada, com `Permission denied: getsockopt` para Maven Central. O cache manteve estado de resolucao negativa para Byte Buddy e nao reconheceu a copia offline como versao cacheada. Nao houve alteracao de dependencia, TLS, repositorio ou versao do projeto. Um init script temporario foi testado e removido.

Resultado: os nove novos testes JWT nao executaram; a compilacao parou em `compileTestJava` por `byte-buddy-agent-1.17.8.jar` sem resolucao cacheada. `clean test`, `build` e `spotbugsMain --rerun-tasks` nao foram declarados nesta etapa. A task 1.1 permanece pendente e a contagem OpenSpec permanece 41/46.

## Validacao final de JWT e SecurityContext - Command 27 - 2026-11-08

Esta execucao foi restrita a task 1.1. A implementacao usa o formato proprio `payload.assinatura`, com HMAC-SHA256, TTL validado, comparacao constante e segredo externo obrigatorio fora de `test`. Como nao ha header JWT/claim de algoritmo nesse formato, um token padrao com `alg=none` e rejeitado como formato incompativel.

`AuthTokenService` rejeita segredo ausente, menor que 32 caracteres e placeholders fora de `test`. `AccessTokenFilter` autentica somente `Authorization: Bearer`, consulta usuario ativo e limpa o contexto quando o usuario nao existe. O dominio suporta `Usuario.ativo`; nao ha logger no fluxo JWT/login/refresh/logout/filtro.

| Requisito | Evidencia | Estado |
|---|---|---|
| Segredo ausente/curto/placeholder | `AuthTokenServiceSecurityTest` existente e novos casos unitarios | Implementado; execucao nova pendente |
| JWT valido/expirado/adulterado/malformado | testes existentes e novo `JwtContextSecurityTest` | Implementado; execucao nova pendente |
| Algoritmo nao permitido/claims invalidas | casos `alg=none` e claims incompativeis rejeitados | Implementado; execucao nova pendente |
| Usuario inexistente/desativado | novos testes via `/auth/me` | Implementado; execucao nova pendente |
| Contexto sequencial e paralelo | teste existente e novo teste com `CyclicBarrier`/dois usuarios | Implementado; execucao nova pendente |
| Refresh cookie nao autentica Bearer-only | novo caso e cobertura existente | Implementado; execucao nova pendente |
| Segredos em respostas/logs | respostas nao ecoam credenciais; fluxo sem logs de credenciais | Auditoria de logs; captura runtime pendente |

Foram implementados 9 novos casos de teste em `JwtContextSecurityTest` e `AuthTokenServiceSecurityTest`. Nenhum teste foi removido ou desabilitado.

A execucao Gradle nao chegou aos testes: houve falha de permissao no cache global `C:\.gradle`, falha de rede/TLS ao resolver `org.webjars:swagger-ui:5.34.1` em um cache local e `AccessDeniedException` para `byte-buddy-agent-1.17.8.jar` em outra tentativa. Nao ha resultado novo para `clean test`, `build` ou `spotbugsMain`. O resultado anterior conhecido permanece: backend 134 aprovados/1 ignorado e frontend 67 aprovados. OpenSpec strict executado agora retornou valido em 41/46; a task 1.1 nao foi marcada.

Nenhuma regra financeira, migration, Security Gate, ZAP ou credencial foi alterada. O token BRAPI historico continua aberto, sem revogacao confirmada.

## H2 console and profile isolation audit - Command 26 - 2026-11-08

Task 2.3 was validated exclusively. The profile matrix is:

| Profile | H2 console | Access policy | Datasource/test evidence |
|---|---|---|---|
| `local` | Enabled only by `application-local.properties` | `/h2-console/**` permitted for local use; `web-allow-others=false` | Real embedded HTTP server returned 2xx for `/h2-console/`; random in-memory H2 |
| `dev` deployment-like | Disabled explicitly | `/h2-console/**` denied by SecurityConfig, including authenticated requests | Property was false; H2 was isolated to the test context |
| `test` | Disabled | Test-only permissive behavior remains confined to `test` | H2 `create-drop`, no external database |
| Other/production-like | No console enablement in base configuration | Non-local path is denied | Production datasource/secret provisioning remains external and was not connected |

The correction removed the `H2_CONSOLE_ENABLED` override from `application-dev.properties`, preventing a deployment-like `dev` run from enabling the console through that development switch. `SecurityConfig` now uses `denyAll()` for `/h2-console/**` outside `local`; this is defense in depth if an external property is misconfigured. No migration, financial rule, integration or Security Gate change was made.

`H2ConsoleSecurityTest` starts an embedded HTTP server and verifies the effective local property plus a real 2xx response from `/h2-console/`. `H2ConsoleDeploymentSecurityTest` verifies the active non-local profile, the effective disabled property and non-public access. The deployment test does not treat 401/403 alone as proof; the property and profile conditions are the primary evidence. MockMvc cannot expose the H2 servlet mapping in its simulated servlet context, so the local functional assertion uses the embedded HTTP port.

Results: backend `clean test` passed with 134 tests, 0 failures, 0 errors and 1 skipped `PostgreSqlSmokeTest` because `smoke.db.url` was absent. `build` and `spotbugsMain --rerun-tasks` passed with no SpotBugs/FindSecBugs findings. Frontend build passed and 16 files/67 tests passed. OpenSpec strict passed; progress is now 41/46. The Security Gate and ZAP were not changed or approved. The historical BRAPI token remains an open risk without confirmed revocation.

## Validação final da task 1.1 — Command 35 — 2026-11-11

A causa das duas falhas JWT foi corrigida exclusivamente: `AuthTokenService` agora rejeita também valores que contenham o prefixo `dev-only-change-me`, incluindo `dev-only-change-me-please-123456789`; a expectativa parametrizada foi corrigida para UTF-8 (`não pode usar um valor padrão ou de exemplo`). A política de segredo vazio, mínimo de 32 caracteres, placeholders conhecidos, geração aleatória somente em `test` e ausência de logs de credenciais foi preservada.

Com Java 21.0.12.1, Gradle 9.5.1 e `GRADLE_USER_HOME=.gradle-user-home-dast-validation`, os testes focalizados `AuthTokenServiceSecurityTest` e `JwtContextSecurityTest` passaram com 13/13 testes. A regressão backend `clean test`, `build` e `spotbugsMain --rerun-tasks` passou; foram observados 143 testes backend, 0 falhas, 0 erros e 1 ignorado (`PostgreSqlSmokeTest`, sem `smoke.db.url`). O frontend passou em `npm run build` e `npm test -- --watch=false`, com 16 arquivos e 67 testes aprovados; somente warnings de budget/DecimalPipe permaneceram.

OpenSpec strict foi validado após a conclusão da task 1.1. Nenhuma migration, regra financeira, dependência, Security Gate ou risco ZAP foi alterado; o risco histórico BRAPI permanece aberto.

## Isolamento dos recursos financeiros — Command 36 — 2026-11-12

A task 3.1 foi validada com inventário dos caminhos financeiros e teste de integração real. A identidade do proprietário vem de `CurrentUser.id()`, derivada do principal instalado pelo JWT validado; nenhum `userId` fornecido pelo cliente é usado como autoridade.

| Endpoint/caminho | Método | Recurso | Escopo/autorização | Risco observado |
|---|---|---|---|---|
| `/api/v1/carteiras` | GET/POST | Carteiras | listagem por `usuarioId`; criação usa usuário autenticado | Nenhum após revisão |
| `/api/v1/carteiras/{id}` | GET/PUT/DELETE | Carteira | `findByIdAndUsuarioId`; exclusão valida proprietário antes de verificar ativos | Exclusão podia revelar estado de carteira alheia; corrigido |
| `/api/v1/carteiras/{id}/posicoes` e `/paginadas` | GET/POST/PUT/DELETE | Posições | carteira do usuário + posição vinculada à carteira | Mutação/exclusão não confirmavam dono antes de carregar posição; corrigido |
| `/vendas`, `/vendas/simulacao` e rotas aninhadas | GET/POST | Vendas e histórico | carteira/posição pertencem ao usuário autenticado | Nenhum identificado |
| `/dashboard/carteiras/{id}`, `/evolucao`, `/proventos`, `/analise-moedas` | GET | Agregações privadas | validação da carteira antes das agregações | Nenhum identificado |
| `/api/v1/acoes`, histórico de mercado e `/api/v1/corretoras` | GET | Catálogo/mercado público | escopo global intencional; dados privados continuam limitados pela carteira | Não são dados privados |

`FinancialOwnershipSecurityTest` usou MockMvc, H2, JWTs reais de dois usuários e recursos persistidos distintos. Foram comprovados acesso próprio, isolamento de listagens, consultas por ID, dashboard, evolução, proventos, análise cambial, simulação, criação, alteração e exclusão; tentativas cruzadas retornaram 404 sem vazamento. A venda própria foi confirmada com sucesso e a posição do segundo usuário permaneceu íntegra. Não houve alteração de fórmulas, preço médio, compra, venda, câmbio, migrations ou integrações.

Resultado: backend `clean test` passou com 144 testes, 0 falhas, 0 erros e 1 ignorado (`PostgreSqlSmokeTest`, sem `smoke.db.url`); `build` e `spotbugsMain --rerun-tasks` passaram. Frontend `npm run build` passou com warnings não bloqueantes de budget/`DecimalPipe`, e `npm test -- --watch=false` passou com 16 arquivos e 67 testes. OpenSpec strict passou após marcar somente a task 3.1; progresso: 43/46. Permanecem as tasks 3.2, 6.4 e 7.5, além do risco histórico BRAPI e alertas ZAP sem aprovação.

## Testes negativos e prevenção de vazamento — Command 37 — 2026-11-12

A task 3.2 foi concluída exclusivamente com `NegativeAuthorizationSecurityTest`, usando MockMvc, H2 isolado e JWTs reais. A matriz negativa executada foi:

| Cenário | Resposta comprovada | Evidência de não vazamento |
|---|---:|---|
| Sem autenticação em listagem, consulta e criação protegidas | 401 | Sem stack trace, classes Java ou dados financeiros |
| JWT inválido e expirado | 401 | Credencial não aparece na resposta |
| Login válido e inválido | 200 / 400 | Access token somente no sucesso; senha, refresh token e e-mail não aparecem no erro |
| Origem CORS não autorizada | 403 | Sem concessão de credenciais ou origem atacante |
| Usuário B usando IDs de carteira/posição de A | 404 | Nome privado, posição e valores não aparecem |
| IDs inexistentes | 404 | Mesma semântica de recurso não encontrado, sem confirmação de existência |
| Alteração/exclusão/criação e venda cruzadas | 404 | Nenhuma mutação persistida e nenhum detalhe interno |

Também foram cobertos dashboard, evolução, proventos, análise cambial, paginação, posições, vendas e referências de ativo/corretora dentro de operações. Ativos e corretoras permanecem catálogos globais por contrato; o escopo privado é aplicado pela carteira proprietária. A autorização continua no backend, antes das consultas e mutações, sem depender do Angular.

Durante a primeira regressão foi identificado apenas um problema de infraestrutura de testes: o banco H2 compartilhado era encerrado por contexto de teste enquanto `PaginationEndpointTest` executava. O erro completo foi `Table "ATIVO_CARTEIRA" not found (this database is empty)`. A correção mínima foi isolar os bancos H2 das suítes de ownership com URLs nomeadas; nenhuma lógica de produção foi alterada.

Regressão final: backend `clean test` passou com 148 testes, 0 falhas, 0 erros e 1 ignorado (`PostgreSqlSmokeTest`, sem `smoke.db.url`); `build` e `spotbugsMain --rerun-tasks` passaram. Frontend `npm run build` passou com warnings não bloqueantes de budget/`DecimalPipe`, e `npm test -- --watch=false` passou com 16 arquivos e 67 testes. OpenSpec strict foi validado após marcar somente a task 3.2; progresso: 44/46. Permanecem apenas as tasks 6.4 e 7.5, o risco histórico BRAPI e alertas ZAP sem aprovação.

## Auditoria e preparação da rotação BRAPI — Command 38 — 2026-11-12

A integração lê `BRAPI_TOKEN` por configuração externa em `integrations.brapi.token=${BRAPI_TOKEN:}`. O valor atual da variável de ambiente está ausente nesta sessão; não existe fallback literal no `application.properties`. `.env.example` deixa a variável vazia, `docker-compose.yml` referencia a variável externa e `env.exemple` contém somente um marcador textual de documentação, não uma credencial real. Nenhum token foi impresso ou registrado.

Os adapters BRAPI mantêm a credencial apenas em memória, enviam-na no header de autenticação quando configurada e não a incluem em mensagens de erro ou logs. Os testes usam apenas valores fictícios. Foram confirmados: configuração sem token como indisponível, token fictício válido no header, resposta 401 do provedor normalizada sem ecoar o token e falhas de integração sem exposição da credencial.

O hash exato `b8a7c34c41c9336e8365b3203751f162b0ecd96` não está presente no banco de objetos desta cópia local, portanto não foi possível resolver esse identificador diretamente. A auditoria confirmou, sem exibir conteúdo sensível, um commit local relacionado (`ab8a7c34...`) cujo `.gitignore` histórico contém uma atribuição não vazia de `BRAPI_TOKEN`; a exposição histórica permanece tratada como confirmada pelos artefatos sanitizados existentes. O histórico não foi alterado.

Procedimento pendente: revogar a credencial antiga diretamente no provedor BRAPI; gerar uma nova credencial pelo fluxo oficial; configurar somente `BRAPI_TOKEN` no ambiente/secret manager; reiniciar a aplicação; validar uma chamada com dados fictícios ou ambiente autorizado; e confirmar ausência em logs. Nenhuma ação externa foi executada. A task 6.4 permanece pendente até confirmação manual da revogação e da configuração do substituto.

Após o reforço do teste sanitizado `BrapiCotacaoAdapterTest`, a regressão backend passou com 149 testes, 0 falhas, 0 erros e 1 ignorado; `build` e `spotbugsMain --rerun-tasks` passaram. O frontend permanece validado em 67/67 testes. OpenSpec continua em 44/46; somente as tasks 6.4 e 7.5 permanecem pendentes.

## Validação da rotação BRAPI — Command 39 — 2026-11-12

`BRAPI_TOKEN` está ausente no ambiente de execução desta sessão. O código usa somente referência externa, sem credencial literal efetiva; o `.gitignore` protege arquivos locais sensíveis. A consulta real autenticada ao provedor não foi executada porque não há nova credencial disponível e não foi apresentada evidência externa de rotação.

Os testes locais não reutilizaram o token antigo. A regressão backend passou com 149 testes, 0 falhas, 0 erros e 1 ignorado; `build` e `spotbugsMain --rerun-tasks` também passaram. A validação com token fictício e resposta simulada 401 permanece aprovada. Nenhuma ação foi realizada na conta BRAPI.

A task 6.4 permanece pendente. Falta evidência não sensível emitida pelo provedor confirmando revogação do token antigo e confirmação de configuração de um substituto no ambiente de execução. OpenSpec permanece em 44/46 e a task 7.5 não foi iniciada.

## Revisão da exceção CSRF e FindSecBugs — Command 40 — 2026-11-12

A task 7.5 foi concluída exclusivamente por revisão técnica e documental. O modelo atual continua bearer-only para recursos financeiros: `SecurityConfig` desabilita CSRF porque as mutações exigem `Authorization: Bearer`; o refresh cookie HttpOnly é aceito somente por refresh/logout e não autoriza leitura ou alteração financeira. SameSite, CORS exato e os testes existentes são controles complementares, não substitutos universais de CSRF.

Foram considerados Origin ausente, Origin nulo/não autorizado, Referer ausente, requisições cross-site e métodos mutáveis. A matriz existente (`CsrfBoundarySecurityTest` e `CorsComprehensiveSecurityTest`) confirma 401 sem autenticação, 403 para origens não autorizadas, ausência de concessão CORS e rejeição de cookie-only em endpoints financeiros. Se qualquer endpoint futuro aceitar cookie como credencial de negócio, a exceção deverá ser removida e CSRF explícito, com token sincronizador ou mecanismo equivalente, deverá ser implementado antes da autorização.

FindSecBugs 1.14.0 permanece configurado com SpotBugs 4.10.5. A execução real anterior carregou o plugin e terminou sem findings emitidos; `SPRING_CSRF_PROTECTION_DISABLED` permanece uma exceção arquitetural estreita e documentada. `CorsRegistryCORSDetector` continua omitido somente porque a versão 1.14.0 apresentou exceção interna reproduzida ao analisar `CorsConfig`; não foi encontrada nem validada no projeto uma versão compatível para substituir o workaround. Não houve duplicação de dependências, alteração de versões, Security Gate ou aprovação de riscos ZAP.

Task 7.5 marcada como concluída. A task 6.4 permanece pendente por falta de evidência de revogação BRAPI. OpenSpec passa a 45/46; somente 6.4 permanece pendente.

## Verificação final da task 6.4 — Command 41 — 2026-11-12

`application.properties` continua usando exclusivamente `BRAPI_TOKEN` como referência externa. Foi identificado um arquivo `.env` local, ignorado pelo Git, com valor não vazio e data de alteração registrada em 2026-11-05; o valor não foi lido, impresso ou testado. Não há backend em execução nesta sessão para comprovar qual ambiente efetivo carregaria esse valor.

Essa presença comprova configuração local disponível, mas não comprova que o valor é o substituto novo nem que a credencial histórica foi revogada. Os relatórios existentes comprovam apenas a integração mockada e a ausência de vazamento; não há confirmação não sensível emitida pelo provedor BRAPI. A task 6.4 permanece pendente exclusivamente por essa evidência de revogação/identidade do substituto.
