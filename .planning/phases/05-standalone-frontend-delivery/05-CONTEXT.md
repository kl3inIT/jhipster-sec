# Phase 5: Standalone Frontend Delivery - Context

**Gathered:** 2026-03-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Ship a standalone Angular frontend under `frontend/` covering:
1. Login + authenticated state management
2. Route protection with correct 401/403/404 handling
3. Security admin: Jmix-style permission matrix editor for roles + separate row policy screen
4. Protected entity screens for Organization, Department, Employee — showing only actions/fields the current user is allowed

</domain>

<decisions>
## Implementation Decisions

### App Structure
- **D-01:** New app under `frontend/` in repo root
- **D-02:** `aef-main/aef-main/` is the PRIMARY structural template — layout, auth, config, routing, entity feature folder pattern. JHipster Angular (`angapp/`) is the secondary reference for auth and admin patterns.
- **D-03:** Angular 21, standalone components, PrimeNG 21.1.3, Tailwind 3.4.19, PrimeFlex 4, Aura preset, NgBootstrap, ngx-translate — match aef-main exactly
- **D-04:** External `.component.html` template files (not inline) — matches aef-main
- **D-05:** Signal-based layout state via `LayoutService` (copy from aef-main); RxJS `AccountService` pattern for auth identity (also from aef-main)

### Authentication
- **D-06:** Copy `aef-main/src/app/core/auth/` verbatim: `auth-jwt.service.ts`, `account.service.ts`, `state-storage.service.ts`, `user-route-access.service.ts`, `auth-expired.interceptor.ts`, `auth.interceptor.ts`
- **D-07:** `AccountService.identity()` fetches `/api/account` — full JHipster account pattern with caching
- **D-08:** JWT stored in localStorage/sessionStorage via StateStorageService (rememberMe-aware)
- **D-09:** `withNavigationErrorHandler` in router config maps 401/403/404 errors to `/login`, `/accessdenied`, `/404`
- **D-10:** Login: POST `/api/authenticate` → `{ id_token }`

### Layout
- **D-11:** Copy `aef-main/src/app/layout/` verbatim
- **D-12:** `ApplicationConfigService.getEndpointFor()` for all API URL resolution
- **D-13:** All authenticated routes nested under AppLayout with `canActivate: [UserRouteAccessService]`

### Navigation / Sidebar
- **D-14:** Menu structure:
  - **Home:** Dashboard (placeholder stub)
  - **Entities:** Organizations, Departments, Employees
  - **Security Admin** (ROLE_ADMIN only): Roles & Permissions, Row Policies
- **D-15:** Authority-gated menu items via `AccountService.hasAnyAuthority(['ROLE_ADMIN'])`

### Protected Entity Screens
- **D-16:** Feature folder per entity: `list/`, `detail/`, `update/`, `route/`, `service/`, model, routes — matching `aef-main/src/app/pages/entities/organization/` exactly
- **D-17:** TypeScript interfaces use optional fields for permission-gated attributes (`budget?: number`, `salary?: number`) — silently absent fields render nothing
- **D-18:** Services use `ApplicationConfigService.getEndpointFor('api/organizations')` etc.; paginated with `X-Total-Count`

---

### Security Admin: Jmix-Style Permission Matrix

#### Conceptual separation (Jmix-aligned)
- **Resource permissions** (entity CRUD + attribute View/Modify) → managed in the Role Permission Matrix editor
- **Row-level restrictions** → managed in a separate Row Policies screen
- These are distinct screens but accessible from the same "Security Admin" section

#### Roles Screen (`/admin/security/roles`)
- **D-19:** List of roles (`GET /api/admin/sec/roles`) in a p-table — columns: Name, Display Name, Type
- **D-20:** Create/edit role in a p-dialog — fields: name (UPPER_SNAKE_CASE), display name, type (RESOURCE_ROLE / ROW_LEVEL_ROLE)
- **D-21:** Each role row has a "Manage Permissions" button → navigates to the Permission Matrix editor for that role
- **D-22:** Delete role also removes its permissions (backend cascade already handles this)

#### Permission Matrix Editor (`/admin/security/roles/:name/permissions`)
Layout: two-panel page within AppLayout

**Top / Left panel — Entity Catalog with CRUD matrix:**
- Load entity list from new backend endpoint `GET /api/admin/sec/catalog` *(backend gap — see below)*
- Display as a p-table with columns: Entity (display name), Create ✓, Read ✓, Update ✓, Delete ✓
- Each checkbox = one `SecPermission` record: `targetType=ENTITY`, `target={entityCode}`, `action={op}`, `effect=GRANT`
- Denied by default — unchecked = no GRANT record exists
- Checking a box PATCHes (adds) one permission; unchecking deletes it

**Bottom / Right panel — Attribute matrix for selected entity:**
- Appears when user clicks a row in the entity table
- Load attribute list from same catalog response
- Display as a p-table: Attribute, View ✓, Modify ✓
- Each checkbox = one `SecPermission` record: `targetType=ATTRIBUTE`, `target="{entityCode}.{attributeName}"`, `action=VIEW|EDIT`, `effect=GRANT`
- Include a wildcard row `*` → `target="{entityCode}.*"` — grants View/Modify on all attributes at once
- When `*` is checked, individual attribute rows become read-only/visual only

**Save strategy:**
- **D-23:** Optimistic immediate saves — each checkbox toggle fires individual POST/DELETE to the permission API. No "Save All" button. This avoids bulk API complexity while keeping the UX responsive.
- **D-24:** Load matrix state on page entry: `GET /api/admin/sec/permissions?authorityName={name}` *(query param filter — backend gap — see below)*; build in-memory Set of granted `{target}:{action}` keys; checkboxes derive their state from this Set

**Visual:**
- **D-25:** Checked checkbox = green-tinted cell or checkmark icon (PrimeNG `p-checkbox` with `binary=true`); unchecked = grey. No partial/inherited state in Phase 5.
- **D-26:** Loading spinner while fetching catalog or permissions
- **D-27:** Toast on save error; silent on success (matrix auto-reflects state)

#### Row Policies Screen (`/admin/security/row-policies`)
- **D-28:** Separate route and screen — NOT embedded in the role matrix
- **D-29:** p-table list of all row policies — columns: Code, Entity, Operation, Policy Type, Expression (truncated)
- **D-30:** Create/edit in a p-dialog — fields: code, entityName (p-select from catalog entity list), operation (READ/UPDATE/DELETE), policyType, expression (textarea)
- **D-31:** UI copy clarifies these are row-level restrictions (filter which rows a principal can access), distinct from resource/attribute permissions
- **D-32:** No role linkage UI in Phase 5 — row policies are global and applied by the enforcement engine independently

---

### Backend Gaps Required for Matrix UX

The current permission data model (`targetType`, `target`, `action`, `effect`) maps perfectly to the matrix. Two API additions are required:

**GAP-1 (REQUIRED): `GET /api/admin/sec/catalog`**
- Returns the list of secured entities with their codes, display names, allowed operations, and attribute names
- Source: `SecuredEntityCatalog.entries()` + JPA metamodel attribute enumeration
- Response shape:
  ```json
  [
    {
      "code": "organization",
      "displayName": "Organization",
      "operations": ["CREATE", "READ", "UPDATE", "DELETE"],
      "attributes": ["code", "name", "ownerLogin", "budget", "departments"]
    },
    ...
  ]
  ```
- Needed by: entity matrix entity list, attribute matrix, row policy entity selector
- **Must be added to the backend before or during Phase 5 planning**

**GAP-2 (REQUIRED): Filter by role on permission list**
- `GET /api/admin/sec/permissions?authorityName={roleName}` — return only permissions for one role
- Current endpoint returns ALL permissions (no filter param)
- Needed to load the matrix initial state for a given role efficiently
- Simple Spring Data query filter addition

**GAP-3 (NICE TO HAVE, NOT BLOCKING): Bulk replace**
- `PUT /api/admin/sec/roles/{name}/permissions` — replaces all permissions for a role atomically
- Not required since D-23 uses per-checkbox saves; add later if performance becomes an issue

---

### API Connectivity
- **D-33:** `proxy.conf.json` routes `/api`, `/management`, `/v3/api-docs` → `http://localhost:8080` (copy aef-main)
- **D-34:** All API paths via `ApplicationConfigService.getEndpointFor()`

### 403/404/401 Handling
- **D-35:** 401 → auth-expired interceptor + redirect to `/login`
- **D-36:** 403 on entity ops → PrimeNG toast "Access denied"
- **D-37:** 404 on detail → navigate to `/404`
- **D-38:** `/accessdenied`, `/404`, `/error` pages — copy from aef-main

### Claude's Discretion
- Exact PrimeIcons for sidebar menu items
- Column ordering in entity list tables
- Breadcrumb label strings
- Whether attribute matrix is bottom panel or side panel (whichever fits p-splitter better)
- Toast message wording for permission save errors

</decisions>

<specifics>
## Specific Ideas

- Jmix role designer reference: entity CRUD matrix on top, attribute View/Modify matrix below for selected entity; denied by default; checkboxes are the primary interaction
- Row policies are conceptually "row-level roles" in Jmix — keep them separate from the resource permission matrix
- Wildcard attribute row (`*`) maps to `target="{code}.*"` in the existing `target` string field — no schema change needed
- Permission matrix "Load" = GET permissions filtered by role + build Set of `{target}:{action}` granted keys; checkbox `checked = grantedSet.has(key)`
- The p-splitter or a two-panel layout with a sticky entity table and scrollable attribute panel below works well for the matrix

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Primary structural template (aef-main)
- `aef-main/aef-main/src/app.config.ts` — bootstrap (PrimeNG, interceptors, router)
- `aef-main/aef-main/src/app.routes.ts` — root routing structure
- `aef-main/aef-main/src/app/core/auth/` — all 5 auth files
- `aef-main/aef-main/src/app/layout/` — full layout module
- `aef-main/aef-main/src/app/pages/entities/organization/` — entity feature folder pattern
- `aef-main/aef-main/proxy.conf.json`
- `aef-main/aef-main/package.json` — exact dependency versions

### Secondary reference (angapp)
- `angapp/src/main/webapp/app/entities/sec-role/` — role admin model + form fields
- `angapp/src/main/webapp/app/entities/sec-permission/` — permission model
- `angapp/src/main/webapp/app/entities/sec-row-policy/` — row policy model

### Backend contracts
- `src/main/java/com/vn/core/web/rest/admin/security/SecRoleAdminResource.java` — `/api/admin/sec/roles`
- `src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java` — `/api/admin/sec/permissions` *(needs filter param)*
- `src/main/java/com/vn/core/web/rest/admin/security/SecRowPolicyAdminResource.java` — `/api/admin/sec/row-policies`
- `src/main/java/com/vn/core/security/catalog/SecuredEntityCatalog.java` — source for GAP-1 catalog endpoint
- `src/main/java/com/vn/core/security/catalog/MetamodelSecuredEntityCatalog.java` — implementation to wrap for catalog endpoint
- `src/main/java/com/vn/core/security/permission/TargetType.java` — ENTITY / ATTRIBUTE / ROW_POLICY
- `src/main/java/com/vn/core/security/permission/EntityOp.java` — READ / CREATE / UPDATE / DELETE
- `src/main/java/com/vn/core/security/permission/AttributeOp.java` — VIEW / EDIT
- `src/main/java/com/vn/core/service/dto/security/SecPermissionDTO.java` — permission DTO (target, action, effect)
- `src/main/java/com/vn/core/service/dto/security/SecRoleDTO.java` — role DTO
- `src/main/java/com/vn/core/service/dto/security/SecRowPolicyDTO.java` — row policy DTO
- `src/main/java/com/vn/core/web/rest/OrganizationResource.java` — `/api/organizations`
- `src/main/java/com/vn/core/web/rest/DepartmentResource.java` — `/api/departments`
- `src/main/java/com/vn/core/web/rest/EmployeeResource.java` — `/api/employees`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `aef-main/src/app/layout/` + `aef-main/src/app/core/auth/` + `aef-main/src/app.config.ts` — copy verbatim
- `aef-main/src/app/pages/entities/organization/` — template for all 3 entity features
- `angapp/entities/sec-row-policy/` — row policy form fields reference

### Permission Model fits the Matrix exactly
- Entity permission: `{ targetType: "ENTITY", target: "organization", action: "READ", effect: "GRANT" }`
- Attribute permission: `{ targetType: "ATTRIBUTE", target: "organization.budget", action: "VIEW", effect: "GRANT" }`
- Wildcard attribute: `{ targetType: "ATTRIBUTE", target: "organization.*", action: "VIEW", effect: "GRANT" }`
- No schema change needed — the existing `target` string field carries whatever the frontend sends

### Backend gaps to implement in Phase 5 backend plans
- **GAP-1:** `GET /api/admin/sec/catalog` — wrap `MetamodelSecuredEntityCatalog.entries()` + enumerate JPA metamodel attributes per entity class
- **GAP-2:** Add `?authorityName=` query param to `GET /api/admin/sec/permissions` in `SecPermissionAdminResource`

### Integration Points
- Backend JWT at `/api/authenticate`, account at `/api/account`
- Secured entity APIs return `Map<String,Object>` — use optional TS fields
- All admin endpoints require ROLE_ADMIN (enforced backend-side with `@PreAuthorize`)
- Pagination: `page`, `size`, `sort` params; `X-Total-Count` header

</code_context>

<deferred>
## Deferred Ideas

- Account profile / change-password screen
- Sec-fetch-plan admin UI (fetch plans are YAML-only)
- Inherited / base-role read-only overlay in matrix (Jmix inheritance) — no role hierarchy in current model
- Bulk permission replace API (GAP-3) — add if per-checkbox saves become a performance issue
- Dashboard with real metrics — stub only
- Full user management screen
- i18n translation completeness
- Role assignment to users (currently via Authority entity, no frontend screen)

</deferred>

---

*Phase: 05-standalone-frontend-delivery*
*Context gathered: 2026-03-22*
