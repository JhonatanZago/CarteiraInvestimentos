# Spec Delta

## MODIFIED Requirements

### Requirement: Calculate portfolio indicators
The system SHALL calculate invested value, current value, result, and profitability in the declared portfolio base currency, while preserving original-currency values for each position.

#### Scenario: Dashboard with positions
- **WHEN** a user requests the dashboard for a portfolio with quoted positions
- **THEN** the response includes invested value, current value, result, profitability, original position values, and the base currency used for totals

#### Scenario: Dashboard with mixed currencies
- **WHEN** a portfolio contains positions in BRL and USD and a valid USD/BRL rate exists
- **THEN** the dashboard converts each eligible value once using that rate before calculating consolidated totals

### Requirement: Avoid undefined profitability
The system SHALL return zero or an explicitly non-calculated profitability when invested value is zero or when required currency conversion is unavailable.

#### Scenario: Zero invested value
- **WHEN** a dashboard calculation has zero invested value
- **THEN** the system completes the response without a division-by-zero error

#### Scenario: Missing conversion for profitability
- **WHEN** a consolidated result cannot be calculated because a required exchange rate is unavailable
- **THEN** profitability is represented as unavailable rather than calculated from mixed currencies

### Requirement: Expose quotation freshness
The system SHALL calculate dashboard values from the latest valid stored quotations and return quotation and conversion freshness metadata used by the indicators.

#### Scenario: Display last update
- **WHEN** a user requests a portfolio dashboard after quotations have been stored
- **THEN** the response identifies the latest quotation update and any exchange-rate update used by the indicators

#### Scenario: Stale or unavailable conversion
- **WHEN** an exchange rate is stale or unavailable for a required conversion
- **THEN** the response identifies the affected conversion state and does not present the aggregate as fully current
