## MODIFIED Requirements

### Requirement: Versioned API responses preserve validated financial metadata
The API SHALL expose validated market/listing country, exchange, currency, logo metadata, typed availability/error status, and decimal financial values wherever an asset, position, dashboard aggregate, or integration result is returned. Currency codes SHALL not be inferred by the browser.

#### Scenario: Currency is explicit
- **WHEN** an API returns a Brazilian or US-listed asset
- **THEN** it includes the corresponding currency code (`BRL` or `USD`) and market metadata, and consumers can format the original value without guessing.

#### Scenario: Predictable operation failure
- **WHEN** a request violates market compatibility, references a missing resource, or encounters a provider failure
- **THEN** the API returns the appropriate machine-readable status/code without exposing credentials or stack traces.

### Requirement: External integrations are resilient and classified
Adapters SHALL apply bounded timeouts, retry only transient failures, cache valid responses where appropriate, and distinguish configuration, authorization, plan restriction, rate limit, timeout, not found, mapping, empty, stale, and unavailable states.

#### Scenario: Rate limit does not become empty data
- **WHEN** an external provider returns 429
- **THEN** the API preserves the last valid value when available, marks it stale/rate-limited, and does not silently return an empty successful result.
