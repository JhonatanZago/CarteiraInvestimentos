# Auditoria de integrações (TASK 7.1)

## Política comum

Todos os adapters usam o `RestClient.Builder` compartilhado em
`RestClientConfig`. Os timeouts vêm de `integration.http.connect-timeout` e
`integration.http.read-timeout` (padrão 5s/10s). A política agora repete no
máximo duas vezes somente requisições GET que falham por erro de rede, HTTP 5xx
ou 429, com pequeno backoff. Não há retry para 4xx determinísticos, e nenhum
corpo de requisição mutável é repetido.

## Matriz de adapters

| Fonte | Adapter | Responsabilidade | Autenticação | Cache/fallback | Observação |
|---|---|---|---|---|---|
| BrasilAPI | `BrasilApiAdapter` | CNPJ empresarial | nenhuma | facade cadastral | 404 vira não encontrado; demais falhas são integração |
| ViaCEP | `ViaCepAdapter` | endereço por CEP | nenhuma | facade cadastral | resposta `erro=true` é não encontrado |
| BRAPI | `BrapiCotacaoAdapter` | cotação e metadados | Bearer opcional | `CotacaoFacade` | moeda/preço são validados antes do mapeamento |
| BRAPI | `BrapiMarketIndicatorAdapter` | IBOV/CDI/USD | Bearer | `MarketInsightService` + BCB | habilitado apenas por propriedade |
| BCB/Yahoo | `BancoCentralMarketIndicatorAdapter` | fallback macro/câmbio/índice | nenhuma | `MarketInsightService` | não expõe credenciais; zero é rejeitado |
| Alpha Vantage | `AlphaVantageCotacaoAdapter` | cotações EUA | chave server-side | `CotacaoFacade` | rate-limit (`Information`/`Note`) não é ticker inexistente |
| CVM | `CvmInstituicaoFinanceiraAdapter` | autorização institucional | nenhuma | facade regulatória | 404 separado de falhas de transporte |
| BRAPI/Yahoo | `BrapiIncomeAdapter`/`YahooFinanceIncomeAdapter` | proventos | Bearer opcional | `MarketInsightService` | cobertura parcial por ticker é preservada |

## Achados e controles

- Credenciais só são lidas por `IntegrationProperties` no backend; não são
  impressas nos logs nem enviadas pelo Angular.
- URLs base são configuráveis por ambiente. O exemplo usa
  `BRAPI_BASE_URL=https://brapi.dev`; os adapters acrescentam os caminhos
  documentados (`/api/...`) uma única vez.
- O cache de indicadores (5 min) e câmbio (5 min) é aplicado nas facades/
  services, evitando chamadas por renderização.
- Falhas transitórias podem usar o último valor válido marcado como stale;
  respostas de mapeamento incompletas continuam sendo erro de integração e não
  são convertidas silenciosamente em dados financeiros.
- Logs registram apenas código da fonte e classe da exceção, sem URL
  autenticada, token ou corpo de resposta.

## Limitações confirmadas

- A política de retry é deliberadamente limitada a GET; operações futuras de
  escrita externa devem definir idempotência própria.
- O cache atual é em memória e é perdido no reinício; persistência do último
  valor é uma melhoria posterior (TASK 7.2/9.3).
