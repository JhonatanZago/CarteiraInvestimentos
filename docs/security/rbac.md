# RBAC da Carteira de Investimentos

## Política aplicada

Os papéis persistidos são `USER` e `ADMIN`. O filtro de access token valida o
JWT e consulta o usuário ativo; em seguida converte o perfil persistido para a
authority Spring correspondente (`ROLE_USER` ou `ROLE_ADMIN`). Assim, a
desativação ou alteração de perfil não depende apenas de um claim antigo do
token.

`USER` pode operar os próprios recursos financeiros. `ADMIN` mantém essas
permissões para os próprios recursos, mas não recebe acesso implícito a
carteiras, posições ou movimentações privadas de terceiros. O ownership continua
sendo aplicado antes de buscar, alterar ou excluir esses recursos.

Method security está habilitado por `@EnableMethodSecurity`, deixando
`@PreAuthorize` disponível para os serviços administrativos quando seus
contratos forem introduzidos.

## Administração implementada

Todos os endpoints abaixo exigem `@PreAuthorize("hasRole('ADMIN')")` no
controller e no serviço:

- `GET /api/v1/admin/usuarios` — lista usuários (`200`);
- `GET /api/v1/admin/usuarios/{id}` — consulta usuário (`200` ou `404`);
- `POST /api/v1/admin/usuarios` — cadastra usuário USER (`201`);
- `PUT /api/v1/admin/usuarios/{id}` — altera nome, e-mail e papel (`200`);
- `PATCH /api/v1/admin/usuarios/{id}/status` — ativa/desativa (`200`);
- `PUT /api/v1/admin/usuarios/{id}/senha` — altera senha (`204`).

Usuários comuns e requisições anônimas recebem `403` e `401`, respectivamente.
Dados inválidos retornam `400`, e e-mails duplicados retornam `409`.

O cadastro administrativo sempre cria `USER`; não existe criação pública de
administrador. A última conta ADMIN ativa não pode ser rebaixada ou desativada.
Alterar senha revoga os refresh tokens da conta.

## Segurança financeira e dados sensíveis

As respostas usam DTOs administrativos e não retornam senha, hash BCrypt,
tokens, segredos ou dados financeiros. A administração de usuários não concede
acesso a carteiras, posições ou movimentações de terceiros; essas consultas
continuam filtradas pelo proprietário autenticado.
