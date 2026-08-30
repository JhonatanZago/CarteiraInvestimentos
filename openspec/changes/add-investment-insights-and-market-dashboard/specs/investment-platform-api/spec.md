## MODIFIED Requirements

### Requirement: Expose versioned resource APIs
The system SHALL expose the specified broker, stock, portfolio, portfolio-position, quotation-history, dashboard, market-indicator, portfolio-evolution, and portfolio-income operations under `/api/v1`.

#### Scenario: API resource request
- **WHEN** a client calls a supported versioned resource endpoint with valid input
- **THEN** the system returns the resource representation and an appropriate HTTP status

#### Scenario: Insight resource request
- **WHEN** a client requests market or portfolio insight data
- **THEN** the system returns a normalized response with reference and availability metadata
