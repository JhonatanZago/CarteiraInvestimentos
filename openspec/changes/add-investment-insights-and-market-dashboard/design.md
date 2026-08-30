## Context

The existing dashboard calculates current aggregates from persisted positions and latest quotes. See proposal.md and delta specs: the additional investment-platform views require historical, market, allocation, and income data that is not currently represented by a stable API contract.

## Goals / Non-Goals

**Goals:**

- Add server-owned insight contracts that the Angular client can render without duplicating financial rules or accessing providers.
- Preserve additive compatibility for existing `/api/v1` consumers.
- Make freshness, empty data, and provider unavailability explicit in responses.

**Non-Goals:**

- Authentication, personal user profiles, trade-lot accounting, or execution of brokerage trades.
- Treating unavailable provider data as a fabricated real-time value.
- Storing provider tokens in the frontend.

## Decisions

### Use normalized insight DTOs with availability metadata

Every external or historical insight response will carry reference time and availability/staleness information. This lets the UI differentiate absent data from a zero financial value.

Alternative considered: UI fallback values. Rejected because it can be interpreted as real market or income data.

### Keep portfolio calculations and classifications in the backend

Portfolio evolution, allocation classes, and ordered/filterable position results are calculated or assigned by the backend, then rendered by Angular.

Alternative considered: categorizing and aggregating in the browser. Rejected because it duplicates domain rules and causes incompatible clients.

### Isolate provider collection behind adapters and cached refreshes

Configured provider adapters own source selection, timeouts, rate limiting, and cache behavior. Controllers return normalized DTOs only.

Alternative considered: browser-provider calls. Rejected because it exposes credentials and bypasses failure handling.

## Risks / Trade-offs

- [A source lacks an indicator or income series] → return a documented unavailable state and retain the last valid reference only when clearly marked stale.
- [Historical data is incomplete] → expose the returned range and empty state; do not interpolate financial points.
- [More dashboard queries] → use bounded cache/refresh policies and batch portfolio reads where possible.

## Migration Plan

1. Add additive DTOs, endpoint documentation, and backend tests for availability states.
2. Add provider/cache adapters and persist or derive the required historical data.
3. Update Angular contracts and feature views with loading, stale, empty, and error states.
4. Deploy additively; rollback by withholding the new UI routes while existing dashboard responses remain compatible.
