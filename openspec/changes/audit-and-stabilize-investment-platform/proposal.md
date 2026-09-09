## Why

The platform has accumulated incremental frontend and integration changes without a single verified contract. This audit will turn confirmed inconsistencies in market validation, multi-currency calculations, lifecycle operations, dashboard data, integrations, and UI accessibility into an incremental, testable correction program.

## What Changes

- Establish an evidence-based baseline for the real Spring Boot and Angular routes, contracts, integrations, and tests.
- Normalize asset market, listing country, exchange, currency, logo, and validation behavior at the backend boundary.
- Introduce a migration-safe model for transaction-level currency/cost data and correct consolidated BRL calculations.
- Stabilize portfolio snapshots, dashboard evolution, distribution, risk, and result views using real data only.
- Harden external adapters with typed failures, timeout, retry, cache, fallback, and safe observability.
- Complete safe delete/archive flows, logo resolution, responsive UI states, accessibility, and regression coverage.

## Capabilities

### New Capabilities
- `investment-audit-baseline`: Evidence and traceability for architecture, routes, contracts, and validation outcomes.

### Modified Capabilities
- `investment-platform-api`: Correct and document versioned DTOs, errors, currency, market metadata, and lifecycle responses.
- `investment-portfolios`: Preserve transaction and snapshot integrity while calculating portfolio values across currencies.
- `portfolio-dashboard`: Use real snapshots and currency-aware aggregates for evolution, distribution, risk, and result panels.
- `stock-quotation-management`: Validate market metadata, currency, logos, refresh, revalidation, and safe deletion/archive.
- `broker-registration`: Preserve validated company data while adding safe broker logo and relationship behavior.

## Impact

Touches Spring domain/DTO/mapper/repository/service/adapter/controller layers, database migrations, Angular route components (`pages.ts`, `insights-dashboard.page.ts`, `position-table.page.ts`), shared logo/formatting components, global styles, environment configuration, automated tests, and operational documentation. Existing CRUD routes and valid payloads remain compatibility constraints.
