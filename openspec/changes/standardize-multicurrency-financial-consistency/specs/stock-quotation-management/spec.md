# Spec Delta

## MODIFIED Requirements

### Requirement: Register a market-validated stock
The system SHALL normalize the ticker, require an explicit supported market, validate it through that market's quotation source, and store the returned company, original currency, quotation, and quotation timestamp data.

#### Scenario: Brazilian stock registration
- **WHEN** a normalized ticker exists in the Brazilian quotation source and market is BRASIL
- **THEN** the system creates the stock and its initial quotation-history record with BRL currency

#### Scenario: Invalid market or ticker
- **WHEN** the market is unsupported, the ticker does not exist in its specified market source, or the source omits currency
- **THEN** the system rejects the registration without persisting an ambiguous stock quotation

### Requirement: Update the latest quotation atomically
The system SHALL obtain an existing stock's quotation from the source selected by its stored market, record its source, original currency, and timestamp in history, and update the latest quotation as one operation.

#### Scenario: Successful update
- **WHEN** the selected quotation source returns complete quotation data
- **THEN** the system updates the stock and records the consulted quotation and currency together

#### Scenario: External quotation failure
- **WHEN** the selected quotation source is unavailable, times out, rate-limits, or returns incomplete data
- **THEN** the system reports an external integration error and preserves the prior valid quotation and currency
