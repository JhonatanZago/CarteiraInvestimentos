## MODIFIED Requirements

### Requirement: Calculate portfolio indicators
The system SHALL calculate invested value as quantity times average price, current value as quantity times latest quotation, result as current value minus invested value, and profitability as result divided by invested value times one hundred. The dashboard response SHALL include the number of assets and composition entries calculated from the same stored positions and quotations.

#### Scenario: Dashboard with positions
- **WHEN** a user requests the dashboard for a portfolio with quoted positions
- **THEN** the response includes the calculated invested value, current value, result, and profitability

#### Scenario: Dashboard composition
- **WHEN** a user requests the dashboard for a portfolio with positions
- **THEN** the response includes one composition entry per position with stock identity and backend-calculated financial values
