---
phase: 05-standalone-frontend-delivery
plan: 05
subsystem: frontend-security-admin
tags: [angular, primeng, permissions, matrix, security-admin]
dependency_graph:
  requires: [05-01, 05-04]
  provides: [permission-matrix-ui]
  affects: []
tech_stack:
  added: []
  patterns:
    - standalone-component
    - forkJoin bootstrap loading
    - optimistic permission toggles
    - pending-save guard
    - two-panel splitter layout
key_files:
  created:
    - frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.html
  modified:
    - frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts
decisions:
  - "Permission grant state is keyed as target:action in a Map so entity and attribute toggles share the same storage model."
  - "New grants are written optimistically with a sentinel id of -1 until the POST response returns the real permission id."
  - "A per-key pending Set blocks rapid double toggles and also disables wildcard-covered attribute rows while saves are in flight."
  - "The attribute matrix does not auto-select an entity on load; admins explicitly choose the entity row they want to edit."
metrics:
  duration: inline
  completed: 2026-03-22
  tasks_completed: 1
  files_created: 2
---

# Phase 5 Plan 05: Permission Matrix Summary

Replaced the placeholder permission-matrix screen with a working Jmix-style editor that loads catalog entries and role permissions, renders entity and attribute checkboxes, and saves each toggle immediately.

## What Was Built

- Added a two-panel `p-splitter` layout with entity CRUD controls in the top panel and attribute View/Modify controls in the bottom panel.
- Wired the component to `SecCatalogService` and `SecPermissionService` with a `forkJoin` startup load keyed by the role name route parameter.
- Implemented immediate-save checkbox behavior for entity and attribute permissions using optimistic local updates.
- Added wildcard `All attributes (*)` handling so wildcard grants disable per-attribute checkboxes for the same action.
- Added save-error recovery that reverts local permission state and surfaces a toast instead of leaving stale UI state behind.

## Verification

- `cmd /c npx.cmd ng build --configuration=development`
- `cmd /c npx.cmd ng test --watch=false`

## Self-Check: PASSED

The permission matrix compiles, the frontend test suite passes, route-param loading is in place, wildcard locking is enforced, and save failures revert the optimistic checkbox state correctly.
