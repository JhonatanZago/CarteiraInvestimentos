# Tasks

## 1. Authentication and token configuration

- [ ] 1.1 Replace the JWT placeholder fallback with external-only configuration and test that non-test startup rejects missing, short, or known-default secrets without logging the secret; verify with focused `AuthTokenService`/context tests.
- [ ] 1.2 Preserve the current HMAC access-token format, expiry validation, refresh-token rotation, and hashed persistence while adding the secure-secret policy; verify existing authentication tests plus valid/invalid/expired token scenarios.
- [ ] 1.3 Add environment-aware refresh-cookie properties and emit/clear `HttpOnly`, `Secure`, `SameSite`, path, and max-age consistently; verify `Set-Cookie` headers for local and secure configurations and Angular refresh compatibility.

## 2. HTTP security boundaries

- [ ] 2.1 Replace broad CORS origin patterns and wildcard headers with exact configured origins, explicit methods/headers, and credential handling; verify allowed and rejected preflight/actual-origin behavior.
- [ ] 2.2 Document and enforce the bearer-token/refresh-cookie CSRF boundary without changing the established Angular flow; verify cookie-only requests cannot access or mutate protected financial endpoints and valid bearer requests remain functional.
- [ ] 2.3 Disable H2 console by default and permit it only for an explicit local-development profile; verify deployment-like profiles reject/disable it and local profile behavior is isolated.

## 3. Authorization and ownership regression coverage

- [ ] 3.1 Review and, where necessary, tighten portfolio, position, sale, dashboard, evolution, income, and currency-analysis access paths to use authenticated owner scope; verify no financial formula, purchase, sale, history, or integration behavior changes.
- [ ] 3.2 Add regression tests for login success/failure, missing/invalid/expired JWT, 401/403 responses, cross-user isolation, protected financial endpoints, and no data leakage; verify with the existing Spring Boot/JUnit/MockMvc stack.

## 4. Security documentation

- [x] 4.1 Create `docs/security/threat-model.md` with STRIDE for JWT, cookies/sessions, Angular/Spring communication, REST controllers, portfolios/positions, purchases/sales, brokers/assets, external APIs, and database/migrations; verify every component has impact, control, and residual-risk entries.
- [x] 4.2 Create `docs/security/hardening-report.md` with changed files, configuration policy, tests executed/results, scanner status, failures or blockers, residual risks, and explicit deferral of later layers; verify it contains no credentials and does not claim unexecuted validation.

## 5. Integrated validation

- [x] 5.1 Run the relevant backend test suite and record the exact command and result in the hardening report; investigate failures without weakening security behavior. The specific controller test, complete backend suite, and build passed after aligning the test with the existing repeated-purchase consolidation rule.
- [x] 5.2 Run the frontend tests/build when the environment permits and record exact results or blockers; verify existing Angular authentication and API integration remain compatible. Build and 63 tests passed outside the Codex sandbox; only pre-existing Angular warnings remain.
- [x] 5.3 Validate the OpenSpec change and confirm only the intended security/configuration/tests/docs files changed; verify no financial rules, migrations, external integrations, push, or merge were altered.

## 6. SAST and secret scanning

- [x] 6.1 Execute Semgrep 1.178.0 with `--metrics=off`, `p/java`, `p/secrets`, and `p/owasp-top-ten` over backend and Angular sources; record SARIF and actual findings.
- [x] 6.2 Add and test the project-specific controller/JPA-entity rule with real `semgrep --test` execution; verify positive and negative fixtures.
- [x] 6.3 Execute Gitleaks 8.30.1 with `--redact` over relevant worktree scopes and separately over Git history; preserve sanitized SARIF reports.
- [ ] 6.4 Triage the potential API credential found in tracked `.gitignore` and history; ownership/validity remain unconfirmed, and revocation/rotation plus removal/history cleanup require explicit authorization. The sanitized investigation and current gate failure are documented.
- [x] 6.5 Execute the local Semgrep/Gitleaks security gate and record fail-closed behavior when a secret finding exists.
- [x] 6.6 Remove the exposed current-worktree value after the requester confirms revocation/rotation; preserve external `BRAPI_TOKEN` configuration and verify current-worktree Gitleaks/Gate results. Historical cleanup remains pending.

## 7. Bytecode security analysis

- [x] 7.1 Configure SpotBugs for Java 21 with HTML/XML reports, security category filtering, and fail-closed execution.
- [x] 7.2 Integrate FindSecBugs 1.14.0 and verify detector loading through a real `spotbugsMain` execution; record the targeted CORS detector limitation.
- [x] 7.3 Correct the confirmed servlet JSON output finding and add a narrowly scoped, documented exception for the intentional bearer-token CSRF boundary.
- [x] 7.4 Integrate `spotbugsMain` into `scripts/security-gate.ps1` and preserve fail-closed behavior for execution errors.
- [ ] 7.5 Revisit the CSRF exception if any state-changing endpoint becomes cookie-authorized, and replace the CORS detector workaround when a compatible FindSecBugs release is available.

## 8. Supply-chain analysis — Command 12

- [x] 8.1 Configure CycloneDX Gradle `3.5.1` and generate the backend JSON SBOM.
- [x] 8.2 Generate and validate the frontend CycloneDX JSON SBOM with npm tooling `6.0.1`, preserving package manifests.
- [x] 8.3 Install official OSV-Scanner Windows amd64 `v2.7.0` with checksum verification.
- [x] 8.4 Scan both SBOMs and preserve JSON evidence with components, IDs, severities, and fixed-version metadata.
- [x] 8.5 Integrate SBOM generation and OSV analysis into the fail-closed gate while preserving existing scanners.
- [x] 8.6 Re-run regression commands and document actual results, Docker limitation, OSV findings, and BRAPI risk.
- [x] 8.7 Apply dependency remediation after compatibility review and explicit authorization; Spring Boot 4.0.9 and aligned Angular/Vitest patch updates passed regression and post-fix OSV validation.

## Workflow follow-up

- Archive this change only after review and explicit confirmation that the phase implementation is accepted.
- Keep the remaining credential triage and the unexecuted ZAP baseline follow-up pending; keep Modules 12/13 out of this change.

## 9. Dynamic analysis — Command 14

- [x] 9.1 Prepare a versioned OWASP ZAP Baseline runner restricted to local HTTP targets and isolated report paths; verify fail-closed behavior when Docker is unavailable.
- [x] 9.2 Execute the Baseline Scan against the isolated local backend and preserve HTML/JSON/XML evidence after Docker became available; 13 local URLs covered, 0 FAIL, WARNs retained.
- [x] 9.3 Triage ZAP alerts, apply only safe corrections, rerun backend/frontend regression, and document residual risk; Swagger UI DOMPurify remains pending due Maven resolution failure.
