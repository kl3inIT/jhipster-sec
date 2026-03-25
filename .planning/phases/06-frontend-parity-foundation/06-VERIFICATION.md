---
phase: 06-frontend-parity-foundation
verified: 2026-03-25T04:27:56Z
status: passed
score: 3/3 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 2/3
  gaps_closed:
    - "The frontend can render core migrated shell and user-management UI text from copied translation assets without fallback gaps."
  gaps_remaining: []
  regressions: []
---

# Phase 6: Frontend Parity Foundation Verification Report

**Phase Goal:** `frontend/` contains the JHipster support files and i18n foundations required for the next admin and shell work instead of relying on partial ad hoc copies.
**Verified:** 2026-03-25T04:27:56Z
**Status:** passed
**Re-verification:** Yes - after gap closure

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Required JHipster support files and translation assets for in-scope flows exist in `frontend/` and are wired into the Angular runtime. | VERIFIED | `frontend/src/app.config.ts` registers `provideTranslateService(...)`, fallback handling, and `AppPageTitleStrategy`; `frontend/src/app/app.ts` applies stored locale precedence plus html/dayjs/title updates; `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` remain the live runtime bundles. |
| 2 | Shared frontend services and utilities for language, alerts, request helpers, and admin or user-management foundations are aligned with the migrated shell needs. | VERIFIED | `frontend/src/app/core/util/alert.service.ts`, `frontend/src/app/core/interceptor/notification.interceptor.ts`, `frontend/src/app/shared/alert/alert.component.ts`, `frontend/src/app/shared/auth/has-any-authority.directive.ts`, `frontend/src/app/core/request/request-util.ts`, `frontend/src/app/pages/admin/user-management/service/user-management.service.ts`, `frontend/src/app/layout/component/menu/app.menu.ts`, and `frontend/src/app.routes.ts` are still present and wired. |
| 3 | The frontend can render core migrated shell and user-management UI text from copied translation assets without fallback gaps. | VERIFIED | `frontend/public/i18n/vi.json` now ships corrected `feedback.*` Vietnamese strings; `frontend/src/app/shared/error/http-error.utils.ts` resolves those keys at runtime; `frontend/src/app/shared/error/http-error.utils.spec.ts` now reads the shipped `en.json` and `vi.json` bundles directly and passes localized English and Vietnamese assertions. |

**Score:** 3/3 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `frontend/src/app/config/translation.config.ts` | Static ngx-translate loader and missing-translation handling | VERIFIED | Uses `./i18n/*.json` bundles and `translation-not-found[...]` fallback markers. |
| `frontend/src/app.config.ts` | Root runtime wiring for translation, router titles, and interceptors | VERIFIED | Registers router, translation loader, fallback language, missing-translation handler, and title strategy. |
| `frontend/src/app/app.ts` | Stored-locale precedence plus html/dayjs/title updates | VERIFIED | Uses `StateStorageService.getLocale()` before `account.langKey` and refreshes title/html/dayjs on language change. |
| `frontend/src/app/core/util/alert.service.ts` | Translated alert state for header-driven shell feedback | VERIFIED | Resolves `translationKey` through `TranslateService.instant(...)` and surfaces alerts through signal state. |
| `frontend/src/app/core/request/request-util.ts` | Typed request helper without `any` request params | VERIFIED | Converts scalar and array query params safely and is consumed by user-management queries. |
| `frontend/src/app/pages/admin/user-management/service/user-management.service.ts` | Preserved `/api/admin/users` and `/api/authorities` contract | VERIFIED | Uses `ApplicationConfigService.getEndpointFor(...)` plus `createRequestOption(req)` for admin user APIs. |
| `frontend/src/app/layout/component/menu/app.menu.ts` | Translated shell menu with admin discoverability for `/admin/users` | VERIFIED | Rebuilds on auth or language changes and exposes the translated admin-only `/admin/users` entry. |
| `frontend/src/app.routes.ts` | Authoritative root/admin route tree with translated titles | VERIFIED | Mounts login, shell, entities, and guarded `/admin` lazy routes with translation-key titles. |
| `frontend/public/i18n/en.json` | Donor and Phase 6 translation keys for shell, admin foundation, and feedback | VERIFIED | Shipped English feedback keys match the helper paths and targeted tests. |
| `frontend/public/i18n/vi.json` | Vietnamese donor and Phase 6 translation keys with usable vi copy | VERIFIED | Corrected feedback subtree now contains localized delete, access-denied, and entity or security feedback text without the prior corruption. |
| `frontend/src/app/shared/error/http-error.utils.ts` | Translation-aware toast and HTTP error helper for the active locale | VERIFIED | Builds messages from `feedback.*` keys and handles 403 versus generic fallbacks correctly. |
| `frontend/src/app/shared/error/http-error.utils.spec.ts` | Regression coverage against shipped feedback bundles | VERIFIED | Reads `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` directly and asserts both bundle content and active-locale message rendering. |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `frontend/src/app.config.ts` | `frontend/src/app/config/translation.config.ts` | `provideTranslateService(...)` and missing-translation handler | WIRED | Static bundle loader, fallback language, and missing handler are registered at bootstrap. |
| `frontend/src/app/app.ts` | `frontend/src/app/core/auth/state-storage.service.ts` | `getLocale()` startup precedence | WIRED | Stored locale still wins over the default and only falls back to `account.langKey` when no locale has been stored. |
| `frontend/src/app/app.ts` | `frontend/src/app/app-page-title-strategy.ts` | `updateTitle(router.routerState.snapshot)` on language change | WIRED | Browser titles refresh from the active translated route metadata. |
| `frontend/src/app/core/interceptor/notification.interceptor.ts` | `frontend/src/app/core/util/alert.service.ts` | `addAlert(...)` after `jhipstersec.*` normalization | WIRED | Backend success headers still become translated shell alerts. |
| `frontend/src/app/pages/admin/user-management/service/user-management.service.ts` | `frontend/src/app/core/request/request-util.ts` | `createRequestOption(req)` | WIRED | Typed query params still flow into `/api/admin/users`. |
| `frontend/src/app.routes.ts` | `frontend/src/app/pages/admin/admin.routes.ts` | `loadChildren` for `/admin` | WIRED | Root route tree still mounts the guarded admin subtree. |
| `frontend/src/app/pages/admin/admin.routes.ts` | `frontend/src/app/pages/admin/user-management/user-management.routes.ts` | `loadChildren` for `users` | WIRED | `/admin/users` remains mounted through the live admin route tree. |
| `frontend/src/app/layout/component/menu/app.menu.ts` | `/admin/users` | Admin-only shell navigation entry | WIRED | Menu spec still confirms discoverability for admins and hiding for non-admins. |
| `frontend/src/app/shared/error/http-error.utils.ts` | `frontend/public/i18n/vi.json` | `feedback.*` translation keys | WIRED | Corrected `vi.json` values now match the helper keys, and the bundle-backed helper spec validates the shipped asset directly. |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| `I18N-01` | `06-01` through `06-06` | Required JHipster support files from `angapp/` for in-scope frontend features are migrated into `frontend/` instead of being reimplemented incompletely. | SATISFIED | Phase 6 support files remain present and wired: translation bootstrap, alert helpers, authority directive, typed request helper, user-management service and routes, translated shell menu, and authoritative admin routing. |
| `I18N-02` | `06-01` through `06-06` | Migrated admin, user-management, and shared shell flows can render translated UI strings and preserve language-aware behavior using copied JHipster translation assets. | SATISFIED | `vi.json` feedback keys are corrected, `http-error.utils.ts` consumes them, the bundle-backed helper spec passes, `app.menu.spec.ts` passes admin discoverability and language-switch checks, `app.routes.spec.ts` passes translated route-title checks, and `npm --prefix frontend run build` completes successfully. |

No orphaned Phase 6 requirements were found in `.planning/REQUIREMENTS.md`.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `frontend/angular.json` | 43 | Initial bundle warning budget remains `500kB` while the current production build totals `887.12 kB` | WARNING | Known non-blocking build warning remains and should be handled by later performance work. |
| `frontend/src/app/pages/admin/security/roles/list/role-list.component.ts` | 96 | Hardcoded English confirmation dialog copy | WARNING | Delete confirmation remains English even though toast feedback is localized. |
| `frontend/src/app/pages/admin/security/row-policies/list/row-policy-list.component.ts` | 92 | Hardcoded English confirmation dialog copy | WARNING | Delete confirmation remains English even though toast feedback is localized. |
| `frontend/src/app/pages/entities/organization/list/organization-list.component.ts` | 187 | Hardcoded English confirmation dialog copy | WARNING | Delete confirmation remains English even though toast feedback is localized. |
| `frontend/src/app/pages/entities/department/list/department-list.component.ts` | 182 | Hardcoded English confirmation dialog copy | WARNING | Delete confirmation remains English even though toast feedback is localized. |
| `frontend/src/app/pages/entities/employee/list/employee-list.component.ts` | 184 | Hardcoded English confirmation dialog copy | WARNING | Delete confirmation remains English even though toast feedback is localized. |

### Human Verification Required

No blocking human-only gaps remain for Phase 6 completion. An optional smoke check in the running app is still prudent: switch to Vietnamese and trigger role, row-policy, organization, department, and employee delete flows to confirm the corrected toast copy reads naturally in the live shell.

### Gaps Summary

The prior blocking gap is closed. The shipped Vietnamese feedback bundle no longer contains the corrupted `feedback.*` values that previously produced broken toast and HTTP error copy, and the helper regression spec now validates the committed `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` assets directly instead of relying on inline stubs. The targeted phase-relevant verification commands passed: `npm --prefix frontend exec ng test -- --watch=false --include src/app/shared/error/http-error.utils.spec.ts --include src/app/layout/component/menu/app.menu.spec.ts --include src/app.routes.spec.ts` reported 3 files and 11 tests passing, and `npm --prefix frontend run build` completed successfully.

Phase 6 now achieves its goal: the required JHipster support files and translation foundations are present, wired, and able to render core migrated shell and user-management text without the previously documented Vietnamese feedback gap. The remaining bundle-budget warning and hardcoded English confirmation dialogs are real but non-blocking residual risks for later phases.

---

_Verified: 2026-03-25T04:27:56Z_
_Verifier: Codex (gsd-verifier)_
