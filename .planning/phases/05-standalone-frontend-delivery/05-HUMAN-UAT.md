---
status: diagnosed
phase: 05-standalone-frontend-delivery
source: [05-VERIFICATION.md]
started: 2026-03-23T12:00:00Z
updated: 2026-03-23T14:30:00Z
---

## Current Test

[testing complete]

## Tests

### 1. JWT login round trip and token persistence
expected: Valid credentials reach the dashboard, refresh keeps the session, and an expired session redirects back to /login.
result: pass

### 2. Admin security-management flow in a browser
expected: An admin can create or update roles and row policies, change matrix permissions by ticking checkboxes, click an entity row to reveal its attribute permission panel, reload the screens, and the saved changes persist.
result: issue
reported: "Create a 'Save' button. When the user clicks it, show a confirmation dialog asking 'Do you want to save?'. If the user agrees, proceed with saving the data."
severity: minor

### 3. Protected-entity gating with deny-default + canViewField fixes (UAT Test 3 retest)
expected: Proof-reader cannot see Code field on organization detail; proof-reader has no Edit button on detail; logout completes without lag; action columns show a spinner before capability resolves; proof-none user is redirected to /accessdenied on create/edit routes.
result: issue
reported: "it ok but the toast should show forbidden not generic message 'something went wrong' and why reload organization page it take so long time to load data"
severity: major

## Summary

total: 3
passed: 1
issues: 2
pending: 0
skipped: 0
blocked: 0

## Gaps

- truth: "Saving changes in the security-management screens should show a confirmation dialog before persisting data."
  status: diagnosed
  reason: "User reported: Create a 'Save' button. When the user clicks it, show a confirmation dialog asking 'Do you want to save?'. If the user agrees, proceed with saving the data."
  severity: minor
  test: 2
  root_cause: "ConfirmationService and p-confirmDialog are already used for delete flows in role-list and row-policy-list, but role-dialog.component.ts (save() line 60), row-policy-dialog.component.ts (save() line 88), and permission-matrix.component.ts ((ngModelChange) handlers) all call the API directly without invoking confirmationService.confirm()."
  artifacts:
    - frontend/src/app/pages/admin/security/roles/dialog/role-dialog.component.ts
    - frontend/src/app/pages/admin/security/row-policies/dialog/row-policy-dialog.component.ts
    - frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts
  missing:
    - confirmationService.confirm() call wrapping save() in role-dialog and row-policy-dialog
    - p-confirmDialog outlet in role-dialog and row-policy-dialog templates

- truth: "Access-denied errors should surface a 'Forbidden' toast, not a generic 'something went wrong' message."
  status: diagnosed
  reason: "User reported: toast shows generic 'something went wrong' instead of 'forbidden' when access is denied."
  severity: major
  test: 3
  root_cause: "All create/update error handlers (organization-update, department-update, employee-update, permission-matrix, role-dialog, row-policy-dialog, role-list, row-policy-list) use bare 'error: () =>{}' callbacks that discard the HttpErrorResponse entirely, so any 403 falls to a generic message. Only the three entity delete flows check err.status === 403. There is no shared interceptor or status-to-message mapping."
  artifacts:
    - frontend/src/app/pages/entities/organization/update/organization-update.component.ts
    - frontend/src/app/pages/entities/department/update/department-update.component.ts
    - frontend/src/app/pages/entities/employee/update/employee-update.component.ts
    - frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts
    - frontend/src/app/pages/admin/security/roles/dialog/role-dialog.component.ts
    - frontend/src/app/pages/admin/security/row-policies/dialog/row-policy-dialog.component.ts
    - frontend/src/app/pages/admin/security/roles/role-list.component.ts
    - frontend/src/app/pages/admin/security/row-policies/row-policy-list.component.ts
  missing:
    - 403-specific branch in all non-delete error handlers (or a shared ErrorHandlerInterceptor that maps status codes to messages)

- truth: "Organization list/detail page should reload data without noticeable lag."
  status: diagnosed
  reason: "User reported: reloading the organization page takes a long time to load data."
  severity: minor
  test: 3
  root_cause: "On hard reload the in-memory shareReplay cache in SecuredEntityCapabilityService is destroyed, forcing a three-request serial waterfall: GET /api/account (identity guard) → GET /api/security/entity-capabilities (route resolver) → GET /api/organizations (component). The entity data cannot start loading until the capability resolver completes. Soft navigation avoids this because the login-time prefetch warms the cache."
  artifacts:
    - frontend/src/app/pages/entities/organization/organization.routes.ts
    - frontend/src/app/pages/entities/organization/shared/service/secured-entity-capability.service.ts
  missing:
    - Parallel fetch of capability and entity data (e.g. move capability out of resolver into component ngOnInit, or persist capability across reloads via sessionStorage)
