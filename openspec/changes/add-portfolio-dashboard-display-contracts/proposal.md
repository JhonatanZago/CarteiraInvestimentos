## Why

The frontend needs backend-authoritative position and composition data to display investment results without duplicating financial calculations in the browser. The current aggregate dashboard and position responses do not contain the stock identity, current quotation, per-position results, asset count, or composition required by the planned interface.

## What Changes

- Enrich portfolio-position responses with backend-calculated display data, including stock identity, quotation freshness, invested value, current value, result, and profitability.
- Enrich the portfolio dashboard response with asset count and backend-calculated composition entries suitable for tables and charts.
- Preserve existing aggregate dashboard fields and position CRUD behavior.
- Add service and API tests that verify calculation accuracy, zero-investment behavior, and response contracts.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `investment-portfolios`: portfolio position responses will expose backend-authoritative display and result information.
- `portfolio-dashboard`: dashboard responses will expose asset count and composition data derived from stored positions and latest quotations.

## Impact

Changes backend DTOs, mappers, dashboard and position services, and the `/api/v1/carteiras/{carteiraId}/posicoes` and `/api/v1/dashboard/carteiras/{carteiraId}` response contracts. The Angular change can then render position results and composition without client-side financial calculations.
