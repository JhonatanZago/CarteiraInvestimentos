# Modelo de ameaças STRIDE — Módulo 11

Escopo: endurecimento inicial da segurança da Carteira de Investimentos. Este documento cobre as fronteiras solicitadas e os controles implementados nesta change. Não substitui SAST, SCA, DAST ou revisão humana de achados.

| Componente | STRIDE / ameaça | Impacto | Controles implementados ou preservados | Risco residual |
|---|---|---|---|---|
| Autenticação JWT | Spoofing, Tampering, Elevation | Falsificação de identidade e acesso a carteiras | Segredo externo obrigatório fora de testes, mínimo de 32 caracteres, rejeição de placeholders, assinatura HMAC e expiração preservadas | Chave HMAC continua sendo segredo crítico; rotação, issuer e audience ficam para etapa futura. |
| Cookies e sessões | Spoofing, Tampering, Information Disclosure, CSRF, Repudiation | Roubo/uso indevido do refresh token ou invalidação cruzada | Cookie `HttpOnly`, `Secure` configurável, `SameSite` configurável, path restrito, limpeza simétrica, refresh rotation e hash persistido | Refresh/logout continuam acionáveis por cookie; não há auditoria de sessão ainda. |
| Angular/Spring Boot | Spoofing, Information Disclosure, CSRF | Origem maliciosa lendo respostas ou enviando requisições credenciadas | CORS por origem exata, métodos/cabeçalhos explícitos, credenciais somente para allowlist, preflight testado | TLS e configuração de domínio continuam responsabilidade do deployment. |
| Controllers REST | Tampering, Information Disclosure, DoS, Elevation | Leitura/escrita sem autenticação, vazamento de entidade ou erro inconsistente | Rotas protegidas exigem autenticação, DTOs/mappers, erros estruturados e validação existente | Scanners e DAST ainda não foram implementados. |
| Carteiras e posições | Tampering, Information Disclosure, Elevation, Repudiation | Usuário acessa ou altera patrimônio de outro usuário | Queries por proprietário, validação de carteira nos serviços, teste de isolamento e rejeição sem contexto autenticado | Não existe trilha de auditoria imutável por alteração. |
| Compras e vendas | Tampering, Information Disclosure, Elevation, Repudiation | Histórico ou resultado financeiro alterado indevidamente | Endpoints protegidos, verificações de propriedade, fórmulas/regras preservadas | Cada nova operação futura precisa de teste de fronteira de autorização. |
| Corretoras e ativos | Spoofing, Tampering, Information Disclosure, DoS | CRUD/revalidação não autorizados ou abuso de dados | API protegida, DTOs, credenciais de provedores no backend e timeouts/retry existentes | Política de ownership para dados globais não foi redesenhada. |
| APIs externas | Spoofing, Tampering, Information Disclosure, DoS | Vazamento de token, abuso do provedor ou indisponibilidade propagada | URLs/chaves por configuração externa, timeouts, retry limitado para GET e adapters isolados | Sem allowlist SSRF, circuit breaker, tracing ou SCA nesta etapa. |
| Banco e migrations | Tampering, Repudiation, Information Disclosure, DoS, Elevation | Deriva/perda de schema, console exposto ou persistência indevida | Nenhum comando destrutivo, H2 desabilitado por padrão, console permitido somente em profile local, constraints JPA preservadas | `ddl-auto=update` permanece; migrations versionadas são pendência separada. |

## Decisão CSRF

Recursos financeiros não são autorizados por cookie: exigem o access token no header `Authorization`. O cookie é usado somente para refresh/logout; portanto a configuração permanece stateless com CSRF do Spring Security desabilitado, e a fronteira é coberta por teste que rejeita cookie sem bearer token em criação de carteira. Se um endpoint futuro autorizar alteração de dados a partir de cookie, deverá introduzir proteção CSRF e o fluxo correspondente no Angular.

## Prioridade residual

1. Implementar o portão de segurança do Módulo 11: Semgrep, Gitleaks, SpotBugs/FindSecBugs, SBOM/CycloneDX, OSV-Scanner e ZAP.
2. Introduzir migrations versionadas antes de abandonar `ddl-auto=update` em produção.
3. Adicionar auditoria de eventos sensíveis e correlação de requisições.
4. Definir rotação/issuer/audience do JWT e política operacional de TLS/segredos.
