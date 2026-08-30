## API contract audit

Audited against `/api/v1` on 2026-08-30.

- Stocks, brokers, portfolios, positions, quotation history, and paginated collections have typed API contracts.
- Quotation history returns `valor` (not `cotacao`); the frontend model uses `valor`.
- Broker creation requires `cnpj`, `cep`, `numero`, and optional `complemento`.
- Dashboard/position display contracts (composition, latest quotation, and backend-calculated values) are provided by the completed `add-portfolio-dashboard-display-contracts` change.
- No frontend financial calculation or external provider call is required.
