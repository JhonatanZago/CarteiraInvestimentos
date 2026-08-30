## MODIFIED Requirements

### Requirement: Manage validated portfolio positions
The system SHALL allow a portfolio to contain a position only for an existing stock and broker, with quantity and average price greater than zero and a first-purchase date that is not in the future. Each returned position SHALL include the stock identity, latest stored quotation, quotation update time, invested value, current value, result, and profitability calculated by the backend.

#### Scenario: Add valid position
- **WHEN** a user submits valid position data for an existing portfolio, stock, and broker
- **THEN** the system creates the position

#### Scenario: Reject invalid position data
- **WHEN** quantity or average price is not positive, or the first-purchase date is future
- **THEN** the system rejects the position without persisting it

#### Scenario: List position display values
- **WHEN** a client requests a portfolio's positions
- **THEN** each response includes backend-calculated display values and the latest stored quotation used by them
