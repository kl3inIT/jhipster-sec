---
status: complete
phase: 05-standalone-frontend-delivery
source: [05-VERIFICATION.md]
started: 2026-03-23T12:00:00Z
updated: 2026-03-24T17:10:00Z
---

## Current Test

[testing complete]

## Tests

### 1. JWT login round trip and token persistence
expected: Valid credentials reach the dashboard, refresh keeps the session, and an expired session redirects back to /login.
result: pass

### 2. Admin security-management flow — save confirmation dialog (re-test after plan 13)
expected: An admin can create or update roles and row policies. Clicking Save shows a confirmation dialog ("Do you want to save?"). Confirming proceeds with saving; cancelling does not call the API.
result: issue
reported: "No Save button on permission matrix page (Entity/Attribute Permissions) — permissions auto-save on each checkbox toggle instead of showing a Save + confirmation dialog"
severity: major

### 3. Protected-entity gating — 403 toast + fast reload (re-test after plans 13 and 14)
expected: (a) A restricted user attempting a forbidden action sees "Access denied" toast (warn severity), not "something went wrong". (b) Hard-reloading an entity page loads without noticeable lag (capability served from sessionStorage). (c) Logging out and back in as a different user fetches fresh capabilities.
result: pass

## Summary

total: 3
passed: 2
issues: 1
pending: 0
skipped: 0
blocked: 0

## Gaps

- truth: "Permission matrix page has an explicit Save button with confirmation dialog before committing changes"
  status: failed
  reason: "User reported: no Save button on permission matrix — permissions auto-save on each checkbox toggle instead of batching with an explicit Save + confirmation"
  severity: major
  test: 2
  artifacts: [frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.html, frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts]
  missing: [Save button in permission-matrix template, batch pending-changes state, confirmation dialog before committing]
