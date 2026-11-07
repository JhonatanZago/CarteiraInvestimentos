# Auditoria técnica — Módulos 11, 12 e 13

Data da auditoria: 2026-11-03  
Escopo: inspeção estática do repositório local e comparação com os quatro materiais indicados pelo professor Jefferson Arpasserini.  
Estado: diagnóstico e planejamento; nenhuma implementação dos módulos foi iniciada.

## 1. Resumo executivo

O projeto é uma aplicação web monolítica com backend Java 21/Spring Boot executado por Gradle e frontend Angular/TypeScript executado por npm. A base funcional é ampla e deve ser preservada: autenticação, carteiras, posições, compras, vendas, dashboard, corretoras, ativos, integrações externas, DTOs, mappers, validações, tratamento centralizado de exceções e testes existentes.

Há implementação parcial de segurança de aplicação, mas não dos controles de engenharia e operação exigidos pelos Módulos 11 e pelo PR Security Validation. O projeto possui Spring Security, BCrypt, access token assinado, refresh token persistido por hash e verificações de propriedade em serviços; porém não possui SAST/SCA/DAST/bytecode gate, varredura de segredos, baseline SARIF, preflight local fail-closed, workflows de auditoria ou revisão automatizada segura.

Os Módulos 12 e 13 estão essencialmente ausentes no repositório atual. Não há GitHub Actions, executor de agente somente leitura, prompts versionados, lint de workflows, Actuator, Prometheus, tracing OpenTelemetry, logs estruturados, SLO, teste k6, backend local de observabilidade ou loop deduplicado de observabilidade.

Conclusão: o projeto está apto a receber as fases propostas, mas a implementação deve começar por fechar riscos de segurança existentes e estabelecer gates reproduzíveis. Gradle e npm devem ser mantidos; os exemplos Maven/Yarn do material precisam ser adaptados somente durante a implementação autorizada.

## 2. Materiais de referência consultados

Os quatro documentos solicitados foram acessados integralmente a partir do repositório do professor:

- [`tutorial/modulo-11-seguranca.md`](https://github.com/jeffersonarpasserini/desenvolvimento-assistido-IA/blob/main/tutorial/modulo-11-seguranca.md)
- [`tutorial/PR-SECURITY-VALIDATION.md`](https://github.com/jeffersonarpasserini/desenvolvimento-assistido-IA/blob/main/tutorial/PR-SECURITY-VALIDATION.md)
- [`tutorial/modulo-12-agentes-ci-cd.md`](https://github.com/jeffersonarpasserini/desenvolvimento-assistido-IA/blob/main/tutorial/modulo-12-agentes-ci-cd.md)
- [`tutorial/modulo-13-desempenho-observabilidade.md`](https://github.com/jeffersonarpasserini/desenvolvimento-assistido-IA/blob/main/tutorial/modulo-13-desempenho-observabilidade.md)

Os materiais orientam, em síntese:

- Módulo 11: threat modeling/STRIDE, SAST, segredos, bytecode, SBOM/SCA, DAST, portão único e triagem humana de achados.
- PR Security Validation: preflight local fail-closed contra merge-base, commits e worktree publicável; higiene de `.env`; Gitleaks; build/teste; SCA; CodeQL Java e JS/TS; SARIF/baseline; revisão humana e evidência no PR.
- Módulo 12: agente “sem caneta”, sem token GitHub e somente leitura; entrada externa tratada como dado em arquivo; jobs separados para publicar; permissões mínimas; actions fixadas por SHA; `pull_request`, não `pull_request_target`; zizmor/actionlint.
- Módulo 13: SLO por rota, sinais RED, logs estruturados sem PII/segredos, métricas Prometheus, Actuator mínimo, tracing OTLP com spans JDBC, teste de fumaça da chegada da telemetria, carga realista com p95, medição antes/depois e loop com estado/deduplicação.

## 3. Baseline do repositório

### 3.1 Estado Git observado

O branch auditado é `main`, alinhado a `origin/main` no momento da inspeção. Já existiam alterações locais não relacionadas a esta auditoria:

- modificados: `frontend/src/app/app.routes.ts`, `frontend-run.log`, `frontend-run-error.log`;
- não rastreado: `frontend-check.png`.

Essas alterações foram preservadas. Nenhum commit, push, reset ou alteração de código existente foi feito.

### 3.2 Estrutura e stack

- Backend: `src/main/java/com/example/carteirainvestimento`, 137 arquivos Java.
- Testes backend: `src/test/java`, 35 arquivos Java.
- Frontend: `frontend/src`, 82 arquivos; Angular/TypeScript/HTML/SCSS.
- Build backend: `build.gradle`, Gradle Wrapper e Java toolchain 21.
- Frontend: `frontend/package.json` e `frontend/package-lock.json`; `packageManager` declara `npm@12.0.2`.
- Spring Boot declarado: `4.0.8`.
- Banco local/teste: H2; execução Docker: PostgreSQL 16.
- Containerização: `Dockerfile`, `docker-compose.yml`, `frontend/Dockerfile` e `frontend/nginx.conf`.
- Especificações OpenSpec existentes em `openspec/`, inclusive mudanças de autenticação, dashboard, vendas, integração e auditoria/estabilização.

Não há `pom.xml`, Yarn, Maven, workflow GitHub Actions ou diretório de migrations versionadas.

## 4. Diagnóstico da arquitetura atual

### 4.1 Backend

O backend segue uma arquitetura em camadas coerente:

| Camada | Evidência | Diagnóstico |
|---|---|---|
| API/controllers | `src/main/java/.../api/*Controller.java` | REST versionada em `/api/v1`; endpoints de auth, ações, corretoras, carteiras, dashboard e insights. |
| Services | `src/main/java/.../service/` | Regras de aplicação, cálculos, autenticação, vendas, snapshots, integrações e consultas. |
| Repositories | `src/main/java/.../repository/` | Spring Data JPA para usuários, tokens, carteiras, posições, vendas, cotações e snapshots. |
| Domain | `src/main/java/.../domain/` | Entidades para `Usuario`, `Carteira`, `Acao`, `AtivoCarteira`, `Corretora`, movimentações, vendas, tokens e snapshots. |
| DTOs | `src/main/java/.../dto/` | Requests/responses separados por recurso; paginação em `dto/common/PageResponse`. |
| Mappers | `src/main/java/.../mapper/` | Conversão explícita de entidades para respostas, reduzindo exposição direta de JPA. |
| Integrações | `src/main/java/.../integration/adapter` e `facade` | Adapters para BRAPI, Twelve Data, BrasilAPI, ViaCEP, CVM/BCB/Yahoo e facades de cotação/instituição. |
| Configuração | `src/main/java/.../config/` | CORS, clientes HTTP, OpenAPI, propriedades de integração e segurança. |
| Erros | `api/error/` e `exception/` | `@RestControllerAdvice`, `ApiError`, códigos de erro e mapeamento de status. |

### 4.2 API e funcionalidades existentes a preservar

Os endpoints observados incluem:

- autenticação: cadastro, login, refresh, logout, `/me`, recuperação e redefinição de senha;
- ações: CRUD/consulta, ticker, atualização de cotação, revalidação individual/lote e histórico;
- corretoras: listagem, cadastro, consulta por ID/CNPJ, revalidação empresarial e exclusão;
- carteiras: CRUD, posições, filtragem, paginação, vendas/simulação e histórico de vendas;
- dashboard e insights: composição, evolução patrimonial, proventos, indicadores de mercado e análise de moedas.

Não alterar regras de cálculo de preço médio, compra adicional, venda, lucro/prejuízo, moeda/mercado, propriedade de carteira ou validação de corretora durante as fases de segurança, CI/CD e observabilidade.

### 4.3 Frontend

O Angular possui layouts de autenticação e aplicação, rotas protegidas, guards, serviço de autenticação, interceptor de access token/refresh, tratamento de erros, contexto de carteira, dashboards, posições, vendas, componentes de logos e testes unitários. O proxy de desenvolvimento aponta `/api/v1` para `127.0.0.1:8081`; o nginx do container encaminha para `app:8081`.

O frontend usa `withCredentials` para o refresh token em cookie e mantém o access token em memória. Essa funcionalidade deve continuar funcionando após endurecimento de cookie, CORS e cabeçalhos.

### 4.4 Persistência e migrations

As entidades JPA e constraints estão implementadas e cobertas por testes de schema/relacionamento/unicidade. `application-dev.properties` usa `spring.jpa.hibernate.ddl-auto=update` por padrão; testes usam H2 `create-drop`; Docker injeta PostgreSQL e `JPA_DDL_AUTO=update`.

Não foram encontrados Flyway, Liquibase ou scripts SQL de migration. Isso é suficiente para desenvolvimento, mas é risco de deriva de schema e não atende uma operação reprodutível de CI/produção. A criação de migrations deve ser planejada separadamente, preservando o schema e os dados existentes.

### 4.5 Integrações externas

`RestClientConfig` centraliza timeouts configuráveis (`PT5S`/`PT10S`) e retry limitado para GET em 5xx/429/IO. Adapters capturam falhas e alguns registram somente classe do erro/ticker, sem expor tokens. As URLs e chaves são configuráveis por ambiente. Não há métrica ou tracing por dependência externa.

## 5. Matriz de conformidade — Módulo 11

Legenda: **Implementado** = evidência direta; **Parcial** = existe parte, mas falta controle exigido; **Ausente** = não encontrado; **Incompatível/risco** = existe, mas diverge do material ou cria risco relevante.

| Requisito | Estado | Evidência / lacuna |
|---|---|---|
| Modelagem STRIDE antes de mudanças | Ausente | Não há threat model versionado específico para os módulos. Há OpenSpec, mas não uma matriz STRIDE vinculada aos riscos. |
| DTOs em vez de expor entidades | Implementado | `dto/`, `mapper/` e controllers retornando responses; deve ser confirmado por SAST/regra própria. |
| Autenticação e isolamento por usuário | Parcial | `SecurityConfig`, `AuthTokenService`, `CurrentUser` e queries `findByIdAndUsuarioId`; falta suíte completa de fronteiras/IDOR e revisão uniforme de todos os recursos. |
| Senhas protegidas | Implementado | `BCryptPasswordEncoder` em `SecurityConfig` e uso em `AuthService`. |
| Tokens persistentes armazenados por hash | Implementado | `RefreshToken`/`PasswordResetToken` armazenam `tokenHash`; rotação de refresh implementada. |
| SAST | Ausente | Não há Semgrep, CodeQL, regras customizadas, SARIF ou gate. |
| Detecção de segredos | Parcial | `.env` é ignorado e `.env.example` está sem valor de token; não há Gitleaks, histórico auditado ou gate de worktree. |
| Bytecode Java | Ausente | Não há SpotBugs/FindSecBugs nem configuração Gradle equivalente. |
| SBOM/SCA backend | Ausente | Não há CycloneDX, OSV-Scanner ou Dependency-Check configurados. |
| DAST OWASP ZAP | Ausente | Não há script, regras `zap-rules.tsv`, job ou teste de cabeçalhos. |
| Portão único com relatórios | Ausente | Não existe `scripts/security_gate.*`, diretório `security/reports` ou execução fail-closed. |
| Cabeçalhos de segurança | Parcial/incompatível | Spring Security usa frame same-origin; não há evidência de `X-Content-Type-Options`, `Cache-Control` e `Cross-Origin-Resource-Policy` versionados/testados. |
| CORS restritivo | Parcial/incompatível | Origins configuráveis, mas padrões `localhost:[*]`/`127.0.0.1:[*]` são amplos e `allowedHeaders("*")`; precisa revisão para produção. |
| Tratamento de erro sem stack trace | Implementado | `GlobalExceptionHandler` retorna `ApiError` e mensagem genérica para exceção inesperada. Não há correlação/traceId. |
| Dependências auditadas continuamente | Ausente | Sem workflow diário/semanal/manual ou política de exceções. |
| Triagem humana de achados | Ausente | Não há prompt/relatório `security/triagem.md` nem processo documentado. |

### Riscos de segurança prioritários já existentes

1. `AuthController.setCookie` define `Secure=false` e não define `SameSite`; refresh token pode ser exposto em transporte não TLS ou ficar mais suscetível a CSRF dependendo do deployment.
2. CSRF está desabilitado globalmente, embora o refresh token use cookie. A decisão precisa ser formalizada e testada junto com SameSite/origem/CORS.
3. `/h2-console/**` é `permitAll` na configuração geral e o console fica habilitado por padrão no perfil dev; isso não deve alcançar ambientes compartilhados/produção.
4. Existe fallback de segredo JWT em `application.properties` (`dev-secret-change-this-value-please`). O serviço exige comprimento mínimo, mas um valor padrão previsível continua sendo risco operacional se promovido sem override.
5. O perfil `test` libera qualquer request; isso é aceitável para isolamento de testes somente se nunca for ativado fora do ambiente de teste e se houver testes que comprovem a separação.
6. Não há auditoria de eventos sensíveis (login, logout, reset, venda, exclusão) nem correlação de eventos.
7. `spring.jpa.hibernate.ddl-auto=update` em Docker não fornece controle de mudança de schema, rollback ou revisão de migrations.

## 6. Matriz de conformidade — PR Security Validation

| Requisito | Estado | Evidência / lacuna |
|---|---|---|
| Bootstrap determinístico de ferramentas | Ausente | Não há `tools/`, versões fixadas de scanners ou bundle CodeQL. |
| Orquestrador local único | Ausente | Não há comando equivalente a `run-local-pr-gates origin/main`. |
| Fail-closed | Ausente | Nenhum gate local para ferramenta ausente, base inválida, SARIF ausente ou configuração inválida. |
| Base explícita via merge-base | Ausente | Não há preflight; Git está limpo apenas em relação ao branch auditado, com mudanças locais existentes. |
| Escopo commits + staged/unstaged + não ignorados | Ausente | Não há varredura do worktree publicável. |
| Higiene `.env` | Parcial | `.env` está ignorado e `.env.example` vazio, mas não há verificação bloqueante de rastreamento/placeholder. |
| Gitleaks em commits e worktree | Ausente | Não configurado. |
| Build/teste backend alterado | Parcial | `gradlew` e suíte existem; não são acionados por gate de PR. |
| Instalação frontend imutável | Parcial | `package-lock.json` existe e npm deve ser preservado; não há gate `npm ci`/build/teste. |
| SCA frontend por runtime/dev | Ausente | Não há auditoria de lockfile, política de severidade ou exceções datadas. |
| SCA backend com relatório | Ausente | Não há Dependency-Check/SBOM/OSV. |
| CodeQL Java e JS/TS | Ausente | Não há configuração, database, SARIF ou baseline com expiração. |
| Revisão humana de autorização/tenant/PII | Parcial | Isolamento existe em serviços, mas não há política/checagem formal em PR. |
| Workflows remotos de auditoria | Ausente | Não há `.github/workflows`. |
| Template de PR com evidência | Ausente | Não há `.github/pull_request_template.md`. |

## 7. Matriz de conformidade — Módulo 12

| Requisito | Estado | Evidência / lacuna |
|---|---|---|
| Triagem de issue assistida | Ausente | Sem workflow, prompt ou artefato. |
| Revisão de PR assistida | Ausente | Sem workflow; deve ler diff como dado e usar scripts/prompts da base confiável. |
| Notas de versão via PR | Ausente | Não há `CHANGELOG.md` ou workflow de rascunho. |
| Executor neutro de fornecedor | Ausente | Não há `scripts/agent-run.sh`/`agent-install.sh`; não deve ser criado sem autorização de implementação. |
| Agente somente leitura/sem token GitHub | Ausente | Nenhum agente no CI atual. |
| Entrada externa em arquivo | Ausente | Nenhum workflow de issue/PR. |
| Jobs separados para análise e publicação | Ausente | Nenhum workflow. |
| Menor privilégio e `permissions: {}` | Ausente | Nenhum workflow para avaliar. |
| Actions fixadas por SHA e checkout sem credencial persistente | Ausente | Nenhuma action declarada. |
| `pull_request`, nunca checkout de PR em `pull_request_target` | Ausente | Nenhum workflow. |
| Teto de tempo/custo/concurrency | Ausente | Nenhum workflow. |
| zizmor/actionlint | Ausente | Não configurados. |
| CODEOWNERS/proteção de branch | Ausente/não verificável localmente | Não há `CODEOWNERS`; regras remotas do GitHub não estão disponíveis na cópia local. |

## 8. Matriz de conformidade — Módulo 13

| Requisito | Estado | Evidência / lacuna |
|---|---|---|
| Actuator com exposição mínima | Ausente | `build.gradle` não contém actuator; `application*.properties` não contém `management.*`. |
| Health liveness/readiness | Ausente | Não há endpoints/sondas configurados. |
| Prometheus/Micrometer | Ausente | Sem registry Prometheus ou métricas de aplicação/negócio. |
| Tracing OpenTelemetry/OTLP | Ausente | Sem bridge/exporter/endpoint OTLP. |
| Spans JDBC | Ausente | Sem `datasource-micrometer-spring-boot`. |
| Logs estruturados/correlação | Parcial | Existem logs SLF4J em adapters/services, mas não JSON ECS, traceId, política de campos ou proibição de PII documentada. |
| SLO por rota | Ausente | Não há `observability/slo.json`. |
| Teste de fumaça da chegada de telemetria | Ausente | Não há backend local, teste ou verificação por traceId. |
| Teste de carga realista/p95 | Ausente | Não há `perf/`/k6/gates. A paginação existe em endpoints, mas não há medição reportada nesta auditoria. |
| Medir antes/depois | Ausente | Não há baseline de desempenho versionada. |
| Loop de observabilidade com estado | Ausente | Não há `obs_digest.py`, estado, deduplicação ou workflow agendado. |
| Actuator isolado internamente | Ausente | Não há management port nem política de exposição. |
| Revalidação do portão após novas dependências | Ausente | Não existe portão de segurança a ser reexecutado. |

## 9. Dependências e ferramentas necessárias

Nenhuma foi instalada nesta etapa. A lista abaixo é planejamento, não alteração realizada.

### Fase 1 — Segurança

- Semgrep para regras Java/secrets/OWASP e regras próprias de exposição de entidade.
- Gitleaks com configuração e relatório redigido.
- SpotBugs + FindSecBugs por plugin Gradle, filtrado para segurança e limiar revisado.
- CycloneDX Gradle plugin para SBOM e OSV-Scanner; avaliar também OWASP Dependency-Check conforme o documento de PR.
- OWASP ZAP baseline e arquivo de exceções versionado.
- Possível filtro/teste de cabeçalhos de segurança.
- Ajustes de configuração de cookie, CORS, H2 console e segredo obrigatório, preservando o contrato funcional.

### Fase 2 — PR Security Validation

- Scripts PowerShell e/ou shell compatíveis com o ambiente do projeto, mantendo Gradle e npm.
- CodeQL CLI/queries para Java e JS/TS, SARIF gate e baseline com expiração.
- `npm ci`, testes e build Angular condicionais ao escopo alterado; não substituir npm por Yarn.
- Preflight local calculando `merge-base`, verificando diff e worktree publicável.
- Workflows remotos semanais/manuais para secrets, SCA e SAST.

### Fase 3 — Agentes e CI/CD

- `scripts/agent-run` somente leitura, com fornecedor selecionável e versões fixadas.
- Prompts em `.agents/prompts/` explicitando que issue, diff e logs são dados.
- Workflows separados para triagem, revisão de PR, notas de versão e lint.
- `zizmor`, `actionlint`, `CODEOWNERS` e proteção de branch a configurar no GitHub.

### Fase 4 — Desempenho e observabilidade

- Spring Boot Actuator.
- Micrometer Prometheus.
- `micrometer-tracing-bridge-otel` e exporter OTLP.
- `datasource-micrometer-spring-boot` para spans JDBC.
- Backend local opcional `grafana/otel-lgtm` com portas verificadas.
- k6 com imagem fixada, script de carga e SLO por rota.
- Scripts determinísticos de digest/estado e workflow agendado sem agente com poder de escrita.

## 10. Ordem de implementação recomendada

### Fase 1 — Segurança (Módulo 11)

1. Registrar threat model inicial e inventário de ativos/limites de confiança.
2. Fechar os riscos de configuração de autenticação/cookies/H2/CORS, com testes de comportamento e sem alterar regras financeiras.
3. Criar regras SAST e teste de regra para DTO/entity leak, SQL perigoso, hashes fracos e segredos.
4. Criar SBOM/SCA, Gitleaks, SpotBugs/FindSecBugs e gate único com relatórios SARIF quando suportado.
5. Subir aplicação em porta controlada em job isolado e adicionar ZAP baseline com exceções justificadas.
6. Reexecutar testes e validar que o frontend continua autenticando com refresh cookie.

### Fase 2 — PR Security Validation

1. Criar bootstrap de ferramentas com versões fixadas.
2. Criar preflight local fail-closed adaptado a PowerShell/Gradle/npm.
3. Implementar higiene de `.env`, diff completo, Gitleaks e gates condicionais.
4. Adicionar CodeQL Java/JS/TS, SARIF gate e baseline temporária/auditável.
5. Criar template de PR e workflows remotos semanais/manuais.
6. Validar workflows com zizmor/actionlint e documentar que o gate local precede push/PR.

### Fase 3 — Agentes e CI/CD (Módulo 12)

1. Definir tarefas somente de leitura e gatilhos controlados.
2. Criar executor/prompt da branch base e isolamento entre análise e publicação.
3. Implementar triagem de issue, revisão de PR e notas como PR.
4. Fixar actions por SHA, limitar permissões, timeout, custo e concorrência.
5. Proteger workflows/scripts/prompts com CODEOWNERS e revisar branch protection.

### Fase 4 — Desempenho e Observabilidade (Módulo 13)

1. Definir SLO por rota e dependência antes de otimizar.
2. Adicionar Actuator mínimo, métricas RED, logs estruturados e traceId.
3. Adicionar OTLP e spans JDBC; provar a chegada no backend local.
4. Criar carga k6 com aquecimento, volume realista e p95/p99 por rota.
5. Medir, corrigir apenas gargalos evidenciados e medir novamente; mudanças de contrato exigem OpenSpec.
6. Criar digest determinístico, estado/deduplicação e workflow agendado com publicação sem agente.

### Fase 5 — Testes, documentação e validação final

1. Executar Gradle test/verify, testes Angular e build com `npm ci`.
2. Reexecutar gates de segurança completos, CodeQL/SARIF e workflows lint.
3. Validar segurança dinâmica, health/readiness, métricas, traces, logs e carga.
4. Atualizar README/DEPLOYMENT/operacional e registrar evidências.
5. Confirmar preservação dos fluxos financeiros e de autenticação.

## 11. Critérios de aceitação por fase

### Fase 1

- Nenhum scanner obrigatório ausente é tratado como sucesso.
- SAST, secrets, bytecode e SCA geram relatórios reproduzíveis; o gate retorna falha para achados bloqueantes.
- ZAP aponta para a aplicação correta, com headers de segurança testados e exceções justificadas/versionadas.
- Cookie de refresh, CSRF/CORS/H2 e segredo JWT têm comportamento documentado e testes correspondentes.
- Nenhum endpoint retorna entidade JPA diretamente sem decisão explícita.

### Fase 2

- `setup` e preflight falham se ferramenta, base, SARIF, baseline ou configuração estiverem ausentes/inválidos.
- O preflight considera `merge-base`, commits, staged, unstaged e arquivos novos não ignorados.
- Backend usa Gradle; frontend usa `npm ci`, testes e build; nenhuma troca de gerenciador ocorre.
- CodeQL executa Java e JS/TS; baseline possui responsável e expiração.
- A PR contém linha de evidência de aprovação e plano de testes.

### Fase 3

- O agente não possui token GitHub, não grava arquivos, não faz push e não executa a própria saída.
- Entradas de issue/PR/diff são arquivos tratados como dados; nenhum valor externo é interpolado em `run`/prompt.
- Publicação ocorre em job separado com permissão mínima; notes viram PR e não push direto em `main`.
- Workflows passam por zizmor pedante e actionlint; actions estão fixadas por SHA.

### Fase 4

- `/actuator/health/liveness`, `/readiness` e `/actuator/prometheus` funcionam com exposição mínima; `env`, `heapdump` e similares permanecem fechados.
- Logs não contêm senha, tokens, PII ou corpo completo de requisições; eventos possuem campos estáveis e traceId.
- Um trace de endpoint de carteira mostra HTTP e spans JDBC no backend de observabilidade.
- O teste k6 usa volume realista, aquecimento e thresholds de p95/erro por rota; resultados do servidor são conferidos.
- O digest deduplica achados e não altera limites SLO para mascarar falhas.

### Fase 5

- Testes backend/frontend e builds passam no ambiente limpo documentado.
- O portão de segurança passa depois das dependências de observabilidade.
- Há evidências de teste de fumaça, DAST, SCA, SAST, carga e chegada de telemetria.
- Funcionalidades financeiras e contratos preservados têm cenários automatizados ou evidência explícita.

## 12. Decisões e restrições para a implementação futura

- Manter Gradle, Gradle Wrapper, Java 21, Angular e npm/package-lock.
- Não migrar Maven/Yarn; comandos dos materiais devem ser traduzidos para `gradlew` e `npm ci`/`npm test`/`npm run build`.
- Não modificar regras financeiras nem remover endpoints/funcionalidades existentes.
- Qualquer alteração de contrato público, paginação, headers consumidos pelo frontend ou formato de erro deve passar por OpenSpec e testes de compatibilidade.
- Não considerar teste “verde” sem execução real: toda conclusão de validação futura deve registrar comando, versão, ambiente, escopo e artefato.

## 13. Limitações desta auditoria

- Não foram executados testes, build, scanners, DAST ou carga nesta etapa; portanto não há resultado de aprovação/reprovação de execução para inventar.
- Regras remotas de proteção de branch, CODEOWNERS efetivo, secrets/variables do GitHub e histórico completo do remoto não são verificáveis apenas pela cópia local.
- A auditoria é estática; achados de segurança devem ser validados dinamicamente e por revisão humana antes de serem classificados como vulnerabilidades confirmadas.

## 14. Autorização necessária

O relatório encerra a etapa de auditoria. A implementação da **Fase 1 — Segurança** deve aguardar autorização explícita do responsável pelo projeto.

