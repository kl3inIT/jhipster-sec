---
status: resolved
trigger: "Phase 6 UAT gap: /admin/users is not discoverable from the shell menu"
created: 2026-03-25T10:47:46.9983881+07:00
updated: 2026-03-25T11:34:39.7370232+07:00
---

## Current Focus
<!-- OVERWRITE on each update - reflects NOW -->

hypothesis: Confirmed. The gap was shell discoverability only.
test: Menu route and translation regression coverage plus phase verification.
expecting: Resolved.
next_action: none

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: Admin users should be able to discover the mounted `/admin/users` route from the shell navigation and open the phase-6 placeholder screen.
actual: The route existed, but the user could not find user management in the menu.
reproduction: Sign in as an admin and inspect the shell menu after phase 6.

## Evidence
<!-- APPEND only - facts discovered -->

- `frontend/src/app/pages/admin/admin.routes.ts` mounts `path: 'users'`.
- `frontend/src/app/pages/admin/user-management/user-management.routes.ts` and `user-management-placeholder.component.ts` confirm the placeholder screen exists.
- The old `frontend/src/app/layout/component/menu/app.menu.ts` model rendered Home, Entities, and the admin-only Security section, but omitted `/admin/users`.
- `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` already contained `global.menu.admin.userManagement`.
- `frontend/src/app/layout/component/menu/app.menu.spec.ts` now asserts the admin-only user-management entry is present, hidden for non-admin users, and survives language changes.

## Resolution
<!-- OVERWRITE as understanding evolves -->

root_cause: The `/admin/users` route foundation shipped, but the admin shell menu never exposed it, so user management was reachable only by deep link.
fix: Added a translated admin-only `/admin/users` menu item in `frontend/src/app/layout/component/menu/app.menu.ts` using the existing `global.menu.admin.userManagement` key and extended `frontend/src/app/layout/component/menu/app.menu.spec.ts` to cover admin visibility, non-admin hiding, and language-switch stability.
verification: Commit `7b8e071` landed the menu fix, the focused menu spec passed, and `.planning/phases/06-frontend-parity-foundation/06-VERIFICATION.md` now marks admin discoverability as verified.
files_changed:
  - frontend/src/app/layout/component/menu/app.menu.ts
  - frontend/src/app/layout/component/menu/app.menu.spec.ts
