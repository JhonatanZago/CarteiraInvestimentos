# Spec Delta

## ADDED Requirements

### Requirement: Require authentication for protected resources
The system SHALL require valid authentication for versioned broker, stock, portfolio, position, dashboard, sales, and market-insight resources, while keeping only explicitly documented authentication and public documentation endpoints unauthenticated.

#### Scenario: Unauthenticated protected request
- **WHEN** a client requests a protected `/api/v1` resource without a valid access token
- **THEN** the system returns HTTP 401 with the machine-readable authentication error and performs no domain mutation

#### Scenario: Authenticated protected request
- **WHEN** a client requests a protected resource with a valid access token
- **THEN** the request reaches the existing API contract and returns the same resource representation/status behavior as before hardening

#### Scenario: Forbidden request
- **WHEN** an authenticated client lacks permission for a protected operation
- **THEN** the system returns HTTP 403 with the machine-readable authorization error and does not expose implementation details
