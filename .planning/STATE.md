---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: "Phase 09 shipped — PR #12"
stopped_at: Completed quick/260330-eke-SUMMARY.md
last_updated: "2026-03-30T08:26:49.104Z"
last_activity: "2026-03-30 - Completed quick task 260330-eke: fix backend permission evaluation entity wildcard and edit-implies-view"
progress:
  total_phases: 8
  completed_phases: 8
  total_plans: 33
  completed_plans: 33
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-27)

**Core value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, authority, and attribute-level access decisions.
**Current focus:** Phase 10 — frontend-reliability-and-regression-coverage

## Current Position

Phase: 10 (frontend-reliability-and-regression-coverage) — READY TO PLAN
Next: Phase 10 (frontend-reliability-and-regression-coverage)

## Performance Metrics

**Velocity:**

- Total plans completed this milestone: 7
- Average duration: 12.1 min
- Total execution time: 1.4 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 6 - Frontend Parity Foundation | 6 | 50 min | 8.3 min |
| 7 - Enterprise Navigation Shell | 0 | - | - |
| 07.1 - Menu Management | 3 | - | - |
| 8 - User Management Delivery | 0 | - | - |
| 8.1 - Jmix-Style DataManager Core Alignment | 3 | 60 min | 20.0 min |
| 8.2 - Multi-App Menu Roles and Jmix-Style JSON Entity Controllers | 0 | - | - |
| 8.3 - Registration, Live Permission Refresh, Typed Entity Flow, Validation, And Row-Policy Removal | 0 | - | - |
| 9 - Enterprise UX And Performance Hardening | 3 | 31 min | 10.3 min |
| 10 - Frontend Reliability And Regression Coverage | 0 | - | - |

**Recent Trend:**

- Phase 08.1 completed with verified `DataManager` / `UnconstrainedDataManager` layering and union-of-`ALLOW` resource semantics.
- Phase 08.2 completed with explicit secure `PATCH`, per-app menu isolation proof, first-grant multi-app role assignment, and green backend or frontend verification.
- Phase 08.3 completed with standalone registration, request-time authority refresh, typed secured entity flow, explicit validation hardening, and full row-policy retirement.
- Phase 9 completed with request-scoped permission caching, signal-based entity lists, responsive columns, and first-render skeleton visibility fixes.

| Phase 07 P05 | 4 | 2 tasks | 19 files |
| Phase 07.1-menu-management P01 | 25 | 2 tasks | 10 files |
| Phase 07.1-menu-management P02 | 5 | 2 tasks | 10 files |
| Phase 07.1-menu-management P03 | 5 | 2 tasks | 4 files |
| Phase 08 P01 | 2 | 2 tasks | 4 files |
| Phase 08 P02 | 10 | 2 tasks | 14 files |
| Phase 08 P03 | 10 min | 2 tasks | 12 files |
| Phase 08 P04 | 5 min | 2 tasks | 2 files |
| Phase 08.1 P01 | 14 min | 2 tasks | 8 files |
| Phase 08.1 P02 | 11 min | 2 tasks | 9 files |
| Phase 08.1 P03 | 35 min | 2 tasks | 4 files |
| Phase 08.2 P03 | 23 min | 2 tasks | 12 files |
| Phase 08.2 P02 | 16 min | 2 tasks | 6 files |
| Phase 08.2 P04 | 9 min | 2 tasks | 9 files |
| Phase 09 P01 | 7 min | 3 tasks | 7 files |
| Phase 09 P02 | 22 min | 2 tasks | 8 files |
| Phase 09 P03 | 2 min | 2 tasks | 3 files |

## Accumulated Context

### Roadmap Evolution

- Phase 07.1 inserted after Phase 7: Menu Management (INSERTED) - admin CRUD for `SecMenuDefinition` and role-based `SecMenuPermission` assignment.
- Phase 08.1 inserted after Phase 8: Jmix-Style DataManager Core Alignment (URGENT) - aligned the internal data-access architecture with a Jmix-style `DataManager` / `UnconstrainedDataManager` split while preserving the `SecureDataManager` facade and brownfield contract.
- Phase 08.2 inserted after Phase 08.1: Multi-App Menu Roles and Jmix-Style JSON Entity Controllers (URGENT) - restored secured `loadByQuery`, supported multi-app menu-role assignment, and moved protected entity boundaries to raw JSON with explicit `PATCH`.
- Phase 08.3 inserted after Phase 08.2: User registration, live permission refresh, entity-native serialization, validation hardening, and row-policy removal - closes missing self-registration, removes stale authority snapshots, hardens the secured entity pipeline, and retires row policy before Phase 9.
- Phase 10 replaced: original frontend reliability and regression coverage phase removed; replaced with performance benchmarking (JMeter/k6 load tests, secured vs standard pipeline) and OpenAPI documentation (Swagger annotations for variable response schemas and fetch-plan params).

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- `SecureDataManager` remains the stable application-facing facade while `DataManager` becomes the secure-default internal layer with explicit `unconstrained()` bypass.
- Resource permissions now use default deny plus union-of-`ALLOW` semantics across entity, attribute, menu, and capability surfaces.
- Verification for this repository must run under Java 25 and use Gradle's `integrationTest` task for `*IT` suites because the `test` task excludes integration classes by design.
- Phase 08.3 will refresh current-user authorities from database state at request time rather than trusting the authority snapshot captured at login.
- Phase 08.3 will move secured entity internals toward typed entity-native flows and keep JSON parsing or serialization confined to explicit edge adapters.
- Phase 08.3 will remove row policy completely instead of expanding or preserving it as a long-term platform feature.
- [Phase 09]: Request-local permission snapshot (D-01/D-02): caches authority validation and PermissionMatrix per HTTP request, destroyed at request end, with graceful non-web fallback via isRequestScopeActive() guard
- [Phase 09]: Use fromEvent(window, resize) with debounceTime for responsive detection since @angular/cdk is not available
- [Phase 09]: Skeleton loaders use tableValue computed signal switching between 5 skeletonRows and real data array for initial-fetch-only skeleton
- [Phase 09]: Initialize entity list loading signals to true so first-render skeleton rows appear before data arrives.
- [Phase 09]: Use an empty verification commit when a required task only performs build validation and introduces no tracked file changes.

### Pending Todos

- Plan or execute Phase 10 - frontend reliability and regression coverage.
- Performance test backend security API vs standard JHipster API (JMeter/k6/Gatling benchmark comparing secured pipeline overhead).
- Configure Swagger OpenAPI docs for security-gated endpoints (variable response schemas, fetch-plan params, `@SecuredEntity` markers).

### Blockers/Concerns

- No open milestone blockers.
- Frontend production build still exceeds the configured initial bundle budget warning threshold.
- Planning debt remains on validation metadata for phases 1, 3, and 4; product verification and the milestone audit both passed.

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 260327-shg | Jmix-style 3-layer organization workbench with nested reference CRUD for security/performance testing | 2026-03-27 | pending | [260327-shg-task-l-m-m-t-m-n-v-i-t-nh-t-3-l-p-v-d-s-](./quick/260327-shg-task-l-m-m-t-m-n-v-i-t-nh-t-3-l-p-v-d-s-/) |
| 260327-usx | Convert menu permission appName from String to enum with schema migration | 2026-03-27 | pending | [260327-usx-ch-qu-n-l-quy-n-c-a-menu-i-c-i-appname-t](./quick/260327-usx-ch-qu-n-l-quy-n-c-a-menu-i-c-i-appname-t/) |
| 260328-d4b | configure local mail catcher for dev registration | 2026-03-28 | pending | [260328-d4b-configure-local-mail-catcher-for-dev-reg](./quick/260328-d4b-configure-local-mail-catcher-for-dev-reg/) |
| 260328-f1a | fix duplicate register text on login page | 2026-03-28 | pending | [260328-f1a-fix-duplicate-register-text-on-login-pag](./quick/260328-f1a-fix-duplicate-register-text-on-login-pag/) |
| 260330-e78 | Fix permission matrix: modify-implies-view and entity wildcard row | 2026-03-30 | 7c2a0d4 | [260330-e78-fix-permission-matrix-attribute-logic-li](./quick/260330-e78-fix-permission-matrix-attribute-logic-li/) |
| 260330-eke | fix backend permission evaluation: entity wildcard (*) and edit-implies-view attribute cascade | 2026-03-30 | eec037e | [260330-eke-fix-backend-permission-evaluation-entity](./quick/260330-eke-fix-backend-permission-evaluation-entity/) |

## Session Continuity

Last activity: 2026-03-30 - Completed quick task 260330-eke: fix backend permission evaluation entity wildcard and edit-implies-view
Last session: 2026-03-30T03:13:29.000Z
Stopped at: Completed quick/260330-eke-SUMMARY.md
Resume file: None
