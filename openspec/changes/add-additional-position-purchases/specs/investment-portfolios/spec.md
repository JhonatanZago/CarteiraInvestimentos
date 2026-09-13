## MODIFIED Requirements

### Requirement: Manage validated portfolio positions
The system SHALL allow a portfolio to contain a position only for an existing stock and broker, with a positive quantity and cost while open. Position creation and subsequent acquisition SHALL be represented by purchase operations whose operation date is not in the future; the first-purchase date is derived from the earliest recorded purchase when available.

#### Scenario: Add valid position
- **WHEN** a user submits a valid initial purchase for an existing portfolio, stock, and broker
- **THEN** the system creates one consolidated position and its purchase operation

#### Scenario: Reject invalid position data
- **WHEN** a purchase quantity or unit price is not positive, or its operation date is future
- **THEN** the system rejects the purchase without persisting an operation or changing the position

### Requirement: Consolidate positions by portfolio, stock, and broker
The system SHALL permit a stock in the same portfolio through different brokers but SHALL maintain only one consolidated position for each portfolio, stock, and broker combination. A later purchase in an existing combination SHALL update that consolidated position and SHALL NOT be rejected as a duplicate position.

#### Scenario: Duplicate consolidated position
- **WHEN** a user buys an asset already held in the same portfolio and broker
- **THEN** the system records a new purchase and retains one consolidated position in that scope
