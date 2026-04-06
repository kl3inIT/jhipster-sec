# Phase 13: Split CI Verification Lanes - Context

**Gathered:** 2026-04-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Separate CI feedback into isolated backend, frontend, and production-validation lanes using GitHub Actions so milestone regressions are detectable per lane without one lane masking another. This phase covers workflow file creation and wire-up only — no new test infrastructure, no new validation scripts beyond what Phase 12 already committed.

</domain>

<decisions>
## Implementation Decisions

### CI Provider
- **D-01:** GitHub Actions is the CI provider. Create `.github/workflows/` and author all lane definitions as GitHub Actions workflow YAML files.
- **D-02:** Follow JHipster 9 GitHub Actions CI guidance as the default reference for job structure, runner choice, and caching patterns.

### Lane Structure
- **D-03:** Implement 2 primary lanes plus 1 production-validation lane:
  - `backend.yml` — Gradle compile + unit tests + integration tests + code quality (javadoc, nohttp, checkstyle, spotless)
  - `frontend.yml` — Angular build + ng unit tests + Playwright E2E (spins up backend jar + services.yml for E2E)
  - `prod-validation.yml` — Docker Compose stack up + phase12:stack:smoke + phase12:backend:prodlike
- **D-04:** Backend lane maps to the existing `ci:backend:test` npm script and its Gradle chain. Frontend lane maps to build, `ng test`, and Playwright `e2e` script sequence.
- **D-05:** The frontend lane must start a backend jar and `services.yml` dependencies before running Playwright (mirrors the existing `ci:e2e:server:start` + `ci:e2e:prepare:docker` pattern in package.json).

### Production-Validation Lane Triggers
- **D-06:** `prod-validation.yml` triggers on `workflow_dispatch` only (manual trigger). It is not attached to push or pull_request events. This keeps PR feedback fast while allowing on-demand milestone validation runs.

### Path-Based Filtering
- **D-07:** No path-based filtering on any lane. All lanes (backend and frontend) run on every push and pull_request to main. This catches cross-cutting regressions and avoids missing integration breaks when API contracts change.

### Trigger Scope (backend + frontend lanes)
- **D-08:** Both `backend.yml` and `frontend.yml` trigger on `push` to `main` and `pull_request` targeting `main`. No branch or path filtering beyond this.

### Claude's Discretion
- Exact runner label (`ubuntu-latest` is standard unless cost or Docker requirements suggest otherwise)
- Gradle wrapper caching strategy (standard `gradle-build-action` or `actions/cache` for `.gradle/`)
- npm dependency caching strategy (standard `actions/cache` for `node_modules/`)
- Whether Jib build image step is included in the backend lane or deferred to a separate release workflow

</decisions>

<specifics>
## Specific Ideas

- Use the existing `ci:backend:test`, `ci:e2e:package`, `ci:e2e:prepare:docker`, `ci:e2e:server:start`, `ci:e2e:teardown`, and `phase12:stack:smoke` / `phase12:backend:prodlike` npm scripts as the lane entry points. Do not reinvent these — wire up GitHub Actions jobs that call the existing scripts.
- JHipster 9 docs are the reference for GitHub Actions structure. Follow their two-stack CI guidance for runner setup, caching, and job sequencing.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase definition and requirements
- `.planning/ROADMAP.md` — Phase 13 goal, dependencies (Phase 12), and success criteria.
- `.planning/REQUIREMENTS.md` — `CICD-01` and `CICD-02` definitions.
- `.planning/PROJECT.md` — milestone goals and constraints.

### Phase 12 validation entry points (wire into prod-validation lane)
- `scripts/phase12-stack-smoke.sh` — deterministic smoke check against the live Compose stack; called via `npm run phase12:stack:smoke`.
- `scripts/phase12-prodlike-regression.mjs` — live-stack regression probe; called via `npm run phase12:backend:prodlike`.
- `src/main/docker/app.yml` — full-stack Compose manifest used by prod-validation lane.

### Existing CI script entry points (wire into backend and frontend lanes)
- `package.json` — root npm scripts: `ci:backend:test`, `ci:e2e:package`, `ci:e2e:prepare:docker`, `ci:e2e:server:start`, `ci:e2e:teardown:docker`, `backend:unit:test`, `backend:doc:test`, `backend:nohttp:test`.

### Existing test infrastructure
- `frontend/playwright.config.ts` — Playwright config; baseURL is `http://localhost:4200`, runner is headless Chromium.
- `frontend/e2e/` — Playwright specs: `security-comprehensive.spec.ts`, `proof-role-gating.spec.ts`, `user-management.spec.ts`, `permission-matrix.spec.ts`.
- `buildSrc/src/main/groovy/jhipster.code-quality-conventions.gradle` — Checkstyle, Spotless, nohttp, JaCoCo, Sonar setup.
- `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle` — Jib build conventions; outputs `jhipster-sec:latest`.

### Frontend and backend runtime config
- `src/main/docker/services.yml` — dependency-only compose (PostgreSQL + Mailpit) for E2E lane setup.
- `frontend/angular.json` — Angular builder config (`@angular/build:unit-test` for `ng test`).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `package.json` already has JHipster-generated `ci:*` scripts that map directly to backend and frontend lane steps. The GitHub Actions workflows call these scripts rather than reimplementing job steps.
- `scripts/phase12-stack-smoke.sh` and `scripts/phase12-prodlike-regression.mjs` are the committed production-validation entry points Phase 12 delivered; the prod-validation lane shells out to these.
- `src/main/docker/services.yml` provides the dependency-only compose stack that the frontend E2E job needs for its database.

### Established Patterns
- JHipster convention: root `package.json` is the single task runner for both backend Gradle invocations and frontend npm commands. CI lanes call `npm run <script>` rather than invoking `./gradlew` directly from workflow YAML.
- Backend integration tests use Testcontainers PostgreSQL — they require Docker socket access, which GitHub-hosted `ubuntu-latest` runners provide.
- Frontend E2E uses Playwright headless Chromium against `localhost:4200`; it requires both the Angular dev server (or built dist) and a running backend on `localhost:8080`.

### Integration Points
- `.github/workflows/backend.yml` — new file, calls `npm run ci:backend:test` as the primary step.
- `.github/workflows/frontend.yml` — new file, calls build + `ng test` + `ci:e2e:prepare:docker` + `ci:e2e:server:start` + `npm run e2e` + `ci:e2e:teardown`.
- `.github/workflows/prod-validation.yml` — new file, `workflow_dispatch` only, calls `npm run phase12:stack:smoke` and `npm run phase12:backend:prodlike` against Compose-launched stack.

</code_context>

<deferred>
## Deferred Ideas

- Path-based filtering to skip irrelevant lanes on focused changes — can be added later when CI costs or feedback latency become a concern.
- Nightly scheduled runs for the production-validation lane — can be added in a later phase or config update.
- Release/publish workflow for Jib image publishing to a registry — outside Phase 13 scope.
- Sonar/SonarCloud integration — quality plugin exists but CI reporting setup deferred.
- Matrix builds across Java or Node versions — not needed for this project's fixed version stack.

</deferred>

---

*Phase: 13-split-ci-verification-lanes*
*Context gathered: 2026-04-06*
