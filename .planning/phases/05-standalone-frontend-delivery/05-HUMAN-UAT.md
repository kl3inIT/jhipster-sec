---
status: partial
phase: 05-standalone-frontend-delivery
source: [05-VERIFICATION.md]
started: 2026-03-23T12:00:00Z
updated: 2026-03-24T17:29:44Z
---

## Current Test

number: 2
name: Admin security-management flow - permission matrix save confirmation re-test
expected: |
  An admin can create or update roles and row policies, and the permission matrix
  shows Save Changes plus a confirmation dialog before committing permission writes.
awaiting: human re-test after plan 15 implementation

## Tests

### 1. JWT login round trip and token persistence
expected: Valid credentials reach the dashboard, refresh keeps the session, and an expired session redirects back to /login.
result: pass

### 2. Admin security-management flow - permission matrix save confirmation re-test
expected: An admin can create or update roles and row policies. The permission matrix shows Save Changes, cancelling preserves pending edits without API calls, and confirming commits the buffered permission writes.
result: [pending]
note: Code fix implemented in 05-15; browser confirmation still required.

### 3. Protected-entity gating - 403 toast + fast reload (re-test after plans 13 and 14)
expected: (a) A restricted user attempting a forbidden action sees "Access denied" toast (warn severity), not "something went wrong". (b) Hard-reloading an entity page loads without noticeable lag (capability served from sessionStorage). (c) Logging out and back in as a different user fetches fresh capabilities.
result: pass

## Summary

total: 3
passed: 2
issues: 0
pending: 1
skipped: 0
blocked: 0

## Gaps
