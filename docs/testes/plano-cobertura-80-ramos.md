# Plano para 80% de cobertura de ramos

## Linha de base

Fonte: `build/reports/jacoco/test/jacocoTestReport.xml`, gerado após a
execução completa do Comando 67. O relatório HTML correspondente está em
`build/reports/jacoco/test/html/index.html`.

- Ramos cobertos: **593**.
- Ramos existentes: **1.051**.
- Ramos não cobertos: **458**.
- Meta de 80%: pelo menos **841 ramos cobertos**.
- Ramos adicionais mínimos necessários: **248**.
- Cobertura atual: **56,42%**.

O cálculo usa arredondamento para cima: `ceil(1.051 x 0,80) = 841`.

## Ranking por impacto absoluto

As estimativas abaixo são planejamento, não resultados de execução. Elas
representam a faixa de ramos potencialmente alcançável com testes de contrato,
integração e cenários negativos; não devem ser somadas como se fossem garantia.

| Classe | Pacote | Cobertos | Não cobertos | Cobertura | Adicionais alcançáveis (estimativa) | Esforço | Risco |
|---|---|---:|---:|---:|---:|---|---|
| `BrapiIncomeAdapter` | `integration.adapter` | 1 | 51 | 1,92% | 30–45 | Alto | Alto |
| `BrapiCotacaoAdapter` | `integration.adapter` | 31 | 41 | 43,06% | 22–32 | Alto | Alto |
| `AcaoRegistrationService` | `service` | 53 | 33 | 61,63% | 18–26 | Médio | Alto |
| `DashboardCarteiraService` | `service` | 75 | 31 | 70,75% | 15–24 | Médio | Alto |
| `MovimentacaoFinanceiraService` | `service` | 28 | 24 | 53,85% | 12–20 | Médio | Muito alto |
| `BrapiMarketIndicatorAdapter` | `integration.adapter` | 5 | 23 | 17,86% | 12–18 | Médio | Médio |
| `TwelveDataCotacaoAdapter` | `integration.adapter` | 17 | 23 | 42,50% | 12–18 | Médio | Médio |
| `AtivoCarteiraService` | `service` | 23 | 19 | 54,76% | 10–15 | Médio | Alto |
| `VendaService` | `service` | 26 | 18 | 59,09% | 10–15 | Médio | Muito alto |
| `YahooFinanceIncomeAdapter` | `integration.adapter` | 0 | 18 | 0% | 10–16 | Alto | Alto |

Outras classes relevantes no XML: `ExchangeRateService` (13 ramos não
cobertos), `AuthProperties` (13), `MarketInsightService` (12),
`ContextoMonetario` (11), `PortfolioSnapshotService` (10),
`CorretoraRegistrationService` (8), `RestClientConfig` (8) e
`BancoCentralMarketIndicatorAdapter` (7).

## Métodos prioritários

O maior impacto concentrado por método foi:

| Classe / método | Ramos cobertos | Não cobertos | Cenários necessários |
|---|---:|---:|---|
| `BrapiIncomeAdapter.parseEventos` | 0 | 22 | resposta válida, campos ausentes, taxa não positiva, datas inválidas e símbolo ausente |
| `BrapiIncomeAdapter.buscarDividendos` | 0 | 20 | lista vazia, resposta principal válida, falha, fallback legado e fallback vazio |
| `BrapiCotacaoAdapter.buscarLegado` | 0 | 20 | resposta vazia, resultado ausente, campos incompletos, moeda inválida e erro HTTP |
| `AcaoRegistrationService.registrar` | 31 | 19 | ticker inválido, país divergente, resposta incompleta, duplicidade e histórico duplicado |
| `TwelveDataCotacaoAdapter.buscarCotacao` | 9 | 13 | símbolo sem preço, moeda inválida, timestamp ausente e resposta de erro |
| `VendaService.calcular` | 14 | 10 | venda parcial, venda total, custo zero, taxas e resultado negativo |
| `ContextoMonetario` construtor | 22 | 10 | estados `AVAILABLE`, `ESTIMATED`, `UNAVAILABLE`, mesma moeda e taxa inválida |
| `MovimentacaoFinanceiraService.resultadoRealizado` | 0 | 8 | lucro, prejuízo, venda parcial e ausência de custo |

## Sequência recomendada

### Fase 1 — Adaptadores externos isolados

Usar respostas JSON fictícias, sem rede real, para BRAPI, TwelveData, Yahoo e
Banco Central. Priorizar as respostas alternativas e os fallbacks. Isso cobre
muitos ramos sem tocar em regras financeiras.

### Fase 2 — Regras financeiras

Adicionar testes de `VendaService`, `MovimentacaoFinanceiraService`,
`DashboardCarteiraService` e `AtivoCarteiraService` com `BigDecimal`, mantendo
compras repetidas, preço médio, venda total/parcial, moeda original e ownership.

### Fase 3 — Contratos monetários e snapshots

Cobrir `ContextoMonetario`, `ExchangeRateService` e `PortfolioSnapshotService`
para taxa positiva, taxa ausente, taxa desatualizada, direção BRL/USD,
indisponibilidade e carteiras mistas.

### Fase 4 — Segurança e infraestrutura

Cobrir `AuthProperties`, `AuthService`, `RestClientConfig` e os caminhos
restantes de autorização, sem alterar JWT, RBAC ou o Security Gate.

Após cada fase, executar o relatório JaCoCo e recalcular o déficit real. A
estimativa de 248 ramos é o requisito mínimo global; não é necessário cobrir
todos os 458 ramos não cobertos se a meta de 841 for atingida por cenários
relevantes.

## Classificação dos ramos

- **Alcançáveis com testes locais:** parsing de JSON, validações, fallbacks,
  exceções, cálculos com fronteiras e ownership. São a prioridade.
- **Dependentes de infraestrutura:** chamadas HTTP reais, indisponibilidade de
  DNS, TLS, timeout e comportamento do provedor. Devem ser simulados por
  adapters/clientes controlados; não devem exigir internet nos testes.
- **Cenários complexos:** concorrência, retry, snapshots diários e combinações
  de carteiras mistas. Exigem testes de integração específicos e devem vir
  depois dos adaptadores isolados.
- **Possivelmente inacessíveis ou mortos:** ramos privados sem chamada,
  fallback legado não referenciado ou DTOs sem uso. Antes de qualquer exclusão,
  confirmar contrato público e uso por busca estática. Não excluir apenas para
  elevar o percentual.

## Oportunidades TDD

`docs/testes/tdd-aula15.md` registra um ciclo RED -> GREEN -> REFACTOR real e
dois conjuntos Green-first. Nesta auditoria não há evidência suficiente para
afirmar dois novos defeitos. Oportunidades que podem produzir ciclos legítimos,
caso o comportamento atual falhe na execução, são:

1. `BrapiIncomeAdapter`: rejeitar ou ignorar evento com taxa não positiva e
   preservar o fallback quando a resposta principal estiver vazia.
2. `VendaService`: impedir resultado realizado incorreto quando o custo da
   posição é zero ou quando a venda é exatamente total.
3. `PortfolioSnapshotService`: manter `UNAVAILABLE` quando falta moeda ou taxa,
   sem substituir o valor por zero.

Cada oportunidade deve começar com teste que falhe por comportamento ausente ou
incorreto. Se o código já cumprir a regra, registrar como Green-first, sem
fabricar uma falha.

## Riscos e esforço

- Adaptadores têm maior potencial de cobertura, mas dependem de contratos JSON e
  podem revelar incompatibilidades de integração.
- Serviços financeiros têm maior risco de regressão; exigem asserts de valores,
  moeda, quantidade, preço médio e histórico, não apenas ausência de exceção.
- Segurança e ownership devem ser validados sem mocks que removam a verificação
  de proprietário.
- A cobertura de ramos de 80% é uma meta acadêmica; o portão atual permanece em
  80% de linhas e 55% de ramos.

## Critério de conclusão

Considerar a meta atingida somente quando uma nova execução real produzir pelo
menos 841 ramos cobertos, mantiver os testes sem falhas e passar
`jacocoTestCoverageVerification`. Os relatórios devem registrar os contadores
reais e os testes ignorados; nenhum ramo deve ser excluído artificialmente.
