## Context

The current portfolio position response contains identifiers and purchase data only, while the dashboard contains aggregate values only. See proposal.md for the required display contracts. Existing services already calculate aggregate dashboard values from stored quotations.

## Goals / Non-Goals

**Goals:**

- Extend response DTOs without removing existing fields.
- Reuse stored latest quotations and backend calculations for every new displayed financial value.
- Keep a single calculation source so position details and dashboard composition agree.

**Non-Goals:**

- Changing position persistence rules or quotation-refresh behavior.
- Introducing client-side financial calculations or external quotation lookups.

## Decisions

### Enrich read responses without altering write requests

Position create and update requests remain unchanged; read DTOs gain stock display and calculated values. This preserves clients that submit the current request shape.

Alternative considered: introduce separate display endpoints. Rejected because existing portfolio reads already own the required resource boundary.

### Derive composition from existing stored data

The dashboard service will map each position and latest stock quotation into a composition entry, reusing the existing aggregate formula helpers where possible.

Alternative considered: calculate charts in Angular. Rejected because it would duplicate backend financial rules.

## Risks / Trade-offs

- [Position without a valid latest quotation] → return a documented non-calculated or zero-safe value consistent with existing dashboard behavior.
- [Expanded response payload] → limit the added fields to display and calculation data required by consumers.

## Migration Plan

1. Add response DTO fields and composition DTOs.
2. Update mapping and calculation services atomically.
3. Add API and service tests for formulas and response compatibility.
4. Deploy as an additive response-contract change; existing clients continue to ignore new fields.
