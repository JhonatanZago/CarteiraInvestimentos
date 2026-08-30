## 1. API foundation and frontend contracts

- [x] 1.1 Audit the existing `/api/v1` responses against the dashboard, history, and collection needs; document any missing backend contract as a blocker rather than deriving financial data in Angular.
- [x] 1.2 Consolidate environment-backed API URL configuration and typed TypeScript models for stocks, brokers, portfolios, positions, history, dashboard, pagination, and API errors; verify strict TypeScript compilation uses no `any` in primary API flows.
- [x] 1.3 Complete typed HTTP services for every supported backend resource and preserve backend pagination parameters; verify service tests assert request URLs, methods, and typed response mapping.

## 2. Shared application feedback and navigation

- [x] 2.1 Extend centralized API error interpretation to map stable backend codes to user-facing messages and notifications; verify interceptor tests use codes rather than provider message text.
- [x] 2.2 Create reusable toast, loading, empty-state, confirmation-dialog, and pagination primitives; verify component tests cover their visible states and actions.
- [x] 2.3 Implement the responsive application shell with navigation, active-route indication, and small-screen adaptation; verify routes remain accessible at desktop and mobile viewport sizes.

## 3. Stock and broker flows

- [x] 3.1 Connect stock list, registration, lookup, quotation refresh, and history navigation to typed API services; verify mocked component flows cover successful registration, duplicate ticker, unavailable integration, and loading-state submission protection.
- [x] 3.2 Connect broker list, registration, lookup by ID/CNPJ, and official address/validation display to typed API services; verify mocked component flows cover valid registration, duplicate CNPJ, invalid CNPJ or CEP, inactive company, and unauthorized institution.
- [x] 3.3 Add reusable collection controls for pagination, search, filters, sorting, and empty states where the backend supports them; verify changes request the expected page and render its results.

## 4. Portfolio, position, dashboard, and history flows

- [x] 4.1 Connect portfolio creation, editing, listing, details, and guarded deletion flows to the backend; verify component tests cover valid changes, conflict responses, confirmation, and list refresh.
- [x] 4.2 Connect position create, edit, delete, and quotation-result display to backend operations without client-side financial rule calculation; verify tests assert backend values are rendered and failed requests preserve the form state.
- [x] 4.3 Build dashboard indicator cards and portfolio composition visualization from backend-provided data; verify tests cover positive, negative, zero-investment, loading, and empty dashboard states.
- [x] 4.4 Build quotation-history table and chart from API history points, with a non-chart fallback for unavailable or empty data; verify the rendered order and timestamps match mocked backend fixtures.

## 5. Visual experience and responsiveness

- [x] 5.1 Apply an investment-platform visual system to cards, forms, tables, modals, and interactive controls with accessible focus, disabled, and status states; verify visual components expose semantic labels and keyboard-accessible actions.
- [x] 5.2 Make dashboard, tables, charts, forms, and sidebar responsive across desktop, tablet, and mobile breakpoints; verify representative component tests or browser checks at each breakpoint.
- [x] 5.3 Add restrained transitions, tooltips, icons, and last-update feedback without obscuring content or interaction states; verify reduced or unavailable data states remain understandable.

## 6. Integration and release verification

- [x] 6.1 Add Angular unit tests for services, interceptor behavior, feature loading/error states, and shared components; verify the frontend test suite passes.
- [x] 6.2 Execute mocked end-to-end frontend-to-backend flows for stocks, brokers, portfolios, positions, dashboard, and history; verify the expected API requests and rendered outcomes.
- [x] 6.3 Run the frontend production build, inspect and resolve relevant compiler warnings, then run the backend and frontend together for a final browser-based smoke test; verify no console CORS or HTTP integration errors occur.
