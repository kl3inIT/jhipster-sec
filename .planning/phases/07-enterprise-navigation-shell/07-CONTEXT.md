# Phase 7: Enterprise Navigation Shell - Context

**Gathered:** 2026-03-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Replace the current hardcoded shell menu and coarse route guards with backend-driven, permission-aware navigation behavior inside the existing standalone Angular shell. This phase covers the menu visibility contract, denied-route behavior, lazy-boundary-aware shell rules, and Jmix-style list-first navigation patterns. It does not deliver the full user-management UI surface or a menu-permission administration UI.

</domain>

<decisions>
## Implementation Decisions

### Navigation contract
- **D-01:** The frontend remains the canonical owner of the `Home`, `Entities`, and `Security` shell tree, including translated labels, icons, stable node ids, and rendered ordering.
- **D-02:** The backend visibility contract returns only the allowed stable menu node ids for the current user; it does not return the rendered tree or frontend ordering.
- **D-03:** Menu authorization must support multiple frontend apps from day one. The backend model is app-scoped and must include an app identifier such as `app_name`.
- **D-04:** If every child inside a fixed section is denied, the frontend hides that section completely.
- **D-05:** Stable menu node ids are frontend-canonical. The backend stores or evaluates those ids rather than defining a separate node-id catalog.

### Route denial behavior
- **D-06:** Unauthorized deep links redirect to `/accessdenied`; the app must not silently reroute denied users to another section or the home page.
- **D-07:** The access-denied experience should mention the blocked destination in user-facing text.
- **D-08:** If a denied leaf belongs to a section that still has accessible siblings, keep the parent section visible and hide only the denied leaf.
- **D-09:** The access-denied page provides a primary action that returns the user to a safe allowed area.

### Enterprise workspace pattern
- **D-10:** Section navigation should be list-first. Admin and secured-entity areas should generally land on list or index workspaces, with detail and edit routes as subordinate drill-ins.
- **D-11:** Returning from detail or edit routes to a list should preserve list context, including filters, pagination, sort, and section selection state.
- **D-12:** The shell should add breadcrumb navigation plus active section highlighting so deep routes remain easy to orient within the enterprise shell.
- **D-13:** Detail and edit flows stay route-based pages in Phase 7; this phase does not shift the app toward dialogs or drawers as the default workflow.

### Visibility threshold
- **D-14:** Entity menu leaves appear when backend navigation grants allow the route to be opened. Entity `READ` capability is not the shell visibility threshold.
- **D-15:** If a user can open an entity area from the menu but lacks entity `READ` permission, that page renders a full-page in-shell access-denied state and must not load list data.
- **D-16:** Admin entries follow a stricter rule than entity entries: they appear only when the admin route is actually reachable, not as visible-but-denied leaves.

### the agent's Discretion
- Exact stable node-id naming convention, as long as ids stay frontend-owned and durable across language changes
- Exact safe fallback selection logic for the access-denied page's "back to allowed area" action
- Breadcrumb copy, styling, and how much section-local chrome to add within the existing Sakai layout
- Exact backend schema, entity, and service shape beyond the locked multi-app `app_name` requirement

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase requirements and milestone context
- `.planning/ROADMAP.md` - `Phase 7: Enterprise Navigation Shell` goal, dependency, and success criteria
- `.planning/REQUIREMENTS.md` - `ROUTE-01`, `ROUTE-02`, `ROUTE-03`, and `UI-04` define the required routing and shell outcomes
- `.planning/PROJECT.md` - milestone context, brownfield constraints, and the locked decision to move from hardcoded menus to backend-driven navigation
- `.planning/STATE.md` - current milestone state shows Phase 7 as the active planning target after Phase 6 completion

### Prior phase outputs
- `.planning/phases/05-standalone-frontend-delivery/05-CONTEXT.md` - original standalone shell, admin route, and permission-matrix assumptions the new shell must evolve from
- `.planning/phases/06-frontend-parity-foundation/06-CONTEXT.md` - donor strategy, translated shell foundations, and the locked stable-menu-id rule from Phase 6

### Current frontend integration seams
- `frontend/src/app/layout/component/menu/app.menu.ts` - current hardcoded translated menu tree that Phase 7 must replace or filter through backend visibility
- `frontend/src/app/layout/component/menu/app.menu.spec.ts` - menu-language-switching and stable-id expectations already enforced by tests
- `frontend/src/app/layout/component/main/app.layout.ts` - current PrimeNG Sakai shell host for sidebar, topbar, alerts, and routed workspace content
- `frontend/src/app.routes.ts` - root route tree, lazy boundaries, and current `ROLE_ADMIN` route metadata
- `frontend/src/app/core/auth/user-route-access.service.ts` - current login plus authority-based guard behavior to extend for permission-aware denial
- `frontend/src/app/pages/admin/admin.routes.ts` - existing admin lazy boundary
- `frontend/src/app/pages/admin/security/security.routes.ts` - current security-admin leaf routes inside the admin boundary
- `frontend/src/app/pages/admin/user-management/user-management.routes.ts` - placeholder user-management route set that Phase 7 shell behavior must accommodate for Phase 8
- `frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts` - current per-user capability cache that already drives entity-level gating

### Current backend contract seams
- `src/main/java/com/vn/core/web/rest/admin/security/SecCatalogAdminResource.java` - existing admin-sec catalog endpoint already exposing secured-entity metadata
- `src/main/java/com/vn/core/service/dto/security/SecCatalogEntryDTO.java` - current catalog response shape consumed by frontend admin/security clients

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/app/layout/component/main/app.layout.ts`, `frontend/src/app/layout/component/sidebar/app.sidebar.ts`, `frontend/src/app/layout/component/menu/app.menuitem.ts`, and `frontend/src/app/layout/service/layout.service.ts`: existing PrimeNG shell and sidebar primitives that Phase 7 should extend rather than replace
- `frontend/src/app/core/auth/user-route-access.service.ts`: current route-guard seam for authenticated and authority-aware denial
- `frontend/src/app.routes.ts` plus `frontend/src/app/pages/admin/admin.routes.ts`: existing lazy boundaries already split admin and entity areas
- `frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts`: cached entity-capability lookup that can support in-shell denied states without repeated requests
- `src/main/java/com/vn/core/web/rest/admin/security/SecCatalogAdminResource.java`: existing backend endpoint that proves frontend security-shell work can consume backend-managed navigation metadata patterns

### Established Patterns
- Phase 6 already locked stable menu ids and client-side translated labels as frontend concerns
- The current app already uses route-based detail and edit pages rather than modal-heavy admin flows
- Admin and entity areas already load through separate lazy route boundaries, so Phase 7 should refine guard and shell behavior rather than collapse the route tree
- Entity screens already rely on capability data to gate actions and field visibility after route entry

### Integration Points
- Replace hardcoded `AppMenu` assembly with a backend visibility filter over frontend-owned node ids
- Extend route metadata and guarding so backend navigation grants and denied-route behavior share one permission-aware source of truth
- Keep admin leaves strict while allowing entity leaves to render in-shell access-denied states when navigation is granted but entity reads are denied
- Add a multi-app backend menu-authorization seam keyed by app identifier plus frontend node id

</code_context>

<specifics>
## Specific Ideas

- Multi-app menu authorization should identify the frontend shell with an app identifier such as `app_name`.
- Frontend ordering remains client-controlled even though visibility is backend-driven.
- The access-denied experience should explicitly name the blocked destination and provide a recovery action back to an allowed area.
- The enterprise feel should come from list-first landing pages, preserved list state, breadcrumbs, and active section highlighting rather than a split-view rewrite.
- Entity areas may be visible from a backend menu grant even when entity `READ` permission is absent; in that case the user sees an in-shell denied state instead of a loaded data table.

</specifics>

<deferred>
## Deferred Ideas

- Menu permission management UI - separate admin capability beyond Phase 7 shell and route work
- Full admin user-management browse and CRUD workflows - Phase 8
- Broader responsiveness and performance hardening - Phase 9
- Frontend reliability and regression coverage expansion - Phase 10

</deferred>

---

*Phase: 07-enterprise-navigation-shell*
*Context gathered: 2026-03-25*
