## Purpose

Allow users to register reliable brokers using authoritative company, address, and financial-market validation data.

## ADDED Requirements

### Requirement: Register a validated broker
The system SHALL create a broker only after its CNPJ is normalized, structurally valid, found in the company source, operationally active, and accepted by the configured financial-market source.

#### Scenario: Valid broker registration
- **WHEN** a user submits a valid, authorized broker CNPJ
- **THEN** the system stores the broker with the official company data and validation audit data

#### Scenario: CNPJ cannot be validated
- **WHEN** the CNPJ is invalid, unknown, inactive, or not authorized as a financial institution
- **THEN** the system rejects the registration without persisting a broker

### Requirement: Validate broker address
The system SHALL normalize and validate the supplied CEP using the address source, use its official CEP, street, neighborhood, city, and state values, and accept only number and complement as user-entered address details.

#### Scenario: Existing CEP
- **WHEN** a registration contains a CEP returned by the address source
- **THEN** the stored broker address contains the source-provided location fields

#### Scenario: Unknown CEP
- **WHEN** the address source does not find the normalized CEP
- **THEN** the system rejects the registration without persisting a broker

### Requirement: Keep broker CNPJ unique
The system SHALL prevent more than one broker from being registered with the same normalized CNPJ.

#### Scenario: Duplicate CNPJ
- **WHEN** a user registers a CNPJ already associated with a broker
- **THEN** the system returns a duplicate-resource error and preserves the existing broker

