---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: unknown
stopped_at: Completed 08.2-04-PLAN.md
last_updated: "2026-03-27T10:16:24.985Z"
last_activity: 2026-03-27
progress:
  total_phases: 8
  completed_phases: 6
  total_plans: 25
  completed_plans: 25
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-27)

**Core value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, row-level, and attribute-level access decisions.
**Current focus:** Phase 9 — enterprise-ux-and-performance-hardening

## Current Position

Phase: 9
Plan: Not started

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
| 8 - User Management Delivery | 0 | - | - |
| 8.1 - Jmix-Style DataManager Core Alignment | 3 | 60 min | 20.0 min |
| 8.2 - Multi-App Menu Roles and Jmix-Style JSON Entity Controllers | 0 | - | - |
| 9 - Enterprise UX And Performance Hardening | 0 | - | - |
| 10 - Frontend Reliability And Regression Coverage | 0 | - | - |

**Recent Trend:**

- Phase 08.1 completed with verified `DataManager` / `UnconstrainedDataManager` layering and union-of-`ALLOW` resource semantics.
- Phase 08.2 was inserted after Phase 08.1 to restore and preserve secured `loadByQuery`, extend menu-role assignment across multiple apps, and move secured entity endpoints toward a Jmix-style raw JSON + PATCH contract.
- Phase 08.2 completed with explicit secure `PATCH`, per-app menu isolation proof, first-grant multi-app role assignment, and green backend/frontend verification.
- The next actionable work is Phase 9 planning.

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
- Phase 08.2 inserted after Phase 08.1: Multi-App Menu Roles and Jmix-Style JSON Entity Controllers (URGENT) - restore and preserve secured `loadByQuery`, support multi-app menu-role assignment, and move protected entity boundaries to raw JSON with explicit PATCH.

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- `SecureDataManager` remains the stable application-facing facade while `DataManager` becomes the secure-default internal layer with explicit `unconstrained()` bypass.
- Resource permissions now use default-deny plus union-of-`ALLOW` semantics across entity, attribute, menu, and capability surfaces.
- Proof-entity integration coverage now seeds explicit attribute `ALLOW` rows in-test so verified HTTP behavior matches the current deny-default attribute model.
- Verification for this repository must run under Java 25 and use Gradle's `integrationTest` task for `*IT` suites because the `test` task excludes integration classes by design.
- [Phase 08.2]: Keep nonblank JPQL explicitly denied and treat blank-JPQL parameters as the only supported JSON filter contract.
- [Phase 08.2]: Keep SecuredEntityQueryVM at the web boundary and adapt it in controllers so the service layer stays ArchUnit-compliant.
- [Phase 08.2]: Serialize raw JSON responses explicitly in controllers because this Spring Boot 4 / Jackson stack does not safely round-trip com.fasterxml tree nodes through HTTP converters.
- [Phase 08.2]: Menu permission matrix menu state now keys grants and pending changes by appName::menuId to avoid cross-app collisions.
- [Phase 08.2]: The permission matrix now renders its menu tree from SecMenuDefinition rows for the selected app instead of the static APP_NAVIGATION_TREE registry.
- [Phase 08.2]: Explicit PATCH endpoints reuse the existing secure save pipeline — This keeps omitted fields untouched and centralizes attribute-level enforcement in the existing secure merge path.
- [Phase 08.2]: Backend IT verification runs via integrationTest on Java 25 — The repository asserts Java 25 at configuration time and the test task excludes *IT suites, so integrationTest is the reliable brownfield gate.
- [Phase 08.2]: Role permission matrix app selectors use the menu-definition catalog plus existing grants — This lets admins create the first menu grant in a new app without duplicating role records or relying on pre-seeded grant rows.

### Pending Todos

- Plan Phase 9 - Enterprise UX And Performance Hardening.

### Blockers/Concerns

- No open milestone blockers.
- Frontend production build still exceeds the configured initial bundle budget warning threshold.
- Planning debt remains on validation metadata for phases 1, 3, and 4; product verification and the milestone audit both passed.

## Session Continuity

Last activity: 2026-03-27
Last session: 2026-03-27T09:58:06.340Z
Stopped at: Completed 08.2-04-PLAN.md
Resume file: None
