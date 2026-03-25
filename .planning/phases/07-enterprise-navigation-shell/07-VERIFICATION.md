---
phase: 07-enterprise-navigation-shell
verified: 2026-03-25T00:00:00Z
status: passed
score: 12/12 must-haves verified
re_verification: false
---

# Phase 7: Enterprise Navigation Shell — Verification Report

**Phase Goal:** Navigation, route protection, and enterprise shell behavior are driven by backend-aware contracts rather than hardcoded client assumptions.
**Verified:** 2026-03-25
**Status:** passed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | GET /api/security/navigation-grants returns only the current user's allowed shell leaf ids with deny-wins behavior | VERIFIED | `NavigationGrantResource.java` L36-43 + `CurrentUserNavigationGrantService.java` L28-48 with ALLOW/DENY filter chain |
| 2 | Navigation grants are stored separately from sec_permission, keyed by authority_name, app_name, node_id | VERIFIED | `SecNavigationGrant.java` entity + `20260325000100_create_sec_navigation_grant.xml` with unique constraint on (authority_name, app_name, node_id); `TargetType.java` has no NAVIGATION constant |
| 3 | The frontend owns the shell tree; only visibility is backend-driven | VERIFIED | `navigation-registry.ts` defines all sections/leaves as frontend constants; `navigation.service.ts` fetches only allowed IDs from backend |
| 4 | Navigation service caches allowed leaf ids per user and clears on auth-state change | VERIFIED | `navigation.service.ts` L183-190 clearCache + sessionStorage; auth state subscription L33-54 |
| 5 | Route reachability and menu visibility come from the same navigation-node metadata and grant service | VERIFIED | `app.menu.ts` calls `navigationService.visibleTree()` L39; `user-route-access.service.ts` calls `navigationService.isNodeVisible()` L27 |
| 6 | Admin and entity areas remain behind distinct lazy boundaries | VERIFIED | `app.routes.ts` L44 `loadChildren` for entities, L49 `loadChildren` for admin; `app.routes.spec.ts` L32-33 asserts both are `function` type |
| 7 | All shell routes expose stable navigation-node metadata without hardcoded ROLE_ADMIN on admin boundary | VERIFIED | `app.routes.ts` has no `data: { authorities: ['ROLE_ADMIN'] }`; all entity and admin routes carry `navigationNodeId` |
| 8 | Breadcrumbs render from route metadata inside authenticated shell | VERIFIED | `breadcrumb.service.ts` builds trail from `navigationNodeId` + `breadcrumbKey` in route data; `app.layout.ts` L38 renders strip conditionally |
| 9 | Access-denied page names blocked destination and offers safe recovery CTA | VERIFIED | `access-denied.component.ts` reads `blockedLabelKey` from history state; calls `resolveFallbackRoute`; HTML renders "Go to {destination}" |
| 10 | Entity list routes with granted navigation leaf but missing entity READ render an in-shell denied state without firing data query | VERIFIED | `organization-list.component.ts` L97-103: `showListDeniedState()` guard short-circuits before HTTP; same pattern in department and employee lists |
| 11 | Detail and edit flows restore preserved list context | VERIFIED | `workspace-context.service.ts` stores/retrieves context by `navigationNodeId`; org list stores on create/view/edit L229-238; org detail restores L63 |
| 12 | Phase 7 navigation behavior covered by unit/integration tests and browser-level Playwright checks | VERIFIED | All spec files exist with substantive assertions; e2e suite covers hidden leaves, blocked destination, in-shell denied state, breadcrumb alignment |

**Score:** 12/12 truths verified

---

## Required Artifacts

### Plan 01 — Backend Navigation Grant Contract

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/vn/core/service/security/CurrentUserNavigationGrantService.java` | Current-user app-scoped grant resolution | VERIFIED | 50 lines; ALLOW/DENY logic present; `getAllowedNodeIds` implemented |
| `src/main/java/com/vn/core/web/rest/NavigationGrantResource.java` | Authenticated REST contract for menu and route reachability | VERIFIED | `@GetMapping("/navigation-grants")`, `@RequestParam("appName")`, `@PreAuthorize("isAuthenticated()")` |
| `src/main/resources/config/liquibase/changelog/20260325000100_create_sec_navigation_grant.xml` | Liquibase schema for app-scoped navigation grants | VERIFIED | `sec_navigation_grant` table, unique constraint, two indexes, FK to jhi_authority |

### Plan 02 — Frontend Navigation Registry and Service

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `frontend/src/app/layout/navigation/navigation-registry.ts` | Canonical shell tree and stable node ids | VERIFIED | All 7 leaf ids present; 3 root sections; routePrefix values match spec |
| `frontend/src/app/layout/navigation/navigation.service.ts` | Current-user visibility filtering, cache, fallback resolution | VERIFIED | 215 lines; `visibleTree`, `isNodeVisible`, `resolveFallbackRoute`, sessionStorage cache, auth-reset subscription |
| `frontend/src/app/layout/navigation/navigation.constants.ts` | SHELL_APP_NAME and storage key constants | VERIFIED | `SHELL_APP_NAME = 'jhipster-security-platform'` |
| `frontend/src/app/layout/navigation/navigation.model.ts` | Navigation node types with `deniedMode` | VERIFIED | `NavigationDeniedMode`, `AppNavigationLeaf`, `AppNavigationSection` types defined |

### Plan 03 — Route Metadata and Guard/Menu Integration

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `frontend/src/app/core/auth/user-route-access.service.ts` | Navigation-aware route gate with blocked-destination redirect | VERIFIED | `isNodeVisible` call, `blockedLabelKey`, `sectionId` in navigation state |
| `frontend/src/app/layout/component/menu/app.menu.ts` | Menu rendering from filtered navigation registry | VERIFIED | No Authority import; `navigationService.visibleTree()` drives model; stable `path:` and `id:` on each item |
| `frontend/src/app.routes.ts` | Route metadata contract for shell navigation | VERIFIED | No `ROLE_ADMIN` hardcode; both `entities` and `admin` use `loadChildren`; `navigationNodeId` metadata on home route |

### Plan 04 — Enterprise Shell Behavior

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `frontend/src/app/layout/navigation/breadcrumb.service.ts` | Shell breadcrumb generation from route metadata | VERIFIED | 97 lines; builds trail from `navigationNodeId`, `breadcrumbKey`, `pageTitleKey`; supports list/detail/edit/permissions routes |
| `frontend/src/app/pages/error/access-denied.component.ts` | Blocked destination messaging and fallback CTA | VERIFIED | `blockedLabelKey` signal from history state; `resolveFallbackRoute` subscribed; CTA renders "Go to {destination}" |
| `frontend/src/app/pages/entities/shared/service/workspace-context.service.ts` | List context preservation across list-detail-edit navigation | VERIFIED | 45 lines; stores/retrieves `WorkspaceContext` by `navigationNodeId` |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `NavigationGrantResource.java` | `CurrentUserNavigationGrantService.java` | `getAllowedNodeIds(` | WIRED | Direct call at L41 |
| `CurrentUserNavigationGrantService.java` | `SecNavigationGrantRepository.java` | `findAllByAppNameAndAuthorityNameIn(` | WIRED | L34 |
| `navigation.service.ts` | `navigation-registry.ts` | `APP_NAVIGATION_TREE` | WIRED | L9, used in visibleTree L87 and other methods |
| `navigation.service.ts` | `api/security/navigation-grants` | `navigation-grants` | WIRED | L24 via `getEndpointFor`; `appName` param set L65 |
| `app.menu.ts` | `navigation.service.ts` | `visibleTree` | WIRED | L39 `switchMap(() => this.navigationService.visibleTree())` |
| `user-route-access.service.ts` | `navigation.service.ts` | `isNodeVisible` | WIRED | L27 `navigationService.isNodeVisible(navigationNodeId)` |
| `app.layout.ts` | `breadcrumb.service.ts` | `breadcrumb` | WIRED | L75 `inject(BreadcrumbService).items` |
| `access-denied.component.ts` | `navigation.service.ts` | `resolveFallbackRoute` | WIRED | L39 `this.navigationService.resolveFallbackRoute(sectionId)` |
| Liquibase master | `20260325000100_create_sec_navigation_grant.xml` | `include` | WIRED | master.xml L22 confirms include |

---

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `app.menu.ts` | `model` (visible tree) | `NavigationService.visibleTree()` → HTTP GET `/api/security/navigation-grants` → `SecNavigationGrantRepository.findAllByAppNameAndAuthorityNameIn` | Yes — DB query against sec_navigation_grant table | FLOWING |
| `organization-list.component.ts` | `organizations` signal | `OrganizationService.query()` (only when `canRead === true`) | Yes — guarded by capability check before HTTP call | FLOWING |
| `access-denied.component.ts` | `fallbackRoute` signal | `NavigationService.resolveFallbackRoute()` → `allowedNodeIds()` → HTTP grant response | Yes — resolves from backend-driven allowed node ids | FLOWING |

---

## Behavioral Spot-Checks

Step 7b: SKIPPED for live server tests (no running server). Static analysis confirms:

| Behavior | Verification Method | Status |
|----------|---------------------|--------|
| `GET /api/security/navigation-grants?appName=X` returns `appName` + `allowedNodeIds` | Code read of `NavigationGrantResource.java` L39-41 + `NavigationGrantResponseDTO` fields | PASS |
| Menu driven from `visibleTree()` not hardcoded authorities | `app.menu.ts` has no Authority import; calls `visibleTree()` directly | PASS |
| Denied route redirects to `/accessdenied` with state metadata | `user-route-access.service.ts` L33-39 passes `blockedUrl`, `blockedLabelKey`, `sectionId` | PASS |
| Entity list denied state without data fetch | `organization-list.component.ts` L97-103 returns before `queryBackend()` call | PASS |
| Breadcrumbs render only when items present | `app.layout.ts` template `@if (breadcrumbs().length > 0)` L38 | PASS |

---

## Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| ROUTE-01 | 01, 02, 05 | Menu and navigation loaded from backend-driven data, not hardcoded client menu | SATISFIED | NavigationGrantResource + NavigationService + app.menu.ts |
| ROUTE-02 | 01, 03, 04, 05 | Routes and nav entries denied before render when user lacks backend-provided permission | SATISFIED | UserRouteAccessService.isNodeVisible gate + /accessdenied redirect |
| ROUTE-03 | 03, 05 | Admin and entity areas behind lazy-loaded route boundaries | SATISFIED | app.routes.ts uses loadChildren for both; spec asserts typeof === function |
| UI-04 | 04, 05 | Enterprise shell with list-first patterns, breadcrumbs, denied states, context preservation | SATISFIED | BreadcrumbService + AccessDeniedComponent + WorkspaceContextService + in-shell denied cards |

All four requirements mapped to Phase 7 in REQUIREMENTS.md traceability table are satisfied. No orphaned requirements found.

---

## Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `frontend/src/app/layout/component/menu/app.menu.ts` L16 | `standalone: true` in decorator (CLAUDE.md: Must NOT set in Angular v20+) | Info | Convention violation; 28 occurrences project-wide, predates Phase 7 work |
| `frontend/src/app/pages/error/access-denied.component.ts` L18 | `standalone: true` in decorator | Info | Same as above; project-wide pattern |
| `frontend/src/app/pages/admin/user-management/user-management.routes.ts` | Routes load `user-management-placeholder.component` | Info | Expected per roadmap; user management is Phase 8 (UMGT-01/02/03); placeholder is intentional, not a Phase 7 stub |

No blockers or warnings. The `standalone: true` occurrences are a pre-existing project-wide convention deviation not introduced by Phase 7. The user-management placeholder is deliberately scoped to Phase 8.

---

## Human Verification Required

### 1. Backend Grant Deny-Wins at Integration Level

**Test:** Start the backend with a test user having two authorities where one ALLOW and one DENY for the same node_id exist in sec_navigation_grant; call `GET /api/security/navigation-grants?appName=jhipster-security-platform`.
**Expected:** The denied node_id is absent from `allowedNodeIds`.
**Why human:** Unit test covers this, but integration test data seed requires a running database.

### 2. Menu Hides Leaves After Backend Returns Restricted Grant

**Test:** Log in as a user whose navigation grants exclude `entities.employee`; observe the sidebar menu.
**Expected:** The "Entities" section is visible but the Employee leaf is absent. The Organizations and Department leaves appear if granted.
**Why human:** Requires a live frontend + backend stack with seeded grant data.

### 3. Access-Denied Recovery CTA Label Matches Resolved Fallback

**Test:** Navigate directly to a route the user cannot reach (e.g., `/admin/security/roles` when `security.roles` is not in their grants); observe the access-denied page.
**Expected:** CTA button reads "Go to {leaf label of first allowed route}" and navigates correctly when clicked.
**Why human:** Requires live routing to confirm history.state is populated by the guard before the component initializes.

### 4. Breadcrumb and Sidebar Alignment on Deep Routes

**Test:** Navigate to `/admin/security/roles/ROLE_ADMIN/permissions`; observe breadcrumb strip and sidebar highlight.
**Expected:** Breadcrumb shows "Security Admin > Security roles > Permission matrix". Sidebar highlights the Security roles leaf.
**Why human:** Requires live DOM inspection; Playwright suite asserts these but tests require running Playwright against a live app.

---

## Gaps Summary

No gaps. All twelve observable truths pass at all four verification levels (exists, substantive, wired, data-flowing). The four requirements — ROUTE-01, ROUTE-02, ROUTE-03, UI-04 — are fully covered by implementation evidence in the codebase. Human verification items are confirmations of live behavior, not blockers.

---

_Verified: 2026-03-25_
_Verifier: Claude (gsd-verifier)_
