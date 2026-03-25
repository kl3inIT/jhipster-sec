# Phase 8: User Management Delivery - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md - this log preserves the alternatives considered.

**Date:** 2026-03-25
**Phase:** 08-user-management-delivery
**Areas discussed:** User List Workspace, Search And Browse Controls, Role Assignment Flow, Action Safety

---

## User List Workspace

### Q1. What should the main `/admin/users` screen feel like?

| Option | Description | Selected |
|--------|-------------|----------|
| Dense admin table | Best fit with donor JHipster behavior and fastest scanning for many users | yes |
| Balanced enterprise table | Still tabular, but with more whitespace and slightly fewer always-visible fields | |
| Master-detail split view | Users list on one side, selected user summary on the other | |

**User's choice:** Dense admin table.

### Q2. Which columns should be visible in that default table?

| Option | Description | Selected |
|--------|-------------|----------|
| Operational default | `login`, full name, `email`, activation status, roles, last modified, actions | yes |
| Full audit table | Add language, created by/date, and modified by/date directly in the main grid | |
| Minimal table | Show only `login`, `email`, status, and actions | |

**User's choice:** Operational default columns.

### Q3. When an admin opens a user from the table, what should the primary drill-in be?

| Option | Description | Selected |
|--------|-------------|----------|
| Detail page first | Fits the list-first shell pattern; edit stays explicit | yes |
| Edit form directly | Faster for heavy admins, but removes the read-only step | |
| No dedicated open page | Keep the list as the only browse surface | |

**User's choice:** Detail page first.

### Q4. How should row actions appear in that dense table?

| Option | Description | Selected |
|--------|-------------|----------|
| Inline action buttons | Show `View`, `Edit`, and status/delete actions directly in the row | yes |
| Compact overflow menu | Cleaner table, extra click for common actions | |
| Mixed | Keep some actions inline and move destructive actions into overflow | |

**User's choice:** Inline action buttons.

### Clarification. How should the screenshot-inspired split layout be applied?

| Option | Description | Selected |
|--------|-------------|----------|
| Edit/Create only | Keep detail read-only in a different layout; use split layout only when editing | |
| Editable detail page | Make the split layout the primary page and editable inline | |
| Split detail, read-only until Edit | Use the split layout for detail too, but keep fields read-only until explicit edit | yes |

**User's choice:** Use the screenshot-style split layout for the detail page too, but keep it read-only until Edit.
**Notes:** User supplied a reference image showing user fields on the left and the role list on the right and asked for the detail view to look like that.

---

## Search And Browse Controls

### Q1. What should be the default way to find a user from the list page?

| Option | Description | Selected |
|--------|-------------|----------|
| One prominent search box | Fast lookup without a heavy filter workspace | yes |
| Search box plus quick filters | Add visible filters like activation state and role | |
| Full filter toolbar | Multiple visible fields at once | |

**User's choice:** One prominent search box.

### Q2. What should that search box match against?

| Option | Description | Selected |
|--------|-------------|----------|
| Login and email only | Predictable and tight | |
| Login, email, and full name | Broader and friendlier when admins know the person instead of the account id | yes |
| Everything visible in the row | Most flexible, least predictable | |

**User's choice:** Login, email, and full name.

### Q3. What should happen with sorting and paging while searching?

| Option | Description | Selected |
|--------|-------------|----------|
| Keep normal paging and sortable columns | Search narrows the dataset, but the workspace still behaves normally | yes |
| Search-first mode | Disable most sorting and simplify to quick results | |
| Auto-jump behavior | Return only the first page of best matches | |

**User's choice:** Keep normal paging and sorting while searching.

### Q4. What should the search interaction feel like?

| Option | Description | Selected |
|--------|-------------|----------|
| Instant as you type, with a short debounce | Fastest admin flow | yes |
| Type then press Enter or click Search | More explicit and conservative | |
| Hybrid | Instant refinement plus explicit Search button | |

**User's choice:** Instant search with a short debounce.

---

## Role Assignment Flow

### Q1. Where should role assignment live for Phase 8?

| Option | Description | Selected |
|--------|-------------|----------|
| In the user create/edit page, on the right-side panel | Keeps identity fields and roles in one workflow | yes |
| Separate access subpage/tab | Cleaner separation, but more navigation | |
| Both | Summary in the user page plus dedicated access screen | |

**User's choice:** Role assignment belongs in the user page on the right-side panel.

### Q2. How should admins select roles in that panel?

| Option | Description | Selected |
|--------|-------------|----------|
| Checkbox table | Matches the enterprise reference and scales better than a dropdown | yes |
| Dual-list picklist | Explicit, but heavier UI | |
| Simple multiselect dropdown | Compact, but less enterprise/admin-oriented | |

**User's choice:** Checkbox table.

### Q3. What information should each role row show?

| Option | Description | Selected |
|--------|-------------|----------|
| Role name only | Smallest footprint | |
| Name plus display label/description | Best balance of context and density | yes |
| Name, description, and role type/app scope | Richest context, widest table | |

**User's choice:** Role code plus a human-friendly label or description.

### Q4. On the read-only detail page, how should the right-side roles panel behave before Edit is clicked?

| Option | Description | Selected |
|--------|-------------|----------|
| Show assigned roles only | Summary until edit mode | |
| Show the full role table but disabled | Keep access context visible and make edit transition obvious | yes |
| Collapse the panel until Edit | Hide access context until editing | |

**User's choice:** Show the full role table in a disabled state until Edit is clicked.

---

## Action Safety

### Q1. How should activate/deactivate work from the list page?

| Option | Description | Selected |
|--------|-------------|----------|
| Inline status toggle in the row | Fastest admin flow and donor-aligned | yes |
| Status change only from detail/edit | Safer, but slower | |
| Inline activate only, deactivate elsewhere | Extra caution only on the disruptive direction | |

**User's choice:** Inline status toggle in the row.

### Q2. Should activation changes require confirmation?

| Option | Description | Selected |
|--------|-------------|----------|
| No extra confirmation | Rely on visible state change and feedback | yes |
| Confirm deactivation only | Add caution to the disruptive action | |
| Confirm both activation and deactivation | Highest safety, slowest flow | |

**User's choice:** No extra confirmation for activation changes.

### Q3. How should delete behave?

| Option | Description | Selected |
|--------|-------------|----------|
| Delete from the list row with a confirmation dialog | Standard admin flow, still guarded | yes |
| Delete only from the detail page | More deliberate, but slower | |
| Delete from both list and detail | Flexible, but duplicates the destructive affordance | |

**User's choice:** Delete from the list row with a confirmation dialog.

### Q4. What should happen for self-protection cases, like the current admin user row?

| Option | Description | Selected |
|--------|-------------|----------|
| Disable self-delete and self-deactivate inline | Clear and safe | yes |
| Allow it, but warn heavily | More freedom, higher risk | |
| Hide those actions entirely | Cleaner, but less explicit | |

**User's choice:** Disable self-delete and self-deactivate inline.

---

## the agent's Discretion

- Exact PrimeNG presentation details for the dense admin table and split-page detail view
- Exact search debounce timing
- Exact iconography, badge styling, and translated success/error copy

## Deferred Ideas

None.
