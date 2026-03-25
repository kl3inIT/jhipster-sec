---
status: resolved
trigger: "Phase 6 UAT gap: feedback messages stay in English after switching to Vietnamese"
created: 2026-03-25T10:47:46.9983881+07:00
updated: 2026-03-25T11:34:39.7370232+07:00
---

## Current Focus
<!-- OVERWRITE on each update - reflects NOW -->

hypothesis: Confirmed. The untranslated feedback path bypassed the shell's translation-aware alerts.
test: Shared helper regression coverage, targeted menu or route regression, and phase verification.
expecting: Resolved.
next_action: none

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: Success and error notifications should respect the active language after the user switches the shell to Vietnamese.
actual: Delete-success and error toasts still rendered in English.
reproduction: Switch the UI to Vietnamese, then trigger delete or error flows from security or entity list pages.

## Evidence
<!-- APPEND only - facts discovered -->

- The old `frontend/src/app/shared/error/http-error.utils.ts` hardcoded English MessageService payloads for access-denied and generic error states.
- The old role, row-policy, organization, department, and employee list components all emitted hardcoded English toast content directly through PrimeNG `MessageService`.
- The header-based shell alert path (`frontend/src/app/core/interceptor/notification.interceptor.ts` -> `frontend/src/app/core/util/alert.service.ts`) was already translation-aware, isolating the issue to the direct MessageService path.
- `frontend/src/app/shared/error/http-error.utils.ts` now resolves `feedback.*` translation keys through `TranslateService` at message creation time.
- `frontend/src/app/shared/error/http-error.utils.spec.ts` now reads the committed `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` bundles directly, which exposed and then verified the corrected Vietnamese `feedback.*` copy.

## Resolution
<!-- OVERWRITE as understanding evolves -->

root_cause: Local CRUD flows bypassed the translation-aware alert pipeline and sent literal English strings into PrimeNG `MessageService`, so notifications stayed English after locale switches.
fix: Introduced translation-backed summary and detail keys in `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json`, routed the shared HTTP error helper and targeted security or entity list flows through those keys, wired dependent callers to `TranslateService`, and then hardened the regression spec to validate the shipped bundles directly.
verification: Commit `22f81e1` localized the direct MessageService flows, commit `002f5e5` fixed the shipped Vietnamese bundle and bundle-backed spec, the focused Angular tests passed, and `.planning/phases/06-frontend-parity-foundation/06-VERIFICATION.md` now marks the active-locale feedback requirement as verified.
files_changed:
  - frontend/src/app/shared/error/http-error.utils.ts
  - frontend/src/app/shared/error/http-error.utils.spec.ts
  - frontend/src/app/pages/admin/security/roles/list/role-list.component.ts
  - frontend/src/app/pages/admin/security/row-policies/list/row-policy-list.component.ts
  - frontend/src/app/pages/entities/organization/list/organization-list.component.ts
  - frontend/src/app/pages/entities/department/list/department-list.component.ts
  - frontend/src/app/pages/entities/employee/list/employee-list.component.ts
  - frontend/public/i18n/en.json
  - frontend/public/i18n/vi.json
  - frontend/tsconfig.spec.json
