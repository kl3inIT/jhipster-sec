---
status: partial
phase: 05-standalone-frontend-delivery
source: [05-VERIFICATION.md]
started: 2026-03-21T20:17:37.2052960Z
updated: 2026-03-21T20:17:37.2052960Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. JWT login round trip and token persistence
expected: Valid credentials reach the dashboard, refresh keeps the session, and an expired session redirects back to /login.
result: pending

### 2. Admin security-management flow in a browser
expected: An admin can create or update roles and row policies, change matrix permissions, reload the screens, and the saved changes persist.
result: pending

### 3. Protected-entity gating across proof roles
expected: Reader, editor, and none users see different actions and sensitive fields, and denied create or edit routes land on /accessdenied before form controls render.
result: pending

## Summary

total: 3
passed: 0
issues: 0
pending: 3
skipped: 0
blocked: 0

## Gaps

None yet.
