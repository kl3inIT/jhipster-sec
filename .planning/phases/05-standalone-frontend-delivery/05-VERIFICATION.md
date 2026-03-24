---
phase: 05-standalone-frontend-delivery
verified: 2026-03-24T17:27:37Z
status: human_needed
score: 4/4 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 1/4
  gaps_closed:
    - "User can log in from the standalone frontend and the app handles authenticated state, route protection, and expected 401/403/404 flows correctly"
    - "Admin can manage merged roles, permission rules, and row policies from the frontend end to end"
    - "Sample protected-entity screens show only the actions and fields the current user is allowed to access"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "JWT login round trip and token persistence"
    expected: "Valid credentials reach the dashboard, refresh keeps the session, and an expired session redirects back to /login."
    why_human: "Requires a live backend plus real browser storage and session behavior."
  - test: "Admin security-management flow in a browser"
    expected: "An admin can create or update roles and row policies, change matrix permissions through Save Changes plus confirmation, reload the screens, and the saved changes persist."
    why_human: "Needs real frontend-to-backend CRUD round trips and seeded admin data."
  - test: "Protected-entity gating across proof roles"
    expected: "Reader, editor, and none users see different actions and sensitive fields, and denied create or edit routes land on /accessdenied before form controls render."
    why_human: "Visual gating across authenticated user roles cannot be fully proven from static inspection alone."
---

# Phase 5: Standalone Frontend Delivery Verification Report

**Phase Goal:** A standalone Angular frontend exposes the migrated auth and security-management experience and proves protected-entity behavior end to end.
**Verified:** 2026-03-24T17:27:37Z
**Status:** human_needed
**Re-verification:** Yes - after gap closure

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | A standalone Angular app exists under `frontend/` and follows the `aef-main/aef-main` structural direction. | VERIFIED | `frontend/package.json` defines the standalone app and frontend stack (`frontend/package.json:2`, `frontend/package.json:16`, `frontend/package.json:21`, `frontend/package.json:27`, `frontend/package.json:39`), `frontend/angular.json` uses Angular 21 application and unit-test builders (`frontend/angular.json:20`, `frontend/angular.json:82`), and both `cmd /c npx.cmd ng build --configuration=development` and `cmd /c npx.cmd ng test --watch=false` passed on 2026-03-21. |
| 2 | User can log in from the standalone frontend and the app handles authenticated state, route protection, and expected 401/403/404 flows correctly. | VERIFIED | JWT login posts to `/api/authenticate` and stores the token (`frontend/src/app/core/auth/auth-jwt.service.ts:29`-`frontend/src/app/core/auth/auth-jwt.service.ts:43`), the login component refreshes identity and navigates after success (`frontend/src/app/pages/login/login.component.ts:58`-`frontend/src/app/pages/login/login.component.ts:69`), the route guard sends authenticated-but-forbidden users to `/accessdenied` and unauthenticated users to `/login` while storing the requested URL (`frontend/src/app/core/auth/user-route-access.service.ts:12`-`frontend/src/app/core/auth/user-route-access.service.ts:30`), the router exposes `/accessdenied`, `/404`, `/error`, and wildcard-to-404 handling (`frontend/src/app.routes.ts:19`-`frontend/src/app.routes.ts:43`), and navigation-error / 401 expiry handling is wired centrally (`frontend/src/app.config.ts:23`-`frontend/src/app.config.ts:34`, `frontend/src/app/core/interceptor/auth-expired.interceptor.ts:17`-`frontend/src/app/core/interceptor/auth-expired.interceptor.ts:21`). |
| 3 | Admin can manage merged roles, permission rules, and row policies from the frontend end to end. | VERIFIED | Roles CRUD is wired through the role dialog and list (`frontend/src/app/pages/admin/security/roles/dialog/role-dialog.component.ts:30`-`frontend/src/app/pages/admin/security/roles/dialog/role-dialog.component.ts:74`, `frontend/src/app/pages/admin/security/roles/list/role-list.component.ts:40`-`frontend/src/app/pages/admin/security/roles/list/role-list.component.ts:77`), row policies load catalog entries and limit policy types to `SPECIFICATION` and `JPQL` (`frontend/src/app/pages/admin/security/row-policies/dialog/row-policy-dialog.component.ts:40`-`frontend/src/app/pages/admin/security/row-policies/dialog/row-policy-dialog.component.ts:105`), the shared services point to `/api/admin/sec/catalog` and `/api/admin/sec/permissions` with `authorityName` filtering (`frontend/src/app/pages/admin/security/shared/service/sec-catalog.service.ts:10`-`frontend/src/app/pages/admin/security/shared/service/sec-catalog.service.ts:15`, `frontend/src/app/pages/admin/security/shared/service/sec-permission.service.ts:10`-`frontend/src/app/pages/admin/security/shared/service/sec-permission.service.ts:27`), the permission matrix now buffers unsaved changes locally, exposes explicit save and discard controls, confirms before flush, and still emits the locked `GRANT` UI contract (`frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.html:17`-`frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.html:40`, `frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts:83`-`frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts:297`), and the backend normalizes that UI contract to stored/runtime `ALLOW` and round-trips it back as `GRANT` (`src/main/java/com/vn/core/service/security/SecPermissionUiContractService.java:32`-`src/main/java/com/vn/core/service/security/SecPermissionUiContractService.java:105`, `src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java:84`-`src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java:149`, `src/test/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResourceIT.java:129`-`src/test/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResourceIT.java:192`, `src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java:79`-`src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java:111`). |
| 4 | Sample protected-entity screens show only the actions and fields the current user is allowed to access. | VERIFIED | Current-user capabilities are exposed at `/api/security/entity-capabilities` and computed from entity plus attribute evaluators (`src/main/java/com/vn/core/web/rest/SecuredEntityCapabilityResource.java:17`-`src/main/java/com/vn/core/web/rest/SecuredEntityCapabilityResource.java:39`, `src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java:39`-`src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java:70`, `src/test/java/com/vn/core/web/rest/SecuredEntityCapabilityResourceIT.java:37`-`src/test/java/com/vn/core/web/rest/SecuredEntityCapabilityResourceIT.java:87`), the frontend caches one shared capability response (`frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts:16`-`frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts:25`), list templates gate `New`, `View`, `Edit`, and `Delete` actions from capability state (`frontend/src/app/pages/entities/organization/list/organization-list.component.ts:36`-`frontend/src/app/pages/entities/organization/list/organization-list.component.ts:49`, `frontend/src/app/pages/entities/organization/list/organization-list.component.ts:193`-`frontend/src/app/pages/entities/organization/list/organization-list.component.ts:196`, `frontend/src/app/pages/entities/organization/list/organization-list.component.html:10`-`frontend/src/app/pages/entities/organization/list/organization-list.component.html:82`; same pattern verified in department and employee list files), detail screens gate `Edit` on resolved update capability (`frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts:19`-`frontend/src/app/pages/entities/organization/detail/organization-detail.component.ts:67`, `frontend/src/app/pages/entities/organization/detail/organization-detail.component.html:31`-`frontend/src/app/pages/entities/organization/detail/organization-detail.component.html:35`; same pattern verified in department and employee detail files), and update screens hide the form until capability resolves, redirect denied routes to `/accessdenied`, and omit budget or salary from both rendering and payloads unless `canEdit` is true (`frontend/src/app/pages/entities/organization/update/organization-update.component.ts:43`-`frontend/src/app/pages/entities/organization/update/organization-update.component.ts:137`, `frontend/src/app/pages/entities/organization/update/organization-update.component.html:12`-`frontend/src/app/pages/entities/organization/update/organization-update.component.html:80`, `frontend/src/app/pages/entities/employee/update/employee-update.component.ts:57`-`frontend/src/app/pages/entities/employee/update/employee-update.component.ts:177`, `frontend/src/app/pages/entities/employee/update/employee-update.component.html:12`-`frontend/src/app/pages/entities/employee/update/employee-update.component.html:97`). Frontend specs lock the organization list/detail/update and employee update gating behavior (`frontend/src/app/pages/entities/organization/list/organization-list.component.spec.ts:79`-`frontend/src/app/pages/entities/organization/list/organization-list.component.spec.ts:108`, `frontend/src/app/pages/entities/organization/detail/organization-detail.component.spec.ts:55`-`frontend/src/app/pages/entities/organization/detail/organization-detail.component.spec.ts:75`, `frontend/src/app/pages/entities/organization/update/organization-update.component.spec.ts:63`-`frontend/src/app/pages/entities/organization/update/organization-update.component.spec.ts:82`, `frontend/src/app/pages/entities/employee/update/employee-update.component.spec.ts:71`-`frontend/src/app/pages/entities/employee/update/employee-update.component.spec.ts:103`). |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `frontend/package.json` | Standalone Angular app definition | VERIFIED | Declares Angular, PrimeNG, ngx-translate, and Tailwind dependencies. |
| `frontend/angular.json` | Build and test configuration for the standalone app | VERIFIED | Uses `@angular/build:application` and `@angular/build:unit-test`. |
| `frontend/src/app.config.ts` | Bootstrap with router, navigation-error handling, and HTTP interceptors | VERIFIED | Registers router features plus `httpInterceptorProviders`. |
| `frontend/src/app.routes.ts` | Authenticated routes plus explicit error pages and wildcard 404 | VERIFIED | `**` now redirects to `404`; admin/security remains authority-gated. |
| `frontend/src/app/core/auth/auth-jwt.service.ts` | JWT login against the existing backend auth API | VERIFIED | Uses `ApplicationConfigService.getEndpointFor('api/authenticate')` and stores the JWT token. |
| `frontend/src/app/core/auth/user-route-access.service.ts` | Route protection and access-denied handling | VERIFIED | Guards routes, stores attempted URL, and redirects to `/login` or `/accessdenied` as appropriate. |
| `frontend/src/app/layout/component/menu/app.menu.ts` | Sidebar with Entities and admin-only Security Admin section | VERIFIED | `Security Admin` is only added when the account has `ROLE_ADMIN`. |
| `frontend/src/app/pages/admin/security/roles/dialog/role-dialog.component.ts` | Role CRUD dialog with exact enum values | VERIFIED | Emits `RESOURCE` and `ROW_LEVEL` only. |
| `frontend/src/app/pages/admin/security/row-policies/dialog/row-policy-dialog.component.ts` | Row-policy CRUD dialog with catalog-backed entity selector | VERIFIED | Loads entities from `SecCatalogService` and restricts policy types to `SPECIFICATION` and `JPQL`. |
| `frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts` | Two-panel matrix editor using the locked UI contract | VERIFIED | Uses role-route params, filtered permission queries, buffered pending state, explicit save confirmation, and `GRANT` payloads. |
| `src/main/java/com/vn/core/service/security/SecPermissionUiContractService.java` | UI-to-runtime permission normalization | VERIFIED | Translates lower-case UI targets plus `GRANT` into stored upper-case targets plus `ALLOW`, and reverses the translation on reads. |
| `src/main/java/com/vn/core/web/rest/SecuredEntityCapabilityResource.java` | Authenticated capability endpoint | VERIFIED | Exposes `/api/security/entity-capabilities` behind `isAuthenticated()`. |
| `frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts` | Cached capability client for secured entity screens | VERIFIED | Uses `shareReplay(1)` and resolves entity-specific capability lookups. |
| `frontend/src/app/pages/entities/organization/list/organization-list.component.html` | Capability-gated list actions | VERIFIED | `New`, `View`, `Edit`, and `Delete` buttons render only when the capability says they should. |
| `frontend/src/app/pages/entities/organization/detail/organization-detail.component.html` | Capability-gated detail action | VERIFIED | `Edit` renders only when `capabilityLoaded()` and `canUpdate()` are both true. |
| `frontend/src/app/pages/entities/organization/update/organization-update.component.ts` | Capability-gated route entry and budget payload shaping | VERIFIED | Denied routes redirect to `/accessdenied`, the form waits on capability, and `budget` only persists when allowed. |
| `frontend/src/app/pages/entities/employee/update/employee-update.component.ts` | Capability-gated route entry and salary payload shaping | VERIFIED | Mirrors the organization pattern for `salary`. |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `frontend/src/app.config.ts` | `frontend/src/app/core/interceptor/index.ts` | `httpInterceptorProviders` | WIRED | Bootstrap registers the interceptor provider array directly. |
| `frontend/src/app.routes.ts` | `frontend/src/app/core/auth/user-route-access.service.ts` | `canActivate` | WIRED | Home, entity, and admin/security routes all use the guard. |
| `frontend/src/app/pages/login/login.component.ts` | `frontend/src/app/core/auth/auth-jwt.service.ts` | `login()` call | WIRED | Login submits credentials through `AuthServerProvider.login()` and refreshes identity after success. |
| `frontend/src/app.routes.ts` | `frontend/src/app/pages/error/not-found.component.ts` | wildcard route | WIRED | Unknown URLs now redirect to `404`, closing the prior gap. |
| `frontend/src/app/layout/component/menu/app.menu.ts` | `/admin/security/*` routes | admin-only menu generation | WIRED | `Security Admin` entries are only added when `hasAnyAuthority(Authority.ADMIN)` is true. |
| `frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts` | `frontend/src/app/pages/admin/security/shared/service/sec-permission.service.ts` | filtered query plus confirmed batch create/delete flush | WIRED | The matrix loads role-scoped permissions, buffers checkbox edits in `pendingChanges`, and flushes create/delete requests only after `confirmSave()` accepts. |
| `src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java` | `src/main/java/com/vn/core/service/security/SecPermissionUiContractService.java` | incoming and outgoing DTO normalization | WIRED | Create, list, get, and update paths all normalize the UI contract. |
| `src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java` | `/api/admin/sec/permissions` -> protected entity authorization | matrix-style permission exercised through `/api/organizations` | WIRED | The test encodes a `GRANT` permission create and asserts the protected entity outcome flips from `403` to `200`. |
| `src/main/java/com/vn/core/web/rest/SecuredEntityCapabilityResource.java` | `src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java` | `GET /api/security/entity-capabilities` | WIRED | The resource delegates directly to the capability service behind `isAuthenticated()`. |
| `frontend/src/app/pages/entities/*` | `frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts` | capability lookups for list, detail, and update gating | WIRED | Organization, department, and employee screens all call `getEntityCapability('<code>')` before rendering gated actions or forms. |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| `AUTH-01` | `05-02-PLAN.md` | User can log in from the standalone `frontend/` app using the existing JWT authentication flow | NEEDS HUMAN | Login, token storage, attempted-URL restore, and 401 redirect behavior are wired in code, but a real backend/browser round trip is still required. |
| `ENT-03` | `05-03-PLAN.md`, `05-07-PLAN.md`, `05-08-PLAN.md`, `05-09-PLAN.md` | Sample entity screens in `frontend/` reflect allowed and denied actions and field visibility correctly | NEEDS HUMAN | Capability endpoint, client cache, gated templates, payload omission, and frontend specs are present; real cross-role browser confirmation is still required. |
| `UI-01` | `05-02-PLAN.md` | A standalone Angular app exists under `frontend/` and follows the `aef-main/aef-main` structure direction | SATISFIED | The app exists, builds, and tests successfully. |
| `UI-02` | `05-01-PLAN.md`, `05-04-PLAN.md`, `05-05-PLAN.md`, `05-06-PLAN.md` | The frontend provides end-to-end role, permission, and row-policy management screens | NEEDS HUMAN | CRUD screens, matrix normalization, and targeted backend integration tests are in place, but a live browser-based admin CRUD round trip is still required. |
| `UI-03` | `05-02-PLAN.md`, `05-06-PLAN.md` | The frontend handles authentication state, route protection, and expected 401/403/404 flows correctly | NEEDS HUMAN | Guarding, wildcard 404, navigation-error handling, and auth-expired redirects are wired; a live browser session is still needed to confirm the actual flow. |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| None | - | No TODO, placeholder, empty-return, or console-log stub patterns found in the verified phase files | - | No code-level blocker found during the anti-pattern scan |

### Human Verification Required

### 1. JWT Login Round Trip

**Test:** Start the backend, open `/login`, sign in with a valid account, refresh the page, and revisit a protected route.
**Expected:** Login succeeds, the dashboard loads, refresh preserves the session, and an expired session redirects to `/login`.
**Why human:** Requires live backend auth plus browser storage behavior.

### 2. Admin Security-Management Flow

**Test:** As an admin, open Roles, Row Policies, and the permission matrix; create or edit records; reload the pages; then use a proof user whose role was changed.
**Expected:** CRUD changes persist, the matrix shows `Save Changes` plus a confirmation dialog before permission writes, still speaks `GRANT` at the UI boundary, and protected-entity behavior changes accordingly for the affected user.
**Why human:** Requires real API persistence and cross-user behavior.

### 3. Protected-Entity Capability Gating

**Test:** Log in as proof reader, proof editor, and proof none users; visit organization, department, and employee list, detail, and edit routes.
**Expected:** Each role sees only the allowed actions; denied create or edit routes redirect to `/accessdenied`; budget and salary stay hidden unless explicit edit capability is granted.
**Why human:** Requires visual confirmation across real authenticated roles.

### Gaps Summary

The prior code gaps are closed. Unknown routes now land on the 404 page, the permission matrix now buffers edits behind an explicit confirmed save while keeping the locked frontend payload shape, and the protected-entity screens consume shared capability data to gate actions, edit buttons, route entry, and sensitive fields.

No new code-level blockers were found in this re-verification. Remaining work is live-environment confirmation only. The frontend build and Vitest suite both passed in this session. Targeted backend integration tests also passed in this session, including `SecPermissionAdminResourceIT`, `SecuredEntityEnforcementIT`, and `SecuredEntityCapabilityResourceIT`; the remaining gap is browser-driven validation of the live UX and role-specific behavior.

---

_Verified: 2026-03-24T17:27:37Z_
_Verifier: Claude (gsd-verifier)_
