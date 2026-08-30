## Purpose

Provide normalized, source-safe market context and portfolio insight series for investment-platform clients.

## ADDED Requirements

### Requirement: Expose normalized market indicators
The system SHALL expose configured market indicators with identifier, display name, value, variation, reference timestamp, source freshness, and an explicit availability state without exposing provider credentials or provider-specific payloads.

#### Scenario: Indicator source is available
- **WHEN** a client requests market indicators and a configured source returns a valid value
- **THEN** the response returns normalized numeric values and the source reference timestamp

#### Scenario: Indicator source is unavailable
- **WHEN** a configured market source cannot provide an indicator
- **THEN** the response identifies that indicator as unavailable or stale without fabricating a current value

### Requirement: Expose portfolio evolution and income summaries
The system SHALL provide portfolio evolution points and income-event summaries derived from stored portfolio data or normalized backend sources, with each datum identifying its reference time and availability.

#### Scenario: Portfolio has historical insight data
- **WHEN** a client requests an available time range for a portfolio
- **THEN** the response returns chronologically ordered evolution points and applicable income summary values

#### Scenario: Portfolio has no historical insight data
- **WHEN** no stored or normalized insight data exists for the requested range
- **THEN** the response returns an explicit empty result suitable for an empty-state presentation
