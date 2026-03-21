# Phase 5: Standalone Frontend Delivery - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Ship a standalone Angular frontend under `frontend/` that covers:
1. Login + authenticated state management
2. Route protection with correct 401/403/404 handling
3. Admin screens for managing merged roles, permission rules, and row policies
4. Protected entity screens for Organization, Department, Employee — showing only the actions and fields the current user is allowed to access

</domain>

<decisions>
## Implementation Decisions

### App Structure
- **D-01:** New app lives under `frontend/` in the repo root
- **D-02:** `aef-main/aef-main/` is the PRIMARY structural template — copy its layout, auth, config, routing, and entity patterns. `jhipter-angular/frontend/` and `angapp/` are secondary references.
- **D-03:** Angular 21 with standalone components throughout — no NgModules (matches aef-main)
- **D-04:** PrimeNG 21.1.3 + Tailwind CSS 3.4.19 + PrimeFlex 4 + Aura preset (matches aef-main exactly)
- **D-05:** External HTML template files (`.component.html`) — matches aef-main convention, not inline templates
- **D-06:** ngx-translate for i18n strings — already in aef-main, keep it
- **D-07:** NgBootstrap retained alongside PrimeNG — aef-main uses both (datepicker, modals)

### Authentication
- **D-08:** Copy `aef-main/aef-main/src/app/core/auth/` verbatim: `auth-jwt.service.ts`, `account.service.ts`, `state-storage.service.ts`, `user-route-access.service.ts`, `auth-expired.interceptor.ts`, `auth.interceptor.ts`
- **D-09:** JWT stored in `localStorage` or `sessionStorage` based on rememberMe flag (StateStorageService pattern)
- **D-10:** `AccountService.identity()` fetches `/api/account` and caches result — full JHipster account pattern
- **D-11:** `UserRouteAccessService` (CanActivateFn) — redirects unauthenticated to `/login`, 403 to `/accessdenied`
- **D-12:** `withNavigationErrorHandler` in router config maps 401/403/404 navigation errors to correct routes (already in aef-main app.config.ts)
- **D-13:** Login endpoint: POST `/api/authenticate` → `{ id_token }`

### Layout
- **D-14:** Copy `aef-main/aef-main/src/app/layout/` verbatim — AppLayout, AppTopbar, AppSidebar, AppMenu, AppMenuitem, AppBreadcrumb, AppFooter, AppConfigurator, LayoutService
- **D-15:** All authenticated routes nested under AppLayout route with `canActivate: [UserRouteAccessService]`
- **D-16:** `ApplicationConfigService` for all API URL resolution — `getEndpointFor('api/organizations')` etc.

### Navigation / Sidebar Menu
- **D-17:** Menu structure:
  - **Home:** Dashboard (placeholder)
  - **Entities:** Organizations, Departments, Employees
  - **Admin** (ROLE_ADMIN only): Roles, Permissions, Row Policies
- **D-18:** Authority-gated menu items using `AccountService.hasAnyAuthority(['ROLE_ADMIN'])`

### Entity Services & Components
- **D-19:** Follow `aef-main/aef-main/src/app/pages/entities/organization/` feature structure: `list/`, `detail/`, `update/`, `route/`, `service/`, `*.model.ts`, `*.routes.ts`
- **D-20:** Entity interfaces use optional fields for permission-gated attributes (e.g., `budget?: number`, `salary?: number`) — silently absent fields from backend render nothing in template
- **D-21:** `ApplicationConfigService.getEndpointFor('api/organizations')` for resource URLs
- **D-22:** `Observable<HttpResponse<T>>` return type from services, `X-Total-Count` for pagination

### Security Admin Screens
- **D-23:** Port sec-role, sec-permission, sec-row-policy screens from `angapp/` data model + forms, but implement with PrimeNG p-table + p-dialog following aef-main entity component pattern
- **D-24:** Backend API paths: `/api/sec-roles`, `/api/sec-permissions`, `/api/sec-row-policies`
- **D-25:** Admin screens require `data: { authorities: ['ROLE_ADMIN'] }` + `UserRouteAccessService`

### API Connectivity
- **D-26:** `proxy.conf.json` routes `/api`, `/management`, `/v3/api-docs` → `http://localhost:8080` (copy aef-main proxy.conf.json)
- **D-27:** `ApplicationConfigService` sets endpoint prefix to empty string for monolith (same-origin)

### 403 / 404 / 401 Handling
- **D-28:** 401 → `auth-expired` interceptor clears token + navigates to `/login`
- **D-29:** 403 on entity operations → PrimeNG toast "Access denied"
- **D-30:** 404 on entity detail → navigate to `/404`
- **D-31:** `/accessdenied`, `/404`, `/error` pages — copy from aef-main or angapp

### Claude's Discretion
- Exact sidebar icon choices (PrimeIcons)
- Breadcrumb label strings
- Toast message wording
- Loading indicator style (skeleton vs spinner)
- Column ordering in list tables

</decisions>

<specifics>
## Specific Ideas

- `aef-main/aef-main/src/app/pages/entities/organization/` is the exact feature folder structure to replicate for Organization, Department, Employee
- `angapp/src/main/webapp/app/entities/sec-role/`, `sec-permission/`, `sec-row-policy/` define the data models and form fields for security admin screens
- Dashboard can be a minimal stub ("Coming soon") — not in scope for Phase 5
- Protected entity fields absent from API response simply don't render — no locked-field placeholder UI needed in Phase 5

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Primary structural template (aef-main)
- `aef-main/aef-main/src/app.config.ts` — app bootstrap (PrimeNG, interceptors, router features)
- `aef-main/aef-main/src/app.routes.ts` — root routing structure
- `aef-main/aef-main/src/app/core/auth/` — full auth module (all 5 files)
- `aef-main/aef-main/src/app/layout/` — full layout module
- `aef-main/aef-main/src/app/pages/entities/organization/` — entity feature pattern
- `aef-main/aef-main/proxy.conf.json` — dev proxy config
- `aef-main/aef-main/package.json` — exact dependency versions

### Security admin screen reference (angapp)
- `angapp/src/main/webapp/app/entities/sec-role/` — role management (model, service, list, update)
- `angapp/src/main/webapp/app/entities/sec-permission/` — permission management
- `angapp/src/main/webapp/app/entities/sec-row-policy/` — row policy management

### Secondary layout reference
- `jhipter-angular/frontend/src/app/layout/` — alternative AppLayout impl (signal-based, Angular 20)
- `jhipter-angular/frontend/src/app/core/auth/` — signal-based auth alternative

### Backend API contracts
- `src/main/java/com/vn/core/web/rest/OrganizationResource.java` — `/api/organizations`
- `src/main/java/com/vn/core/web/rest/DepartmentResource.java` — `/api/departments`
- `src/main/java/com/vn/core/web/rest/EmployeeResource.java` — `/api/employees`
- `src/main/java/com/vn/core/web/rest/admin/security/SecRoleAdminResource.java` — `/api/sec-roles`
- `src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java` — `/api/sec-permissions`
- `src/main/java/com/vn/core/web/rest/admin/security/SecRowPolicyAdminResource.java` — `/api/sec-row-policies`
- `src/main/java/com/vn/core/service/dto/security/SecRoleDTO.java` — role shape
- `src/main/java/com/vn/core/service/dto/security/SecPermissionDTO.java` — permission shape
- `src/main/java/com/vn/core/service/dto/security/SecRowPolicyDTO.java` — row policy shape

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `aef-main/aef-main/src/app/layout/`: Copy entire directory — all layout components and LayoutService
- `aef-main/aef-main/src/app/core/auth/`: Copy all 5 auth files verbatim
- `aef-main/aef-main/src/app/pages/entities/organization/`: Use as feature template for all 3 entities
- `aef-main/aef-main/proxy.conf.json`: Copy as-is
- `aef-main/aef-main/src/app.config.ts`: Copy and adapt

### Established Patterns
- Entity feature: `list/` + `detail/` + `update/` + `route/` + `service/` + model + routes file
- Service: `ApplicationConfigService.getEndpointFor()`, `Observable<HttpResponse<T>>`, `X-Total-Count` pagination
- Route guard: `UserRouteAccessService` CanActivateFn checking `AccountService.hasAnyAuthority()`
- Error routing: `withNavigationErrorHandler` in router config

### Integration Points
- Backend JWT issued by `/api/authenticate`, account info at `/api/account`
- Secured entities return `Map<String,Object>` — use optional fields in TypeScript interfaces
- Admin endpoints require ROLE_ADMIN (enforced backend-side)
- Pagination: `page`, `size`, `sort` query params; `X-Total-Count` response header

</code_context>

<deferred>
## Deferred Ideas

- Account profile / change-password screen — only login/logout needed in Phase 5
- Sec-fetch-plan admin UI — fetch plans are YAML-defined, no admin screen needed
- Role-based "locked field" indicators in entity forms — UX polish for later
- Dashboard with real metrics — stub is sufficient
- Full user management screen — out of scope for Phase 5
- i18n translation completeness — keep English stubs, full i18n later

</deferred>

---

*Phase: 05-standalone-frontend-delivery*
*Context gathered: 2026-03-21*
