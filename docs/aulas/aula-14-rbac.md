# Aula 14 - RBAC na Carteira de Investimentos

## Autenticacao x autorizacao

Autenticacao confirma quem fez a requisicao por meio do access token Bearer.
Autorizacao decide se essa identidade pode executar a operacao. O backend nao
confia em guards, botoes ou parametros de usuario enviados pelo frontend.

## Papeis persistidos

`Usuario.perfil` usa o enum `PerfilUsuario` com `USER` e `ADMIN`. O filtro de
access token consulta o usuario ativo e converte o perfil atual para
`ROLE_USER` ou `ROLE_ADMIN`. Assim, mudar o perfil ou desativar a conta nao
depende exclusivamente de um claim antigo do JWT.

## Method Security

`SecurityConfig` possui `@EnableMethodSecurity`. Os endpoints e o servico de
administracao usam `@PreAuthorize("hasRole('ADMIN')")`. A regra e aplicada no
backend e continua valida mesmo que o frontend seja contornado.

## Politica efetiva

- `USER`: administra as proprias carteiras, posicoes, compras, vendas,
  historico e indicadores.
- `ADMIN`: administra usuarios e mantem as permissoes de usuario para os seus
  proprios recursos.
- Nenhum papel ignora ownership financeiro.

Um ADMIN nao deve acessar automaticamente carteiras privadas de terceiros:
administrar contas e uma funcao diferente de consultar patrimonio privado. Os
servicos financeiros filtram a carteira pelo usuario autenticado e retornam
`404` quando o recurso nao pertence ao solicitante, ocultando sua existencia.

## Respostas

- `401 Unauthorized`: requisicao sem autenticacao ou com token invalido,
  expirado, adulterado ou associado a usuario inexistente/inativo.
- `403 Forbidden`: autenticacao valida, mas papel insuficiente, como USER
  tentando acessar `/api/v1/admin/usuarios`.
- `404 Not Found`: recurso financeiro inexistente ou pertencente a outro
  usuario, conforme o contrato de ocultacao de ownership.

## Evidencias

- `SecurityConfig.java`: method security, stateless e authorities.
- `Usuario.java` e `PerfilUsuario.java`: persistencia do papel e status ativo.
- `UsuarioAdministracaoController.java`: seis operacoes administrativas com
  `@PreAuthorize`.
- `UsuarioAdministracaoService.java`: regras de papel, senha, status e ultimo
  ADMIN ativo.
- `UsuarioAdministracaoSecurityTest.java`: 401, 403, sucesso ADMIN, BCrypt,
  desativacao, ultimo ADMIN e isolamento financeiro.
- `FinancialOwnershipSecurityTest.java` e `JwtContextSecurityTest.java`:
  isolamento horizontal, inclusive para ADMIN.
