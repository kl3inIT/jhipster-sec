---
status: awaiting_human_verify
trigger: "Investigate GitHub issue #10: ROLE_NHANH can still see organization.id and department.costCenter in Department/Organization flows despite limited field-view permissions."
created: 2026-03-28T00:00:00Z
updated: 2026-03-28T00:48:00Z
---

## Current Focus

hypothesis: Explicit grants for `department.costCenter VIEW` and `organization.name VIEW` should load correctly; the real matrix bug is wildcard child rows rendering unchecked, while explicit unchecked screenshots are more likely unsaved local edits or stale evidence
test: Verify permission-matrix load/save semantics in component code and unit tests, then patch wildcard-derived child checkbox state to reflect stored grants
expecting: Explicit field grants will map directly into the `granted` map, local unsaved toggles will remain client-side until Save, and wildcard child rows will need a template/state fix
next_action: Ask reporter to reload the `ROLE_NHANH` permission matrix and confirm whether the remaining mismatch was the inherited wildcard display issue or an unsaved edit snapshot

## Symptoms

expected: A user with limited field-level VIEW permission must not receive or see denied fields such as organization.id or department.costCenter in secured API payloads or Angular UI.
actual: GitHub issue #10 reports ROLE_NHANH can still see organization.id and department.costCenter while viewing Department data.
errors: No stack trace reported; issue describes authorization leakage only.
reproduction: Log in as ROLE_NHANH user, open Department, inspect visible Department/Organization data in UI and API.
started: Reported on 2026-03-28; current HEAD may differ from prior attribute-permission investigation.

## Eliminated

## Evidence

- timestamp: 2026-03-28T00:08:00Z
  checked: .planning/debug/knowledge-base.md
  found: No debug knowledge base file exists yet for matching prior authorization patterns.
  implication: No prior resolved session can shortcut the current investigation.

- timestamp: 2026-03-28T00:10:00Z
  checked: frontend/src/app/pages/entities/department/detail/department-detail.component.html and frontend/src/app/pages/entities/department/list/department-list.component.html
  found: Department list/detail render `organization.name`, not `organization.id`, and both already guard `costCenter` behind `canViewField('costCenter')`.
  implication: The reported `organization.id` leak is unlikely to come from Department list/detail UI; `department.costCenter` is not obviously leaking from those two read-only templates.

- timestamp: 2026-03-28T00:12:00Z
  checked: src/main/resources/fetch-plans.yml and src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java
  found: Department fetch plans explicitly include nested `organization.id`, and the current enforcement integration test asserts `$[0].organization.id == 100` on `GET /api/departments`.
  implication: Backend response shaping currently expects nested organization IDs to be present in Department payloads.

- timestamp: 2026-03-28T00:14:00Z
  checked: src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java
  found: Serializer treats `id` as always visible for any entity whenever it appears in the fetch plan, bypassing attribute permission checks.
  implication: This is a direct mechanism for leaking denied `organization.id` and root `organization.id` in API responses.

- timestamp: 2026-03-28T00:16:00Z
  checked: frontend/src/app/pages/entities/organization/list/organization-list.component.html and frontend/src/app/pages/entities/organization/detail/organization-detail.component.html
  found: Organization list and Organization detail both render `organization.id` unconditionally with no attribute-capability guard.
  implication: Even if the backend stopped sending denied IDs, the Organization UI currently has no field-level protection for the `id` field.

- timestamp: 2026-03-28T00:18:00Z
  checked: src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java and frontend/src/app/pages/entities/department/list/department-list.component.spec.ts
  found: Capability DTO generation enumerates all JPA attributes, including `costCenter`, with explicit `canView` values; Department list tests already cover hiding `costCenter` when capability denies it.
  implication: A `department.costCenter` leak is less likely to be caused by a missing capability entry on Department list and more likely to be backend serialization or a different UI surface that ignores attribute permissions.

- timestamp: 2026-03-28T00:32:00Z
  checked: Live dev DB evidence provided by reporter
  found: User `nhanh` has authority `ROLE_NHANH`; persisted grants include `ENTITY DEPARTMENT READ`, `ENTITY DEPARTMENT UPDATE`, `ENTITY ORGANIZATION READ`, `ATTRIBUTE DEPARTMENT.COSTCENTER VIEW`, `ATTRIBUTE DEPARTMENT.* EDIT`, and `ATTRIBUTE ORGANIZATION.NAME VIEW`.
  implication: `department.costCenter` appearing in API/UI is expected from the stored permissions. The mismatch has shifted upstream to permission-matrix display or user workflow, not backend enforcement for that field.

- timestamp: 2026-03-28T00:40:00Z
  checked: frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts plus SecPermissionAdminResource/SecPermissionUiContractService
  found: The matrix fetches permissions for the route role name, normalizes stored targets back to UI keys, and populates `granted` by exact `target:action` keys. Explicit grants such as `department.costCenter VIEW` and `organization.name VIEW` therefore load as checked when returned by the API.
  implication: If those explicit VIEW cells appeared unchecked in a screenshot while the DB already contained those rows, the likely cause is not a load failure but either unsaved local edits, a stale screenshot, or a different role than `ROLE_NHANH`.

- timestamp: 2026-03-28T00:42:00Z
  checked: permission-matrix.component.ts toggle/flush flow
  found: Checkbox toggles only stage local `pendingChanges`; no API write occurs until Save/confirm triggers `flushChanges()`. The UI can therefore diverge from persisted DB state until the user clicks Save Changes.
  implication: An unchecked screenshot can be consistent with a still-granted DB row if the user unticked a box but never saved, or if save failed and the user captured the local staged state.

- timestamp: 2026-03-28T00:45:00Z
  checked: permission-matrix.component.html prior behavior for attribute child rows under wildcard grants
  found: Child attribute checkboxes used exact-key lookup for `[ngModel]` but only used wildcard state for disabling, so a stored wildcard like `department.* EDIT` could render child EDIT boxes as disabled yet visually unchecked.
  implication: This is a real code bug that can make the permission matrix appear to disagree with persisted grants.

- timestamp: 2026-03-28T00:47:00Z
  checked: `npx ng test --watch=false --include src/app/pages/admin/security/permission-matrix/permission-matrix.component.spec.ts`
  found: Permission-matrix spec passed with 21 tests after adding coverage for explicit attribute grant loading and wildcard-inherited child checkbox state.
  implication: The matrix now correctly reflects wildcard-derived attribute grants in the UI, and the explicit-grant load path remains verified.

## Resolution

root_cause: Two separate causes were identified. First, the live DB state for `ROLE_NHANH` legitimately grants `department.costCenter VIEW`, so the backend/UI showing that field is expected and not an enforcement bug. Second, the permission-matrix UI had a real rendering defect for inherited wildcard attribute grants: child rows computed checked state from exact keys only, so a stored wildcard like `department.* EDIT` could display the per-field EDIT checkbox as unchecked even though the grant existed.
fix: Updated the permission-matrix attribute checkbox bindings to use wildcard-aware effective state via `isAttributeEffectivelyGranted(...)`, so child rows now render as checked when covered by a stored wildcard grant. Added regression tests proving explicit field grants load into the matrix and wildcard-derived child rows display as checked.
verification: Angular unit test suite for `permission-matrix.component.spec.ts` passed (21 tests), including new cases for explicit attribute grants and wildcard child-row rendering. Code inspection also confirms toggles remain local until Save Changes, explaining how screenshots can differ from DB without any persisted update.
files_changed:
  - frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts
  - frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.html
  - frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.spec.ts
