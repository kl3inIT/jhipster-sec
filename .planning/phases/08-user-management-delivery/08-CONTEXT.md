# Phase 8: User Management Delivery - Context

**Gathered:** 2026-03-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver the full frontend admin user-management experience in `frontend/` on top of the preserved backend admin-user and authority contracts. This phase covers browse, search, sort, open, create, edit, activate, deactivate, delete, and role assignment flows for users. It does not add non-user-management admin utilities, and it does not broaden into the Phase 9 responsiveness/performance hardening scope.

</domain>

<decisions>
## Implementation Decisions

### User List Workspace
- **D-01:** The main `/admin/users` screen uses a dense admin table, not a master-detail split list and not a minimal card list.
- **D-02:** The default grid columns are `login`, full name, `email`, activation status, assigned roles, last modified, and row actions.
- **D-03:** Opening a user from the table lands on a dedicated user detail route before editing; edit remains an explicit action.
- **D-04:** Primary row actions stay inline in the table rather than moving into an overflow menu.
- **D-05:** The user detail and edit surface follows an enterprise split-page layout: user fields on the left and the role list on the right.
- **D-06:** That split-page layout is also the primary detail view. It stays read-only until the admin explicitly enters edit mode.

### Search And Browse Controls
- **D-07:** The list page uses one prominent search box above the table as the default browse control.
- **D-08:** Search must match `login`, `email`, and the user's name rather than only one identifier.
- **D-09:** Searching keeps the normal paginated, sortable admin-table behavior instead of switching into a separate lookup mode.
- **D-10:** Search updates as the admin types, using a short debounce rather than an explicit submit button.

### Role Assignment Flow
- **D-11:** Role assignment lives directly inside the user create/edit page on the right-side panel rather than a separate access subpage.
- **D-12:** Roles are assigned through a checkbox table, not a picklist or multiselect dropdown.
- **D-13:** Each role row shows the role code together with a human-friendly label or description.
- **D-14:** The read-only detail page still renders the full role table on the right in a disabled state so admins can inspect access before choosing Edit.

### Action Safety
- **D-15:** Activation and deactivation are inline list-row actions.
- **D-16:** Activation changes do not require an extra confirmation dialog; visible state change and success or error feedback are sufficient.
- **D-17:** Delete is available from the list row, but it must remain guarded by a confirmation dialog.
- **D-18:** The current admin user row must disable self-deactivate and self-delete actions inline.

### the agent's Discretion
- Exact PrimeNG table density, badges, spacing, and icon choices for the user list and split-page detail surface
- Exact debounce duration for the search box
- Exact read-only affordance styling for the disabled role table on the detail page
- Exact inline feedback mix between translated toasts and inline status messages, as long as it stays fast and enterprise-oriented

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase requirements and milestone context
- `.planning/ROADMAP.md` - `Phase 8: User Management Delivery` goal, dependency, and success criteria
- `.planning/REQUIREMENTS.md` - `UMGT-01`, `UMGT-02`, and `UMGT-03` define the required browse, CRUD, and role-assignment outcomes
- `.planning/PROJECT.md` - `v1.1 Enterprise Admin Experience` context plus the locked brownfield and donor constraints for admin-user migration

### Prior phase decisions that carry forward
- `.planning/phases/05-standalone-frontend-delivery/05-CONTEXT.md` - standalone Angular structure, donor strategy, and the deferred user-management scope that Phase 8 now fulfills
- `.planning/phases/06-frontend-parity-foundation/06-CONTEXT.md` - migrated user-management model, route, service, translation baseline, and shared request helpers already staged for this phase
- `.planning/phases/07-enterprise-navigation-shell/07-CONTEXT.md` - locked `security.users` shell placement, list-first route pattern, route denial behavior, and list-context preservation expectations

### Current frontend integration seams
- `frontend/src/app/pages/admin/user-management/user-management.routes.ts` - current route skeleton, resolver, route metadata, and breadcrumb keys to preserve
- `frontend/src/app/pages/admin/user-management/service/user-management.service.ts` - preserved `/api/admin/users` and `/api/authorities` service contract already wired into `frontend/`
- `frontend/src/app/pages/admin/user-management/user-management.model.ts` - current frontend user payload shape and audit-field availability
- `frontend/src/app/pages/entities/shared/service/workspace-context.service.ts` - existing list-context preservation utility to reuse for returning from detail/edit to the list
- `frontend/src/app/layout/navigation/navigation-registry.ts` - locked `security.users` node id, shell ordering, and route prefix
- `frontend/src/app/pages/admin/security/roles/list/role-list.component.ts` - current PrimeNG security-admin list pattern for table styling, inline actions, feedback, and translated admin interactions
- `frontend/src/app/core/request/request.model.ts` - shared request shape already includes `SearchWithPagination`
- `frontend/src/app/core/request/request-util.ts` - shared query-param builder that should remain the frontend request seam

### Donor user-management references
- `angapp/src/main/webapp/app/admin/user-management/user-management.route.ts` - donor route structure for list, detail, new, and edit
- `angapp/src/main/webapp/app/admin/user-management/service/user-management.service.ts` - donor admin user-management service behavior and authority lookup pattern
- `angapp/src/main/webapp/app/admin/user-management/list/user-management.component.ts` - donor dense admin-table browse flow, paging, sort, and inline activation behavior
- `angapp/src/main/webapp/app/admin/user-management/list/user-management.component.html` - donor column set, inline action arrangement, and browse-page semantics
- `angapp/src/main/webapp/app/admin/user-management/detail/user-management-detail.component.ts` - donor detail route contract
- `angapp/src/main/webapp/app/admin/user-management/detail/user-management-detail.component.html` - donor detail information baseline
- `angapp/src/main/webapp/app/admin/user-management/update/user-management-update.component.ts` - donor edit/create form behavior and authority loading
- `angapp/src/main/webapp/app/admin/user-management/update/user-management-update.component.html` - donor field set and validation baseline
- `angapp/src/main/webapp/app/admin/user-management/delete/user-management-delete-dialog.component.ts` - donor delete confirmation behavior

### Preserved backend contracts
- `src/main/java/com/vn/core/web/rest/UserResource.java` - admin browse, create, update, get-by-login, and delete endpoints plus allowed sort properties
- `src/main/java/com/vn/core/service/UserService.java` - backend user-management behavior for create, update, delete, activation persistence, and authority mapping
- `src/main/java/com/vn/core/service/dto/AdminUserDTO.java` - canonical admin user payload shape including `authorities`
- `src/main/java/com/vn/core/web/rest/AuthorityResource.java` - authority list endpoint used to populate the role-assignment table

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/app/pages/admin/user-management/user-management.routes.ts`, `user-management.service.ts`, and `user-management.model.ts` already provide the route, resolver, and backend contract baseline for Phase 8.
- `frontend/src/app/pages/entities/shared/service/workspace-context.service.ts` already preserves list query params across list, detail, and edit flows for entity workspaces; the same pattern should carry into `/admin/users`.
- `frontend/src/app/pages/admin/security/roles/list/role-list.component.ts` and the current security-admin screens already establish the PrimeNG card + table + inline-action style to follow.
- `angapp/src/main/webapp/app/admin/user-management/` contains the donor browse, detail, update, and delete flows that should be adapted rather than reinvented.

### Established Patterns
- The shell is list-first and route-based. Detail and edit stay as subordinate routes rather than modal-only workflows.
- Admin pages in `frontend/` already rely on translated PrimeNG feedback, inline actions, and route metadata tied to navigation node ids.
- The current backend user admin API already supports pagination and approved sorting on `/api/admin/users`; any search support must preserve that public contract shape.
- Authority assignment already rides on `/api/authorities` and the `AdminUserDTO.authorities` field rather than a separate user-role API.

### Integration Points
- Replace the placeholder component at `/admin/users` with a real browse page while preserving `security.users` route metadata, breadcrumbs, and access control.
- Apply `WorkspaceContextService` to the user list so detail and edit routes can return admins to the same page, sort, and search state.
- Reuse the shared request model and request util for paginated search requests instead of inventing a parallel query layer.
- Keep create and update flows mapped to `AdminUserDTO` and feed the right-side role table from `/api/authorities`.

</code_context>

<specifics>
## Specific Ideas

- The user wants the primary detail/edit experience to resemble the provided enterprise reference: a wide page with user information on the left and the role grid on the right.
- The detail view should expose access context immediately, so the role table remains visible even before Edit is activated.
- The role-assignment surface should feel like an admin permission list, not a casual multiselect control.
- The browse workspace should optimize for fast admin operations: dense rows, inline status changes, inline view/edit/delete actions, and search-as-you-type.

</specifics>

<deferred>
## Deferred Ideas

None - discussion stayed within phase scope.

</deferred>

---

*Phase: 08-user-management-delivery*
*Context gathered: 2026-03-25*
