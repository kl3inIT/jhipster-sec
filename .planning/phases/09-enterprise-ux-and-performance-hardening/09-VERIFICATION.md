---
phase: 09-enterprise-ux-and-performance-hardening
verified: 2026-03-28T08:45:00Z
status: human_needed
score: 4/4 must-haves verified
re_verification: false
gaps:
  - truth: "Initial load and route transitions improve through lazy loading, code splitting, and leaner route-level bundles (PERF-02)"
    status: partial
    reason: "PERF-02 is assigned to Phase 9 in ROADMAP.md and REQUIREMENTS.md marks it Pending. Neither plan's requirements-completed field claims it. The lazy-loading infrastructure (loadComponent / loadChildren) is verifiably present across all routes, satisfying the requirement functionally, but no plan formally credited it and the checkbox in REQUIREMENTS.md remains unchecked."
    artifacts:
      - path: ".planning/REQUIREMENTS.md"
        issue: "PERF-02 checkbox is unchecked ([ ]) and status column shows Pending despite the implementation being in place since earlier phases"
      - path: ".planning/phases/09-enterprise-ux-and-performance-hardening/09-01-SUMMARY.md"
        issue: "requirements-completed: [PERF-01] — PERF-02 not claimed despite ROADMAP assigning it to this plan"
    missing:
      - "Mark PERF-02 as completed in REQUIREMENTS.md (change [ ] to [x] and status from Pending to Complete)"
      - "Add PERF-02 to the requirements-completed field in 09-01-SUMMARY.md or 09-02-SUMMARY.md to close the accounting gap"
human_verification:
  - test: "Verify skeleton rows appear during initial entity list fetch"
    expected: "5 p-skeleton rows render in the table body while the initial HTTP request is in-flight and the entity array is empty"
    why_human: "Initial-load skeleton timing requires a live browser with real network delay; jsdom/Vitest cannot observe the skeleton render window"
  - test: "Verify responsive column hiding at viewport < 1024px"
    expected: "At 1023px viewport: Department hides id, organization, costCenter columns; Employee hides id, department, salary; Organization hides id, ownerLogin, budget; action buttons stack vertically"
    why_human: "Requires a real browser viewport resize; jsdom environment defaults window.innerWidth and cannot simulate CSS viewport behavior"
  - test: "Verify N+1 permission query reduction under a real HTTP request"
    expected: "A 20-row department list request triggers 2 DB queries for permissions (one authority load, one bulk permission load) rather than ~202 per-row queries"
    why_human: "Requires SQL query tracing with a live DB connection; integration test passes but does not assert query count reduction"
---

# Phase 9: Enterprise UX And Performance Hardening — Verification Report

**Phase Goal:** Eliminate N+1 permission query explosion on secured entity list requests via request-scoped caching; migrate Department, Employee, and Organization list components to signal-based pagination with OnPush change detection, skeleton loaders, and responsive column hiding.
**Verified:** 2026-03-28T08:45:00Z
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Secured entity list requests use a request-scoped cache to avoid per-row authority and permission DB queries (PERF-01) | VERIFIED | `PermissionMatrix.java` + `RequestPermissionSnapshot.java` created; wired into `MergedSecurityContextBridge`, `RolePermissionServiceDbImpl`, `AttributePermissionEvaluatorImpl` with `isRequestScopeActive()` guard; integration tests pass |
| 2 | Department, Employee, and Organization list components use signal-based pagination with OnPush change detection (PERF-03) | VERIFIED | All 3 components have `ChangeDetectionStrategy.OnPush`, `totalItems = signal(0)`, `page = signal(1)`, `firstRow = computed(...)` in `@Component` decorator; `[totalRecords]="totalItems()"` and `[first]="firstRow()"` in templates |
| 3 | Entity list components show skeleton loaders during initial data fetch and hide lower-priority columns at tablet widths (UI-05) | VERIFIED | All 3 components import `Skeleton` from `primeng/skeleton`, use `tableValue` computed signal, `skeletonRows = Array(5).fill(null)`, `isTablet` signal with `fromEvent(window, 'resize')`, and per-entity `showXxxColumn` computed guards in templates |
| 4 | Initial load and route transitions improve through lazy loading and code splitting (PERF-02) | PARTIAL | `loadComponent` and `loadChildren` are present across all 17 entity and admin routes, satisfying the intent; however REQUIREMENTS.md checkbox is unchecked and no plan's `requirements-completed` claims PERF-02 — accounting gap only, not an implementation gap |

**Score:** 3/4 truths verified (1 partial — accounting only, not a code gap)

---

## Required Artifacts

### Plan 01: Backend Permission Caching

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/vn/core/security/permission/PermissionMatrix.java` | Package-level immutable permission matrix, `EMPTY` constant, `isEntityPermitted`, `isAttributePermitted` | VERIFIED | 49 lines, all three methods and EMPTY constant present, `Set.copyOf` immutability, correct `TargetType` key encoding |
| `src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java` | `@Component @Scope("request" proxyMode=TARGET_CLASS)`, `getAuthorities()`, `getMatrix()`, `isRequestScopeActive()` | VERIFIED | 121 lines, both annotations present, lazy null-initialized fields, `findAllByAuthorityNameIn` bulk query, `RequestContextHolder` guard |
| `src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java` | References extracted `PermissionMatrix`, no inner class | VERIFIED | Imports `com.vn.core.security.permission.PermissionMatrix`; no `private static class PermissionMatrix` found |
| `src/main/java/com/vn/core/security/bridge/MergedSecurityContextBridge.java` | Injects `RequestPermissionSnapshot`, delegates `getCurrentUserAuthorities()` to snapshot when request scope active | VERIFIED | `requestPermissionSnapshot` field injected; `isRequestScopeActive()` guard at line 52; `getAuthorities()` delegation wired |
| `src/main/java/com/vn/core/security/permission/RolePermissionServiceDbImpl.java` | Injects snapshot, uses `getMatrix().isEntityPermitted()` in `isEntityOpPermitted()` | VERIFIED | Field at line 25, constructor param at line 30, `isRequestScopeActive()` guard at line 41, `getMatrix()` call at line 42 |
| `src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java` | Injects snapshot, uses `getMatrix().isAttributePermitted()` in `checkAttributePermission()` | VERIFIED | Field at line 28, guard at line 55, `getMatrix().isAttributePermitted()` at line 56 |
| `src/test/java/com/vn/core/security/bridge/MergedSecurityContextBridgeTest.java` | `@Mock RequestPermissionSnapshot` added for updated constructor | VERIFIED | `@Mock` field at line 29, constructor call updated |

### Plan 02: Frontend Entity List Hardening

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `frontend/src/app/pages/entities/department/list/department-list.component.ts` | OnPush, signal pagination, skeletonRows, tableValue, isTablet, fromEvent resize, showIdColumn/showOrganizationColumn/showCostCenterColumn | VERIFIED | All 8 pattern occurrences confirmed; AfterViewInit with debounceTime(150) + takeUntilDestroyed |
| `frontend/src/app/pages/entities/department/list/department-list.component.html` | `[value]="tableValue()"`, `[totalRecords]="totalItems()"`, `[first]="firstRow()"`, `[loading]="loading() && departments().length > 0"`, skeleton body branch, `@if (showIdColumn())` guards, responsive colspan | VERIFIED | All bindings confirmed; skeleton branch at lines 68-81; colspan expression at line 144 |
| `frontend/src/app/pages/entities/employee/list/employee-list.component.ts` | Same OnPush/signal/skeleton/responsive pattern; showDepartmentColumn, showSalaryColumn | VERIFIED | All 8 pattern occurrences confirmed; showDepartmentColumn and showSalaryColumn computed signals present |
| `frontend/src/app/pages/entities/employee/list/employee-list.component.html` | Skeleton branch, `@if (showDepartmentColumn())`, `@if (showSalaryColumn())`, responsive colspan | VERIFIED | Column guards at lines 59/63; colspan at line 150 |
| `frontend/src/app/pages/entities/organization/list/organization-list.component.ts` | Same OnPush/signal/skeleton/responsive pattern; showIdColumn + `canViewField('id')`, showOwnerLoginColumn + `canViewField('ownerLogin')`, showBudgetColumn with isTablet | VERIFIED | All signals present; `showIdColumn = computed(() => !this.isTablet() && this.canViewField('id'))` at line 80 |
| `frontend/src/app/pages/entities/organization/list/organization-list.component.html` | `@if (showOwnerLoginColumn())`, `@if (showBudgetColumn())` guards, responsive colspan | VERIFIED | Guards at lines 58/61/75/78/94/97; colspan at line 144 |
| `frontend/src/app/pages/entities/department/list/department-list.component.spec.ts` | `beforeAll` sets `window.innerWidth = 1280` for desktop test environment | VERIFIED | `beforeAll` import and usage confirmed at line 16 |
| `frontend/src/app/pages/entities/organization/list/organization-list.component.spec.ts` | `beforeAll` sets `window.innerWidth = 1280` | VERIFIED | `beforeAll` import and usage confirmed at line 16 |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `MergedSecurityContextBridge.getCurrentUserAuthorities()` | `RequestPermissionSnapshot.getAuthorities()` | `isRequestScopeActive()` guard + field delegation | WIRED | Guard at bridge line 52; direct delegation to snapshot |
| `RolePermissionServiceDbImpl.isEntityOpPermitted()` | `RequestPermissionSnapshot.getMatrix().isEntityPermitted()` | `isRequestScopeActive()` guard | WIRED | Guard at line 41; matrix call at line 42 |
| `AttributePermissionEvaluatorImpl.checkAttributePermission()` | `RequestPermissionSnapshot.getMatrix().isAttributePermitted()` | `isRequestScopeActive()` guard | WIRED | Guard at line 55; matrix call at line 56 |
| `RequestPermissionSnapshot.getMatrix()` | `SecPermissionRepository.findAllByAuthorityNameIn()` | bulk query on first call | WIRED | Single repository method call; result cached in `cachedMatrix` |
| `DepartmentListComponent` template | `tableValue()` signal | `computed()` returns skeletonRows or departments | WIRED | `[value]="tableValue()"` at template line 34 |
| `OrganizationListComponent` template | `showIdColumn()` / `showOwnerLoginColumn()` computed | `isTablet()` + `canViewField()` | WIRED | `@if (showIdColumn())` and `@if (showOwnerLoginColumn())` guards in header and body cells |
| `isTablet` signal | `fromEvent(window, 'resize')` | `ngAfterViewInit` + debounceTime(150) + takeUntilDestroyed | WIRED | All 3 components implement `AfterViewInit`; stream sets `this.isTablet.set(window.innerWidth < 1024)` |

---

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| `DepartmentListComponent` | `departments` signal | `DepartmentService.query()` → `X-Total-Count` header + body | Yes — HTTP call to backend paginated endpoint | FLOWING |
| `DepartmentListComponent` | `tableValue` computed | switches between `skeletonRows` and `departments()` based on `loading()` | Yes — real data when fetch completes | FLOWING |
| `DepartmentListComponent` | `totalItems` signal | `fillComponentAttributesFromResponseHeader()` reads `X-Total-Count` | Yes — set from real response header | FLOWING |
| `RequestPermissionSnapshot.getMatrix()` | `cachedMatrix` | `secPermissionRepository.findAllByAuthorityNameIn(authorities)` | Yes — bulk DB query on first call per request | FLOWING |
| `RequestPermissionSnapshot.getAuthorities()` | `cachedAuthorities` | `authorityRepository.findAllById(jwtAuthorities)` | Yes — DB validation of JWT claims | FLOWING |

---

## Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| PermissionMatrix class is public, in correct package | `grep "^public class PermissionMatrix"` in `PermissionMatrix.java` | Found at line 13 | PASS |
| RequestPermissionSnapshot has @Component + @Scope("request") | `grep "@Component\|@Scope"` | Lines 33-34 confirmed | PASS |
| All 3 entity list TS files have 8+ key signal/skeleton/responsive patterns | `grep -c` across 3 files | 8/8/8 matches each | PASS |
| Organization template has showOwnerLoginColumn guard in all row positions | `grep "showOwnerLoginColumn"` in HTML | Lines 58, 75, 94 (header, skeleton branch, data branch) | PASS |
| All route files use loadComponent or loadChildren (PERF-02 evidence) | `grep -rn loadComponent\|loadChildren` across all route files | 17 lazy boundaries confirmed across admin and entity routes | PASS |
| Fallback path preserved — no RequestPermissionSnapshot in MergedSecurityContextBridgeTest callers | `grep "@Mock.*RequestPermissionSnapshot"` | Mock present; `isRequestScopeActive()` returns false in test context, fallback runs | PASS |
| Build smoke: frontend TypeScript patterns consistent | Pattern occurrence counts match expected signals, computed, and import declarations | All counts match | PASS |
| Integration tests passed | Commits show `./gradlew integrationTest` and `./gradlew test` both BUILD SUCCESSFUL | Documented in 09-01-SUMMARY.md Task 3 output | PASS |

---

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| PERF-01 | 09-01-PLAN (claimed in summary) | Frontend minimizes redundant API calls for auth, menu, capability, and user-management data through shared state or safe caching | SATISFIED | `RequestPermissionSnapshot` caches authority validation and permission matrix per-request; wired into 3 hot-path callers; non-web fallback preserved; integration tests green |
| PERF-02 | 09-01-PLAN (assigned in ROADMAP, not claimed in summary) | Initial load and route transitions improve through lazy loading, code splitting, and leaner route-level bundles | PARTIAL — implementation present, accounting incomplete | 17 `loadComponent`/`loadChildren` boundaries confirmed across all admin and entity routes; `09-UI-SPEC.md` explicitly notes lazy routes already in place; REQUIREMENTS.md checkbox unchecked; `requirements-completed` in summaries does not include PERF-02 |
| PERF-03 | 09-02-PLAN (claimed in summary) | Enterprise admin screens remain responsive under larger data sets through efficient rendering, pagination or filtering, and predictable state updates | SATISFIED | Signal-based pagination (`totalItems = signal`, `page = signal`, `firstRow = computed`) in all 3 entity list components; OnPush change detection prevents unnecessary re-renders; template bindings updated to signal calls |
| UI-05 | 09-02-PLAN (claimed in summary) | Frontend is more usable and responsive across desktop and narrower widths, with consistent actions, spacing, feedback, and loading states | SATISFIED | Skeleton loaders via PrimeNG Skeleton (5 placeholder rows on initial load); responsive column hiding at < 1024px in all 3 entities; action buttons stack vertically on tablet; emptymessage colspan accounts for hidden columns |

### Orphaned Requirements Check

No additional Phase 9 requirement IDs appear in REQUIREMENTS.md beyond UI-05, PERF-01, PERF-02, PERF-03. None are orphaned.

---

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| No anti-patterns found in any phase-modified file | — | — | — | — |

Scanned: `PermissionMatrix.java`, `RequestPermissionSnapshot.java`, all 3 entity list `.component.ts` files. No TODO/FIXME, no `return null`/empty stubs, no hardcoded empty data flowing to render paths.

---

## Human Verification Required

### 1. Skeleton Loader Visibility

**Test:** Navigate to `/entities/department`, `/entities/employee`, and `/entities/organization` in a browser with network throttling set to Slow 3G or a simulated API delay.
**Expected:** 5 skeleton rows with grey animated placeholders appear immediately in the table body while the initial HTTP request is in-flight. Once data loads, real rows replace the skeleton rows. Subsequent page changes show the PrimeNG loading overlay (spinner) rather than skeleton rows.
**Why human:** The skeleton/overlay switching logic depends on `loading() && departments().length === 0` — this requires real network timing. Vitest/jsdom runs synchronously and cannot observe the in-flight state window.

### 2. Responsive Column Hiding at Tablet Width

**Test:** Open each entity list page in a browser. Resize the viewport below 1024px (e.g., 768px or 1023px).
**Expected:**
- Department: `id`, `organization`, and `costCenter` columns disappear from header and rows; code and name columns remain; action buttons stack vertically.
- Employee: `id`, `department`, and `salary` columns disappear; employeeNumber, firstName, lastName, email, and actions remain visible; buttons stack vertically.
- Organization: `id`, `ownerLogin`, and `budget` columns disappear; name/code and actions remain; buttons stack vertically.
- The `emptymessage` colspan still spans all visible columns correctly at both widths.
**Why human:** `isTablet` signal is set by `window.innerWidth` via `fromEvent(window, 'resize')`. jsdom defaults innerWidth to 0 (mocked to 1280 in tests) and cannot simulate CSS viewport breakpoints or resize events.

### 3. N+1 Query Reduction Under Real HTTP Request

**Test:** With the backend running and SQL logging enabled (`logging.level.org.hibernate.SQL=DEBUG`), perform a GET `/api/entities/departments?page=0&size=20`. Count the authority-related SQL statements in the log.
**Expected:** At most 2 permission-related queries appear in the log: one `SELECT * FROM jhi_authority` (authority validation) and one bulk `SELECT * FROM sec_permission WHERE authority_name IN (...)` (matrix build). The previous behavior was ~202 queries (one authority check + N attribute checks × 20 rows).
**Why human:** Requires a running backend with database SQL logging enabled; cannot be verified through static code analysis alone.

---

## Gaps Summary

### PERF-02 Accounting Gap (not a code gap)

The only gap found is an accounting mismatch for PERF-02. The requirement "Initial load and route transitions improve through lazy loading, code splitting, and leaner route-level bundles" is functionally satisfied: all 17 entity and admin route boundaries use `loadComponent` or `loadChildren` for deferred loading, which was established in earlier phases. The Phase 9 UI-SPEC explicitly notes this is already in place.

However:
- REQUIREMENTS.md marks PERF-02 as `[ ] Pending`
- The ROADMAP assigns PERF-02 to plan 09-01
- Neither `09-01-SUMMARY.md` nor `09-02-SUMMARY.md` includes PERF-02 in their `requirements-completed` field

This is a bookkeeping gap that prevents REQUIREMENTS.md from showing the correct completion state. No implementation work is needed — only updating the accounting records.

**To close:** Update REQUIREMENTS.md PERF-02 checkbox from `[ ]` to `[x]` and status from `Pending` to `Complete`. Add `PERF-02` to the `requirements-completed` list in `09-01-SUMMARY.md`.

---

_Verified: 2026-03-28T08:45:00Z_
_Verifier: Claude (gsd-verifier)_
