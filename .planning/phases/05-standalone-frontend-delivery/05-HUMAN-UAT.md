---
status: partial
phase: 05-standalone-frontend-delivery
source: [05-VERIFICATION.md]
started: 2026-03-21T20:17:37.2052960Z
updated: 2026-03-23T00:00:00.000Z
---

## Current Test

[testing paused — Test 3 awaiting manual human verification]

## Tests

### 1. JWT login round trip and token persistence
expected: Valid credentials reach the dashboard, refresh keeps the session, and an expired session redirects back to /login.
result: pass

### 2. Admin security-management flow in a browser
expected: An admin can create or update roles and row policies, change matrix permissions, reload the screens, and the saved changes persist.
result: issue
reported: "navigate to manage permission tick into permission matrix and my app crash and i cant select entity to view attribute"
severity: blocker
fix_applied: |
  Two root causes diagnosed and fixed (commits dc92571, fbd62ed, d84af4d):
  1. @else if (selectedEntity; as entity) — invalid Angular control-flow alias, replaced with plain @else
  2. getAttributeRows() called directly in template binding — returned new array every CD cycle,
     triggering PrimeNG p-checkbox markForCheck() → Angular reactive scheduler → infinite tick loop
     → browser renderer crash. Fixed by caching rows in selectedEntityAttributeRows property.
  Playwright E2E: 4/4 tests pass (entity table renders, entity row click shows attribute panel,
  checkbox tick does not crash, attribute checkboxes work after entity select).
retest_result: pass

### 3. Protected-entity gating across proof roles
expected: Reader, editor, and none users see different actions and sensitive fields, and denied create or edit routes land on /accessdenied before form controls render.
result: pending
automated_result: |
  Playwright E2E suite added (frontend/e2e/proof-role-gating.spec.ts) — 6/6 tests pass:
  - READER: org list shows no New/Edit/Delete buttons ✓
  - READER: /entities/organization/new redirects to /accessdenied ✓
  - EDITOR: org list shows New Organization button ✓
  - EDITOR: /entities/organization/new renders form without redirect ✓
  - NONE: org list shows no buttons ✓
  - NONE: /entities/organization/new redirects to /accessdenied ✓
manual_check_accounts: |
  Three persistent test accounts created in dev DB for human verification:

  proof-reader / Test1234! — ROLE_PROOF_READER
    1. Organizations list loads, no New/Edit/Delete buttons visible
    2. Navigate to /entities/organization/new → Access Denied page

  proof-editor / Test1234! — ROLE_PROOF_EDITOR
    1. Organizations list shows "New Organization" button
    2. Click New Organization → form opens (not redirected)
    3. Budget field is absent from the form (attribute EDIT denied)

  proof-none / Test1234! — ROLE_PROOF_NONE
    1. Organizations list: no rows, no buttons
    2. Navigate to /entities/organization/new → Access Denied page

## Summary

total: 3
passed: 2
issues: 0
pending: 1
skipped: 0
blocked: 0

## Gaps

[none — all diagnosed gaps resolved]
