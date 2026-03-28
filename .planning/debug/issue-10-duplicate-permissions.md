---
status: fixing
trigger: "Latest GitHub comment on issue #10 says permission data loads incompletely from DB, ticking checkboxes saves duplicate permission rows, disable/removal no longer works because duplicates remain, and authorization state becomes wrong due to duplicate records."
created: 2026-03-28T00:00:00Z
updated: 2026-03-28T00:28:00Z
---

## Current Focus

hypothesis: Confirmed. CamelCase attribute targets are normalized back to lowercase UI keys (for example `department.costCenter` becomes `department.costcenter`), so existing DB grants do not match matrix rows; users then re-create the same stored permission, and missing uniqueness/idempotent save semantics turn that mismatch into duplicate rows that unchecks do not fully remove
test: Patch outgoing normalization to preserve original attribute case, make permission creation idempotent by logical key, and enforce uniqueness at the schema level; then verify with regression tests
expecting: Persisted camelCase grants will load under the same key the matrix uses, repeated POSTs for the same logical permission will reuse or dedupe the canonical row, and duplicates will no longer survive save or remove workflows
next_action: Patch UI target normalization, repository or resource save semantics, and Liquibase uniqueness for sec_permission

## Symptoms

expected: Existing permission rows should load completely from the database, toggling a checkbox should update the effective permission once, and unticking or disabling a permission should remove or negate the persisted row cleanly.
actual: Permission data loads incompletely from DB; ticking checkboxes creates additional duplicate permission rows; disable/removal no longer works correctly because duplicates remain; effective authorization becomes wrong because duplicate records accumulate.
errors: No stack trace reported; issue manifests as incorrect permission persistence and authorization state.
reproduction: Open the admin permission matrix for the affected role, observe missing persisted permissions, tick or untick boxes, save changes, then inspect DB/API state and effective authorization behavior for duplicate permission rows.
started: Reported in the latest GitHub comment on issue #10 on 2026-03-28; replaces the prior field-visibility symptom report.

## Eliminated

## Evidence

- timestamp: 2026-03-28T00:00:00Z
  checked: .planning/debug/authorization-field-10-visible.md
  found: Earlier issue #10 investigation determined the field-visibility report mixed legitimate DB grants with a permission-matrix wildcard rendering bug, but it did not examine duplicate-row persistence semantics.
  implication: The current duplicate-permission problem needs a separate investigation focused on load/save behavior and backend persistence, though the same permission-matrix area remains relevant.

- timestamp: 2026-03-28T00:12:00Z
  checked: frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts, frontend/src/app/pages/admin/security/shared/service/sec-permission.service.ts, src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java, src/main/java/com/vn/core/security/domain/SecPermission.java, src/main/java/com/vn/core/security/repository/SecPermissionRepository.java
  found: The frontend loads permissions into a `Map` keyed only by `target:action`, deletes by a single stored id, and creates via POST for every checked pending change; the backend POST endpoint blindly saves a new `SecPermission` row and the entity/repository define no logical uniqueness enforcement.
  implication: Duplicate logical permission rows would be collapsed on load, created repeatedly on save, and only one duplicate would be removed by an uncheck operation.

- timestamp: 2026-03-28T00:20:00Z
  checked: src/main/java/com/vn/core/service/security/SecPermissionUiContractService.java and frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts
  found: The UI-contract service builds stored-to-UI mappings from lowercased UI targets, while the matrix rows use catalog attribute names as-is; for camelCase attributes such as `costCenter`, outgoing normalization would yield `department.costcenter` but the checkbox row key remains `department.costCenter`.
  implication: Persisted camelCase attribute grants can load under a different key than the matrix row, making them appear absent and provoking duplicate inserts when the user re-checks them.

- timestamp: 2026-03-28T00:28:00Z
  checked: src/main/java/com/vn/core/web/rest/admin/security/SecCatalogAdminResource.java and src/main/java/com/vn/core/domain/Department.java
  found: The catalog API emits JPA attribute names unchanged, and the Department entity declares the field as `costCenter`.
  implication: The permission matrix row key is definitively `department.costCenter`, so the lowercase permission target returned by the backend is a real mismatch.

## Resolution

root_cause: The permission admin flow had two coupled defects. `SecPermissionUiContractService.normalizeOutgoing(...)` reconstructed UI targets from lowercased keys, so stored camelCase attribute permissions such as `DEPARTMENT.COSTCENTER` were returned as `department.costcenter` instead of `department.costCenter`. The permission matrix therefore failed to recognize existing DB grants and let users re-save them. The backend `POST /api/admin/sec/permissions` endpoint then inserted a brand-new `sec_permission` row every time because there was no logical uniqueness enforcement or idempotent upsert behavior, so re-saving a seemingly missing grant created duplicates that later unchecks could not fully remove.
fix:
verification:
files_changed: []
