## Purpose

Provide an auditable baseline that identifies the code paths, contracts, and observable failures before implementation changes begin.

## ADDED Requirements

### Requirement: Architecture and route inventory
The project SHALL maintain an inventory of backend layers, frontend routes, real rendered components, services, endpoints, and test coverage for the supported investment flows.

#### Scenario: Route ownership is unambiguous
- **WHEN** the inventory is reviewed
- **THEN** `/dashboard`, `/carteiras`, `/acoes`, and `/corretoras` each identify the component actually loaded by the router and its backend data endpoints.

### Requirement: Evidence-based baseline
The audit SHALL record command results, confirmed defects, hypotheses requiring verification, risks, and known missing tests without presenting hypotheses as confirmed defects.

#### Scenario: Baseline is reproducible
- **WHEN** the prescribed backend tests, frontend tests, typecheck, and build are executed
- **THEN** their exit status and relevant warnings/errors are recorded in the change artifacts.
