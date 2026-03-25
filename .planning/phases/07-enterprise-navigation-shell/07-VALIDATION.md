---
phase: 7
slug: enterprise-navigation-shell
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-25
updated: 2026-03-25
---

# Phase 7 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Spring Boot Test for backend contracts plus Angular test builder with Vitest and Playwright for frontend shell behavior |
| **Config file** | `build.gradle`, `frontend/angular.json`, `frontend/tsconfig.spec.json`, `frontend/playwright.config.ts` |
| **Quick run command** | `npm --prefix frontend exec ng test -- --watch=false --include src/app/layout/component/menu/app.menu.spec.ts --include src/app.routes.spec.ts` |
| **Full suite command** | `./gradlew test && npm --prefix frontend run build && npm --prefix frontend exec ng test -- --watch=false && npm --prefix frontend exec playwright test e2e/security-comprehensive.spec.ts` |
| **Estimated runtime** | ~240 seconds |

---

## Sampling Rate

- **After every task commit:** Run the task-specific command from the verification map below
- **After every plan wave:** Run `npm --prefix frontend run build` plus the wave's mapped unit or integration tests
- **Before `$gsd-verify-work`:** Run `./gradlew test && npm --prefix frontend run build && npm --prefix frontend exec ng test -- --watch=false && npm --prefix frontend exec playwright test e2e/security-comprehensive.spec.ts`
- **Max feedback latency:** 60 seconds for unit/integration checks, 240 seconds for the full phase gate

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 07-01-01 | 01 | 1 | ROUTE-01 | integration | `./gradlew test --tests "com.vn.core.service.security.CurrentUserNavigationGrantServiceTest" --tests "com.vn.core.web.rest.NavigationGrantResourceIT"` | ❌ W0 | pending |
| 07-02-01 | 02 | 1 | ROUTE-01 | unit | `npm --prefix frontend exec ng test -- --watch=false --include src/app/layout/navigation/navigation.service.spec.ts --include src/app/layout/component/menu/app.menu.spec.ts` | ❌ W0 | pending |
| 07-03-01 | 03 | 2 | ROUTE-02 | unit | `npm --prefix frontend exec ng test -- --watch=false --include src/app/core/auth/user-route-access.service.spec.ts --include src/app.routes.spec.ts` | ❌ W0 | pending |
| 07-04-01 | 04 | 3 | UI-04 | component | `npm --prefix frontend exec ng test -- --watch=false --include src/app/layout/navigation/breadcrumb.service.spec.ts --include src/app/pages/error/access-denied.component.spec.ts --include src/app/pages/entities/organization/list/organization-list.component.spec.ts` | ❌ W0 | pending |
| 07-05-01 | 05 | 4 | ROUTE-01, ROUTE-02, ROUTE-03, UI-04 | build + e2e | `npm --prefix frontend run build && npm --prefix frontend exec playwright test e2e/security-comprehensive.spec.ts` | ✅ | pending |

*Status: pending | green | red | flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/vn/core/service/security/CurrentUserNavigationGrantServiceTest.java` - cover app-scoped authority merge and deny-wins behavior for current-user navigation grants
- [ ] `src/test/java/com/vn/core/web/rest/NavigationGrantResourceIT.java` - cover `/api/security/navigation-grants` response contract and authentication rules
- [ ] `frontend/src/app/layout/navigation/navigation.service.spec.ts` - cover cache warm-start, auth-state reset, and backend leaf filtering
- [ ] `frontend/src/app/core/auth/user-route-access.service.spec.ts` - cover unauthenticated login redirect, unreachable leaf redirect, and allowed leaf pass-through
- [ ] `frontend/src/app/layout/navigation/breadcrumb.service.spec.ts` - cover breadcrumb generation from route metadata and active section ancestry
- [ ] `frontend/src/app/pages/error/access-denied.component.spec.ts` - cover blocked destination copy and safe fallback CTA rendering

*Existing infrastructure covers the framework and runners; Phase 7 Wave 0 is about missing spec files, not tool installation.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Mobile breadcrumb wrapping and stacked workspace header remain readable below 992px | UI-04 | Requires visual confirmation across responsive breakpoints and real shell chrome | 1. Run the frontend locally. 2. Open an entity detail or edit route at a mobile viewport. 3. Confirm breadcrumbs wrap cleanly, the primary action stacks below the title, and no text is clipped off-screen. |
| Safe fallback CTA on `/accessdenied` chooses an actually allowed destination for mixed-access users | ROUTE-02 | Requires a live role mix and route navigation semantics rather than isolated component rendering | 1. Log in as a user with at least one denied leaf and one allowed sibling. 2. Deep-link to the denied route. 3. Confirm the denied page names the blocked destination. 4. Click the recovery CTA and confirm it lands on an allowed route in the same section when possible. |

---

## Validation Sign-Off

- [x] All planned tasks have an automated verification target or an explicit Wave 0 dependency
- [x] Sampling continuity: no more than 2 consecutive tasks should land without a quick verification command
- [x] No watch-mode flags
- [ ] Wave 0 coverage is complete for new navigation, guard, breadcrumb, and denied-state tests
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
