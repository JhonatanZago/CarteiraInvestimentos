# portfolio-dashboard Specification

## Purpose

Provide portfolio-level indicators derived from stored positions and their latest valid stock quotations.

## Requirements

### Requirement: Calculate portfolio indicators
The system SHALL calculate invested value as quantity times average price, current value as quantity times latest quotation, result as current value minus invested value, and profitability as result divided by invested value times one hundred.

#### Scenario: Dashboard with positions
- **WHEN** a user requests the dashboard for a portfolio with quoted positions
- **THEN** the response includes the calculated invested value, current value, result, and profitability

### Requirement: Avoid undefined profitability
The system SHALL return zero or an explicitly non-calculated profitability when invested value is zero.

#### Scenario: Zero invested value
- **WHEN** a dashboard calculation has zero invested value
- **THEN** the system completes the response without a division-by-zero error

### Requirement: Expose quotation freshness
The system SHALL calculate dashboard values from the latest valid stored quotations and return the latest relevant quotation update time.

#### Scenario: Display last update
- **WHEN** a user requests a portfolio dashboard after quotations have been stored
- **THEN** the response identifies the last quotation update used by the indicators
