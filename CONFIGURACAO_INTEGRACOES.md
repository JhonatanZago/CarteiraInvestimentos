# Configuração de integrações externas

O backend lê as credenciais locais do arquivo `.env` na raiz do projeto. Esse arquivo já é ignorado pelo Git e nunca deve ser copiado para o Angular.

## BRAPI

Adicione ou atualize estas linhas no `.env` local:

```properties
BRAPI_BASE_URL=https://brapi.dev
BRAPI_TOKEN=cole_a_chave_aqui
```

Reinicie o backend depois da alteração:

```powershell
.\gradlew.bat bootRun
```

O adaptador de cotação usa o endpoint `/api/v2/stocks/quote` do provedor para ações do mercado brasileiro. Cadastre uma ação, então chame `POST /api/v1/acoes/{id}/atualizar-cotacao` no Swagger para validar a chave sem expô-la.

## Fonte americana opcional

Quando houver uma chave compatível, configure também:

```properties
ALPHA_VANTAGE_BASE_URL=https://www.alphavantage.co
ALPHA_VANTAGE_API_KEY=cole_a_chave_aqui
```

Nunca publique o `.env`, tokens, cabeçalhos `Authorization` ou URLs com chaves embutidas.
