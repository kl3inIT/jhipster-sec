---
phase: 12-production-runtime-foundation
plan: 02
subsystem: validation
tags: [docker-compose, runtime-validation, prodlike, regression]
requires:
  - phase: 12-production-runtime-foundation
    provides: portable compose-backed runtime contract from plan 12-01
provides:
  - compose-backed stack smoke command for production-like readiness proof
  - live-stack regression command for auth/account, admin-user, and secured-entity flows
  - validation contract narrowed to the milestone scope the user approved
affects: [phase-12-verification, ci, production-runtime]
tech-stack:
  added: []
  patterns: [compose-backed smoke validation, live-stack HTTP regression probes, scope-honest phase validation]
key-files:
  created:
    - scripts/phase12-stack-smoke.sh
    - scripts/phase12-prodlike-regression.mjs
  modified:
    - package.json
    - src/main/docker/app.yml
    - .planning/phases/12-production-runtime-foundation/12-VALIDATION.md
key-decisions:
  - "Use committed live-stack commands as the primary Phase 12 runtime proof instead of relying only on in-process integration suites."
  - "Narrow Phase 12 validation to auth/account, admin-user, and secured-entity flows; defer mail delivery, registration, and forgot-password proof to a later milestone."
patterns-established:
  - "Phase runtime smoke uses the Compose-launched stack and readiness endpoint as the baseline health proof."
  - "Live-stack regression scripts must describe exactly what the milestone proves and explicitly defer out-of-scope flows."
requirements-completed: [PROD-02]
duration: 0 min
completed: 2026-04-02
---

# Phase 12 Plan 02: Production Runtime Validation Summary

**Committed smoke and live-stack regression commands now prove the compose-launched runtime for the approved Phase 12 scope: auth/account, admin-user, and secured-entity behavior.**

## Performance

- **Duration:** 0 min
- **Tasks:** 2
- **Files created:** 2
- **Files modified:** 3

## Accomplishments
- Added `phase12:stack:smoke` to start the committed Compose stack, verify app/postgresql/mailpit container health, and check `/management/health/readiness` against the live backend.
- Added `phase12:backend:prodlike` to authenticate against the Compose-launched backend and validate auth/account, admin-user, capability, and secured-entity allow/deny behavior over HTTP.
- Updated Phase 12 validation docs to keep the milestone claims honest after the user narrowed scope to exclude Mailpit proof, registration, and forgot-password flows.

## Task Commits

1. **Task 1: Add a Compose-backed stack smoke command for the finalized runtime contract** — `03e94d4` (feat), `639f9ac` (fix), `057dc21` (fix)
2. **Task 2: Add live-stack PROD-02 regression probes that mirror the brownfield-safe runtime flows** — `3aecedc` (feat), `dda1080` (fix)

## Files Created/Modified
- `package.json` — Added `phase12:stack:smoke`, `phase12:stack:down`, and `phase12:backend:prodlike` wrappers.
- `scripts/phase12-stack-smoke.sh` — Added repeatable Compose-backed readiness verification.
- `scripts/phase12-prodlike-regression.mjs` — Added live-stack regression checks for auth/account, admin-user, and secured-entity flows.
- `src/main/docker/app.yml` — Added runtime env support needed by the smoke path.
- `.planning/phases/12-production-runtime-foundation/12-VALIDATION.md` — Documented the narrowed Phase 12 proof contract and supporting checks.

## Decisions Made
- Treat the committed smoke and regression commands as the primary Phase 12 PROD-02 proof.
- Defer mail delivery, registration, and forgot-password validation from this milestone based on explicit user direction.

## Deviations from Plan
- The original plan and validation draft included mail-oriented proof. That portion was intentionally removed after the user clarified it is not required in this milestone.

## Issues Encountered
- The narrowed live-stack commands pass, but the supporting Gradle integration suite still reports existing failures in `src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java`. Those failures were not introduced by the final scope-narrowing changes and were left as follow-up work per user direction to finish 12-02 now.

## Verification
- Passed: `npm run phase12:stack:smoke`
- Passed: `npm run phase12:backend:prodlike`
- Failing supporting suite: `./gradlew integrationTest --tests com.vn.core.web.rest.AccountResourceIT --tests com.vn.core.web.rest.UserResourceIT --tests com.vn.core.web.rest.SecuredEntityCapabilityResourceIT --tests com.vn.core.web.rest.SecuredEntityEnforcementIT`

## Next Phase Readiness
- Phase 12 now has committed runtime commands that later CI work can split into dedicated lanes.
- Follow-up debugging is still needed for the existing `SecuredEntityEnforcementIT` failures before Phase 12 can fully clear supporting integration coverage.

---
*Phase: 12-production-runtime-foundation*
*Completed: 2026-04-02*

## Self-Check: PASSED
- Found `D:\jhipster\.planning\phases\12-production-runtime-foundation\12-02-SUMMARY.md`.
- Verified task commits `03e94d4`, `639f9ac`, `057dc21`, `3aecedc`, and `dda1080` exist in git history.
