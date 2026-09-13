## Context

The existing model has one `AtivoCarteira` record per portfolio, stock, and broker and stores its quantity, average price, and first-purchase date directly. The same uniqueness constraint currently rejects the second acquisition. Sales already reduce the consolidated balance, so future purchases must use remaining—not original—cost.

## Goals / Non-Goals

**Goals:**

- Add an immutable purchase ledger while retaining a single consolidated position per scope.
- Make the backend authoritative for weighted-cost calculations, ownership, concurrency, and idempotency.
- Preserve legacy position balances and make the Angular purchase experience explicit and reviewable.

**Non-Goals:**

- Reconstructing unrecorded legacy purchases or changing currencies.
- Revaluing acquisition cost from market quotations.
- Silently applying retroactive purchases when chronological replay is not implemented.

## Decisions

### Introduce a purchase-operation aggregate linked to the consolidated position

Each accepted acquisition is persisted with portfolio, asset, broker, position, quantity, unit price, currency, date, fees, quotation snapshot metadata, and request idempotency key. `AtivoCarteira` remains the sole consolidated balance. This preserves the existing uniqueness boundary instead of weakening it or creating duplicate position rows. A ledger is selected over overwriting the position because history, retry safety, and sale-aware accounting require distinct operations.

### Store remaining total cost separately from display average cost

The position stores a high-precision remaining acquisition cost and derives/updates its average with an explicit `BigDecimal` scale and rounding policy. Purchase cost is quantity times confirmed unit price plus fees once. This avoids reconstructing cost from an already rounded displayed average. BRL and USD stay tied to the asset currency and are never converted for this workflow.

### Serialize updates and idempotently retry requests

Purchase creation runs in a transaction and locks or version-checks the consolidated position. A unique owner-and-idempotency-key record returns the prior response for a repeated request, while requests without the same key remain independent—even with identical commercial values. This is selected over value-based deduplication because equal purchases are legitimate.

### Treat retroactive operations as an explicit boundary

The first release accepts purchases on or after the latest recorded operation affecting the position. A dated insertion that would require recomputing later purchases and sales is rejected clearly. Full chronological replay is deliberately deferred rather than allowing inconsistent balances.

### Snapshot accepted market prices

When the UI uses a quote, it submits/receives the verified current quote and timestamp, which become operation data. The stock's current quote remains only the market-value input. This avoids substituting today's quote for historical acquisitions.

## Risks / Trade-offs

- [Legacy positions lack operation rows] → retain their existing remaining cost as an opening balance and never synthesize history.
- [Optimistic locking conflicts during concurrent purchases] → retry or return a controlled conflict; no partial purchase is committed.
- [Retroactive purchase limitation] → state the rejected-date rule in API and UI validation until replay is designed.
- [A client loses an idempotency key] → a retry can become a distinct intentional purchase, which is safer than value-based suppression.

## Migration Plan

1. Add nullable/new ledger and precision fields without deleting or rewriting existing positions.
2. Deploy code that reads legacy balances as an opening balance and writes all new purchases to the ledger.
3. Verify data and API behavior before enabling the UI action.
4. Roll back application code without deleting the additive schema; existing position rows remain intact.
