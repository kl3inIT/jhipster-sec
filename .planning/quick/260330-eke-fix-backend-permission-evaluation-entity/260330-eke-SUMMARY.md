---
quick_task: 260330-eke
title: Fix backend permission evaluation entity
completed: "2026-03-30T03:37:20Z"
duration: 8 min
tasks_completed: 2
files_changed: 3
commits:
  - bbdbb40
tags:
  - testing
  - security
  - permissions
key_files:
  created:
    - src/test/java/com/vn/core/security/permission/PermissionMatrixTest.java
  modified:
    - src/test/java/com/vn/core/security/permission/RolePermissionServiceDbImplTest.java
    - src/test/java/com/vn/core/security/permission/AttributePermissionEvaluatorImplTest.java
decisions:
  - PermissionMatrixTest tests the matrix directly rather than through evaluators to isolate key-building logic
  - Added @Mock RequestPermissionSnapshot to both evaluator tests for proper Mockito constructor injection
---

# Quick Task 260330-eke Summary

## One-liner

Added 16-test `PermissionMatrixTest` covering ALLOW/DENY/wildcard semantics and fixed missing `@Mock RequestPermissionSnapshot` in both permission evaluator test classes.

## What Was Done

### Task 1 — PermissionMatrixTest

Created `src/test/java/com/vn/core/security/permission/PermissionMatrixTest.java` with 16 unit tests:

- `emptyPermissionListDeniesEntityOp` — deny-default from empty list
- `emptyConstantDeniesEverything` — EMPTY constant denies entity and attribute checks
- `entityAllowGrantsMatchingOp` — ALLOW entry grants the exact entity+action
- `entityAllowDoesNotGrantDifferentOp` — ALLOW for READ does not grant CREATE
- `entityAllowDoesNotGrantDifferentEntity` — ALLOW for ORDER does not grant INVOICE
- `entityDenyOnlyReturnsFalse` — DENY-only entry denies
- `entityAllowAndDenyTogetherStillPermits` — union-of-ALLOW: any ALLOW wins over DENY
- `specificAttributeAllowGrantsView` — ATTRIBUTE ALLOW grants the exact view
- `specificAttributeAllowDoesNotGrantEdit` — VIEW ALLOW does not grant EDIT
- `attributeDenyOnlyReturnsFalse` — DENY-only denies attribute
- `wildcardAttributeAllowGrantsSpecificAttribute` — `ENTITY.*` grants specific field
- `wildcardAttributeAllowGrantsMultipleFields` — wildcard EDIT grants multiple fields
- `wildcardAttributeDoesNotGrantDifferentEntity` — wildcard doesn't bleed across entities
- `entityAllowDoesNotGrantAttributeCheck` — entity ALLOW doesn't satisfy attribute check
- `attributeAllowDoesNotGrantEntityCheck` — attribute ALLOW doesn't satisfy entity check
- `multipleEntityAllowsGrantAll` — multiple ALLOW entries each grant their target

### Task 2 — @Mock injection fix

Added `@Mock RequestPermissionSnapshot requestPermissionSnapshot;` to:
- `RolePermissionServiceDbImplTest` — matches the three-arg constructor of `RolePermissionServiceDbImpl`
- `AttributePermissionEvaluatorImplTest` — matches the three-arg constructor of `AttributePermissionEvaluatorImpl`

Previously Mockito's `@InjectMocks` injected `null` for the missing mock. The tests still passed because `RequestPermissionSnapshot.isRequestScopeActive()` (a static method checking `RequestContextHolder`) always returns `false` in unit test context, so the null reference was never dereferenced. Adding the mock makes the injection explicit and correct.

## Verification

```
./gradlew test --tests "*PermissionMatrix*" --tests "*RolePermission*" --tests "*AttributePermission*" -x integrationTest
```

Result: BUILD SUCCESSFUL
- `PermissionMatrixTest` — 16 tests, 0 failures
- `RolePermissionServiceDbImplTest` — 7 tests, 0 failures
- `AttributePermissionEvaluatorImplTest` — 8 tests, 0 failures

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None.

## Self-Check: PASSED

- `src/test/java/com/vn/core/security/permission/PermissionMatrixTest.java` — FOUND
- Commit `bbdbb40` — FOUND
