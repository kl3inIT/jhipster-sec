---
status: diagnosed
phase: 05-standalone-frontend-delivery
source: [05-VERIFICATION.md]
started: 2026-03-21T20:17:37.2052960Z
updated: 2026-03-22T10:00:00.000Z
---

## Current Test

[testing paused — 1 item blocked pending fix]

## Tests

### 1. JWT login round trip and token persistence
expected: Valid credentials reach the dashboard, refresh keeps the session, and an expired session redirects back to /login.
result: pass

### 2. Admin security-management flow in a browser
expected: An admin can create or update roles and row policies, change matrix permissions, reload the screens, and the saved changes persist.
result: issue
reported: "navigate to manage permission tick into permission matrix and my app crash and i cant select entity to view attribute"
severity: blocker

### 3. Protected-entity gating across proof roles
expected: Reader, editor, and none users see different actions and sensitive fields, and denied create or edit routes land on /accessdenied before form controls render.
result: blocked
blocked_by: prior-phase
reason: "can test because the issue fix it first"

## Summary

total: 3
passed: 1
issues: 1
pending: 0
skipped: 0
blocked: 1

## Gaps

- truth: "Admin can change permission matrix checkboxes and select an entity to view its attribute permissions without the app crashing"
  status: failed
  reason: "User reported: navigate to manage permission tick into permission matrix and my app crash and i cant select entity to view attribute"
  severity: blocker
  test: 2
  root_cause: |
    Two distinct bugs:
    1. CHECKBOX CRASH: p-checkbox uses both [ngModel] and (onChange) simultaneously. In zoneless Angular 21, PrimeNG's onChange fires after onModelChange() has already updated ngModel — causing a re-entrant CD cycle violation. Fix: replace (onChange) with (ngModelChange) to use Angular's standard two-way binding path, which is safe in zoneless mode.
    2. ENTITY SELECTION BROKEN: Template uses invalid Angular control-flow syntax `@else if (selectedEntity; as entity)` — the `as` alias is only valid on primary `@if`, not `@else if`. This causes the attribute table branch to never render when an entity is selected. Fix: replace with plain `@else` block (the prior branches already guard null and zero-attribute cases).
  artifacts:
    - path: "frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.html"
      issue: "p-checkbox uses [ngModel] + (onChange) causing zoneless re-entrant CD crash; also @else if (expr; as alias) is invalid template syntax"
  missing:
    - "Replace (onChange) with (ngModelChange) on all p-checkbox elements"
    - "Replace @else if (selectedEntity; as entity) with plain @else"
  debug_session: ""
