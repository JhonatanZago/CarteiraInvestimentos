## MODIFIED Requirements

### Requirement: Asset registration validates the confirmed listing market
Asset creation and revalidation SHALL compare the selected market/country with provider-confirmed listing metadata and currency before persistence, including direct API clients, and SHALL reject incompatible or ambiguous symbols with an actionable typed error.

#### Scenario: Incompatible market
- **WHEN** AAPL is submitted for Brazil or PETR4 is submitted for the United States
- **THEN** the API rejects the request with a market-mismatch response and persists no new asset.

#### Scenario: Valid market
- **WHEN** an asset is confirmed on the selected exchange with a compatible currency
- **THEN** the asset is persisted with normalized ticker, listing metadata, original currency, quote time, source, and optional validated logo.

### Requirement: Asset deletion protects financial relationships
An asset SHALL be physically deleted only when no positions, histories, snapshots, movements, or income records reference it; otherwise the API SHALL return a typed conflict and offer a non-destructive archive path where supported.

#### Scenario: Asset has history
- **WHEN** deletion is requested for an asset with financial history
- **THEN** the API returns 409 without cascading deletion, and the UI displays the reason.
