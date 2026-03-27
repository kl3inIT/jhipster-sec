---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: ready_for_next_phase
stopped_at: Phase 08.3 execution completed; next up is Phase 9 planning
last_updated: "2026-03-27T17:16:05.273Z"
last_activity: 2026-03-28
progress:
  total_phases: 9
  completed_phases: 7
  total_plans: 30
  completed_plans: 30
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-27)

**Core value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, authority, and attribute-level access decisions.
**Current focus:** Phase 9 — enterprise-ux-and-performance-hardening

## Current Position

Phase: 08.3 (user-registration-live-permission-refresh-entity-native-serialization-validation-hardening-and-row-policy-removal) — COMPLETED
Next: Phase 9 (enterprise-ux-and-performance-hardening)

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
| 9 - Enterprise UX And Performance Hardening | 0 | - | - |
| 10 - Frontend Reliability And Regression Coverage | 0 | - | - |

**Recent Trend:**

- Phase 08.1 completed with verified `DataManager` / `UnconstrainedDataManager` layering and union-of-`ALLOW` resource semantics.
- Phase 08.2 completed with explicit secure `PATCH`, per-app menu isolation proof, first-grant multi-app role assignment, and green backend or frontend verification.
- Phase 08.3 completed with standalone registration, request-time authority refresh, typed secured entity flow, explicit validation hardening, and full row-policy retirement.
- Phase 9 is now unblocked and becomes the next active milestone focus.

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

## Accumulated Context

### Roadmap Evolution

- Phase 07.1 inserted after Phase 7: Menu Management (INSERTED) - admin CRUD for `SecMenuDefinition` and role-based `SecMenuPermission` assignment.
- Phase 08.1 inserted after Phase 8: Jmix-Style DataManager Core Alignment (URGENT) - aligned the internal data-access architecture with a Jmix-style `DataManager` / `UnconstrainedDataManager` split while preserving the `SecureDataManager` facade and brownfield contract.
- Phase 08.2 inserted after Phase 08.1: Multi-App Menu Roles and Jmix-Style JSON Entity Controllers (URGENT) - restored secured `loadByQuery`, supported multi-app menu-role assignment, and moved protected entity boundaries to raw JSON with explicit `PATCH`.
- Phase 08.3 inserted after Phase 08.2: User registration, live permission refresh, entity-native serialization, validation hardening, and row-policy removal - closes missing self-registration, removes stale authority snapshots, hardens the secured entity pipeline, and retires row policy before Phase 9.

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- `SecureDataManager` remains the stable application-facing facade while `DataManager` becomes the secure-default internal layer with explicit `unconstrained()` bypass.
- Resource permissions now use default deny plus union-of-`ALLOW` semantics across entity, attribute, menu, and capability surfaces.
- Verification for this repository must run under Java 25 and use Gradle's `integrationTest` task for `*IT` suites because the `test` task excludes integration classes by design.
- Phase 08.3 will refresh current-user authorities from database state at request time rather than trusting the authority snapshot captured at login.
- Phase 08.3 will move secured entity internals toward typed entity-native flows and keep JSON parsing or serialization confined to explicit edge adapters.
- Phase 08.3 will remove row policy completely instead of expanding or preserving it as a long-term platform feature.

### Pending Todos

- Plan or execute Phase 9 - enterprise UX and performance hardening.
- Cache current-user permission checks to remove repeated `jhi_authority` and `sec_permission` lookups during secured reads while preserving request-time refresh semantics.

### Blockers/Concerns

- No open milestone blockers.
- Frontend production build still exceeds the configured initial bundle budget warning threshold.
- Planning debt remains on validation metadata for phases 1, 3, and 4; product verification and the milestone audit both passed.

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 260327-shg | Jmix-style 3-layer organization workbench with nested reference CRUD for security/performance testing | 2026-03-27 | pending | [260327-shg-task-l-m-m-t-m-n-v-i-t-nh-t-3-l-p-v-d-s-](./quick/260327-shg-task-l-m-m-t-m-n-v-i-t-nh-t-3-l-p-v-d-s-/) |
| 260327-usx | Convert menu permission appName from String to enum with schema migration | 2026-03-27 | pending | [260327-usx-ch-qu-n-l-quy-n-c-a-menu-i-c-i-appname-t](./quick/260327-usx-ch-qu-n-l-quy-n-c-a-menu-i-c-i-appname-t/) |

## Session Continuity

Last activity: 2026-03-27
Last session: 2026-03-27T22:26:55.0533207+07:00
Stopped at: Phase 08.3 execution completed; next up is Phase 9 planning
Resume file: None
