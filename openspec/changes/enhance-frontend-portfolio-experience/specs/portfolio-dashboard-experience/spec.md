## Purpose

Oferecer uma visualização de carteira clara, responsiva e resiliente, baseada exclusivamente nos dados financeiros fornecidos pela API do produto.

## ADDED Requirements

### Requirement: Display a selected portfolio dashboard
The frontend SHALL allow the user to select an available portfolio and display its backend-provided invested value, current value, result, profitability, asset count, consolidated positions, and quotation freshness for that selected portfolio.

#### Scenario: Select another portfolio
- **WHEN** the user selects an available portfolio different from the current one
- **THEN** the application updates the dashboard route and renders data for the newly selected portfolio

#### Scenario: Refresh a portfolio dashboard
- **WHEN** the user requests an update for the selected portfolio
- **THEN** the application reloads its dashboard data and communicates that loading is in progress

### Requirement: Preserve primary dashboard data during supplementary failures
The frontend SHALL render a successfully returned portfolio summary and its positions even when market indicators, income data, or portfolio evolution cannot be obtained. It SHALL identify each unavailable supplementary data set without presenting financial values that were not returned by the backend.

#### Scenario: Market data is unavailable
- **WHEN** the portfolio summary request succeeds and the market-indicator request fails
- **THEN** the summary and positions remain visible and the market-indicator area communicates its unavailability

#### Scenario: Income data is unavailable
- **WHEN** the portfolio summary request succeeds and the income request fails
- **THEN** the summary remains visible and the income area communicates its unavailability

#### Scenario: Primary dashboard request fails
- **WHEN** the selected portfolio dashboard request fails
- **THEN** the application explains that the portfolio could not be loaded and offers a retry action

### Requirement: Communicate dashboard states clearly
The frontend SHALL present a distinct initial loading state, an empty-portfolio state, an unavailable-history state, and partial-unavailability notices. The empty-portfolio state SHALL direct the user to the portfolio-management flow.

#### Scenario: Initial dashboard load
- **WHEN** the application is waiting for the first selected portfolio response
- **THEN** it displays visual loading placeholders instead of blank financial cards

#### Scenario: Portfolio has no positions
- **WHEN** the selected portfolio summary contains no positions
- **THEN** the application explains how to add an asset and position and provides navigation to portfolio management

### Requirement: Visualize returned portfolio insights without external chart services
The frontend SHALL render available evolution and allocation information as understandable visualizations derived from API responses, while remaining usable when either data set is empty. It SHALL not call third-party financial or charting services from the browser.

#### Scenario: Evolution data exists
- **WHEN** the selected portfolio has one or more evolution points
- **THEN** the application displays their relative progression over time

#### Scenario: No evolution data exists
- **WHEN** the selected portfolio has no evolution points
- **THEN** the application displays an explicit no-history state and does not render fabricated points

### Requirement: Provide responsive dashboard navigation and data access
The frontend SHALL provide a responsive primary navigation and dashboard layout in which navigation, portfolio controls, cards, visualizations, and position tables remain available on desktop and narrow screens. Position tables SHALL support horizontal access to columns that cannot fit on a narrow screen.

#### Scenario: Use the dashboard on a narrow screen
- **WHEN** a user accesses the application on a narrow viewport
- **THEN** the navigation can be opened and closed and dashboard content remains readable without hiding required actions or position data

### Requirement: Verify partial-failure behavior
The frontend SHALL include automated coverage that verifies a successful primary dashboard response remains visible when one or more supplementary dashboard requests fail.

#### Scenario: Automated partial-failure check
- **WHEN** a dashboard test simulates successful portfolio data and failing indicator and income responses
- **THEN** the test verifies that primary financial values and a supplementary-data availability notice are rendered
