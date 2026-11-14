# Spec Delta

## Purpose

Define uma verificação automatizada, reproduzível e fail-closed para impedir que alterações sem build, testes, análise de segurança ou validação OpenSpec comprovados avancem nas branches protegidas.

## ADDED Requirements

### Requirement: Backend validation is reproducible
The CI process SHALL validate the backend with Java 21 and the repository's Gradle Wrapper, using only authorized dependency repositories and no local developer Gradle cache.

#### Scenario: Backend validation succeeds
- **WHEN** a pull request or protected-branch run has resolvable dependencies
- **THEN** backend tests, build, and SpotBugs with FindSecBugs complete successfully

#### Scenario: Backend validation fails
- **WHEN** compilation, a backend test, build, or static-security analysis fails
- **THEN** the CI run is failed and the protected check cannot pass

### Requirement: Frontend validation is reproducible
The CI process SHALL install the locked frontend dependency graph with `npm ci` and validate the Angular application with its supported Node.js and npm versions.

#### Scenario: Frontend validation succeeds
- **WHEN** the lockfile is consistent and the frontend is valid
- **THEN** frontend tests run without watch mode and the production build completes successfully

#### Scenario: Frontend dependency drift is detected
- **WHEN** `package-lock.json` does not describe the declared dependencies
- **THEN** `npm ci` fails the CI run before tests or build are accepted

### Requirement: CI credentials and permissions are minimal
The CI process SHALL use read-only repository permissions, shall not require real application credentials for isolated tests, and shall not perform deployment.

#### Scenario: CI runs without production secrets
- **WHEN** backend and frontend checks run in a pull request
- **THEN** no real BRAPI, JWT, refresh-token, or deployment credential is supplied or printed

#### Scenario: Workflow attempts an unauthorized capability
- **WHEN** a workflow step would deploy or require write access unrelated to validation
- **THEN** the workflow definition rejects that capability through its permissions and job design

### Requirement: Validation evidence is retained
The CI process SHALL publish test, build, and static-analysis reports as artifacts even when a validation step fails.

#### Scenario: Reports are available after a failed check
- **WHEN** any validation job fails
- **THEN** its available backend, frontend, and SpotBugs reports are uploaded for diagnosis without exposing secrets

#### Scenario: Reports are available after a successful check
- **WHEN** all validation jobs finish successfully
- **THEN** the same report categories are available as build evidence

### Requirement: OpenSpec validation is enforced
The CI process SHALL execute strict OpenSpec validation for the repository change under review and fail when the change is structurally or semantically invalid.

#### Scenario: OpenSpec change is valid
- **WHEN** the change artifacts satisfy the installed OpenSpec schema
- **THEN** the OpenSpec check passes and publishes its output as evidence

#### Scenario: OpenSpec change is invalid
- **WHEN** strict validation reports an error
- **THEN** the CI run fails and no passing validation status is reported

### Requirement: Execution is bounded and cache-safe
The CI process SHALL run on pull requests and the protected default branch with bounded execution, cancellable duplicate runs, and service-managed caches that do not depend on local `.gradle-user-home-*` directories.

#### Scenario: Duplicate runs are superseded
- **WHEN** a newer commit starts a run for the same workflow and ref
- **THEN** the older in-progress run is cancelled when safe

#### Scenario: A job exceeds its bound
- **WHEN** a validation job exceeds its configured timeout
- **THEN** the job fails and its diagnostic artifacts are retained when available
