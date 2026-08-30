## ADDED Requirements

### Requirement: Classify position presentation data
The system SHALL return each portfolio position with a backend-defined presentation classification and market identity suitable for allocation and filtering views, while retaining existing financial display values.

#### Scenario: List classified positions
- **WHEN** a client requests a portfolio's positions
- **THEN** each response identifies the position's market and backend-defined allocation class

### Requirement: Support position collection filtering and ordering
The system SHALL support documented position collection filters and ordering for market, allocation class, ticker, current value, result, and profitability without changing backend-calculated values.

#### Scenario: Filter portfolio positions
- **WHEN** a client requests positions with a supported market or allocation-class filter
- **THEN** the response contains only matching positions and preserves the requested page semantics
