## 1. Position display contract

- [x] 1.1 Extend portfolio-position response models with stock identity, latest quotation, quotation timestamp, and backend-calculated invested/current/result/profitability values; verify mapper unit tests cover every added field.
- [x] 1.2 Update portfolio position read operations to populate the enriched response without changing create or update request contracts; verify controller tests preserve CRUD statuses and expose the new fields.
- [x] 1.3 Define zero-safe behavior for positions without calculable investment and verify service tests cover zero investment and latest-quotation cases.

## 2. Dashboard composition contract

- [x] 2.1 Add dashboard composition response models containing one backend-calculated entry per portfolio position and the asset count; verify serialization tests cover empty and populated responses.
- [x] 2.2 Update dashboard calculation to produce aggregate indicators and composition from the same stored quotations; verify unit tests cover positive, negative, zero-investment, and multiple-position formulas.
- [x] 2.3 Expose the enriched dashboard response through the existing endpoint without removing aggregate fields; verify MockMvc fixtures assert asset count and composition values.

## 3. Compatibility and release verification

- [x] 3.1 Run the backend test suite and OpenAPI smoke test; verify all existing and new `/api/v1` contracts pass.
- [x] 3.2 Document the additive position and dashboard response fields in OpenAPI examples; verify Swagger UI exposes the updated schemas.

## 4. Consolidated positions accuracy

- [x] 4.1 Consolidate dashboard positions by `acaoId`, calculate the open weighted-average cost from purchase and sale movements with `BigDecimal`/`HALF_EVEN` (falling back to persisted legacy positions when no movement history exists), expose quantity, average price and explicit currency, and render the real Visão geral table with currency-aware values, internal horizontal scrolling and aligned numeric columns; verify backend, frontend, build and visual route validation.
