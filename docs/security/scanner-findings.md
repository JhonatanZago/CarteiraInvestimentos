# Achados de SAST e segredos — Módulo 11

Data da execução: 2026-11-04  
Escopo: backend Java, frontend Angular, configurações, documentação e histórico Git.  
Os valores detectados foram redigidos; este documento não contém credenciais.

## Limitação da referência

`tutorial/modulo-11-seguranca.md` não existe na cópia local. As seções 11, 12 e 16 foram consultadas na versão oficial remota do repositório do professor. A conformidade aqui registrada é limitada aos comandos e escopo efetivamente executados neste ambiente.

## Semgrep

- Versão: `1.178.0`, executada por `uvx`.
- Regras: `p/java`, `p/secrets`, `p/owasp-top-ten` e a regra customizada do projeto.
- Opção obrigatória: `--metrics=off`.
- Backend: 142 arquivos, 101 regras, 0 achados.
- Angular: 79 arquivos, 115 regras, 0 achados.
- Scan combinado com regra customizada: 221 arquivos, 174 regras, 0 achados.
- Regra customizada: `controller-returns-jpa-entity`, com 1/1 teste Semgrep aprovado.
- Relatórios: `security/reports/semgrep-backend.sarif`, `semgrep-frontend.sarif` e `semgrep-all.sarif`.

A regra customizada procura controllers Spring que retornem diretamente entidades JPA conhecidas. Nenhum controller atual foi classificado como exposição confirmada; os controllers analisados usam DTOs/respostas dedicadas.

## SpotBugs e FindSecBugs - 2026-11-05

- Plugin Gradle: `com.github.spotbugs` `6.5.13`.
- Engine SpotBugs: `4.10.5`.
- Plugin FindSecBugs: `com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0`.
- Escopo: bytecode do source set `main`, com filtro de categoria `SECURITY`.
- Comando: `./gradlew.bat spotbugsMain`.
- Relatorios: `build/reports/spotbugs/main/spotbugs.html` e `build/reports/spotbugs/main/spotbugs.xml`.

O detector FindSecBugs foi confirmado na execucao: a primeira analise identificou `SPRING_CSRF_PROTECTION_DISABLED` em `SecurityConfig`. O detector `CorsRegistryCORSDetector` da versao 1.14.0 apresentou uma excecao interna ao analisar `CorsConfig`; ele foi omitido de forma especifica no Gradle para permitir a analise restante. Nao houve supressao global de achados.

| Detector | Severidade original | Local | Classificacao | Tratamento |
|---|---:|---|---|---|
| `XSS_SERVLET` | media (prioridade 2) | `SecurityConfig.jsonError` | corrigido | Resposta serializada pelo `ObjectMapper` e escrita como bytes UTF-8. |
| `SPRING_CSRF_PROTECTION_DISABLED` | alta (prioridade 1) | `SecurityConfig.security` | excecao arquitetural documentada | Bearer token e obrigatorio para mutacoes financeiras; refresh cookie nao autoriza esses endpoints. Teste: `SecurityRegressionTest.cookieAloneCannotMutateFinancialResource`. |

Resultado final do relatorio SpotBugs: 0 achados emitidos apos a correcao e a excecao especifica documentada. O achado CSRF original permanece rastreavel na configuracao e nao deve ser interpretado como protecao CSRF universal.

Validacao final: `spotbugsMain` aprovado, `clean test` aprovado (100 testes, 0 falhas, 1 ignorado), `build` aprovado e `scripts/security-gate.ps1` aprovado. O gate atual nao analisa o historico Git; o historico Gitleaks permanece com 1 achado BRAPI ja documentado.

O detector de CORS nao foi executado por causa do crash conhecido dessa versao do FindSecBugs; a configuracao CORS continua coberta pelos testes Spring e pelos scans Semgrep. Essa e uma limitacao residual da ferramenta.

## Gitleaks

- Versão: imagem Docker `zricethezav/gitleaks:v8.30.1`.
- Todas as execuções usaram `--redact`.
- Código Java (`src`): 0 achados.
- Código Angular (`frontend/src`): 0 achados.
- Documentação (`docs`): 0 achados.
- Configurações selecionadas (`env.exemple`, `.env.example`, `docker-compose.yml`, `build.gradle`): 0 achados.
- `.gitignore`: 1 achado `generic-api-key` na linha 56.
- Histórico Git: 1 achado `generic-api-key` em `.gitignore`, associado ao commit `ab8a7c34c41c9336e8365b3203751f162b0ecd96`.
- Relatórios: arquivos `security/reports/gitleaks-*.sarif` correspondentes a cada escopo.

### Achado confirmado para investigação

O `.gitignore` rastreado contém um valor com formato de token de API na linha 56. O mesmo valor aparece no histórico Git. O valor foi deliberadamente omitido deste relatório e dos logs apresentados.

Classificação provisória: **alto — possível credencial exposta**, aguardando confirmação do proprietário do serviço. A validade do token não foi testada e nenhum serviço externo foi consultado.

Controles recomendados:

1. Revogar/rotacionar o token junto ao provedor, antes de qualquer limpeza histórica.
2. Remover o valor do arquivo rastreado e manter somente a variável sem valor ou referência a configuração externa.
3. Avaliar limpeza do histórico somente após autorização explícita, com backup e plano de comunicação.
4. Verificar se o token foi reutilizado em outros ambientes.
5. Manter Gitleaks no portão para impedir reincidência.

Não foram aplicadas essas ações porque revogação, remoção e reescrita de histórico alteram estado externo ou histórico Git e exigem autorização específica.

### Contexto não versionado e artefatos

O `.env` local, ignorado pelo Git, contém variáveis não vazias com nomes de credenciais. Ele foi incluído na varredura ampla e os valores foram redigidos. Deve ser tratado como potencial segredo operacional e rotacionado se tiver sido compartilhado ou reutilizado.

A varredura ampla do worktree também encontrou falsos positivos em relatórios de testes gerados, devido a senhas aleatórias de desenvolvimento, e erros de leitura em locks do cache Gradle. Esses artefatos não fazem parte do escopo versionado de código/configuração e foram separados das conclusões acima.

## Comandos reproduzíveis

PowerShell, a partir da raiz:

```powershell
$env:PYTHONUTF8 = '1'
uvx semgrep==1.178.0 scan --metrics=off `
  --config p/java --config p/secrets --config p/owasp-top-ten `
  --config semgrep-rules/ --error --sarif `
  --output security/reports/semgrep-all.sarif src/ frontend/src/

uvx semgrep==1.178.0 --metrics=off --test semgrep-rules/

docker run --rm -v "${PWD}:/repo" `
  zricethezav/gitleaks:v8.30.1 dir /repo/src --no-banner --redact `
  --report-format sarif --report-path /repo/security/reports/gitleaks-src.sarif

docker run --rm -v "${PWD}:/repo" `
  zricethezav/gitleaks:v8.30.1 git /repo --no-banner --redact `
  --report-format sarif --report-path /repo/security/reports/gitleaks-history.sarif
```

O histórico deve ser analisado separadamente. Não executar limpeza automática, `git filter-repo`, reset ou push como parte do scanner.

## Investigação histórica do achado — 2026-11-04

- Categoria: `generic-api-key`.
- Arquivo atual: `.gitignore`, linha 56.
- Variável associada: `BRAPI_TOKEN`.
- Serviço provável: BRAPI, porque o projeto usa `BRAPI_TOKEN` para a integração configurada em `brapi.dev`.
- Ocorrências no worktree relevante: somente `.gitignore`; o valor não foi exibido.
- Arquivos de exemplo e documentação usam placeholders vazios ou marcadores, sem o valor detectado.
- Branches locais: `main`.
- Referências remotas locais: `origin/main`.
- Branches disponíveis no remoto consultado: somente `main`.
- Histórico: o valor aparece no commit `ab8a7c34c41c9336e8365b3203751f162b0ecd96`, em `.gitignore`.

Classificação: **possível segredo não confirmado**, tratado operacionalmente como comprometido até que o proprietário confirme o contrário. A validade da credencial não foi testada, não houve acesso à API BRAPI e não foi declarada revogação.

O arquivo e o histórico não foram modificados. A correção depende de autorização para o proprietário revogar/rotacionar a credencial; depois disso, o valor deve ser removido do `.gitignore` e a limpeza histórica deve ser avaliada separadamente.

## Remediação do worktree — 2026-11-04

Conforme a autorização operacional informada, o valor sensível foi removido do `.gitignore`. As regras existentes foram preservadas, incluindo `.env` e `.env.local`. Nenhum novo token foi adicionado.

A configuração continua externa em `src/main/resources/application.properties`:

```properties
integrations.brapi.token=${BRAPI_TOKEN:}
```

Os adapters continuam mantendo o token no backend, enviando-o somente no header da chamada ao provedor quando configurado. Testes usam mocks e não acessaram a BRAPI real.

Resultados após a remoção:

- Security Gate: aprovado.
- Semgrep: 0 achados.
- Gitleaks nos escopos atuais: 0 achados.
- Gitleaks no histórico: 1 achado no commit antigo `ab8a7c34c41c9336e8365b3203751f162b0ecd96`.

A remoção do worktree foi concluída, mas o risco histórico não foi resolvido. O estado de revogação do provedor não foi verificado diretamente; ele é uma premissa operacional informada pelo solicitante.
## Reexecução dos scanners em 2026-11-05

- SpotBugs/FindSecBugs: `./gradlew.bat spotbugsMain --rerun-tasks` aprovado; HTML/XML gerados, `0` achados emitidos e `0` erros de análise. O XML confirma o plugin `com.h3xstream.findsecbugs` habilitado.
- Semgrep no gate: aprovado, `0` achados.
- Gitleaks no gate: não executado com sucesso; os nove comandos falharam porque o Docker Desktop não estava disponível (`dockerDesktopLinuxEngine` ausente). O gate terminou `REPROVADO` e registrou cada falha, sem mascará-la como achado zero.
- Gitleaks histórico previamente preservado: `1` achado `generic-api-key`, associado ao token BRAPI antigo; a credencial continua sem revogação confirmada.

Esta execução não altera a triagem histórica: o valor não foi exibido, testado, revogado ou removido do histórico.
## Revalidação de ambiente em 2026-11-06

Gradle foi executado com Java 21 explícito e cache alternativo temporário:

| Comando | Resultado |
|---|---|
| `./gradlew.bat clean test --console=plain` | APROVADO |
| `./gradlew.bat build --console=plain` | APROVADO |
| `./gradlew.bat spotbugsMain --rerun-tasks --console=plain` | APROVADO |
| `docker version` / `docker info` | REPROVADOS: cliente presente, Engine indisponível e named pipe ausente |
| `scripts/security-gate.ps1` | REPROVADO fail-closed por falha de execução dos escopos Gitleaks; Semgrep 0 e SpotBugs/FindSecBugs aprovados |

O Gitleaks atual não foi considerado limpo nesta revalidação. O achado histórico BRAPI permanece aberto, sem exibição do valor e sem revogação confirmada.
## CycloneDX e OSV-Scanner — 2026-11-07

O tutorial `tutorial/modulo-11-seguranca.md` nÃ£o estava presente; foram usadas documentaÃ§Ãµes oficiais e a configuraÃ§Ã£o real do projeto. CycloneDX Gradle `3.5.1` gerou o SBOM backend com 109 componentes. `@cyclonedx/cyclonedx-npm` `6.0.1` gerou o SBOM frontend com 336 componentes. OSV-Scanner oficial `v2.7.0`, com checksum verificado, gerou os relatÃ³rios JSON correspondentes.

| Escopo | Raw | IDs Ãºnicos | CrÃ­ticas | Altas | Moderadas | Baixas |
|---|---:|---:|---:|---:|---:|---:|
| Backend | 18 | 11 | 4 | 10 | 4 | 0 |
| Frontend | 42 | 42 | 2 | 13 | 24 | 3 |

Backend: os quatro crÃ­ticos estÃ£o no Tomcat embed `11.0.24`, transitivo do Spring Boot `4.0.8`, com correÃ§Ã£o indicada em `11.0.25`. Jackson 2.21.5 e `tools.jackson` 3.1.5 tambÃ©m foram confirmados como componentes transitivos/gerenciados. IDs crÃ­ticos: `GHSA-9xv2-5v5q-p794`, `GHSA-f525-44xv-f2qj`, `GHSA-gcx9-497g-6cp6`, `GHSA-h3x4-894j-xpx5`.

Frontend: `@angular/router 22.1.3` Ã© dependÃªncia direta e possui achados altos. Os crÃ­ticos incluem `piscina 5.2.0` e `proxy-addr 2.0.7`; os demais sÃ£o majoritariamente transitivos ou de tooling. Os achados sÃ£o vulnerabilidades reportadas pelo OSV, nÃ£o prova de exploraÃ§Ã£o. AtualizaÃ§Ãµes de Spring Boot/Angular e do grafo transitivo ficam pendentes de compatibilidade e autorizaÃ§Ã£o.

O gate integrado executou Semgrep, Gitleaks, CycloneDX, OSV e SpotBugs/FindSecBugs. Semgrep e SpotBugs passaram; Gitleaks ficou nÃ£o verificado por indisponibilidade do Docker e OSV bloqueou pelos achados. A credencial BRAPI histÃ³rica continua aberta, sem exibiÃ§Ã£o ou revogaÃ§Ã£o.
## ReanÃ¡lise OSV apÃ³s correÃ§Ã£o de dependÃªncias — 2026-11-07

CorreÃ§Ãµes aplicadas:

| DependÃªncia/grafo | VersÃ£o anterior | VersÃ£o nova | Vulnerabilidade | Severidade | Status |
|---|---|---|---|---|---|
| Spring Boot managed Tomcat | 11.0.24 | 11.0.27 via Spring Boot 4.0.9 | 4 GHSA crÃ­ticos do Tomcat embed | CRITICAL | Corrigido; OSV nÃ£o retornou resultados |
| Jackson 2.x | 2.21.5 | 2.21.7 via Spring Boot 4.0.9 | GHSA de core/databind | HIGH/MODERATE | Corrigido; OSV nÃ£o retornou resultados |
| Jackson 3.x | 3.1.5 | 3.1.7 via Spring Boot 4.0.9 | GHSA de core/databind | HIGH/MODERATE | Corrigido; OSV nÃ£o retornou resultados |
| Angular | 22.1.x/22.1.3 | 22.2.5; build/CLI 22.2.6 | GHSA do `@angular/router` | HIGH | Corrigido; OSV nÃ£o retornou resultados |
| Vitest | 4.1.11 | 4.1.12 | GHSA do `@vitest/mocker` | MODERATE | Corrigido; OSV nÃ£o retornou resultados |
| Tooling transitivo | versões anteriores | grafo regenerado por npm | `piscina`, `proxy-addr`, Vite, Hono, qs e demais IDs OSV | CRITICAL/HIGH/MODERATE/LOW | Corrigido no grafo atual; OSV nÃ£o retornou resultados |

Comparativo real: backend passou de 18 ocorrÃªncias/11 IDs para zero; frontend passou de 42 vulnerabilidades/371 pacotes lidos para zero/308 pacotes lidos. Os SBOMs atuais possuem 109 componentes backend e 272 frontend.

`npm ci` foi executado normalmente apÃ³s a regeneraÃ§Ã£o limpa do lockfile e passou. Uma tentativa intermediÃ¡ria de resolver o lockfile com `--legacy-peer-deps` foi usada apenas para diagnosticar/regenerar a Ã¡rvore; nÃ£o foi usado `--force` nem `npm audit fix`. O lockfile final foi validado por `npm ci` normal.

O gate final executou Semgrep, CycloneDX, OSV e SpotBugs/FindSecBugs com sucesso. Gitleaks permanece nÃ£o verificÃ¡vel porque o Docker Engine nÃ£o estava disponÃ­vel; o gate corretamente terminou reprovado. O token BRAPI antigo continua exposto no histÃ³rico Git, sem revogaÃ§Ã£o confirmada.
## OWASP ZAP / DAST — preparação em 2026-11-07

Foi criado `scripts/run-zap-baseline.ps1` para usar `ghcr.io/zaproxy/zaproxy:2.17.0` contra um alvo local explícito. O alvo padrão é `http://host.docker.internal:8081/swagger-ui/index.html`, adequado ao Docker Desktop no Windows quando o backend estiver escutando em `8081`. O script gera `zap-baseline.html`, `zap-baseline.json` e `zap-baseline.xml` em `security/reports/` e rejeita hosts externos, URLs com credenciais e esquemas diferentes de HTTP.

O Baseline Scan não foi executado: o Docker Engine falhou no named pipe `docker_engine` e não havia backend escutando na porta `8081`. Não existem alertas ZAP nesta execução. O estado correto é **DAST não executado por falha de infraestrutura**, não “0 alertas”.

Escopo planejado, quando o ambiente estiver disponível: Swagger/OpenAPI e endpoints locais de autenticação, respostas HTTP, cabeçalhos, CORS e exposição de informações, usando H2/test profile isolado, dados fictícios e sem chamadas a integrações externas. Não serão usados tokens JWT reais ou o segredo BRAPI.
## OWASP ZAP / DAST — resultado e triagem em 2026-11-07

Ambiente: Docker Desktop `4.88.1` / Engine `29.7.2`; backend Spring Boot `4.0.9`, Java 21, H2 em memÃ³ria e profile `dev`. O alvo foi exclusivamente `http://host.docker.internal:8081/swagger-ui/index.html`; nenhum domÃ­nio externo, produÃ§Ã£o, dado real ou credencial real foi usado. O Baseline Scan `ghcr.io/zaproxy/zaproxy:2.17.0` cobriu 13 URLs e produziu HTML, JSON e XML.

| Categoria | Severidade | EvidÃªncia | AvaliaÃ§Ã£o |
|---|---|---|---|
| `10003 Vulnerable JS Library` | High / Medium confidence | DOMPurify `3.2.6` em `/swagger-ui/swagger-ui-bundle.js`; ZAP listou CVEs/GHSAs no JSON | Achado de dependÃªncia do Swagger UI transitivo; nÃ£o Ã© evidÃªncia de exploraÃ§Ã£o dos endpoints financeiros. AtualizaÃ§Ã£o `swagger-ui 5.34.1` ficou pendente por falha de resoluÃ§Ã£o Gradle/Maven Central. |
| `10055 CSP` | Medium / High confidence | `frame-ancestors`/`form-action` sem fallback e `unsafe-inline` | CSP foi adicionado. `unsafe-inline` permanece por compatibilidade com Swagger UI; remoÃ§Ã£o exige nonce/hash e validaÃ§Ã£o adicional. |
| `90004 COEP/CORP` | Low / Medium confidence | Recursos Swagger sem polÃ­tica aceita pelo detector | Headers COOP/COEP/CORP foram adicionados, mas o detector ainda reporta parte dos recursos; `require-corp` nÃ£o foi aplicado para preservar compatibilidade cross-origin. |
| `10096 Timestamp Disclosure` | Low / Low confidence | Timestamps em `swagger-ui-standalone-preset.js` | Falso positivo/baixo impacto em biblioteca estÃ¡tica; sem informaÃ§Ã£o sensÃ­vel do projeto. |
| `10049 Non-Storable Content` | Informational | `Cache-Control: no-store` em respostas 401 e recursos Swagger | Controle conservador, nÃ£o vulnerabilidade. |
| `10109 Modern Web Application` | Informational | Swagger UI | Informativo. |

Antes dos headers: 7 categorias WARN-NEW, 0 FAIL, 61 PASS. Depois: 6 categorias WARN-NEW, 0 FAIL, 62 PASS. O DAST foi executado de fato; portanto nÃ£o deve ser registrado como “0 alertas”.

O gate explÃ­cito `scripts/security-gate.ps1 -RunZap` confirmou Semgrep 0, Gitleaks atual sem leaks nos escopos, OSV 0, SpotBugs/FindSecBugs 0 e ZAP com WARNs. O resultado foi `REPROVADO` por `zap:alerts`, sem supressÃ£o genÃ©rica. O token BRAPI histÃ³rico continua pendente e sem revogaÃ§Ã£o confirmada.
Na reexecuÃ§Ã£o final, apÃ³s `frame-ancestors` e `form-action` serem explicitados, o alerta CSP de fallback desapareceu. Permaneceram 6 categorias WARN-NEW, 0 FAIL e 62 PASS; o CSP restante Ã© exclusivamente `unsafe-inline` para script/style do Swagger UI.
