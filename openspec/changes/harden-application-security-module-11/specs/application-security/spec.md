# Spec Delta

## Purpose

Provide secure, environment-aware authentication and browser/API boundaries for the investment application while preserving the current JWT refresh flow and protecting user financial data from unauthorized access.

## ADDED Requirements

### Requirement: Require a secure JWT signing secret
The system SHALL require a non-default JWT signing secret from external configuration outside the isolated test profile and SHALL refuse unsafe startup configuration.

#### Scenario: Missing production secret
- **WHEN** the application starts outside the test profile without a configured JWT secret
- **THEN** startup fails before serving requests and no fallback secret is used

#### Scenario: Configured secret
- **WHEN** the application starts with a configured secret meeting the minimum policy
- **THEN** access-token issuance and validation continue to work with the configured secret

#### Scenario: Secret is not logged
- **WHEN** authentication succeeds or fails
- **THEN** logs and API responses contain no JWT signing secret, access token, or refresh token

### Requirement: Protect refresh tokens in browser cookies
The system SHALL issue refresh tokens only in `HttpOnly` cookies whose `Secure` and `SameSite` behavior is explicitly controlled by environment configuration and whose path is limited to authentication endpoints.

#### Scenario: Production refresh cookie
- **WHEN** login or refresh succeeds in a secure deployment
- **THEN** the response sets an `HttpOnly`, `Secure` refresh cookie with the configured SameSite policy and authentication path

#### Scenario: Local development refresh cookie
- **WHEN** login or refresh succeeds in the explicitly configured local HTTP profile
- **THEN** the response remains usable by Angular without exposing the cookie to JavaScript, using only the documented local-development exception for `Secure`

#### Scenario: Logout clears cookie attributes
- **WHEN** logout succeeds
- **THEN** the refresh cookie is expired with matching security attributes and no token value is returned

### Requirement: Apply a deliberate CSRF policy
The system SHALL enforce a documented CSRF policy that matches its bearer access-token and cookie-based refresh authentication model and SHALL preserve the Angular authentication flow.

#### Scenario: Bearer-protected financial request
- **WHEN** an authenticated Angular client calls a financial endpoint with a valid bearer access token
- **THEN** the request is authorized according to the existing user-ownership rules

#### Scenario: Cookie-authenticated state change
- **WHEN** a state-changing request could be authenticated solely by the refresh cookie
- **THEN** the configured CSRF policy rejects a forged cross-site request or ensures the endpoint cannot use the refresh cookie as its authorization credential

### Requirement: Restrict cross-origin API access
The system SHALL allow only configured frontend origins, methods, and headers and SHALL reject unconfigured origins during preflight and actual requests.

#### Scenario: Allowed Angular origin
- **WHEN** a preflight or API request comes from a configured frontend origin with an allowed method and headers
- **THEN** the request receives the configured CORS response and can use credentials where required

#### Scenario: Unconfigured origin
- **WHEN** a preflight or API request comes from an origin outside the configured allowlist
- **THEN** the response does not grant CORS access or credentials to that origin

### Requirement: Keep the H2 console local-only
The system SHALL disable the H2 console by default outside an explicitly authorized local-development profile and SHALL not make the console a general production-public endpoint.

#### Scenario: Deployment profile
- **WHEN** the application runs with a deployment or production configuration
- **THEN** the H2 console is disabled and its endpoint is not publicly accessible

#### Scenario: Authorized local profile
- **WHEN** an explicitly authorized local-development profile enables the H2 console
- **THEN** only that profile may use the console under its documented local access rules

### Requirement: Preserve security regression coverage
The project SHALL maintain automated tests for authentication, authorization, ownership isolation, browser security configuration, and unsafe runtime configuration.

#### Scenario: Invalid authentication
- **WHEN** a request has no token, an invalid token, or an expired token
- **THEN** protected endpoints return the documented unauthorized response and do not expose financial data

#### Scenario: Cross-user resource access
- **WHEN** an authenticated user requests another user's portfolio or nested position resource
- **THEN** the system denies access without changing or revealing the other user's data

#### Scenario: Unsafe configuration test
- **WHEN** a non-test application context is created without a safe JWT secret or with the H2 console enabled
- **THEN** the security configuration test fails or startup is rejected
