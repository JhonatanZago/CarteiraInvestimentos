# Design

## Context

See [proposal.md](proposal.md) for the motivation and scope. The current application already uses a stateless bearer access token plus a rotated refresh token stored in a cookie. `SecurityConfig` currently disables CSRF globally, permits the H2 console regardless of environment, `AuthController` emits a non-secure refresh cookie, CORS allows broad local-origin patterns and all headers, and `AuthTokenService` accepts a predictable default secret. Portfolio services already contain several owner-scoped repository queries, but the protected boundary and regression tests are incomplete.

The implementation must keep Java 21, Spring Boot 4, Gradle, Angular/npm, existing `/api/v1` contracts, external integrations, and all financial calculations. Existing tests run with the isolated `test` profile and currently rely on test-only permissive request handling; this compatibility constraint must be addressed without making deployment profiles permissive.

## Goals / Non-Goals

**Goals:**

- Make runtime authentication fail closed when a non-test JWT secret is missing, short, or a known placeholder.
- Make refresh-cookie attributes explicit, environment-aware, and compatible with the Angular `withCredentials` flow.
- Make the bearer-token/refresh-cookie CSRF boundary explicit and regression-tested.
- Restrict CORS to configured exact origins and explicit methods/headers.
- Make H2 console access local-profile-only and disabled by default elsewhere.
- Add focused regression tests for authentication, authorization, ownership isolation, cookie/CORS/CSRF behavior, and unsafe configuration.
- Document STRIDE for all requested components and record residual risk.

**Non-Goals:**

- No migration to OAuth/OIDC or replacement of the current JWT/refresh-token mechanism.
- No role model redesign; existing `PerfilUsuario` authorities remain.
- No financial-rule, formula, purchase, sale, history, integration, or API-resource removal.
- No Semgrep, Gitleaks, SpotBugs/FindSecBugs, SBOM, OSV-Scanner, ZAP, automated gate, or triage implementation in this change.
- No destructive database operation and no migration of already applied schema.

## Decisions

### 1. Fail closed for JWT secrets outside test

`AuthTokenService` will receive the configured secret and active-profile context. In `test`, an empty value may use an in-memory generated secret for the lifetime of the test context, avoiding a credential in source. Outside `test`, blank values, the former fallback, and obvious placeholders fail startup; the configured value must meet a stronger minimum length than the current 24-character check. The secret is never included in exception text, logs, DTOs, or cookies.

Alternative considered: keep the current default and warn. Rejected because a warning still permits predictable token signing in a deployment mistake.

### 2. Use a dedicated cookie policy and `ResponseCookie`

Cookie settings will be bound from `app.auth` configuration: secure flag, SameSite policy, and path. `HttpOnly` remains unconditional; the refresh cookie path remains `/api/v1/auth`. Production-like profiles must explicitly opt into `Secure=true`; local development may use HTTP with an explicit local exception. `ResponseCookie` will be used so SameSite is emitted consistently on set and clear operations.

Alternative considered: expose refresh tokens to Angular storage. Rejected because it increases token theft impact and changes the established mechanism.

### 3. Keep CSRF disabled for bearer-protected business resources, with a tested boundary

The access token is sent in the `Authorization` header and financial state-changing endpoints do not authorize from the refresh cookie. The cookie-only endpoints are refresh/logout; refresh returns its response under same-origin policy and logout is a session invalidation operation. CSRF remains disabled for this stateless API, but the design is documented and tested: a cookie without a bearer token cannot access or mutate financial resources. SameSite and strict CORS reduce cross-site cookie use. If a future endpoint authorizes business state from a cookie, that change must introduce CSRF protection and an Angular token flow.

Alternative considered: enable default CSRF globally. Rejected for the current bearer API because it would break Angular requests without adding protection to bearer-authenticated calls; a future cookie-authorized endpoint must revisit this decision.

### 4. Exact-origin CORS with explicit credentials and headers

`CorsProperties` will bind allowed origins, methods, and headers. `CorsConfig` will use exact `allowedOrigins`, `allowedMethods`, `allowedHeaders`, and `allowCredentials(true)`, with no wildcard origin patterns. Defaults remain the two local Angular origins; deployment must override origins externally. Preflight and disallowed-origin tests will assert the observable headers.

Alternative considered: keep wildcard local port patterns for developer convenience. Rejected because any process on a local port would become an accepted browser origin and because wildcard headers unnecessarily expand the surface.

### 5. H2 console only in explicit local profile

`application-dev.properties` will default the console to disabled. A dedicated local profile may enable it explicitly. Security will permit `/h2-console/**` only when that local profile is active; test and deployment profiles will not make it public. Existing frame support is retained only for the local H2 console behavior.

Alternative considered: permit the console globally and rely only on the property. Rejected because a configuration drift could re-expose a permitted endpoint.

### 6. Ownership enforcement at the service/query boundary

Protected API requests require authentication in non-test runtime profiles. Portfolio services will continue using owner-scoped repository queries and will be reviewed for nested positions, sales, dashboard, evolution, income, and currency analysis. A cross-user lookup returns the existing not-found semantics rather than leaking resource existence. Public catalog-like resources retain their existing intended behavior only where the application explicitly documents them; the first implementation will not invent a user ownership model for global action/broker reference data.

Alternative considered: controller-only checks. Rejected because service methods are reused by multiple endpoints and controller checks are easy to bypass.

### 7. Regression tests before broader scanners

Focused Spring Boot/MockMvc tests will exercise valid/invalid login, missing/invalid/expired access tokens, 401/403 behavior, cross-user portfolio isolation, cookie flags, CORS allow/deny, CSRF boundary, H2 profile behavior, and secret validation. Existing tests that use the isolated test profile will remain compatible through explicit test-only test configuration, never through a deployment profile.

## STRIDE threat model

| Component / trust boundary | Threats | Impact | Controls in this change | Residual risk |
|---|---|---|---|---|
| JWT authentication | Spoofing, tampering, elevation | Account takeover or forged identity if signing secret is predictable or tokens are accepted incorrectly | External secret required outside test, minimum length, placeholder rejection, signature/expiry checks retained, no token logging, invalid/expired tests | HMAC key remains a high-value shared secret; rotation/issuer/audience are future work. |
| Cookies and sessions | Spoofing, tampering, repudiation, information disclosure, CSRF | Refresh-token theft, session fixation/abuse, cross-site logout/refresh | HttpOnly, environment-aware Secure, SameSite, auth-only path, rotation and hashed persistence retained, clear-cookie symmetry, boundary tests | Cookie-only refresh/logout remain browser-triggerable; no server-side audit trail yet. |
| Angular/Spring Boot communication | Spoofing, information disclosure, CSRF | Malicious origin reads responses or sends credentialed requests | Exact CORS origins, explicit methods/headers, credentials only for configured origins, preflight tests | TLS/secure deployment and browser policy remain deployment responsibilities. |
| REST controllers | Tampering, information disclosure, DoS, elevation | Unauthorized reads/writes, entity leakage, inconsistent errors | Authentication boundary, DTO responses, centralized error contract, ownership tests, existing validation/pagination | No DAST/SAST yet; rate limits and security headers remain later work. |
| Portfolios and positions | Tampering, information disclosure, elevation, repudiation | Cross-user access to holdings or unauthorized financial mutation | Authenticated owner-scoped repository queries and nested-resource tests; financial rules unchanged | No immutable audit event for every sensitive change. |
| Purchases and sales | Tampering, repudiation, information disclosure, elevation | Incorrect or unauthorized trade history/result changes | Protected endpoints, owner checks, regression tests around access; no formula/schema changes | Authorization coverage for every future operation must remain mandatory. |
| Brokers and assets | Spoofing, tampering, information disclosure, DoS | Unauthorized CRUD/revalidation or provider data abuse | Protected resource boundary, DTOs, external credentials kept server-side, existing adapter timeouts | Global reference-data ownership policy is not redesigned here. |
| External APIs | Spoofing, tampering, information disclosure, DoS | Credential leakage, provider abuse, stale/incorrect data, request exhaustion | Environment-backed URLs/tokens, bounded timeouts/retry already present, no token logs, protected backend boundary | No SSRF allowlist, circuit breaker, dependency scan, or tracing yet. |
| Database and migrations | Tampering, repudiation, information disclosure, DoS, elevation | Data loss, schema drift, credential exposure, unauthorized persistence | No destructive commands; retain JPA constraints/data; test isolation; H2 console local-only; migration gap documented | `ddl-auto=update` remains for now; versioned migrations are a later controlled change. |

## Risks / Trade-offs

- [Test-only permissive security] → Keep it confined to the isolated test profile and add explicit security tests with the permissive switch disabled; never copy this behavior to dev/deployment.
- [Cookie Secure=false in local HTTP] → Make it an explicit local-only setting and require Secure in production-like profiles.
- [CSRF disabled] → Do not authorize financial resources from cookies; test cookie-only rejection and document that a future cookie-authenticated state change requires CSRF.
- [CORS configuration drift] → Exact defaults plus external deployment override and preflight allow/deny tests; do not use wildcard patterns.
- [H2 local access] → Disabled by default and permitted only under an explicit local profile; do not expose it through deployment configuration.
- [No migrations yet] → Avoid schema changes in this change and record migration introduction as a separate controlled task.
- [Legacy tests assume unauthenticated requests] → Preserve their isolated test behavior while adding separate tests that exercise the real protected boundary; verify no non-test profile inherits it.

## Migration Plan

1. Apply configuration/code changes without changing tables or financial records.
2. Run backend tests and frontend build/tests.
3. Validate local profile with an explicitly supplied local JWT secret policy and H2 console behavior.
4. Validate a deployment-like profile fails without required secret/secure cookie configuration and starts with externally supplied values.
5. Roll back by restoring the previous application artifact/configuration; no data rollback is needed because this change has no schema mutation.

## Open Questions

- The repository currently has no explicit production profile. The implementation will treat any profile other than `test`, `dev`, and the explicit local profile as deployment-like for secret/cookie validation; deployment naming can be standardized later without changing the security contract.
