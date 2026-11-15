# Padrão JWT da autenticação

## Formato

O access token é um JWS compacto no formato `HEADER.PAYLOAD.SIGNATURE`, assinado
com `HS256` por meio da API JOSE do Spring Security. A aplicação permanece
stateless para o access token, que é enviado pelo frontend no header
`Authorization: Bearer <token>`.

## Claims

- `sub`: identificador numérico do usuário autenticado.
- `iss`: emissor configurado em `AUTH_JWT_ISSUER`.
- `iat`: instante de emissão.
- `exp`: instante de expiração.
- `authorities`: autoridade derivada do perfil persistido do usuário.

O token não contém senha, hash de senha, refresh token, dados financeiros ou
outros segredos. A validação verifica assinatura HS256, emissor e expiração.

## Configuração

- `AUTH_JWT_SECRET`: segredo externo, obrigatório fora do perfil de teste e com
  pelo menos 32 caracteres; não deve ser um valor de exemplo.
- `AUTH_JWT_ISSUER`: emissor esperado, com padrão técnico
  `carteira-investimento` quando não for informado.

O refresh token continua sendo um valor aleatório armazenado apenas como hash
no backend e entregue em cookie HttpOnly conforme a configuração existente; ele
não é tratado como access JWT.
