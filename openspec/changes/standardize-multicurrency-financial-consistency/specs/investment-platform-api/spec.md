# Spec Delta

## MODIFIED Requirements

### Requirement: Return machine-readable errors
The system SHALL return error responses containing timestamp, HTTP status, error, stable code, message, and request path; clients SHALL be able to distinguish validation, external integration, and unavailable currency-conversion failures through the code.

#### Scenario: Domain validation error
- **WHEN** a request violates a domain validation rule
- **THEN** the system returns a structured error with the corresponding stable code

#### Scenario: Conversion unavailable
- **WHEN** a requested aggregate requires a missing or invalid exchange rate
- **THEN** the system returns the documented availability state or stable conversion error without substituting a monetary default

### Requirement: Keep external services behind the backend boundary
The client application SHALL access quotation and exchange-rate data only through this API, and the API SHALL expose normalized currency, conversion provenance, and availability fields without provider-specific response models.

#### Scenario: Frontend quotation use
- **WHEN** the frontend displays a quotation, exchange rate, or dashboard indicator
- **THEN** it obtains the value and its monetary context from the versioned backend API
