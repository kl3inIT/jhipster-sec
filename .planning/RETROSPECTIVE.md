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

## Cross-Milestone Trends

### Process Evolution

| Milestone | Sessions | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.0 | not tracked | 5 | Established the security-platform migration pattern: preserve baseline flows, centralize enforcement, prove on sample entities, then ship the frontend |

### Cumulative Quality

| Milestone | Tests | Coverage | Zero-Dep Additions |
|-----------|-------|----------|-------------------|
| v1.0 | Backend unit + integration coverage plus frontend Vitest and human UAT | Coverage not aggregated in planning artifacts | Several runtime additions reused the existing Spring and Angular stack rather than introducing a new platform dependency |

### Top Lessons (Verified Across Milestones)

1. Final human-verification outcomes must be folded back into planning docs before archival.
2. Security features are safer to scale when CRUD, row, attribute, and fetch-plan enforcement share one explicit access pipeline.
