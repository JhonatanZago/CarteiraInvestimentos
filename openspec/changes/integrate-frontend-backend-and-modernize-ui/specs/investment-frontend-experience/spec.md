## Purpose

Provide a responsive investment-platform experience that lets users manage portfolios through the backend API and understand their investment data with clear feedback and visualizations.

## ADDED Requirements

### Requirement: Consume the backend through typed frontend contracts
The frontend SHALL use typed request and response contracts to consume the versioned backend API for stocks, brokers, portfolios, positions, quotation history, and dashboard data. The frontend SHALL not call external market, company, address, or financial-institution providers directly.

#### Scenario: Load a managed resource
- **WHEN** a user opens a supported management or dashboard screen
- **THEN** the screen requests its data only from the versioned backend API and renders the typed response

#### Scenario: Configure the API endpoint
- **WHEN** the application is configured for a deployment environment
- **THEN** its backend API base URL is provided through centralized frontend configuration

### Requirement: Present backend outcomes consistently
The frontend SHALL present loading, success, empty, validation, domain, and external-integration outcomes consistently without requiring users to inspect the browser console.

#### Scenario: A request is in progress
- **WHEN** a user submits a form or refreshes a quotation
- **THEN** the affected control shows a loading state and prevents duplicate submission until the request completes

#### Scenario: The backend returns a machine-readable error
- **WHEN** the backend returns an error code
- **THEN** the frontend displays an appropriate user-facing notification based on that code

### Requirement: Support investment management flows
The frontend SHALL let users manage stocks, brokers, portfolios, and portfolio positions through the corresponding backend operations, including list navigation, forms, validations, and destructive-action confirmation.

#### Scenario: Register a broker
- **WHEN** a user submits a broker registration form with CNPJ and address details
- **THEN** the frontend submits the data to the backend and displays the returned official broker data or the reported validation outcome

#### Scenario: Manage a portfolio position
- **WHEN** a user creates, changes, or removes a portfolio position
- **THEN** the frontend uses the backend operation and refreshes the affected portfolio data after a successful response

### Requirement: Display backend-provided portfolio insights
The frontend SHALL display portfolio indicators, quotation freshness, historical quotations, and portfolio composition using data returned by the backend. Financial calculations displayed by the frontend SHALL use backend-provided values as their source of truth.

#### Scenario: View a portfolio dashboard
- **WHEN** a user opens a portfolio dashboard with available data
- **THEN** the frontend displays the backend-provided invested value, current value, result, profitability, and last update alongside visual composition insights

#### Scenario: View quotation history
- **WHEN** a user opens quotation history for a stock
- **THEN** the frontend displays the available historical points in a readable table or chart

### Requirement: Provide a responsive investment-platform interface
The frontend SHALL provide responsive navigation, readable data tables, filters or search where collections are available, pagination when supplied by the backend, and accessible feedback states across desktop and mobile layouts.

#### Scenario: Navigate on a small screen
- **WHEN** a user accesses the application on a narrow viewport
- **THEN** navigation, forms, indicator cards, tables, and charts adapt without hiding required actions or data

#### Scenario: Browse a paginated collection
- **WHEN** a user changes page, filter, or sort controls for a supported collection
- **THEN** the frontend requests and renders the corresponding backend page while preserving clear loading and empty states

### Requirement: Verify frontend integration behavior
The frontend SHALL include automated tests for API services, global error handling, representative user-facing loading and error states, and the production build.

#### Scenario: Backend error handling test
- **WHEN** a frontend test simulates a backend error response with a stable code
- **THEN** the test verifies the user-facing error behavior without relying on provider-specific messages
