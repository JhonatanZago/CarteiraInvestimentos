## Purpose

Expose a consistent, documented API and frontend boundary for safely managing investment-portfolio information.

## ADDED Requirements

### Requirement: Expose versioned resource APIs
The system SHALL expose the specified broker, stock, portfolio, portfolio-position, quotation-history, and dashboard operations under `/api/v1`.

#### Scenario: API resource request
- **WHEN** a client calls a supported versioned resource endpoint with valid input
- **THEN** the system returns the resource representation and an appropriate HTTP status

### Requirement: Provide paginated collection access
The system SHALL support page and size parameters for stock, broker, and portfolio collection endpoints, ordering stocks by ticker and brokers by corporate name in ascending order by default.

#### Scenario: Request a collection page
- **WHEN** a client requests a supported collection with page and size parameters
- **THEN** the system returns the requested page with the specified default ordering

### Requirement: Return machine-readable errors
The system SHALL return error responses containing timestamp, HTTP status, error, stable code, message, and request path; clients SHALL be able to distinguish validation and external integration failures through the code.

#### Scenario: Domain validation error
- **WHEN** a request violates a domain validation rule
- **THEN** the system returns a structured error with the corresponding stable code

### Requirement: Keep external services behind the backend boundary
The client application SHALL access CNPJ, CEP, financial validation, and quotation data only through this API, and the API SHALL expose no provider-specific response model.

#### Scenario: Frontend quotation use
- **WHEN** the frontend displays a quotation or dashboard indicator
- **THEN** it obtains the data from the versioned backend API

