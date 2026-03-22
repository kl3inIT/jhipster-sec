---
phase: 05-standalone-frontend-delivery
plan: 12
subsystem: security-permissions
tags: [attribute-permissions, deny-default, field-visibility, loading-state, angular, spring]
dependency_graph:
  requires: [05-09, 05-10, 05-11]
  provides: [attribute-level-field-gating, deny-default-backend, action-column-skeleton]
  affects: [organization-detail, department-detail, employee-detail, organization-list, department-list, employee-list]
tech_stack:
  added: []
  patterns: [deny-default-permission, capabilityLoaded-signal, fieldVisibility-signal, canViewField-helper]
key_files:
  created: []
  modified:
    - src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java
    - src/test/java/com/vn/core/security/permission/AttributePermissionEvaluatorImplTest.java
    - frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts
    - frontend/src/app/pages/entities/organization/detail/organization-detail.component.html
    - frontend/src/app/pages/entities/department/detail/department-detail.component.ts
    - frontend/src/app/pages/entities/department/detail/department-detail.component.html
    - frontend/src/app/pages/entities/employee/detail/employee-detail.component.ts
    - frontend/src/app/pages/entities/employee/detail/employee-detail.component.html
    - frontend/src/app/pages/entities/organization/list/organization-list.component.ts
    - frontend/src/app/pages/entities/organization/list/organization-list.component.html
    - frontend/src/app/pages/entities/department/list/department-list.component.ts
    - frontend/src/app/pages/entities/department/list/department-list.component.html
    - frontend/src/app/pages/entities/employee/list/employee-list.component.ts
    - frontend/src/app/pages/entities/employee/list/employee-list.component.html
decisions:
  - "AttributePermissionEvaluatorImpl uses deny-default (empty perms = false) because permission matrix stores only GRANT records; empty result means no GRANT was given"
  - "canViewField() uses capabilityLoaded() gate: optimistically shows fields before load, then applies vis map — avoids blank detail view on empty attributes array"
  - "Action columns always visible (removed showRowActions() gate from header/body); spinner shown until capabilityLoaded computed signal becomes true"
metrics:
  duration_minutes: 5
  completed_date: "2026-03-22"
  tasks_completed: 3
  files_modified: 14
---

# Phase 05 Plan 12: Attribute Permission Deny-Default and Field Visibility Summary

**One-liner:** Backend deny-default for attribute permissions plus frontend field-level visibility gating on all three entity detail views, with action-column skeleton loading.

## What Was Built

### Task 1 — Backend deny-default (commit 2e6465a)

`AttributePermissionEvaluatorImpl.checkAttributePermission()` previously returned `true` when the permission repository returned an empty list (no records for the attribute). Since the permission matrix UI only stores GRANT records (unchecking deletes the row), this made the entire attribute permission matrix ineffective — unchecked attributes were treated as allowed.

Fix: changed `perms.isEmpty() -> return true` to `return false`. Updated Javadoc to reflect deny-default semantics.

Updated tests:
- `testCanViewWithNoRulesReturnsTrue` renamed to `testCanViewWithNoRulesReturnsFalse` with `assertThat(result).isFalse()`
- Added `testCanEditWithNoRulesReturnsFalse` confirming canEdit path also denies on empty

### Task 2 — Frontend field-level visibility (commit 2aacfb2)

All three entity detail components (organization, department, employee) now:
1. Hold a `fieldVisibility = signal<Record<string, boolean>>({})` signal
2. Build the visibility map in `loadCapability()` from `capability.attributes` (each attr's `canView` boolean)
3. Expose a `canViewField(fieldName: string): boolean` helper that:
   - Returns `true` before capability loads (optimistic — prevents blank detail view)
   - Returns `vis[fieldName] !== false` after load (explicit `false` hides, absent means visible)

All data fields in each detail template are wrapped with `@if (canViewField('fieldName'))` guards. ID fields remain ungated (structural identifiers). Existing undefined/null guards for optional fields are preserved and combined with the canViewField check using `&&`.

### Task 3 — Action column loading skeleton (commit 5bbbad4)

All three entity list components (organization, department, employee) now:
1. Add `capabilityLoaded = computed(() => this.capability() !== null)` signal
2. Action column header is always rendered (removed `@if (showRowActions())` wrapper)
3. Action column body cell shows `<i class="pi pi-spin pi-spinner text-gray-400"></i>` while `!capabilityLoaded()`, then reveals actual action buttons

This eliminates the abrupt action column pop-in after capability resolves. The emptymessage colspan is updated to always include the actions column.

## Deviations from Plan

**1. [Rule 3 - Blocking] Gradle test verification skipped — Java version mismatch**

- **Found during:** Task 1 verification
- **Issue:** `build.gradle` asserts `java.specification.version == "25"` but only Java 17 is installed in the environment. The `gradlew test` command fails at project evaluation before tests run.
- **Fix:** Skipped automated test verification. The code changes are structurally correct: the renamed test method and updated assertion (`isFalse()`) are consistent with the backend deny-default change. The CI pipeline (which runs with Java 25) will validate.
- **Files modified:** none (environment pre-existing issue, not caused by plan 12)

## Verification Results

- `grep canViewField` in all three detail .component.ts files: 1 match each (the helper method definition)
- `grep canViewField` in organization-detail.html: 4 matches (code, name, ownerLogin, budget)
- `grep canViewField` in department-detail.html: 4 matches (code, name, organization, costCenter)
- `grep canViewField` in employee-detail.html: 6 matches (employeeNumber, firstName, lastName, department, email, salary)
- `grep capabilityLoaded` in all three list .component.ts files: match in each
- `grep pi-spin pi-spinner` in all three list .component.html files: 1 match each
- Production build (`ng build --configuration=production`): SUCCESS (bundle size warning is pre-existing)

## Self-Check: PASSED

- `D:/jhipster/src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java` contains `return false` in the `perms.isEmpty()` branch
- `D:/jhipster/src/test/java/com/vn/core/security/permission/AttributePermissionEvaluatorImplTest.java` contains `testCanViewWithNoRulesReturnsFalse` and `testCanEditWithNoRulesReturnsFalse`
- All six detail component files contain `canViewField` references
- All three list component .ts files contain `capabilityLoaded`
- All three list component .html files contain `pi-spin pi-spinner`
- Commits confirmed: 2e6465a, 2aacfb2, 5bbbad4
