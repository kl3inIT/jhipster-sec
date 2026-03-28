---
phase: 9
phase_name: enterprise-ux-and-performance-hardening
status: draft
created: 2026-03-28
requirements: [UI-05, PERF-01, PERF-02, PERF-03]
---

# UI-SPEC: Phase 9 — Enterprise UX And Performance Hardening

## 1. Design System

| Field | Value | Source |
|-------|-------|--------|
| Tool | PrimeNG 21.1.x + PrimeFlex + Aura/Sakai | CLAUDE.md (locked) |
| CSS utility layer | Tailwind CSS 3.4.x (minimal overrides only) | CLAUDE.md (locked) |
| Component source | Official PrimeNG components; custom markup only for layout composition | CLAUDE.md (locked) |
| Icon library | PrimeIcons (`pi pi-*`) | Detected in all existing list components |
| shadcn | Not applicable — PrimeNG-first project | Detected no `components.json` |
| Third-party registries | None | Agent discretion |

## 2. Spacing

Scale: 4-point multiples only via Tailwind utility classes.

| Token | px value | Tailwind class | Usage |
|-------|----------|----------------|-------|
| xs | 4px | `gap-1`, `p-1` | Icon-button inner gap in action columns |
| sm | 8px | `gap-2`, `p-2` | Action-button row gap, badge gap |
| md | 16px | `gap-4`, `p-4` | Section gap within `p-card`, search bar bottom margin |
| lg | 24px | `gap-6` | Vertical spacing between page header and table card |
| xl | 32px | `py-8` | Empty-state vertical padding |
| touch | 44px (min height) | Enforced via PrimeNG `p-button` defaults | All clickable row-action buttons |

Exception: skeleton rows use a fixed 12px height (`h-3` / `p-skeleton` default) to match PrimeNG Skeleton rendering.

## 3. Typography

Declared sizes: 4. Declared weights: 2.

| Role | Size | Weight | Line-height | Element |
|------|------|--------|-------------|---------|
| Page title | 24px (text-2xl) | 600 (semibold) | 1.2 | `<h1>` in list page header |
| Section heading | 20px (text-xl) | 600 (semibold) | 1.2 | Denied-state `<h2>`, dialog headers |
| Body / table cell | 14px (text-sm) | 400 (regular) | 1.5 | `p-datatable-sm` cell content, form labels |
| Secondary / hint | 12px (text-xs) | 400 (regular) | 1.5 | `text-color-secondary` spans, pagination report template |

Font family: inherited from PrimeNG Aura theme (system-ui stack) — do not override.

## 4. Color Contract

Split: 60% dominant / 30% secondary / 10% accent. All values deferred to PrimeNG Aura design tokens; do not hardcode hex values.

| Role | PrimeNG token | Coverage | Reserved for |
|------|--------------|----------|-------------|
| Dominant surface | `--p-surface-0` | 60% | Page background, main content area |
| Secondary surface | `--p-surface-100` | 30% | `p-card` backgrounds, sidebar, topbar |
| Accent (primary) | `--p-primary-color` | 10% | Primary CTA buttons (`pButton` without severity), active nav item, sort icon hover |
| Destructive | `p-button-danger` severity class | Reserved | Delete buttons only — never used for warnings or information |
| Warning | `p-button-warning` severity class | Reserved | Deactivate/suspend action only |
| Success | `p-button-success` severity class | Reserved | Activate action, success toasts |

Dark mode: toggled via `layoutService.layoutConfig.darkTheme` (topbar toggle already implemented). Skeleton loaders must use `p-skeleton` which respects theme tokens automatically.

## 5. Component Inventory

Components used in this phase. All are official PrimeNG components.

| Component | PrimeNG import | Usage in this phase |
|-----------|---------------|---------------------|
| `p-table` | `TableModule` | All entity list screens (department, employee, organization) — already present; gains skeleton body template |
| `p-skeleton` | `SkeletonModule` | Skeleton rows replacing spinner-per-row during initial entity list load |
| `p-paginator` (via `p-table [paginator]`) | `TableModule` | Server-side pagination already wired via `[lazy]="true"` + `onLazyLoad`; no change to component, only backend endpoint |
| `p-card` | `CardModule` | List page wrapper — already present |
| `p-button` | `ButtonModule` | CTA and row actions — already present |
| `p-toast` | `ToastModule` | Feedback toasts — already present |
| `p-confirmDialog` | `ConfirmDialogModule` | Delete confirmation — already present |
| `p-tag` | `TagModule` | User status badge (user management) — already present |
| `p-iconfield` / `p-inputicon` | `IconFieldModule`, `InputIconModule` | Search bar (user management) — already present |

No new third-party components. No registry vetting required.

## 6. Skeleton Loader Contract

Applies to: Department list, Employee list, Organization list.

Trigger: render skeleton rows when `loading()` is `true` AND `items().length === 0` (initial fetch only, not on pagination navigation where stale rows stay visible during reload).

### Skeleton Row Specification

```
<ng-template pTemplate="body" let-row>
  @if (loading() && firstLoad()) {
    <!-- skeleton row: same column count as the visible column set -->
    <tr>
      @for (col of skeletonColumns(); track $index) {
        <td><p-skeleton height="1.25rem" /></td>
      }
    </tr>
  } @else {
    <!-- normal body row -->
  }
</ng-template>
```

- Skeleton row count: 5 rows (fixed, regardless of page size).
- Column count: mirrors `skeletonColumns()` signal — computed from the same visibility signals used for real columns (e.g., `showOrganizationColumn`, `showSalaryColumn`).
- `p-skeleton` height: `1.25rem` (20px), matching `p-datatable-sm` row density.
- Animation: PrimeNG default `wave` animation via `animation="wave"` prop.
- Remove spinner: the existing `<i class="pi pi-spin pi-spinner">` fallback in the action column is replaced by the skeleton row approach; do not render action-column spinners inside skeleton rows.

## 7. Responsive Column Hiding Contract

Breakpoint signal name: `isTablet` — `true` when viewport width is 768px–1024px, `false` otherwise.
Detection: computed from `window.innerWidth` with a `resize` listener or Angular CDK `BreakpointObserver` (`'(max-width: 1024px)'`).

### Column Visibility by Entity

**Department list** (tablet = 768–1024px):

| Column | Desktop | Tablet |
|--------|---------|--------|
| ID | visible | hidden |
| Code | visible | visible |
| Name | visible | visible |
| Organization | conditional (attribute permission) | hidden |
| Cost Center | conditional (attribute permission + data) | hidden |
| Actions | visible | visible (icon-only buttons, no label) |

**Employee list** (tablet = 768–1024px):

| Column | Desktop | Tablet |
|--------|---------|--------|
| ID | visible | hidden |
| Employee Number | visible | visible |
| First Name | visible | visible |
| Last Name | visible | visible |
| Department | visible | visible |
| Email | visible | hidden |
| Salary | conditional (attribute permission) | hidden |
| Actions | visible | visible (icon-only buttons) |

**Organization list** (tablet = 768–1024px):

| Column | Desktop | Tablet |
|--------|---------|--------|
| ID | visible | hidden |
| Code | visible | visible |
| Name | visible | visible |
| Owner Login | visible | hidden |
| Budget | conditional (attribute permission + data) | hidden |
| Actions | visible | visible (icon-only buttons) |

Implementation: extend existing computed signals. Add `isTablet = computed(() => this.breakpointService.isTablet())` injected from a shared breakpoint service. Combine with existing permission-based visibility: `showIdColumn = computed(() => !this.isTablet())`.

## 8. Server-Side Pagination Contract

Already implemented in all three entity list components (department, employee, organization) with `ITEMS_PER_PAGE = 20`, `[lazy]="true"`, `onLazyLoad`, and `X-Total-Count` header reading. The backend endpoint must support `page`, `size`, and `sort` query params and return `X-Total-Count`.

Backend changes for Spring Data `Pageable` are outside the UI contract; the frontend side is already correct. No UI changes needed beyond confirming the backend returns the header.

Default page size: 20 (matches user-management reference, matches existing `ITEMS_PER_PAGE` constant).
Rows-per-page options: `[10, 20, 30]` (matches existing list components).

## 9. Copywriting Contract

All copy uses i18n keys via `@ngx-translate`. Do not hardcode visible strings.

### Primary CTAs

| Screen | CTA label key | Action |
|--------|--------------|--------|
| Department list | `angappApp.department.home.createLabel` | Navigate to create form |
| Employee list | `angappApp.employee.home.createLabel` | Navigate to create form |
| Organization list | `angappApp.organization.home.createLabel` | Navigate to create form |

### Empty States

| Screen | Title key | Body key | Icon |
|--------|-----------|----------|------|
| Department list | `angappApp.department.home.notFound` | (inline, single row) | none (table `emptymessage` row) |
| Employee list | `angappApp.employee.home.notFound` | (inline, single row) | none |
| Organization list | `angappApp.organization.home.notFound` | (inline, single row) | none |

No change to empty-state pattern for entity lists. User management list uses richer empty state (`pi-users` icon + title + body) — preserve as-is.

### Access Denied State

| Screen | Title key | Body key |
|--------|-----------|----------|
| Department list | `angappApp.department.home.denied.title` | `angappApp.department.home.denied.message` |
| Employee list | `angappApp.employee.home.denied.title` | `angappApp.employee.home.denied.message` |
| Organization list | `angappApp.organization.home.denied.title` | `angappApp.organization.home.denied.message` |

### Error States

| Screen | Pattern | Key |
|--------|---------|-----|
| User management load error | Retry button + message | `userManagement.home.loadError` + `userManagement.home.refreshListLabel` |
| Entity list load error | `handleHttpError(...)` toast via `MessageService` | Generic HTTP error key from `http-error.utils.ts` |

### Destructive Actions

| Action | Confirmation approach | Copy keys |
|--------|-----------------------|-----------|
| Delete department | `p-confirmDialog` (already implemented) | `angappApp.department.delete.question`, `angappApp.department.delete.title`, `entity.action.delete`, `entity.action.cancel` |
| Delete employee | `p-confirmDialog` (already implemented) | `angappApp.employee.delete.question`, `angappApp.employee.delete.title` |
| Delete organization | `p-confirmDialog` (already implemented) | `angappApp.organization.delete.question`, `angappApp.organization.delete.title` |
| Delete user | `p-confirmDialog` (already implemented) | Existing user management keys |
| Deactivate user | Inline toggle — no confirmation dialog | `userManagement.actions.deactivate` |

No new destructive actions introduced in this phase.

## 10. Loading State Transitions

| State | What the user sees |
|-------|-------------------|
| Initial page load (no cached data) | 5 skeleton rows in the table body; paginator hidden until `totalItems > 0` |
| Pagination / sort navigation | Existing rows stay visible; `[loading]="true"` on `p-table` shows the built-in PrimeNG loading overlay |
| Capability check pending | Existing: action-column spinner (`pi pi-spin pi-spinner`) — keep this pattern for the capability-loading state only |
| Capability loaded, access denied | Denied state card replaces table |
| Data loaded successfully | Real rows render, paginator becomes active |
| Delete in progress | `ConfirmDialog` accept triggers delete; success toast via `addTranslatedMessage` |

## 11. Interaction Contracts

### Row Actions

- View: `pi pi-eye`, `p-button-text p-button-sm`, navigates to `/entities/{entity}/{id}/view`.
- Edit: `pi pi-pencil`, `p-button-text p-button-sm`, navigates to `/entities/{entity}/{id}/edit`.
- Delete: `pi pi-trash`, `p-button-text p-button-danger p-button-sm`, opens `p-confirmDialog`.
- All row-action buttons carry `[attr.aria-label]` bound to a translation key — do not remove.
- At tablet widths: buttons remain icon-only (no label), `p-button-text p-button-sm` styling unchanged.

### Sort

- `pSortableColumn` + `p-sortIcon` already on all sortable columns — no change.
- Sort state persists in URL query params via `navigateToWithComponentValues`.

### Pagination

- `onLazyLoad` handles both page changes and sort changes from the PrimeNG table event.
- Page state persists in URL query params.

### Focus Management

- After delete confirmation and reload, focus returns to the table (PrimeNG `p-table` manages this via its own focus model).
- After create/edit navigation, returning to the list restores previous scroll position via `WorkspaceContextService` query-param storage.

## 12. Accessibility Checklist

- All action buttons: `[attr.aria-label]` bound to translated string — mandatory, already present.
- `p-confirmDialog`: PrimeNG handles ARIA dialog role and focus trap.
- `p-skeleton`: PrimeNG renders `aria-busy="true"` on the container — verify after integration.
- Color contrast: PrimeNG Aura tokens meet WCAG AA for text on surface at both light and dark modes — do not override colors with lower-contrast values.
- Paginator: PrimeNG `p-paginator` provides ARIA navigation — do not replace with custom paginator.
- Keyboard navigation: PrimeNG table column sort headers are keyboard-activatable by default.

## 13. Backend-Only Changes (no UI contract impact)

The following Phase 9 items are purely backend and do not change the visual or interaction contract:

- `PERF-01`: Request-local permission snapshot in `MergedSecurityContextBridge`, `RolePermissionServiceDbImpl`, `AttributePermissionEvaluatorImpl`, `SecureEntitySerializerImpl`, `SecuredEntityCapabilityService` — no frontend change.
- `D-04`: `SecPermissionMapper` bean fix for `SecuredEntityEnforcementIT` — test-infrastructure only.
- `PERF-02` (code splitting): Lazy route boundaries already in place (`loadComponent`); any further splitting is a build-config change with no visual effect.

## 14. Registry Safety Gate

No third-party registries declared. Gate not applicable.

---

## Pre-Population Source Log

| Decision | Source |
|----------|--------|
| PrimeNG-first, Tailwind utility layer | CLAUDE.md |
| `p-table` + `[lazy]` + `onLazyLoad` pattern | Detected in department, employee, organization list components |
| `ITEMS_PER_PAGE = 20`, `X-Total-Count` header pattern | Detected in all three entity list `.component.ts` files |
| Existing computed-signal column visibility pattern | Detected `showOrganizationColumn`, `showSalaryColumn`, `showBudgetColumn` |
| Skeleton replaces spinner (D-06) | 09-CONTEXT.md locked decision |
| Tablet breakpoint 768–1024px (D-06) | 09-CONTEXT.md locked decision |
| Request-local caching only, no cross-request (D-01, D-02) | 09-CONTEXT.md locked decisions |
| Page size 20 default | 09-CONTEXT.md agent discretion, matching user management |
| Column hiding choices per entity | Agent discretion — ID/secondary columns hidden at tablet, primary identity columns preserved |
| `p-confirmDialog` for destructive actions | Detected in all existing list components |
| `addTranslatedMessage` / `handleHttpError` error pattern | Detected in component source, CLAUDE.md |
| Dark mode via `layoutService.layoutConfig.darkTheme` | Detected in `app.topbar.ts` |
