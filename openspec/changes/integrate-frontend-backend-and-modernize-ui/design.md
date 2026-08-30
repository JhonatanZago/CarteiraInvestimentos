## Context

The Angular 22 application already uses standalone routes, `HttpClient`, typed API files, and an error interceptor, but its feature screens are consolidated and need complete backend-connected flows and a richer responsive interface. See proposal.md for motivation and the `investment-frontend-experience` spec for observable behavior. The Spring Boot API remains the boundary for all domain rules, calculations, and external-provider communication.

## Goals / Non-Goals

**Goals:**

- Evolve the existing standalone Angular structure into cohesive core, shared, and feature responsibilities.
- Keep all API URLs, contracts, error mapping, notifications, loading state, and collection handling centralized and reusable.
- Build responsive portfolio-management and dashboard experiences using only backend responses as the financial-data source.
- Establish test coverage and release checks for the frontend integration layer.

**Non-Goals:**

- Reimplementing business validation, financial calculations, provider selection, or external API calls in Angular.
- Adding authentication, user accounts, trade ledgers, or new investment-domain backend capabilities.
- Treating chart data unavailable from the backend as a client-side financial calculation; a missing API contract must be identified before implementation.

## Decisions

### Typed API boundary with environment configuration

Keep API models and resource services under the Angular core layer, with the base URL supplied through environment-aware configuration. Components consume resource services rather than constructing HTTP requests.

Alternative considered: local URLs or request logic inside pages. Rejected because it makes deployment configuration and error behavior inconsistent.

### Centralized request feedback

Extend the global error interceptor with a notification service and reusable loading, empty-state, confirmation, and pagination components. Feature screens own only the loading state necessary for their local interactions.

Alternative considered: duplicated alerts and loading flags per page. Rejected because the same backend code could produce inconsistent user feedback.

### Backend-authoritative dashboard and charts

Render dashboard cards from backend-calculated values and derive charts only from collections returned by the API. Use an Angular-compatible charting library for presentation; chart configuration must not introduce financial rules.

Alternative considered: calculate aggregated financial values in the browser. Rejected because it can diverge from the backend source of truth.

### Incremental feature migration

Split the existing feature pages into routed feature components and shared visual primitives incrementally, preserving current routes where possible. Implement resource flows in dependency order: API foundation, stocks and brokers, portfolios and positions, dashboard and history, then visual polish and tests.

Alternative considered: replace the UI in a single rewrite. Rejected because it increases regression risk and makes backend-contract gaps hard to isolate.

## Risks / Trade-offs

- [Backend response lacks data needed for a requested visualization] → identify the missing contract during implementation and propose a backend spec update before adding client-side calculations.
- [Large interface scope delays usable integration] → deliver resource flows and shared feedback primitives before optional visual enhancements.
- [Chart library bundle size and accessibility] → use lazy-loaded feature charts where feasible and provide table or textual equivalents for key data.
- [Backend unavailable during local frontend work] → keep services mockable and cover representative flows with HTTP tests.

## Migration Plan

1. Add or consolidate frontend configuration, typed contracts, API services, and global feedback primitives while preserving existing routes.
2. Migrate resource screens incrementally and verify each against mocked API contracts.
3. Add dashboard/history visualizations and responsive layout behavior after the core management flows are connected.
4. Run frontend tests and production build, then validate end-to-end behavior against a locally running backend.
5. Roll back by deploying the previous frontend build; the change does not require a data migration.
