---
status: partial
phase: 05-standalone-frontend-delivery
source: [05-VERIFICATION.md]
started: 2026-03-21T20:17:37.2052960Z
updated: 2026-03-23T02:30:00.000Z
---

## Current Test

[testing complete — diagnosing issues]

## Tests

### 1. JWT login round trip and token persistence
expected: Valid credentials reach the dashboard, refresh keeps the session, and an expired session redirects back to /login.
result: pass

### 2. Admin security-management flow in a browser
expected: An admin can create or update roles and row policies, change matrix permissions, reload the screens, and the saved changes persist.
result: issue
reported: "navigate to manage permission tick into permission matrix and my app crash and i cant select entity to view attribute"
severity: blocker
fix_applied: |
  Two root causes diagnosed and fixed (commits dc92571, fbd62ed, d84af4d):
  1. @else if (selectedEntity; as entity) — invalid Angular control-flow alias, replaced with plain @else
  2. getAttributeRows() called directly in template binding — returned new array every CD cycle,
     triggering PrimeNG p-checkbox markForCheck() → Angular reactive scheduler → infinite tick loop
     → browser renderer crash. Fixed by caching rows in selectedEntityAttributeRows property.
  Playwright E2E: 4/4 tests pass (entity table renders, entity row click shows attribute panel,
  checkbox tick does not crash, attribute checkboxes work after entity select).
retest_result: pass

### 3. Protected-entity gating across proof roles
expected: Reader, editor, and none users see different actions and sensitive fields, and denied create or edit routes land on /accessdenied before form controls render.
result: issue
reported: "issue when i click to logout it lag and dont logout for me (i must to reload page) and why it take so long to show action column and attribute permission dont work correct i still can see code (i dont has permission) and modify name (i dont has permission)"
severity: major
screenshot_evidence: |
  - proof-reader logged in can see Code field (197850) on org detail view despite READ=false
  - Edit button visible on org detail view for proof-reader (no Update permission)
  - ROLE_USER permission matrix shows code has no Read/Edit checked yet code displays

issues_diagnosed:
  - id: A
    label: logout-lag
    root_cause: |
      app.topbar.ts:41 calls router.navigate(['']) after logout, which hits the guarded home route.
      canActivate fires UserRouteAccessService → accountService.identity() → GET /api/account (401) →
      catchError null → navigate ['/login'] → LoginComponent.ngOnInit fires identity() again (second 401).
      Two sequential server round-trips before login page renders. Fix: navigate(['/login']) directly.
    file: frontend/src/app/layout/component/topbar/app.topbar.ts:41

  - id: B
    label: action-column-slow
    root_cause: |
      Action column visibility is gated on canUpdate() signal populated by SecuredEntityCapabilityService.
      Service is root-scoped with shareReplay(1) cache. On first load after a login (or cache miss)
      it fires GET /api/... capability request before revealing buttons. No optimistic/skeleton state.
    file: frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts:14-20

  - id: C
    label: attribute-read-permission-not-enforced
    root_cause: |
      Three compounding bugs:
      1. Backend permissive-default: AttributePermissionEvaluatorImpl.checkAttributePermission() returns
         true when no records found (line 53: if perms.isEmpty() return true). The matrix only stores
         GRANT records — unchecking deletes the GRANT but never creates a DENY. So no records = canView=true.
      2. Frontend detail component: organization-detail.component.ts:53-67 loadCapability() reads only
         canUpdate from the capabilities response and discards the entire attributes array. No field-level
         signals exist in the component.
      3. Template has no guards: each field block renders unconditionally — no @if (canViewField('code')).
    files:
      - src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java:53
      - frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts:53-67
      - frontend/src/app/pages/entities/organization/detail/organization-detail.component.html:11-14

  - id: D
    label: edit-button-visible-for-reader
    root_cause: |
      SecuredEntityCapabilityService is providedIn: root with shareReplay(1) cache that is never cleared
      on auth change. If an admin session populated canUpdate=true for organization, then the same Angular
      runtime (SPA navigation or dev hot-reload) reuses that cached observable for proof-reader, causing
      canUpdate() = true and the Edit button to render.
    file: frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts:14-20

## Summary

total: 3
passed: 1
issues: 2
pending: 0
skipped: 0
blocked: 0

## Gaps

- truth: "Attribute read permissions are enforced: fields with READ=false are hidden from detail and list views"
  status: failed
  reason: "User reported: code field (READ=false) visible on org detail view for proof-reader"
  severity: major
  test: 3
  issue_id: C
  root_cause: "Backend permissive-default evaluator + frontend detail component discards capability.attributes + no @if guards on field blocks"
  fix:
    - "Change AttributePermissionEvaluatorImpl to deny-default (return false when no records found)"
    - "In organization-detail.component.ts loadCapability(), build canViewField/canEditField maps from capability.attributes"
    - "Add @if(canViewField('code')), @if(canViewField('name')) etc. guards to each field in the detail template"

- truth: "Entity-level permissions gate action buttons: proof-reader sees no Edit/Delete buttons"
  status: failed
  reason: "User reported: Edit button visible on org detail view while logged in as proof-reader"
  severity: major
  test: 3
  issue_id: D
  root_cause: "Root-scoped SecuredEntityCapabilityService cache never cleared on auth change — stale admin capabilities served to proof-reader"
  fix:
    - "In SecuredEntityCapabilityService, subscribe to AccountService auth events and reset cache on login/logout"

- truth: "Logout completes immediately without requiring page reload"
  status: failed
  reason: "User reported: logout lags and does not complete, must reload page"
  severity: major
  test: 3
  issue_id: A
  root_cause: "app.topbar.ts:41 navigates to [''] (guarded home route) after logout, triggering 2 sequential /api/account 401 round-trips"
  fix:
    - "Change router.navigate(['']) to router.navigate(['/login']) in app.topbar.ts:41"

- truth: "Action column renders promptly without noticeable delay"
  status: failed
  reason: "User reported: action column takes too long to show"
  severity: minor
  test: 3
  issue_id: B
  root_cause: "Action buttons gated on capability fetch completing; no skeleton/loading state while request in-flight"
  fix:
    - "Show skeleton buttons or spinner while capability loads; reveal immediately after response"
