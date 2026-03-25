---
status: diagnosed
trigger: "Phase 6 UAT gap: /admin/users is not discoverable from the shell menu"
created: 2026-03-25T10:47:46.9983881+07:00
updated: 2026-03-25T10:47:46.9983881+07:00
---

## Symptoms

expected: Admin users should be able to discover the mounted `/admin/users` route from the shell navigation and open the phase-6 placeholder screen.
actual: The route exists, but the user cannot find user management in the menu.
reproduction: Sign in as an admin and inspect the shell menu after phase 6.

## Evidence

- `frontend/src/app/pages/admin/admin.routes.ts` mounts `path: 'users'`.
- `frontend/src/app/pages/admin/user-management/user-management.routes.ts` and `user-management-placeholder.component.ts` confirm the placeholder screen exists.
- `frontend/src/app/layout/component/menu/app.menu.ts` only renders Home, Entities, and the admin-only Security section, with no `/admin/users` item.
- `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` already contain `global.menu.admin.userManagement`.

## Resolution

root_cause: The route foundation shipped, but the menu model never exposed it. This is a shell discoverability omission, not a routing failure.
recommended_fix:
  - Add a translated `/admin/users` entry to the admin-only shell menu.
  - Extend `app.menu.spec.ts` to verify the entry is present for admins and remains translated after a language change.
