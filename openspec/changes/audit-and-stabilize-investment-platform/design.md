## Context

The repository is a Spring Boot application with JPA/H2 or PostgreSQL persistence and an Angular standalone frontend. The router loads `InsightsDashboardPage` for dashboard routes and `AcoesPage`, `CarteirasPage`, `CorretorasPage`, `HistoricoPage`, and `PositionTablePage` for the other supported routes. Existing OpenSpec changes are complete but cover incremental features rather than a cross-cutting audit baseline.

## Goals / Non-Goals

**Goals:**

- Establish traceable evidence before changing behavior.
- Correct financial integrity, market/currency contracts, lifecycle safety, integrations, and real rendered UI paths incrementally.
- Preserve compatible CRUD routes and prevent data loss.

**Non-Goals:**

- Rewriting the application, replacing working services, adding authentication, or introducing fictitious provider data.
- Implementing any correction during this audit proposal phase.

## Decisions

- Use the existing Spring adapter/facade boundaries for providers; do not call providers from Angular.
- Treat BRL as the consolidated portfolio display currency only when a valid exchange rate exists; retain original position currency.
- Use additive migrations and nullable backfills, with explicit revalidation rather than destructive data rewriting.
- Make route ownership and API-contract evidence a prerequisite for each implementation phase.
- Prefer typed errors and explicit unavailable/stale states over silent fallbacks. A fallback is allowed only for provider failures covered by the specification, never for mapping defects.
- Keep the existing shared asset-logo pattern and add broker-logo behavior through a single reusable component/registry rather than per-template mappings.

## Risks / Trade-offs

- [Historical positions lack transaction-level FX data] → Mark affected calculations as limited/estimated and migrate only provable values.
- [Provider plans/cobertura may remain restricted] → Preserve last valid data and expose an honest unavailable reason; mock providers in tests.
- [Existing inline Angular templates make physical replacement risky] → Verify router usage and compile/tests after each small task.
- [Schema changes must support existing PostgreSQL data] → Use additive nullable columns, backfills, indexes, and tested rollback scripts.

## Migration Plan

1. Capture baseline and freeze evidence.
2. Add compatible contracts and nullable schema fields.
3. Backfill/revalidate only values confirmed by providers.
4. Roll out services and UI behind existing endpoints.
5. Run unit, integration, contract, frontend, smoke, and visual checks.

Rollback consists of disabling the new adapters/UI derivations, retaining additive columns, and restoring the previous response mapping; no existing records are deleted.
