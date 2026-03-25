# Phase 7: Enterprise Navigation Shell - Research

**Researched:** 2026-03-25
**Status:** Ready for planning

## Executive Summary

Phase 7 should not treat backend-driven navigation as "the backend sends the menu tree." The locked Phase 7 decision is the opposite: the frontend remains the canonical owner of the shell tree, labels, icons, ordering, and stable node ids, while the backend returns only the allowed stable leaf ids for the current user and app.

The clean architecture is:

1. A frontend-owned navigation registry that defines every shell node once.
2. A backend current-user navigation-grants endpoint that is app-scoped and returns allowed node ids.
3. A frontend navigation service that caches those grants, filters the registry, and feeds both the menu and the route guard.
4. Route metadata that makes navigation, breadcrumbs, section highlighting, and denied-mode decisions come from one source of truth.

The main technical choice is whether to extend the existing permission system for menu grants or create a dedicated navigation-grant model. The dedicated model is the safer Phase 7 recommendation because Phase 7 requires frontend-owned stable ids and multi-app `app_name`, while the current permission system is optimized for entity and attribute enforcement and already backs the admin permission matrix UI.

## What Exists Today

### Frontend

- `frontend/src/app/layout/component/menu/app.menu.ts` builds a hardcoded menu tree in the component.
- `frontend/src/app/core/auth/user-route-access.service.ts` only checks authentication plus static `route.data.authorities`.
- `frontend/src/app.routes.ts` still hardcodes `ROLE_ADMIN` on the `admin` lazy boundary.
- Entity routes already resolve capability payloads through `SecuredEntityCapabilityService`.
- `SecuredEntityCapabilityService` already uses a good cache pattern for current-user shell data: sessionStorage warm start plus in-memory reuse, cleared on auth-state change.
- `LayoutService.layoutState.activePath` already gives the shell a place to keep active section state, but current menu items do not carry a strong route metadata model.
- Entity route titles are still literal strings (`'Organizations'`, `'Edit Organization'`) instead of translation keys, which will fight the new breadcrumb and shell-title contract if left unchanged.

### Backend

- `SecuredEntityCapabilityResource` exposes current-user entity and attribute capabilities at `/api/security/entity-capabilities`.
- `SecCatalogAdminResource` exposes admin-only secured-entity catalog metadata at `/api/admin/sec/catalog`.
- `SecPermissionUiContractService` already proves there is a translation layer between UI-facing target ids and stored runtime permission targets.
- `SecPermission` and `TargetType` are currently centered on entity, attribute, and row-policy enforcement. There is no app-scoped navigation model and no `app_name` field in the current permission schema.

### Tests Already in Place

- `frontend/src/app/layout/component/menu/app.menu.spec.ts` already locks stable menu ids and language-switch behavior.
- `frontend/src/app.routes.spec.ts` already checks lazy route structure.
- `frontend/e2e/security-comprehensive.spec.ts` already covers `/accessdenied`, direct deep-link denial, and capability-cache invalidation.

This means Phase 7 is building on a real shell foundation, not starting from zero.

## Recommended Architecture

## 1. Frontend-Owned Navigation Registry

Create a dedicated frontend registry for the shell tree instead of continuing to embed it inside `AppMenu`.

Recommended shape:

- one registry file for stable shell sections and leaves
- each leaf defines:
  - stable node id
  - section id
  - translation key
  - icon
  - router link
  - active-path matcher or canonical route prefix
  - denied mode (`route` or `in-shell`)

Why this matters:

- It preserves D-01, D-02, and D-05 from `07-CONTEXT.md`.
- The menu, breadcrumb service, access-denied copy, and route guard can all consume the same metadata.
- It keeps lazy route boundaries untouched because the registry only stores path metadata, not route-module imports.

Recommended stable ids from `07-UI-SPEC.md`:

- `home.dashboard`
- `entities.organization`
- `entities.department`
- `entities.employee`
- `security.users`
- `security.roles`
- `security.row-policies`

## 2. Dedicated Navigation-Grant Backend Model

Do **not** overload `SecPermission` for Phase 7 menu grants.

Recommendation:

- create a separate navigation-grant persistence model and service, keyed by:
  - `authority_name`
  - `app_name`
  - `node_id`
  - optional `effect` if deny-wins semantics are needed

Why a separate model is better than extending `SecPermission`:

- current permission semantics are runtime security semantics for ENTITY and ATTRIBUTE enforcement
- current admin permission-matrix UI already assumes entity/attribute-style targets and normalization
- Phase 7 navigation ids are intentionally frontend-owned, language-independent shell ids, not domain-runtime targets
- D-03 requires app scoping from day one, which the existing `sec_permission` model does not provide
- Phase 7 explicitly defers a menu-permission management UI, so a new dedicated read model avoids dragging the current permission UI into a half-complete contract

Recommended endpoint:

- `GET /api/security/navigation-grants?appName={app}`

Recommended response:

- `appName`
- `allowedNodeIds: string[]`

Optional additions:

- `defaultNodeId` or `defaultRoute`

Those are optional because the frontend can derive a safe fallback from the first allowed leaf in the registry order.

## 3. Shared Frontend Navigation Service

Add a current-user navigation service beside `SecuredEntityCapabilityService`.

Recommended behavior:

- fetch allowed node ids from the backend
- cache per authenticated user in memory plus sessionStorage
- clear cache on auth-state changes, mirroring `SecuredEntityCapabilityService`
- expose:
  - visible menu tree
  - `isNodeVisible(nodeId)`
  - safe fallback route selection
  - current denied destination metadata for `/accessdenied`

Use a single configured app identifier, not scattered string literals. The best fit is a dedicated frontend config constant or environment-backed value consumed by the navigation service.

## 4. Route Metadata As The Shell Contract

Phase 7 should stop relying on ad hoc route strings and static `authorities` checks.

Recommended route-data additions:

- `navigationNodeId`
- `sectionId`
- `breadcrumbKey`
- `pageTitleKey`
- `deniedMode`

Recommended guard flow:

1. If unauthenticated: store URL and redirect to `/login`.
2. If authenticated but `navigationNodeId` is not granted: redirect to `/accessdenied` before component render.
3. If granted and the route is an entity list route with `deniedMode: in-shell`, allow route activation and let the component render the in-shell denied state when `canRead` is false.
4. For create/edit routes, keep route-level hard denial when capability blocks create or update.

This gives one consistent decision system:

- backend navigation grants decide shell visibility and route reachability
- entity capability decides whether a granted entity workspace shows real data or an in-shell denied state

That split matches D-14 through D-16 exactly.

## 5. Breadcrumb And Active-Section System

Phase 7 should build breadcrumbs from route metadata, not from brittle URL parsing.

Recommended approach:

- derive breadcrumb trail from the activated route tree plus the navigation registry
- use translation keys, not literal page titles
- keep section highlighting driven by route metadata or registry prefixes rather than by translated labels

Important current-state note:

- `app.menu.spec.ts` already protects stable ids against language switching
- extending that stable-id principle into breadcrumb and section state is the lowest-risk Phase 7 path

## 6. Preserve Existing Lazy Boundaries

Phase 7 does **not** need to redesign the route tree to satisfy ROUTE-03.

Why:

- `frontend/src/app.routes.ts` already lazy-loads `entities` and `admin`
- `frontend/src/app/pages/admin/admin.routes.ts` already splits `security` and `users`
- `frontend/src/app/pages/entities/entity.routes.ts` already lazy-loads entity route groups

The planning focus should be:

- remove coarse hardcoded authority assumptions
- preserve or refine the lazy boundaries
- add tests that ensure menu filtering and route decisions do not force eager imports

## Key Implementation Implications For Planning

## Backend Work

Planner should expect backend tasks for:

- Liquibase schema for navigation grants
- entity, repository, and service for current-user app-scoped grant lookup
- REST endpoint for current-user allowed node ids
- tests proving:
  - app scoping works
  - current-user authority merge works
  - hidden section behavior can be derived correctly from returned leaf ids

The planner should **not** combine this with a menu-permission admin UI. That belongs to a later phase.

## Frontend Shell Work

Planner should expect frontend tasks for:

- extracting the hardcoded menu into a registry plus navigation service
- filtering the registry with backend grants
- replacing static admin visibility assumptions in `AppMenu`
- route metadata upgrade for navigation, titles, and breadcrumbs
- breadcrumb strip added to `AppLayout`
- active section highlight driven from route identity, not label text

## Denial Behavior Work

Planner should separate two denial tracks:

- hard route denial to `/accessdenied` with blocked destination copy and safe recovery action
- in-shell entity denial state for granted entity list routes with missing `READ`

That split should be explicit in the plans so execution does not collapse everything back into a single redirect pattern.

## Route-Title And Translation Cleanup

Planner should include a route metadata normalization step:

- replace literal entity route titles with translation keys
- make breadcrumb labels and page titles come from the same canonical route metadata

Without this, Phase 7 will build a breadcrumb system on top of mixed literal strings and translation keys.

## Risks And Pitfalls

## 1. Overloading `SecPermission`

Biggest design risk.

Why it is risky:

- leaks frontend shell ids into runtime permission storage
- pressures `TargetType` to grow for UI concerns
- complicates the current permission-matrix UI contract
- makes `app_name` retrofitting invasive in brownfield security code

Recommendation: keep navigation grants separate.

## 2. Using Entity `READ` As Menu Visibility

This would violate a locked Phase 7 decision.

Why it fails:

- users could lose menu visibility for routes that should remain visible but data-denied
- admin and entity visibility rules are intentionally different in Phase 7

Recommendation: route visibility comes from navigation grants, not from `canRead`.

## 3. Letting The Menu Component Own Business Rules

If the filtering logic stays inside `AppMenu`, route guard and breadcrumb behavior will drift.

Recommendation: centralize visibility and fallback logic in a navigation service, not in the menu component.

## 4. Regressing Existing Route Guards

Current update routes already redirect before form render when capability denies create or update. Phase 7 should not weaken that.

Recommendation: preserve existing create/edit hard denial and add the new navigation-grant layer ahead of it.

## 5. Breaking Active State On Language Change

Current tests already caught this class of bug for menu ids.

Recommendation: active state must be based on stable ids and route paths, never translated labels.

## Validation Architecture

Phase 7 should validate navigation and shell behavior at three levels.

### Backend

- service tests for current-user navigation grant resolution by `app_name`
- resource tests for `/api/security/navigation-grants`
- deny-wins and multi-authority merge tests if `effect` is supported

### Frontend Unit/Component

- navigation service cache and auth-reset tests
- menu filtering tests using backend-provided allowed node ids
- route access decision tests for:
  - unauthenticated -> `/login`
  - unreachable leaf -> `/accessdenied`
  - granted entity list + no `READ` -> in-shell denied state
- breadcrumb generation tests from route metadata
- `/accessdenied` component tests for blocked destination copy and safe-action behavior

### End-To-End

Extend the existing Playwright suite rather than replacing it.

Critical E2E additions:

- hidden unauthorized menu leaves never appear
- authorized mixed-access user still sees allowed siblings when one leaf is denied
- direct deep link to an unreachable route lands on `/accessdenied` before content renders
- granted entity list route with no `READ` shows in-shell denied state and does not load table data
- breadcrumb and active sidebar state stay aligned on deep routes

## Planning Recommendation

The cleanest Phase 7 plan split is likely:

1. backend navigation-grant contract
2. frontend navigation registry plus current-user grant service
3. route metadata, guard, and `/accessdenied` upgrade
4. breadcrumb, active-section, and in-shell denied-state rollout
5. targeted tests and shell cleanup

That split matches the actual dependency order and keeps backend contract work ahead of shell filtering and route behavior.

---

*Phase: 07-enterprise-navigation-shell*
*Research completed: 2026-03-25*
*Sources: .planning/phases/07-enterprise-navigation-shell/07-CONTEXT.md, .planning/phases/07-enterprise-navigation-shell/07-UI-SPEC.md, frontend/src/app/layout/component/menu/app.menu.ts, frontend/src/app/core/auth/user-route-access.service.ts, frontend/src/app.routes.ts, frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts, frontend/src/app/layout/component/menu/app.menu.spec.ts, frontend/e2e/security-comprehensive.spec.ts, src/main/java/com/vn/core/web/rest/SecuredEntityCapabilityResource.java, src/main/java/com/vn/core/web/rest/admin/security/SecCatalogAdminResource.java, src/main/java/com/vn/core/service/security/SecPermissionUiContractService.java*
