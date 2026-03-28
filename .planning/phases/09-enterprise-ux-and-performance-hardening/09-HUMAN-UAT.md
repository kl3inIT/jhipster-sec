---
status: partial
phase: 09-enterprise-ux-and-performance-hardening
source: [09-VERIFICATION.md]
started: 2026-03-28T08:50:00Z
updated: 2026-03-28T08:50:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. Skeleton Loader Visibility
expected: Navigate to /entities/department, /entities/employee, /entities/organization with Slow 3G throttling — 5 animated skeleton rows appear while initial HTTP request is in-flight; real rows replace them once data loads; subsequent page changes show PrimeNG loading overlay (not skeletons).
result: [pending]

### 2. Responsive Column Hiding at Tablet Width
expected: At viewport <1024px — Department hides id/organization/costCenter; Employee hides id/department/salary; Organization hides id/ownerLogin/budget; action buttons stack vertically; emptymessage colspan is correct at both widths.
result: [pending]

### 3. N+1 Query Reduction Under Real HTTP Request
expected: GET /api/entities/departments?page=0&size=20 with SQL logging enabled shows at most 2 permission-related queries (one jhi_authority SELECT, one bulk sec_permission SELECT). Previous behavior was ~202 queries.
result: [pending]

## Summary

total: 3
passed: 0
issues: 0
pending: 3
skipped: 0
blocked: 0

## Gaps
