---
status: complete
phase: 06-frontend-parity-foundation
source:
  - 06-01-SUMMARY.md
  - 06-02-SUMMARY.md
  - 06-03-SUMMARY.md
  - 06-04-SUMMARY.md
  - 06-05-SUMMARY.md
  - 06-06-SUMMARY.md
  - 06-VERIFICATION.md
started: 2026-03-25T10:21:38.7745899+07:00
updated: 2026-03-25T11:34:39.7370232+07:00
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
result: pass

### 7. Translated Success Alert For Security Mutations
expected: Creating, updating, or deleting a security role or row policy shows a visible success banner inside the shell, using translated copy such as `A security role is created with identifier ...` or the Vietnamese equivalent, and the banner can be dismissed.
result: pass

## Summary

total: 7
passed: 7
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps
