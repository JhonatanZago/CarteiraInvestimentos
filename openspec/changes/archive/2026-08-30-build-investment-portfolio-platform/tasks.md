## 1. Project foundation

- [x] 1.1 Create the Java 21 Spring Boot project with Web, JPA, Validation, Lombok, OpenAPI, H2, PostgreSQL, and test dependencies; verify the application context starts.
- [x] 1.2 Create the Angular application with routing, SCSS, reactive forms, and HttpClient; verify production build succeeds.
- [x] 1.3 Add environment-backed integration, datasource, timeout, CORS, and OpenAPI configuration; verify no API URL, token, or key is hardcoded.
- [x] 1.4 Create the base package structure, standard error model, exception hierarchy, and centralized exception handler; verify a validation failure returns the documented error fields and stable code.

## 2. Persistence and shared API contracts

- [x] 2.1 Implement market, currency, quotation-source, and related enums plus JPA entities for stock, broker, portfolio, position, and quotation history; verify the H2 schema is generated successfully.
- [x] 2.2 Add database uniqueness constraints for normalized ticker and CNPJ, quotation timestamp per stock, and portfolio-stock-broker positions; verify constraint tests reject duplicate rows.
- [x] 2.3 Create request/response DTOs, validation annotations, and mappers without repository or HTTP dependencies; verify mapper unit tests cover every entity conversion.
- [x] 2.4 Implement pageable list response handling and default sorting for stocks and brokers; verify endpoint tests return requested pages in the required order.

## 3. External integration boundary

- [x] 3.1 Implement internal DTOs and adapter contracts for company, address, financial-institution, and quotation lookups; verify services do not import provider response models.
- [x] 3.2 Implement BrasilAPI and ViaCEP adapters with normalization, bounded timeouts, and missing-data/error translation; verify mocked 404, timeout, malformed-response, and unavailable cases.
- [x] 3.3 Implement the configured financial-institution adapter and facade, recording source and validation time; verify an unauthorized institution is rejected.
- [x] 3.4 Implement BRAPI and Alpha Vantage quotation adapters plus market-based facade selection; verify BRASIL selects BRAPI, EUA selects Alpha Vantage, and unsupported markets fail.
- [x] 3.5 Map provider rate limits and incomplete responses to external integration errors; verify no partial domain data is returned or persisted in adapter/service tests.

## 4. Broker capability

- [x] 4.1 Implement CNPJ and CEP normalization plus CNPJ check-digit validation; verify valid formatted inputs normalize and invalid CNPJs fail before lookup.
- [x] 4.2 Implement broker registration service with duplicate check, company status validation, financial authorization, CEP enrichment, audit data, and transaction boundary; verify successful registration persists official data.
- [x] 4.3 Add broker create, list, get-by-id, and get-by-CNPJ `/api/v1/corretoras` endpoints; verify MockMvc tests cover success, duplicate CNPJ, unknown/inactive company, unauthorized institution, and unknown CEP.

## 5. Stock and quotation capability

- [x] 5.1 Implement ticker normalization and stock registration service that requires an explicit market, validates the provider result, and writes the stock with initial history atomically; verify Brazilian and US registration tests pass.
- [x] 5.2 Enforce ticker and historical-point uniqueness while recording quotation source and provider timestamp; verify repeated ticker and timestamp scenarios are rejected or deduplicated as specified.
- [x] 5.3 Implement quotation-refresh service that selects the stored market provider and atomically updates latest quotation plus history; verify external failure preserves the previous quotation.
- [x] 5.4 Add stock create, list, get-by-id, get-by-ticker, refresh, and history `/api/v1/acoes` endpoints; verify MockMvc tests cover successful refresh and all external error codes.

## 6. Portfolio and dashboard capability

- [x] 6.1 Implement portfolio create, list, retrieve, update, and guarded delete services; verify deletion of a portfolio with positions returns conflict.
- [x] 6.2 Implement position create, list, update, and delete services with existing stock/broker checks, positive quantity/average price, non-future purchase date, and composite uniqueness; verify rule tests pass.
- [x] 6.3 Add `/api/v1/carteiras` and nested portfolio-position endpoints; verify MockMvc tests cover valid CRUD, invalid values, duplicate positions, and guarded deletion.
- [x] 6.4 Implement dashboard calculations from stored latest quotations, including zero-investment safety and last-update time; verify unit tests cover all formulas and zero division.
- [x] 6.5 Add `GET /api/v1/dashboard/carteiras/{carteiraId}`; verify its response matches calculated portfolio fixtures.

## 7. Frontend and documentation

- [x] 7.1 Build Angular core HTTP/error handling and typed API services for all backend resources; verify unit tests map the backend error `code` without comparing message text.
- [x] 7.2 Build routed Angular features for brokers, stocks, portfolios/positions, quotation history, and dashboard using reactive forms; verify the production build and component tests pass.
- [x] 7.3 Render dashboard values with the latest-update timestamp and use backend-provided quotation data only; verify an end-to-end mocked API flow displays the expected indicators.
- [x] 7.4 Document endpoints, request/response examples, statuses, and error codes in OpenAPI; verify Swagger UI loads and exposes every `/api/v1` resource.

## 8. Quality and release verification

- [x] 8.1 Add service, facade, adapter, and integration tests for all mandatory source-specification scenarios; verify the backend test suite passes.
- [x] 8.2 Run the backend against H2 and perform a PostgreSQL configuration smoke test; verify schema constraints and API startup succeed in both environments.
- [x] 8.3 Verify CORS permits the Angular development origin through centralized configuration; verify a browser-origin preflight succeeds.
- [x] 8.4 Execute the full frontend build, backend tests, and OpenAPI smoke test as a release check; verify all commands pass before deployment.
