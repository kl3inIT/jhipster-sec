---
status: diagnosed
phase: 06-frontend-parity-foundation
source:
  - 06-01-SUMMARY.md
  - 06-02-SUMMARY.md
  - 06-03-SUMMARY.md
  - 06-04-SUMMARY.md
  - 06-05-SUMMARY.md
started: 2026-03-25T10:21:38.7745899+07:00
updated: 2026-03-25T10:47:46.9983881+07:00
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. Cold Start Smoke Test
expected: Stop any running frontend and backend processes, then start the application from a clean state. The backend should boot without migration/startup errors, the frontend should load without a blank page, and opening `/login` or `/` should show live UI instead of raw translation keys or startup failures.
result: pass

### 2. Login And Home Translation Surface
expected: In English, `/login` shows translated copy such as `Sign in`, `Username`, and `Password`. After signing in, the home page shows translated content such as `Welcome, Java Hipster!` and `This is your homepage` instead of raw i18n keys.
result: pass

### 3. Language Switch Persists And Rebuilds Shell
expected: Using the topbar language switch immediately updates visible shell labels, including items like `Home`, `Entities`, and `Security Admin`, and after a full page reload the selected language stays active.
result: pass

### 4. Route Titles And Error Pages Follow The Active Language
expected: Browser titles and page copy for routes such as `/login`, `/`, `/404`, `/error`, and `/admin/security/roles` resolve to translated text for the current language, with no raw keys like `pageTitle.*` or untranslated placeholders.
result: pass

### 5. Admin Route Guard And Security Mount
expected: As an admin, `/admin/security/roles`, `/admin/security/row-policies`, and `/admin/users` load under the shared admin route tree. As a non-admin or logged-out user, direct access to `/admin/...` is blocked rather than exposing admin content.
result: pass

### 6. User Management Placeholder Routes Resolve Cleanly
expected: `/admin/users` shows the translated placeholder screen for the users route, and placeholder routes such as `/admin/users/new` and `/admin/users/{login}/view` resolve to the same placeholder instead of 404 or route errors.
result: issue
reported: "i dont see user management in menu"
severity: major

### 7. Translated Success Alert For Security Mutations
expected: Creating, updating, or deleting a security role or row policy shows a visible success banner inside the shell, using translated copy such as `A security role is created with identifier ...` or the Vietnamese equivalent, and the banner can be dismissed.
result: issue
reported: "I’ve already switched the interface to Vietnamese, but error messages and success notifications (like “deleted successfully…”) are still showing in English"
severity: major

## Summary

total: 7
passed: 5
issues: 2
pending: 0
skipped: 0
blocked: 0

## Gaps

- truth: "The mounted `/admin/users` foundation is discoverable from the admin shell and resolves to the translated placeholder route instead of being hidden from navigation."
  status: failed
  reason: "User reported: i dont see user management in menu"
  severity: major
  test: 6
  root_cause: "The `/admin/users` route is mounted in `frontend/src/app/pages/admin/admin.routes.ts`, but `frontend/src/app/layout/component/menu/app.menu.ts` never adds a menu item for it. The shell only renders Home, Entities, and an admin-only Security section, so user management exists only as a deep link."
  artifacts:
    - path: "frontend/src/app/layout/component/menu/app.menu.ts"
      issue: "Admin menu model omits the `/admin/users` route entirely."
    - path: "frontend/src/app/layout/component/menu/app.menu.spec.ts"
      issue: "Menu tests only cover the security admin section and never assert discoverability of `/admin/users`."
    - path: "frontend/src/app/pages/admin/admin.routes.ts"
      issue: "The admin route tree exposes `users`, confirming the problem is navigation, not routing."
  missing:
    - "Add a translated `/admin/users` menu item to the admin-only shell navigation."
    - "Extend the menu regression spec to assert the user-management entry is visible for admins and survives language changes."
  debug_session: ".planning/debug/missing-user-management-menu-link.md"

- truth: "Security and related CRUD feedback messages render in the active language after a locale switch, instead of staying in English."
  status: failed
  reason: "User reported: I’ve already switched the interface to Vietnamese, but error messages and success notifications (like “deleted successfully…”) are still showing in English"
  severity: major
  test: 7
  root_cause: "The shell alert pipeline is translation-aware, but many CRUD flows bypass it. `frontend/src/app/shared/error/http-error.utils.ts` and several list components call PrimeNG `MessageService.add(...)` with hardcoded English summary/detail strings, so notifications remain English regardless of the active locale."
  artifacts:
    - path: "frontend/src/app/shared/error/http-error.utils.ts"
      issue: "Shared error helper hardcodes English summaries and details."
    - path: "frontend/src/app/pages/admin/security/roles/list/role-list.component.ts"
      issue: "Delete success and load/delete failures use hardcoded English MessageService text."
    - path: "frontend/src/app/pages/admin/security/row-policies/list/row-policy-list.component.ts"
      issue: "Delete success and load/delete failures use hardcoded English MessageService text."
    - path: "frontend/src/app/pages/entities/organization/list/organization-list.component.ts"
      issue: "Delete success and error toasts use hardcoded English text."
    - path: "frontend/src/app/pages/entities/department/list/department-list.component.ts"
      issue: "Delete success and error toasts use hardcoded English text."
    - path: "frontend/src/app/pages/entities/employee/list/employee-list.component.ts"
      issue: "Delete success and error toasts use hardcoded English text."
  missing:
    - "Move shared error and success toast copy into translation keys in the vi/en bundles."
    - "Use translated MessageService payloads in the security and entity list flows instead of hardcoded English strings."
    - "Add regression coverage for translated toast/error helpers after a language switch."
  debug_session: ".planning/debug/messages-not-localized-after-language-switch.md"
