---
phase: 05-standalone-frontend-delivery
plan: 08
subsystem: frontend-entities
tags: [angular, permissions, capabilities, entities, ui]
dependency_graph:
  requires: [05-03, 05-07]
  provides: [entity-list-action-gating, entity-detail-action-gating]
  affects: [05-09]
tech_stack:
  added: []
  patterns:
    - capability-driven action rendering
    - hide-until-capability-loaded
    - shared entity capability consumption
key_files:
  created:
    - frontend/src/app/pages/entities/organization/list/organization-list.component.spec.ts
    - frontend/src/app/pages/entities/organization/detail/organization-detail.component.spec.ts
  modified:
    - frontend/src/app/pages/entities/organization/list/organization-list.component.ts
    - frontend/src/app/pages/entities/organization/list/organization-list.component.html
    - frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts
    - frontend/src/app/pages/entities/organization/detail/organization-detail.component.html
    - frontend/src/app/pages/entities/department/list/department-list.component.ts
    - frontend/src/app/pages/entities/department/list/department-list.component.html
    - frontend/src/app/pages/entities/department/detail/department-detail.component.ts
    - frontend/src/app/pages/entities/department/detail/department-detail.component.html
    - frontend/src/app/pages/entities/employee/list/employee-list.component.ts
    - frontend/src/app/pages/entities/employee/list/employee-list.component.html
    - frontend/src/app/pages/entities/employee/detail/employee-detail.component.ts
    - frontend/src/app/pages/entities/employee/detail/employee-detail.component.html
decisions:
  - "Protected-entity actions default to hidden until the shared capability response arrives, preventing transient overexposure during page bootstrap."
  - "Detail-screen Edit buttons use the same shared capability source as list actions instead of duplicating ad hoc permission logic per feature."
requirements-completed: [ENT-03]
metrics:
  duration: parallel
  completed: 2026-03-22
  tasks_completed: 2
  files_created: 2
---

# Phase 5 Plan 08: Entity Action Gating Summary

Applied the shared capability contract to protected-entity list and detail screens so Organization, Department, and Employee actions now reflect the current user's actual entity permissions instead of always rendering optimistically.

## What Was Built

### List Action Gating

- Wired Organization, Department, and Employee list components to `SecuredEntityCapabilityService`.
- Hid `New`, `View`, `Edit`, and `Delete` controls until capability data loads.
- Added organization list regression coverage proving only `View` remains when create/update/delete permissions are denied.

### Detail Edit Gating

- Wired all three detail components to the shared capability service.
- Hid the `Edit` button until capability data loads and rendered it only when `canUpdate` is true.
- Added organization detail regression coverage for both denied and allowed update capability states.

## Task Commits

1. **Task 1: Gate protected entity list actions** - `29648b3` (`feat(05-08)`)
2. **Task 2: Gate protected entity detail actions** - `42360ac` (`feat(05-08)`)

## Verification

- `cmd /c npx.cmd ng test --watch=false`

## Self-Check: PASSED

List and detail actions now stay hidden until capability data is known, and the visible controls across organization, department, and employee screens match the current user's entity-level permissions.
