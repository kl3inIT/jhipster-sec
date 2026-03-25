---
phase: 06-frontend-parity-foundation
plan: 01
subsystem: ui
tags: [angular, ngx-translate, i18n, dayjs, routing]
requires: []
provides:
  - static vi and en translation bundles under frontend/public/i18n
  - standalone ngx-translate bootstrap wiring with missing-translation handling
  - translated route-title and dayjs locale synchronization
affects: [07-enterprise-navigation-shell, 08-user-management-delivery, 09-enterprise-ux-and-performance-hardening]
tech-stack:
  added: []
  patterns: [static-json-i18n-bundles, translated-route-titles, locale-precedence]
key-files:
  created:
    - frontend/public/i18n/en.json
    - frontend/public/i18n/vi.json
    - frontend/src/app/config/translation.config.ts
    - frontend/src/app/config/dayjs.ts
    - frontend/src/app/app-page-title-strategy.ts
  modified:
    - frontend/src/app.config.ts
    - frontend/src/app/app.ts
key-decisions:
  - "Serve two merged static translation bundles from frontend/public/i18n instead of recreating the old JHipster namespace-merge build step."
  - "Stored locale in StateStorageService wins over the default locale, and account.langKey only applies when no locale has been stored."
  - "Route titles, html[lang], and dayjs locale are refreshed from one shared language-change path."
patterns-established:
  - "Static translation contract: donor namespaces stay intact inside en.json and vi.json."
  - "Title strategy pattern: routes carry translation keys and AppPageTitleStrategy resolves them at runtime."
requirements-completed: [I18N-01, I18N-02]
duration: 2min
completed: 2026-03-25
---

# Phase 6: Plan 01 Summary

**Static vi/en donor bundles, standalone ngx-translate wiring, and translated route-title plus dayjs locale plumbing for the new frontend shell**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-25T10:01:49+07:00
- **Completed:** 2026-03-25T10:04:00+07:00
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments

- Merged donor translation namespaces and new shell/security keys into static `en.json` and `vi.json` bundles.
- Added standalone translation bootstrap helpers, missing-translation handling, and default/fallback locale constants.
- Wired translated route titles, `document.documentElement.lang`, and dayjs locale updates into app bootstrap.

## Task Commits

No atomic task commits were created. The repository entered phase execution with unrelated dirty planning artifacts, so verification was completed in-place without committing.

## Files Created/Modified

- `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` - canonical donor-backed translation bundles with shell additions
- `frontend/src/app/config/translation.config.ts` - static loader and missing-translation handler contract
- `frontend/src/app/config/dayjs.ts` - registered `vi` and `en` dayjs locale bridge
- `frontend/src/app/app-page-title-strategy.ts` - translated document-title strategy
- `frontend/src/app.config.ts` and `frontend/src/app/app.ts` - standalone translation bootstrap and locale lifecycle wiring

## Decisions Made

- Used static JSON bundles as the production translation artifact to avoid reviving the old namespace merge pipeline.
- Kept locale persistence in `sessionStorage` through `StateStorageService` to match the donor contract.
- Used translation keys in route metadata so later phases can change copy without touching the route tree.

## Deviations from Plan

None - plan executed as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Alert, menu, topbar, and admin foundations can now consume a stable translation contract and title strategy.

---
*Phase: 06-frontend-parity-foundation*
*Completed: 2026-03-25*
