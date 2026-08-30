## Context

The existing Angular feature file combines multiple large inline pages. Dashboard navigation currently assumes a numeric portfolio identifier, and the global HTTP error interceptor notifies before pages can render their own recovery states. The resulting problems span routing, HTTP, form controls, feedback, and page structure. See proposal.md and the workflow spec for expected behavior.

## Goals / Non-Goals

**Goals:**

- Make portfolio selection valid before dependent API requests.
- Separate feature pages into maintainable files while preserving existing API contracts.
- Deliver accessible, responsive workflows using real backend values and contextual failures.

**Non-Goals:**

- Move financial calculations or external-provider access to the browser.
- Bypass broker financial-market validation to make registration succeed.
- Add a charting/UI dependency when existing Angular, CSS, and SVG capabilities suffice.

## Decisions

### Resolve portfolio context before dashboard requests

Introduce a root portfolio-context service backed by local storage. Dashboard list loading validates a route ID against the retrieved portfolio set, then navigates with replaceUrl before calling dashboard, income, or evolution endpoints. This eliminates ID 1 assumptions and requests against known-invalid routes.

Alternative considered: retain a fixed dashboard route and let the dashboard API select a default. This would alter the backend contract and would not support stable deep links.

### Mark page-managed errors as silent HTTP errors

Use an HTTP context token for requests whose UI provides a dedicated error state. The interceptor skips global notification for that context; the notification service also rejects duplicate visible message/tone pairs. Other requests retain current centralized error reporting.

Alternative considered: suppress all interceptor messages. This would hide errors from pages that do not yet provide contextual handling.

### Split feature pages and share behavioral primitives

Create one directory per primary workflow with TS, HTML, SCSS, and tests. Extract form-field, page-header, empty-state, status badge, skeleton, and dialog only where behavior/accessibility is reused. This isolates styles and tests without introducing wrapper-only components.

### Preserve form state and map API errors

Forms use nullable controls and field-level validation. Normalization happens immediately before API submission; API codes are mapped to friendly in-page outcomes while development-only diagnostic details can remain in the console. Requests use finalize and scoped loading signals to prevent duplicate submits.

## Risks / Trade-offs

- [A stored portfolio ID may be stale] → Validate against the freshly loaded portfolio list before use.
- [Different external sources produce inconsistent broker errors] → Preserve the backend code and map known outcomes; log diagnostics in development and document any adapter defect found.
- [Feature extraction can regress routes] → Keep route paths stable, add route-level tests, and migrate one workflow at a time.
- [Responsive tables may be dense on phones] → Use horizontal scroll and retain labeled headers rather than removing financial columns.

## Migration Plan

1. Add the portfolio context and silent-error plumbing, then migrate routes and dashboard selection.
2. Extract and rebuild the four feature workflows with their local tests and shared accessible components.
3. Test broker-provider behavior against configured public sources without weakening validation; make a compatible backend fix only if evidence identifies an adapter/configuration defect.
4. Run frontend and backend tests, production build, and route/API smoke tests; rollback by restoring the prior feature routes and components if necessary.
