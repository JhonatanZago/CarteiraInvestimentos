# Proposal

## Why

Carteiras com ativos BRL e USD ainda podem exibir moeda implícita, somar valores sem conversão histórica e gerar snapshots sem registrar a taxa utilizada. Isso ameaça a interpretação do patrimônio, risco e rentabilidade; a correção precisa definir um contrato monetário explícito antes de qualquer mudança de código ou migration.

## What Changes

- Definir moeda original, moeda-base da carteira, taxa de câmbio, data e fonte da conversão.
- Impedir fallback silencioso de valores sem moeda para BRL.
- Separar valores originais de valores convertidos e declarar quando a conversão estiver indisponível.
- Tornar snapshots e indicadores compatíveis com carteiras BRL, USD e mistas.
- Preservar custo histórico e arredondamento das operações já registradas.
- Formalizar compatibilidade para posições legadas sem inventar moeda ou taxa histórica.
- Cobrir backend, DTOs, snapshots, dashboard, histórico e formatadores Angular com testes de contrato e integração.

## Capabilities

### New Capabilities

- `portfolio-currency-consistency`: contrato explícito para moeda original, moeda-base, conversão cambial e disponibilidade dos valores financeiros.

### Modified Capabilities

- `portfolio-dashboard`: indicadores, composição, risco e evolução devem declarar e respeitar a moeda-base e a disponibilidade de conversão.
- `investment-portfolios`: posições e operações devem preservar moeda original e custo histórico sem defaults monetários silenciosos.
- `investment-platform-api`: DTOs e erros devem transportar moeda, conversão e indisponibilidade de forma determinística.
- `stock-quotation-management`: cotações e histórico devem continuar transportando a moeda de origem em todos os contratos.

## Impact

- Backend: `PortfolioSnapshotService`, `DashboardCarteiraService`, `ExchangeRateService`, DTOs, mappers, movimentações e repositórios.
- Frontend: `HistoricoPage`, `CarteirasPage`, `InsightsDashboardPage`, modelos, APIs e formatadores monetários.
- Testes unitários, MockMvc, integração H2 e testes Angular.
- Possíveis migrations aditivas para preservar moeda-base e metadados cambiais de snapshots; nenhuma migration será feita nesta etapa de planejamento.
- Nenhum impacto esperado em autenticação, Security Gate, BRAPI ou regras de autorização.
