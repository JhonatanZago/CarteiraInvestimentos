# Aula 13 - Login com JWT

## Objetivo

O backend autentica usuarios com access token JWT e protege os endpoints
privados sem manter sessao HTTP no servidor. O refresh token e separado do JWT
e trafega em cookie HttpOnly.

## Arquitetura atual

- Cadastro: `POST /api/v1/auth/cadastro`.
- Login: `POST /api/v1/auth/login`.
- Usuario autenticado: `GET /api/v1/auth/me`.
- Renovacao: `POST /api/v1/auth/refresh`, usando o cookie `refresh_token`.
- Logout: `POST /api/v1/auth/logout`, revogando o refresh token apresentado.
- Rotas privadas: header `Authorization: Bearer <access-token>`.
- O filtro consulta o usuario persistido e usa o perfil atual para criar a
  authority Spring.

## Cadastro

Requisicao:

```http
POST {{baseUrl}}/api/v1/auth/cadastro
Content-Type: application/json

{
  "nome": "Usuario de Teste",
  "email": "usuario.teste@example.com",
  "senha": "SenhaAcademica123!",
  "confirmacaoSenha": "SenhaAcademica123!"
}
```

Resposta esperada: `201 Created`, com `id`, `nome`, `email` e `perfil`. A
resposta nao inclui senha nem hash.

## Login

```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json

{
  "email": "usuario.teste@example.com",
  "senha": "SenhaAcademica123!",
  "manterConectado": false
}
```

Resposta `200 OK`:

```json
{
  "accessToken": "<token-jwt-gerado-pelo-backend>",
  "expiresInSeconds": 86400,
  "usuario": {
    "id": 1,
    "nome": "Usuario de Teste",
    "email": "usuario.teste@example.com",
    "perfil": "USER"
  }
}
```

O response tambem define o cookie HttpOnly `refresh_token`. O valor do cookie
nao deve ser copiado para documentacao ou logs.

## Estrutura e validacao do JWT

O access token e um JWS compacto:

```text
HEADER.PAYLOAD.SIGNATURE
```

Ele e assinado com HS256. As claims utilizadas sao:

- `sub`: id numerico do usuario.
- `iss`: emissor configurado.
- `iat`: instante de emissao.
- `exp`: instante de expiracao.
- `authorities`: autoridade derivada do perfil, como `ROLE_USER` ou
  `ROLE_ADMIN`.

O decoder rejeita assinatura invalida, emissor inesperado, token expirado,
claims malformadas e algoritmos nao permitidos. O token nao contem senha, hash,
refresh token, segredo ou dados financeiros.

## Uso em rota protegida

```http
GET {{baseUrl}}/api/v1/auth/me
Authorization: Bearer <access-token>
```

Resposta esperada: `200 OK` com os dados publicos do usuario autenticado.

## Refresh e logout

O cliente deve manter o cookie de refresh no cookie jar. Nao e enviado como
Bearer token.

```http
POST {{baseUrl}}/api/v1/auth/refresh
```

O refresh e rotacionado e o cookie e substituido. Para logout:

```http
POST {{baseUrl}}/api/v1/auth/logout
```

Resposta esperada: `204 No Content`, com instrucao de limpeza do cookie.

## Respostas de seguranca

- `401 Unauthorized`: ausencia, formato invalido, assinatura invalida, token
  expirado ou usuario inexistente/inativo.
- `403 Forbidden`: autenticacao valida, mas papel insuficiente para a operacao.

## Configuracao

- `AUTH_JWT_SECRET`: segredo externo, obrigatorio fora do perfil de teste e com
  pelo menos 32 caracteres; nao usar valor de exemplo.
- `AUTH_JWT_ISSUER`: emissor esperado; padrao tecnico `carteira-investimento`.
- `AUTH_REFRESH_COOKIE_SECURE`: deve ser `true` fora dos perfis locais.
- `AUTH_REFRESH_COOKIE_SAME_SITE`: `Lax`, `Strict` ou `None`.
- `AUTH_REFRESH_COOKIE_PATH`: por padrao `/api/v1/auth`.

Segredos devem ser fornecidos pelo ambiente ou por um gerenciador externo. Nao
coloque valores reais em colecoes Postman, codigo, logs ou commits.

## Evidencias automatizadas

As principais evidencias estao em `AuthTokenServiceSecurityTest`,
`JwtContextSecurityTest`, `SecurityRegressionTest` e
`UsuarioAdministracaoSecurityTest`.
