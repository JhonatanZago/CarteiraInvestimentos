## Purpose

Provide one versioned API path for the Angular application in local development and production deployments without exposing backend host details to browser clients.

## ADDED Requirements

### Requirement: Route frontend API traffic through a same-origin proxy
The system SHALL make Angular requests to `/api/v1` available through a local development proxy and through the production web server, forwarding them to the Spring Boot API without changing the request path or payload.

#### Scenario: Development API request
- **WHEN** the Angular development server receives a request for `/api/v1/**`
- **THEN** it forwards the request to the locally configured Spring Boot service and returns its response to the browser

#### Scenario: Production API request
- **WHEN** a deployed browser requests `/api/v1/**` from the application origin
- **THEN** the production web server forwards it to the Spring Boot service and preserves the API response status and body

### Requirement: Serve the SPA with client-side route fallback
The production web server SHALL serve the built Angular application and return its entry document for non-API routes so direct navigation to client routes remains available.

#### Scenario: Direct client-route navigation
- **WHEN** a user opens a valid Angular route directly in the browser
- **THEN** the web server returns the SPA entry document rather than a server-side not-found response
