---
quick_task: 260330-eke
title: Fix backend permission evaluation entity
type: fix
autonomous: true
---

# Quick Task: Fix Backend Permission Evaluation Entity

## Objective

Add missing `PermissionMatrixTest` unit test class and add `@Mock RequestPermissionSnapshot`
to existing permission evaluator tests so the `*PermissionMatrix*` pattern finds coverage
and all permission unit tests are properly structured.

## Tasks

### Task 1 — Add PermissionMatrixTest

Create `src/test/java/com/vn/core/security/permission/PermissionMatrixTest.java`
covering:
- Empty permission list → deny (EMPTY constant)
- ALLOW entry → permits entity/attribute checks
- DENY-only entry → deny
- Wildcard attribute ALLOW (`ENTITY.*`) → permits specific attribute
- `isEntityPermitted` vs `isAttributePermitted` dispatch
- Case: attrTarget without dot handled safely

### Task 2 — Fix missing @Mock in existing tests

Add `@Mock RequestPermissionSnapshot requestPermissionSnapshot;` to:
- `RolePermissionServiceDbImplTest`
- `AttributePermissionEvaluatorImplTest`

Ensures proper Mockito constructor injection pattern.

## Verification

Run: `./gradlew test --tests "*PermissionMatrix*" --tests "*RolePermission*" --tests "*AttributePermission*" -x integrationTest 2>&1 | tail -20`

Expected: BUILD SUCCESSFUL, PermissionMatrixTest tests included.
