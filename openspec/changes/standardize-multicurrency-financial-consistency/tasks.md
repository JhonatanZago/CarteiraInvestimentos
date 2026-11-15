# Tasks

## 1. Monetary contract and legacy inventory

- [x] 1.1 Define the monetary-context DTOs/enums for original currency, base currency, exchange rate, effective timestamp, source, and availability; verify serialization tests cover AVAILABLE, ESTIMATED, and UNAVAILABLE states.
- [x] 1.2 Inventory existing portfolios, positions, operations, and snapshots for missing or unverifiable currency metadata without rewriting data; verify the inventory report classifies every legacy record as resolved or unresolved.
- [x] 1.3 Define additive persistence changes for portfolio base currency and snapshot conversion provenance; verify schema-generation and migration-design tests describe a safe backfill and preserve unresolved legacy rows.

## 2. Backend calculations and historical values

- [x] 2.1 Update exchange-rate access to return validated rate provenance and reject non-positive or missing rates; verify unit tests do not invent a rate when the provider is unavailable.
- [x] 2.2 Update dashboard aggregation to convert each original-currency component before summing in the base currency; verify BRL-only, USD-only, and mixed-currency tests preserve original values and calculate consolidated totals only with a valid rate.
- [x] 2.3 Update snapshots to persist base currency and the exact conversion provenance used at capture time; verify a later rate change does not alter an existing snapshot.
- [ ] 2.4 Preserve operation cost, sale result, and weighted-average semantics while carrying original currency; verify repeated purchase, partial sale, total sale, and historical-cost tests remain unchanged or are explicitly covered by the purchase-operations change.
- [ ] 2.5 Define structured unavailable/estimated responses for missing FX and legacy currency gaps; verify API integration tests expose a stable state without returning a false zero or BRL default.

## 3. API and Angular presentation

- [ ] 3.1 Extend position, dashboard, snapshot, evolution, and quotation-history contracts with explicit currency context using additive fields; verify existing valid response fields remain compatible.
- [ ] 3.2 Remove silent BRL fallbacks from `HistoricoPage`, `CarteirasPage`, and `InsightsDashboardPage`; verify USD values render as USD and unresolved values render an explicit unavailable state.
- [ ] 3.3 Update Angular monetary formatters and models to require currency for financial values and to display conversion provenance/availability; verify unit tests cover BRL, USD, mixed, unavailable, and legacy states.
- [ ] 3.4 Update dashboard allocation, risk, result, evolution, and snapshot views to use backend-converted base values rather than raw cross-currency sums; verify component tests cover mixed-currency percentages and unavailable conversion.

## 4. Integrated validation and documentation

- [ ] 4.1 Add H2/MockMvc integration coverage for BRL, USD, mixed-currency, missing-rate, stale-rate, and unresolved-legacy scenarios; verify no aggregate mixes incompatible units.
- [ ] 4.2 Run backend tests, build, SpotBugs/FindSecBugs, frontend tests, and production build with Java 21 and the configured Node/npm versions; verify all results are recorded without changing security or financial rules outside this change.
- [ ] 4.3 Document the monetary contract, legacy review procedure, unavailable-conversion semantics, rounding policy, and residual risks; verify examples do not contain invented exchange rates or credentials.
- [ ] 4.4 Execute OpenSpec strict validation for this change and review the final diff for absence of authentication, BRAPI, Security Gate, and unrelated migration changes.
