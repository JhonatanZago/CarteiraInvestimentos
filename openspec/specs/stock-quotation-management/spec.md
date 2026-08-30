# stock-quotation-management Specification

## Purpose

Maintain validated Brazilian and US stock records with their most recent quotation and a durable history of consulted prices.

## Requirements

### Requirement: Register a market-validated stock
The system SHALL normalize the ticker, require an explicit supported market, validate it through that market's quotation source, and store the returned company, currency, quotation, and quotation timestamp data.

#### Scenario: Brazilian stock registration
- **WHEN** a normalized ticker exists in the Brazilian quotation source and market is BRASIL
- **THEN** the system creates the stock and its initial quotation-history record

#### Scenario: Invalid market or ticker
- **WHEN** the market is unsupported or the ticker does not exist in its specified market source
- **THEN** the system rejects the registration without persisting a stock

### Requirement: Maintain unique stocks and quotation history
The system SHALL keep tickers unique after normalization and SHALL not persist the same stock quotation timestamp more than once.

#### Scenario: Duplicate ticker
- **WHEN** a user registers an already stored normalized ticker
- **THEN** the system returns a duplicate-resource error

#### Scenario: Repeated quotation point
- **WHEN** a quotation is received for an existing stock and quotation timestamp
- **THEN** the system does not create a duplicate history record

### Requirement: Update the latest quotation atomically
The system SHALL obtain an existing stock's quotation from the source selected by its stored market, record its source and timestamp in history, and update the latest quotation as one operation.

#### Scenario: Successful update
- **WHEN** the selected quotation source returns complete quotation data
- **THEN** the system updates the stock and records the consulted quotation together

#### Scenario: External quotation failure
- **WHEN** the selected quotation source is unavailable, times out, rate-limits, or returns incomplete data
- **THEN** the system reports an external integration error and preserves the prior valid quotation
