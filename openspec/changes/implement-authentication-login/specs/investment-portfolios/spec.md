## MODIFIED Requirements

### Requirement: Manage portfolios
The system SHALL allow an authenticated user to create, list, retrieve, update, and delete only portfolios owned by that user, with a name and optional description.

#### Scenario: Create portfolio for authenticated user
- **WHEN** an authenticated user submits a valid portfolio name
- **THEN** the system associates the new portfolio with the authenticated user and returns its creation date

#### Scenario: Create portfolio
- **WHEN** a user submits a valid portfolio name
- **THEN** the system creates and returns the portfolio with its creation date

#### Scenario: Prevent cross-user portfolio access
- **WHEN** a user requests, updates, or deletes a portfolio owned by another user
- **THEN** the system returns the adopted not-found or forbidden response and exposes no portfolio data

#### Scenario: Delete non-empty portfolio
- **WHEN** an authenticated user deletes a portfolio with one or more positions
- **THEN** the system returns a conflict error and retains the portfolio and positions

### Requirement: Protect portfolios containing positions
The system SHALL refuse to delete a portfolio that still contains investment positions.

#### Scenario: Delete non-empty portfolio
- **WHEN** an authenticated user deletes a portfolio with one or more positions
- **THEN** the system returns a conflict error and retains the portfolio and positions

### Requirement: Manage validated portfolio positions
The system SHALL allow an authenticated user to manage positions only in an owned portfolio, for an existing stock and broker, with quantity and average price greater than zero and a first-purchase date that is not in the future.

#### Scenario: Add valid position
- **WHEN** an authenticated user submits valid position data for an owned portfolio, stock, and broker
- **THEN** the system creates the position

#### Scenario: Reject invalid or foreign position data
- **WHEN** quantity or average price is not positive, the first-purchase date is future, or the portfolio belongs to another user
- **THEN** the system rejects the position without persisting it

#### Scenario: Reject invalid position data
- **WHEN** quantity or average price is not positive, or the first-purchase date is future
- **THEN** the system rejects the position without persisting it

### Requirement: Consolidate positions by portfolio, stock, and broker
The system SHALL permit a stock in the same owned portfolio through different brokers but SHALL permit only one position for each portfolio, stock, and broker combination.

#### Scenario: Duplicate consolidated position
- **WHEN** an authenticated user adds a position with an existing portfolio, stock, and broker combination
- **THEN** the system returns a duplicate-resource error
