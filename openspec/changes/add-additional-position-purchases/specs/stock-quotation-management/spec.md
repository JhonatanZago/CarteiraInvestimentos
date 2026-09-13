## ADDED Requirements

### Requirement: Preserve quotation-based purchase snapshots
The system SHALL store the price, currency, and timestamp actually accepted when a user selects a current quotation for a purchase; later quotation refreshes SHALL affect market valuation only and SHALL NOT alter an operation's acquisition price or a position's weighted average cost.

#### Scenario: Confirm a live quotation for a purchase
- **WHEN** a user chooses the current quotation while registering a purchase
- **THEN** the resulting operation retains that confirmed price and quotation timestamp

#### Scenario: Refresh a quotation after purchase
- **WHEN** a stock quotation changes after a purchase is recorded
- **THEN** the position's market value can change while its remaining acquisition cost and weighted average cost remain unchanged
