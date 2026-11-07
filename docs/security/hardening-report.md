# Relatório de hardening — Módulo 11

Data: 2026-11-03  
Change: `harden-application-security-module-11`  
Status: implementação inicial em validação; módulo não concluído.

## Escopo realizado

- Criada a change OpenSpec com `proposal.md`, `design.md`, `tasks.md` e deltas de specs.
- Criada a política `AuthProperties` para segredo JWT e refresh cookie.
- Removido o fallback JWT de runtime; fora do perfil de teste, a aplicação exige segredo externo seguro.
- Preservado o formato atual de access token HMAC, expiração, rotação de refresh token e armazenamento por hash.
- Cookie de refresh passou a usar `HttpOnly`, `Secure`, `SameSite`, path e expiração configuráveis.
- CORS passou de padrões amplos/wildcard para origens, métodos e cabeçalhos configuráveis e credenciais explícitas.
- H2 Console passou a ficar desabilitado por padrão em `dev` e habilitável somente no profile local explícito.
- `/api/v1/auth/me` deixou de estar implicitamente público; endpoints públicos de autenticação foram enumerados.
- Adicionados testes focados para segredo, tokens, login, cookie, isolamento, CORS e boundary CSRF.
- Criado o modelo STRIDE em [threat-model.md](threat-model.md).

## SpotBugs e FindSecBugs - 2026-11-05

Foi configurado o plugin `com.github.spotbugs` `6.5.13`, com SpotBugs `4.10.5`, FindSecBugs `1.14.0`, relatorios HTML/XML, categoria `SECURITY`, `ignoreFailures=false` e filtros versionados em `config/spotbugs/`. O Gradle preserva a tarefa `spotbugsMain` e o gate local passou a executa-la.

Correcoes realizadas:

- `SecurityConfig.jsonError` deixou de concatenar JSON manualmente e passou a usar `ObjectMapper.writeValueAsBytes`, removendo o achado `XSS_SERVLET`.
- A desativacao de CSRF foi mantida porque o contrato atual exige bearer token para mutacoes financeiras e o cookie e usado somente no fluxo de refresh/logout. O achado `SPRING_CSRF_PROTECTION_DISABLED` e excluido somente por padrao, com justificativa no `config/spotbugs/security-exclude.xml`, no threat model e em teste de regressao.
- O detector `CorsRegistryCORSDetector` foi omitido de forma especifica devido a uma excecao interna reproduzida no padrao de `CorsConfig`; a limitacao e registrada em `scanner-findings.md`.

Resultado observado: `spotbugsMain` executou com FindSecBugs carregado, gerou os relatorios HTML/XML e terminou com sucesso; o relatorio final nao possui achados emitidos. A contagem bruta anterior a correcao/excecao foi de dois achados: um `XSS_SERVLET` corrigido e um `SPRING_CSRF_PROTECTION_DISABLED` tratado como excecao arquitetural documentada.

Validacao final apos o ajuste:

| Comando | Resultado real |
|---|---|
| `./gradlew.bat clean test` | APROVADO; 100 testes, 0 falhas, 1 ignorado. |
| `./gradlew.bat build` | APROVADO. |
| `./gradlew.bat spotbugsMain` | APROVADO; relatorios HTML/XML gerados, 0 `BugInstance` emitidos. |
| `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\\scripts\\security-gate.ps1` | APROVADO; Semgrep, Gitleaks atual e SpotBugs/FindSecBugs passaram. |

O resultado aprovado do gate se refere ao codigo e aos arquivos atuais nos escopos configurados. A analise historica do Gitleaks continua com 1 ocorrencia BRAPI pendente e nao e mascarada pelo gate local.

O token BRAPI continua pendente no historico Git. O worktree atual esta sem o valor, mas o historico ainda contem uma ocorrencia sanitizada no commit ja documentado; nao houve revogacao automatica, limpeza de historico, commit ou push.

## Arquivos principais

- `src/main/java/com/example/carteirainvestimento/config/AuthProperties.java`
- `src/main/java/com/example/carteirainvestimento/config/CorsProperties.java`
- `src/main/java/com/example/carteirainvestimento/config/CorsConfig.java`
- `src/main/java/com/example/carteirainvestimento/config/SecurityConfig.java`
- `src/main/java/com/example/carteirainvestimento/service/AuthTokenService.java`
- `src/main/java/com/example/carteirainvestimento/api/AuthController.java`
- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-local.properties`
- `src/main/resources/application-test.properties`
- `src/test/java/com/example/carteirainvestimento/service/AuthTokenServiceSecurityTest.java`
- `src/test/java/com/example/carteirainvestimento/api/SecurityRegressionTest.java`
- `src/test/java/com/example/carteirainvestimento/api/H2ConsoleSecurityTest.java`

## Diagnóstico do ambiente

- Sistema operacional: Windows 11, build `10.0.26100.9711`, PowerShell `5.1.26100.9711`.
- Java no PATH: Temurin/OpenJDK `21.0.12.1`.
- `JAVA_HOME` original: apontava para um diretório inexistente de JDK 17. Para as validações Gradle foi usado apenas no processo o caminho instalado `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`.
- Gradle Wrapper: `9.5.1`, definido em `gradle/wrapper/gradle-wrapper.properties`.
- Node.js: `v24.20.0`; npm: `12.0.2`.
- Backend: raiz do repositório, onde estão `build.gradle`, `gradlew.bat` e `src/`.
- Frontend: `frontend/`, com `package.json`, `package-lock.json`, `angular.json` e `node_modules/`.
- Repositório Gradle: somente `mavenCentral()` em `build.gradle`.
- Registry npm: `https://registry.npmjs.org/`; o lockfile preserva URLs HTTPS.
- Não foram encontrados proxies HTTP/HTTPS nas variáveis de ambiente.
- O cache Gradle local foi redirecionado para `.gradle-user-home` porque o padrão tentava criar lockfile em `C:\.gradle`, sem permissão. O cache não continha o POM requerido de `spring-boot-starter-security:4.0.8`.
- `npm cache verify` falhou com `EPERM` ao remover um arquivo do cache global em `C:\Users\jhona\AppData\Local\npm-cache`; não foi executada limpeza destrutiva.

## Validação executada

| Comando | Resultado | Observação |
|---|---|---|
| `openspec validate harden-application-security-module-11 --strict` | APROVADO | Change válida no schema `spec-driven`. |
| `$env:JAVA_HOME=...; $env:GRADLE_USER_HOME=...; .\gradlew.bat clean test` | BLOQUEADO | `:clean` executou, mas `:compileJava` não resolveu `spring-boot-starter-security:4.0.8`; Maven Central retornou `Permission denied: getsockopt`. Repetido com Java 21. |
| `$env:JAVA_HOME=...; $env:GRADLE_USER_HOME=...; .\gradlew.bat build` | BLOQUEADO | Executado fora do sandbox; falhou no mesmo ponto de resolução HTTPS do Maven Central, antes da compilação. |
| `cd frontend; npm run build` (sandbox) | BLOQUEADO PELO AMBIENTE | Erros `Access is denied` ao ler módulos/arquivos; resultado não usado como falha do código. |
| `cd frontend; npm run build` (fora do sandbox) | APROVADO COM WARNINGS | Bundle Angular gerado em `frontend/dist/frontend`; warnings preexistentes de orçamento e `DecimalPipe` não utilizado. |
| `cd frontend; npm test -- --watch=false` (fora do sandbox) | APROVADO | 15 arquivos de teste e 63 testes passaram. Warning `DecimalPipe` não utilizado. |

O bloqueio do backend foi reproduzido fora do sandbox, portanto permanece uma falha de rede/socket ou política do ambiente Windows, não uma falha de compilação demonstrada. Não houve resolução DNS/TCP funcional para `repo.maven.apache.org:443` durante o diagnóstico. Não foram desabilitados TLS, verificações de segurança ou repositórios seguros.

Nenhum resultado de teste é considerado aprovado sem execução real. O bloqueio de rede não é tratado como sucesso.

## Pendências e riscos residuais

- Naquele ponto histórico do relatório ainda não haviam sido implementados Semgrep, Gitleaks, SpotBugs/FindSecBugs, SBOM CycloneDX, OSV-Scanner, OWASP ZAP ou portão automatizado; as camadas Semgrep, Gitleaks e SpotBugs/FindSecBugs foram adicionadas nas seções posteriores, enquanto SBOM/OSV/ZAP continuam pendentes.
- Não foram criadas migrations; `ddl-auto=update` permanece e requer change própria antes de produção.
- Não foram adicionados headers de segurança do ZAP, Actuator, tracing ou observabilidade; pertencem às etapas posteriores.
- A decisão CSRF depende de nenhum endpoint financeiro aceitar autenticação exclusivamente por cookie; isso está documentado e testado na suíte proposta, mas aguarda execução.
- A política exige `AUTH_JWT_SECRET` externo fora de teste; ambientes locais/deployment precisam fornecer valor seguro operacionalmente.
- O profile `test` mantém permissividade histórica apenas para testes existentes via `app.security.test-permit-all=true`; a suíte de segurança desabilita explicitamente essa opção. Esse bypass nunca deve ser usado em deployment.
- Os testes backend de segurança, autenticação, autorização, CORS, cookies, CSRF e H2 ainda não foram executados por falta das dependências Gradle; não há evidência para marcá-los como aprovados.
- O build frontend passou somente fora do sandbox; a diferença de ambiente deve ser considerada ao reproduzir localmente.

## Comandos para reprodução manual no Windows

Executar em PowerShell, após garantir acesso HTTPS ao Maven Central e ao registry npm. Os comandos alteram apenas variáveis do processo atual:

```powershell
Set-Location 'C:\Users\jhona\Desktop\CarteiraInvestimento'
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:GRADLE_USER_HOME = 'C:\Users\jhona\Desktop\CarteiraInvestimento\.gradle-user-home'
Test-NetConnection repo.maven.apache.org -Port 443
.\gradlew.bat clean test
.\gradlew.bat build

Set-Location '.\frontend'
npm ci
npm run build
npm test -- --watch=false
```

`npm ci` só deve ser usado se a instalação local continuar inconsistente; ele preserva `package-lock.json`. Não executar `npm audit fix --force`.

## Não realizado

- Nenhuma regra financeira, fórmula de lucro/prejuízo, preço médio, compra repetida, histórico, integração externa ou tabela foi alterada.
- Nenhum comando destrutivo, migration, commit, push ou merge foi executado.
- Módulos 12 e 13 não foram iniciados.

## Conclusão provisória

A implementação inicial de hardening e a modelagem STRIDE foram produzidas, e a validação do frontend foi executada com sucesso. A Fase 1 não pode ser declarada concluída enquanto a suíte completa do backend não passar. Os scanners e o portão de segurança permanecem explicitamente fora desta change, conforme solicitado.

## Atualização de diagnóstico e validação — 2026-11-04

O acesso ao Maven Central foi verificado fora do sandbox do Codex. DNS, TCP/443 e HTTPS funcionaram no PowerShell normal do Windows; o erro `Permission denied: getsockopt` ficou confirmado como restrição do ambiente de execução do Codex, não como problema de repository, TLS ou código.

Correções mínimas realizadas para estabilizar a validação:

- `SecurityConfig.java`: substituída a expressão condicional inválida sobre métodos `void` por um bloco equivalente, preservando `sameOrigin` no profile `local` e `deny` nos demais profiles.
- `SecurityRegressionTest.java`: o teste passou a criar seu `ObjectMapper` localmente, pois esta configuração não expõe `ObjectMapper` como bean. Nenhuma regra de produção foi alterada.

Resultados reais fora do sandbox:

| Comando | Resultado |
|---|---|
| `./gradlew.bat clean test` | Compilação passou; 100 testes executados, 1 falhou e 1 foi ignorado. |
| `./gradlew.bat build` | Compilação e empacotamento passaram; falhou na etapa de testes pelo mesmo teste financeiro. |
| `./gradlew.bat test --tests AuthTokenServiceSecurityTest --tests SecurityRegressionTest --tests H2ConsoleSecurityTest` | APROVADO. Suíte focada de segurança passou. |

A única falha restante é `CarteiraControllerTest.rejeitaDadosInvalidosEPosicaoDuplicada`, em `CarteiraControllerTest.java:103`: o teste espera HTTP 409 para posição duplicada, mas a aplicação retorna HTTP 201. Essa falha envolve regra financeira existente e não foi alterada.

Status OpenSpec após a validação: `5.2` permanece concluída; `5.1` permanece pendente por causa da falha da suíte completa. As tarefas específicas de segurança não devem ser consideradas globalmente concluídas até a revisão da pendência financeira e a confirmação final do escopo.

## Execução específica do bloqueio de compilação — 2026-11-04

Com Java 21 e `GRADLE_USER_HOME` configurados no processo atual, fora do sandbox:

| Comando | Resultado real |
|---|---|
| `.\gradlew.bat compileJava` | APROVADO; `BUILD SUCCESSFUL`. |
| `.\gradlew.bat clean test` | Compilação aprovada; 100 testes executados, 1 falhou e 1 foi ignorado. |
| `.\gradlew.bat build` | Empacotamento aprovado, mas a tarefa `test` falhou pela mesma falha financeira. |

A falha de compilação do `SecurityConfig` foi eliminada pela lambda em bloco de `frameOptions`. Não foram identificados outros erros de compilação relacionados ao Spring Security 4.0.8. A suíte focada de segurança continua aprovada; a pendência é exclusivamente a falha de regressão financeira documentada acima.
## Correção da regressão de compras repetidas — 2026-11-04

### Causa

O teste `CarteiraControllerTest.rejeitaDadosInvalidosEPosicaoDuplicada` esperava `409 DUPLICATE_RESOURCE` ao repetir a combinação carteira/ação/corretora. Essa expectativa não correspondia ao comportamento vigente: `AtivoCarteiraService.criar` consolida uma nova compra na posição existente, soma a quantidade e recalcula o preço médio ponderado. O relatório XML confirmou resposta `201` e posição resultante com quantidade `20`.

### Correção

O teste foi renomeado para `rejeitaDadosInvalidosEConsolidaComprasRepetidas` e passou a verificar:

- rejeição de quantidade zero com `400 VALIDATION_ERROR`;
- segunda compra do mesmo ativo pela mesma corretora com `201`;
- quantidade consolidada igual a `20`;
- preço médio ponderado igual a `20`, calculado a partir de 10 unidades a 25 e 10 unidades a 15;
- uma única posição retornada na listagem da carteira.

Nenhuma regra de produção, fórmula financeira, migration, integração ou configuração de segurança foi modificada.

### Validação final

| Comando | Resultado |
|---|---|
| `./gradlew.bat test --tests "*CarteiraControllerTest"` | APROVADO. |
| `./gradlew.bat clean test` | APROVADO. Suíte completa executada sem falhas. |
| `./gradlew.bat build` | APROVADO. Artefato compilado e testes concluídos. |

O isolamento de carteiras, autenticação JWT, autorização, validação de DTOs e tratamento de exceções permaneceram cobertos pela suíte existente e pela suíte focada de segurança anteriormente aprovada.
Status atual desta auditoria: a tarefa OpenSpec `5.1` foi concluída após a correção do teste de compras repetidas. A suíte completa e o build passaram; a observação anterior de pendência financeira fica superada pelos resultados registrados na seção de correção acima.

## SAST, segredos e portão — 2026-11-04

O tutorial local obrigatório não está presente; as seções 11, 12 e 16 foram consultadas na cópia oficial remota. A execução abaixo não presume conformidade além do que foi efetivamente verificado.

### Ferramentas e escopo

- Semgrep `1.178.0`, executado por `uvx`, sempre com `--metrics=off`.
- Gitleaks `v8.30.1`, executado por Docker, sempre com `--redact`.
- Docker Engine `29.7.2`; `uv`/`uvx` `0.13.1`.
- Backend: `src/`; Angular: `frontend/src/`; documentação/configuração em escopos separados; histórico Git em execução separada.
- Regra customizada: `semgrep-rules/controller-returns-jpa-entity.yaml`, com fixture Java positiva/negativa.
- Relatório detalhado sanitizado: [scanner-findings.md](scanner-findings.md).

### Resultados

| Camada | Comando/escopo | Resultado |
|---|---|---|
| Semgrep oficial | `p/java`, `p/secrets`, `p/owasp-top-ten` sobre backend | 0 achados; 142 arquivos; SARIF gerado. |
| Semgrep oficial | regras JavaScript/segredos/OWASP sobre Angular | 0 achados; 79 arquivos; SARIF gerado. |
| Semgrep combinado | fontes backend/Angular + regra customizada | 0 achados; 221 arquivos; SARIF `security/reports/semgrep-all.sarif`. |
| Teste customizado | `uvx semgrep==1.178.0 --metrics=off --test semgrep-rules/` | 1/1 teste aprovado. |
| Gitleaks | `src`, `frontend/src`, `docs`, configurações selecionadas | 0 achados em código/configuração/documentação desses escopos. |
| Gitleaks | `.gitignore` rastreado | 1 `generic-api-key` na linha 56. |
| Gitleaks histórico | 27 commits | 1 `generic-api-key` no mesmo `.gitignore`, commit `ab8a7c34c41c9336e8365b3203751f162b0ecd96`. |
| Portão local | `powershell -File scripts/security-gate.ps1` | REPROVADO corretamente por `gitleaks:gitignore`; Semgrep aprovado. |

### Achado e risco residual

O valor da linha 56 de `.gitignore` tem formato de token de API e também aparece no histórico. O valor não foi reproduzido neste relatório nem nos logs intencionais. A validade não foi testada e nenhum serviço externo foi acessado.

Classificação provisória: alto, possível credencial exposta. Recomenda-se revogação/rotação pelo proprietário, remoção do valor do arquivo rastreado e avaliação de limpeza do histórico. Essas ações não foram executadas porque exigem autorização e podem alterar estado externo ou histórico Git.

O `.env` local, ignorado pelo Git, contém variáveis de credenciais não vazias e deve ser tratado como potencial segredo operacional. Os artefatos `build/` também geraram falsos positivos de senhas aleatórias de testes; não foram usados para declarar falha de código.

### Regressão após scanners

| Comando | Resultado |
|---|---|
| `./gradlew.bat clean test` | APROVADO. |
| `./gradlew.bat build` | APROVADO. |
| `cd frontend; npm run build` | APROVADO, com warnings existentes de orçamento e `DecimalPipe`. |
| `cd frontend; npm test -- --watch=false` | APROVADO; 15 arquivos e 63 testes. |

Nenhuma regra financeira, migration, integração, frontend ou histórico Git foi alterada pelos scanners. SBOM/CycloneDX, OSV-Scanner e OWASP ZAP permanecem pendentes, assim como a triagem operacional do segredo.

## Investigação do achado Gitleaks em `.gitignore` — 2026-11-04

O achado `generic-api-key` foi localizado na linha 56 do `.gitignore`, associado à variável `BRAPI_TOKEN`. O serviço provável é BRAPI, pois essa variável é usada pela integração configurada para `brapi.dev`. O valor foi omitido de todos os relatórios, comandos exibidos e respostas.

A ocorrência foi encontrada no worktree relevante somente em `.gitignore`. O arquivo é rastreado pelo Git. A análise de referências encontrou apenas `main` e `origin/main`; ambas contêm o arquivo. A consulta somente leitura ao remoto exibiu apenas `refs/heads/main`. O histórico Gitleaks identificou o commit `ab8a7c34c41c9336e8365b3203751f162b0ecd96` como afetado.

Classificação atual: **possível segredo não confirmado**. O formato e a associação à variável justificam tratá-lo como comprometido até confirmação do proprietário. Não foi possível confirmar validade, revogação ou rotação sem acessar o provedor; nenhum serviço externo foi consultado.

O arquivo, o histórico e o remoto não foram modificados. Não foi executada revogação, rotação, remoção ou reescrita histórica. Próxima ação autorizável: o responsável pelo BRAPI deve revogar/rotacionar a credencial; só então deve-se substituir o valor por configuração externa/placeholder e planejar limpeza do histórico.

Reexecução do gate:

```text
Semgrep: aprovado, 0 achados.
Gitleaks: reprovado em gitleaks:gitignore.
Resultado final: REPROVADO.
```

O portão não foi alterado para ocultar o achado. O risco residual é exposição histórica potencial da credencial e possível reutilização em ambientes externos.

## Remoção da credencial atual — 2026-11-04

O valor detectado foi removido exclusivamente do `.gitignore`. As regras existentes foram preservadas, incluindo `.env` e `.env.local`; nenhum token novo foi incluído no repositório.

A integração BRAPI permanece externa e funcional por configuração:

```properties
integrations.brapi.token=${BRAPI_TOKEN:}
```

Os adapters BRAPI não foram alterados. Os testes de integração usam `MockRestServiceServer`, cobrindo token ausente, resposta não autorizada, respostas incompletas e falhas do provedor sem chamadas reais à BRAPI.

Resultados após a remoção:

| Validação | Resultado |
|---|---|
| Testes mockados BRAPI: `./gradlew.bat test --tests "*Brapi*"` | APROVADO. |
| `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\security-gate.ps1` | APROVADO. Semgrep e Gitleaks dos escopos atuais sem achados. |
| Gitleaks histórico `git /repo` | 1 achado histórico permanece no commit antigo. |
| `./gradlew.bat clean test` | APROVADO. |
| `./gradlew.bat build` | APROVADO. |

O histórico Git não foi reescrito e o remoto não foi alterado. O risco residual é a exposição histórica potencial; a limpeza histórica continua pendente e requer autorização separada. A revogação/rotação foi informada como realizada pelo solicitante, mas não foi verificada diretamente no provedor.
## Validação reproduzida em 2026-11-05

Os comandos foram repetidos com Java 21 e um `GRADLE_USER_HOME` alternativo no workspace, sem remover o cache original. O cache original apresentou `AccessDeniedException` ao compilar; o cache alternativo permitiu a execução completa.

| Comando | Resultado real |
|---|---|
| `./gradlew.bat clean test` | APROVADO; `BUILD SUCCESSFUL`. |
| `./gradlew.bat build` | APROVADO; `BUILD SUCCESSFUL`, incluindo `spotbugsMain` e `spotbugsTest`. |
| `./gradlew.bat spotbugsMain --rerun-tasks` | APROVADO; `BUILD SUCCESSFUL`; HTML/XML gerados. FindSecBugs carregado no XML (`com.h3xstream.findsecbugs`). |
| `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\\scripts\\security-gate.ps1` | REPROVADO por falha de execução dos nove comandos Gitleaks: Docker Desktop indisponível. Semgrep passou com 0 achados e SpotBugs/FindSecBugs passou. |

O gate permaneceu fail-closed: a indisponibilidade do Docker não foi convertida em aprovação e não é evidência de que os escopos Gitleaks estejam limpos. A pendência histórica BRAPI continua sem revogação verificada. Nenhum commit, push, publicação ou alteração de histórico foi executado.
## Estabilização reproduzida em 2026-11-06

- `JAVA_HOME` apontava para JDK 17; a execução foi corrigida no processo para Temurin JDK 21.0.12.1.
- O cache padrão `.gradle-user-home` é legível diretamente, mas o Gradle falhou com `AccessDeniedException` durante a compilação. Não havia daemons Java/Gradle ativos.
- Foi usado o cache alternativo temporário `.gradle-user-home-validation`, sem apagar ou alterar o cache padrão.
- `./gradlew.bat clean test --console=plain`: APROVADO.
- `./gradlew.bat build --console=plain`: APROVADO.
- `./gradlew.bat spotbugsMain --rerun-tasks --console=plain`: APROVADO.
- Docker CLI `29.7.2` está instalado, mas o Engine/Desktop não está operacional: `docker version` e `docker info` falharam porque o named pipe `docker_engine` não existe. Também houve `Access is denied` em `C:\Users\jhona\.docker\config.json`.
- `scripts/security-gate.ps1`: REPROVADO fail-closed. Semgrep passou com 0 achados; SpotBugs/FindSecBugs passou; todos os escopos Gitleaks falharam por indisponibilidade do Docker.

Para executar o gate posteriormente, iniciar o Docker Desktop pelo menu Iniciar, aguardar o Engine ficar em estado Running e confirmar com `docker info`; depois repetir o comando do gate. A credencial BRAPI histórica continua um risco aberto e não foi revogada.
## SBOM e SCA reproduzidos em 2026-11-07

O tutorial local `tutorial/modulo-11-seguranca.md` nÃ£o estava disponÃ­vel. A configuraÃ§Ã£o foi adaptada ao projeto usando documentaÃ§Ã£o oficial das ferramentas. Nenhuma dependÃªncia foi atualizada e `npm audit fix` nÃ£o foi executado.

Ferramentas: CycloneDX Gradle `3.5.1`; CycloneDX npm `6.0.1`; OSV-Scanner oficial `v2.7.0` Windows amd64 com checksum verificado. RelatÃ³rios: `security/reports/sbom-backend.json`, `sbom-frontend.json`, `osv-backend.json` e `osv-frontend.json`.

Os SBOMs sÃ£o CycloneDX JSON 1.6: backend com 109 componentes e frontend com 336 componentes. O OSV leu 109 pacotes do backend e 371 do frontend.

| Escopo | Raw | IDs Ãºnicos | CrÃ­ticas | Altas | Moderadas | Baixas |
|---|---:|---:|---:|---:|---:|---:|
| Backend | 18 | 11 | 4 | 10 | 4 | 0 |
| Frontend | 42 | 42 | 2 | 13 | 24 | 3 |

Os quatro achados crÃ­ticos do backend estÃ£o em `org.apache.tomcat.embed:tomcat-embed-core:11.0.24`, transitivo do Spring Boot `4.0.8`; o OSV indica `11.0.25` como correÃ§Ã£o. Jackson 2.21.5 e `tools.jackson` 3.1.5 tambÃ©m sÃ£o transitivos/gerenciados. No frontend, `@angular/router:22.1.3` Ã© direto; os demais achados sÃ£o majoritariamente transitivos ou de tooling. Nenhuma atualizaÃ§Ã£o foi aplicada sem revisÃ£o de compatibilidade/autorizacÃ£o.

RegressÃ£o: `clean test`, `build`, `spotbugsMain --rerun-tasks`, `npm.cmd run build` e `npm.cmd test -- --watch=false` foram aprovados; o frontend manteve apenas warnings existentes e 63 testes passaram. SpotBugs/FindSecBugs permaneceu com 0 achados.

O Security Gate foi `REPROVADO` fail-closed: Semgrep passou com 0 achados; Gitleaks nÃ£o foi executado com sucesso porque Docker Engine/Desktop estava indisponÃ­vel; OSV encontrou vulnerabilidades nos dois SBOMs. O token BRAPI antigo continua exposto no histÃ³rico Git, sem revogaÃ§Ã£o confirmada, e permanece risco aberto.
## CorreÃ§Ã£o de dependÃªncias OSV reproduzida em 2026-11-07

Foi aplicada uma correÃ§Ã£o de patch compatÃ­vel no backend: Spring Boot `4.0.8` -> `4.0.9`. A consulta ao BOM do Spring Boot confirmou Tomcat `11.0.27`, Jackson 2 `2.21.7` e Jackson 3 `3.1.7`, eliminando os componentes vulnerÃ¡veis sem constraints manuais, alteraÃ§Ã£o de Java/Gradle ou mudanÃ§a de contrato.

No frontend, foram atualizados somente pacotes dentro das majors existentes: Angular para `22.2.5` (build/CLI `22.2.6`) e Vitest para `4.1.12`. O `package-lock.json` foi regenerado apÃ³s a detecÃ§Ã£o de entradas opcionais inconsistentes no lockfile anterior; a verificaÃ§Ã£o final com `npm ci` normal passou. NÃ£o foi executado `npm audit fix`.

| Escopo | Antes | Depois | Resultado OSV |
|---|---|---|---|
| Backend | Tomcat 11.0.24; Jackson 2.21.5/3.1.5; 18 ocorrÃªncias, 11 IDs | Spring Boot 4.0.9; Tomcat 11.0.27; Jackson 2.21.7/3.1.7 | 0 resultados; OSV exit 0 |
| Frontend | 336 componentes SBOM; 371 pacotes OSV; 42 vulnerabilidades | 272 componentes SBOM; 308 pacotes OSV | 0 resultados; OSV exit 0 |

RegressÃ£o apÃ³s a correÃ§Ã£o:

- `./gradlew.bat clean test --console=plain`: APROVADO.
- `./gradlew.bat build --console=plain`: APROVADO.
- `./gradlew.bat spotbugsMain --console=plain`: APROVADO; 0 achados.
- `cd frontend; npm ci`: APROVADO.
- `cd frontend; npm run build`: APROVADO, com warnings preexistentes de budget e `DecimalPipe`.
- `cd frontend; npm test -- --watch=false`: APROVADO; 15 arquivos e 63 testes.
- RegeraÃ§Ã£o dos dois SBOMs e OSV-Scanner: APROVADA, 0 resultados em ambos.

O Security Gate permanece `REPROVADO` fail-closed somente pelos escopos Gitleaks, pois o Docker Engine continua indisponÃ­vel. Semgrep passou com 0 achados, OSV passou nos dois escopos e SpotBugs/FindSecBugs passou com 0 achados. A credencial BRAPI histÃ³rica continua risco aberto e sem revogaÃ§Ã£o confirmada.
