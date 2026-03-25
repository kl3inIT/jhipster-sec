---
phase: 06-frontend-parity-foundation
plan: 06
subsystem: ui
tags: [angular, i18n, menu, toast, primeng]
requires:
  - phase: 06-frontend-parity-foundation
    provides: translated shell navigation and language-driven menu rebuilds from plan 04
  - phase: 06-frontend-parity-foundation
    provides: live /admin/users route foundation and translated root routing from plan 05
provides:
  - admin-visible /admin/users discoverability from the shell
  - translation-backed PrimeNG toast feedback for the targeted security and entity list flows
  - regression coverage for locale-aware menu and HTTP error toast behavior
affects: [07-enterprise-navigation-shell, 08-user-management-delivery, 10-frontend-reliability-and-regression-coverage]
tech-stack:
  added: []
  patterns: [translation-aware-toast-helper, admin-menu-discoverability, locale-driven-feedback]
key-files:
  created:
    - frontend/src/app/shared/error/http-error.utils.spec.ts
  modified:
    - frontend/src/app/layout/component/menu/app.menu.ts
    - frontend/src/app/layout/component/menu/app.menu.spec.ts
    - frontend/src/app/shared/error/http-error.utils.ts
    - frontend/src/app/pages/admin/security/roles/list/role-list.component.ts
    - frontend/src/app/pages/admin/security/row-policies/list/row-policy-list.component.ts
    - frontend/src/app/pages/entities/organization/list/organization-list.component.ts
    - frontend/src/app/pages/entities/department/list/department-list.component.ts
    - frontend/src/app/pages/entities/employee/list/employee-list.component.ts
    - frontend/public/i18n/en.json
    - frontend/public/i18n/vi.json
key-decisions:
  - "Expose `/admin/users` through the existing admin-only Security Admin menu section using the donor `global.menu.admin.userManagement` key instead of inventing a new root shell group in phase 6."
  - "Centralize translated toast creation in `http-error.utils.ts` so direct `MessageService` callers resolve copy from the active locale at message creation time."
patterns-established:
  - "PrimeNG toast localization: list flows should emit translated summary and detail strings through shared helpers rather than hardcoded English literals."
  - "Admin discoverability: mounted admin routes are not considered complete until the shell exposes a guarded navigation path to them."
requirements-completed: [I18N-01, I18N-02]
duration: 39min
completed: 2026-03-25
---

# Phase 6: Plan 06 Summary

**Admin users can now reach `/admin/users` from the shell, and targeted security/entity toast feedback follows the active language instead of staying in English**

## Performance

- **Duration:** 39 min
- **Started:** 2026-03-25T10:48:50+07:00
- **Completed:** 2026-03-25T11:27:56+07:00
- **Tasks:** 2 planned tasks, 1 verifier-driven follow-up
- **Files modified:** 22

## Accomplishments

- Added an admin-only user-management menu entry so the mounted `/admin/users` foundation is discoverable from the live shell.
- Replaced hardcoded English `MessageService` feedback in the targeted security and entity list flows with translation-backed summaries and details.
- Added focused regression coverage for translated menu rebuilds and HTTP error or success toast behavior.
- Hardened the helper regression spec to read the shipped `en.json` and `vi.json` assets directly, which caught and fixed corrupted Vietnamese feedback strings before closeout.

## Task Commits

Each task was committed atomically:

1. **Task 1: Expose user management from the admin shell menu** - `7b8e071` (`fix(frontend): expose admin user management menu`)
2. **Task 2: Localize MessageService success and error feedback** - `22f81e1` (`fix(frontend): localize admin and entity toast feedback`)
3. **Verification follow-up: validate shipped bundles and fix Vietnamese feedback copy** - `002f5e5` (`test(frontend): verify shipped feedback bundles`)

## Files Created/Modified

- `frontend/src/app/layout/component/menu/app.menu.ts` - adds the translated `/admin/users` entry inside the admin-only shell menu
- `frontend/src/app/layout/component/menu/app.menu.spec.ts` - verifies admin discoverability, non-admin hiding, and language-switch stability
- `frontend/src/app/shared/error/http-error.utils.ts` - centralizes translation-aware toast creation and HTTP error fallback handling
- `frontend/src/app/shared/error/http-error.utils.spec.ts` - covers English and Vietnamese toast translation behavior
- `frontend/src/app/pages/admin/security/roles/list/role-list.component.ts` - routes role list load/delete feedback through translated toasts
- `frontend/src/app/pages/admin/security/row-policies/list/row-policy-list.component.ts` - routes row-policy list load/delete feedback through translated toasts
- `frontend/src/app/pages/entities/organization/list/organization-list.component.ts` - replaces hardcoded English delete/error toasts with translated feedback
- `frontend/src/app/pages/entities/department/list/department-list.component.ts` - replaces hardcoded English delete/error toasts with translated feedback
- `frontend/src/app/pages/entities/employee/list/employee-list.component.ts` - replaces hardcoded English delete/error toasts with translated feedback
- `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` - add the new shared feedback keys consumed by the shell and list flows
- `frontend/tsconfig.spec.json` - enables Node typings so the helper spec can read the committed translation bundles directly

## Decisions Made

- Kept user management discoverability inside the existing admin-only shell section so phase 6 closes the gap without pre-empting phase 7 navigation redesign.
- Let helper callers pass either translation keys or literal fallback text so dependent save flows stay compatible while the targeted list flows become fully translation-backed now.

## Deviations from Plan

### Auto-fixed Issues

**1. Dependent TranslateService wiring for shared error helper adoption**
- **Found during:** Task 2 (Localize MessageService success and error feedback)
- **Issue:** Making the shared HTTP error helper translation-aware broke dependent update/dialog/spec surfaces that instantiate components without `TranslateService`.
- **Fix:** Wired `TranslateService` into the affected callers and added the minimal spec providers required for those components to compile cleanly.
- **Files modified:** `frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts`, `frontend/src/app/pages/admin/security/roles/dialog/role-dialog.component.ts`, `frontend/src/app/pages/admin/security/row-policies/dialog/row-policy-dialog.component.ts`, `frontend/src/app/pages/entities/organization/update/organization-update.component.ts`, `frontend/src/app/pages/entities/department/update/department-update.component.ts`, `frontend/src/app/pages/entities/employee/update/employee-update.component.ts`, and the related specs
- **Verification:** `npm --prefix frontend exec ng test -- --watch=false --include src/app/layout/component/menu/app.menu.spec.ts --include src/app/shared/error/http-error.utils.spec.ts` and `npm --prefix frontend run build`
- **Committed in:** `22f81e1`

**2. Verifier-driven bundle-backed regression hardening**
- **Found during:** Phase verification
- **Issue:** The first helper spec relied on inline translations, which failed to prove the committed `vi.json` bundle was intact; verifier review then found corrupted Vietnamese `feedback.*` copy in the shipped asset.
- **Fix:** Switched `http-error.utils.spec.ts` to read `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` from disk, added Node typings in `frontend/tsconfig.spec.json`, and corrected the shipped Vietnamese feedback strings.
- **Files modified:** `frontend/src/app/shared/error/http-error.utils.spec.ts`, `frontend/public/i18n/vi.json`, and `frontend/tsconfig.spec.json`
- **Verification:** `npm --prefix frontend exec ng test -- --watch=false --include src/app/shared/error/http-error.utils.spec.ts --include src/app/layout/component/menu/app.menu.spec.ts --include src/app.routes.spec.ts` and `npm --prefix frontend run build`
- **Committed in:** `002f5e5`

---

**Total deviations:** 2 auto-fixed (shared-helper adoption plus verifier-driven bundle hardening)
**Impact on plan:** No scope creep. The follow-up commit tightened proof that the shipped translation assets, not just test stubs, satisfy the phase goal.

## Issues Encountered

- The first targeted test run surfaced a PrimeNG typing mismatch in the new helper spec (`MessageService` stub shape plus non-exported `Message` type). Both were corrected before the green rerun.
- Verification then exposed corrupted Vietnamese `feedback.*` strings in the committed bundle. The bundle-backed spec and Unicode copy fix closed that gap before phase sign-off.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 6's two diagnosed UAT gaps now have committed code coverage and green verification commands.
- Phase 7 can assume admin user management is discoverable from the shell and that targeted toast feedback respects the active locale.
- Residual non-blocking debt remains: the frontend initial bundle still exceeds the configured warning budget, and delete confirmation dialogs in the affected list views are still English.

---
*Phase: 06-frontend-parity-foundation*
*Completed: 2026-03-25*
