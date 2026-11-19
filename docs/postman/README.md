# Colecao Postman da Aula 13

## Pre-requisitos

1. Inicie o backend com Java 21 e um perfil local/teste apropriado.
2. Configure `AUTH_JWT_SECRET` conforme `docs/security/jwt-standard.md` quando
   executar fora do perfil de teste. Nunca use o valor real na colecao.
3. Confirme a URL local, normalmente `http://localhost:8080`.

## Importacao

Importe `CarteiraInvestimentos-Aula13.postman_collection.json` no Postman e
selecione a colecao. A variavel `baseUrl` pode ser alterada na aba Variables.

As variaveis usadas sao:

- `baseUrl`: URL do backend.
- `accessToken`: preenchida automaticamente pelo teste de login.
- `testEmail` e `testPassword`: credenciais ficticias da execucao local.
- `expiredToken`: opcional; preencher somente com um JWT expirado gerado no
  ambiente de teste, sem publicar seu valor.

O refresh token atual e um cookie HttpOnly chamado `refresh_token`. O Postman
deve manter o cookie no cookie jar; ele nao e capturado em uma variavel nem
impresso pelo script.

## Ordem recomendada

1. Executar **Cadastro**.
2. Executar **Login**; o script salva apenas o access token na variavel local.
3. Executar **Usuario autenticado**.
4. Executar **Refresh** usando o cookie armazenado pelo Postman.
5. Executar **Logout**.
6. Repetir **Sem token** e **Token invalido** para evidenciar `401`.
7. Preencher `expiredToken` somente se houver um token expirado de teste e
   executar o caso correspondente.
8. Executar **Admin sem permissao** com um token USER para evidenciar `403`.

Os scripts validam status e estrutura sem registrar tokens. A colecao nao
possui credenciais reais, segredos JWT ou chamadas a provedores externos.

## Evidencias academicas

Use o Postman Runner ou execute as requisicoes individualmente. Exporte apenas
resultados sem headers sensiveis e remova tokens, cookies e credenciais antes de
compartilhar qualquer captura.
