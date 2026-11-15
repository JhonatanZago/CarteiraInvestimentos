# Spec Delta

## Purpose

Define a moeda original, a moeda-base e a conversão cambial de cada valor financeiro para que carteiras BRL, USD e mistas sejam calculadas e exibidas sem conversões implícitas ou dados históricos inventados.

## ADDED Requirements

### Requirement: Declare monetary context
Every portfolio-level financial response SHALL declare the original currency when applicable and the portfolio base currency used for consolidated values.

#### Scenario: Mixed-currency portfolio response
- **WHEN** a portfolio contains BRL and USD positions
- **THEN** each original value identifies its currency and consolidated values identify the base currency

### Requirement: Preserve conversion provenance
Every converted amount SHALL identify the valid exchange rate, effective timestamp, and source used for that conversion, when conversion is available.

#### Scenario: Converted USD value
- **WHEN** a USD amount is converted to the BRL base currency
- **THEN** the response includes the converted amount and the rate provenance used to calculate it

### Requirement: Never invent unavailable conversion
The system SHALL not convert a non-base amount when no valid exchange rate is available and SHALL represent the affected aggregate as unavailable or explicitly estimated according to the contract.

#### Scenario: Missing USD/BRL rate
- **WHEN** a mixed-currency aggregate requires USD/BRL conversion and no valid rate exists
- **THEN** the response does not substitute a default rate and declares conversion unavailable

### Requirement: Preserve historical monetary meaning
Historical operations and snapshots SHALL retain their original monetary values and SHALL not be recalculated with a later exchange rate as if it were the historical rate.

#### Scenario: Later quote refresh
- **WHEN** a current exchange rate changes after an operation or snapshot was stored
- **THEN** the stored historical value remains unchanged and the newly observed rate is not applied retroactively

### Requirement: Handle legacy currency gaps explicitly
Legacy records without a provable currency or historical rate SHALL be marked as unresolved or estimated and SHALL not be silently treated as BRL.

#### Scenario: Legacy position without currency evidence
- **WHEN** a legacy position lacks a reliable currency marker
- **THEN** the system preserves the record and exposes its unresolved monetary status for review
