---
phase: 06-frontend-parity-foundation
plan: 04
subsystem: ui
tags: [angular, shell, menu, topbar, i18n]
requires:
  - phase: 06-frontend-parity-foundation
    provides: translated runtime contract from plan 01
provides:
  - translated menu and topbar shell surfaces
  - persisted vi and en language switching from the topbar
  - translated login, home, and error page templates
affects: [07-enterprise-navigation-shell, 09-enterprise-ux-and-performance-hardening]
tech-stack:
  added: []
  patterns: [translated-menu-model, persisted-locale-switcher, shell-surface-i18n]
key-files:
  created: []
  modified:
    - frontend/src/app/layout/component/menu/app.menu.ts
    - frontend/src/app/layout/component/topbar/app.topbar.ts
    - frontend/src/app/layout/component/topbar/app.topbar.html
    - frontend/src/app/pages/login/login.component.html
    - frontend/src/app/pages/home/home.component.html
    - frontend/src/app/pages/error/access-denied.component.html
    - frontend/src/app/pages/error/not-found.component.html
    - frontend/src/app/pages/error/error.component.html
key-decisions:
  - "Rebuild the shell menu on both auth-state and language-change events so translated labels stay current."
  - "Persist locale switches through StateStorageService and keep the topbar button state derived from the active translate language."
  - "Track menu root items with stable ids rather than translated labels so language changes do not destabilize the Angular tree."
patterns-established:
  - "Translated shell model: menu labels are built via TranslateService.instant and refreshed from runtime events."
  - "Locale switcher pattern: topbar writes locale state, triggers translateService.use, and reflects the active language in UI state."
requirements-completed: [I18N-01, I18N-02]
duration: 3min
completed: 2026-03-25
---

# Phase 6: Plan 04 Summary

**Translated shell, login, and error surfaces with persisted vi/en switching and stable menu recomputation**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-25T10:07:30+07:00
- **Completed:** 2026-03-25T10:10:30+07:00
- **Tasks:** 1
- **Files modified:** 10

## Accomplishments

- Replaced hardcoded shell/login/error copy with translation keys from the merged bundles.
- Added topbar language switching that persists through `StateStorageService`.
- Ensured menu labels rebuild when authentication or language state changes.

## Task Commits

No atomic task commits were created. The repository entered phase execution with unrelated dirty planning artifacts, so verification was completed in-place without committing.

## Files Created/Modified

- `frontend/src/app/layout/component/menu/app.menu.ts` - translated menu model with auth/lang refresh behavior
- `frontend/src/app/layout/component/topbar/app.topbar.ts` and `.html` - persisted locale switcher and translated controls
- `frontend/src/app/pages/login/login.component.html` - translated login copy
- `frontend/src/app/pages/home/home.component.html` - translated home copy
- `frontend/src/app/pages/error/*.html` - translated access-denied, not-found, and generic error screens

## Decisions Made

- Kept shell translations in component logic instead of hardcoded template literals so later menu refactors only touch translation keys.
- Stabilized `@for` tracking with root menu ids because translated labels are not a safe tracking key.

## Deviations from Plan

### Auto-fixed Issues

**1. Stable menu tracking for language changes**
- **Found during:** full frontend unit-suite verification
- **Issue:** the menu template tracked items by translated labels, which caused an Angular `ExpressionChangedAfterItHasBeenCheckedError` when switching languages.
- **Fix:** added stable root `id` values to the menu model and changed the template tracking expression to `item.id ?? $index`.
- **Verification:** `npm --prefix frontend exec ng test -- --watch=false`

---

**Total deviations:** 1 auto-fixed (runtime-stability fix)
**Impact on plan:** Necessary correctness fix for language switching. No scope expansion beyond the translated shell.

## Issues Encountered

- Language-change verification exposed unstable tracking in the translated menu; the fix is now part of the component contract.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

The shell now exposes translated navigation and locale switching for the larger admin and UX phases that follow.

---
*Phase: 06-frontend-parity-foundation*
*Completed: 2026-03-25*
