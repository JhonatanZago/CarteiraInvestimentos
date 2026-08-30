## Purpose

Enable users to organize validated stocks into portfolios and maintain consolidated investment positions by broker.

## ADDED Requirements

### Requirement: Manage portfolios
The system SHALL allow users to create, list, retrieve, and update portfolios with a name and optional description.

#### Scenario: Create portfolio
- **WHEN** a user submits a valid portfolio name
- **THEN** the system creates and returns the portfolio with its creation date

### Requirement: Protect portfolios containing positions
The system SHALL refuse to delete a portfolio that still contains investment positions.

#### Scenario: Delete non-empty portfolio
- **WHEN** a user deletes a portfolio with one or more positions
- **THEN** the system returns a conflict error and retains the portfolio and positions

### Requirement: Manage validated portfolio positions
The system SHALL allow a portfolio to contain a position only for an existing stock and broker, with quantity and average price greater than zero and a first-purchase date that is not in the future.

#### Scenario: Add valid position
- **WHEN** a user submits valid position data for an existing portfolio, stock, and broker
- **THEN** the system creates the position

#### Scenario: Reject invalid position data
- **WHEN** quantity or average price is not positive, or the first-purchase date is future
- **THEN** the system rejects the position without persisting it

### Requirement: Consolidate positions by portfolio, stock, and broker
The system SHALL permit a stock in the same portfolio through different brokers but SHALL permit only one position for each portfolio, stock, and broker combination.

#### Scenario: Duplicate consolidated position
- **WHEN** a user adds a position with an existing portfolio, stock, and broker combination
- **THEN** the system returns a duplicate-resource error

