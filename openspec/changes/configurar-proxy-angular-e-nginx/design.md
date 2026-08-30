## Context

The Angular production environment already represents the API as `/api/v1`, while local development currently uses an absolute backend URL. The Spring Boot service runs separately from the SPA and production needs a web server to serve the static build and proxy versioned API requests.

## Goals / Non-Goals

**Goals:**

- Keep browser API calls on the application origin in development and production.
- Provide a deployable Nginx configuration and a documented local proxy workflow.
- Preserve the existing Spring Boot API paths and response contracts.

**Non-Goals:**

- Deploy Nginx or provision a server, DNS, TLS certificate, or process manager.
- Add authentication or alter Spring Boot business endpoints.

## Decisions

### Use `/api/v1` as the sole browser-facing API base path

Both Angular environments use a relative path. The Angular development server proxies that path to `localhost:8081`; Nginx proxies the same path in production. This avoids browser CORS requirements for normal deployments and prevents hostnames from being compiled into the frontend.

Alternative considered: preserve an absolute development API URL. Rejected because it requires CORS and makes development behavior differ from production.

### Keep Nginx configuration versioned with the project

The repository will include a server block that serves the Angular build, falls back to `index.html` for SPA navigation, and forwards `/api/v1/` to the local Spring Boot upstream with forwarded headers.

Alternative considered: only document a configuration snippet. Rejected because it is easier to drift from the frontend build and API prefix.

## Risks / Trade-offs

- [Spring Boot is unavailable] → Nginx returns an upstream error; deployment documentation identifies the backend service and port to monitor.
- [A deployment uses a different upstream host or port] → make the Nginx upstream value an explicit deployment-time setting.
- [TLS or a domain is required] → configure it at the hosting environment; this change supplies HTTP proxying only.

## Migration Plan

1. Add the Angular development proxy and relative API base URL.
2. Add the Nginx configuration and deployment documentation.
3. Build the Angular application and verify requests to `/api/v1` reach Spring Boot through the development proxy.
4. Roll back by restoring the previous absolute development API URL if a local proxy cannot be used.
