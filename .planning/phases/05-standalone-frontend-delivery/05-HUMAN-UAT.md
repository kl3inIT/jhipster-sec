---
status: partial
phase: 05-standalone-frontend-delivery
source: [05-VERIFICATION.md]
started: 2026-03-23T12:00:00Z
updated: 2026-03-23T18:00:00Z
---

## Current Test

[awaiting human re-verification — all 3 diagnosed gaps fixed by plans 13 and 14]

## Tests

### 1. JWT login round trip and token persistence
expected: Valid credentials reach the dashboard, refresh keeps the session, and an expired session redirects back to /login.
result: pass

### 2. Admin security-management flow — save confirmation dialog (re-test after plan 13)
expected: An admin can create or update roles and row policies. Clicking Save shows a confirmation dialog ("Do you want to save?"). Confirming proceeds with saving; cancelling does not call the API.
result: [pending]

### 3. Protected-entity gating — 403 toast + fast reload (re-test after plans 13 and 14)
expected: (a) A restricted user attempting a forbidden action sees "Access denied" toast (warn severity), not "something went wrong". (b) Hard-reloading an entity page loads without noticeable lag (capability served from sessionStorage). (c) Logging out and back in as a different user fetches fresh capabilities.
result: [pending]

## Summary

total: 3
passed: 1
issues: 0
pending: 2
skipped: 0
blocked: 0

## Gaps
