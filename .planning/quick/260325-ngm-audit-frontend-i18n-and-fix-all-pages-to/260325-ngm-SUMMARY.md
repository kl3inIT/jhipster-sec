# Quick Task Summary

- Quick ID: `260325-ngm`
- Task: Audit frontend i18n and convert remaining frontend pages to translation-backed UI text.

## Outcome

- Replaced remaining hard-coded UI strings across entity pages, security admin pages, and layout fragments with translation bindings.
- Added missing translation keys in both `frontend/src/i18n/en/**` and `frontend/src/i18n/vi/**`.
- Regenerated merged bundles in `frontend/public/i18n/*.json` and the i18n hash artifact.
- Updated impacted frontend specs to assert translated output instead of stale literals.

## Notable Changes

- Entity pages: organization, department, employee list/detail/update screens now use i18n for titles, labels, actions, validation text, paginator labels, and confirm dialogs.
- Security pages: roles, row policies, menu definitions, and permission matrix now use i18n for headings, tables, dialogs, buttons, confirm flows, menu-access labels, and empty states.
- Layout: sidebar brand, breadcrumb aria-label, and footer text now resolve through translation keys.
- Accessibility follow-up: menu definition icon-only action buttons now expose translated accessible labels.

## Verification

- `npm --prefix frontend run i18n:merge`
- `npm --prefix frontend run test -- --watch=false --include src/app/layout/component/main/app.layout.spec.ts --include src/app/pages/entities/organization/list/organization-list.component.spec.ts --include src/app/pages/admin/security/permission-matrix/permission-matrix.component.spec.ts`
- `npm --prefix frontend run build`

## Notes

- `frontend/src/app/app.html` still contains Angular starter/demo text, but the app bootstraps from the inline template in `frontend/src/app/app.ts`, so that file is not part of the runtime shell.
- Frontend build passes, but the existing Angular bundle budget warning remains: initial bundle `895.63 kB` vs configured budget `500.00 kB`.
