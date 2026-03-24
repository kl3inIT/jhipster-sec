# Phase 6: Frontend Parity Foundation - Context

**Gathered:** 2026-03-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Bring the missing JHipster frontend runtime foundations into `frontend/` so the app can support translated shell, admin, account, and user-management work without relying on partial ad hoc copies. This phase delivers support files, translation assets, shared feedback/runtime helpers, and user-management groundwork only. It does not deliver backend-driven navigation contracts (Phase 7) or the full user-management UI surface (Phase 8).

</domain>

<decisions>
## Implementation Decisions

### Donor and support-file scope
- **D-01:** Keep `aef-main` as the structural template for standalone Angular bootstrap, router/layout patterns, and PrimeNG shell behavior, but use `angapp` as the canonical donor for JHipster-specific support files, translations, and admin/user-management runtime pieces.
- **D-02:** Phase 6 migrates only the minimum in-scope donor pack needed for later shell and admin work: translation config/runtime, language assets, alert/notification plumbing, shared request and pagination helpers, authority display/gating helpers, and the preserved admin user-management model/service/route foundation.
- **D-03:** Copied donor pieces must be adapted to the standalone Angular and PrimeNG shell already established in `frontend/`; do not reintroduce broad NgModule-era shared-module structure or copy out-of-scope ops/admin pages.

### Language and translation baseline
- **D-04:** Vietnamese (`vi`) is the default language on first load; English (`en`) is the secondary language.
- **D-05:** The selected locale is persisted through the existing frontend storage flow, and the persisted choice overrides the default language on later visits.
- **D-06:** Phase 6 must include `vi` and `en` translation assets for all in-scope shared shell, admin, account, and user-management foundations so later phases do not build on missing-key gaps.

### Alerts and shared feedback
- **D-07:** Replace console-only success-notification handling with a user-visible, translated alert foundation so backend `app-alert` headers surface inside the frontend runtime.
- **D-08:** Keep the feedback foundation compatible with the PrimeNG shell: translated inline or route-safe alerts are the baseline, with planners free to use toast presentation where appropriate for transient success states.
- **D-09:** Port the JHipster alert/support helpers from `angapp` only as far as needed to restore shared feedback behavior; do not copy the full legacy Bootstrap-centric alert stack wholesale.

### User-management groundwork
- **D-10:** Phase 6 migrates the preserved admin user-management foundation now: user model, service contract, route skeleton or resolver, request models, pagination helpers, and directly supporting shared components or directives.
- **D-11:** Full user-management screens and CRUD interactions remain Phase 8 work; Phase 6 only prepares the shared runtime and translation groundwork those screens will depend on.
- **D-12:** Shared list, filter, and pagination helpers should be brought over only when they directly support in-scope admin or shell groundwork; unrelated legacy admin utilities remain out of scope.

### the agent's Discretion
- Exact file-by-file migration list within the approved donor pack
- Whether helper utilities are direct ports or lightweight standalone-adapted rewrites
- Exact alert presentation component structure and whether success feedback is inline, toast, or mixed by context
- Whether a language switcher UI appears in Phase 6 or Phase 7, as long as the `vi`/`en` runtime and persisted locale behavior are in place

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase requirements and milestone rules
- `.planning/ROADMAP.md` - `Phase 6: Frontend Parity Foundation` goal, dependencies, and success criteria
- `.planning/REQUIREMENTS.md` - `I18N-01` and `I18N-02` define the required frontend support-file and translation outcomes
- `.planning/PROJECT.md` - milestone context, constraints, and the locked donor decision that `angapp` provides required frontend support files while `aef-main` remains the structural reference
- `.planning/phases/05-standalone-frontend-delivery/05-CONTEXT.md` - locked frontend foundation decisions from Phase 5, including the `aef-main` structural template and existing auth, layout, and interceptor baseline

### Current frontend integration seams
- `frontend/angular.json` - current asset pipeline; translation assets and copied support files must be wired into this app
- `frontend/package.json` - existing Angular, PrimeNG, and ngx-translate dependency baseline
- `frontend/src/app/app.config.ts` - current bootstrap/provider gap that must be aligned with the parity foundation
- `frontend/src/app/app.routes.ts` - current route shell gap that later translated/admin foundations must plug into
- `frontend/src/app/core/auth/account.service.ts` - current account/authentication state flow and authority checks
- `frontend/src/app/core/auth/state-storage.service.ts` - existing locale, token, and return-URL persistence seam
- `frontend/src/app/core/interceptor/notification.interceptor.ts` - current console-only success notification behavior
- `frontend/src/app/core/interceptor/error-handler.interceptor.ts` - current lightweight error handling baseline
- `frontend/src/app/core/request/request-util.ts` - current request helper seam to align with migrated admin foundations

### `aef-main` structural references
- `aef-main/aef-main/src/app.config.ts` - standalone bootstrap/provider pattern, router features, PrimeNG setup, and translation module import shape
- `aef-main/aef-main/src/app.routes.ts` - route/layout structure pattern to preserve while parity work is added

### `angapp` donor references
- `angapp/src/main/webapp/app/config/translation.config.ts` - JHipster translation loader and missing-key contract
- `angapp/src/main/webapp/app/shared/language/translation.module.ts` - locale bootstrap and persisted-language startup behavior
- `angapp/src/main/webapp/app/shared/language/translate.directive.ts` - translation directive pattern used throughout donor templates
- `angapp/src/main/webapp/app/core/util/alert.service.ts` - translated alert service behavior
- `angapp/src/main/webapp/app/core/interceptor/notification.interceptor.ts` - backend alert-header handling
- `angapp/src/main/webapp/app/shared/alert/alert.component.ts` - shared alert rendering foundation
- `angapp/src/main/webapp/app/shared/auth/has-any-authority.directive.ts` - template-level authority gating helper
- `angapp/src/main/webapp/app/shared/pagination/item-count.component.ts` - shared pagination summary helper
- `angapp/src/main/webapp/app/shared/filter/filter.component.ts` - shared filter-display helper when admin list foundations need it
- `angapp/src/main/webapp/app/core/request/request.model.ts` - request/pagination model used by admin list services
- `angapp/src/main/webapp/app/admin/user-management/user-management.model.ts` - preserved admin user-management model shape
- `angapp/src/main/webapp/app/admin/user-management/user-management.route.ts` - user-management route/resolver foundation
- `angapp/src/main/webapp/app/admin/user-management/service/user-management.service.ts` - preserved `/api/admin/users` service contract and authority lookup pattern

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `frontend/src/app/core/auth/account.service.ts` already carries authenticated account state and authority checks; migrated admin/shared helpers should reuse this instead of introducing a second auth state layer.
- `frontend/src/app/core/auth/state-storage.service.ts` already persists tokens, return URLs, and locale data; it is the natural seam for the `vi` default plus persisted-language override.
- `frontend/src/app/layout/component/main/app.layout.ts` and the existing layout/menu components provide the PrimeNG shell that migrated alerts and translated shared UI must fit into.
- `angapp` already contains the missing translation, alert, shared request, pagination, and user-management foundation files; these are the donor assets for Phase 6 rather than fresh reinvention.

### Established Patterns
- Phase 5 locked `aef-main` as the structure template, so Phase 6 should adapt donor runtime pieces into the standalone Angular and PrimeNG shape already used in `frontend/`.
- The current workspace already includes `@ngx-translate/core` and `@ngx-translate/http-loader` in `frontend/package.json`, so the gap is runtime wiring and assets, not dependency selection.
- Current `frontend` interceptors are intentionally lightweight, but Phase 6 needs to restore translated shared-feedback behavior before later admin surfaces are added.
- Preserved admin user-management backend contracts already exist; the donor user-management service, model, and route files are the clearest parity baseline for later Phase 8 screens.

### Integration Points
- `frontend/angular.json` and the frontend asset tree need an `i18n` source that can serve copied `vi` and `en` translation files.
- `frontend/src/app/app.config.ts` needs the provider wiring for HTTP interceptors, PrimeNG shell config, and translation bootstrap so copied support files actually run.
- `frontend/src/app/core/interceptor/notification.interceptor.ts` should connect to a migrated alert service instead of writing to `console.warn`.
- The migrated admin user-management foundation should plug into the preserved backend endpoints `/api/admin/users` and `/api/authorities` without changing backend contracts.
- Later phases depend on this groundwork: Phase 7 for translated/shared shell behavior and Phase 8 for the full user-management UI.

</code_context>

<specifics>
## Specific Ideas

- `vi` is the product-default language; `en` is available as the secondary language.
- Use `aef-main` for the shell/bootstrap shape, but treat `angapp` as the donor for JHipster-specific runtime and translation behavior.
- Remove the current console-only feedback baseline and restore real translated alert handling before more admin screens land.
- Bring over only the user-management foundations needed to unblock Phase 8, not the full screen set or unrelated legacy admin pages.

</specifics>

<deferred>
## Deferred Ideas

- Backend-driven navigation and permission-aware menu loading - Phase 7
- Full admin user-management browse, detail, create, edit, and delete UI - Phase 8
- Broader enterprise responsiveness and performance hardening - Phase 9
- Frontend regression coverage expansion - Phase 10
- Legacy ops/admin pages such as health, metrics, logs, configuration, and docs - still out of scope for this milestone unless a later phase explicitly pulls shared pieces from them

</deferred>

---

*Phase: 06-frontend-parity-foundation*
*Context gathered: 2026-03-25*
