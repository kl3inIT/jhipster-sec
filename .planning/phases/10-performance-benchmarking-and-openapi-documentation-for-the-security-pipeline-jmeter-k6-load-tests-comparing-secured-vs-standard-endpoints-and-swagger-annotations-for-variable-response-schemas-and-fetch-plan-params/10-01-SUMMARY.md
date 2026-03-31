---
phase: 10-performance-benchmarking-and-openapi-documentation
plan: 01
subsystem: testing
tags: [k6, performance, benchmark, openapi, spring-boot]
requires:
  - phase: 9-enterprise-ux-and-performance-hardening
    provides: request-scoped permission caching and stable secured Organization endpoints for benchmark comparison
provides:
  - api-docs-profile benchmark baseline endpoint for Organization list/detail reads
  - k6 scripts for secured vs baseline latency comparison at 1/10/50 VU
  - markdown and raw-json benchmark result export via k6 handleSummary
  - manual runbook for local benchmark setup and execution
affects: [phase-10-plan-02-openapi-annotations, performance-validation, manual-benchmark-runs]
tech-stack:
  added: [k6 scripts]
  patterns:
    - profile-gated benchmark controller hidden from generated OpenAPI
    - shared k6 auth helper module with setup token flow
    - scenario-tagged secured vs baseline metrics with summary generation
key-files:
  created:
    - src/main/java/com/vn/core/web/rest/BenchmarkOrganizationResource.java
    - load-tests/scripts/auth.js
    - load-tests/scripts/org-list-benchmark.js
    - load-tests/scripts/org-detail-benchmark.js
    - load-tests/README.md
    - load-tests/results/.gitkeep
  modified: []
key-decisions:
  - "Benchmark baseline endpoint reuses SecuredEntityJsonAdapter serialization so measured delta isolates permission and CRUD-check overhead."
  - "Benchmark endpoint is gated with @Profile('api-docs') and hidden with @Hidden to keep it dev-only and excluded from OpenAPI output."
patterns-established:
  - "k6 scripts use paired secured/baseline scenarios per VU level with endpoint tags and shared threshold keys."
  - "k6 handleSummary writes both markdown and raw JSON artifacts directly into load-tests/results/."
requirements-completed: [BENCH-01]
duration: 11 min
completed: 2026-03-31
---

# Phase 10 Plan 01: Performance Benchmarking Infrastructure Summary

**k6 secured-vs-baseline Organization benchmark suite with a profile-gated unconstrained baseline resource and Markdown overhead reporting.**

## Performance

- **Duration:** 11 min
- **Started:** 2026-03-31T03:48:57Z
- **Completed:** 2026-03-31T03:59:44Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Added `BenchmarkOrganizationResource` under `/api/benchmark/organizations` using `UnconstrainedDataManager` and shared `SecuredEntityJsonAdapter` serialization, gated to `api-docs` and hidden from OpenAPI.
- Implemented reusable k6 auth module plus list/detail benchmark scripts with 6 scenarios each (secured/baseline at 1, 10, 50 VU), threshold tags, and `handleSummary` export.
- Added `load-tests/README.md` with actionable local setup and run instructions, plus tracked `load-tests/results/.gitkeep` for output artifacts.

## Task Commits

Each task was committed atomically:

1. **Task 1: Create BenchmarkOrganizationResource and k6 scripts with shared auth** - `7b8dae9` (feat)
2. **Task 2: Create load-tests README and results directory** - `bbef2fd` (docs)

## Files Created/Modified

- `D:/jhipster/.claude/worktrees/agent-a6fd738a/src/main/java/com/vn/core/web/rest/BenchmarkOrganizationResource.java` - dev-only benchmark controller for baseline list/detail Organization reads.
- `D:/jhipster/.claude/worktrees/agent-a6fd738a/load-tests/scripts/auth.js` - k6 JWT authentication and authorization header helper.
- `D:/jhipster/.claude/worktrees/agent-a6fd738a/load-tests/scripts/org-list-benchmark.js` - list endpoint benchmark scenarios and summary export.
- `D:/jhipster/.claude/worktrees/agent-a6fd738a/load-tests/scripts/org-detail-benchmark.js` - detail endpoint benchmark scenarios and summary export.
- `D:/jhipster/.claude/worktrees/agent-a6fd738a/load-tests/README.md` - manual install, boot, run, and interpretation guide for benchmark execution.
- `D:/jhipster/.claude/worktrees/agent-a6fd738a/load-tests/results/.gitkeep` - tracked placeholder for benchmark outputs.

## Decisions Made

- Used the same `SecuredEntityJsonAdapter` fetch-plan serialization on baseline responses to avoid conflating serializer behavior with security pipeline overhead.
- Returned 404 for missing Organization IDs in benchmark detail endpoint by catching `EntityNotFoundException` from `UnconstrainedDataManager.load(...)`.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## Authentication Gates

None.

## User Setup Required

External service configuration required for benchmark execution:

- Install k6 on Windows using `winget install k6 --source winget` (or download from Grafana releases).

## Next Phase Readiness

- Benchmark infrastructure and runbook are complete and ready for manual benchmark runs.
- Phase 10 plan 02 can proceed with OpenAPI annotation work independently.

## Self-Check: PASSED

- Verified all expected files exist on disk.
- Verified task commits `7b8dae9` and `bbef2fd` exist in git history.
