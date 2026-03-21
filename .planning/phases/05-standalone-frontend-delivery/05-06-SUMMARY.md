---
phase: 05-standalone-frontend-delivery
plan: 06
subsystem: frontend-security-admin
tags: [angular, spring, permissions, matrix, routing]
dependency_graph:
  requires: [05-01, 05-02, 05-05]
  provides: [matrix-ui-contract-normalization, frontend-404-regression]
  affects: []
tech_stack:
  added: []
  patterns:
    - catalog-driven permission target normalization
    - UI-to-runtime effect translation
    - wildcard route regression coverage
    - matrix contract regression coverage
key_files:
  created:
    - src/main/java/com/vn/core/service/security/SecPermissionUiContractService.java
    - frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.spec.ts
  modified:
    - src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java
    - src/test/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResourceIT.java
    - src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java
    - frontend/src/app.routes.ts
decisions:
  - "The permission matrix keeps its locked GRANT/lowercase UI contract; backend normalization now translates that contract to runtime-compatible ALLOW and upper-case targets."
  - "Unknown frontend URLs now route to /404 explicitly instead of falling through to the dashboard shell."
requirements-completed: [UI-02, UI-03]
metrics:
  duration: parallel
  completed: 2026-03-22
  tasks_completed: 2
  files_created: 2
---

# Phase 5 Plan 06: Matrix Contract Repair Summary

Closed the remaining UI-02 and UI-03 contract gaps by routing unmatched frontend URLs to the 404 page and adding a backend translation layer that makes matrix-created permissions enforceable at runtime without changing the locked frontend payload contract.

## What Was Built

### Backend Permission Normalization

- Added `SecPermissionUiContractService` to translate matrix UI targets and `GRANT` effects into stored/runtime targets and `ALLOW` effects using `SecuredEntityCatalog` plus JPA metamodel attributes.
- Updated `SecPermissionAdminResource` so create/update paths normalize incoming matrix payloads before persistence.
- Updated admin permission and secured-entity integration tests to prove matrix-created permissions round-trip as `GRANT` on reads while changing protected-entity behavior end to end.

### Frontend Route And Matrix Regression Coverage

- Changed the wildcard frontend route to redirect to `/404`.
- Added `permission-matrix.component.spec.ts` to lock the matrix client to `effect: 'GRANT'` and preserve wildcard attribute-disable behavior.

## Task Commits

1. **Task 1: Normalize matrix permissions for runtime enforcement** - `bc5a48c` (`feat(05-06)`)
2. **Task 2: Lock matrix route and UI regressions** - `321ec6c` (`test(05-06)`)

## Verification

- `cmd /c npx.cmd ng test --watch=false`
- `JAVA_HOME=C:\Users\admin\.jdks\temurin-25.0.2 GRADLE_USER_HOME=D:\jhipster\.gradle-home .\gradlew integrationTest --tests "com.vn.core.web.rest.admin.security.SecPermissionAdminResourceIT" --tests "com.vn.core.web.rest.SecuredEntityEnforcementIT"`

## Self-Check: PASSED

Matrix-created entity and attribute permissions now survive the UI/runtime contract boundary, the proof organization API reacts to a matrix-style admin POST, and unmatched frontend routes now land on the 404 page.
