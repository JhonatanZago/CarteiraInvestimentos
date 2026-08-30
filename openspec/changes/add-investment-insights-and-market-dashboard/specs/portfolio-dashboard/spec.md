## MODIFIED Requirements

### Requirement: Calculate portfolio indicators
The system SHALL calculate invested value as quantity times average price, current value as quantity times latest quotation, result as current value minus invested value, and profitability as result divided by invested value times one hundred. The dashboard response SHALL include the number of assets and composition entries calculated from the same stored positions and quotations, and SHALL expose any available backend-provided income summary and insight freshness metadata.

#### Scenario: Dashboard with positions
- **WHEN** a user requests the dashboard for a portfolio with quoted positions
- **THEN** the response includes the calculated invested value, current value, result, profitability, composition, and available income summary

#### Scenario: Dashboard composition
- **WHEN** a user requests the dashboard for a portfolio with positions
- **THEN** the response includes one composition entry per position with stock identity and backend-calculated financial values

#### Scenario: Dashboard without income data
- **WHEN** a requested portfolio has no available income events
- **THEN** the dashboard identifies the income summary as unavailable or empty without representing an estimated value as actual income
