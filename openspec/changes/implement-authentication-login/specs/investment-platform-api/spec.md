## MODIFIED Requirements

### Requirement: Expose versioned resource APIs
The system SHALL expose the specified broker, stock, portfolio, portfolio-position, quotation-history, and dashboard operations under `/api/v1`, requiring authentication for financial resources and leaving only documented authentication and infrastructure endpoints public.

#### Scenario: Authenticated API resource request
- **WHEN** an authenticated client calls a supported versioned resource endpoint with valid input
- **THEN** the system returns the resource representation and an appropriate HTTP status

#### Scenario: API resource request
- **WHEN** a client calls a supported versioned resource endpoint with valid input
- **THEN** the system returns the resource representation and an appropriate HTTP status

#### Scenario: Unauthenticated financial request
- **WHEN** a client calls a portfolio, stock, position, sale, history, income, or dashboard endpoint without a valid access token
- **THEN** the system returns a JSON HTTP 401 response

### Requirement: Provide paginated collection access
The system SHALL support page and size parameters for stock, broker, and portfolio collection endpoints, ordering stocks by ticker and brokers by corporate name in ascending order by default, while applying ownership filtering to user-scoped collections.

#### Scenario: Request a collection page
- **WHEN** an authenticated user requests a supported collection with page and size parameters
- **THEN** the system returns only records visible to that user with the specified default ordering

### Requirement: Return machine-readable errors
The system SHALL return error responses containing timestamp, HTTP status, error, stable code, message, and request path; clients SHALL be able to distinguish validation, authentication, authorization, and external integration failures through the code.

#### Scenario: Authentication error
- **WHEN** a request has missing, invalid, or expired credentials
- **THEN** the system returns a structured JSON error with HTTP 401 or 403 and a stable code

#### Scenario: Domain validation error
- **WHEN** a request violates a domain validation rule
- **THEN** the system returns a structured error with the corresponding stable code

### Requirement: Keep external services behind the backend boundary
The client application SHALL access CNPJ, CEP, financial validation, and quotation data only through this API, and the API SHALL expose no provider-specific response model.

#### Scenario: Frontend quotation use
- **WHEN** the frontend displays a quotation or dashboard indicator for an authenticated user
- **THEN** it obtains the data from the versioned backend API
