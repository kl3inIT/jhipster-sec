---
phase: 05-standalone-frontend-delivery
plan: 04
subsystem: frontend-security-admin
tags: [angular, primeng, security, roles, row-policies, crud]
dependency_graph:
  requires: [05-02]
  provides: [security-admin-roles-ui, security-admin-row-policies-ui, sec-catalog-service, sec-permission-service]
  affects: [05-05]
tech_stack:
  added: []
  patterns: [standalone-component, reactive-forms, confirmation-service, primeng-table, primeng-select, primeng-dialog]
key_files:
  created:
    - frontend/src/app/pages/admin/security/security.routes.ts
    - frontend/src/app/pages/admin/security/shared/sec-catalog.model.ts
    - frontend/src/app/pages/admin/security/shared/sec-permission.model.ts
    - frontend/src/app/pages/admin/security/shared/service/sec-catalog.service.ts
    - frontend/src/app/pages/admin/security/shared/service/sec-permission.service.ts
    - frontend/src/app/pages/admin/security/roles/sec-role.model.ts
    - frontend/src/app/pages/admin/security/roles/service/sec-role.service.ts
    - frontend/src/app/pages/admin/security/roles/list/role-list.component.ts
    - frontend/src/app/pages/admin/security/roles/list/role-list.component.html
    - frontend/src/app/pages/admin/security/roles/dialog/role-dialog.component.ts
    - frontend/src/app/pages/admin/security/roles/dialog/role-dialog.component.html
    - frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts
    - frontend/src/app/pages/admin/security/row-policies/sec-row-policy.model.ts
    - frontend/src/app/pages/admin/security/row-policies/service/sec-row-policy.service.ts
    - frontend/src/app/pages/admin/security/row-policies/list/row-policy-list.component.ts
    - frontend/src/app/pages/admin/security/row-policies/list/row-policy-list.component.html
    - frontend/src/app/pages/admin/security/row-policies/dialog/row-policy-dialog.component.ts
    - frontend/src/app/pages/admin/security/row-policies/dialog/row-policy-dialog.component.html
  modified: []
decisions:
  - "ISecRole uses name as PK string (not numeric id) matching backend RoleType.valueOf() contract"
  - "RoleType enum values are RESOURCE and ROW_LEVEL (not RESOURCE_ROLE/ROW_LEVEL_ROLE - UI-SPEC doc error corrected)"
  - "policyType options limited to SPECIFICATION and JPQL; JAVA excluded to prevent AccessDeniedException"
  - "PermissionMatrixComponent is a placeholder stub for Plan 05-05"
metrics:
  duration_minutes: 7
  completed_date: "2026-03-21"
  tasks_completed: 2
  files_created: 18
---

# Phase 5 Plan 04: Security Admin CRUD Screens Summary

Security admin roles list/dialog and row-policies list/dialog with shared catalog and permission services, enabling full frontend CRUD for security roles and row policies.

## What Was Built

### Shared Models and Services

- `ISecCatalogEntry` model for entity catalog entries from `api/admin/sec/catalog`
- `ISecPermission` model with `authorityName` filter support
- `SecCatalogService` - queries `api/admin/sec/catalog` for entity dropdown population
- `SecPermissionService` - CRUD with `authorityName` query param for filtered reads

### Roles Admin

- `ISecRole` model: `name` as primary key string (no numeric id), `type` as `"RESOURCE"` or `"ROW_LEVEL"`
- `SecRoleService`: full CRUD using name in URL paths (`/api/admin/sec/roles/{name}`)
- `RoleListComponent`: p-table with Name/DisplayName/Type/Actions columns, empty state, Manage Permissions navigation to `/admin/security/roles/:name/permissions`
- `RoleDialogComponent`: reactive form with name readonly in edit mode, type p-select with exact enum values `RESOURCE` and `ROW_LEVEL`, save/error handling

### Row Policies Admin

- `ISecRowPolicy` model: `id` as optional numeric PK, `policyType` limited to `SPECIFICATION` and `JPQL`
- `SecRowPolicyService`: full CRUD using id in URL paths (`/api/admin/sec/row-policies/{id}`)
- `RowPolicyListComponent`: p-table with Code/Entity/Operation/PolicyType/Expression columns, truncated expression display, empty state
- `RowPolicyDialogComponent`: entity selector populated from `SecCatalogService`, graceful empty catalog handling, `policyType` options limited to SPECIFICATION and JPQL (JAVA excluded)

### Security Routes

Updated `security.routes.ts` with lazy-loaded routes for `roles`, `roles/:name/permissions`, and `row-policies`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Pre-existing build errors from parallel Plan 05-03**
- **Found during:** Task 1 build verification
- **Issue:** Plan 05-03 (parallel execution) had created `department.routes.ts`, `entity.routes.ts`, and `employee/*` route references before the components existed, causing TypeScript build errors on missing component imports
- **Fix:** Created stub components for `department-detail`, `department-update`, `employee-list`, `employee-detail`, `employee-update`, and `employee.routes.ts`; the parallel agent subsequently replaced these stubs with full implementations
- **Files modified:** `frontend/src/app/pages/entities/department/detail/department-detail.component.ts`, `frontend/src/app/pages/entities/employee/employee.routes.ts`, and employee list/detail/update stubs
- **Commit:** aee7b58

## Known Stubs

- `frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts` - Displays "Permission matrix coming soon" placeholder; Plan 05-05 will implement the full permission matrix editor

## Self-Check
