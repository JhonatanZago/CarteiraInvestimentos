# Operação e verificação das integrações

## Variáveis de ambiente

Defina no `.env` da raiz (nunca no Angular e nunca no Git):

```properties
BRAPI_BASE_URL=https://brapi.dev
BRAPI_TOKEN=
ALPHA_VANTAGE_BASE_URL=https://www.alphavantage.co
ALPHA_VANTAGE_API_KEY=
BRASIL_API_BASE_URL=https://brasilapi.com.br
VIACEP_BASE_URL=https://viacep.com.br
FINANCIAL_INSTITUTION_BASE_URL=https://www2.cvm.gov.br
HTTP_CONNECT_TIMEOUT=PT5S
HTTP_READ_TIMEOUT=PT10S
```

O `docker-compose.yml` repassa somente essas variáveis ao backend. Nenhuma
credencial é compilada no frontend ou escrita em logs.

## Fontes e cobertura

- BRAPI: cotações e ações corporativas, conforme documentação em
  <https://brapi.dev/docs>.
- Banco Central SGS/PTAX (via adapter de indicadores): séries públicas em
  <https://www.bcb.gov.br/estabilidadefinanceira/sgs> e
  <https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata>.
- Alpha Vantage: cotação internacional quando `ALPHA_VANTAGE_API_KEY` estiver
  configurada, conforme <https://www.alphavantage.co/documentation/>.
- BrasilAPI e ViaCEP: validação cadastral e endereço, conforme a documentação
  pública de cada serviço.
- CVM: consulta regulatória institucional. Ela não substitui validação
  cadastral nem transforma CNPJ em autorização automática.

Quando a fonte principal falha, somente falhas transitórias elegíveis acionam
o adapter alternativo. Respostas de mapeamento incompletas não são convertidas
em números. O último valor válido pode ser retornado como `STALE` pelos
serviços que possuem cache.

## Verificação operacional

```powershell
./gradlew test
cd frontend
npx tsc -p tsconfig.app.json --noEmit
npm test -- --watch=false
npm run build
```

Com a aplicação em execução, verifique:

```text
GET /api/v1/mercado/indicadores
GET /api/v1/carteiras/{id}/evolucao
GET /api/v1/carteiras/{id}/analise-moedas
GET /api/v1/carteiras/{id}/proventos
```

As respostas devem expor apenas dados normalizados, status e datas reais; não
devem conter tokens, stack traces ou URLs autenticadas.

## Rollback

As alterações desta auditoria são compatíveis e aditivas. Para rollback,
desative a fonte/adapters novos removendo suas variáveis de ambiente e faça
novo deploy da versão anterior. Não remova colunas ou registros adicionados e
não execute `docker compose down --volumes`, pois isso apaga o volume do banco.

## Limitações conhecidas

- O cache atual é em memória e é perdido ao reiniciar o backend.
- A cobertura de proventos e índices depende do plano e da disponibilidade da
  fonte; o sistema exibe indisponibilidade honesta quando não há fallback.
- Posições legadas sem câmbio histórico comprovado permanecem identificadas
  como estimadas/limitadas.
