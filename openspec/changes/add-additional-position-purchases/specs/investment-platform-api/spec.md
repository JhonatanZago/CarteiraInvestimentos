## ADDED Requirements

### Requirement: Expose authenticated purchase-operation APIs
The system SHALL expose authenticated versioned APIs to create a purchase for an owned portfolio position and list the purchase history of an owned position. A successful create response SHALL return the updated consolidated position and accepted operation.

#### Scenario: Create an idempotent purchase request
- **WHEN** a client submits a valid purchase with a new idempotency key
- **THEN** the API accepts one operation and returns its resulting consolidated position

#### Scenario: Repeat an idempotent purchase request
- **WHEN** a client repeats the same accepted request with its idempotency key
- **THEN** the API returns the original result without adding another purchase
