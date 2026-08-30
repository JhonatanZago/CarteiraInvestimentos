## 1. Backend insight contracts

- [x] 1.1 Define additive DTOs for market indicators, availability/freshness, evolution points, income summaries, and position allocation classification; verify JSON serialization and OpenAPI schemas expose numeric values and reference timestamps.
- [x] 1.2 Add versioned market-indicator, portfolio-evolution, and portfolio-income API operations with stable empty/unavailable responses; verify controller tests cover available, empty, and unavailable cases.
- [x] 1.3 Extend dashboard and position responses with additive insight/allocation fields and supported filtering/ordering parameters; verify service and MockMvc tests preserve existing response fields and page semantics.

## 2. Insight sourcing and calculations

- [x] 2.1 Implement backend-owned indicator sourcing through normalized adapters with bounded caching, timeout/error mapping, and no provider payload leakage; verify adapter tests cover stale and source-unavailable results.
- [x] 2.2 Implement chronological portfolio evolution and income summary derivation from stored data or configured normalized sources without browser-side financial calculations; verify positive, negative, zero, and empty-result tests.
- [x] 2.3 Assign documented allocation classifications and calculate allocation values from the same stored positions and quotations used by the dashboard; verify multi-class portfolio tests and zero-safe behavior.

## 3. Investment-platform frontend

- [x] 3.1 Extend typed Angular API models and services for insight, allocation, filtering, and availability contracts; verify HTTP service tests assert URLs, query parameters, and typed responses.
- [x] 3.2 Build the responsive market-aware dashboard with indicator strip, summary cards, allocation visualization, evolution chart/table fallback, income panels, and explicit stale/empty states; verify component tests cover available, stale, empty, and negative-result rendering.
- [x] 3.3 Upgrade the portfolio position table with search, market/class/result filters, supported ordering, pagination, freshness labels, and backend-provided values; verify interactions request the expected backend query and render the returned page.
- [x] 3.4 Refine the application shell, header, and broker/asset flows to match the financial visual system while preserving accessibility, keyboard interaction, loading, retry, confirmation, and error feedback; verify representative component tests at desktop and mobile viewports.

## 4. Release verification

- [x] 4.1 Run backend unit, controller, integration, and OpenAPI smoke tests; verify existing `/api/v1` contracts remain compatible and new insight contracts document unavailable states.
- [x] 4.2 Run Angular unit tests and production build; verify no external-provider credential or direct provider URL appears in frontend source.
- [x] 4.3 Run the Angular frontend with the Spring Boot backend and perform browser smoke flows for dashboard, portfolio filtering, quotation freshness, and unavailable insight states; verify no CORS, console, or HTTP integration errors.
