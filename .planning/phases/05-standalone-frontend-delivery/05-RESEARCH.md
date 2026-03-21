# Phase 5: Standalone Frontend Delivery - Research

**Researched:** 2026-03-22
**Domain:** Angular 21 standalone app, PrimeNG 21, JHipster auth pattern, permission matrix UI
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** New app under `frontend/` in repo root
- **D-02:** `aef-main/aef-main/` is the PRIMARY structural template. JHipster Angular (`angapp/`) is the secondary reference for auth and admin patterns.
- **D-03:** Angular 21, standalone components, PrimeNG 21.1.3, Tailwind 3.4.19, PrimeFlex 4, Aura preset, NgBootstrap, ngx-translate — match aef-main exactly
- **D-04:** External `.component.html` template files (not inline)
- **D-05:** Signal-based layout state via `LayoutService`; RxJS `AccountService` pattern for auth identity
- **D-06:** Copy `aef-main/src/app/core/auth/` verbatim: `auth-jwt.service.ts`, `account.service.ts`, `state-storage.service.ts`, `user-route-access.service.ts`, `auth-expired.interceptor.ts`, `auth.interceptor.ts`
- **D-07:** `AccountService.identity()` fetches `/api/account` with caching
- **D-08:** JWT stored in localStorage/sessionStorage via StateStorageService (rememberMe-aware)
- **D-09:** `withNavigationErrorHandler` in router config maps 401/403/404 errors to `/login`, `/accessdenied`, `/404`
- **D-10:** Login: POST `/api/authenticate` → `{ id_token }`
- **D-11:** Copy `aef-main/src/app/layout/` verbatim
- **D-12:** `ApplicationConfigService.getEndpointFor()` for all API URL resolution
- **D-13:** All authenticated routes nested under AppLayout with `canActivate: [UserRouteAccessService]`
- **D-14:** Menu structure: Home (Dashboard), Entities (Organizations/Departments/Employees), Security Admin ROLE_ADMIN only (Roles & Permissions, Row Policies)
- **D-15:** Authority-gated menu items via `AccountService.hasAnyAuthority(['ROLE_ADMIN'])`
- **D-16:** Feature folder per entity: `list/`, `detail/`, `update/`, `route/`, `service/`, model, routes
- **D-17:** TypeScript interfaces use optional fields for permission-gated attributes
- **D-18:** Services use `ApplicationConfigService.getEndpointFor('api/organizations')` etc.; paginated with `X-Total-Count`
- **D-19 through D-32:** Permission matrix and row policies screen decisions (see CONTEXT.md)
- **D-33:** `proxy.conf.json` routes `/api`, `/management`, `/v3/api-docs` → `http://localhost:8080`
- **D-34:** All API paths via `ApplicationConfigService.getEndpointFor()`
- **D-35 through D-38:** 401/403/404 handling decisions

### Claude's Discretion

- Exact PrimeIcons for sidebar menu items (specified in UI-SPEC as pi-home, pi-building, pi-sitemap, pi-users, pi-shield, pi-filter)
- Column ordering in entity list tables
- Breadcrumb label strings
- Whether attribute matrix is bottom panel or side panel (p-splitter — bottom panel per UI-SPEC)
- Toast message wording for permission save errors (specified in UI-SPEC)

### Deferred Ideas (OUT OF SCOPE)

- Account profile / change-password screen
- Sec-fetch-plan admin UI
- Inherited / base-role read-only overlay in matrix
- Bulk permission replace API (GAP-3)
- Dashboard with real metrics — stub only
- Full user management screen
- i18n translation completeness
- Role assignment to users
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | User can log in from the standalone `frontend/` app using the existing JWT authentication flow | Auth service pattern copied from aef-main; POST `/api/authenticate` → JWT → localStorage; AccountService identity caching verified |
| ENT-03 | Sample entity screens in `frontend/` reflect allowed and denied actions and field visibility correctly | Backend returns `Map<String,Object>` with absent attributes; TS interfaces use optional fields; `@if (field !== undefined)` guards; confirmed via domain entity inspection |
| UI-01 | A standalone Angular app exists under `frontend/` following `aef-main/aef-main` structural direction | Full aef-main structure confirmed: `app.config.ts`, `app.routes.ts`, `layout/`, `core/`, `pages/entities/`, `pages/login/` |
| UI-02 | Frontend provides end-to-end role, permission, and row-policy management screens | Backend DTOs and REST contracts confirmed; GAP-1 and GAP-2 backend additions required; permission matrix interaction contract defined in CONTEXT.md |
| UI-03 | Frontend handles authentication state, route protection, and 401/403/404 flows correctly | `UserRouteAccessService` as `CanActivateFn`, `withNavigationErrorHandler`, `AuthExpiredInterceptor`, `AuthInterceptor` — all confirmed in aef-main |
</phase_requirements>

---

## Summary

Phase 5 is a predominantly frontend construction phase with two small backend additions. The aef-main Angular app is a well-structured template that can be adapted with minimal friction — the auth core, layout, interceptors, and entity feature pattern are all directly reusable. The primary technical challenge is the permission matrix UI: managing in-memory grant state as a `Set<string>` keyed by `{target}:{action}`, driving `p-checkbox` cells with immediate-save per toggle, and handling the catalog load + permissions filter load on entry.

The backend contract is stable and confirmed. Two backend gaps (GAP-1 catalog endpoint, GAP-2 permission filter param) must be implemented as part of this phase before the matrix UI can be tested. All three proof entities (Organization, Department, Employee) are confirmed `@SecuredEntity` annotated with the expected field shapes. The attribute-optional pattern for `budget` and `salary` is structurally sound and confirmed in the domain entities.

The Angular stack is fully determined: Angular 21 standalone components, PrimeNG 21.1.3 with Aura preset, Tailwind 3.4.19 + PrimeFlex 4, ngx-translate 17, NgBootstrap 20. The exact versions are confirmed from `aef-main/aef-main/package.json`.

**Primary recommendation:** Initialize `frontend/` by copying the `aef-main/aef-main` scaffolding, adapting the menu and routes for this project's navigation contract, then building the feature screens in dependency order: auth → entity screens → roles list → permission matrix → row policies.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `@angular/core` | ^21.2.0 | Framework | Locked by D-03; matches aef-main exactly |
| `@angular/router` | ^21.2.0 | Routing, guards, navigation error handler | Locked |
| `@angular/forms` | ^21.2.0 | Reactive forms for update/create screens | Locked |
| `@angular/common` | ^21.2.0 | CommonModule, HttpClient | Locked |
| `primeng` | ^21.1.3 | Full UI component library | Locked by D-03 |
| `@primeuix/themes` | ^2.0.3 | Aura theme preset | Locked; provides CSS custom properties for color |
| `primeicons` | ^7.0.0 | Icon set (`pi pi-*` classes) | Locked |
| `primeflex` | ^4.0.0 | Utility CSS spacing/layout fallback | Locked by D-03 |
| `tailwindcss` | ^3.4.19 | Utility CSS primary spacing | Locked by D-03; dev dep |
| `@ng-bootstrap/ng-bootstrap` | ^20.0.0 | Date-pickers | Locked by D-03 |
| `@ngx-translate/core` | ^17.0.0 | i18n string resolution | Locked by D-03 |
| `@ngx-translate/http-loader` | ^17.0.0 | Translation file loader | Locked by D-03 |
| `dayjs` | ^1.11.20 | Date handling (NgbDateAdapter) | Locked; already in aef-main |
| `rxjs` | ~7.8.0 | Reactive programming, auth state | Locked |

### Supporting / Dev

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `@angular/cli` | ^21.2.2 | Build tooling (`ng serve`, `ng build`) | Dev tool |
| `@angular/build` | ^21.2.2 | Application builder (esbuild) | Dev tool |
| `typescript` | ~5.9.2 | Type checking | Dev tool |
| `vitest` | ^4.0.8 | Unit test runner (used by aef-main) | Testing |
| `prettier` | ^3.8.1 | Code formatting | Dev tool |
| `autoprefixer` | ^10.4.27 | PostCSS for Tailwind | Dev tool |
| `postcss` | ^8.5.8 | Tailwind CSS processing | Dev tool |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `primeng` | Angular Material | Locked — PrimeNG is the project stack |
| `tailwindcss` v3 | Tailwind v4 | Locked at 3.4.19 — aef-main uses v3; v4 has different PostCSS integration |
| `vitest` | Karma/Jasmine | aef-main uses vitest; Angular CLI 21 test builder is configured |

**Installation (from repo root):**
```bash
ng new frontend --standalone --routing --style=scss --skip-git
cd frontend
npm install primeng@^21.1.3 @primeuix/themes@^2.0.3 primeicons@^7.0.0 primeflex@^4.0.0
npm install @ng-bootstrap/ng-bootstrap@^20.0.0
npm install @ngx-translate/core@^17.0.0 @ngx-translate/http-loader@^17.0.0
npm install dayjs@^1.11.20
npm install -D tailwindcss@^3.4.19 autoprefixer@^10.4.27 postcss@^8.5.8
```

---

## Architecture Patterns

### Recommended Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── core/
│   │   │   ├── auth/                   # Copied verbatim from aef-main (D-06)
│   │   │   │   ├── account.model.ts
│   │   │   │   ├── account.service.ts
│   │   │   │   ├── auth-jwt.service.ts
│   │   │   │   ├── state-storage.service.ts
│   │   │   │   └── user-route-access.service.ts
│   │   │   ├── config/
│   │   │   │   └── application-config.service.ts
│   │   │   ├── interceptor/
│   │   │   │   ├── auth.interceptor.ts
│   │   │   │   ├── auth-expired.interceptor.ts
│   │   │   │   ├── error-handler.interceptor.ts
│   │   │   │   ├── notification.interceptor.ts
│   │   │   │   └── index.ts
│   │   │   ├── request/
│   │   │   └── util/
│   │   ├── layout/                     # Copied verbatim from aef-main (D-11)
│   │   │   ├── component/
│   │   │   │   ├── main/app.layout.ts
│   │   │   │   ├── menu/app.menu.ts    # Modified: project-specific nav
│   │   │   │   ├── menu/app.menuitem.ts
│   │   │   │   ├── topbar/
│   │   │   │   ├── sidebar/
│   │   │   │   └── footer/
│   │   │   └── service/layout.service.ts
│   │   ├── config/
│   │   │   └── authority.constants.ts
│   │   ├── shared/
│   │   │   ├── sort/                   # SortService, sortStateSignal
│   │   │   ├── language/               # TranslationModule
│   │   │   └── shared.module.ts
│   │   └── pages/
│   │       ├── login/
│   │       ├── home/                   # Dashboard stub
│   │       ├── error/                  # accessdenied, 404, error pages
│   │       ├── entities/
│   │       │   ├── entity.routes.ts
│   │       │   ├── organization/       # list/ detail/ update/ service/ model routes
│   │       │   ├── department/
│   │       │   └── employee/
│   │       └── admin/
│   │           └── security/
│   │               ├── roles/          # list + create/edit dialog
│   │               ├── permission-matrix/   # two-panel p-splitter editor
│   │               └── row-policies/   # list + create/edit dialog
│   ├── app.config.ts
│   ├── app.routes.ts
│   ├── main.ts
│   └── assets/
│       ├── styles.scss
│       ├── tailwind.css
│       └── i18n/
├── proxy.conf.json
├── angular.json
├── tsconfig.json
└── package.json
```

### Pattern 1: Standalone Component with PrimeNG Imports

All components are standalone. Each imports exactly the PrimeNG modules it uses. No NgModule for features.

```typescript
// Source: aef-main/src/app/pages/entities/organization/list/organization-list.component.ts
@Component({
  selector: 'app-organization-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, SharedModule,
    CardModule, TableModule, ButtonModule,
    ConfirmDialogModule, ToastModule, TranslateModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './organization-list.component.html',
})
export default class OrganizationListComponent { ... }
```

**Note:** Default export is required for lazy-loaded `loadComponent()` routes.

### Pattern 2: Auth Guard as CanActivateFn (functional)

```typescript
// Source: aef-main/src/app/core/auth/user-route-access.service.ts
export const UserRouteAccessService: CanActivateFn = (next, state) => {
  const accountService = inject(AccountService);
  return accountService.identity().pipe(
    map(account => {
      if (account) {
        const { authorities } = next.data;
        if (!authorities || authorities.length === 0 || accountService.hasAnyAuthority(authorities)) {
          return true;
        }
        inject(Router).navigate(['/accessdenied']);
        return false;
      }
      inject(StateStorageService).storeUrl(state.url);
      inject(Router).navigate(['/login']);
      return false;
    }),
  );
};
```

Admin-only routes add `data: { authorities: ['ROLE_ADMIN'] }`.

### Pattern 3: Route Configuration with Error Handler

```typescript
// Source: aef-main/src/app.config.ts
withNavigationErrorHandler((e: NavigationError) => {
  const router = inject(Router);
  if (e.error.status === 403) router.navigate(['/accessdenied']);
  else if (e.error.status === 404) router.navigate(['/404']);
  else if (e.error.status === 401) router.navigate(['/login']);
  else router.navigate(['/error']);
})
```

### Pattern 4: Entity Service Shape

```typescript
// Source: aef-main/src/app/pages/entities/organization/service/organization.service.ts
@Injectable({ providedIn: 'root' })
export class OrganizationService {
  protected resourceUrl = inject(ApplicationConfigService).getEndpointFor('api/organizations');

  query(req?: any): Observable<HttpResponse<IOrganization[]>> {
    const options = createRequestOption(req);
    return this.http.get<IOrganization[]>(this.resourceUrl, { params: options, observe: 'response' });
  }
  // create / update / partialUpdate / find / delete
}
```

For this project the backend returns `Map<String,Object>` so the TS interface must use `Record<string, unknown>` or typed optional fields. Services use `HttpResponse<IOrganization>` where `IOrganization` has optional sensitive fields.

### Pattern 5: Permission Matrix Set-Based State

No existing code to copy — this is a new pattern for the matrix screen.

```typescript
// Conceptual — planner derives implementation from this
interface PermissionKey {
  target: string;
  action: string;
}

// On load: GET /api/admin/sec/permissions?authorityName=ROLE_X
// Build granted Set<string>
const granted = new Set<string>(
  permissions.map(p => `${p.target}:${p.action}`)
);

// Checkbox checked state
isGranted(target: string, action: string): boolean {
  return this.granted.has(`${target}:${action}`);
}

// On check: POST to create permission; on uncheck: DELETE by id
// Store id map: Map<string, number> keyed by "{target}:{action}" → permissionId
```

The `idMap` allows DELETE by id on uncheck without a secondary lookup.

### Pattern 6: Lazy-Loaded Feature Routes

```typescript
// app.routes.ts pattern
{
  path: '',
  loadComponent: () => import('./app/layout/component/main/app.layout').then(m => m.AppLayout),
  children: [
    { path: 'entities', loadChildren: () => import('./app/pages/entities/entity.routes') },
    { path: 'admin/security', loadChildren: () => import('./app/pages/admin/security/security.routes') },
  ]
}
```

### Pattern 7: Menu with Authority Gate

```typescript
// Source: aef-main/src/app/layout/component/menu/app.menu.ts
private buildMenu(): void {
  const isAdmin = this.accountService.hasAnyAuthority(Authority.ADMIN);
  this.model = [
    { label: 'Home', items: [...] },
    { label: 'Entities', items: [...] },
    ...(isAdmin ? [{ label: 'Security Admin', items: [...] }] : []),
  ];
}
```

Subscribe to `accountService.getAuthenticationState()` so menu rebuilds on login/logout.

### Anti-Patterns to Avoid

- **NgModule feature wrappers:** Every component is standalone. Don't create feature modules.
- **Hardcoded API URLs:** Always use `ApplicationConfigService.getEndpointFor('api/...')`.
- **HttpClient without `observe: 'response'`:** Entity list services must return `HttpResponse<T[]>` to read `X-Total-Count` header.
- **Enum-typed TS interface fields:** Use `string` for `type`, `targetType`, `action`, `effect` in permission/role/policy interfaces — the backend DTOs use `String` and do not send enum names consistently. Keep the interface open.
- **Optional field placeholders:** When `budget` or `salary` is absent from the API response, render nothing. Do not show "N/A" or an empty table cell.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Dark/light mode toggle | Custom CSS class toggle | `LayoutService.toggleDarkMode()` from aef-main | View Transition API integration already handled |
| JWT token storage | Manual localStorage | `StateStorageService.storeAuthenticationToken()` | rememberMe logic, session vs. local storage split |
| Pagination header parsing | Custom header read | `TOTAL_COUNT_RESPONSE_HEADER` constant + `fillComponentAttributesFromResponseHeader` | Already tested pattern in aef-main |
| Sort state management | Ad hoc signals | `SortService`, `sortStateSignal` from aef-main shared/sort | Handles URL sync, sort param build |
| Delete confirmation | Custom modal | `ConfirmationService` + `p-confirmDialog` | PrimeNG built-in, wires to accept/reject pattern |
| API URL prefix | String concat | `ApplicationConfigService.getEndpointFor()` | Microfrontend-safe, proxy-aware |
| PrimeNG theme config | Manual CSS | `providePrimeNG({ theme: { preset: Aura, options: { darkModeSelector: '.app-dark' } } })` | Required in `app.config.ts` |

**Key insight:** The aef-main codebase already solved auth state management, JWT storage, interceptor wiring, sort/pagination, and layout — copy those files verbatim rather than reimplementing.

---

## Backend API Contracts (Confirmed)

### Existing Endpoints (Phase 4 products)

| Endpoint | Method | Auth | Returns |
|----------|--------|------|---------|
| `/api/authenticate` | POST | none | `{ id_token: string }` |
| `/api/account` | GET | JWT | Account with `authorities: string[]` |
| `/api/organizations` | GET | authenticated | `Map<String,Object>[]` + `X-Total-Count` |
| `/api/organizations/{id}` | GET/PUT/POST/DELETE | authenticated | `Map<String,Object>` |
| `/api/departments` | GET | authenticated | paged |
| `/api/employees` | GET | authenticated | paged |
| `/api/admin/sec/roles` | GET/POST/PUT/DELETE | ROLE_ADMIN | `SecRoleDTO[]` |
| `/api/admin/sec/permissions` | GET/POST/PUT/DELETE | ROLE_ADMIN | `SecPermissionDTO[]` |
| `/api/admin/sec/row-policies` | GET/POST/PUT/DELETE | ROLE_ADMIN | `SecRowPolicyDTO[]` |

### Backend Gaps Required in Phase 5 (HIGH confidence — confirmed by reading source)

**GAP-1 (REQUIRED): `GET /api/admin/sec/catalog`**

Must be added to `SecPermissionAdminResource` (or a new `SecCatalogAdminResource`). Wraps `MetamodelSecuredEntityCatalog.entries()`. Response shape:

```json
[
  {
    "code": "organization",
    "displayName": "Organization",
    "operations": ["CREATE", "READ", "UPDATE", "DELETE"],
    "attributes": ["code", "name", "ownerLogin", "budget", "departments"]
  }
]
```

The `MetamodelSecuredEntityCatalog` already has `entries()` returning `List<SecuredEntityEntry>`. Each `SecuredEntityEntry` has `code()` and `operations()`. Attribute enumeration requires iterating the JPA metamodel `EntityType.getAttributes()` for the entry's `entityClass()`.

**GAP-2 (REQUIRED): `?authorityName=` param on `GET /api/admin/sec/permissions`**

Current `getAllPermissions()` calls `secPermissionRepository.findAll()`. Add:
- `@RequestParam(required = false) String authorityName`
- When present: `secPermissionRepository.findByAuthorityName(authorityName)` (add JPQL query to repository)
- When absent: existing `findAll()` behavior

### Key DTO Shapes (Confirmed)

**SecRoleDTO:**
```typescript
interface ISecRole {
  name: string;         // UPPER_SNAKE_CASE, max 50
  displayName?: string; // max 255
  type: string;         // "RESOURCE" or "ROW_LEVEL" (confirmed: RoleType.java enum values)
}
```

Confirmed: `RoleType.java` defines exactly `RESOURCE` and `ROW_LEVEL`. The role create/edit dialog p-select options must send those exact strings.

**SecPermissionDTO:**
```typescript
interface ISecPermission {
  id?: number;
  authorityName: string;   // max 50
  targetType: string;      // "ENTITY" | "ATTRIBUTE"
  target: string;          // e.g. "organization" or "organization.budget"
  action: string;          // "READ" | "CREATE" | "UPDATE" | "DELETE" | "VIEW" | "EDIT"
  effect: string;          // "GRANT"
}
```

**SecRowPolicyDTO:**
```typescript
interface ISecRowPolicy {
  id?: number;
  code: string;        // max 100
  entityName: string;  // max 255
  operation: string;   // "READ" | "UPDATE" | "DELETE"
  policyType: string;  // "SPECIFICATION" | "JPQL" (confirmed; JAVA is rejected at runtime with AccessDeniedException)
  expression: string;  // max 1000
}
```

### Entity Field Maps (Confirmed from domain entities)

| Entity | Required fields | Optional/permission-gated fields |
|--------|----------------|----------------------------------|
| Organization | `id`, `code`, `name`, `ownerLogin` | `budget` (BigDecimal) |
| Department | `id`, `code`, `name`, `organization` | `costCenter` (partial — not a security gate but still optional) |
| Employee | `id`, `employeeNumber`, `firstName`, `lastName`, `department` | `email`, `salary` (BigDecimal) |

---

## Common Pitfalls

### Pitfall 1: `SecRoleDTO.name` is the entity ID (not a numeric id)

**What goes wrong:** The roles resource uses `name` (a String) as the primary key, not a numeric `id`. PUT and DELETE go to `/api/admin/sec/roles/{name}`, not `/{id}`. Angular entity services usually use `id: number` — the role service must use `name: string` as the identifier.

**How to avoid:** Role model interface has no numeric `id` field. Service methods use `name` as the path parameter. The `getRoleIdentifier` helper returns `role.name`.

### Pitfall 2: `Map<String,Object>` response requires optional TS fields

**What goes wrong:** Backend returns only the fields the current user is allowed to see. A TypeScript interface with required fields will fail type-checking or throw at runtime when the field is absent.

**How to avoid:** All entity interfaces use optional fields for everything except `id`. Guard column visibility with `@if (row.budget !== undefined)`. Do NOT use `?.` with a fallback — the design intent is to render nothing when absent.

### Pitfall 3: Permission matrix idMap drift on server error

**What goes wrong:** A `p-checkbox` toggle fires POST/DELETE immediately. If the server returns an error, the local `granted` Set and `idMap` are out of sync with server state.

**How to avoid:** On save error, revert the local state before showing the toast. Pattern: update local state optimistically, on error pipe — reverse the change, show error toast. Alternatively: on error, reload permissions from server (GET) to resync.

### Pitfall 4: Wildcard `*` disabling individual attribute checkboxes

**What goes wrong:** When `organization.*:VIEW` is in the granted set, individual `organization.budget:VIEW` checkboxes must be `[disabled]="true"`. If the disable logic only checks the in-memory set without special-casing the wildcard key, users can still click individual boxes and create redundant records.

**How to avoid:** Compute `isWildcardGranted(entityCode, action): boolean` separately. In template: `[disabled]="isWildcardGranted(entity.code, 'VIEW')"`. Only suppress checkbox interaction; do not suppress the visual checked state for individual attributes that have their own grant.

### Pitfall 5: Auth interceptor skips external URLs

**What goes wrong:** The `AuthInterceptor` (confirmed in aef-main) checks `request.url.startsWith(serverApiUrl)` and skips token injection for external URLs. If `serverApiUrl` is empty (no prefix configured), ALL requests get tokens.

**How to avoid:** The `ApplicationConfigService.endpointPrefix` defaults to `''`. With proxy config, the empty prefix is correct — all `/api/...` calls go through the dev proxy. For production builds, set the prefix to the backend base URL. Do not configure `endpointPrefix` for dev; it works with proxy as-is.

### Pitfall 6: PrimeNG `p-table` lazy load vs. client-side pagination

**What goes wrong:** Using `[lazy]="true"` with `(onLazyLoad)="onLazyLoad($event)"` without handling the `TableLazyLoadEvent` correctly causes infinite reload loops or wrong page numbers.

**How to avoid:** Follow the aef-main pattern exactly: `onLazyLoad` translates `event.first` and `event.rows` into a `page` number, then calls `navigateToPage(page)` if the page changed, or `navigateToWithComponentValues(sortState)` if only sort changed. Never call `load()` directly from `onLazyLoad`.

### Pitfall 7: `p-confirmDialog` requires a single instance per component

**What goes wrong:** Multiple `<p-confirmDialog>` tags in the template or template inheritance causes dialogs to stack or not open.

**How to avoid:** Declare exactly one `<p-confirmDialog></p-confirmDialog>` at the bottom of each list component template. Provide `ConfirmationService` in the component's `providers: []` array (not `providedIn: 'root'`).

### Pitfall 8: RoleType enum mismatch

**What goes wrong:** The `SecRoleAdminResource` calls `RoleType.valueOf(dto.getType())`. Sending any value other than the exact Java enum name causes `IllegalArgumentException` returning 500. The confirmed enum values are `"RESOURCE"` and `"ROW_LEVEL"` — not `"RESOURCE_ROLE"` or `"ROW_LEVEL_ROLE"`.

**How to avoid:** Use `"RESOURCE"` and `"ROW_LEVEL"` as the p-select option values — these are confirmed from `RoleType.java`. Do not use any other spelling.

---

## Code Examples

### Bootstrap (app.config.ts)

```typescript
// Source: aef-main/aef-main/src/app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(appRoutes, ...routerFeatures),
    importProvidersFrom(BrowserModule),
    importProvidersFrom(TranslationModule),
    provideHttpClient(withInterceptorsFromDi()),
    providePrimeNG({ theme: { preset: Aura, options: { darkModeSelector: '.app-dark' } } }),
    Title,
    { provide: LOCALE_ID, useValue: 'en' },
    { provide: NgbDateAdapter, useClass: NgbDateDayjsAdapter },
    httpInterceptorProviders,
    { provide: TitleStrategy, useClass: AppPageTitleStrategy },
  ],
};
```

### Root Routes (app.routes.ts) — adapted for this project

```typescript
// Adapted from: aef-main/aef-main/src/app.routes.ts
export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./app/pages/login/login.component') },
  {
    path: '',
    loadComponent: () => import('./app/layout/component/main/app.layout').then(m => m.AppLayout),
    children: [
      { path: '', loadComponent: () => import('./app/pages/home/home.component'), canActivate: [UserRouteAccessService] },
      { path: 'accessdenied', loadComponent: () => import('./app/pages/error/access-denied.component') },
      { path: '404', loadComponent: () => import('./app/pages/error/not-found.component') },
      { path: 'error', loadComponent: () => import('./app/pages/error/error.component') },
      { path: 'entities', loadChildren: () => import('./app/pages/entities/entity.routes'), canActivate: [UserRouteAccessService] },
      { path: 'admin/security', loadChildren: () => import('./app/pages/admin/security/security.routes'), canActivate: [UserRouteAccessService], data: { authorities: ['ROLE_ADMIN'] } },
    ],
  },
  { path: '**', redirectTo: '' },
];
```

### Permission Matrix State Management (new pattern)

```typescript
// Conceptual — derived from CONTEXT.md D-23/D-24 contract
export default class PermissionMatrixComponent implements OnInit {
  private granted = new Map<string, number>(); // key -> permissionId for DELETE

  isGranted(target: string, action: string): boolean {
    return this.granted.has(`${target}:${action}`);
  }

  toggle(authorityName: string, targetType: string, target: string, action: string, checked: boolean): void {
    const key = `${target}:${action}`;
    if (checked) {
      this.permService.create({ authorityName, targetType, target, action, effect: 'GRANT' }).subscribe({
        next: perm => this.granted.set(key, perm.body!.id!),
        error: () => { this.messageService.add({ severity: 'error', summary: 'Save failed', detail: '...' }); }
      });
    } else {
      const id = this.granted.get(key);
      if (id == null) return;
      this.permService.delete(id).subscribe({
        next: () => this.granted.delete(key),
        error: () => { this.messageService.add({ severity: 'error', summary: 'Save failed', detail: '...' }); }
      });
    }
  }
}
```

### Attribute Visibility Guard (template pattern)

```html
<!-- D-17: Optional field renders nothing when absent -->
@if (org.budget !== undefined) {
  <td>{{ org.budget | number:'1.2-2' }}</td>
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Angular NgModule architecture | Standalone components | Angular 14+ (stable in 17+) | All components must be standalone — no feature NgModules |
| `CanActivate` class-based guard | `CanActivateFn` functional guard | Angular 15+ | `UserRouteAccessService` is already a `CanActivateFn` in aef-main |
| Karma/Jasmine test runner | `vitest` via `@angular/build:unit-test` | Angular CLI 21 | aef-main uses vitest; angular.json test builder is `@angular/build:unit-test` |
| `HttpClientModule` in NgModule | `provideHttpClient(withInterceptorsFromDi())` | Angular 15+ | Required for standalone; confirmed in aef-main `app.config.ts` |
| PrimeNG theme via CSS import | `providePrimeNG({ theme: { preset: Aura } })` | PrimeNG 17+ | Required for Aura preset; confirmed in aef-main |

---

## Open Questions

1. **RoleType enum exact names — RESOLVED**
   - Confirmed: `RoleType.java` defines exactly `RESOURCE` and `ROW_LEVEL`
   - Use these exact strings in the role create/edit dialog p-select options

2. **SecRowPolicy `policyType` enum values — RESOLVED**
   - Confirmed via `RowLevelPolicyProviderDbImpl.java`: supported values are `"SPECIFICATION"` and `"JPQL"`
   - `"JAVA"` is parsed but throws `AccessDeniedException` immediately — do not offer it in the UI
   - Row policy dialog p-select should offer `"SPECIFICATION"` and `"JPQL"` only

3. **Department/Employee foreign key IDs in forms**
   - What we know: Employee has `department` FK; Department has `organization` FK
   - What's unclear: How to populate the `p-select` dropdown for these FKs in create/update forms — the backend returns Map payloads, not typed entities with nested FK IDs
   - Recommendation: The update form should load a list of departments/organizations from the API and populate p-select from the result; map the `id` field from the returned map entries

4. **Catalog endpoint attribute enumeration — RESOLVED**
   - Confirmed: `SecuredEntityEntry` record fields are `entityClass`, `code`, `operations`, `fetchPlanCodes`, `jpqlAllowed` — NO attribute names stored in the catalog entry
   - The GAP-1 endpoint MUST enumerate attributes separately via `entityManager.getMetamodel().entity(entry.entityClass()).getAttributes()` returning `.stream().map(Attribute::getName).toList()` per entry

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | vitest 4.0.8 (via `@angular/build:unit-test`) |
| Config file | `angular.json` test builder — `@angular/build:unit-test` (no separate vitest config) |
| Quick run command | `cd frontend && ng test --watch=false` |
| Full suite command | `cd frontend && ng test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | Login with JWT flow — `AuthServerProvider.login()` posts to `/api/authenticate` and stores token | unit | `ng test --watch=false --include="**/auth-jwt.service.spec.ts"` | Wave 0 |
| AUTH-01 | `AccountService.identity()` fetches `/api/account` and caches | unit | `ng test --watch=false --include="**/account.service.spec.ts"` | Wave 0 |
| AUTH-01 | `UserRouteAccessService` redirects unauthenticated to `/login` | unit | `ng test --watch=false --include="**/user-route-access.service.spec.ts"` | Wave 0 |
| UI-03 | `AuthExpiredInterceptor` clears token and navigates to `/login` on 401 | unit | `ng test --watch=false --include="**/auth-expired.interceptor.spec.ts"` | Wave 0 |
| UI-03 | Error pages render for `/accessdenied`, `/404`, `/error` routes | component smoke | `ng test --watch=false --include="**/error/**/*.spec.ts"` | Wave 0 |
| ENT-03 | Organization list does not render `budget` column when field absent from response | component | `ng test --watch=false --include="**/organization-list.component.spec.ts"` | Wave 0 |
| ENT-03 | Employee list does not render `salary` column when field absent from response | component | `ng test --watch=false --include="**/employee-list.component.spec.ts"` | Wave 0 |
| UI-02 | Permission matrix `isGranted()` returns true only when key exists in granted map | unit | `ng test --watch=false --include="**/permission-matrix.component.spec.ts"` | Wave 0 |
| UI-02 | Wildcard `*` check disables individual attribute checkboxes | component | included in permission-matrix spec | Wave 0 |

### Sampling Rate

- **Per task commit:** `cd frontend && ng build --configuration=development` (compile check only — fast)
- **Per wave merge:** `cd frontend && ng test --watch=false`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

All test files below must be created before or alongside the features they cover:

- [ ] `frontend/src/app/core/auth/auth-jwt.service.spec.ts` — covers AUTH-01 login flow
- [ ] `frontend/src/app/core/auth/account.service.spec.ts` — covers AUTH-01 identity caching
- [ ] `frontend/src/app/core/auth/user-route-access.service.spec.ts` — covers UI-03 guard redirect
- [ ] `frontend/src/app/core/interceptor/auth-expired.interceptor.spec.ts` — covers UI-03 401 handling
- [ ] `frontend/src/app/pages/entities/organization/list/organization-list.component.spec.ts` — covers ENT-03 budget visibility
- [ ] `frontend/src/app/pages/entities/employee/list/employee-list.component.spec.ts` — covers ENT-03 salary visibility
- [ ] `frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.spec.ts` — covers UI-02 matrix state

---

## Sources

### Primary (HIGH confidence)

- Direct file reads — `aef-main/aef-main/src/app.config.ts`, `src/app.routes.ts`, `src/app/core/auth/*`, `src/app/layout/**/*`, `src/app/pages/entities/organization/**/*`, `tsconfig.json`, `angular.json`, `package.json`, `proxy.conf.json`
- Direct file reads — `src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java`, `SecRoleAdminResource.java`
- Direct file reads — `src/main/java/com/vn/core/service/dto/security/SecPermissionDTO.java`, `SecRoleDTO.java`, `SecRowPolicyDTO.java`
- Direct file reads — `src/main/java/com/vn/core/domain/Organization.java`, `Department.java`, `Employee.java`
- Direct file reads — `src/main/java/com/vn/core/security/catalog/SecuredEntityCatalog.java`, `MetamodelSecuredEntityCatalog.java`
- Direct file reads — `src/main/java/com/vn/core/web/rest/OrganizationResource.java`
- Direct file reads — `angapp/src/main/webapp/app/entities/sec-role/sec-role.model.ts`
- CONTEXT.md, UI-SPEC.md, REQUIREMENTS.md, STATE.md, ROADMAP.md

### Secondary (MEDIUM confidence)

- Angular 21 standalone component pattern — inferred from aef-main codebase structure (verified against actual code, no external docs consulted)
- PrimeNG 21 import patterns — confirmed from actual aef-main component imports

### Tertiary (LOW confidence)

- `RoleType.java` and `RowLevelPolicyProviderDbImpl.java` — read directly; previously flagged questions resolved to HIGH confidence

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — exact versions confirmed from package.json
- Architecture: HIGH — confirmed from actual file reads of aef-main
- Backend contracts: HIGH — confirmed from actual Java source reads
- Permission matrix pattern: MEDIUM — design is confirmed from CONTEXT.md; implementation pattern is new (no existing code to copy)
- Pitfalls: HIGH — derived from reading actual code paths

**Research date:** 2026-03-22
**Valid until:** 2026-04-22 (stable Angular/PrimeNG stack; backend contracts locked by Phase 4)
