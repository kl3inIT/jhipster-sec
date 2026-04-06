# Roadmap: JHipster Security Platform

## Milestones

- [shipped] **v1.0 MVP** - Phases 1-5 shipped 2026-03-25. Archive: `.planning/milestones/v1.0-ROADMAP.md`
- [shipped] **v1.1 Enterprise Admin Experience** - Phases 6-11 shipped 2026-03-31. Archive: `.planning/milestones/v1.1-ROADMAP.md`
- [active] **v1.2 CI/CD & Production Validation** - Phases 12-16 planned. Focus: production-like runtime composition, split CI verification, trusted benchmark baselines, permission lookup optimization, and production security proof.

## Progress

| Milestone | Phases | Requirements | Status | Archive |
|-----------|--------|--------------|--------|---------|
| v1.0 MVP | 1-5 | 18/18 complete | Shipped 2026-03-25 | `.planning/milestones/v1.0-ROADMAP.md` |
| v1.1 Enterprise Admin Experience | 6-11 | 23/23 complete | Shipped 2026-03-31 | `.planning/milestones/v1.1-ROADMAP.md` |
| v1.2 CI/CD & Production Validation | 12-16 | 0/9 complete | Planned | - |

## Overview

This roadmap starts v1.2 by making the production-like runtime reproducible, then separates CI so backend, frontend, and stack validation feedback can run predictably, then establishes an explicit production-like benchmark baseline with realistic data volume, multiple tables, and concurrency before any deeper permission optimization begins. Only after that trusted baseline exists does the roadmap optimize permission lookup behavior and close with production security proof against the realistic stack.

## Phases

**Phase Numbering:**
- Integer phases continue across milestones by default.
- `v1.1` ended at Phase 11, so `v1.2` starts at Phase 12.

- [ ] **Phase 12: Production Runtime Foundation** - Make the production-like compose and runtime configuration usable as the baseline environment for the milestone.
- [x] **Phase 13: Split CI Verification Lanes** - Separate backend, frontend, and production-validation automation so failures are isolated and repeatable. (completed 2026-04-06)
- [ ] **Phase 14: Production-Like Benchmark Baseline** - Establish the trusted benchmark dataset, concurrency runs, and stack validation evidence before optimization starts.
- [ ] **Phase 15: Permission Lookup Optimization** - Improve permission-path performance against the trusted benchmark baseline without weakening security guarantees.
- [ ] **Phase 16: Production Security Proof** - Prove the optimized system still enforces the right security behavior in the production-like stack.

## Phase Details

### Phase 12: Production Runtime Foundation
**Goal**: A production-like runtime stack can be started from committed configuration and compose assets so milestone validation happens against a realistic environment instead of dev-only shortcuts.
**Depends on**: Phase 11
**Requirements**: PROD-01, PROD-02
**Success Criteria** (what must be TRUE):
  1. The application stack can be started from committed production-like configuration and compose assets without requiring ad hoc local-only setup.
  2. Auth, account, admin-user, and secured-entity flows work in the production-like stack so later validation is grounded in a realistic runtime shape.
  3. The stack exposes the runtime dependencies needed for later benchmark and security-validation work, including database-backed application startup and operationally relevant configuration.
**Plans**: 2 plans
Plans:
- [x] 12-01-PLAN.md u2014 Normalize the prod profile and Docker Compose runtime contract around env-driven configuration and the existing Jib image path.
- [ ] 12-02-PLAN.md u2014 Add repeatable backend and browser validation entry points for the production-like stack baseline.

### Phase 13: Split CI Verification Lanes
**Goal**: CI feedback is separated into reliable backend, frontend, and production-validation lanes so milestone regressions are easier to detect and rerun.
**Depends on**: Phase 12
**Requirements**: CICD-01, CICD-02
**Success Criteria** (what must be TRUE):
  1. Backend verification can fail independently from frontend verification so one lane does not hide the status of the other.
  2. Frontend verification can run without waiting on unrelated backend-only work when the UI lane is the only thing that changed.
  3. The production-validation prerequisites for this milestone can be launched through repeatable CI automation instead of manual operator setup.
**Plans**: 2 plans
Plans:
- [x] 13-01-PLAN.md u2014 Create the backend CI verification lane as a standalone GitHub Actions workflow.
- [x] 13-02-PLAN.md u2014 Create the frontend CI lane and production-validation lane as GitHub Actions workflows.

### Phase 13.1: Continuous Deployment Pipeline (INSERTED)

**Goal:** Add a separate continuous deployment workflow that publishes the Jib-built backend image to GHCR and SSH-deploys to production only after Backend CI succeeds on main, then verifies readiness.
**Requirements**: D-01 to D-14 (locked in `13.1-CONTEXT.md`)
**Depends on:** Phase 13
**Plans:** 1/1 plans complete

Plans:
- [x] 13.1-01-PLAN.md — Add the GHCR publish, SSH deploy, and readiness-gated Continuous Deployment workflow.

### Phase 14: Production-Like Benchmark Baseline
**Goal**: The project has a trusted production-like benchmark and validation baseline with realistic data volume, multiple secured tables, and concurrency evidence before optimization decisions are made.
**Depends on**: Phase 13
**Requirements**: BENCH-02, BENCH-03, VAL-01
**Success Criteria** (what must be TRUE):
  1. Benchmark runs use a production-like dataset that exercises multiple secured tables and representative relationship depth rather than a narrow single-table sample.
  2. Benchmark summaries show secured vs baseline behavior at multiple concurrency levels and persist results that can be compared across reruns.
  3. The team has an explicit validation report showing the runtime stack and benchmark design are trusted enough to guide later optimization work.
  4. Permission optimization work is blocked from proceeding until this baseline evidence exists and is reviewable.
**Plans**: TBD

### Phase 15: Permission Lookup Optimization
**Goal**: Permission lookup overhead is reduced against the trusted production-like benchmark baseline while preserving current security behavior.
**Depends on**: Phase 14
**Requirements**: PERF-05
**Success Criteria** (what must be TRUE):
  1. Benchmark reruns show measurable improvement over the production-like baseline for the permission-driven bottlenecks identified in Phase 14.
  2. CRUD, authority, attribute-level, and fetch-plan security outcomes remain correct after the optimization work.
  3. The optimized path can be explained in benchmark evidence as an improvement to permission lookup behavior rather than a change in workload shape.
**Plans**: TBD

### Phase 16: Production Security Proof
**Goal**: The optimized system is proven secure and production-ready under realistic runtime conditions using the production-like stack and benchmark evidence.
**Depends on**: Phase 15
**Requirements**: SEC-05
**Success Criteria** (what must be TRUE):
  1. Production-like validation shows the optimized stack still enforces expected allow and deny outcomes for secured business operations.
  2. The final proof package links runtime validation and benchmark evidence so performance gains are not accepted without preserved security behavior.
  3. The milestone closes with production-like evidence that the deployment path, benchmark baseline, and permission optimization all hold together coherently.
**Plans**: TBD

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|----------|
| 12. Production Runtime Foundation | 1/2 | In Progress|  |
| 13. Split CI Verification Lanes | 2/2 | Complete   | 2026-04-06 |
| 14. Production-Like Benchmark Baseline | 0/TBD | Not started | - |
| 15. Permission Lookup Optimization | 0/TBD | Not started | - |
| 16. Production Security Proof | 0/TBD | Not started | - |
