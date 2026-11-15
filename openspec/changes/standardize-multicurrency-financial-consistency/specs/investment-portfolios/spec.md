# Spec Delta

## MODIFIED Requirements

### Requirement: Manage validated portfolio positions
The system SHALL allow a portfolio to contain a position only for an existing stock and broker, with positive quantity and cost, a non-future first-purchase date, and an explicit or provable original currency.

#### Scenario: Add valid position
- **WHEN** a user submits valid position data for an existing portfolio, stock, broker, and supported currency
- **THEN** the system creates the position while preserving its original currency

#### Scenario: Reject invalid position data
- **WHEN** quantity or average price is not positive, the first-purchase date is future, or currency cannot be established
- **THEN** the system rejects the position without persisting an ambiguous monetary value

### Requirement: Consolidate positions by portfolio, stock, and broker
The system SHALL permit a stock in the same portfolio through different brokers but SHALL preserve one consolidated position per portfolio, stock, and broker without losing the original currency or historical cost context.

#### Scenario: Duplicate consolidated position
- **WHEN** a user adds a position with an existing portfolio, stock, and broker combination
- **THEN** the system returns a duplicate-resource error without merging values from different currencies implicitly

#### Scenario: Legacy position with unresolved currency
- **WHEN** an existing position lacks reliable currency evidence
- **THEN** the position remains readable but is marked unresolved or estimated and is excluded from unsupported base-currency totals
