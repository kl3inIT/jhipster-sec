---
phase: 08-user-management-delivery
plan: 02
subsystem: frontend/user-management
tags: [angular, primeng, user-management, browse, detail, search]
dependency_graph:
  requires: [08-01]
  provides: [user-management-list, user-management-detail, authority-label-util]
  affects: [user-management.routes, user-management.service, user-management.model, i18n]
tech_stack:
  added: [p-iconfield, p-inputicon, p-tag, p-checkbox]
  patterns: [debounced-search, workspace-context-preservation, split-page-detail, inline-actions]
key_files:
  created:
    - frontend/src/app/pages/admin/user-management/list/user-management-list.component.ts
    - frontend/src/app/pages/admin/user-management/list/user-management-list.component.html
    - frontend/src/app/pages/admin/user-management/list/user-management-list.component.spec.ts
    - frontend/src/app/pages/admin/user-management/detail/user-management-detail.component.ts
    - frontend/src/app/pages/admin/user-management/detail/user-management-detail.component.html
    - frontend/src/app/pages/admin/user-management/detail/user-management-detail.component.spec.ts
    - frontend/src/app/pages/admin/user-management/shared/authority-label.util.ts
  modified:
    - frontend/src/app/pages/admin/user-management/user-management.routes.ts
    - frontend/src/app/pages/admin/user-management/user-management.routes.spec.ts
    - frontend/src/app/pages/admin/user-management/service/user-management.service.ts
    - frontend/src/app/pages/admin/user-management/service/user-management.service.spec.ts
    - frontend/src/app/pages/admin/user-management/user-management.model.ts
    - frontend/src/i18n/en/user-management.json
    - frontend/src/i18n/vi/user-management.json
decisions:
  - "UserManagementService.query accepts SearchWithPagination for combined search and pagination"
  - "List component uses component-level ConfirmationService and MessageService providers for PrimeNG integration"
  - "Detail component uses FormsModule for ngModel binding on disabled p-checkbox rows"
  - "Authority labels resolve from i18n keys with raw authority code as fallback"
metrics:
  duration_seconds: 624
  completed: 2026-03-25
---

# Phase 8 Plan 2: User Management Browse Workspace Summary

Dense admin list with debounced search, inline activation safety, delete confirmation, workspace context preservation, and read-only split-page detail with disabled role checkbox table.

## Tasks Completed

| # | Task | Commit | Key Files |
|---|------|--------|-----------|
| 1 | Update route and service seam for query-aware list and detail | 27869a0 | user-management.service.ts, user-management.routes.ts, authority-label.util.ts, i18n/*.json |
| 2 | Build dense list workspace and read-only split detail view | a835631 | list/*.ts, list/*.html, detail/*.ts, detail/*.html, specs |

## What Was Built

### List Workspace (/admin/users)
- PrimeNG p-table with backend pagination and sorting via query params
- Debounced search input (300ms) writing `query` param, staying paginated
- Columns: login, full name, email, activation status (p-tag), roles (first 2 + overflow), last modified, actions
- Inline row actions: View, Edit, Activate/Deactivate, Delete
- Activation/deactivation fires immediately with toast feedback (no confirmation dialog)
- Delete uses ConfirmationService with translated confirmation dialog
- Self-deactivate and self-delete disabled for current admin user
- WorkspaceContextService stores list state before detail/edit navigation
- Empty state with icon, heading, and guidance copy
- Error state with retry affordance

### Detail View (/admin/users/:login/view)
- Split-page layout: 65/35 grid with user summary card (left) and role card (right)
- Resolves user from route data via existing resolver
- Left card shows login, name, email, language, activation status, audit info
- Right card shows all authorities as disabled p-checkbox rows in a p-table
- Authority labels resolved via i18n with raw code fallback
- Edit User and Back to List actions in header
- Back to List restores stored workspace context query params

### Service and Model Updates
- UserManagementService.query now accepts SearchWithPagination
- IUserRoleRow interface added to model
- resolveAuthorityLabel and buildAuthorityRows utility functions
- Route lazy imports updated to point to real list and detail components

### Translation Coverage
- en/vi translations for: search placeholder, empty states, error states, action labels, column headers, authority labels, status labels, delete confirmation copy

## Test Coverage

- 23 tests across 4 spec files, all passing
- Route spec: path assertions, lazy-load verification, resolver behavior
- Service spec: SearchWithPagination query params, authority mapping, error propagation
- List spec: load, debounced search, page/sort mapping, direct activation, delete confirmation, self-action disablement, workspace context storage, authority overflow
- Detail spec: resolved user rendering, disabled role rows, label fallback, back navigation, edit navigation

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None - all data paths are wired to real backend endpoints.

## Self-Check: PASSED
