---
phase: 08
slug: user-management-delivery
status: approved
shadcn_initialized: false
preset: none
created: 2026-03-25
reviewed_at: 2026-03-25T21:38:12.0666929+07:00
---

# Phase 08 - UI Design Contract

> Visual and interaction contract for the admin user-management experience in `frontend/`.
> Generated for Phase 8 and aligned to the project's PrimeNG-first rule.

---

## Design System

| Property | Value | Source |
|----------|-------|--------|
| Tool | none (shadcn not applicable - Angular/PrimeNG stack) | project stack |
| Preset | PrimeNG Aura preset with Sakai shell chrome, emerald primary, light default, dark mode via `.app-dark` | `frontend/src/app.config.ts`, Phase 7 UI-SPEC |
| Component library | PrimeNG 21.1.3 + PrimeFlex 4 + existing Tailwind utility layer where already present | `frontend/package.json` |
| Icon library | PrimeIcons 7 (`pi pi-*`) | `frontend/package.json` |
| Font | `Lato, sans-serif` | `frontend/src/assets/layout/_core.scss` |
| Component sourcing | PrimeNG-first. Use official `https://primeng.org/` components and current examples whenever a suitable component exists. Custom UI is allowed only for layout composition or gaps where PrimeNG has no suitable component. | user decision captured on 2026-03-25 |

---

## Spacing Scale

Declared values (must be multiples of 4):

| Token | Value | Usage | Source |
|-------|-------|-------|--------|
| xs | 4px | Inline icon gaps, dense status spacing, sort affordance spacing | inherited from Phase 7 |
| sm | 8px | Compact control gaps, checkbox-label spacing, breadcrumb separators | inherited from Phase 7 |
| md | 16px | Default form spacing, row-action spacing, card internals | inherited from Phase 7 |
| lg | 24px | Workspace section padding and header-to-content spacing | inherited from Phase 7 |
| xl | 32px | Desktop gutters and major split-page spacing | inherited from Phase 7 |
| 2xl | 48px | Empty and error state spacing | inherited from Phase 7 |
| 3xl | 64px | Wide desktop workspace breathing room | inherited from Phase 7 |

Exceptions:
- Icon-only action buttons must expose a minimum 44x44px hit target.
- The user detail/edit split may collapse from two columns to one below 1024px, but internal card padding stays at least 16px.

---

## Typography

| Role | Size | Weight | Line Height | Source |
|------|------|--------|-------------|--------|
| Body | 14px | 400 | 1.5 | `frontend/src/assets/layout/_core.scss`, Phase 7 UI-SPEC |
| Label | 14px | 600 | 1.4 | Phase 7 UI-SPEC |
| Heading | 24px | 600 | 1.2 | Phase 7 UI-SPEC |
| Display | 32px | 600 | 1.2 | Phase 7 UI-SPEC |

Rules:
- Use only 400 and 600 weights for new Phase 8 work.
- Table cells, helper copy, and field values stay at 14px.
- Metadata labels and muted supporting text use the existing neutral text tokens, not a smaller custom size.

---

## Color

| Role | Value | Usage | Source |
|------|-------|-------|--------|
| Dominant (60%) | `#f1f5f9` light / `#09090b` dark | Workspace background and page canvas | Phase 7 UI-SPEC |
| Secondary (30%) | `#ffffff` light / `#18181b` dark | Cards, tables, role panel, split-page surfaces | Phase 7 UI-SPEC |
| Accent (10%) | `#10b981` light / `#34d399` dark | Primary CTA, active breadcrumb terminal, active pagination state, selected role counts, focused search affordance | Phase 7 UI-SPEC |
| Destructive | `#ef4444` light / `#f87171` dark | Delete actions and destructive confirmations only | Phase 7 UI-SPEC |

Accent reserved for:
- Primary page CTA such as `New User`
- Active or selected states that PrimeNG already themes, such as paginator current page and selected controls
- Breadcrumb current page
- Small positive status accents, never all links and never all row actions

Rule:
- Implement color through PrimeNG and Sakai tokens, not hardcoded component-level hex values.

---

## PrimeNG Component Contract

All phase surfaces must be built from official PrimeNG components first and follow the current examples on `primeng.org`. Custom components in this phase may compose layout and route structure around PrimeNG primitives, but they must not replace a suitable PrimeNG control.

| Use Case | PrimeNG Component | Official Docs | Contract |
|----------|-------------------|---------------|----------|
| User list workspace | `p-table` | `https://primeng.org/table` | Use the official sortable, paginated table pattern as the base for browse, search result, and dense admin-row interactions. |
| Search input | `p-iconfield`, `p-inputicon`, `p-inputtext`, `p-fluid` | `https://primeng.org/iconfield`, `https://primeng.org/inputtext`, `https://primeng.org/fluid` | Search is one prominent input above the table with a leading search icon and full-width behavior on narrow layouts. |
| Page hierarchy | `p-breadcrumb` | `https://primeng.org/breadcrumb` | Detail, edit, and create routes use PrimeNG breadcrumb instead of a custom breadcrumb widget. |
| Primary and row actions | `p-button` | `https://primeng.org/button` | Inline row actions remain visible in the table. Use severity, variant, and icon patterns from the official button docs rather than custom button markup. |
| Status display | `p-tag` | `https://primeng.org/tag` | Activation state and count summaries use tags, not custom badge CSS. |
| Role assignment | `p-table` + `p-checkbox` | `https://primeng.org/table`, `https://primeng.org/checkbox` | Roles render in a dense checkbox table on the right-side panel for detail and edit flows. |
| Delete confirmation | `p-confirmdialog` | `https://primeng.org/confirmdialog` | Delete must use the shared confirmation-service pattern from the official docs. |
| Feedback | `p-toast` | `https://primeng.org/toast` | Success and error feedback use toast overlays driven by the shared message service. |
| Loading state | `p-skeleton` | `https://primeng.org/skeleton` | Initial list, detail, and role-loading states use skeletons instead of custom spinners where content shape is known. |
| Content surface | `p-card` | `https://primeng.org/card` | Primary page surfaces use PrimeNG cards or card-like Sakai surfaces; avoid custom panel primitives when `p-card` is suitable. |

Non-negotiable rules:
1. Do not introduce a custom table, custom checkbox list, custom breadcrumb, or custom confirmation modal for this phase.
2. Do not prefer `ng-bootstrap` or ad hoc HTML controls when a PrimeNG component above already fits the use case.
3. Custom code is allowed only for the split-page workspace composition, route-level shells, and read-only rendering details that PrimeNG does not provide directly.

---

## Visual Hierarchy Contract

- List route focal point: the header row with `New User` as the primary CTA and the prominent search field as the main scanning control before the dense admin table.
- Secondary emphasis on the list route: the user table itself, with activation tags and inline actions remaining visible but visually subordinate to the header row.
- Detail and edit focal point: the left-side identity card title and primary action row, with the right-side role card acting as the supporting access-context surface.
- Destructive actions must never become the strongest visual element in the default state. Delete stays danger-styled only at the row-action level and inside the confirmation dialog.

---

## List Workspace Contract

Source: `08-CONTEXT.md` decisions D-01 through D-18.

### Route and layout

- Primary route: `/admin/users`
- Layout host: existing admin shell with breadcrumb row, page title, and page action row before content
- Main surface: one primary `p-card` or equivalent Sakai content surface containing search controls and the user table

### Search and browse

- Search sits above the table as one prominent PrimeNG search field using `p-iconfield`, `p-inputicon`, and `p-inputtext`.
- Search placeholder copy: `Search by login, email, or name`
- Search updates while typing with a 300ms debounce.
- Search preserves the normal paginated and sortable table contract; it must not switch into a separate lookup mode.
- Search state, page, and sort must round-trip through `WorkspaceContextService` so return navigation restores the previous list context.

### Table structure

- Base component: `p-table` with backend paging and sort integration.
- Default columns in this order: `login`, full name, `email`, activation status, assigned roles, last modified, row actions.
- Activation status renders with `p-tag`, not raw text.
- Assigned roles render as a compact list. Show the first two roles inline, then summarize the remainder as `+N more` when needed.
- Last modified renders as neutral metadata text, not a dominant cell.

### Row actions

- Keep actions inline in the last column. Do not move them into an overflow menu.
- Visible row actions: `View User`, `Edit User`, `Activate User` or `Deactivate User`, and `Delete User`.
- Activation and deactivation are immediate actions with toast feedback and no extra confirmation dialog.
- Delete uses `p-confirmdialog`.
- The current admin user's own row disables `Deactivate` and `Delete` inline.

### Empty and loading states

- Empty state heading: `No users found`
- Empty state body: `Try another search or create a new user.`
- Use `p-skeleton` rows while initial list data is loading.
- Load failures stay inside the workspace surface with retry affordance plus a toast for request failure details.

---

## Detail and Edit Split-Page Contract

Source: `08-CONTEXT.md` decisions D-03, D-05, D-06, D-11 through D-14.

### Route model

- Detail route: `:login/view`
- Create route: `new`
- Edit route: `:login/edit`
- Detail is the default drill-in destination from the list. Edit is always an explicit follow-up action.

### Layout

- Desktop: split page with a 65/35 visual balance.
- Left column: user profile card containing identity, contact, locale, activation state, and audit metadata when available.
- Right column: role-assignment card containing the role table.
- Mobile and narrow tablet: collapse to a single column with the user card first and the role card second.

### Left-side user card

- Use PrimeNG form controls for edit mode and read-only value rows for detail mode.
- Preferred controls: `p-inputtext` for login, first name, last name, and email; `p-checkbox` binary for activation when editing.
- Use `p-fluid` so form controls span the available card width.
- Validation messages stay inline beneath the owning field using existing translated error handling patterns.

### Right-side role card

- Use a dense `p-table` plus `p-checkbox` rows for role assignment.
- Each row shows role code and a human-friendly description or label.
- Detail mode keeps the full role table visible but disabled.
- Edit and create modes enable the role checkboxes directly in the same right-side panel.

### New vs. edit vs. detail

- New and edit reuse the same split-page layout to avoid a second form pattern.
- Detail mode is read-only and exposes a primary `Edit User` action.
- Edit mode keeps the role table visible while fields become editable; role assignment is not a separate screen or dialog.

---

## Feedback and Safety Contract

| Scenario | PrimeNG Pattern | Contract |
|----------|-----------------|----------|
| User save success | `p-toast` success | Show success feedback after create and update. |
| User save failure | `p-toast` error plus inline field errors where available | Preserve backend validation semantics and do not mask field-level errors. |
| Activate or deactivate | `p-toast` success or error | No confirmation dialog. Feedback must be fast and obvious. |
| Delete user | `p-confirmdialog` + `p-toast` | Confirmation is mandatory before delete. |
| List, detail, or authority load pending | `p-skeleton` | Match the expected content shape instead of showing a blank surface. |
| Self-deactivate or self-delete | disabled `p-button` | Disable inline and keep the row visually readable. |

Delete confirmation copy:
- Header: `Delete User`
- Message: `Are you sure you want to delete {{ login }}? This action cannot be undone.`
- Accept label: `Delete`
- Reject label: `Keep User`

---

## Copywriting Contract

| Element | Copy |
|---------|------|
| Primary CTA | `New User` |
| Detail primary action | `Edit User` |
| Search placeholder | `Search by login, email, or name` |
| Empty state heading | `No users found` |
| Empty state body | `Try another search or create a new user.` |
| Error state | `We couldn't load users. Refresh the page. If the problem continues, contact an administrator.` |
| Destructive confirmation | `Delete User`: `Are you sure you want to delete {{ login }}? This action cannot be undone.` |

Button and row-action labels:
- `View User`
- `Edit User`
- `Activate User`
- `Deactivate User`
- `Delete User`
- `Save User`
- `Create User`
- `Discard Changes`

Copy rules:
- Keep admin language direct and operational.
- Do not use playful or marketing phrasing.
- Use the same noun consistently: `User`, not mixed `account`, `member`, or `profile`.

---

## Accessibility and Translation Contract

| Area | Requirement |
|------|-------------|
| Focus management | Route changes to list, detail, create, and edit place focus on the page H1. |
| Search input | The search field must have a visible label or an accessible name that matches the translated page copy. |
| Row actions | Icon buttons require accessible labels; text-plus-icon buttons are preferred where space allows. |
| Disabled self actions | Self-deactivate and self-delete must remain visibly disabled and understandable without relying on color alone. |
| Role table | Checkbox rows must keep proper label association for keyboard and screen-reader use. |
| Contrast | Status tags, buttons, and table states must meet WCAG AA using PrimeNG token colors. |
| Translation | All labels, button text, empty states, error states, confirmation copy, and breadcrumb labels must be translation-key driven in both `en` and `vi`. |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | none | not applicable - Angular/PrimeNG stack |
| npm `primeng` official package | `table`, `iconfield`, `inputicon`, `inputtext`, `fluid`, `breadcrumb`, `button`, `tag`, `checkbox`, `confirmdialog`, `toast`, `skeleton`, `card` | approved official package and official `primeng.org` docs |
| Local custom components | split-page workspace composition and route-specific layout only | allowed only when PrimeNG has no suitable component or for thin composition around PrimeNG primitives |

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Registry Safety: PASS

**Approval:** approved 2026-03-25

---

*Phase: 08-user-management-delivery*
*UI-SPEC created: 2026-03-25*
*Sources: `08-CONTEXT.md`, `.planning/PROJECT.md`, Phase 7 UI-SPEC, `frontend/package.json`, `frontend/src/app.config.ts`, `frontend/src/assets/layout/_core.scss`, PrimeNG official docs reviewed on 2026-03-25: `https://primeng.org/table`, `https://primeng.org/iconfield`, `https://primeng.org/inputtext`, `https://primeng.org/fluid`, `https://primeng.org/breadcrumb`, `https://primeng.org/button`, `https://primeng.org/tag`, `https://primeng.org/checkbox`, `https://primeng.org/confirmdialog`, `https://primeng.org/toast`, `https://primeng.org/skeleton`, `https://primeng.org/card`*
