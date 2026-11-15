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
## Preparação DAST OWASP ZAP — 2026-11-07

O tutorial local `tutorial/modulo-11-seguranca.md` continua ausente. A preparação segue o Baseline Scan oficial do ZAP, que realiza spider limitado e análise passiva, sem ataques ativos. Foi criado `scripts/run-zap-baseline.ps1` com imagem oficial versionada `ghcr.io/zaproxy/zaproxy:2.17.0`, relatórios HTML/JSON/XML em `security/reports/` e alvo padrão `http://host.docker.internal:8081/swagger-ui/index.html`.

O runner aceita somente HTTP para `localhost`, `127.0.0.1`, `host.docker.internal` ou loopback IPv6. Não aceita credenciais na URL, domínios externos ou produção. O processo não usa BRAPI nem outros serviços externos como alvo.

Resultado do ambiente: Docker CLI `29.7.2` está instalado, porém `docker version`/`docker info` não conseguem conectar ao named pipe `docker_engine`; o Docker Engine/Desktop está indisponível. Não havia processo escutando na porta local `8081`. A execução do runner foi interrompida fail-closed com `Docker Engine is unavailable; ZAP Baseline was not executed.`

Portanto, nesta validação: DAST **NÃO EXECUTADO**, sem relatórios ZAP, sem alertas classificados e sem alegação de cobertura. Para executar posteriormente, iniciar o Docker Desktop, iniciar o backend isolado com H2/test data e credenciais exclusivamente de teste, confirmar `docker info` e então executar o runner. O token BRAPI histórico continua risco aberto, sem revogação confirmada.
## OWASP ZAP / DAST executado em 2026-11-07

O Docker Desktop ficou operacional com permissÃ£o elevada: Docker `29.7.2`, Docker Desktop `4.88.1`. O backend foi iniciado somente para o DAST com Java 21, Spring profile `dev`, H2 em memÃ³ria `jdbc:h2:mem:dast`, `JPA_DDL_AUTO=create-drop`, `H2_CONSOLE_ENABLED=false`, segredo JWT temporÃ¡rio exclusivo da execuÃ§Ã£o e integraÃ§Ãµes externas sem URLs/chaves. O processo foi encerrado apÃ³s o scan.

Comando executado:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-zap-baseline.ps1
```

Imagem: `ghcr.io/zaproxy/zaproxy:2.17.0`. Alvo Ãºnico: `http://host.docker.internal:8081/swagger-ui/index.html`. O container confirmou conectividade e cobriu 13 URLs, todas locais. RelatÃ³rios sanitizados: `security/reports/zap-baseline.html`, `zap-baseline.json` e `zap-baseline.xml`.

Resultado inicial: 7 categorias WARN-NEW, 0 FAIL, 61 PASS. ApÃ³s a correÃ§Ã£o de headers defensivos no `SecurityConfig`, o resultado foi 6 categorias WARN-NEW, 0 FAIL, 62 PASS. O exit code `2` do ZAP representa alertas WARN; nÃ£o foi tratado como falha de infraestrutura.

Triagem:

| Alerta | Severidade | SituaÃ§Ã£o |
|---|---|---|
| Vulnerable JS Library — DOMPurify `3.2.6` no Swagger UI | High, confianÃ§a Medium | Risco residual; o webjar atual `swagger-ui:5.30.1` Ã© transitivo do SpringDoc. A atualizaÃ§Ã£o proposta para `5.34.1` nÃ£o foi aplicada porque o Gradle nÃ£o conseguiu resolver Maven Central por erro TLS/rede. |
| CSP: `frame-ancestors`/`form-action` sem fallback e `unsafe-inline` | Medium, confianÃ§a High | Header CSP adicionado; os avisos restantes refletem compatibilidade necessÃ¡ria do Swagger UI e exigem uma polÃ­tica com nonce/hash para eliminaÃ§Ã£o completa. |
| Cross-Origin-Embedder/Resource-Policy | Low | Headers defensivos adicionados; o ZAP ainda reporta incompatibilidade em recursos Swagger. Não foi aplicado `require-corp` para não arriscar quebrar recursos/API cross-origin. |
| Timestamp Disclosure — Unix | Low | Evidência em JavaScript estático do Swagger UI; sem dados financeiros ou segredo. |
| Non-Storable Content | Informational | `no-store` em respostas 401 e recursos locais; comportamento conservador para conteúdo potencialmente sensível. |
| Modern Web Application | Informational | Caracterização do Swagger UI, sem correção necessária. |

As correções de headers não alteraram autenticação, contratos REST, migrations ou regras financeiras. Regressão após a correção: `clean test`, `build` e `spotbugsMain` aprovados; `npm run build` aprovado; 63 testes frontend aprovados.

O Security Gate foi executado explicitamente com `-RunZap`: Semgrep 0, Gitleaks nos nove escopos sem leaks, OSV backend/frontend sem vulnerabilidades, SpotBugs/FindSecBugs 0 achados e ZAP 0 FAIL/6 WARN; resultado final `REPROVADO` por `zap:alerts`. O gate padrão não executa ZAP automaticamente e informa `DAST: NÃO EXECUTADO` quando `-RunZap` não é fornecido. O token BRAPI histórico continua risco aberto, sem revogação confirmada.
ReexecuÃ§Ã£o anterior Ã  atualizaÃ§Ã£o do webjar, apÃ³s explicitar `frame-ancestors 'none'` e `form-action 'self'`: 13 URLs, 0 FAIL, 6 categorias WARN-NEW e 62 PASS. O alerta CSP de diretivas sem fallback foi eliminado; naquele momento permaneciam `unsafe-inline`, DOMPurify 3.2.6, polÃ­ticas COEP/CORP, timestamps e itens informativos. O resultado posterior estÃ¡ registrado abaixo.

## Tratamento dos alertas ZAP e DOMPurify — 2026-11-07

### Investigacao da cadeia Swagger UI

O dependency insight real confirmou `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0` trazendo `org.webjars:swagger-ui:5.30.1`. O bundle efetivamente servido continha `DOMPurify.version="3.2.6"`, exatamente a evidencia do alerta ZAP `10003 Vulnerable JS Library` (High, confidence Medium).

Foi comparado o artefato oficial `org.webjars:swagger-ui:5.34.1` antes da alteracao. Seu bundle contem `DOMPurify.version="3.4.17"`, acima das versoes corrigidas referenciadas pelo proprio alerta. A dependencia direta `org.webjars:swagger-ui:5.34.1` foi adicionada ao `build.gradle`, sem alterar Spring Boot, Java, Gradle, contratos REST, migrations ou regras financeiras. O dependency insight passou a selecionar `5.34.1` sobre a transitiva `5.30.1`.

### Maven Central / Gradle

O ambiente inicialmente apresentou `Permission denied: getsockopt` ao acessar o POM via `https://repo.maven.apache.org/maven2`. A investigacao confirmou Java 21 explicito, Gradle Wrapper 9.5.1, `mavenCentral()` via HTTPS, DNS e TCP/443 funcionais. Nao foram desabilitados TLS, verificacao de certificados ou proxy, nem foram instalados certificados desconhecidos. Com Java 21 e cache alternativo do projeto, a resolucao da dependencia `swagger-ui:5.34.1` foi concluida com sucesso.

### CSP, COEP e alertas informativos

O CSP permanece restritivo para o escopo atual, com `frame-ancestors 'none'` e `form-action 'self'`. `unsafe-inline` para script/style foi mantido porque o Swagger UI local depende dessa compatibilidade; sua remocao exige nonce/hash e validacao especifica. Os headers COOP, COEP `unsafe-none` e CORP `same-site` foram preservados; nao foi aplicado `require-corp` sem validacao de impacto nos recursos Swagger.

No novo scan, `Non-Storable Content` e `Modern Web Application` foram classificados como informativos; `Timestamp Disclosure - Unix` permanece baixo e restrito a JavaScript estatico do Swagger UI. Nenhum alerta foi suprimido genericamente.

### Regressao e nova evidencia DAST

- `./gradlew.bat clean test --console=plain`: aprovado.
- `./gradlew.bat build --console=plain`: aprovado, incluindo SpotBugs/FindSecBugs.
- `npm run build`: aprovado.
- `npm test -- --watch=false`: aprovado; 15 arquivos e 63 testes.
- ZAP `ghcr.io/zaproxy/zaproxy:2.17.0`: 13 URLs locais, `PASS 63`, `FAIL 0`, `WARN-NEW 5`, exit code 2 por alertas.
- O alerta DOMPurify foi eliminado pela atualizacao para Swagger UI 5.34.1.

Alertas residuais: `CSP: script-src unsafe-inline` (Medium), `Non-Storable Content` (Informational), `Timestamp Disclosure - Unix` (Low), `Modern Web Application` (Informational) e `Cross-Origin-Embedder-Policy Header Missing or Invalid` (Low). O gate continua fail-closed por `zap:alerts`; isso nao equivale a DAST nao executado.

O token BRAPI antigo continua exposto somente no historico Git conhecido, sem revogacao confirmada. Nao foi exibido, reutilizado ou revogado automaticamente.

O Security Gate final foi executado com backend local isolado e `-RunZap`: Semgrep 0, Gitleaks atual sem leaks nos escopos configurados, OSV backend/frontend 0, SpotBugs/FindSecBugs 0 e ZAP 63 PASS/0 FAIL/5 WARN. O gate terminou `REPROVADO` somente por `zap:alerts`; a excecao foi registrada de forma fail-closed.

## Triagem final dos alertas ZAP — Command 17 — 2026-11-07

Os tres artefatos reais (`zap-baseline.json`, `.xml` e `.html`) foram revisados. O JSON possui 7 entradas WARN em 5 IDs de regra: `10055` script/style CSP, `90004` COEP/CORP, `10096` timestamp, `10109` Modern Web Application e `10049` Non-Storable Content. O alerta `10003` de DOMPurify nao aparece mais.

Headers observados diretamente no backend local:

```text
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; form-action 'self'
Cross-Origin-Embedder-Policy: unsafe-none
Cross-Origin-Resource-Policy: same-site
Cross-Origin-Opener-Policy: same-origin
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
```

`unsafe-inline` aparece separadamente em `script-src` e `style-src`, somente na pagina Swagger. A politica nao foi relaxada com `unsafe-eval` nem origens genericas. Nonces/hashes e politica especifica por rota foram avaliados como trabalho futuro, pois exigem mudanca de renderizacao e validacao real do Swagger/Angular.

`COEP: unsafe-none` e `CORP: same-site` sao deliberados para compatibilidade dos recursos Swagger; `require-corp`/`same-origin` nao foram aplicados apenas para zerar o scanner. O alerta de cache decorre do `no-store` global do Spring Security e foi mantido para nao arriscar armazenamento de respostas autenticadas/financeiras. Os timestamps pertencem a constantes estaticas do bundle oficial; nao ha evidencia de metadados da aplicacao. `Modern Web Application` e somente informativo.

Foi criado `docs/security/zap-risk-acceptance.md` com uma proposta individual para cada entrada residual. Todos os itens estao como `PROPOSTO`, sem bypass implementado. O gate permanece bloqueante por `zap:alerts`. O token BRAPI historico continua risco aberto, sem revogacao confirmada.

## Correção final de CSP — Command 18 — 2026-11-07

O artefato real `swagger-ui:5.34.1` foi inspecionado: o `index.html` possui tres scripts externos e nenhum bloco de script inline. Foi removido somente `unsafe-inline` de `script-src`; `style-src 'unsafe-inline'` permanece por compatibilidade ainda nao comprovada do Swagger UI.

Validacao HTTP local: Swagger HTML, CSS, JavaScript e rota protegida retornaram normalmente; cada header apareceu uma vez, sem duplicidade ou conflito. O Swagger respondeu `200` e a rota financeira protegida respondeu `401` sem token.

Novo ZAP: 13 URLs, 63 PASS, 0 FAIL, 5 categorias WARN e 6 entradas. O alerta `10055 script-src unsafe-inline` foi eliminado. Permanecem `10055 style-src`, COEP/CORP, timestamps, Modern Web Application e Non-Storable Content. A ausencia de navegador funcional foi registrada como limitacao; nao foi possivel confirmar visualmente a renderizacao completa.

O Security Gate final com `-RunZap` executou Semgrep, Gitleaks atual, CycloneDX/OSV e SpotBugs/FindSecBugs sem achados bloqueantes. O resultado permaneceu `REPROVADO` por `zap:alerts`, como exigido pela politica fail-closed.

## Auditoria final de encerramento — Command 19

Foi criado `docs/security/module-11-final-audit.md`. A auditoria confirmou 36/46 tarefas concluídas, 10 pendentes, gate reprovado por `zap:alerts` e risco BRAPI histórico aberto. A recomendação técnica é não encerrar o Módulo 11 até concluir as validações de autenticação/autorização, obter decisões formais de risco e confirmar a revogação/rotação da credencial histórica.

## Cobertura adicional das tasks 1–3 — Command 20

Foram adicionados testes de segurança para assinatura JWT adulterada, escopo do `SecurityContext`, rotação/reuso/revogação de refresh token, CORS negativo, H2 em perfil deployment-like, isolamento CRUD de carteira/posição e armazenamento seguro no Angular.

Resultados reais: backend com 107 testes aprovados; frontend com 16 arquivos e 65 testes aprovados; `clean test`, `build` e `spotbugsMain --rerun-tasks` aprovados. As tasks 1.1–3.2 continuam pendentes porque a cobertura ainda não inclui concorrência de refresh, matriz local/HTTPS completa, cross-site para todos os endpoints mutáveis, 403 real e todos os recursos financeiros de vendas/proventos/indicadores/corretoras.

## Concorrência de refresh token — Command 22 — 2026-11-08

Antes da implementação, `git status`, `git diff`, os testes e a implementação foram revisados. Não havia alteração efetiva atribuível ao Command 21 nem teste concorrente existente. A implementação anterior fazia leitura, validação e marcação de `usado` sem bloqueio pessimista, permitindo uma condição de corrida entre transações concorrentes.

Correção aplicada: `RefreshTokenRepository.findByTokenHashForUpdate` usa `@Lock(PESSIMISTIC_WRITE)` e consulta JPQL explícita; `AuthService.refresh` passou a utilizá-la. A transação existente foi preservada. Não foram alteradas migrations, contratos REST, regras financeiras ou o Security Gate.

Evidência: `RefreshTokenConcurrencyTest` usa banco H2, MockMvc real, `ExecutorService` e `CountDownLatch`. Em cada cenário, duas requisições usam o mesmo refresh token: uma retorna 200, a outra 400; a persistência contém o token anterior inutilizado e exatamente um sucessor válido. A classe executou 10 repetições JUnit em cada uma de 3 execuções externas, totalizando 30 cenários, sem falhas.

Resultados finais deste comando: `./gradlew.bat clean test --console=plain` aprovado, 117 testes backend, 0 falhas e 1 ignorado; `./gradlew.bat build --console=plain` aprovado, incluindo `spotbugsTest` e `spotbugsMain`. A task OpenSpec 1.2 foi marcada como concluída; as demais tasks não foram alteradas. OpenSpec strict foi validada. O ZAP/Security Gate não foi executado novamente por escopo; seus bloqueios anteriores permanecem. O risco do token BRAPI histórico continua aberto, sem revogação confirmada.
## Testes de cookies e sessão — Command 23 — 2026-11-08

Foi concluída a validação exclusiva da task 1.3. Nenhuma alteração de produção foi necessária: a configuração já era ambiente-aware e o `AuthController` já emitia/limpava `HttpOnly`, `Secure`, `SameSite`, Path e Max-Age de forma consistente. A correção concorrente da task 1.2 permaneceu intacta.

`RefreshCookieLocalSecurityTest` validou o perfil local HTTP (`Secure=false`, `SameSite=Lax`), login, refresh, logout, rotação, limpeza, ausência, adulteração e expiração. `RefreshCookieDeploymentSecurityTest` validou o perfil deployment-like (`Secure=true`, `SameSite=Strict`) com `X-Forwarded-Proto=https`; a segurança continua explícita por configuração, sem confiar nesse cabeçalho para habilitar o cookie.

No Angular, `auth.service.spec.ts` validou refresh com `withCredentials=true`, ausência de refresh token em `localStorage`/`sessionStorage` e limpeza após 401 no `restore()`.

Resultados: `clean test`, `build` e `spotbugsMain --rerun-tasks` aprovados; backend com 120 testes, 0 falhas e 1 ignorado. O único ignorado é `PostgreSqlSmokeTest`, condicionado por `@EnabledIfSystemProperty` para `smoke.db.url`; não havia PostgreSQL de smoke configurado. Frontend: build aprovado e 16 arquivos/67 testes aprovados. OpenSpec strict foi aprovada e somente a task 1.3 foi marcada como concluída.

A fronteira CSRF/cross-site permanece na task 2.2. Não houve alteração do Security Gate, aprovação de riscos ZAP, commit, push ou uso de credenciais reais.

## Validação completa de CORS — Command 24 — 2026-11-08

Foi concluída exclusivamente a task 2.1. A configuração efetiva usa `allowedOrigins` exatos (`http://localhost:4200` e `http://127.0.0.1:4200`), métodos explícitos (`GET,POST,PUT,DELETE,OPTIONS`), headers explícitos (`Authorization,Content-Type,Accept,X-Requested-With`), `allowCredentials=true` e `maxAge=3600`. Não há wildcard, reflexão dinâmica de origem ou headers expostos.

`CorsComprehensiveSecurityTest` adicionou 9 testes MockMvc cobrindo preflight permitido/negado, origem ausente, origem não permitida, método/header não permitido, credenciais, ausência de credenciais, porta/protocolo diferentes, `Origin: null`, wildcard e requisições reais GET/POST/PUT/DELETE. O teste Bearer confirmou que CORS permitido não substitui autenticação: sem token houve 401; com token válido houve 200.

Matriz observada: preflight válido retornou 200 com `Access-Control-Allow-Origin` exato, `Access-Control-Allow-Methods`, `Access-Control-Allow-Headers`, `Access-Control-Allow-Credentials: true` e `Vary: Origin`; preflight inválido retornou 403 sem concessão; origem ausente não recebeu headers CORS; origens proibidas não foram refletidas; nenhuma resposta retornou `Access-Control-Allow-Origin: *`.

O Angular mantém API relativa `/api/v1`, `withCredentials` e `Authorization` Bearer no interceptor. Não foi necessária alteração de arquitetura ou configuração.

Resultados reais: `clean test`, `build` e `spotbugsMain --rerun-tasks` aprovados; backend com 129 testes, 0 falhas e 1 ignorado. O ignorado é `PostgreSqlSmokeTest`, condicionado por `smoke.db.url`, sem banco PostgreSQL de smoke configurado. Frontend: build aprovado e 16 arquivos/67 testes aprovados. OpenSpec strict aprovado; somente a task 2.1 foi marcada. O Security Gate não foi alterado e os riscos ZAP/BRAPI permanecem conforme documentação anterior.
## CSRF boundary audit - Command 25 - 2026-11-08

Task 2.2 was validated exclusively. The existing stateless design keeps CSRF disabled because financial endpoints require Authorization Bearer; refresh_token is accepted only by refresh/logout. No CORS expansion, migration, financial rule, JWT or Security Gate change was made.

Evidence matrix:

| Area | Cross-site vector | Observed protection | Evidence |
|---|---|---|---|
| Financial endpoints | Cookie without Bearer | Cookie does not create authentication | GET/POST/PUT/DELETE rejected; 401 without Origin and 403 for untrusted/null Origin |
| Financial endpoints | Valid Bearer | JWT filter authenticates per request | GET 200 and POST 201 from allowed origin |
| Refresh/logout | Cookie plus untrusted/null Origin | CORS rejects the origin; no financial context is created | 403 without CORS grant; success only from allowed origin |
| Login/content type | Form/plain/JSON | Invalid media type is rejected without a cookie | 415 generic response; untrusted JSON Origin gets 403 |
| Preflight | Origin, method and requested headers | Exact CORS policy | 200 only for configured origin/method/headers |

CsrfBoundarySecurityTest uses real MockMvc and H2 and covers allowed, untrusted, absent and null Origin, cookie with/without Bearer, JSON, form, text/plain, refresh, logout and preflight. The evidence separates CORS from authentication: an untrusted Origin is rejected by the server CORS filter, while an Origin-less request without Bearer reaches authentication and receives 401. No financial endpoint is authorized solely by cookie.

The only production correction was in GlobalExceptionHandler: HttpMediaTypeNotSupportedException previously fell through to 500 and now returns a generic 415 response, with no stack trace or credential disclosure.

Results: clean test, build and spotbugsMain --rerun-tasks passed; backend 134 tests, 0 failures, 0 errors and 1 skipped PostgreSqlSmokeTest because smoke.db.url was absent. Frontend build and 67 tests in 16 files passed. OpenSpec strict passed; only task 2.2 was changed. The historical BRAPI token remains an open risk without confirmed revocation.
## Recuperacao Gradle e validacao JWT - Command 28 - 2026-11-08

Java 21.0.12.1 foi confirmado, mas `JAVA_HOME` persistia em JDK 17; a sessao foi corrigida para o JDK 21. O Wrapper e Gradle 9.5.1. Nao existe `gradle.properties`; o build usa `mavenCentral()` e nao teve dependencias alteradas.

O cache global `C:\.gradle` e o caminho de usuario sugerido nao sao gravaveis nesta sessao. Daemons Gradle dos caches do projeto foram parados. O `byte-buddy-agent-1.17.8.jar` foi removido somente do artefato isolado apos confirmar a parada dos daemons, restaurado de cache local com hash identico, mas a rede permaneceu bloqueada (`Permission denied: getsockopt`) e o cache nao reconheceu a copia como resolucao offline valida.

Os nove testes JWT nao executaram; `compileTestJava` falhou por resolucao do Byte Buddy. A regressao completa nao foi executada nem declarada. A task 1.1 continua pendente, OpenSpec strict permanece valido em 41/46, e o token BRAPI historico continua sem revogacao confirmada.

## Validacao final de JWT e SecurityContext - Command 27 - 2026-11-08

Foram adicionados testes focados na task 1.1: `JwtContextSecurityTest` cobre Bearer valido, ausencia, vazio, expirado, adulterado, malformado, `alg=none`, refresh como access token, usuario inexistente/desativado, nao eco da credencial e isolamento concorrente; `AuthTokenServiceSecurityTest` cobre segredo curto, placeholders e claims incompativeis. Foram implementados 9 novos casos, sem alterar producao ou remover testes.

A task nao foi marcada porque a execucao nova foi bloqueada antes dos testes: cache global sem permissao, dependencia `swagger-ui:5.34.1` sem resolucao por rede/TLS e `AccessDeniedException` em `byte-buddy-agent-1.17.8.jar`. Resultado anterior conhecido: backend 134 aprovados/1 ignorado e frontend 67 aprovados. OpenSpec strict valido, 41/46. Security Gate, ZAP e risco BRAPI historico permanecem inalterados.

## H2 console and profile isolation audit - Command 26 - 2026-11-08

Task 2.3 was completed without changing financial behavior or migrations. Effective configuration is local-only: `application-local.properties` enables the H2 console explicitly and sets `web-allow-others=false`; `application-test.properties` disables it; `application-dev.properties` now fixes `spring.h2.console.enabled=false` without a development environment override. `SecurityConfig` permits `/h2-console/**` only for `local` and applies `denyAll()` outside that profile.

| Profile | Expected | Observed evidence |
|---|---|---|
| `local` | Console enabled and locally reachable | `H2ConsoleSecurityTest` used an embedded HTTP port and received 2xx from `/h2-console/`; effective property was true |
| `dev` | Console disabled and unavailable | `H2ConsoleDeploymentSecurityTest` observed property false and non-public endpoint; no local profile was active |
| `test` | Console disabled | Existing test profile sets false; H2 is in-memory `create-drop` only |
| production-like/other | No local console authorization | Base configuration does not enable the console; non-local SecurityConfig denies the route |

The first strengthened test attempt exposed a MockMvc limitation: the H2 servlet is not mapped in the simulated servlet context. The final local evidence therefore uses the real embedded HTTP server, while the deployment-like test uses the effective property plus route denial. No real database, production port, credential or external integration was used.

Regression results: `./gradlew.bat clean test --console=plain` passed with 134 tests, 0 failures, 0 errors and 1 skipped PostgreSQL smoke test; `./gradlew.bat build --console=plain` passed; `./gradlew.bat spotbugsMain --rerun-tasks --console=plain` passed with no findings. Frontend `npm run build` passed and `npm test -- --watch=false` passed with 16 files and 67 tests. OpenSpec strict validation passed and only task 2.3 was marked complete. Security Gate, ZAP risks and the historical BRAPI credential status were not changed; BRAPI revocation remains unconfirmed.

## Validação final JWT — Command 35 — 2026-11-11

Foram corrigidas exclusivamente as duas falhas observadas na execução anterior. A validação de placeholders passou a rejeitar também valores com o prefixo `dev-only-change-me`, sem registrar ou substituir segredos. A expectativa do teste parametrizado foi corrigida para a mensagem UTF-8 correta da implementação: `AUTH_JWT_SECRET não pode usar um valor padrão ou de exemplo`.

Com Java 21.0.12.1, Gradle 9.5.1 e o cache `.gradle-user-home-dast-validation`, os testes focalizados passaram: `AuthTokenServiceSecurityTest` e `JwtContextSecurityTest`, 13 testes aprovados e 0 falhas. A regressão backend passou com `clean test`, `build` e `spotbugsMain --rerun-tasks`; o relatório final contém 143 testes, 0 falhas, 0 erros e 1 ignorado (`PostgreSqlSmokeTest`, condicionado à ausência de `smoke.db.url`). O frontend passou com `npm run build` e `npm test -- --watch=false`: 16 arquivos e 67 testes aprovados.

Os warnings de bundle budget e `DecimalPipe` não utilizado são não bloqueantes. OpenSpec strict foi validado e a task 1.1 foi marcada como concluída. Não houve alteração de migrations, regras financeiras, dependências, Security Gate ou aprovação de riscos ZAP; o risco histórico BRAPI continua aberto.

## Isolamento de recursos financeiros — Command 36 — 2026-11-12

Foi concluída exclusivamente a task 3.1. A revisão cobriu carteiras, posições, vendas/histórico, dashboard, evolução patrimonial, proventos e análise cambial. `CurrentUser.id()` é obtido do principal autenticado pelo JWT; consultas privadas usam o proprietário da carteira e relações carteira-posição, enquanto ações, histórico de mercado e corretoras permanecem catálogos globais não privados.

Matriz resumida: listagem de carteiras usa `findAllByUsuarioId`; leitura/alteração/exclusão usa `findByIdAndUsuarioId`; posições e vendas são alcançadas através de carteira pertencente ao usuário; dashboard, evolução, proventos e análise de moedas validam a carteira antes das agregações. O teste também cobriu paginação, filtros, consultas por ID e listagens sem registros de terceiros.

Foi corrigida uma falha de ownership em `AtivoCarteiraService`: atualização e exclusão agora validam a carteira do usuário antes de carregar a posição. `CarteiraService.excluir` também valida o proprietário antes da verificação de ativos, evitando diferença de resposta baseada em estado de carteira alheia. A correção não modifica cálculos financeiros, regras de compra/venda, migrations ou integrações.

Evidência: `FinancialOwnershipSecurityTest` executou com MockMvc/H2, dois usuários, JWT válido e recursos persistidos; acessos cruzados de leitura, alteração, exclusão, criação e venda foram rejeitados sem vazamento, enquanto a venda própria foi aprovada e os dados do segundo usuário permaneceram isolados.

Regressão executada: backend `clean test` — 144 testes, 0 falhas, 0 erros, 1 ignorado (`PostgreSqlSmokeTest` sem `smoke.db.url`); `build` — aprovado; `spotbugsMain --rerun-tasks` — aprovado sem findings. Frontend: build aprovado com warnings não bloqueantes e 67/67 testes em 16 arquivos aprovados. OpenSpec strict aprovado em 43/46, com somente a task 3.1 adicionalmente concluída. Riscos residuais: tasks 3.2, 6.4 e 7.5, risco histórico BRAPI e alertas ZAP continuam sem aprovação.

## Testes negativos e prevenção de vazamento — Command 37 — 2026-11-12

Foi concluída exclusivamente a task 3.2. `NegativeAuthorizationSecurityTest` usa MockMvc/H2, JWT real e bancos de teste isolados. Os cenários cobrem login válido e inválido, ausência de credencial, JWT inválido/expirado, 401, 403 de CORS, 404 de IDs de terceiros/inexistentes, leitura de agregações, paginação, criação, alteração, exclusão, simulação e confirmação de venda.

As respostas negativas não contêm nomes privados, IDs sensíveis, valores financeiros, tokens, refresh tokens, segredos, stack traces ou nomes de classes Java. A semântica observada preserva 401 para autenticação ausente/inválida, 403 para origem não autorizada e 404 para recursos privados inexistentes ou pertencentes a terceiros. A autorização permanece no serviço/backend antes da consulta ou mutação.

Nenhuma falha de autorização foi encontrada. A única falha durante a primeira regressão foi de isolamento de contexto H2 (`ATIVO_CARTEIRA` ausente por encerramento de banco compartilhado); foi corrigida somente na configuração das suítes de teste com URLs H2 nomeadas. Não houve alteração de migrations, JWT, fórmulas, regras financeiras, Security Gate, integrações ou riscos ZAP.

Resultados reais: backend `clean test` — 148 testes, 0 falhas, 0 erros, 1 ignorado; `build` — aprovado; `spotbugsMain --rerun-tasks` — aprovado sem findings. Frontend `npm run build` — aprovado com warnings não bloqueantes; `npm test -- --watch=false` — 16 arquivos e 67/67 testes aprovados. OpenSpec strict — aprovado em 44/46. Riscos residuais: tasks 6.4 e 7.5, risco histórico BRAPI e alertas ZAP permanecem abertos.

## Auditoria e preparação da rotação BRAPI — Command 38 — 2026-11-12

`integrations.brapi.token` usa exclusivamente a referência externa `BRAPI_TOKEN`, sem valor padrão literal em `application.properties`. A variável estava ausente na sessão. `.env.example` está vazio para essa variável; `docker-compose.yml` também exige fornecimento externo; o arquivo `env.exemple` contém apenas texto de exemplo. Não há credencial atual no código/configuração efetiva auditada.

Os adapters BRAPI não registram o token. A cobertura existente valida token ausente como indisponível e respostas do provedor como falha normalizada; foi adicionado um caso com token fictício que confirma header de autenticação e garante que uma resposta 401 não ecoa a credencial. Não foram usados tokens reais nem chamadas externas.

O identificador histórico solicitado `b8a7c34c41c9336e8365b3203751f162b0ecd96` não é alcançável no banco de objetos local. O commit relacionado disponível localmente (`ab8a7c34...`) contém, de forma sanitizada, uma atribuição histórica não vazia de `BRAPI_TOKEN` no `.gitignore`. Isso confirma a natureza da exposição sem exibir o valor; nenhum histórico foi reescrito.

Ação manual obrigatória: revogar a credencial antiga no provedor BRAPI, emitir substituta pelo fluxo oficial, configurar `BRAPI_TOKEN` em secret manager/ambiente, reiniciar e validar a integração sem registrar o segredo. Essa ação não foi executada por depender de autorização e acesso externo. Portanto, a task 6.4 continua pendente.

Regressão após o teste: backend `clean test` — 149 testes, 0 falhas, 0 erros, 1 ignorado; `build` — aprovado; `spotbugsMain --rerun-tasks` — aprovado sem findings. Frontend permanece em 67/67 testes aprovados. OpenSpec strict permanece em 44/46, com somente 6.4 e 7.5 pendentes. Security Gate, migrations, regras financeiras e riscos ZAP não foram alterados.

## Validação da rotação BRAPI — Command 39 — 2026-11-12

Resultado da verificação local: `BRAPI_TOKEN=ABSENT`; nenhum valor foi impresso. A configuração efetiva continua externalizada e sem credencial literal. O `.gitignore` contém proteção para arquivos de ambiente/segredos. Não foi possível executar consulta autenticada real à BRAPI nesta sessão sem um substituto configurado.

Não houve reutilização do token antigo nem ação na conta BRAPI. A validação sanitizada anterior cobriu token fictício, header de autenticação, resposta 401 e ausência de eco em mensagens. A evidência necessária de revogação deve ser obtida manualmente no provedor, sem compartilhar o segredo: confirmação de revogação/estado inativo da credencial antiga e confirmação de que a nova foi instalada no secret manager/ambiente de execução.

Regressão realizada após a auditoria: `clean test` — 149 testes, 0 falhas, 0 erros, 1 ignorado; `build` — aprovado; `spotbugsMain --rerun-tasks` — aprovado sem findings. A task 6.4 permanece pendente, OpenSpec permanece 44/46 e a task 7.5 não foi iniciada.

## Revisão da exceção CSRF e FindSecBugs — Command 40 — 2026-11-12

A task 7.5 foi revisada sem alteração de código de produção. O fluxo vigente não possui autenticação por cookie para recursos financeiros: `Authorization: Bearer` é obrigatório nas mutações; o cookie HttpOnly participa apenas de refresh/logout. Os testes existentes cobrem Origin ausente, Origin nulo, origem não autorizada, cross-site, métodos mutáveis, cookie-only e Bearer válido. Portanto, a exceção `SPRING_CSRF_PROTECTION_DISABLED` continua limitada ao modelo atual e não deve ser interpretada como proteção CSRF geral.

FindSecBugs 1.14.0 está integrado ao SpotBugs 4.10.5, com execução real e relatório atual sem findings emitidos. O detector `CorsRegistryCORSDetector` continua omitido de forma específica porque sua análise de `CorsConfig` causa exceção interna reproduzida. Não foi validada uma versão alternativa compatível no projeto; não foram alteradas versões ou criadas tarefas duplicadas. A condição de reavaliação está registrada: qualquer endpoint que passe a autorizar estado financeiro via cookie exige proteção CSRF explícita; uma versão futura do detector só substituirá o workaround após execução compatível comprovada.

Nenhum alerta ZAP foi aprovado, nenhum Security Gate foi alterado e nenhuma migration/regra financeira/JWT foi modificada. Task 7.5 concluída; task 6.4 permanece pendente por falta de comprovação externa de revogação BRAPI. OpenSpec: 45/46.

## Verificação final da task 6.4 — Command 41 — 2026-11-12

A configuração continua externalizada por `BRAPI_TOKEN`. Foi encontrado `.env` local ignorado pelo Git com valor não vazio, sem revelar o conteúdo; contudo não há processo backend ativo nesta sessão e não existe evidência de que esse valor seja o substituto novo. Não foi acessado o provedor nem reutilizado o token histórico.

Conclusão: a configuração local está disponível, mas faltam confirmação não sensível de revogação no provedor BRAPI e vínculo verificável do valor local à nova credencial. A task 6.4 permanece pendente. OpenSpec permanece em 45/46; nenhuma auditoria adicional, alteração de integração ou regressão foi executada nesta revisão.
