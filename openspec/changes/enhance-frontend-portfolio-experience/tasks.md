## 1. Dashboard data flow

- [x] 1.1 Add or complete typed portfolio-list access and load the available portfolios for the dashboard selector; verify that a successful `GET /api/v1/carteiras` response populates selectable portfolio names.
- [x] 1.2 Rework dashboard loading so the primary portfolio summary governs the page error while indicators, evolution, and income have independent fallback outcomes; verify that a successful summary remains rendered when each supplementary request fails.
- [x] 1.3 Bind the selected portfolio to the dashboard route and add manual refresh and retry actions; verify route navigation reloads the selected portfolio and retry repeats a failed primary request.

## 2. Dashboard experience

- [x] 2.1 Implement dashboard loading skeletons, empty-portfolio guidance, unavailable-history messaging, primary-error retry, and partial-unavailability notices; verify each state is reachable from controlled API responses.
- [x] 2.2 Add summary cards, consolidated position table, market and income sections using only returned API values; verify null or unavailable values are labeled rather than replaced with fabricated financial data.
- [x] 2.3 Render portfolio evolution with native SVG and asset allocation with CSS-derived segments, including zero, single-point, and equal-value safeguards; verify these data shapes render without invalid geometry.

## 3. Responsive application shell and styling

- [x] 3.1 Update the application shell with active navigation, product identity, and a toggleable mobile menu while preserving current route destinations; verify all navigation destinations remain available at desktop and narrow viewport widths.
- [x] 3.2 Establish the dashboard design tokens, responsive card grids, notices, skeletons, and horizontally accessible position table styles; verify required controls and table columns remain reachable at 320px viewport width.

## 4. Verification

- [x] 4.1 Add dashboard component tests for successful primary data with failing indicator and income requests, primary failure with retry, and empty/history states; verify the frontend test command passes.
- [x] 4.2 Run the production frontend build and the full frontend test suite; verify both finish without compilation errors and the partial-failure behavior remains covered.
