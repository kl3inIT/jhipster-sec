---
phase: 07-enterprise-navigation-shell
plan: 03
subsystem: ui
tags: [angular, routing, navigation, auth, vitest]
requires:
  - phase: 07-enterprise-navigation-shell
    provides: backend navigation grants and the shared frontend navigation service
provides:
  - navigation-node metadata on shell routes without collapsing lazy boundaries
  - navigation-aware route guarding with blocked-destination state for denied routes
  - menu rendering from the filtered navigation registry instead of inline admin checks
affects: [07-04 breadcrumbs, access-denied recovery, workspace context]
tech-stack:
  added: []
  patterns: [route metadata as shell contract, shared navigation visibility for menu and guard]
key-files:
  created:
    - frontend/src/app/core/auth/user-route-access.service.spec.ts
  modified:
    - frontend/src/app.routes.ts
    - frontend/src/app.routes.spec.ts
    - frontend/src/app/core/auth/user-route-access.service.ts
    - frontend/src/app/layout/component/menu/app.menu.ts
    - frontend/src/app/layout/component/menu/app.menu.spec.ts
    - frontend/src/app/pages/admin/security/security.routes.ts
    - frontend/src/app/pages/admin/user-management/user-management.routes.ts
    - frontend/src/app/pages/entities/organization/organization.routes.ts
    - frontend/src/app/pages/entities/department/department.routes.ts
    - frontend/src/app/pages/entities/employee/employee.routes.ts
    - .planning/phases/07-enterprise-navigation-shell/07-03-SUMMARY.md
key-decisions:
  - "Shell routes now carry `navigationNodeId`, `sectionId`, `breadcrumbKey`, `pageTitleKey`, and `deniedMode` so menu visibility and route denial come from the same metadata."
  - "Menu active state uses stable `path` ids derived from the navigation registry instead of translated labels or inline role checks."
patterns-established:
  - "Leaf routes, not top-level lazy boundaries, own the shell authorization metadata."
  - "Denied deep links preserve `blockedUrl`, `blockedLabelKey`, and `sectionId` in router state for later recovery UI."
requirements-completed: [ROUTE-02, ROUTE-03]
duration: 10min
completed: 2026-03-25
---

# Phase 07 Plan 03: Route Visibility Summary

**Navigation-metadata-driven shell routes, menu filtering, and denied-route guard behavior without breaking lazy boundaries**

## Performance

- **Duration:** 10 min
- **Started:** 2026-03-25T06:13:00Z
- **Completed:** 2026-03-25T06:22:54Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments

- Added stable navigation metadata to home, admin, user-management, security, and entity routes while keeping the existing `admin` and `entities` lazy boundaries intact.
- Reworked `UserRouteAccessService` to use `NavigationService.isNodeVisible(...)`, preserve blocked-route metadata, and keep public shell error routes reachable.
- Rebuilt the sidebar menu from `NavigationService.visibleTree()` so hidden leaves disappear from the shell without relying on inline `ROLE_ADMIN` checks.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add route metadata for navigation nodes and preserve lazy boundaries** - `6537918` (feat)
2. **Task 2: Make the menu and route guard consume the navigation service** - `5a65423` (feat)

## Files Created/Modified

- `frontend/src/app.routes.ts` - Home route now exposes shell metadata and the top-level admin boundary no longer hardcodes `ROLE_ADMIN`.
- `frontend/src/app.routes.spec.ts` - Asserts lazy boundaries stay in place and shell leaf routes expose `navigationNodeId` metadata.
- `frontend/src/app/pages/admin/security/security.routes.ts` - Security admin leaves now carry stable navigation metadata and route guarding.
- `frontend/src/app/pages/admin/user-management/user-management.routes.ts` - User-management placeholder routes now expose `security.users` metadata and route titles as translation keys.
- `frontend/src/app/pages/entities/organization/organization.routes.ts` - Organization routes now inherit the entity shell node with list `in-shell` denial semantics.
- `frontend/src/app/pages/entities/department/department.routes.ts` - Department routes follow the same metadata contract as organization routes.
- `frontend/src/app/pages/entities/employee/employee.routes.ts` - Employee routes now expose stable entity leaf metadata without changing the lazy route shape.
- `frontend/src/app/core/auth/user-route-access.service.ts` - Guard now redirects hidden authenticated routes to `/accessdenied` with blocked-destination state.
- `frontend/src/app/core/auth/user-route-access.service.spec.ts` - Covers unauthenticated login redirect, hidden-leaf denial, allowed-leaf pass-through, and public shell route access.
- `frontend/src/app/layout/component/menu/app.menu.ts` - Menu model now comes from the filtered navigation registry and stable path ids.
- `frontend/src/app/layout/component/menu/app.menu.spec.ts` - Covers hidden-leaf filtering and label refresh without stable-id drift.

## Decisions Made

- Kept the top-level `admin` route lazy and metadata-light so route reachability lives on actual shell leaves instead of a coarse parent `ROLE_ADMIN` gate.
- Used route `breadcrumbKey` as the blocked-destination label source so later access-denied recovery UI can reuse the same translation contract as the menu.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Breadcrumbs and access-denied recovery can now consume blocked-route state and consistent per-route metadata.
- Entity list, detail, and edit flows now share stable shell ids, which is the prerequisite for preserved workspace context in the next plan.

---
*Phase: 07-enterprise-navigation-shell*
*Completed: 2026-03-25*
