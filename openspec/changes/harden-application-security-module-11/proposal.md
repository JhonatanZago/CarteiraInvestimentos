# Proposal

## Why

The application already has authentication and ownership checks, but several deployment and boundary controls are unsafe or undocumented: a fallback JWT secret, insecure refresh-cookie attributes, broad local-development access, and no explicit threat model. These gaps must be closed before adding automated security gates, without changing financial behavior or the existing authentication mechanism.

## What Changes

- Require an externally supplied JWT signing secret outside the isolated test profile; remove the insecure runtime fallback.
- Harden refresh-token cookies with environment-aware `HttpOnly`, `Secure`, `SameSite`, path, and expiration behavior while preserving the Angular refresh flow.
- Make the CSRF decision explicit for the mixed bearer-token/cookie authentication model and test the selected behavior.
- Restrict CORS origins, methods, and headers by environment and preserve valid Angular preflight requests.
- Restrict the H2 console to an explicitly authorized local-development profile and keep it unavailable in production/deployment profiles.
- Add security regression tests for authentication, authorization, ownership isolation, CORS, CSRF behavior, and configuration safety.
- Document a STRIDE threat model and a hardening report with evidence, residual risks, and test results.
- Do not add Semgrep, Gitleaks, SpotBugs/FindSecBugs, SBOM, OSV-Scanner, OWASP ZAP, an automated security gate, or finding triage in this change; those remain later Módulo 11 work.

## Capabilities

### New Capabilities

- `application-security`: Define secure authentication boundaries, browser security behavior, environment safety, and security regression expectations for the application.

### Modified Capabilities

- `investment-platform-api`: Protected versioned APIs must reject unauthenticated access consistently while retaining machine-readable error responses and the Angular backend boundary.
- `investment-portfolios`: Portfolio and position operations must enforce authenticated-user ownership isolation, including nested financial endpoints.

## Impact

Affected areas include `SecurityConfig`, `AuthController`, authentication properties, CORS configuration, profile-specific H2 settings, portfolio/position authorization paths, security tests, and security documentation. The change keeps Java 21, Spring Boot, Gradle, Angular, npm, the current JWT/refresh-token mechanism, existing API paths, external integrations, and all financial calculations. No database migration is expected; existing JPA schema and financial data must remain unchanged.
