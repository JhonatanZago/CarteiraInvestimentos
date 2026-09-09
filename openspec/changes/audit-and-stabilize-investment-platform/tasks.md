## 0. Diagnóstico e baseline

- [x] 0.1 Consolidate backend layer inventory (domains, DTOs, enums, repositories, mappers, services, facades, adapters, controllers, errors, configuration, migrations) and verify paths against the repository.
- [x] 0.2 Consolidate frontend route/component/service/endpoint inventory, explicitly verifying `app.routes.ts`, `pages.ts`, `insights-dashboard.page.ts`, and `position-table.page.ts` ownership.
- [x] 0.3 Record baseline results for `./gradlew test`, `npx tsc -p tsconfig.app.json --noEmit`, `npm test -- --watch=false`, and `npm run build`, including warnings and skipped tests.
- [x] 0.4 Produce the field-level screen/source/currency/formula matrix and classify every finding as confirmed defect, risk, debt, hypothesis, or optional improvement.

## 1. Contratos e integridade

- [x] 1.1 Compare Java DTOs/mappers with TypeScript interfaces and add contract tests for market, country, exchange, currency, logo, status, and decimal fields.
- [x] 1.2 Define typed API errors and verify 400/404/409/422/502 behavior without stack traces or secrets.
- [x] 1.3 Design additive migrations/indexes for currency, listing metadata, logos, transaction FX, and snapshot idempotency; verify upgrade and rollback on a copy of the schema.

## 2. Cadastro validado de ativos

- [x] 2.1 Make selected country/market mandatory and validate confirmed listing metadata in the backend before persistence; verify PETR4/BR, PETR4/US, AAPL/US, AAPL/BR, ITUB4/BR, ITUB/US, and AAPL34/BR scenarios.
- [x] 2.2 Handle duplicate, ambiguous, unsupported, unavailable, and incomplete provider responses with actionable errors and no persistence.
- [x] 2.3 Revalidate existing assets safely, preserving IDs/relationships and recording before/after metadata; verify batch results.

## 3. Movimentações e câmbio

- [x] 3.1 Introduce transaction-level original price, currency, FX rate/time/source, costs, and decimal calculations with compatibility for current positions.
- [x] 3.2 Implement and test multi-buy, partial sell, full sell, realized/unrealized result, and historical/current FX formulas.
- [x] 3.3 Mark legacy positions without provable historical FX as limited/estimated and verify no direct BRL+USD summation.

## 4. Carteiras

- [x] 4.1 Correct position and consolidated DTOs/mappers to expose original and converted values with explicit currency and FX metadata.
- [x] 4.2 Add safe edit/delete/archive behavior and relationship checks; verify no cascade removes positions, history, or snapshots.
- [x] 4.3 Make snapshot creation event-driven and daily-idempotent; verify page loads do not create snapshots.

## 5. Visão geral

- [x] 5.1 Derive dashboard evolution/result series from real snapshots and verify empty, one-point, equal-value, ordered, and multi-point histories.
- [x] 5.2 Correct distribution, risk, profit/loss, tooltip, period filters, and mixed-currency calculations with no NaN/Infinity or duplicate HTTP calls.
- [x] 5.3 Verify the actual dashboard route visually at desktop/mobile widths and both themes for clipping, overflow, and accessible states.

## 6. Ativos e corretoras

- [x] 6.1 Centralize dynamic money formatting and connect validated asset/broker logos plus deterministic fallbacks on every real rendered screen.
- [x] 6.2 Complete asset and broker revalidation, delete conflict, and archive UI flows with loading, confirmation, and actionable errors.
- [x] 6.3 Verify image URL security, lazy loading, failure handling, CNPJ/ticker exact matching, and no broken-image layout shifts.

## 7. Integrações externas

- [x] 7.1 Audit each adapter/facade (BrasilAPI, ViaCEP, brapi, American source, BCB/PTAX, regulatory source) for timeout, retry, cache, rate-limit, mapping, and safe logs.
- [x] 7.2 Add provider mocks and tests for missing/invalid token, 401/403/404/429/5xx, timeout, partial response, stale cache, and fallback selection.

## 8. Frontend e acessibilidade

- [x] 8.1 Remove duplicated/unused rendered blocks only after route verification and preserve the approved current components.
- [x] 8.2 Standardize themes, contrast, focus, labels, dialogs, loading/empty/error states, table scrolling, and responsive min-width behavior.
- [x] 8.3 Add component tests for currency, logos, selection, deletion, dashboards, tooltip interaction, and no page-level overflow.

## 9. Testes e entrega

- [x] 9.1 Run backend unit/integration/contract tests with external APIs mocked and verify decimal precision and migration behavior.
- [x] 9.2 Run frontend typecheck, unit tests, production build, and route smoke tests against the real running app.
- [x] 9.3 Update configuration/documentation with required environment variables, source/licensing notes, limitations, rollback, and operational verification evidence.
