## MODIFIED Requirements

### Requirement: Broker identity and logo are independent
Broker registration SHALL continue validating company data by CNPJ while treating logo resolution as optional identity metadata keyed by normalized CNPJ, with HTTPS/local validation and a deterministic fallback.

#### Scenario: Logo source unavailable
- **WHEN** no validated logo exists or an image fails to load
- **THEN** the broker remains usable and the UI displays consistent initials without a broken-image icon.

### Requirement: Broker relationships are preserved
Broker updates, revalidation, and any delete/archive operation SHALL preserve CNPJ identity and financial relationships and SHALL not cascade-delete positions or history.

#### Scenario: Broker is referenced
- **WHEN** a broker is linked to a position
- **THEN** destructive deletion is rejected with a typed conflict and existing financial records remain intact.
