---
phase: 08-user-management-delivery
plan: 03
subsystem: frontend/user-management
tags: [angular, primeng, user-management, create-edit, authority-management, playwright]
dependency_graph:
  requires: [08-02]
  provides: [user-management-update, user-management-form-service, authority-save-smoke]
  affects: [user-management.routes, user-management.update, user-management.e2e, i18n]
tech_stack:
  added: [p-select, p-message, p-toast]
  patterns: [shared-create-edit-surface, form-service-mapping, authority-table-editing, mocked-access-smoke]
key_files:
  created:
    - frontend/src/app/pages/admin/user-management/update/user-management-form.service.ts
    - frontend/src/app/pages/admin/user-management/update/user-management-update.component.ts
    - frontend/src/app/pages/admin/user-management/update/user-management-update.component.html
    - frontend/src/app/pages/admin/user-management/update/user-management-update.component.spec.ts
    - frontend/e2e/user-management.spec.ts
  modified:
    - frontend/src/app/pages/admin/user-management/user-management.routes.ts
    - frontend/src/app/pages/admin/user-management/user-management.routes.spec.ts
    - frontend/src/i18n/en/user-management.json
    - frontend/src/i18n/vi/user-management.json
decisions:
  - "Create and edit reuse one split-page component so the detail and edit surfaces stay structurally aligned"
  - "UserManagementFormService owns trim/default mapping so create and edit flows serialize one consistent IUser payload"
  - "The first authority-save proof uses deterministic Playwright API mocks to verify a persisted grant changes downstream admin route access"
metrics:
  duration_seconds: 607
  completed: 2026-03-25
---

# Phase 8 Plan 3: Editable User Management Surface Summary

Create and edit now run through one real split-page form with inline authority assignment, preserved admin-user contracts, and a focused Playwright smoke that proves a saved grant changes app access for the affected session.

## Tasks Completed

| # | Task | Commit | Key Files |
|---|------|--------|-----------|
| 1 | Build the form service and shared create or edit component | 5e63acb | update/user-management-form.service.ts, update/user-management-update.component.ts, update/user-management-update.component.html, update/user-management-update.component.spec.ts, i18n/*.json |
| 2 | Wire new or edit routes and add the first authority-save smoke | 2534efe | user-management.routes.ts, user-management.routes.spec.ts, frontend/e2e/user-management.spec.ts |

## What Was Built

### Shared Create and Edit Surface
- `UserManagementFormService` now owns form creation, reset behavior, default values, and IUser payload extraction including `authorities`
- `/admin/users/new` and `/admin/users/:login/edit` both render the real split-page workspace instead of a placeholder
- The left card handles login, names, email, language, and activation with inline validation matching `AdminUserDTO`
- The right card keeps the authority matrix visible as a dense PrimeNG checkbox table in both create and edit flows
- Save routes to `POST /api/admin/users` for create and `PUT /api/admin/users` for edit without adding a separate role endpoint

### Navigation and Success Flow
- Create and edit resolve from the existing route tree and keep the existing route metadata contract
- Successful save returns to `/admin/users/:login/view`
- Cancel returns to the stored list context for create and back to the user detail route for edit
- Save failures keep the form visible and route unexpected errors through the shared HTTP error handler

### Initial Authority Persistence Proof
- Added the first focused Playwright user-management smoke under `frontend/e2e/user-management.spec.ts`
- The smoke drives the real `/admin/users` list, detail, and edit routes
- The mocked persistence layer records the authority change, then the affected user session is exercised to confirm `/admin/users` becomes reachable after the saved grant

## Test Coverage

- `npm --prefix frontend exec ng test -- --watch=false --include src/app/pages/admin/user-management/update/user-management-update.component.spec.ts`
- `npm --prefix frontend run build`
- `npm --prefix frontend exec playwright test e2e/user-management.spec.ts`

All checks passed on 2026-03-25.

## Deviations from Plan

None - plan executed exactly as written.

## Known Follow-Up

- Phase 08-04 extends the initial grant smoke into the final grant-and-revoke access proof and adds direct route-guard regression coverage.

## Self-Check: PASSED
