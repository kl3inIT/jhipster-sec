---
status: partial
phase: 05-standalone-frontend-delivery
source: [05-VERIFICATION.md]
started: 2026-03-23T12:00:00Z
updated: 2026-03-23T12:00:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. JWT login round trip and token persistence
expected: Valid credentials reach the dashboard, refresh keeps the session, and an expired session redirects back to /login.
result: [pending]

### 2. Admin security-management flow in a browser
expected: An admin can create or update roles and row policies, change matrix permissions by ticking checkboxes, click an entity row to reveal its attribute permission panel, reload the screens, and the saved changes persist.
result: [pending]

### 3. Protected-entity gating with deny-default + canViewField fixes (UAT Test 3 retest)
expected: Proof-reader cannot see Code field on organization detail; proof-reader has no Edit button on detail; logout completes without lag; action columns show a spinner before capability resolves; proof-none user is redirected to /accessdenied on create/edit routes.
result: [pending]

## Summary

total: 3
passed: 0
issues: 0
pending: 3
skipped: 0
blocked: 0

## Gaps
