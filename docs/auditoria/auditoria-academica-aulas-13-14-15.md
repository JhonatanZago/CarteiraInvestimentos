# Auditoria academica - Aulas 13, 14 e 15

Data da auditoria: 2026-11-16.

## Resumo executivo

Os materiais originais `AULA-13-LOGIN-COM-JWT.md`,
`AULA-14-CONTROLE-DE-ACESSO.md` e `AULA-15-TESTES-E-COBERTURA.md` nao foram
encontrados no projeto. Tambem nao foi encontrada colecao Postman. Portanto,
esta auditoria compara o codigo com a documentacao interna disponivel e nao
declara conformidade academica plena sem os criterios originais.

O estado tecnico observado e:

- JWT padrao HS256 funcional e validado por testes de seguranca.
- RBAC USER/ADMIN e administracao de usuarios implementados.
- Ownership financeiro preservado inclusive para ADMIN.
- JaCoCo configurado e portao minimo aprovado.
- Duas evidencias RED -> GREEN -> REFACTOR legitimas documentadas; os demais
  comportamentos TDD foram Green-first.
- Meta academica de 80% de ramos ainda pendente.

## Matriz de requisitos

| Aula | Requisito | Status | Evidencia | Pendencia/prioridade |
|---|---|---|---|---|
| 13 | Entidade e persistencia de usuario | COMPLETO | `domain/Usuario.java`, `UsuarioRepository.java` | Confirmar contra material original |
| 13 | BCrypt | COMPLETO | `SecurityConfig.passwordEncoder()`, `AuthService` | Nenhuma tecnica |
| 13 | Login e emissao JWT | COMPLETO | `AuthController`, `AuthService`, `AuthTokenService` | Confirmar contrato didatico |
| 13 | JWT HEADER.PAYLOAD.SIGNATURE / HS256 | COMPLETO | `AuthTokenService`, `AuthTokenServiceSecurityTest` | Nenhuma tecnica |
| 13 | `sub`, `iss`, `iat`, `exp`, authorities | COMPLETO | `AuthTokenServiceSecurityTest.emitsStandardHs256JwtClaimsWithoutSensitiveData` | Nenhuma tecnica |
| 13 | Assinatura, emissor, expiracao e adulteracao | COMPLETO | `JwtContextSecurityTest`, `AuthTokenServiceSecurityTest` | Nenhuma tecnica |
| 13 | Stateless e protecao HTTP | COMPLETO | `SecurityConfig`, `SecurityRegressionTest` | Nenhuma tecnica |
| 13 | Logout/refresh e revogacao | PARCIAL | `AuthController`, `AuthService`, testes de refresh/logout | Confirmar semantica exigida pela aula |
| 13 | Migrations | PENDENTE | Nenhum diretorio de migration encontrado | Verificar requisito original; P1 |
| 13 | Postman/documentacao de requisicoes | PENDENTE | Nenhuma colecao encontrada; `docs/security/jwt-standard.md` existe | Obter material original; P1 |
| 14 | Perfis USER e ADMIN persistidos | COMPLETO | `PerfilUsuario`, `Usuario.perfil` | Nenhuma tecnica |
| 14 | `@EnableMethodSecurity` | COMPLETO | `SecurityConfig` | Nenhuma tecnica |
| 14 | `@PreAuthorize` administrativo | COMPLETO | `UsuarioAdministracaoController` e `UsuarioAdministracaoService` | Nenhuma tecnica |
| 14 | 401 e 403 | COMPLETO | `UsuarioAdministracaoSecurityTest`, `NegativeAuthorizationSecurityTest` | Nenhuma tecnica |
| 14 | CRUD administrativo e senha/status | COMPLETO | `UsuarioAdministracaoController`, service e testes MockMvc | Confirmar contratos didaticos |
| 14 | DTOs sem hash/token | COMPLETO | `UsuarioAdminResponse`, testes administrativos | Nenhuma tecnica |
| 14 | Ownership financeiro inclusive para ADMIN | COMPLETO | `FinancialOwnershipSecurityTest`, `JwtContextSecurityTest` | Nenhuma tecnica |
| 15 | JUnit 5/Mockito/MockMvc | COMPLETO | `build.gradle`, suites `src/test/java` | Nenhuma tecnica |
| 15 | JaCoCo HTML/XML/CSV | COMPLETO | `build.gradle`, `build/reports/jacoco/test` | Nenhuma tecnica |
| 15 | Portao de linhas 80% e ramos 55% | COMPLETO | `jacocoTestCoverageVerification` | E um limite minimo, nao a meta final |
| 15 | Meta academica 80% de ramos | PENDENTE | Resultado atual: 70,98% | Faltam 95 ramos; P1 |
| 15 | Tres ciclos TDD completos | PARCIAL | `docs/testes/tdd-aula15.md` | Duas evidencias RED legitimas; nao declarar 3 |

## Aula 13 - JWT

`AuthTokenService` usa `NimbusJwtEncoder`/`NimbusJwtDecoder`, HS256, emissor,
expiracao e segredo externo. `AccessTokenFilter` aceita somente Bearer, consulta
o usuario persistido e considera o perfil atual. Os testes cobrem token valido,
expirado, adulterado, malformado, `alg=none`, emissor invalido, usuario
desconhecido/inativo e ausencia de dados sensiveis.

O ponto academico nao comprovado e a ausencia dos materiais originais, alem de
migrations e Postman nao localizados. O refresh token e cookie HttpOnly com
hash no backend, mas a equivalencia dessa arquitetura com a aula precisa ser
confirmada no material do professor.

## Aula 14 - RBAC

`SecurityConfig` habilita method security. A autoridade persistida e convertida
para `ROLE_USER`/`ROLE_ADMIN`; endpoints administrativos possuem
`@PreAuthorize("hasRole('ADMIN')")` no controller e no service. Os testes
administrativos cobrem 200/201/204, 400/401/403/404/409, senha BCrypt,
desativacao e protecao do ultimo ADMIN.

O ADMIN nao recebe acesso automatico a carteiras de terceiros. Isso e uma
decisao de dominio comprovada pelos testes de ownership e deve ser preservada.

## Aula 15 - TDD e JaCoCo

Configuracao atual: JaCoCo 0.8.14, relatorios HTML/XML/CSV e verificacao com
80% de linhas e 55% de ramos. A execucao realizada foi:

```text
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Resultado real:

- 241 testes.
- 0 falhas.
- 0 erros.
- 1 ignorado.
- Linhas: 89,56% (1.441/1.609).
- Ramos: 70,98% (746/1.051).
- Instrucoes: 87,75%.
- Metodos: 86,08%.
- Classes: 97,24%.
- Portao JaCoCo: PASS.

Os limites configurados sao 80% de linhas e 55% de ramos. A meta academica de
80% de ramos e adicional e permanece pendente. A documentacao comprova dois
ciclos RED reais: taxa cambial negativa em movimentacao financeira e uso
incorreto do relogio em `ExchangeRateService`. Os ciclos de administracao e do
limite JWT foram Green-first; nao devem ser contados como RED artificial.

## Teste ignorado

`PostgreSqlSmokeTest.startsWithPostgreSqlAndCreatesTheSchemaConstraints()` e
ignorado por `@EnabledIfSystemProperty(named = "smoke.db.url", matches = ".+")`.
Ele exige uma instancia PostgreSQL e as propriedades `smoke.db.url`,
`smoke.db.username`, `smoke.db.password` e `smoke.db.driver`. Nao foi alterado.
Pode ser habilitado com seguranca em ambiente PostgreSQL controlado, mas nao
deve ser convertido em teste H2 nem executado com credenciais reais sem
configuracao explicita.

## Contagem e limitacoes

A contagem atual foi confirmada pelos XMLs Gradle. Relatos historicos anteriores
possuem agregacoes de cenarios que nao podem ser reconciliadas completamente
sem os XMLs correspondentes; essa limitacao esta documentada, sem inventar
testes removidos ou parametrizacoes.

## Plano priorizado

1. Obter os arquivos originais das Aulas 13-15 e a colecao Postman para fechar
   os requisitos academicos nao verificaveis.
2. Confirmar a estrategia de migrations exigida pela disciplina.
3. Habilitar `PostgreSqlSmokeTest` em ambiente PostgreSQL dedicado e registrar
   a evidencia, sem alterar o teste.
4. Continuar testes de alto impacto para aproximar ramos de 80%, mantendo o
   portao minimo atual.
5. Preparar uma matriz de demonstracao para a prova, separando evidencias de
   codigo, testes e execucao de infraestrutura.

Nenhuma funcionalidade foi implementada nesta auditoria.
