---
phase: 09-enterprise-ux-and-performance-hardening
plan: "02"
subsystem: ui
tags: [angular, primeng, signals, skeleton-loader, responsive, onpush]

requires:
  - phase: 08.3-user-registration-live-permission-refresh-entity-native-serialization-validation-hardening-and-row-policy-removal
    provides: secured entity list components with permission-gated column display and capability-driven state

provides:
  - Signal-based pagination state (totalItems, page, firstRow) in Department, Employee, Organization list components
  - OnPush change detection in all three entity list components
  - Skeleton loader rows during initial data fetch via PrimeNG Skeleton
  - Responsive column hiding at < 1024px viewport width via fromEvent(window, resize)
  - Responsive action button layout (stacked on tablet, inline on desktop)

affects:
  - frontend entity list UX and rendering performance
  - future plan 10 regression coverage (list components now OnPush)

tech-stack:
  added: []
  patterns:
    - "Signal pagination: totalItems = signal(0), page = signal(1), firstRow = computed(() => (page() - 1) * itemsPerPage)"
    - "Skeleton loader: tableValue computed signal switches between skeletonRows and real data on initial load"
    - "Responsive detection: isTablet signal + fromEvent(window, resize) + debounceTime(150) + takeUntilDestroyed"
    - "OnPush across all entity list components with CommonModule removed in favor of DecimalPipe"

key-files:
  created: []
  modified:
    - frontend/src/app/pages/entities/department/list/department-list.component.ts
    - frontend/src/app/pages/entities/department/list/department-list.component.html
    - frontend/src/app/pages/entities/department/list/department-list.component.spec.ts
    - frontend/src/app/pages/entities/employee/list/employee-list.component.ts
    - frontend/src/app/pages/entities/employee/list/employee-list.component.html
    - frontend/src/app/pages/entities/organization/list/organization-list.component.ts
    - frontend/src/app/pages/entities/organization/list/organization-list.component.html
    - frontend/src/app/pages/entities/organization/list/organization-list.component.spec.ts

key-decisions:
  - "Used fromEvent(window, resize) with debounceTime(150) and takeUntilDestroyed for responsive detection (no @angular/cdk)"
  - "Skeleton loaders use tableValue computed signal to switch between 5 skeleton rows and real data on initial load only"
  - "PrimeNG loading overlay kept for subsequent page loads (not initial); skeleton only fires when entity array is empty"
  - "Spec files set window.innerWidth = 1280 in beforeAll to simulate desktop in jsdom test environment"

patterns-established:
  - "Pagination signals pattern: convert totalItems/page to WritableSignal, firstRow to computed()"
  - "Skeleton loader pattern: skeletonRows = Array(5).fill(null), tableValue computed, skeleton branch in ng-template body"
  - "Responsive column pattern: isTablet signal + showXxxColumn computed merging tablet + canViewField checks"

requirements-completed: [UI-05, PERF-03]

duration: 22min
completed: 2026-03-28
---

# Phase 9 Plan 02: Frontend Entity List Hardening Summary

**Migrated Department, Employee, and Organization list components to signal-based pagination with OnPush change detection, added skeleton loaders for initial data fetch, and added responsive column hiding at tablet widths.**

## Performance

- **Duration:** 22 min
- **Started:** 2026-03-28T07:20:00Z
- **Completed:** 2026-03-28T07:42:00Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- All three entity list components (Department, Employee, Organization) now use `WritableSignal<number>` for `totalItems` and `page`, and `computed()` for `firstRow`, replacing plain mutable fields and a getter
- `ChangeDetectionStrategy.OnPush` added to all three list components, removing `CommonModule` in favor of specific `DecimalPipe`
- Skeleton loaders show 5 placeholder rows during initial data fetch using PrimeNG Skeleton; subsequent page loads continue to show the PrimeNG loading overlay
- Responsive column hiding hides low-priority columns (id, organization/department/ownerLogin, budget/salary/costCenter) below 1024px viewport width using window resize events
- Action buttons stack vertically at tablet width for better usability
- Build succeeds with no TypeScript errors; 29/33 test files pass (4 pre-existing failures unrelated to this plan)

## Task Commits

Both tasks were committed atomically:

1. **Task 1+2: Migrate pagination to signals, add OnPush, skeleton loaders, responsive columns** - `fb0a4bd` (feat)

## Files Created/Modified

- `frontend/src/app/pages/entities/department/list/department-list.component.ts` - Signal pagination, OnPush, skeleton, responsive
- `frontend/src/app/pages/entities/department/list/department-list.component.html` - Skeleton rows, responsive column @if guards, responsive action buttons
- `frontend/src/app/pages/entities/department/list/department-list.component.spec.ts` - Added beforeAll to set window.innerWidth = 1280 for desktop test
- `frontend/src/app/pages/entities/employee/list/employee-list.component.ts` - Signal pagination, OnPush, skeleton, responsive
- `frontend/src/app/pages/entities/employee/list/employee-list.component.html` - Skeleton rows, responsive column @if guards, responsive action buttons
- `frontend/src/app/pages/entities/organization/list/organization-list.component.ts` - Signal pagination, OnPush, skeleton, responsive (showIdColumn + showOwnerLoginColumn added)
- `frontend/src/app/pages/entities/organization/list/organization-list.component.html` - Skeleton rows, responsive column @if guards, responsive action buttons
- `frontend/src/app/pages/entities/organization/list/organization-list.component.spec.ts` - Added beforeAll to set window.innerWidth = 1280 for desktop test

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing] Added window.innerWidth mock in spec files**
- **Found during:** Task 2
- **Issue:** jsdom test environment defaults `window.innerWidth` to 0, causing `isTablet()` to return `true` and hiding organization/department columns even when `canView: true` was set — would break existing spec assertions
- **Fix:** Added `beforeAll` to set `window.innerWidth = 1280` in department-list and organization-list spec files, restoring desktop column visibility in test environment
- **Files modified:** `department-list.component.spec.ts`, `organization-list.component.spec.ts`
- **Commit:** fb0a4bd

## Known Stubs

None — all pagination signals are wired to real backend data, skeleton loaders fire on real loading state, and responsive signals use actual window measurements.

## Self-Check: PASSED

Files verified:
- `frontend/src/app/pages/entities/department/list/department-list.component.ts` - FOUND
- `frontend/src/app/pages/entities/employee/list/employee-list.component.ts` - FOUND
- `frontend/src/app/pages/entities/organization/list/organization-list.component.ts` - FOUND

Commits verified:
- `fb0a4bd` - FOUND
