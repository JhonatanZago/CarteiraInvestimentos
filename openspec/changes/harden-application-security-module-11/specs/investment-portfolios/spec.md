# Spec Delta

## ADDED Requirements

### Requirement: Isolate portfolio data by authenticated owner
The system SHALL scope portfolio, position, sale, dashboard, evolution, income, and currency-analysis reads and writes to the authenticated portfolio owner.

#### Scenario: Owner accesses portfolio data
- **WHEN** the authenticated owner requests one of their portfolios or nested financial resources
- **THEN** the system returns or mutates only that owner's data using the existing financial rules

#### Scenario: User accesses another user's portfolio
- **WHEN** an authenticated user supplies the identifier of a portfolio owned by another user
- **THEN** the system returns the documented not-found or authorization response and performs no read-visible or persistent mutation

#### Scenario: Missing ownership context
- **WHEN** a protected portfolio operation has no authenticated user context
- **THEN** the system rejects the operation before accessing or mutating portfolio data
