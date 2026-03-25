---
status: diagnosed
trigger: "Phase 6 UAT gap: feedback messages stay in English after switching to Vietnamese"
created: 2026-03-25T10:47:46.9983881+07:00
updated: 2026-03-25T10:47:46.9983881+07:00
---

## Symptoms

expected: Success and error notifications should respect the active language after the user switches the shell to Vietnamese.
actual: Messages such as delete-success and error toasts still render in English.
reproduction: Switch the UI to Vietnamese, then trigger delete or error flows from security or entity list pages.

## Evidence

- `frontend/src/app/shared/error/http-error.utils.ts` hardcodes English MessageService payloads for access denied and generic errors.
- `frontend/src/app/pages/admin/security/roles/list/role-list.component.ts` hardcodes English delete/load failure toasts.
- `frontend/src/app/pages/admin/security/row-policies/list/row-policy-list.component.ts` hardcodes English delete/load failure toasts.
- `frontend/src/app/pages/entities/organization/list/organization-list.component.ts`, `department/list.component.ts`, and `employee/list.component.ts` hardcode English delete success and error toasts.
- The header-based shell alert path (`frontend/src/app/core/interceptor/notification.interceptor.ts` -> `frontend/src/app/core/util/alert.service.ts`) already uses translation keys, so the problem is the separate MessageService path.

## Resolution

root_cause: Local CRUD flows bypass the translation-aware alert pipeline and send literal English strings into PrimeNG `MessageService`.
recommended_fix:
  - Introduce translation-backed toast/error helpers for MessageService flows.
  - Move the affected summary/detail copy into `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json`.
  - Add regression coverage that verifies translated toast/error output after switching languages.
