---
phase: 05-standalone-frontend-delivery
plan: 09
subsystem: frontend-entities
tags: [angular, permissions, capabilities, forms, routing]
dependency_graph:
  requires: [05-03, 05-07]
  provides: [entity-update-route-gating, sensitive-field-edit-gating]
  affects: []
tech_stack:
  added: []
  patterns:
    - capability-gated form bootstrap
    - access-denied redirect before form exposure
    - sensitive field payload omission
key_files:
  created:
    - frontend/src/app/pages/entities/organization/update/organization-update.component.spec.ts
    - frontend/src/app/pages/entities/employee/update/employee-update.component.spec.ts
  modified:
    - frontend/src/app/pages/entities/organization/update/organization-update.component.ts
    - frontend/src/app/pages/entities/organization/update/organization-update.component.html
    - frontend/src/app/pages/entities/department/update/department-update.component.ts
    - frontend/src/app/pages/entities/department/update/department-update.component.html
    - frontend/src/app/pages/entities/employee/update/employee-update.component.ts
    - frontend/src/app/pages/entities/employee/update/employee-update.component.html
decisions:
  - "Create and edit screens now wait for capability resolution before exposing the form, then redirect directly to /accessdenied when entity-level create/update permission is denied."
  - "Budget and salary are only rendered and persisted when the capability response grants explicit attribute edit permission."
requirements-completed: [ENT-03]
metrics:
  duration: parallel
  completed: 2026-03-22
  tasks_completed: 1
  files_created: 2
---

# Phase 5 Plan 09: Update Form Capability Gating Summary

Completed the protected-entity update-flow hardening so create/edit routes respect entity capability upfront and sensitive form inputs stay hidden until attribute edit permission is explicitly allowed.

## What Was Built

- Wired Organization, Department, and Employee update components to `SecuredEntityCapabilityService`.
- Redirected denied create/edit routes to `/accessdenied` before the form is exposed.
- Removed the forced new-mode `showBudgetField` and `showSalaryField` behavior; budget and salary now render only when capability data grants edit permission.
- Added regression coverage for budget/salary visibility and an access-denied redirect path in the employee update flow.

## Task Commits

1. **Task 1 RED: Add failing capability gating specs** - `d921f6a` (`test(05-09)`)
2. **Task 1 GREEN: Gate entity update screens by capability** - `eca7dcf` (`feat(05-09)`)

## Verification

- `cmd /c npx.cmd ng test --watch=false`

## Self-Check: PASSED

Denied create/edit routes no longer expose the update form, and budget/salary inputs stay hidden unless the capability response explicitly allows editing them.
