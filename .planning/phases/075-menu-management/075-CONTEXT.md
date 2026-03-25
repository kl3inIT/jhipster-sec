# Phase 7.5: Menu Management - Context

**Gathered:** 2026-03-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver the admin UI and backend endpoints for managing menu node definitions (SecMenuDefinition) and assigning menu access permissions per role (SecMenuPermission). This phase covers full CRUD for menu node catalog entries, role-based menu permission assignment, backend-driven dynamic node rendering, and the necessary admin REST endpoints. It does not deliver user management (Phase 8) or any changes to the current-user navigation grant read path.

</domain>

<decisions>
## Implementation Decisions

### Permission Assignment UI
- **D-01:** Menu permission assignment is added to the existing role screen as a "Menu Access" tab or section — role-first approach, consistent with the established permission matrix pattern.
- **D-02:** Menu nodes are displayed in a tree/hierarchy view matching the navigation registry structure (grouped by section: Home, Entities, Security), not a flat list.

### Menu Catalog Management
- **D-03:** `SecMenuDefinition` supports full admin CRUD — admins can create, edit, and delete menu node entries, not just view them.
- **D-04:** The primary use case for admin-created nodes is dynamic menu items: admin registers ad-hoc entries (e.g. external links, custom dashboards) that the frontend renders at runtime from the backend catalog.
- **D-05:** The frontend renders dynamic nodes via backend-driven rendering — it fetches the full menu catalog from the backend at runtime and merges dynamically-created nodes alongside the static registry entries. The backend becomes the source of truth for dynamic entries.
- **D-06:** Menu definition management lives at a dedicated page: `/admin/security/menu-definitions`. List of all `SecMenuDefinition` entries with Create/Edit/Delete actions. Separate from the permission assignment screen.

### Default Access Model
- **D-07:** No `SecMenuPermission` row = **deny**. Admins must explicitly grant each menu node per role. Secure-by-default, consistent with Phase 7 deny-wins resolution.

### Backend Admin Endpoints
- **D-08:** A new `AdminMenuPermissionResource` provides CRUD for `SecMenuPermission` (assign/revoke per role+menu). Secured to `ROLE_ADMIN`.
- **D-09:** A sync endpoint seeds/refreshes `SecMenuDefinition` from the frontend navigation registry. Admins can trigger this to add newly-registered frontend nodes to the catalog.
- **D-10:** Existing `MenuPermissionResource` (current-user GET) is not modified — admin endpoints live in a separate resource.

### Claude's Discretion
- Exact form layout and field ordering in the menu definition create/edit dialog
- How hierarchy is rendered in the tree view (PrimeNG Tree component vs grouped table rows)
- Whether the menu definition list uses the same list+dialog pattern as Roles and Row Policies (recommended — consistent)
- Pagination or scroll strategy for the menu definitions list
- Exact sync endpoint design (POST trigger vs startup hook)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase requirements and roadmap context
- `.planning/ROADMAP.md` — Phase 7 goal and Phase 8 dependency chain; Phase 7.5 inserts between them
- `.planning/REQUIREMENTS.md` — ROUTE-01 (backend-driven navigation) is the requirement this phase operationalizes on the admin side
- `.planning/PROJECT.md` — milestone context, brownfield constraints, multi-app navigation design

### Phase 7 decisions (locked)
- `.planning/phases/07-enterprise-navigation-shell/07-CONTEXT.md` — D-01 (frontend owns static node IDs), D-03 (app-scoped with appName), D-05 (backend stores/evaluates frontend-canonical IDs); Phase 7.5 extends but does not contradict these

### Existing backend entities
- `src/main/java/com/vn/core/security/domain/SecMenuDefinition.java` — menu node catalog entity (menuId, appName, menuName, label, description, parentMenuId, route, icon, ordering)
- `src/main/java/com/vn/core/security/domain/SecMenuPermission.java` — role→menu permission entity (role, appName, menuId)
- `src/main/java/com/vn/core/web/rest/MenuPermissionResource.java` — existing current-user GET endpoint; admin endpoints must NOT modify this

### Existing frontend patterns to follow
- `frontend/src/app/layout/navigation/navigation-registry.ts` — canonical frontend nav tree; structure and node IDs are the source for the tree view and catalog sync
- `frontend/src/app/pages/admin/security/roles/list/role-list.component.ts` — list+dialog pattern to follow for menu definitions page
- `frontend/src/app/pages/admin/security/security.routes.ts` — where new `/admin/security/menu-definitions` and menu permission routes must be registered

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `SecRoleService` + role list+dialog pattern: established list-with-dialog CRUD pattern for security entities; follow this for menu definitions
- `permission-matrix` component: role-first permission assignment pattern to extend for menu access tab
- `navigation-registry.ts` APP_NAVIGATION_TREE: use this as the data source for the tree view and catalog sync endpoint

### Established Patterns
- Security admin pages follow list+dialog (not list+separate-route) for CRUD — roles and row-policies both use this
- PrimeNG TableModule + ConfirmDialogModule + CardModule is the standard import set for admin list components
- Backend admin resources use `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` and live in `com.vn.core.web.rest`

### Integration Points
- New routes register under `/admin/security/` in `security.routes.ts`
- Navigation registry needs a new node ID for `security.menu-definitions` so the route guard and menu visibility work correctly
- `AdminMenuPermissionResource` follows the same package and auth pattern as existing admin resources (`UserResource`, `AuthorityResource`)

</code_context>

<specifics>
## Specific Ideas

- Dynamic menu items use case: admin registers external links or custom dashboards via the menu definitions CRUD; frontend fetches and renders these at runtime alongside the static registry nodes
- Tree view for node selection should reflect the navigation registry hierarchy: Home > Dashboard; Entities > Organization / Department / Employee; Security > Users / Roles / Row Policies / Menu Definitions

</specifics>

<deferred>
## Deferred Ideas

- i18n key management for dynamically-created menu node labels (labels stored in DB, no translation bundle entry — deferred to Phase 9 UX hardening)
- Ordering drag-and-drop in the menu definitions list (deferred to Phase 9)
- Multi-app menu management UI (current phase targets `jhipster-security-platform` app only; multi-app admin is a future enhancement)

</deferred>

---

*Phase: 075-menu-management*
*Context gathered: 2026-03-25*
