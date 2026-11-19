# Evidencias da Aula 14 - RBAC

## Classes relevantes

| Classe | Evidencia |
|---|---|
| `SecurityConfig` | `@EnableMethodSecurity`, sessao stateless, filtro Bearer e handlers 401/403 |
| `Usuario` | Campo persistido `perfil` com `PerfilUsuario` e campo `ativo` |
| `AuthTokenService` | Emissao HS256 com authority derivada do perfil |
| `UsuarioAdministracaoController` | `@PreAuthorize("hasRole('ADMIN')")` em listar, buscar, criar, atualizar, status e senha |
| `UsuarioAdministracaoService` | Mesma restricao no servico e regras de negocio administrativas |
| `CarteiraService` | Consultas por `findByIdAndUsuarioId` e ocultacao por 404 |
| `AtivoCarteiraService`, `VendaService`, `DashboardCarteiraService` | Validacao de carteira pertencente ao usuario atual |
| `CurrentUser` | Identidade obtida do contexto de seguranca |

## Matriz de permissoes

| Endpoint | Acesso | Ownership |
|---|---|---|
| `POST /api/v1/auth/cadastro` | Publico | N/A |
| `POST /api/v1/auth/login` | Publico | N/A |
| `POST /api/v1/auth/refresh` | Publico com cookie de refresh | Cookie revogado/rotacionado no backend |
| `POST /api/v1/auth/logout` | Publico com cookie | Revoga o refresh apresentado |
| `GET /api/v1/auth/me` | Autenticado | Usuario do Bearer |
| `GET/POST /api/v1/carteiras` | Autenticado | Criar e listar apenas recursos do usuario |
| `GET/PUT/DELETE /api/v1/carteiras/{id}` | Autenticado | Dono; terceiro recebe 404 |
| `/api/v1/carteiras/{id}/posicoes/**` | Autenticado | Dono da carteira |
| `/api/v1/carteiras/{id}/vendas/**` | Autenticado | Dono da carteira |
| `GET /api/v1/dashboard/carteiras/{id}` | Autenticado | Dono da carteira |
| `GET /api/v1/carteiras/{id}/analise-moedas` | Autenticado | Dono da carteira |
| `GET /api/v1/carteiras/{id}/evolucao` | Autenticado | Dono da carteira |
| `GET /api/v1/carteiras/{id}/proventos` | Autenticado | Dono da carteira |
| `GET/POST /api/v1/acoes/**` | Autenticado | Dados de mercado; sem ownership de carteira |
| `GET/POST /api/v1/corretoras/**` | Autenticado | Cadastro global conforme contrato existente |
| `GET /api/v1/admin/usuarios` | ADMIN | Administracao de contas, nao patrimonio |
| `GET /api/v1/admin/usuarios/{id}` | ADMIN | Conta de usuario |
| `POST /api/v1/admin/usuarios` | ADMIN | Cria sempre USER |
| `PUT /api/v1/admin/usuarios/{id}` | ADMIN | Conta e papel |
| `PATCH /api/v1/admin/usuarios/{id}/status` | ADMIN | Conta; preserva dados financeiros |
| `PUT /api/v1/admin/usuarios/{id}/senha` | ADMIN | Senha BCrypt |

Observacao: a linha de evolucao/proventos usa os mappings reais de
`MarketInsightController`; a grafia correta do prefixo e
`/api/v1/carteiras/{carteiraId}/...`.

## Testes automatizados

- `UsuarioAdministracaoSecurityTest`: 401 sem token, 403 para USER, sucesso
  ADMIN, cadastro, duplicidade, senha, desativacao, ultimo ADMIN e carteira
  privada.
- `NegativeAuthorizationSecurityTest`: tokens ausentes, invalidos, expirados,
  IDs de terceiros e operacoes indevidas.
- `FinancialOwnershipSecurityTest`: isolamento de recursos financeiros.
- `JwtContextSecurityTest`: authorities, usuario inativo e isolamento do
  SecurityContext.

## Resultado da validacao

Execucao local:

```text
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Resultado registrado: 241 testes, 0 falhas, 0 erros, 1 ignorado; JaCoCo PASS,
89,56% de linhas e 70,98% de ramos.

## Pendencias reais

- A colecao Postman abaixo foi validada como JSON, mas nao executada no Postman.
- `adminToken` e `thirdPartyPortfolioId` exigem preparacao de um ambiente de
  teste; nenhum valor real e fornecido no repositorio.
- Os materiais originais da Aula 14 nao estao no projeto, portanto a
  conformidade academica deve ser confirmada contra o documento do professor.
