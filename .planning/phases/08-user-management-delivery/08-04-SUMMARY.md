---
phase: 08-user-management-delivery
plan: 04
subsystem: frontend/user-management
tags: [angular, route-guard, playwright, user-management, access-control, regression]
dependency_graph:
  requires: [08-03]
  provides: [user-management-route-guard-regression, user-management-grant-revoke-proof]
  affects: [user-route-access.service.spec.ts, user-management.e2e]
tech_stack:
  added: []
  patterns: [authority-driven-route-regression, grant-and-revoke-access-proof, focused-phase-gate]
key_files:
  created: []
  modified:
    - frontend/src/app/core/auth/user-route-access.service.spec.ts
    - frontend/e2e/user-management.spec.ts
decisions:
  - "User-route-access coverage should assert /admin/users directly against the effective authority set, not just a generic hidden-leaf case"
  - "The user-management smoke proves both grant-then-allow and revoke-then-deny against the same mocked admin flow"
  - "The final focused gate for Phase 8 remains the user-management list, detail, update, and route-access specs plus the dedicated Playwright smoke"
metrics:
  duration_seconds: 300
  completed: 2026-03-25
---

# Phase 8 Plan 4: Authority Access Proof Summary

Phase 8 now proves saved authority edits change real admin-route outcomes for the affected user through both guard-level regression coverage and a grant-and-revoke Playwright smoke.

## Tasks Completed

| # | Task | Commit | Key Files |
|---|------|--------|-----------|
| 1 | Add guard-level regression coverage for authority-driven admin access | f4fd3a2 | frontend/src/app/core/auth/user-route-access.service.spec.ts |
| 2 | Extend the Playwright smoke to prove grant-then-allow and revoke-then-deny | 06d41e2 | frontend/e2e/user-management.spec.ts |

## What Was Built

### Route Guard Regression Coverage
- `UserRouteAccessService` now has direct tests for `/admin/users` under an effective admin authority set
- The spec proves the route is allowed when the authenticated account includes `ROLE_ADMIN`
- The spec also proves the same route is denied with blocked-route metadata once that authority is removed

### End-to-End Access Outcome Proof
- The focused Playwright suite still drives the real `/admin/users`, detail, and edit routes
- The grant scenario persists `ROLE_ADMIN`, switches to the affected user session, and proves `/admin/users` becomes reachable
- The revoke scenario removes `ROLE_ADMIN`, switches sessions, and proves the same route redirects to `/accessdenied`
- The smoke remains deterministic through request mocking while still asserting a real application access outcome after save

## Test Coverage

- `npm --prefix frontend exec ng test -- --watch=false --include src/app/pages/admin/user-management/list/user-management-list.component.spec.ts --include src/app/pages/admin/user-management/detail/user-management-detail.component.spec.ts --include src/app/pages/admin/user-management/update/user-management-update.component.spec.ts --include src/app/core/auth/user-route-access.service.spec.ts`
- `npm --prefix frontend run build`
- `npm --prefix frontend exec playwright test e2e/user-management.spec.ts`

All checks passed on 2026-03-25.

## Deviations from Plan

None - plan executed exactly as written.

## Requirement Closure

This plan closes the last UMGT-03 proof gap by showing that a saved authority change affects downstream access for the modified user, not only the saved payload.

## Self-Check: PASSED
