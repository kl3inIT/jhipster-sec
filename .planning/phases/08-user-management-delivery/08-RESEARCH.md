# Phase 8: User Management Delivery - Research

**Researched:** 2026-03-25
**Domain:** Standalone Angular admin user-management delivery on preserved JHipster/Spring admin-user contracts
**Confidence:** MEDIUM

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### User List Workspace
- **D-01:** The main `/admin/users` screen uses a dense admin table, not a master-detail split list and not a minimal card list.
- **D-02:** The default grid columns are `login`, full name, `email`, activation status, assigned roles, last modified, and row actions.
- **D-03:** Opening a user from the table lands on a dedicated user detail route before editing; edit remains an explicit action.
- **D-04:** Primary row actions stay inline in the table rather than moving into an overflow menu.
- **D-05:** The user detail and edit surface follows an enterprise split-page layout: user fields on the left and the role list on the right.
- **D-06:** That split-page layout is also the primary detail view. It stays read-only until the admin explicitly enters edit mode.

#### Search And Browse Controls
- **D-07:** The list page uses one prominent search box above the table as the default browse control.
- **D-08:** Search must match `login`, `email`, and the user's name rather than only one identifier.
- **D-09:** Searching keeps the normal paginated, sortable admin-table behavior instead of switching into a separate lookup mode.
- **D-10:** Search updates as the admin types, using a short debounce rather than an explicit submit button.

#### Role Assignment Flow
- **D-11:** Role assignment lives directly inside the user create/edit page on the right-side panel rather than a separate access subpage.
- **D-12:** Roles are assigned through a checkbox table, not a picklist or multiselect dropdown.
- **D-13:** Each role row shows the role code together with a human-friendly label or description.
- **D-14:** The read-only detail page still renders the full role table on the right in a disabled state so admins can inspect access before choosing Edit.

#### Action Safety
- **D-15:** Activation and deactivation are inline list-row actions.
- **D-16:** Activation changes do not require an extra confirmation dialog; visible state change and success or error feedback are sufficient.
- **D-17:** Delete is available from the list row, but it must remain guarded by a confirmation dialog.
- **D-18:** The current admin user row must disable self-deactivate and self-delete actions inline.

### Claude's Discretion
- Exact PrimeNG table density, badges, spacing, and icon choices for the user list and split-page detail surface
- Exact debounce duration for the search box
- Exact read-only affordance styling for the disabled role table on the detail page
- Exact inline feedback mix between translated toasts and inline status messages, as long as it stays fast and enterprise-oriented

### Deferred Ideas (OUT OF SCOPE)
None - discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| UMGT-01 | Admin can browse, search, sort, and open existing users from `frontend/` using the preserved backend admin API. | Use a route-first PrimeNG list/detail workflow, preserve `/api/admin/users` pagination and allowed sort keys, and add an optional `query` parameter to the existing endpoint so search remains paginated and sortable. |
| UMGT-02 | Admin can create, edit, activate, deactivate, and delete users from `frontend/` without breaking current validation or contract behavior. | Mirror `AdminUserDTO` validation in a reactive form, keep create/update/delete on the existing admin-user endpoints, use inline activation toggles, and surface backend RFC7807 validation/errors through the existing shared error utilities. |
| UMGT-03 | Admin can assign and update a user's roles or authorities from the frontend, and the persisted assignment affects downstream access decisions. | Feed the right-side role table from `GET /api/authorities`, bind selections to `authorities: string[]`, and keep update payloads aligned with the preserved backend authority-mapping behavior in `UserService`. |
</phase_requirements>

## Summary

Phase 8 should be planned as a focused delivery of the donor user-management behavior into the new `frontend/` admin shell, not as a redesign of backend contracts. The workspace already has the route skeleton, model, service, navigation node, request utilities, translation baseline, and reusable admin patterns needed for list, detail, and edit flows. The right planning move is to replace the placeholder route with real PrimeNG list/detail/update components that follow the same route-first, state-preserving structure already used elsewhere in `frontend/`.

The main planning hazard is search. The locked Phase 8 decisions require a single search box that keeps normal paginated and sortable table behavior, but the current backend `GET /api/admin/users` endpoint only accepts `Pageable`. The frontend already has a shared `SearchWithPagination` request shape and a generic query-param builder, so the least disruptive implementation is to extend the existing endpoint with an optional `query` parameter instead of introducing a parallel search API. Everything else in the phase can remain on the preserved `/api/admin/users` and `/api/authorities` contracts.

The donor `angapp` code is still the best behavior reference for field set, route shape, inline activation, and authority assignment, but it must be adapted into the current Angular 21 + PrimeNG admin patterns. Do not copy donor Bootstrap or ng-bootstrap UI. Reuse the existing PrimeNG card/table/toast/confirm stack, `WorkspaceContextService`, and shared HTTP error handling so user management behaves like the rest of the migrated admin workspace.

**Primary recommendation:** Implement Phase 8 as a route-first PrimeNG admin workspace on the existing `/admin/users` routes, preserve current create/update/delete/detail contracts, and add only one backend seam: an optional `query` parameter on `GET /api/admin/users` so search stays server-backed, paginated, and sortable.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Angular standalone router, forms, signals | 21.2.x | Feature routing, resolver-fed detail/edit routes, reactive forms, local UI state | Already the workspace baseline in `frontend/`; aligns with project instructions and current Angular docs. |
| PrimeNG | 21.1.3 | Dense admin table, card layout, checkbox role table, toast feedback, confirm dialog, skeletons | Already the active frontend design system and explicitly referenced by the Phase 8 UI contract. |
| `@ngx-translate/core` | 17.0.0 | Translated labels, validation messages, feedback toasts, authority labels | Already used throughout `frontend/`; required to keep admin UX consistent and bilingual. |
| Preserved admin-user API (`/api/admin/users`, `/api/authorities`) | Spring Boot 4.0.3 / JHipster 9 backend | User browse/detail/create/update/delete and authority lookup | The phase goal is delivery on top of the preserved backend contract, not a new API surface. |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| RxJS | Bundled with Angular 21 | Search debounce, request orchestration, save finalization | Search-as-you-type, loading states, save flows, and route-triggered refreshes. |
| `WorkspaceContextService` | Repo-local | Preserve page, sort, and filter state between list/detail/edit | Any navigation away from `/admin/users` that must restore the same list state on return. |
| `createRequestOption` + `SearchWithPagination` | Repo-local | Serialize `query`, `page`, `size`, and `sort` without ad hoc `HttpParams` code | All list loads and search refreshes. |
| PrimeNG `MessageService` + `ConfirmationService` | 21.1.3 | Fast translated feedback and guarded destructive actions | Save feedback, activation/deactivation results, and delete confirmation. |
| Vitest + Playwright | 4.0.8 / 1.58.2 | Component/integration testing and browser smoke coverage | Required to close Nyquist validation gaps for list, detail, CRUD, and role assignment flows. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Optional `query` on `GET /api/admin/users` | A separate search endpoint | A separate endpoint adds API sprawl and duplicate list semantics; the optional query parameter preserves the current contract shape and existing callers. |
| PrimeNG checkbox role table | MultiSelect or PickList | Faster to wire, but it breaks the locked split-page inspection/edit pattern and makes role review worse in read-only mode. |
| `WorkspaceContextService` | Per-component manual query-param parsing and state caches | Manual state handling duplicates an existing project seam and makes back navigation inconsistent across admin pages. |

**Installation:** No new foundational packages are required for this phase. Use the existing frontend workspace dependencies.

```bash
cd frontend
npm install
```

**Version verification:** Before planning against package behavior, verify current registry versions.

```bash
npm view @angular/core version
npm view primeng version
npm view @ngx-translate/core version
npm view vitest version
npm view @playwright/test version
```

Verified on 2026-03-25:
- `@angular/core` latest `21.2.5` (published 2026-03-18). Workspace is already on Angular `21.2.x`; no phase-scoped upgrade is needed.
- `primeng` latest `21.1.3` (published 2026-03-04). Workspace is already current.
- `@ngx-translate/core` latest `17.0.0` (published 2025-07-28). Workspace is already current.
- `vitest` latest `4.1.1` (published 2026-03-23). Workspace uses `4.0.8`; keep the pinned workspace version for this phase to avoid unrelated toolchain churn.
- `@playwright/test` latest `1.58.2` (published 2026-02-06). Workspace is already current.

## Architecture Patterns

### Recommended Project Structure
```text
frontend/src/app/pages/admin/user-management/
+-- list/                    # Dense admin browse table, search, paging, sort, inline actions
+-- detail/                  # Read-only split-page detail view with disabled role table
+-- update/                  # Create/edit split-page form with live authority selection
+-- shared/                  # Authority label helpers, query-state helpers, shared view models
+-- service/                 # Backend contract client for users and authorities
+-- user-management.model.ts # Preserved user payload shape
+-- user-management.routes.ts
+-- *.spec.ts                # Route, service, and component-level coverage
```

### Pattern 1: Route-First Admin Workspace
**What:** Keep list, detail, create, and edit as explicit routes under `/admin/users`, with resolver-fed user loading for detail/edit and route metadata preserved for shell navigation, breadcrumbs, and access denial behavior.

**When to use:** For every primary user-management workflow. Do not collapse the phase into modal-only CRUD.

**Example:**
```typescript
// Source: https://angular.dev/guide/routing/data-resolvers
// Source: frontend/src/app/pages/admin/user-management/user-management.routes.ts
export const userManagementRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/user-management-list.component').then(m => m.UserManagementListComponent),
  },
  {
    path: ':login/view',
    resolve: { user: userManagementResolve },
    loadComponent: () => import('./detail/user-management-detail.component').then(m => m.UserManagementDetailComponent),
  },
  {
    path: 'new',
    loadComponent: () => import('./update/user-management-update.component').then(m => m.UserManagementUpdateComponent),
  },
  {
    path: ':login/edit',
    resolve: { user: userManagementResolve },
    loadComponent: () => import('./update/user-management-update.component').then(m => m.UserManagementUpdateComponent),
  },
];
```

### Pattern 2: Server-Backed Search Through Shared Request Utilities
**What:** Extend the existing frontend query method from `Pagination` to `SearchWithPagination`, and send `query`, `page`, `size`, and `sort` via `createRequestOption`. The backend should treat `query` as optional and preserve the current response body and pagination header shape.

**When to use:** All list loads, search refreshes, and sort changes on `/admin/users`.

**Example:**
```typescript
// Source: frontend/src/app/core/request/request.model.ts
// Source: frontend/src/app/core/request/request-util.ts
// Source: frontend/src/app/pages/admin/user-management/service/user-management.service.ts
query(req?: SearchWithPagination): Observable<HttpResponse<IUser[]>> {
  return this.http.get<IUser[]>(this.resourceUrl, {
    params: createRequestOption(req),
    observe: 'response',
  });
}
```

### Pattern 3: Split Detail/Edit Surface with One Shared Field Model
**What:** Use the same field layout for detail and edit. Detail mode renders the left-side user fields read-only and the right-side authority table disabled; edit mode enables the same surface instead of navigating to a different information architecture.

**When to use:** `:login/view`, `:login/edit`, and `new`.

**Example:**
```typescript
// Source: https://angular.dev/guide/forms/reactive-forms
// Source: frontend/src/app.config.ts
readonly user = input<IUser | null>(null);
readonly editMode = signal(false);

protected readonly form = this.formBuilder.nonNullable.group({
  login: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(50)]],
  firstName: ['', [Validators.maxLength(50)]],
  lastName: ['', [Validators.maxLength(50)]],
  email: ['', [Validators.required, Validators.email, Validators.minLength(5), Validators.maxLength(254)]],
  activated: true,
  langKey: ['en', [Validators.required]],
  authorities: this.formBuilder.nonNullable.control<string[]>([]),
});
```

### Pattern 4: List Context Preservation
**What:** Store current list state before navigating to detail or edit, then restore page, sort, and search when returning after save, cancel, or back.

**When to use:** Any transition from `/admin/users` to `:login/view`, `new`, or `:login/edit`.

**Example:**
```typescript
// Source: frontend/src/app/pages/entities/shared/service/workspace-context.service.ts
this.workspaceContext.store('security.users', {
  page: this.page(),
  sort: this.sortState(),
  filters: { query: this.searchControl.value ?? '' },
});
```

### Anti-Patterns to Avoid
- **Client-side-only search on the current page:** It looks functional in development, but it fails `D-08` and `D-09` because the search is not global and stops matching backend pagination.
- **A new dedicated search endpoint with a different payload shape:** It adds unnecessary API drift when an optional `query` parameter on the existing endpoint is enough.
- **Copying donor Bootstrap or ng-bootstrap components into `frontend/`:** The donor behavior is useful; the donor UI stack is not. This workspace is PrimeNG-first.
- **Sending unsupported sort keys from the table:** `UserResource` only allows a fixed property set. Table column names must map exactly to those backend sort properties.
- **Losing list state on view/edit/save:** Failing to reuse `WorkspaceContextService` makes the admin flow feel broken and forces re-searching after every edit.
- **Allowing self-deactivate or self-delete:** The current admin row must disable those actions inline, not merely fail after the click.
- **Hiding backend validation semantics behind generic UI errors:** Reuse the shared error helpers so current RFC7807 and message-key behavior remains visible.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Query-string building for search/paging/sort | Custom `HttpParams` code in each component | `SearchWithPagination` + `createRequestOption` | The shared request seam already supports `query`, arrays, and empty-value filtering. |
| Back navigation state | Ad hoc local storage or per-component caches | `WorkspaceContextService` | It is already the project standard for list/detail/edit round-tripping. |
| Delete confirmation dialogs | Feature-specific modal plumbing | PrimeNG `ConfirmationService` + `p-confirmDialog` | Accessible, translated, and consistent with the rest of the admin UI. |
| Save and failure feedback | Inline string manipulation or `console` logging | PrimeNG `MessageService` + shared HTTP error helpers | Keeps translated success/error behavior aligned with existing pages. |
| Search debounce | Manual `setTimeout` lifecycle bookkeeping | Reactive control `valueChanges` with debounce and distinct change handling | Easier cancellation, clearer tests, and fewer stale-request bugs. |
| Authority label rendering | Hardcoded display strings scattered in templates | Translation keys or a feature-local authority label map | Keeps role labels localized without changing the preserved authority API. |

**Key insight:** The expensive mistakes in this phase are not visual. They come from bypassing existing request, error, and navigation seams and accidentally changing contract semantics while implementing a fairly standard admin table and form workflow.

## Common Pitfalls

### Pitfall 1: Search Looks Global but Only Filters the Loaded Page
**What goes wrong:** The UI appears to support search, but it only filters the current page of results in memory.

**Why it happens:** The current backend endpoint has paging and sort support but no search parameter, so it is tempting to fake search in the component.

**How to avoid:** Plan an optional `query` parameter on `GET /api/admin/users` and keep search server-backed.

**Warning signs:** Search results change when the admin flips pages, and matching users outside the current page never appear.

### Pitfall 2: Sort Keys Drift from Backend-Allowed Properties
**What goes wrong:** Sort clicks yield 400s or silently incorrect ordering.

**Why it happens:** PrimeNG column field names or computed display fields are sent directly instead of mapping to backend sort keys such as `login`, `email`, or `lastModifiedDate`.

**How to avoid:** Maintain an explicit UI-column-to-backend-sort mapping and verify it against `UserResource`.

**Warning signs:** Sorting works for some columns but not others, especially full name or roles.

### Pitfall 3: The Detail/Edit Experience Regresses into Two Different Layouts
**What goes wrong:** Detail and edit diverge into separate structures, and the role table disappears from detail mode.

**Why it happens:** Teams often build edit first and then bolt on a lightweight detail page later.

**How to avoid:** Plan one shared split-page field model up front, with disabled controls and disabled role checkboxes in read-only mode.

**Warning signs:** The detail page lacks authority visibility or uses a different field grouping than edit.

### Pitfall 4: Self-Safety Rules Are Checked Too Late
**What goes wrong:** The current admin can click deactivate or delete on their own row and only sees an error after the request.

**Why it happens:** The UI relies only on backend protection instead of reflecting the locked inline safety rule.

**How to avoid:** Compare each row login with the current account login and disable the relevant inline actions before click.

**Warning signs:** The action buttons remain enabled on the current user row.

### Pitfall 5: Role Labels Are Unclear or Inconsistent
**What goes wrong:** The role table shows only raw authority codes in some places and friendly labels elsewhere.

**Why it happens:** `/api/authorities` returns names only, so the label concern gets deferred until late.

**How to avoid:** Decide in planning that role descriptions come from translation keys keyed by authority name unless backend metadata already exists.

**Warning signs:** Mockups show human-friendly labels, but the implementation plan does not name a source for them.

### Pitfall 6: Existing Validation Behavior Gets Flattened into Generic UI Errors
**What goes wrong:** Create and update flows stop surfacing duplicate-login, duplicate-email, or field-level validation errors in a meaningful way.

**Why it happens:** Custom save handlers bypass the project's shared HTTP error helpers.

**How to avoid:** Route all save failures through the existing translated error handling utilities and keep frontend validators aligned with `AdminUserDTO`.

**Warning signs:** The UI shows a generic failure toast for every 400 response.

## Code Examples

Verified patterns from official sources and current project seams:

### Resolver-Fed Detail/Edit Loading
```typescript
// Source: https://angular.dev/guide/routing/data-resolvers
// Source: frontend/src/app/pages/admin/user-management/user-management.routes.ts
export const userManagementResolve: ResolveFn<IUser | null> = route => {
  const login = route.params.login;
  if (!login) {
    return of(null);
  }
  return inject(UserManagementService).find(login);
};
```

### Reactive Form That Mirrors Backend Validation
```typescript
// Source: https://angular.dev/guide/forms/reactive-forms
// Source: src/main/java/com/vn/core/service/dto/AdminUserDTO.java
protected readonly form = this.formBuilder.nonNullable.group({
  login: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(50)]],
  firstName: ['', [Validators.maxLength(50)]],
  lastName: ['', [Validators.maxLength(50)]],
  email: ['', [Validators.required, Validators.email, Validators.minLength(5), Validators.maxLength(254)]],
  activated: true,
  langKey: ['en', [Validators.required]],
  authorities: this.formBuilder.nonNullable.control<string[]>([]),
});
```

### Guarded Delete with Shared Feedback
```typescript
// Source: https://primeng.org/confirmdialog
// Source: https://primeng.org/toast
// Source: frontend/src/app/shared/error/http-error.utils.ts
this.confirmationService.confirm({
  message: this.translate.instant('userManagement.delete.question', { login: user.login }),
  accept: () => {
    this.userManagementService.delete(user.login).subscribe({
      next: () => addTranslatedMessage(this.messageService, 'success', 'userManagement.deleted', { login: user.login }),
      error: error => handleHttpError({ error, messageService: this.messageService, translateService: this.translate }),
    });
  },
});
```

### Shared Search/Paging Query Serialization
```typescript
// Source: frontend/src/app/core/request/request.model.ts
// Source: frontend/src/app/core/request/request-util.ts
this.userManagementService.query({
  query: this.searchControl.value?.trim() ?? '',
  page: this.page(),
  size: this.itemsPerPage,
  sort: this.sortState(),
});
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Donor JHipster Bootstrap/ng-bootstrap admin screens in `angapp` | Standalone Angular 21 + PrimeNG admin pages in `frontend/` | Frontend migration phases 5-7, current workspace state on 2026-03-25 | Reuse donor behavior and field semantics, but implement them with the current design system and admin shell conventions. |
| Manual route subscriptions for resolved data | Resolver-driven routes with `withComponentInputBinding()` enabled in `frontend/src/app.config.ts` | Enabled in the current frontend app configuration | Detail and edit components can stay thinner and more deterministic. |
| Minimal placeholder `/admin/users` route | Full list/detail/edit admin workspace with preserved route metadata and list-context restoration | Phase 8 target state | Replace the placeholder rather than creating parallel routes or shell entries. |

**Deprecated/outdated:**
- Donor delete-dialog plumbing through ng-bootstrap is outdated for this workspace; use PrimeNG confirmation services instead.
- Client-side filtering of only the loaded page is outdated for this requirement set; search must remain compatible with backend paging and sorting.

## Open Questions

1. **What exact backend search semantics should `query` implement?**
   - What we know: The frontend already has `SearchWithPagination`, the phase requires a single search box, and the current backend endpoint only exposes pageable browse.
   - What's unclear: Whether search should be simple case-insensitive OR matching across `login`, `email`, `firstName`, and `lastName`, or whether name search should tokenize and normalize more aggressively.
   - Recommendation: Plan a backward-compatible optional `query` parameter on `GET /api/admin/users` that performs case-insensitive OR matching across `login`, `email`, `firstName`, and `lastName`, while keeping the same body and pagination headers.

2. **Where should the human-friendly role label or description come from?**
   - What we know: `/api/authorities` returns authority names only, but `D-13` requires each role row to show the code plus a human-friendly label or description.
   - What's unclear: Whether the product already has a canonical server-side role-metadata source or whether labels should remain a frontend translation concern.
   - Recommendation: Default to frontend translation keys derived from authority names unless an existing backend metadata source already exists outside this phase.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Vitest `4.0.8` via Angular `@angular/build:unit-test`, plus Playwright `1.58.2` for browser smoke coverage |
| Config file | `frontend/angular.json`, `frontend/tsconfig.spec.json`, `frontend/playwright.config.ts` |
| Quick run command | `npm run test -- --watch=false --include src/app/pages/admin/user-management/**/*.spec.ts` |
| Full suite command | `npm run test -- --watch=false` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| UMGT-01 | List, search, sort, paginate, and open user detail while preserving list context | component/integration + e2e smoke | `npm run test -- --watch=false --include src/app/pages/admin/user-management/list/**/*.spec.ts` | No - Wave 0 |
| UMGT-02 | Create, edit, activate, deactivate, and delete users while preserving backend validation behavior | component/integration + e2e smoke | `npm run test -- --watch=false --include src/app/pages/admin/user-management/update/**/*.spec.ts` | No - Wave 0 |
| UMGT-03 | View, assign, and persist authority changes from the split-page role table | component/integration + e2e smoke | `npm run test -- --watch=false --include src/app/pages/admin/user-management/update/**/*.spec.ts` | No - Wave 0 |

### Sampling Rate
- **Per task commit:** `npm run test -- --watch=false --include src/app/pages/admin/user-management/**/*.spec.ts`
- **Per wave merge:** `npm run test -- --watch=false`
- **Phase gate:** Full unit suite green and a targeted Playwright smoke such as `npm run e2e -- --project=chromium --grep "user management"` once the spec exists, before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `frontend/src/app/pages/admin/user-management/list/user-management-list.component.spec.ts` - covers UMGT-01 search debounce, paging, sort mapping, inline activation, and list-context storage
- [ ] `frontend/src/app/pages/admin/user-management/detail/user-management-detail.component.spec.ts` - covers UMGT-01 detail route rendering, disabled role table, and back navigation restoration
- [ ] `frontend/src/app/pages/admin/user-management/update/user-management-update.component.spec.ts` - covers UMGT-02 and UMGT-03 validation mapping, authority checkbox selection, save success, and save failure handling
- [ ] `frontend/e2e/user-management.spec.ts` - covers end-to-end browse, create/edit, activation toggle, delete confirmation, and authority assignment smoke
- [ ] Existing `user-management.routes.spec.ts` and `service/user-management.service.spec.ts` should be retained, but they are insufficient as primary phase validation because they do not cover page behavior

## Sources

### Primary (HIGH confidence)
- `frontend/src/app/pages/admin/user-management/user-management.routes.ts` - current route skeleton, resolver seam, shell metadata, and breadcrumb integration
- `frontend/src/app/pages/admin/user-management/service/user-management.service.ts` - current frontend contract for `/api/admin/users` and `/api/authorities`
- `frontend/src/app/core/request/request.model.ts` - confirms `SearchWithPagination` already exists in the shared frontend request layer
- `frontend/src/app/core/request/request-util.ts` - confirms the shared request serializer already supports optional `query`
- `frontend/src/app/pages/entities/shared/service/workspace-context.service.ts` - project-standard list state preservation seam
- `src/main/java/com/vn/core/web/rest/UserResource.java` - preserved browse/detail/create/update/delete contract and allowed sort properties
- `src/main/java/com/vn/core/service/UserService.java` - canonical create/update/delete/authority-mapping behavior
- `src/main/java/com/vn/core/service/dto/AdminUserDTO.java` - canonical frontend validation mirror for admin user payloads
- `src/main/java/com/vn/core/web/rest/AuthorityResource.java` - canonical authority lookup contract
- https://angular.dev/guide/forms/reactive-forms - reactive form pattern verification
- https://angular.dev/guide/routing/data-resolvers - resolver pattern verification
- https://primeng.org/table - PrimeNG table behavior and accessibility reference
- https://primeng.org/confirmdialog - PrimeNG destructive-action confirmation pattern
- https://primeng.org/toast - PrimeNG feedback/toast pattern
- `npm view @angular/core version`, `npm view primeng version`, `npm view @ngx-translate/core version`, `npm view vitest version`, `npm view @playwright/test version` - current version verification on 2026-03-25

### Secondary (MEDIUM confidence)
- `.planning/phases/08-user-management-delivery/08-UI-SPEC.md` - locked UI contract, component choices, and enterprise split-page layout target
- `angapp/src/main/webapp/app/admin/user-management/` - donor behavior reference for browse/detail/update/delete flows and field semantics
- `frontend/src/app/pages/entities/organization/` - current PrimeNG list/detail/update patterns already accepted in this workspace
- `frontend/src/app/pages/admin/security/roles/list/role-list.component.ts` - current admin security table and feedback pattern

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - grounded in current workspace dependencies, verified registry versions, and official Angular/PrimeNG documentation
- Architecture: MEDIUM - current project seams are clear, but the required search behavior depends on a small backend contract extension that is not implemented yet
- Pitfalls: HIGH - derived directly from locked phase decisions, preserved backend constraints, donor behavior, and current frontend admin patterns

**Research date:** 2026-03-25
**Valid until:** 2026-04-08
