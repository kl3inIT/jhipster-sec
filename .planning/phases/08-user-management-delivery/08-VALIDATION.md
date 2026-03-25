---
phase: 8
slug: user-management-delivery
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-25
updated: 2026-03-25
---

# Phase 8 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Spring Boot Test for admin-user API contract coverage plus Angular test builder with Vitest and Playwright for frontend user-management flows |
| **Config file** | `build.gradle`, `frontend/angular.json`, `frontend/tsconfig.spec.json`, `frontend/playwright.config.ts` |
| **Quick run command** | `npm --prefix frontend exec ng test -- --watch=false --include src/app/pages/admin/user-management/user-management.routes.spec.ts --include src/app/pages/admin/user-management/service/user-management.service.spec.ts` |
| **Full suite command** | `./gradlew test --tests "com.vn.core.web.rest.UserResourceIT" && npm --prefix frontend run build && npm --prefix frontend exec ng test -- --watch=false --include src/app/pages/admin/user-management/**/*.spec.ts && npm --prefix frontend exec playwright test e2e/user-management.spec.ts` |
| **Estimated runtime** | ~180 seconds |

---

## Sampling Rate

- **After every task commit:** Run the task-specific command from the verification map below
- **After every plan wave:** Run the wave's mapped backend or frontend verification commands, plus `npm --prefix frontend run build` after any UI wave
- **Before `$gsd-verify-work`:** Run `./gradlew test --tests "com.vn.core.web.rest.UserResourceIT" && npm --prefix frontend run build && npm --prefix frontend exec ng test -- --watch=false --include src/app/pages/admin/user-management/**/*.spec.ts && npm --prefix frontend exec playwright test e2e/user-management.spec.ts`
- **Max feedback latency:** 60 seconds for task-level checks, 180 seconds for the full phase gate

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 08-W0-01 | W0 | 0 | UMGT-01 | integration | `./gradlew test --tests "com.vn.core.web.rest.UserResourceIT"` | ✅ | pending |
| 08-W0-02 | W0 | 0 | UMGT-01 | unit | `npm --prefix frontend exec ng test -- --watch=false --include src/app/pages/admin/user-management/user-management.routes.spec.ts --include src/app/pages/admin/user-management/service/user-management.service.spec.ts` | ✅ | pending |
| 08-W0-03 | W0 | 0 | UMGT-01 | component | `npm --prefix frontend exec ng test -- --watch=false --include src/app/pages/admin/user-management/list/user-management-list.component.spec.ts --include src/app/pages/admin/user-management/detail/user-management-detail.component.spec.ts` | ❌ W0 | pending |
| 08-W0-04 | W0 | 0 | UMGT-02, UMGT-03 | component | `npm --prefix frontend exec ng test -- --watch=false --include src/app/pages/admin/user-management/update/user-management-update.component.spec.ts` | ❌ W0 | pending |
| 08-W0-05 | W0 | 0 | UMGT-01, UMGT-02, UMGT-03 | build + e2e | `npm --prefix frontend run build && npm --prefix frontend exec playwright test e2e/user-management.spec.ts` | ❌ W0 | pending |

*Status: pending | green | red | flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/vn/core/web/rest/UserResourceIT.java` - extend coverage for optional `query` filtering, allowed sort keys, and preserved admin browse response headers
- [ ] `frontend/src/app/pages/admin/user-management/list/user-management-list.component.spec.ts` - cover search debounce, backend paging and sort mapping, inline activation, and list-context storage
- [ ] `frontend/src/app/pages/admin/user-management/detail/user-management-detail.component.spec.ts` - cover detail route rendering, disabled role table, and return navigation restoration
- [ ] `frontend/src/app/pages/admin/user-management/update/user-management-update.component.spec.ts` - cover create/edit validation mapping, authority checkbox selection, save success, save failure, and delete follow-up paths
- [ ] `frontend/e2e/user-management.spec.ts` - cover browse, detail, create, edit, activation toggle, delete confirmation, and role assignment smoke

*Existing route and service specs are retained, but they are not sufficient as primary phase validation.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| The split-page detail and edit layout stays readable below 1024px, with the role table remaining visible beneath the user card on narrow widths | UMGT-01, UMGT-03 | Requires visual confirmation of responsive layout, table density, and card stacking in the live PrimeNG shell | 1. Run the frontend locally. 2. Open `/admin/users`, then a detail route and an edit route. 3. Test desktop and a narrow tablet or mobile viewport. 4. Confirm the list remains usable, the split-page collapses to one column, and no actions or role rows are clipped. |
| Route transitions focus the page heading and all user-management copy remains translated in both `en` and `vi`, including delete confirmation and role labels | UMGT-01, UMGT-02, UMGT-03 | Requires browser focus behavior, translated overlays, and live route transitions that unit tests do not fully prove | 1. Switch the app between English and Vietnamese. 2. Navigate list, detail, create, and edit routes. 3. Confirm the H1 receives focus after each route change. 4. Trigger delete confirmation and verify translated copy. 5. Confirm role labels remain readable in both locales. |

---

## Validation Sign-Off

- [x] All phase requirements have automated verification targets or explicit Wave 0 coverage
- [x] Sampling continuity: no more than 2 consecutive tasks should land without a quick verification command
- [x] No watch-mode flags
- [ ] Wave 0 coverage is complete for backend search, list/detail/update, and targeted end-to-end user-management tests
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
