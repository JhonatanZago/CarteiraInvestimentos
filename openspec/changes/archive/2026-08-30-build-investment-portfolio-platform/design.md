## Context

This repository currently contains the product specification but no application implementation. See [proposal.md](proposal.md) for the product motivation and the capability specs for behavioral contracts. The solution must support Java 21/Spring Boot, Angular, H2 for local development, PostgreSQL for deployment, and several potentially unreliable external HTTP APIs.

## Goals / Non-Goals

**Goals:**

- Establish a layered backend that keeps domain rules, persistence, and external-provider concerns isolated.
- Ensure critical writes are transactional and preserve previously valid quotation data on integration failure.
- Provide stable versioned HTTP contracts that the Angular frontend can consume without provider coupling.
- Make integrations configurable and independently testable.

**Non-Goals:**

- No authentication, users, JWT, dividends, trade ledger, automatic average-price calculation, funds/ETFs, or scheduled quotation refresh in this change.
- No automatic provider fallback for US quotations in the first release.

## Decisions

### Layered modular backend

Organize the backend by the specified resource, service, repository, mapper, facade, and adapter responsibilities. Resources handle HTTP/DTO concerns; services own business rules and transactions; facades select and coordinate integrations; adapters translate provider responses into internal DTOs. This prevents provider DTOs and HTTP concerns from leaking into the domain.

Alternative considered: calling provider clients directly from services. Rejected because provider selection, error mapping, and test doubles would become coupled to domain rules.

### Domain model and integrity at two levels

Persist `Acao`, `Corretora`, `Carteira`, `AtivoCarteira`, and `HistoricoCotacao` with `BigDecimal` monetary fields and database unique constraints for normalized ticker, normalized CNPJ, `(acao, dataHoraCotacao)`, and `(carteira, acao, corretora)`. Services also check uniqueness to give useful errors before database conflicts.

Alternative considered: service-only validation. Rejected because concurrent requests could violate invariants.

### Explicit market-driven quotation adapters

Use an internal quotation contract selected by the stored `Mercado`: BRAPI for Brazil and Alpha Vantage for the US. Twelve Data remains an optional explicit adapter, not an automatic fallback. Every adapter maps transport, timeout, rate-limit, malformed-response, and availability failures to the application's external-error taxonomy.

Alternative considered: a generic provider response exposed to services. Rejected because it would couple domain behavior to unstable provider schemas.

### Transaction boundaries around aggregate writes

Use transactions for stock creation plus initial history, quotation refresh plus history, position changes, and dependent portfolio writes. Persist the stock quotation and its history together; on external failure, perform no quotation mutation.

Alternative considered: separate commits for current quotation and history. Rejected because a partial update would make dashboard results inconsistent.

### Configuration and HTTP resilience

Bind base URLs, tokens, API keys, connection timeouts, and read timeouts from environment-backed application configuration. Keep secrets outside source control. Cache only CEP and CNPJ lookups initially; do not cache quotations.

Alternative considered: hardcoded client configuration or broad caching. Rejected for security and freshness reasons.

### Versioned API and Angular boundary

Expose `/api/v1` request/response DTOs with pageable collection responses and a centralized error body containing a stable `code`. The Angular app uses `HttpClient`, reactive forms, routes, feature modules/components, and consumes only these backend endpoints.

Alternative considered: direct browser calls to data providers. Rejected because it exposes credentials and duplicates business validation.

## Risks / Trade-offs

- [External provider availability and quotas] → Use bounded timeouts, normalized external errors, mocks in tests, and preserve stored quotations on failure.
- [CVM/public-source eligibility criteria may vary] → Encapsulate the selected criterion in the financial-institution adapter and record source plus validation time.
- [Provider rate limits in development] → Keep provider calls behind adapters and supply mocked integration tests.
- [H2/PostgreSQL behavior differences] → Use portable JPA mappings and run integration tests against H2, with a PostgreSQL deployment smoke test.
- [Large initial scope] → Implement in the phases listed in tasks, completing core validation and persistence before portfolio and UI features.

## Migration Plan

1. Create backend and frontend project skeletons with environment-based configuration and local H2 support.
2. Deliver domains, integrity constraints, integrations, and versioned API capabilities in task order.
3. Deploy with PostgreSQL configuration, required integration environment variables, and OpenAPI verification.
4. Roll back by deploying the prior application version; this is an initial greenfield schema, so no data migration is required. Database backups are required before any production schema evolution.

## Open Questions

- The exact public data source and acceptance criterion for financial-institution authorization will be selected during adapter configuration; it does not alter the required observable outcome of accepting only authorized institutions.
