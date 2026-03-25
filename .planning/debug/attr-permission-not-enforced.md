---
status: investigating
trigger: "Investigate why attribute-level READ permissions are not being enforced in the Angular frontend for the organization detail/view component. proof-reader can see code field (READ=false) and Edit button (no Update permission)."
created: 2026-03-23T00:00:00Z
updated: 2026-03-23T00:01:00Z
---

## Current Focus

hypothesis: CONFIRMED (two separate root causes found)
test: Read organization-detail.component.html, organization-detail.component.ts, secured-entity-capability.model.ts, secured-entity-capability.service.ts, SecuredEntityCapabilityService.java
expecting: Template uses no attribute capability signals — confirmed
next_action: Report ROOT CAUSE FOUND

## Symptoms

expected: proof-reader should NOT see the `code` field (READ=false in permission matrix) and should NOT see Edit button (no Update entity permission)
actual: proof-reader CAN see the `code` field and CAN see the Edit button
errors: none reported — visual/behavioral regression
reproduction: Log in as proof-reader, navigate to organization detail view
started: Unknown — possibly always broken

## Eliminated

- hypothesis: Backend does not return attribute capabilities or canUpdate correctly
  evidence: SecuredEntityCapabilityService.java (line 69) calls attributePermissionEvaluator.canView() per attribute and sets canUpdate from entityPermissionEvaluator. The API payload is correctly structured. Issue is entirely in the Angular template/component consuming the data.
  timestamp: 2026-03-23T00:01:00Z

- hypothesis: Key mismatch between frontend and backend attribute name
  evidence: Backend derives attribute names from JPA metamodel (Attribute::getName), which returns "code" in lowerCamelCase. The permission matrix stores target as "organization.code". The capability payload uses ISecuredAttributeCapability.name = "code". The template simply does not look up this name at all — the bug is the absence of any lookup, not a key mismatch.
  timestamp: 2026-03-23T00:01:00Z

## Evidence

- timestamp: 2026-03-23T00:01:00Z
  checked: organization-detail.component.html lines 11-14
  found: The "Code" field row has no @if or structural directive. It is always rendered unconditionally: `<span>{{ org.code }}</span>` with no guard.
  implication: Attribute-level READ capability (ISecuredAttributeCapability.canView for "code") is never consulted. The field is always visible.

- timestamp: 2026-03-23T00:01:00Z
  checked: organization-detail.component.ts lines 21, 53-66 (loadCapability)
  found: Component only reads capability.canUpdate from the API response and stores it in a single `canUpdate` signal. It does not extract the `attributes` array into any signals or maps. There is no canViewAttribute signal, no attributeMap, nothing.
  implication: Even though the API returns full attribute capabilities (ISecuredEntityCapability.attributes), the component discards them entirely. No field-level visibility control is possible with the current implementation.

- timestamp: 2026-03-23T00:01:00Z
  checked: organization-detail.component.html line 33
  found: Edit button is guarded by `@if (capabilityLoaded() && canUpdate())` — this IS the correct guard mechanism.
  implication: The Edit button guard is architecturally correct. The question is whether canUpdate() is ever set to false for proof-reader.

- timestamp: 2026-03-23T00:01:00Z
  checked: secured-entity-capability.service.ts lines 23-25 (getEntityCapability)
  found: Uses `capability.code === code` where code = 'organization'. The backend sets dto.setCode(entry.code()) from SecuredEntityEntry.code(). This match depends on what string the catalog registers the organization entity under.
  implication: If the catalog registers organization as "Organization" (PascalCase) rather than "organization" (lowercase), getEntityCapability('organization') returns null, capability becomes null, canUpdate defaults to false (line 59: `capability?.canUpdate ?? false`). If null is returned, canUpdate = false is correct — Edit button should hide. But user reports Edit IS visible.

- timestamp: 2026-03-23T00:01:00Z
  checked: SecuredEntityCapabilityService.java lines 43-51 (toDto)
  found: dto.setCode(entry.code()) — the code value comes from SecuredEntityEntry.code(). Need to verify what value that returns for Organization.
  implication: If entry.code() returns "Organization" (capital O), then frontend lookup for 'organization' fails → returns null → canUpdate defaults to false → Edit button should hide. This contradicts the reported symptom. Must check the catalog registration.

- timestamp: 2026-03-23T00:01:00Z
  checked: secured-entity-capability.service.spec.ts line 50
  found: Test fixture uses capability code 'organization' (lowercase) and the test hits `api/security/entity-capabilities` without /api prefix (bare URL in test vs ApplicationConfigService in prod).
  implication: The test establishes that lowercase 'organization' is the intended key. Catalog likely registers it as 'organization'. The Edit button visibility failure is therefore explained differently — see below.

## Resolution

root_cause: THREE independent bugs across backend semantics + frontend display:

BUG 1 — Permission matrix uses GRANT-only storage; permissive-default evaluator treats "no record" as ALLOW (backend + UI contract semantic mismatch):
The permission matrix UI (permission-matrix.component.ts lines 118-148 / 156-169) only ever creates GRANT records or deletes them. Unchecking a VIEW checkbox deletes the GRANT row — it never creates a DENY row. The AttributePermissionEvaluatorImpl (AttributePermissionEvaluatorImpl.java lines 42-62) uses "permissive default" semantics: when `secPermissionRepository.findByRolesAndTarget(...)` returns an empty list (zero records), it returns `true` (line 53). So when the admin unchecks VIEW for `organization.code` for proof-reader, the GRANT is deleted, the evaluator finds no records, defaults to permissive, and `canView` in the capability response is still `true`. This means the backend API itself returns `canView: true` for `code` even after unchecking — the attribute is not actually blocked at source.

BUG 2 — Frontend detail component ignores attribute capabilities entirely (frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts + .html):
`OrganizationDetailComponent.loadCapability()` (component.ts lines 53-67) fetches the capability response but only reads `capability.canUpdate` (line 59). The full `attributes: ISecuredAttributeCapability[]` array from the API is silently discarded. No attribute-level signals are declared. In organization-detail.component.html, every field row — including Code (lines 11-14) — renders unconditionally with no `@if` guard. Even if BUG 1 were fixed and the backend returned `canView: false` for `code`, the template would still render the field because no attribute guard exists.

BUG 3 — SecuredEntityCapabilityService.cachedCapabilities$ is a root-scoped singleton with no session invalidation (frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts lines 14-20):
The service is `providedIn: 'root'` and caches the HTTP response via `shareReplay(1)`. When an admin user loads any page that triggers `query()`, the admin's capabilities are cached. If a proof-reader then navigates to the organization detail page in the same browser session (e.g., test runner that doesn't full-reload, or HMR), the singleton still holds the admin's cached observable and serves admin's `canUpdate: true` to the proof-reader's component. This is why `canUpdate()` is `true` despite proof-reader having no UPDATE permission — the Edit button guard condition `@if (capabilityLoaded() && canUpdate())` is correct in form but fed stale data.

fix: (not applied — diagnose-only mode)
  BUG 1: Change the permission matrix to use DENY semantics instead of delete-on-uncheck. When unchecking a permission, create a DENY record rather than deleting the GRANT. Alternatively, invert the evaluator to use deny-default (no records = deny) for attribute permissions, consistent with entity-level permissions in RolePermissionServiceDbImpl which already uses deny-default (lines 52-58: returns false when no records found via isEmpty implicit through no-ALLOW path).
  BUG 2: In organization-detail.component.ts, build a `Map<string, boolean>` of attributeName→canView from `capability.attributes` in `loadCapability()`. Expose a helper method `canViewField(name: string): boolean`. In the template, wrap each field row in `@if (canViewField('code'))`, `@if (canViewField('name'))`, etc.
  BUG 3: Clear `cachedCapabilities$` in SecuredEntityCapabilityService on auth change. The service should listen to AccountService login/logout events and reset the cache, or use `takeUntilDestroyed` with a login-scoped lifetime, or scope the service to the route rather than root.
verification:
files_changed: []
