## 1. Portfolio context and HTTP feedback

- [x] 1.1 Implement a shared portfolio-context service that loads available portfolios, restores only a valid persisted selection, and exposes selection changes; verify unit tests reject stale IDs and select the first available portfolio when no selection exists.
- [ ] 1.2 Update routes and navigation so dashboard entry resolves a real portfolio or shows first-portfolio onboarding, with no fixed `/dashboard/1` request; verify route tests cover missing, valid, invalid, and empty portfolio lists.
- [ ] 1.3 Add the silent HTTP-error context token and apply it to dashboard requests with page-managed outcomes; verify interceptor tests show no global toast for silently handled failures.
- [x] 1.4 Deduplicate visible notification messages by message/tone while preserving notifications for unmanaged requests; verify repeated identical errors produce one notification.

## 2. Feature structure and shared UI

- [ ] 2.1 Split dashboard, portfolio, broker, and stock workflows out of the monolithic feature file into focused feature directories with their templates, styles, and tests; verify all existing route paths still resolve and the application builds.
- [ ] 2.2 Extract only behavior-bearing shared components for headers, labels/help/errors, loading skeletons, empty states, status badges, dialogs, and collection controls; verify keyboard focus and `aria-describedby` associations in component tests.
- [x] 2.3 Establish responsive layout tokens and shared interaction styles for primary, secondary, destructive, hover, focus, active, and disabled states; verify required controls remain reachable at desktop, tablet, and 320px viewport widths.

## 3. Interactive dashboard

- [x] 3.1 Rebuild the dashboard header with a named portfolio selector, last-update context, refresh action, and accessible loading state; verify selecting a portfolio updates the URL and reloads its data.
- [x] 3.2 Implement dashboard onboarding, empty-portfolio guidance, primary failure recovery, and independent supplementary failures for indicators, income, and evolution; verify one contextual outcome is shown and valid summary data remains visible.
- [x] 3.3 Render backend-provided summary cards, composition/position data, evolution visualization, and allocation visualization without fabricated values or external chart services; verify empty, single-point, equal-value, and unavailable data cases.

## 4. Portfolio and position workflow

- [ ] 4.1 Rebuild portfolio management with a focused create/edit surface, selectable portfolio cards, selected state, options menu, and confirmed deletion; verify create/edit/delete operations preserve route behavior and successful list refresh.
- [ ] 4.2 Replace numeric position inputs with nullable stock and broker selects populated by typed API services, plus labeled purchase fields and domain help; verify initial controls are null, names/tickers are shown, and submit remains disabled until valid.
- [ ] 4.3 Handle missing stock/broker prerequisites and position save/reset/error states with action links and preserved values; verify no unusable form is displayed when a dependency collection is empty.
- [ ] 4.4 Render the position table with all available backend fields, responsive horizontal access, and scoped loading for save/update/delete actions; verify only the affected action is disabled during its request.

## 5. Broker workflow

- [ ] 5.1 Rebuild broker registration and search as separate labeled surfaces with CNPJ/CEP masks, examples, numeric normalization, touched/submit validation, and accessible field errors; verify submitted payloads contain digits only and invalid forms make no request.
- [ ] 5.2 Map broker validation, duplicate, inactive, unavailable-integration, and pending-validation outcomes to in-form messages while preserving values on failure; verify each stable API error code maps to the expected user-facing message.
- [ ] 5.3 Render broker list cards/table with formatted identity, location, validation status/source/date, empty three-step guidance, and no-result search state; verify successful registration clears the form and refreshes the list without a generic toast.
- [ ] 5.4 Exercise configured broker provider adapters (company, postal address, and financial institution) independently and document/fix only proven configuration or normalization defects; verify backend tests cover the identified failure path without bypassing validation.

## 6. Stock workflow

- [ ] 6.1 Rebuild stock registration with labels, ticker/market explanations, uppercase normalization, examples, and preserved form values on failure; verify ticker normalization and duplicate/not-found/provider-unavailable outcomes.
- [ ] 6.2 Add stock search by ticker/name and market filtering, and render quote value, currency, freshness, source state, history link, and per-row quote refresh loading; verify filtering and scoped refresh tests use backend values only.

## 7. Verification and delivery

- [ ] 7.1 Add route, dashboard, portfolio, broker, stock, notification, accessibility, and responsive component tests for the specified critical flows; verify the complete frontend test suite passes.
- [ ] 7.2 Run frontend `npm ci`, `npm run build`, and `npm test -- --run` (or the project-compatible non-watch equivalent), then run `./gradlew test`/`gradlew.bat test`; verify all commands pass without critical warnings.
- [ ] 7.3 Execute desktop, tablet, and mobile route/API smoke checks against the running backend and frontend, and record remaining external-integration limitations and changed files; verify every acceptance criterion in the source specification has evidence.
