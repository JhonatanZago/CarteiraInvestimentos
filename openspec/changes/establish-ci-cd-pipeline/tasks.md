# Tasks

## 1. CI contract and runner decisions

- [x] 1.1 Confirm the protected default branch (`main` is the current local branch) and select a versioned, authorized way to provide OpenSpec CLI 1.14.3 to the runner; verify the decision does not require a personal installation or unapproved network source.
- [x] 1.2 Confirm the Angular 22 / Node.js / npm compatibility matrix against `frontend/package.json` and lock the runner versions; verify `npm ci` is reproducible from `package-lock.json`.

## 2. Backend validation job

- [x] 2.1 Add the backend CI job with Java 21 and the repository Gradle Wrapper 9.5.1; verify `clean test`, `build`, and `spotbugsMain --rerun-tasks` fail the job on errors.
- [x] 2.2 Configure Gradle caching through supported GitHub Actions cache mechanisms and authorized repositories; verify the job does not read `.gradle-user-home-*` or copy artifacts.
- [ ] 2.3 Upload backend JUnit and SpotBugs HTML/XML reports with conditional artifact publication; verify artifacts remain available after a failed backend step and contain no secrets.

## 3. Frontend validation job

- [ ] 3.1 Add the frontend CI job with the versions selected in 1.2; verify `npm ci`, `npm test -- --watch=false`, and `npm run build` execute in a clean checkout.
- [ ] 3.2 Configure npm caching keyed by the lockfile and upload available frontend test/build evidence; verify a lockfile inconsistency fails before acceptance.

## 4. OpenSpec and workflow security

- [ ] 4.1 Add the strict OpenSpec validation job using the decision from 1.1; verify invalid change artifacts fail the check and valid artifacts produce retained output.
- [x] 4.2 Set pull-request and protected-branch triggers, per-ref concurrency cancellation, job timeouts, and `contents: read` permissions; verify the workflow contains no deploy job or write-capable token.
- [ ] 4.3 Verify the workflow supplies no real `BRAPI_TOKEN`, JWT secret, refresh token, ZAP approval, or other production credential; verify logs and artifacts do not print environment secrets and isolated tests remain runnable without BRAPI.

## 5. Integration verification and documentation

- [ ] 5.1 Run the new workflow on a validation pull request and inspect all job statuses, skipped-test counts, cache behavior, and artifacts; verify backend failures, frontend failures, SpotBugs findings, and OpenSpec errors block the run.
- [ ] 5.2 Document the required branch-protection check names, ignored-test policy, report locations, cache policy, and no-deploy boundary; verify the documentation matches the committed workflow and does not alter Module 11 or task 6.4.
