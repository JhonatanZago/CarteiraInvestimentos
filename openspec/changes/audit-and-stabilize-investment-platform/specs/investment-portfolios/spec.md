## MODIFIED Requirements

### Requirement: Portfolio financial values are currency-aware and precise
Portfolio calculations SHALL use decimal arithmetic, preserve original asset currency and price, convert only through a valid recorded exchange rate for consolidated BRL values, and never add unlike currencies directly.

#### Scenario: Mixed currency portfolio
- **WHEN** a portfolio contains BRL and USD positions
- **THEN** individual values remain in their original currencies and consolidated values identify the exchange rate, timestamp, and source used.

#### Scenario: Exchange unavailable
- **WHEN** no valid exchange rate exists
- **THEN** original currency values remain available, the conversion is marked unavailable/stale, and no fabricated total is produced.

### Requirement: Position lifecycle preserves history
Creating, editing, deleting, or archiving a position SHALL preserve valid historical snapshots and SHALL not create snapshots merely because a dashboard is loaded.

#### Scenario: Repeated daily close
- **WHEN** the same portfolio close is processed more than once for a date
- **THEN** the existing daily snapshot is updated/idempotently reused rather than duplicated.
