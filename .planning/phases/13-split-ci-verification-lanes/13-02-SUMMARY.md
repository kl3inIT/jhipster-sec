---
phase: 13-split-ci-verification-lanes
plan: "02"
subsystem: ci
tags: [github-actions, ci, frontend, playwright, e2e, production-validation]
dependency_graph:
  requires: []
  provides:
    - frontend-ci-lane
    - prod-validation-lane
  affects:
    - .github/workflows/frontend.yml
    - .github/workflows/prod-validation.yml
tech_stack:
  added:
    - GitHub Actions (frontend.yml, prod-validation.yml)
    - actions/setup-java@v4 (JDK 25, temurin)
    - gradle/actions/setup-gradle@v4
    - actions/upload-artifact@v4
  patterns:
    - Two-job frontend lane (build-and-test -> e2e) with job dependency chaining
    - Background backend process with health-poll await pattern
    - workflow_dispatch-only trigger for on-demand milestone validation
key_files:
  created:
    - .github/workflows/frontend.yml
    - .github/workflows/prod-validation.yml
  modified: []
decisions:
  - "Frontend lane uses two sequential jobs (build-and-test then e2e) so unit test failures give fast feedback before expensive E2E setup"
  - "prod-validation.yml is workflow_dispatch-only to keep PR feedback fast and avoid running Jib + full compose stack on every push"
  - "E2E job backgrounds ci:e2e:server:start then polls health via ci:server:await (180s timeout) matching existing package.json pattern"
metrics:
  duration_minutes: 8
  completed: "2026-04-06T03:39:47Z"
  tasks_completed: 2
  tasks_total: 2
  files_created: 2
  files_modified: 0
---

# Phase 13 Plan 02: Frontend CI and Production-Validation Lanes Summary

**One-liner:** Two GitHub Actions lanes — Angular build/unit-test/Playwright E2E on every push/PR, and a manual-dispatch-only production-like stack validation that builds Jib image, starts full Compose, and runs Phase 12 probes.

## What Was Built

### `.github/workflows/frontend.yml` — Frontend CI Lane

Two-job workflow triggered on push to `main` and pull_request targeting `main`.

**Job 1: `build-and-test`** (15-minute timeout)
- Sets up Node 24 with npm cache spanning both `package-lock.json` and `frontend/package-lock.json`
- Installs root and frontend dependencies
- Runs `cd frontend && npm run build` then `cd frontend && npm test` (Angular unit tests via Vitest)
- Fast-fails before triggering heavier E2E setup

**Job 2: `e2e`** (30-minute timeout, `needs: build-and-test`)
- Adds JDK 25 (temurin) and Gradle setup
- Installs Playwright chromium with system deps (`npx playwright install --with-deps chromium`)
- Packages backend: `npm run ci:e2e:package` (runs `./gradlew bootJar -Pe2e` and copies to `e2e.jar`)
- Starts dependency services: `npm run ci:e2e:prepare:docker` (PostgreSQL + Mailpit via `services.yml`)
- Backgrounds backend: `npm run ci:e2e:server:start &` (starts `java -jar e2e.jar` with profiles `e2e,secret-samples,prod`)
- Awaits health: `npm run ci:server:await` (polls `http://127.0.0.1:8080/management/health` with 180s timeout)
- Runs Playwright: `cd frontend && npm run e2e` (headless Chromium against `localhost:4200`)
- Teardown: `npm run ci:e2e:teardown` on `always()` (stops services.yml containers)
- Uploads `frontend/playwright-report/` artifact on failure (7-day retention)

**Concurrency:** group `frontend-${{ github.ref }}`, cancel-in-progress.

### `.github/workflows/prod-validation.yml` — Production-Validation Lane

Single-job workflow triggered exclusively by `workflow_dispatch` — no push, no pull_request.

- JDK 25, Node 24, Gradle setup
- Builds Jib image: `./gradlew -Pprod jibDockerBuild` (produces `jhipster-sec:latest`)
- Starts full compose stack: `docker compose -f src/main/docker/app.yml up -d`
- Waits for readiness: polls `http://localhost:8080/management/health/readiness` every 2s with 120s `timeout` guard
- Runs `npm run phase12:stack:smoke` (calls `scripts/phase12-stack-smoke.sh`)
- Runs `npm run phase12:backend:prodlike` (calls `scripts/phase12-prodlike-regression.mjs`)
- Tears down: `docker compose -f src/main/docker/app.yml down -v` on `always()`
- No concurrency group (dispatch-only; parallel runs acceptable)

## Decisions Made

1. **Two sequential frontend jobs** — `build-and-test` gates `e2e` so compilation or unit-test failures produce fast feedback (< 5 min) without wasting runner time on Gradle packaging and Docker setup.

2. **`workflow_dispatch`-only for prod-validation** — Building the Jib image and starting the full Compose stack would add 10-15 minutes to every push. Keeping it manual preserves PR feedback speed while making milestone validation repeatable and fully automated when triggered.

3. **Background + health-poll pattern** — `ci:e2e:server:start &` matches the existing JHipster package.json pattern; `ci:server:await` (wait-on, 180s) is the authoritative health gate already tested in Phase 12 runs.

4. **No path filters on frontend lane** — Per decision D-07, all lanes run on every push/PR to catch cross-cutting regressions (e.g., API contract changes that break Playwright specs).

## Deviations from Plan

None — plan executed exactly as written.

## Threat Model Coverage

- **T-13-03 (Tampering):** Jib builds from committed Gradle config; runner is ephemeral. Accepted.
- **T-13-04 (Information Disclosure):** `prod-validation.yml` uses `secret-samples` profile (non-sensitive placeholder secrets). No real credentials in CI. Mitigated.
- **T-13-05 (DoS — resource consumption):** 15/30-minute timeouts on frontend jobs; concurrency group cancels duplicate runs. 20-minute timeout on prod-validation. Mitigated.

## Known Stubs

None — workflows call existing scripts; no placeholder data flows.

## Threat Flags

None — no new network endpoints, auth paths, file access patterns, or schema changes introduced.

## Self-Check: PASSED

- `.github/workflows/frontend.yml` — FOUND
- `.github/workflows/prod-validation.yml` — FOUND
- Commit `e4d0243` (frontend.yml) — FOUND
- Commit `144fc80` (prod-validation.yml) — FOUND
- Automated verification scripts for both files — PASSED
