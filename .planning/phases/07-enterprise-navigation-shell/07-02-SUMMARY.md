---
phase: 07-enterprise-navigation-shell
plan: 02
subsystem: ui
tags: [angular, navigation, shell, permissions, vitest]
requires:
  - phase: 06-frontend-parity-foundation
    provides: stable shell ids, translated shell chrome, and standalone route boundaries
provides:
  - frontend-owned navigation metadata for Home, Entities, and Security leaves
  - current-user navigation grant caching with sessionStorage warm start and auth-reset invalidation
  - unit coverage for query params, visibility filtering, and fallback route selection
affects: [07-03 route guards, breadcrumbs, menu integration]
tech-stack:
  added: []
  patterns: [frontend-owned registry filtering, in-memory plus sessionStorage current-user cache]
key-files:
  created:
    - frontend/src/app/layout/navigation/navigation.model.ts
    - frontend/src/app/layout/navigation/navigation.constants.ts
    - frontend/src/app/layout/navigation/navigation-registry.ts
    - frontend/src/app/layout/navigation/navigation.service.ts
    - frontend/src/app/layout/navigation/navigation.service.spec.ts
  modified:
    - .planning/phases/07-enterprise-navigation-shell/07-02-SUMMARY.md
key-decisions:
  - "Entity leaves use `in-shell` deniedMode while security leaves use `route` to preserve the Phase 7 denial split."
  - "Allowed node ids are normalized to known frontend registry leaves in registry order before filtering or fallback resolution."
patterns-established:
  - "Registry-first navigation: the frontend owns ids, labels, icons, ordering, and denied modes while the backend returns allowed leaf ids only."
  - "Navigation grants reuse the capability-service cache pattern with sessionStorage warm start and login-based invalidation."
requirements-completed: [ROUTE-01]
duration: 13min
completed: 2026-03-25
---

# Phase 07 Plan 02: Navigation Registry Summary

**Frontend-owned shell registry and current-user navigation grant caching for leaf-id-driven menu visibility**

## Performance

- **Duration:** 13 min
- **Started:** 2026-03-25T05:56:30Z
- **Completed:** 2026-03-25T06:09:47Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Added a typed navigation model plus shared constants for the shell app name and grant storage key.
- Defined the canonical Home, Entities, and Security navigation tree with stable leaf ids, icons, route prefixes, and denied-mode metadata.
- Implemented a shared navigation grant service with current-user HTTP lookup, memory plus sessionStorage caching, auth-state invalidation, and focused Vitest coverage.

## Task Commits

Each task was committed atomically:

1. **Task 1: Define the canonical frontend navigation registry** - `1565c50` (feat)
2. **Task 2: Implement the current-user navigation service and tests** - `6a7a0c0` (feat)

## Files Created/Modified

- `frontend/src/app/layout/navigation/navigation.model.ts` - Typed section and leaf contracts shared by the shell registry and service.
- `frontend/src/app/layout/navigation/navigation.constants.ts` - Shared app name and sessionStorage keys for navigation grants.
- `frontend/src/app/layout/navigation/navigation-registry.ts` - Canonical frontend-owned shell tree and ordered leaf inventory.
- `frontend/src/app/layout/navigation/navigation.service.ts` - Current-user grant lookup, cache management, visibility filtering, and fallback route selection.
- `frontend/src/app/layout/navigation/navigation.service.spec.ts` - Unit coverage for backend query params, cache reuse, storage warm start, auth-state reset, filtering, and fallback behavior.
- `.planning/phases/07-enterprise-navigation-shell/07-02-SUMMARY.md` - Execution summary for this plan.

## Decisions Made

- Used the registry as the single source of truth for leaf ordering so fallback routing stays frontend-controlled even when the backend response order varies.
- Stored a login sentinel alongside the cached payload so sessionStorage warm starts remain tied to the authenticated user and are cleared on login changes.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- PowerShell rejected `&&` during the first commit attempt, so the task commits were rerun with PowerShell-safe command sequencing.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Route guards, breadcrumb work, and menu rendering can now consume the shared navigation registry and grant service instead of duplicating shell metadata.
- No blockers found in the owned frontend scope; the scoped navigation service test command passed.

---

*Phase: 07-enterprise-navigation-shell*
*Completed: 2026-03-25*
