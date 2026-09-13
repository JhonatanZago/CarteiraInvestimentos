## Purpose

Provide an auditable acquisition ledger that safely updates a single consolidated position for each portfolio, asset, and broker scope.

## ADDED Requirements

### Requirement: Record an acquisition operation
The system SHALL record every accepted purchase with its authenticated owner's portfolio, asset, broker, quantity, confirmed unit price, currency, operation date, and acquisition fees when applicable.

#### Scenario: Record an additional purchase
- **WHEN** an owner submits a valid purchase for an existing portfolio, asset, and broker position
- **THEN** the system persists a distinct purchase operation and updates that one consolidated position

#### Scenario: Allow intentionally equal purchases
- **WHEN** an owner submits two distinct purchase requests with equal quantity, price, and date
- **THEN** the system records both operations unless they use the same idempotency key

### Requirement: Consolidate remaining acquisition cost
The system SHALL calculate remaining quantity and cost using decimal arithmetic from the remaining position cost plus the effective cost of a new purchase, including acquisition fees exactly once.

#### Scenario: Purchase after a partial sale
- **WHEN** a position with six remaining units and remaining cost 600 receives four units at 80 without fees
- **THEN** the position contains ten units, remaining cost 920, and weighted average cost 92

#### Scenario: Purchase after closing a position
- **WHEN** a zero-quantity position receives a valid purchase
- **THEN** the position starts its new open-balance cost from that purchase while retaining prior operation history and realized sale results

### Requirement: Protect operation ownership and atomicity
The system SHALL resolve the owner from authentication, verify all submitted portfolio, asset, broker, and position links belong to that owner, and commit the purchase and consolidated-position update atomically.

#### Scenario: Reject cross-account purchase
- **WHEN** an authenticated user references another user's portfolio or position
- **THEN** the system rejects the request without persisting an operation or changing a position

#### Scenario: Concurrent purchases
- **WHEN** purchases for the same position are accepted concurrently
- **THEN** the final balance includes each accepted operation without a lost update

### Requirement: Reject unsafe retroactive purchases
The system SHALL either replay later operations chronologically in one transaction or reject a purchase date whose insertion would require replaying later operations.

#### Scenario: Retroactive purchase requiring replay
- **WHEN** a requested purchase predates an existing later operation and chronological replay is unavailable
- **THEN** the system returns a clear validation error and preserves all balances

### Requirement: Provide purchase operation history
The system SHALL allow an owner to retrieve the purchases associated with a consolidated position in operation-date order.

#### Scenario: View persisted purchases
- **WHEN** an owner requests the purchase history of an owned position
- **THEN** the response includes each stored purchase and does not fabricate operations for legacy balances
