# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.0 - MVP

**Shipped:** 2026-03-25
**Phases:** 5 | **Plans:** 30 | **Git commits:** 160

### What Was Built
- Preserved the existing backend auth, account, admin-user, and mail behavior while bridging authority assignments into the merged security runtime.
- Added merged security metadata management plus a centralized secure data-access layer with CRUD, row-level, attribute-level, and YAML/code-defined fetch-plan enforcement.
- Proved the model end to end with proof entities and shipped a standalone Angular frontend for login, security administration, and protected-entity screens.

### What Worked
- Phase-by-phase verification kept backend and frontend security behavior grounded in concrete evidence instead of assumption.
- Using proof entities before broader `angapp` workflow migration created a contained way to validate the security platform end to end.

### What Was Inefficient
- Planning metadata drifted after the final frontend UAT pass, so milestone completion required cleanup of stale roadmap, requirements, verification, and validation records.
- Some phase-summary one-liners were too noisy for archival automation, which reduced the quality of the first generated milestone summary.

### Patterns Established
- Security enforcement changes should land behind `SecureDataManager` and capability endpoints, not in ad hoc controller or component logic.
- Proof-domain entities are the right place to validate new security behaviors before applying them to broader business workflows.
- Human UAT remains necessary for auth/session flows, frontend admin persistence, and permission-gated UI behavior even when automated coverage is strong.

### Key Lessons
1. The brownfield migration stayed stable because the existing auth/admin lane was preserved first, then new security behavior was layered in behind explicit runtime seams.
2. Planning artifacts need a final sync immediately after human UAT; otherwise milestone archival tooling will faithfully snapshot stale state.

### Cost Observations
- Model mix: not tracked in repository artifacts
- Sessions: not tracked directly; git history for the milestone contains 160 commits
- Notable: most product risk concentrated in the frontend verification tail rather than the backend enforcement core

---

## Milestone: v1.1 - Enterprise Admin Experience

**Shipped:** 2026-03-31
**Phases:** 10 | **Plans:** 39 | **Git commits:** not tracked in retrospective metadata

### What Was Built
- Delivered enterprise admin parity in the standalone frontend: backend-driven navigation, full user-management flows, and menu/permission management.
- Completed security-core realignment: live authority refresh, typed secured-entity internals, explicit fail-closed validation, and row-policy retirement.
- Shipped performance/documentation work for secured entities and completed Phase 11 hardening to meet PERF-04 overhead target with load-test proof.

### What Worked
- Keeping security enforcement behind `SecureDataManager`/`DataManager` boundaries allowed aggressive refactors without API contract breakage.
- Phase-level verification artifacts made gap discovery fast when traceability drift appeared.

### What Was Inefficient
- Milestone metadata drifted (ROADMAP progress table, REQUIREMENTS traceability, and milestone audit naming), requiring end-of-milestone bookkeeping cleanup.
- Some summary metadata (for example one-liner fields) remained inconsistent, reducing automation quality in milestone summarization.

### Patterns Established
- Performance regressions in secured pipelines should be closed with benchmark artifacts committed under `load-tests/results/` as release evidence.
- Requirement closure needs synchronized updates across summary frontmatter, verification tables, and REQUIREMENTS traceability in the same phase window.

### Key Lessons
1. Technical delivery can be complete while planning artifacts are still stale; close documentation drift before archival to avoid false blockers.
2. Security performance improvements are most reliable when each optimization is paired with measurable before/after evidence and explicit pass/fail KPI framing.

### Cost Observations
- Model mix: balanced profile used across planning and execution artifacts
- Sessions: not tracked directly in retrospective metadata
- Notable: highest effort concentrated in security-pipeline performance diagnosis and closure (Phases 10-11)

---
## Cross-Milestone Trends

### Process Evolution

| Milestone | Sessions | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.0 | not tracked | 5 | Established the security-platform migration pattern: preserve baseline flows, centralize enforcement, prove on sample entities, then ship the frontend |
| v1.1 | not tracked | 10 | Extended the pattern with benchmark-driven performance closure and tighter requirements/verification traceability discipline |

### Cumulative Quality

| Milestone | Tests | Coverage | Zero-Dep Additions |
|-----------|-------|----------|-------------------|
| v1.0 | Backend unit + integration coverage plus frontend Vitest and human UAT | Coverage not aggregated in planning artifacts | Several runtime additions reused the existing Spring and Angular stack rather than introducing a new platform dependency |
| v1.1 | Added targeted unit/integration suites for security pipeline hot paths plus benchmark artifacts | PERF-04 proven by committed load-test results; frontend reliability backlog remains | Continued reusing existing Spring/Angular/Hazelcast stack without introducing new platform dependencies |

### Top Lessons (Verified Across Milestones)

1. Final human-verification outcomes must be folded back into planning docs before archival.
2. Security features are safer to scale when CRUD, row, attribute, and fetch-plan enforcement share one explicit access pipeline.

