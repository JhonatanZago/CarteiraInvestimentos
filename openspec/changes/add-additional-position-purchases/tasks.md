## 1. Domain and persistence

- [ ] 1.1 Inspect existing position, sale, ownership, and migration conventions; add additive purchase-operation and remaining-cost persistence with no legacy-data rewrite, and verify schema generation/migration tests pass
- [ ] 1.2 Implement decimal purchase-cost consolidation, zero-balance repurchase, fee handling, chronological-date validation, and quotation snapshots; verify unit tests cover USD and BRL weighted-average cases
- [ ] 1.3 Make purchase application transactional, ownership-scoped, concurrency-safe, and idempotent; verify duplicate-key, equal-intentional-purchase, cross-account, and concurrent-purchase tests pass
- [ ] 1.4 Keep sales consistent with remaining cost and expose owned purchase history; verify partial-sale, zero-balance, legacy-position, and quote-refresh tests pass

## 2. API contract

- [ ] 2.1 Add authenticated versioned purchase-create and purchase-history endpoints with validated request/response DTOs and structured errors; verify controller and integration tests pass
- [ ] 2.2 Preserve existing position creation compatibility while routing new position creation through purchase semantics; verify the duplicate-position constraint still rejects duplicate position rows but accepts an additional purchase

## 3. Portfolio purchase experience

- [ ] 3.1 Update Angular models and API service for purchase requests, operation history, idempotency keys, and returned consolidated positions; verify frontend unit tests compile
- [ ] 3.2 Rename the portfolio form to purchase terminology and allow an asset already held in a selected portfolio to be selected for a new purchase; verify the form does not prefill a prior purchase's quantity or price
- [ ] 3.3 Add a distinct “Comprar mais” action on each position that preselects its portfolio, asset, and broker and requests quantity, unit price, date, and supported fees; verify it remains separate from Editar
- [ ] 3.4 Render a client-side before-submit preview, disable submit while pending, preserve entered data on failure, refresh affected positions and summaries on success, and show success/error toasts; verify component tests and manual flow behavior

## 4. Validation and delivery

- [ ] 4.1 Execute backend unit, integration, persistence, concurrency, idempotency, sale-consistency, authorization, and quotation-snapshot tests; verify all pass
- [ ] 4.2 Execute Angular tests and production build; verify the portfolio purchase workflow builds without visual regressions
- [ ] 4.3 Capture the Comprar mais action and the consolidated position after two purchases, and record changed files, migrations, test results, and the original duplicate-block cause in the delivery notes
