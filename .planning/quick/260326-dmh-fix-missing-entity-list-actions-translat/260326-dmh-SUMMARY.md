# Quick Task Summary

- Quick ID: `260326-dmh`
- Task: Fix missing `entity.list.actions` translation and confirm why menu visibility did not refresh immediately for the current admin user.

## Outcome

- Added the missing `entity.list.actions` key to the canonical source translation files in `frontend/src/i18n/en/entity.json` and `frontend/src/i18n/vi/entity.json`.
- Regenerated the runtime bundles in `frontend/public/i18n/*.json` through `npm --prefix frontend run i18n:merge`.
- Confirmed the regenerated translation bundles now resolve `entity.list.actions` to `Actions` and `Thao tác`.
- Confirmed the observed menu-visibility lag is not a hardcoded admin bypass in `CurrentUserMenuPermissionService`.

## Investigation Findings

- Current-user menu visibility is calculated as the union of ALLOW grants across all current authorities, with DENY taking precedence per menu id.
- The default seeded `admin` user in the database starts with both `ROLE_ADMIN` and `ROLE_USER`.
- If you change the currently logged-in user's roles, a browser reload alone does not fully refresh the effective menu state:
  - the JWT still carries the old authority claim until logout or re-login
  - `NavigationService` also reuses cached `allowedMenuIds` from session storage until auth state changes
- The fact that logout and login fixed the menu proves the stale-session explanation, not a permanent frontend bypass.

## Verification

- `npm --prefix frontend run i18n:merge`
- Parsed `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` with Node and confirmed `entity.list.actions` resolves in both regenerated bundles.
- Updated [07.1-UAT.md](D:\jhipster\.planning\phases\07.1-menu-management\07.1-UAT.md) to mark test 8 passed and diagnose the menu-definition edit blocker.

## Residual Gaps

- Menu visibility is not yet live-update safe for the currently logged-in user after self-role changes or self-relevant menu-permission changes.
- Menu-definition edit remains blocked because the frontend update request omits `id` in the PUT body while the backend requires body id to match the path id.
