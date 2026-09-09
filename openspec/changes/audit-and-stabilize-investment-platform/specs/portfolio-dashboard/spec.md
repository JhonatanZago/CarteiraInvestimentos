## MODIFIED Requirements

### Requirement: Dashboard uses real, distinct financial series
The dashboard SHALL derive portfolio evolution from stored snapshots, distinguish current amount from result, and calculate distribution, risk, and result by the validated currency-aware position values.

#### Scenario: Result and patrimony differ
- **WHEN** invested capital changes independently from market performance
- **THEN** the result series uses current amount minus invested amount while the patrimony series uses current amount, without treating contributions as profit.

#### Scenario: Insufficient history
- **WHEN** the selected period has zero or one real snapshot
- **THEN** the dashboard shows the corresponding empty or forming-history state and does not fabricate points or a trend.

### Requirement: Dashboard interactions are bounded to their panels
Charts, tooltips, filters, lists, and tables SHALL remain within their cards, use accessible labels, and avoid page-level horizontal overflow in supported responsive layouts.

#### Scenario: Tooltip interaction
- **WHEN** a pointer enters, moves across, leaves, or touches a chart
- **THEN** the tooltip and reference marker update without changing layout dimensions, duplicating requests, or producing invalid coordinates.
