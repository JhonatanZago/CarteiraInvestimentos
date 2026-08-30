## Why

The project needs a complete investment-portfolio application that keeps broker, asset, quotation, and portfolio data reliable even when the required external data sources fail. The existing product specification defines the academic scope and must now become an implementable, testable delivery plan.

## What Changes

- Add a Spring Boot API for brokers, stocks, portfolios, portfolio positions, quotation history, and dashboard indicators.
- Add validated external-data integrations for CNPJ, CEP, financial-institution eligibility, and Brazilian and US stock quotations.
- Add transactional persistence, API error contracts, pagination, OpenAPI documentation, and automated tests.
- Add an Angular client that consumes only the backend API and exposes the specified management and dashboard flows.

## Capabilities

### New Capabilities
- `broker-registration`: validate and register brokers from CNPJ, address, and financial-market data.
- `stock-quotation-management`: register market-validated stocks, maintain their latest quotation, and record quotation history.
- `investment-portfolios`: manage portfolios and their consolidated positions by stock and broker.
- `portfolio-dashboard`: calculate portfolio investment, current value, result, profitability, and last update.
- `investment-platform-api`: provide the layered API platform, standardized errors, documentation, pagination, and frontend integration boundary.

### Modified Capabilities

None. The repository has a single source specification document and no existing capability delta specs.

## Impact

Creates the Java 21/Spring Boot backend and Angular frontend structure; adds H2/PostgreSQL persistence, external HTTP clients, environment-based integration configuration, Swagger/OpenAPI, and JUnit/Mockito test coverage. It exposes the `/api/v1` resources defined by the source specification.
