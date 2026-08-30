## Why

The current product presents portfolio aggregates and positions, but it does not yet provide the market context, historical portfolio evolution, income events, or asset classifications needed for an investment-platform experience. The frontend specification requires these values to be stable backend contracts rather than browser calculations or direct provider calls.

## What Changes

- Add backend-owned market indicators, portfolio evolution series, income-event summaries, and portfolio allocation classifications.
- Extend dashboard and position contracts with the data needed for an investment-focused overview, including freshness and allocation metadata.
- Provide responsive frontend views for market context, richer dashboard charts, asset-table filtering, and clearly identified unavailable-data states.
- Keep external-provider selection, credentials, caching, and financial calculations exclusively in Spring Boot.

## Capabilities

### New Capabilities

- `market-investment-insights`: Serve normalized market indicators, portfolio evolution, and income-event summaries through the versioned API.

### Modified Capabilities

- `portfolio-dashboard`: Enrich portfolio insight responses with backend-calculated evolution, allocation, and income summary data.
- `investment-portfolios`: Classify portfolio positions for presentation and expose supported filtering/sorting metadata.
- `investment-platform-api`: Expose the additional versioned insight operations and preserve machine-readable failure behavior.

## Impact

Touches Spring Boot dashboard/position DTOs, services, controllers, persistence or provider adapters as required, OpenAPI documentation, Angular models/services/routes/components, and automated backend/frontend integration tests. New provider-backed data remains optional at runtime and MUST use explicit unavailable/stale states when a configured source cannot supply it.
