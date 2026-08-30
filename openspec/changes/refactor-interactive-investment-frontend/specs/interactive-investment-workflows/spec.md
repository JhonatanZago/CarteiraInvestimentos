## Purpose

Permitir que investidores usem dashboard, carteiras, corretoras e ações sem conhecer identificadores internos ou interpretar detalhes técnicos de integração.

## ADDED Requirements

### Requirement: Resolve a valid portfolio before loading the dashboard
The frontend SHALL resolve a valid available portfolio before requesting portfolio-specific dashboard data. It SHALL persist the selected valid portfolio, use it when still available, and replace an absent or invalid dashboard route with a valid portfolio route. When no portfolios exist, it SHALL present onboarding and SHALL not request dashboard data with an invented identifier.

#### Scenario: Open dashboard without an identifier
- **WHEN** a user opens the dashboard route without a portfolio identifier and portfolios exist
- **THEN** the application selects a saved valid portfolio or the first available portfolio and replaces the URL with that portfolio's dashboard route

#### Scenario: Open an invalid portfolio route
- **WHEN** a user opens a dashboard route whose portfolio does not exist in the available portfolio list
- **THEN** the application replaces the route with a valid available portfolio before requesting its dashboard data

#### Scenario: No portfolios exist
- **WHEN** a user opens the dashboard and no portfolios are available
- **THEN** the application displays guidance to create a first portfolio and makes no portfolio-specific dashboard request

### Requirement: Present contextual dashboard outcomes without duplicate notifications
The frontend SHALL present a maximum of one contextual dashboard outcome for a loading cycle: a primary-error banner or a supplementary-data notice. It SHALL not display repeated global notifications for failures explicitly handled by the dashboard.

#### Scenario: Supplementary requests fail
- **WHEN** indicator, income, and evolution requests fail after a successful dashboard summary
- **THEN** the summary remains visible and the application presents one supplementary-data notice without duplicate global error notifications

### Requirement: Support portfolio and position management by domain names
The frontend SHALL let users create, edit, select, and delete portfolios through a focused form surface and SHALL show a clear dashboard action for the selected portfolio. It SHALL let users add positions by choosing registered stocks by ticker/name and brokers by name, never by entering technical identifiers.

#### Scenario: Add a position with dependencies available
- **WHEN** a user opens the position form with registered stocks and brokers available
- **THEN** stock and broker controls start empty, display human-readable choices, require valid purchase details, and do not show numeric zero defaults

#### Scenario: Position dependency is absent
- **WHEN** no stocks or no brokers are available for a position
- **THEN** the application explains the missing prerequisite and provides navigation to register it instead of displaying an unusable form

### Requirement: Guide broker registration and search
The frontend SHALL provide persistent labels, examples, field-level validation, formatted CNPJ and CEP input, and domain-specific outcomes for broker registration. It SHALL normalize CNPJ and CEP to digits before submission, preserve entered values on failure, and keep registration separate from search.

#### Scenario: Invalid broker form
- **WHEN** a user submits a broker form with invalid CNPJ, CEP, or address number
- **THEN** the application identifies the invalid fields and does not submit the registration

#### Scenario: Backend registration failure
- **WHEN** broker registration is rejected by a validation or external-integration outcome
- **THEN** the application preserves form values and presents the applicable friendly explanation within the registration flow

### Requirement: Provide understandable stock registration and quotation actions
The frontend SHALL explain ticker and market selection, normalize a ticker to uppercase, and preserve the form on registration errors. Stock list entries SHALL show backend-provided quote details and allow only the selected row's quotation update action to enter a loading state.

#### Scenario: Filter stocks
- **WHEN** a user searches by ticker or company name or filters by market
- **THEN** the application shows only matching registered stocks without inventing quotation data

### Requirement: Provide accessible responsive workflow states
The frontend SHALL provide explicit loading, success, empty, and error states for each workflow; persistent labels and associated help/errors for form controls; visible focus; and responsive navigation, forms, cards, and tables for desktop through mobile viewports.

#### Scenario: Submit invalid form
- **WHEN** a user submits an invalid workflow form
- **THEN** focus moves to the first invalid field and its label, help, and error information remain available to assistive technology

#### Scenario: Narrow viewport
- **WHEN** a user accesses any workflow on a narrow viewport
- **THEN** navigation is controllable, actions remain reachable, and wide tables provide horizontal access to their columns
