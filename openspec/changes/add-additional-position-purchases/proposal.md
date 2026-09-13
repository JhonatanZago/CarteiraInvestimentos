## Why

The platform currently treats the creation of a consolidated position as the only acquisition event. Its uniqueness rule therefore rejects a legitimate second purchase of the same asset, preventing accurate average-cost accounting and an auditable acquisition history.

## What Changes

- Introduce durable purchase operations for portfolio positions, including quantity, execution price, operation date, currency, broker, and acquisition fees.
- Add a purchase endpoint and UI flows for registering an additional purchase from a position or from the portfolio form, while keeping one consolidated position per portfolio, asset, and broker.
- Calculate and persist remaining cost and weighted average cost from acquisition operations; support subsequent purchases after partial sales or a zero balance.
- Snapshot an accepted live quotation as the purchase price when that option is selected, without allowing later quotation refreshes to alter acquisition cost.
- Add ownership, transactional concurrency, request-idempotency, validation, and operation-history behavior for purchases.

## Capabilities

### New Capabilities

- `portfolio-purchase-operations`: Records immutable acquisition operations and applies them safely to a consolidated portfolio position.

### Modified Capabilities

- `investment-portfolios`: Positions are created and updated through purchase operations while retaining their portfolio, asset, and broker uniqueness scope.
- `stock-quotation-management`: A quotation selected for a purchase is retained as an operation-price snapshot and remains separate from current market valuation.
- `investment-platform-api`: Adds authenticated, versioned purchase-operation resources and their API contracts.

## Impact

- Backend domain entities, database schema/migration, repositories, portfolio services, sale calculations, security, controllers, DTOs, and tests.
- Angular portfolio forms, API clients, models, position actions, previews, notifications, and tests.
- Existing positions and sale history must remain readable without inventing historical operations.
