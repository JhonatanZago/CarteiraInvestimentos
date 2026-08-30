## MODIFIED Requirements

### Requirement: Expose versioned resource APIs
The system SHALL expose the specified broker, stock, portfolio, portfolio-position, quotation-history, and dashboard operations under `/api/v1`, and frontend clients SHALL use that relative versioned path so a same-origin proxy can route requests to the API.

#### Scenario: API resource request
- **WHEN** a client calls a supported versioned resource endpoint with valid input
- **THEN** the system returns the resource representation and an appropriate HTTP status

#### Scenario: Proxied frontend API request
- **WHEN** the Angular application requests a supported endpoint through `/api/v1`
- **THEN** the request reaches the API without requiring an absolute backend URL in browser configuration
