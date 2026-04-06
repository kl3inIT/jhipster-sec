---
phase: 12-production-runtime-foundation
plan: 01
subsystem: infra
tags: [docker-compose, spring-boot, jib, postgresql, mailpit]
requires:
  - phase: 11-performance-hardening
    provides: benchmarked security runtime with preserved auth, admin-user, mail, and secured-entity behavior
provides:
  - environment-driven prod profile for datasource, Liquibase, and mail base URL
  - password-backed PostgreSQL compose baseline with health-gated startup
  - Jib runtime env contract aligned across Spring Boot, Docker Compose, and entrypoint secret loading
affects: [phase-12-validation, ci, production-runtime, benchmark-baseline]
tech-stack:
  added: []
  patterns: [environment-driven production profile, compose health-gated dependency startup, Jib _FILE runtime resolution]
key-files:
  created: []
  modified:
    - src/main/resources/config/application-prod.yml
    - src/main/docker/app.yml
    - src/main/docker/postgresql.yml
    - src/main/docker/jib/entrypoint.sh
key-decisions:
  - "Keep the production-like stack on the existing jhipster-sec:latest Jib image instead of introducing a second container build path."
  - "Use one env contract for datasource, Liquibase, and mail base URL across Spring Boot, Compose, and the Jib entrypoint."
patterns-established:
  - "Production-like runtime config lives in committed compose assets and Spring placeholders, not machine-specific literals."
  - "Backend startup waits on healthy PostgreSQL and Mailpit dependencies before the app readiness check is evaluated."
requirements-completed: [PROD-01]
duration: 0 min
completed: 2026-04-02
---

# Phase 12 Plan 01: Production Runtime Foundation Summary

**Portable prod-profile datasource and mail runtime wiring backed by a passworded PostgreSQL compose baseline and Jib-based container startup.**

## Performance

- **Duration:** 0 min
- **Started:** 2026-04-02T08:08:27Z
- **Completed:** 2026-04-02T08:09:13Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Replaced machine-specific production datasource settings with Spring environment placeholders shared by datasource and Liquibase configuration.
- Normalized the production-like compose stack around the existing `jhipster-sec:latest` Jib image, PostgreSQL, and Mailpit.
- Added file-backed env resolution for `JHIPSTER_MAIL_BASE_URL` and switched backend health checks to readiness-aware startup validation.

## Task Commits

Each task was committed atomically:

1. **Task 1: Externalize the prod-profile runtime contract** - `9445976` (feat)
2. **Task 2: Align Compose and entrypoint files to one production-like env contract** - `149c910` (feat)

**Plan metadata:** Pending

## Files Created/Modified
- `src/main/resources/config/application-prod.yml` - Externalized datasource, Liquibase, and mail base-url values behind Spring environment placeholders.
- `src/main/docker/app.yml` - Aligned the app service with the Jib image, shared datasource env contract, readiness health check, and dependency health gates.
- `src/main/docker/postgresql.yml` - Replaced trust auth with explicit database, user, and password environment variables plus DB-aware readiness.
- `src/main/docker/jib/entrypoint.sh` - Added `_FILE` handling for the mail base URL used by the production-like stack.

## Decisions Made
- Kept the compose stack on the existing `jhipster-sec:latest` Jib image so Phase 12 preserves one backend packaging path.
- Used one environment-variable contract across Spring Boot config, compose manifests, and the Jib entrypoint to reduce runtime drift.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Phase 12 now has a portable production-like runtime contract ready for validation entry points in `12-02-PLAN.md`.
- PostgreSQL, Mailpit, and app readiness behavior now expose the baseline runtime dependencies needed by later validation and benchmark work.

---
*Phase: 12-production-runtime-foundation*
*Completed: 2026-04-02*

## Self-Check: PASSED
- Found `D:\jhipster\.planning\phases\12-production-runtime-foundation\12-01-SUMMARY.md`.
- Verified task commits `9445976` and `149c910` exist in git history.
