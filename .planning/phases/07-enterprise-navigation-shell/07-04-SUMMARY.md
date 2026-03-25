---
phase: 07-enterprise-navigation-shell
plan: 04
subsystem: ui
tags: [angular, shell, breadcrumbs, navigation, workspace, vitest]
requires:
  - phase: 07-enterprise-navigation-shell
    provides: shell route metadata, navigation grants, and filtered menu visibility
provides:
  - breadcrumb chrome driven by route metadata and navigation registry
  - blocked-destination recovery inside the authenticated shell
  - preserved entity list context across list, detail, and edit flows
  - in-shell denied states for granted navigation leaves without entity READ
affects: [07-05 validation, shell e2e coverage]
tech-stack:
  added: []
  patterns: [route-metadata breadcrumbs, workspace context restoration, in-shell denied cards]
key-files:
  created:
    - frontend/src/app/layout/navigation/breadcrumb.service.ts
    - frontend/src/app/layout/navigation/breadcrumb.service.spec.ts
    - frontend/src/app/layout/component/main/app.layout.spec.ts
    - frontend/src/app/pages/error/access-denied.component.spec.ts
    - frontend/src/app/pages/entities/shared/service/workspace-context.service.ts
  modified:
    - frontend/src/app/layout/component/main/app.layout.ts
    - frontend/src/app/pages/error/access-denied.component.ts
    - frontend/src/app/pages/error/access-denied.component.html
    - frontend/src/app/pages/entities/organization/list/organization-list.component.ts
    - frontend/src/app/pages/entities/organization/list/organization-list.component.html
    - frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts
    - frontend/src/app/pages/entities/organization/detail/organization-detail.component.html
    - frontend/src/app/pages/entities/organization/update/organization-update.component.ts
    - frontend/src/app/pages/entities/organization/list/organization-list.component.spec.ts
    - frontend/src/app/pages/entities/organization/detail/organization-detail.component.spec.ts
    - frontend/src/app/pages/entities/organization/update/organization-update.component.spec.ts
    - frontend/src/app/pages/entities/department/list/department-list.component.ts
    - frontend/src/app/pages/entities/department/list/department-list.component.html
    - frontend/src/app/pages/entities/department/detail/department-detail.component.ts
    - frontend/src/app/pages/entities/department/update/department-update.component.ts
    - frontend/src/app/pages/entities/employee/list/employee-list.component.ts
    - frontend/src/app/pages/entities/employee/list/employee-list.component.html
    - frontend/src/app/pages/entities/employee/detail/employee-detail.component.ts
    - frontend/src/app/pages/entities/employee/update/employee-update.component.ts
    - .planning/phases/07-enterprise-navigation-shell/07-04-SUMMARY.md
key-decisions:
  - "Breadcrumbs resolve from `navigationNodeId` and route `pageTitleKey`, keeping shell chrome tied to the same metadata contract as route visibility."
  - "Access-denied stays inside the authenticated shell and derives its recovery CTA from `NavigationService.resolveFallbackRoute(sectionId)`."
  - "Entity workspaces preserve list query params by navigation leaf id so back, cancel, and successful saves return users to their prior list context."
patterns-established:
  - "Granted shell access without entity READ renders an in-shell denied card instead of firing the list query."
  - "Detail and edit routes restore preserved list state instead of dropping users onto a reset list surface."
requirements-completed: [ROUTE-02, UI-04]
duration: 16min
completed: 2026-03-25
---

# Phase 07 Plan 04: Enterprise Shell Summary

**Breadcrumb shell chrome, blocked-route recovery, and workspace context preservation for entity flows**

## Performance

- **Duration:** 16 min
- **Started:** 2026-03-25T06:23:00Z
- **Completed:** 2026-03-25T06:39:00Z
- **Tasks:** 2
- **Files modified:** 20

## Accomplishments

- Added a shell breadcrumb service and breadcrumb strip so authenticated deep routes derive their trail from route metadata instead of URL parsing.
- Reworked `/accessdenied` to show the blocked destination and a safe fallback CTA while staying inside the authenticated shell.
- Added workspace context preservation for organization, department, and employee flows so list state survives drill-ins and update flows.
- Rendered in-shell denied cards for entity list routes that have a visible shell leaf but no entity `READ`, and covered the new shell behavior with focused Angular specs.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add breadcrumb shell chrome and route-aware access-denied recovery** - `68f78e3` (feat)
2. **Task 2: Preserve list context and render in-shell denied states for entity lists** - `e4ecee0` (feat)

## Files Created/Modified

- `frontend/src/app/layout/navigation/breadcrumb.service.ts` - Builds shell breadcrumbs from the activated route snapshot and navigation registry metadata.
- `frontend/src/app/layout/navigation/breadcrumb.service.spec.ts` - Covers list, detail, edit, and permission-matrix breadcrumb trails.
- `frontend/src/app/layout/component/main/app.layout.ts` - Renders the breadcrumb strip above the alert area and router outlet.
- `frontend/src/app/layout/component/main/app.layout.spec.ts` - Verifies the breadcrumb strip only renders when breadcrumb items exist.
- `frontend/src/app/pages/error/access-denied.component.ts` - Reads blocked-route metadata and resolves a safe fallback route and label.
- `frontend/src/app/pages/error/access-denied.component.html` - Shows blocked destination copy and the recovery CTA inside the shell surface.
- `frontend/src/app/pages/error/access-denied.component.spec.ts` - Covers current-navigation and history-state recovery behavior.
- `frontend/src/app/pages/entities/shared/service/workspace-context.service.ts` - Stores normalized list query params by `navigationNodeId`.
- `frontend/src/app/pages/entities/organization/list/organization-list.component.ts` - Skips the list query when `canRead` is false and stores workspace context before navigation.
- `frontend/src/app/pages/entities/organization/list/organization-list.component.html` - Renders an in-shell denied card and keeps create actions gated behind readable workspaces.
- `frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts` - Restores preserved list context on back navigation.
- `frontend/src/app/pages/entities/organization/update/organization-update.component.ts` - Restores preserved list context on cancel and save success while keeping `/accessdenied` for denied create or update.
- `frontend/src/app/pages/entities/department/*` and `frontend/src/app/pages/entities/employee/*` - Mirror the same list denial and workspace-context restore behavior for the other entity workspaces.

## Decisions Made

- Used route metadata, not ad hoc path parsing, as the source of truth for breadcrumb labels and current-page state.
- Kept entity denied handling inside each list workspace so denied routes still preserve shell context, titles, and menu state.
- Preserved list query params rather than inventing a heavier session object, which keeps the restore behavior aligned with router-driven state.

## Deviations from Plan

None - plan executed as written.

## Issues Encountered

- Targeted Angular specs initially surfaced two mock typing issues in the new tests; tightening the mock return types resolved them without changing runtime behavior.

## User Setup Required

None - no additional environment or config changes are required for this plan.

## Verification

- `npm --prefix frontend exec ng test -- --watch=false --include src/app/layout/navigation/breadcrumb.service.spec.ts --include src/app/layout/component/main/app.layout.spec.ts --include src/app/pages/error/access-denied.component.spec.ts --include src/app/pages/entities/organization/list/organization-list.component.spec.ts --include src/app/pages/entities/organization/detail/organization-detail.component.spec.ts --include src/app/pages/entities/organization/update/organization-update.component.spec.ts`

## Next Phase Readiness

- Final validation can now prove the shell behavior end to end, including breadcrumbs, hidden-leaf denial, and in-shell denied list states.
- The remaining phase work is the 07-05 validation sweep: frontend and backend test coverage completion, shell build verification, and Playwright proof.

---
*Phase: 07-enterprise-navigation-shell*
*Completed: 2026-03-25*
