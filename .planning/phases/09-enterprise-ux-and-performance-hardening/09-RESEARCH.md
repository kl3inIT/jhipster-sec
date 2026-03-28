# Phase 9: Enterprise UX And Performance Hardening - Research

**Researched:** 2026-03-28
**Domain:** Spring Boot permission caching, Angular entity list pagination, PrimeNG skeleton loaders, MapStruct test wiring
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01 (inherited):** Phase 9 may optimize backend secured-read permission checks as part of PERF-01, but it must preserve the request-time authority refresh semantics introduced in Phase 08.3. The permission snapshot is per-request only.
- **D-02 (inherited):** For current-user permission caching, the first implementation step is request-local reuse only. Cross-request or session-level caching is out of scope unless a later plan proves explicit invalidation for authority, permission, and menu changes.
- **D-03 (inherited):** Reuse the existing bulk-permission-matrix pattern already present in `SecuredEntityCapabilityService` (`findAllByAuthorityNameIn(...)` once, serve checks from that result) rather than adding another parallel permission-aggregation model.
- **D-04:** Fix the `SecPermissionMapper` bean-not-found failure at `SecuredEntityEnforcementIT` context startup in Phase 9.
- **D-05:** Add server-side pagination to entity list endpoints for Department, Employee, and Organization using Spring Data `Pageable`. Follow the existing user-management list pattern (`page`, `size`, `sort` query params + `X-Total-Count` header). Wire the PrimeNG paginator in each entity list component.
- **D-06:** UI-05 covers both (a) skeleton loaders replacing spinners during initial entity list data fetch, and (b) responsive column/action behavior at tablet widths (768px–1024px) — column hiding for lower-priority columns, stacked or collapsed action buttons on narrower viewports.

### Claude's Discretion
- Exact request-local storage mechanism (request attributes vs. request-scoped Spring bean) as long as non-web and test contexts degrade gracefully without a servlet container.
- Whether to extract a shared `PermissionSnapshot` or `PermissionMatrix` type from existing capability code or reuse the existing shape.
- Whether the backend permission caching + SecPermissionMapper fix lands in one plan or two.
- Default page size for entity list pagination (suggest 20, matching user-management).
- Specific columns to hide per entity at tablet widths — researcher should read each list component's column definitions.
- Whether skeleton rows use a fixed column count or mirror the visible column count per entity.

### Deferred Ideas (OUT OF SCOPE)
- Cross-request or session-level permission caching.
- Row-policy code (retired in Phase 08.3).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| UI-05 | Skeleton loaders + responsive columns/actions at 768–1024px for entity list screens | Column inventory per entity documented; PrimeNG Skeleton confirmed available; responsive signal pattern identified |
| PERF-01 | Request-local permission caching to eliminate per-call `jhi_authority` and `sec_permission` N+1 queries | Full call chain mapped; PermissionMatrix reuse pattern from `SecuredEntityCapabilityService` documented |
| PERF-02 | Verified by integration tests passing after permission caching is applied | `SecuredEntityEnforcementIT` confirmed green; test runner task documented |
| PERF-03 | Server-side pagination for Department, Employee, Organization lists | Backend already has `Pageable` support; frontend pagination code already sends `page`/`size`/`sort`; `X-Total-Count` already present; root cause of stale `totalItems` identified |
</phase_requirements>

---

## Summary

Phase 9 has four independent workstreams: backend permission caching (PERF-01), unblocking integration tests (PERF-02/D-04), entity list pagination correctness (PERF-03), and frontend skeleton loaders + responsive columns (UI-05).

**Critical finding on D-04:** `SecuredEntityEnforcementIT` passes today (`./gradlew integrationTest` exits green). The reported "bean-not-found" blocker is not reproducible in the current codebase. The generated `SecPermissionMapperImpl` exists in `build/generated/sources/annotationProcessor/java/main/` as a `@Component` and is picked up by the full `@SpringBootApplication` context that `@IntegrationTest` loads. D-04 is either already resolved by recent commits or was a transient IDE-classpath issue. The planner should treat D-04 as a **green baseline verification step** rather than a fix task.

**Backend pagination is already implemented.** `DepartmentResource`, `EmployeeResource`, and `OrganizationResource` all accept a `Pageable` parameter and return `X-Total-Count` headers via `PaginationUtil`. The frontend entity list components already send `page`, `size`, and `sort` query parameters and read `X-Total-Count`. The only gap is that `totalItems` and `firstRow` are plain `number`/`get` properties rather than signals, so reactive updates to the paginator `[totalRecords]` and `[first]` bindings may lag. This is a minor signal/state alignment fix, not a pagination implementation.

**Frontend pagination summary:** Department, Employee, and Organization list components already have `p-table [lazy]="true" [paginator]="true"` wired and `queryBackend()` sending paginated params. The structural pagination is complete. The remaining gap is `totalItems` being a plain `number` field that doesn't trigger signal-based change detection, and `firstRow` being a getter rather than `computed()`. These need to become signals to match user-management's pattern.

**Primary recommendation:** Implement PERF-01 as a request-scoped Spring bean (`PermissionSnapshotContext`) that loads authorities + permission matrix once per request, injected into `MergedSecurityContextBridge`, `RolePermissionServiceDbImpl`, and `AttributePermissionEvaluatorImpl`. Add UI-05 skeleton loaders and responsive breakpoint signal in parallel. Verify with `integrationTest` as the gate.

---

## 1. Backend Permission Caching (PERF-01)

### Current Call Chain

A single GET /api/departments (list) request triggers:

```
DepartmentResource.getAllDepartments(Pageable)
  └─ DepartmentService.list(pageable)
       └─ SecureDataManagerImpl.loadList(Department.class, pageable)
            └─ DataManagerImpl.loadPage(Department.class, null, pageable, READ)
                 └─ DataManagerImpl.checkCrud(Department.class, READ)         [CALL 1]
                      └─ CrudEntityConstraint.applyTo(CrudEntityContext)
                           └─ RolePermissionServiceDbImpl.isEntityOpPermitted(Department.class, READ)
                                ├─ MergedSecurityService.getCurrentUserAuthorityNames()
                                │    └─ MergedSecurityContextBridge.getCurrentUserAuthorities()
                                │         └─ authorityRepository.findAllById(jwtAuthorities)  [DB QUERY A]
                                └─ SecPermissionRepository.findByRolesAndTarget(authorities, ENTITY, DEPARTMENT, READ)  [DB QUERY B]
  └─ securedEntityJsonAdapter.toJsonArrayString(page.getContent(), "department-list")
       └─ SecureEntitySerializerImpl.serialize(entity, fetchPlan)  [once per row]
            └─ for each FetchPlanProperty:
                 └─ AttributePermissionEvaluatorImpl.canView(Department.class, attr)  [CALL N per attribute per row]
                      ├─ MergedSecurityService.getCurrentUserAuthorityNames()
                      │    └─ MergedSecurityContextBridge.getCurrentUserAuthorities()
                      │         └─ authorityRepository.findAllById(jwtAuthorities)  [DB QUERY A repeated]
                      └─ SecPermissionRepository.findByRolesAndTargets(authorities, ATTRIBUTE, [specific+wildcard], VIEW)  [DB QUERY B repeated]
```

### N+1 Query Locations

For a list response with **R rows** and **F fetch-plan attributes per row**:

| Location | Occurrences | Query |
|----------|-------------|-------|
| `MergedSecurityContextBridge.getCurrentUserAuthorities()` | 1 (CRUD check) + R×F (attribute checks) | `SELECT name FROM jhi_authority WHERE name IN (...)` |
| `RolePermissionServiceDbImpl.isEntityOpPermitted()` | 1 per CRUD check | `SELECT * FROM sec_permission WHERE authority_name IN ... AND target=ENTITY AND target_name=X AND action=Y` |
| `AttributePermissionEvaluatorImpl.checkAttributePermission()` | R×F | `SELECT * FROM sec_permission WHERE authority_name IN ... AND target=ATTRIBUTE AND target IN (specific, wildcard) AND action=VIEW` |

For a department list with 20 rows and 5 visible attributes: **1 CRUD authority query + 1 CRUD permission query + 100 attribute authority queries + 100 attribute permission queries = 202 DB queries per request**.

### Request-Local Storage Mechanism Recommendation

**Use a request-scoped Spring bean** (`@Scope("request")`), not `HttpServletRequest` attributes. Rationale:
- Spring's `@Scope("request")` is already used in JHipster projects for request-bound state.
- Degrades cleanly in non-web contexts (test, batch): when no request scope is active, the `SimpleThreadScope` fallback or a `@Scope(value="request", proxyMode=ScopedProxyMode.TARGET_CLASS)` with a null-safe accessor handles it.
- Avoids raw `HttpServletRequest` attribute strings (no type safety).
- Unlike `ThreadLocal`, the Spring scoped proxy approach is automatically garbage-collected with the request.

**Graceful degradation pattern for non-web callers:**

```java
// In the snapshot bean or its consumers:
try {
    snapshot = permissionSnapshotContext.get(); // request-scoped proxy call
} catch (IllegalStateException e) {
    // No active request scope (test, batch) - load fresh
    snapshot = loadFresh();
}
```

Alternatively, use a `RequestContextHolder.getRequestAttributes()` null-check before accessing the scoped bean, and fall back to a direct load.

### Implementation Pattern

The `PermissionMatrix` inner class in `SecuredEntityCapabilityService` is the ideal reuse target. It should be extracted as a package-accessible type and shared with the snapshot bean.

**New types:**

```java
// src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestPermissionSnapshot {
    private Collection<String> authorities;          // loaded once
    private PermissionMatrix matrix;                 // loaded once

    // Returns cached authorities, loading from MergedSecurityContextBridge on first call
    public Collection<String> getAuthorities() { ... }

    // Returns cached matrix, loading from SecPermissionRepository on first call
    public PermissionMatrix getMatrix() { ... }

    // For non-web contexts: static factory that loads fresh
    public static RequestPermissionSnapshot loadFresh(
        SecurityContextBridge bridge,
        SecPermissionRepository repo
    ) { ... }
}
```

**Callers to update (blast radius — 3 classes + 1 service):**

| Class | Change |
|-------|--------|
| `MergedSecurityContextBridge` | Replace `authorityRepository.findAllById(...)` call with `snapshot.getAuthorities()` |
| `RolePermissionServiceDbImpl` | Replace `mergedSecurityService.getCurrentUserAuthorityNames()` + `secPermissionRepository.findByRolesAndTarget(...)` with `snapshot.getMatrix().isEntityPermitted(...)` |
| `AttributePermissionEvaluatorImpl` | Replace `mergedSecurityService.getCurrentUserAuthorityNames()` + `secPermissionRepository.findByRolesAndTargets(...)` with `snapshot.getMatrix().isAttributePermitted(...)` |
| `SecuredEntityCapabilityService` | Already uses its own `PermissionMatrix`; align with shared type, or leave as-is since it already bulk-loads |

**D-01/D-02 compliance:** The `@Scope("request")` bean is destroyed at end of each HTTP request, so the next request always rebuilds fresh. Per-request refresh semantics are preserved exactly.

---

## 2. Test Blocker Fix (D-04)

### Current State (confirmed by running tests)

`./gradlew integrationTest` **passes green** as of 2026-03-28. All tests in `SecuredEntityEnforcementIT` execute successfully. The `SecPermissionMapperImpl` is generated at main compile time (`build/generated/sources/annotationProcessor/java/main/com/vn/core/service/mapper/security/SecPermissionMapperImpl.java`) and is picked up by `@SpringBootTest(classes = {JhipsterSecApp.class, ...})` because `@SpringBootApplication` triggers component scanning of `com.vn.core.**`.

### Root Cause of Historical Failure

The build.gradle declares `annotationProcessor libs.mapstruct.processor` only for the main compile configuration (no `testAnnotationProcessor`). However this is not a problem because `@IntegrationTest` loads the full application context which includes the main-compiled `SecPermissionMapperImpl`. The error previously reported was likely a stale Gradle cache or IDE classpath issue where the annotation processor had not run yet (i.e., a clean checkout before the first `./gradlew classes` invocation). Once the main sources are compiled, the generated impl is on the test classpath.

### Fix Strategy

**No code fix required.** D-04 is resolved. The planner should include a **verification task** rather than a repair task:

1. Run `./gradlew integrationTest` and confirm exit code 0.
2. Confirm test count includes `SecuredEntityEnforcementIT` tests in the XML results under `build/test-results/integrationTest/`.
3. If the CI environment does a clean build without prior `./gradlew classes`, ensure `integrationTest` task depends on `classes` (it already does via `integrationTest.dependsOn(test)` → `:test` → `:classes` in `gradle/profile_dev.gradle`).

---

## 3. Entity List Pagination (PERF-03)

### User-Management Reference Pattern

`UserManagementListComponent` is the canonical reference. Key characteristics:
- `totalItems = signal(0)` — reactive signal, not a plain field.
- `firstRow = computed(() => (this.page() - 1) * this.itemsPerPage)` — computed signal.
- `page = signal(1)` — signal.
- `load()` uses `finalize(() => this.loading.set(false))`.
- `onLazyLoad()` calls `handleNavigation()` which navigates with query params, triggering route-driven reload.
- Template binds `[totalRecords]="totalItems()"` and `[first]="firstRow()"`.

### Backend Status (already complete)

All three resource controllers already accept `Pageable` and return `X-Total-Count`:

| Controller | Pageable | X-Total-Count | Status |
|------------|----------|---------------|--------|
| `DepartmentResource.getAllDepartments(@ParameterObject Pageable)` | YES | YES via `PaginationUtil` | Complete |
| `EmployeeResource.getAllEmployees(@ParameterObject Pageable)` | YES | YES via `PaginationUtil` | Complete |
| `OrganizationResource.getAllOrganizations(@ParameterObject Pageable)` | YES | YES via `PaginationUtil` | Complete |

The backend needs **no changes** for PERF-03.

### Frontend Status (gap analysis)

The three entity list components already:
- Use `[lazy]="true" [paginator]="true"` on `p-table`.
- Call `queryBackend()` which sends `page`, `size`, `sort` query params.
- Read `X-Total-Count` from response headers.

**The gap:** `totalItems` and `page` are plain mutable fields (`totalItems = 0; page = 1;`) rather than signals. `firstRow` is a getter (`get firstRow(): number`). This means:
- `[totalRecords]="totalItems"` — Angular does not detect changes to plain numeric properties in OnPush components, but these components are not yet `OnPush`.
- Missing `ChangeDetectionStrategy.OnPush` in all three entity list components.
- `CommonModule` is imported (legacy) instead of individual imports.

### Per-Entity Changes Required (Frontend Only)

**Department list component (`department-list.component.ts`):**
- `totalItems = 0` → `totalItems = signal(0)`
- `page = 1` → `page = signal(1)`
- `get firstRow()` → `firstRow = computed(() => (this.page() - 1) * this.itemsPerPage)`
- Update `fillComponentAttributesFromResponseHeader`: `this.totalItems.set(Number(...))`
- Update `fillComponentAttributeFromRoute`: `this.page.set(+(page ?? 1))`
- Update `load()` and `queryBackend()` to use `this.page()` instead of `this.page`
- Update `handleNavigation()` to use `this.page()` and `this.sortState()`
- Update `onLazyLoad()` to compare against `this.page()`
- Add `ChangeDetectionStrategy.OnPush` to `@Component` decorator
- Remove `CommonModule` import; add explicit Angular imports used in template
- Remove `standalone: true` explicit annotation (match surrounding local convention — these files already have it; keep as-is to avoid noise)

**Template update:** `[totalRecords]="totalItems"` → `[totalRecords]="totalItems()"`, `[first]="firstRow"` → `[first]="firstRow()"`

**Employee list component (`employee-list.component.ts`):** Identical signal migration pattern as Department.

**Organization list component (`organization-list.component.ts`):** Identical signal migration pattern as Department.

**Note on `NgZone`:** User-management list does NOT use `NgZone.run()` in `handleNavigation` — it calls `router.navigate` directly. The entity list components wrap navigation in `this.ngZone.run(...)`. When adding `OnPush`, this pattern should be preserved (or removed if confirmed unnecessary); do not break existing navigation behavior.

---

## 4. Skeleton Loaders + Responsive UI (UI-05)

### Column Priority Per Entity

#### Department List (current columns)
| Column | Sortable | Priority | Hide at tablet (768–1024px) |
|--------|----------|----------|-----------------------------|
| id | YES | LOW — technical key | YES |
| code | YES | HIGH — business identifier | NO |
| name | YES | HIGH — primary text | NO |
| organization | NO (conditional via `showOrganizationColumn()`) | MEDIUM — relation | YES |
| costCenter | YES (conditional via `showCostCenterColumn()`) | LOW — secondary detail | YES |
| actions | NO | HIGH — always needed | NO (collapse to icon-only) |

**Responsive columns to hide at tablet:** `id`, `organization`, `costCenter` — 3 conditional columns.
**Static column count (always visible):** `code`, `name`, `actions` = 3.

#### Employee List (current columns)
| Column | Sortable | Priority | Hide at tablet |
|--------|----------|----------|----------------|
| id | YES | LOW | YES |
| employeeNumber | YES | HIGH — business identifier | NO |
| firstName | YES | HIGH | NO |
| lastName | YES | HIGH | NO |
| department | NO | MEDIUM — relation | YES |
| email | YES | MEDIUM | NO |
| salary | YES (conditional via `showSalaryColumn()`) | LOW — sensitive | YES (already conditional) |
| actions | NO | HIGH | NO (collapse to icon-only) |

**Responsive columns to hide at tablet:** `id`, `department` — 2 additional columns beyond the existing conditional `salary`.
**Static column count (always visible):** `employeeNumber`, `firstName`, `lastName`, `email`, `actions` = 5.

#### Organization List (current columns)
| Column | Sortable | Priority | Hide at tablet |
|--------|----------|----------|----------------|
| id | YES | LOW | YES |
| code | YES | HIGH — business identifier | NO |
| name | YES | HIGH | NO |
| ownerLogin | YES | MEDIUM — ownership metadata | YES |
| budget | YES (conditional via `showBudgetColumn()`) | LOW — sensitive | YES (already conditional) |
| actions | NO | HIGH | NO (collapse to icon-only) |

**Responsive columns to hide at tablet:** `id`, `ownerLogin` — 2 additional columns beyond the existing conditional `budget`.
**Static column count (always visible):** `code`, `name`, `actions` = 3.

### Responsive Breakpoint Pattern

The existing conditional columns use `computed()` signals against `capability()` data (permission-gated). The responsive columns should use a separate `isTablet = signal(false)` (or `computed`) that reacts to viewport width.

**Pattern to implement:**

```typescript
// In each list component
private readonly breakpointService = inject(BreakpointObserver); // or window resize

readonly isTablet = signal(false);

// During ngOnInit or via effect:
// Watch (min-width: 1024px) breakpoint — hide tablet-priority columns when below
```

**Simplest approach (no external library):** Use Angular CDK `BreakpointObserver` from `@angular/cdk/layout` — already available since Angular CDK is a peer of PrimeNG. Observe `(max-width: 1023px)` and set the signal.

If Angular CDK is not currently imported, the fallback is a `fromEvent(window, 'resize')` observable with `debounceTime` that checks `window.innerWidth`.

Check if `@angular/cdk` is in `frontend/package.json`:

```bash
grep "@angular/cdk" /d/jhipster/frontend/package.json
```

The computed column-hide signals then become:

```typescript
showIdColumn = computed(() => !this.isTablet());
showOrganizationColumn = computed(() =>
  !this.isTablet() && this.canViewField('organization')
);
```

### Skeleton Loader Pattern

PrimeNG `SkeletonModule` is available in `primeng/fesm2022/primeng-skeleton.mjs` (confirmed in `node_modules/primeng`). No installation step needed.

**Implementation approach:** Render skeleton rows inside `ng-template pTemplate="body"` when `loading()` is true AND the current `departments()` signal is empty (first load). For subsequent page loads the existing `[loading]="loading()"` PrimeNG built-in overlay is sufficient.

```typescript
// Import to add to component
import { SkeletonModule } from 'primeng/skeleton';
```

```html
<!-- In the body template, show skeleton for first-load -->
@if (loading() && departments().length === 0) {
  @for (row of skeletonRows; track $index) {
    <tr>
      @for (col of visibleSkeletonCols; track $index) {
        <td><p-skeleton height="1.2rem" /></td>
      }
    </tr>
  }
} @else {
  <!-- existing row template -->
}
```

**Skeleton row count:** Use a fixed array of 5 rows — `readonly skeletonRows = Array(5)`. This matches common enterprise table skeleton UX without needing to mirror the exact page size.

**Skeleton column count:** Mirror the count of always-visible columns per entity (excludes conditional/tablet-hidden ones) plus the actions column. Department: 3 (code, name, actions); Employee: 5 (employeeNumber, firstName, lastName, email, actions); Organization: 3 (code, name, actions).

**Spinner removal:** The `<i class="pi pi-spin pi-spinner text-gray-400"></i>` inside the actions cell when `!capabilityLoaded()` can remain as the capability-loading indicator — this is a different state from data loading and is appropriate to keep.

---

## Plan Decomposition Recommendation

The planner should create **two plans**:

### Plan A — Backend Permission Caching + Test Baseline (PERF-01, PERF-02)

**Wave 0:** Confirm `integrationTest` green baseline; extract `PermissionMatrix` to shared package.
**Wave 1:** Implement `RequestPermissionSnapshot` request-scoped bean.
**Wave 2:** Wire snapshot into `MergedSecurityContextBridge` (authority caching) and `RolePermissionServiceDbImpl` (entity permission caching).
**Wave 3:** Wire snapshot into `AttributePermissionEvaluatorImpl` (attribute permission caching — highest multiplier).
**Wave 4:** Run `integrationTest` as regression gate; all tests must stay green.

This plan is purely backend Java. No frontend changes. Low risk of regression since the snapshot read-through falls back to live queries when scope is unavailable.

### Plan B — Frontend Entity List Hardening + Skeleton Loaders (PERF-03, UI-05)

**Wave 0:** Migrate `totalItems`, `page`, `firstRow` to signals in all three entity list components; add `ChangeDetectionStrategy.OnPush`.
**Wave 1:** Add `isTablet` responsive signal; wire conditional column hiding for `id`, `organization`/`ownerLogin`/`department` columns.
**Wave 2:** Add `SkeletonModule` import; replace initial-load spinner with skeleton rows in each list template.
**Wave 3:** Remove `CommonModule` from imports; replace with individual Angular directives used.

Plans A and B have no shared files and can be executed in parallel or sequentially.

---

## Standard Stack

### Core (Backend)
| Library | Purpose | Source |
|---------|---------|--------|
| `@Scope("request")` Spring | Request-scoped bean lifecycle | Spring Framework (already in use) |
| `SecPermissionRepository.findAllByAuthorityNameIn()` | Bulk permission load — same query used by `SecuredEntityCapabilityService` | Verified in source |
| `ScopedProxyMode.TARGET_CLASS` | Injects request-scoped bean into singleton callers | Standard Spring pattern |

### Core (Frontend)
| Library | Version | Purpose |
|---------|---------|---------|
| `primeng/skeleton` | 21.1.x (installed) | Skeleton placeholder rows |
| `@angular/cdk/layout` | CDK version matching Angular 21 | `BreakpointObserver` for tablet detection |
| Angular signals (`signal`, `computed`) | Angular 21 | Reactive state |

---

## Common Pitfalls

### Pitfall 1: Request-Scoped Bean in Non-Web Context
**What goes wrong:** `RequestPermissionSnapshot` is injected via scoped proxy into a singleton. During integration tests, the test thread may not have an active Spring `RequestScope`, causing `IllegalStateException: No thread-bound request found`.
**Why it happens:** `@SpringBootTest` without a web environment (`webEnvironment = MOCK` via `@AutoConfigureMockMvc`) does set up request/session scopes for `MockMvc` calls, but NOT outside of an active `MockMvc.perform(...)` execution.
**How to avoid:** Inside the snapshot bean's `getAuthorities()` / `getMatrix()` methods, check `RequestContextHolder.getRequestAttributes() != null` before reading cached state. If null (non-web context), bypass the cache and load fresh. Mark the snapshot as "non-caching" in that path. This keeps tests working without mock request scope setup.
**Warning signs:** `ScopeNotActiveException` or `IllegalStateException` in test logs.

### Pitfall 2: Signal vs Plain Field in `p-table` `[totalRecords]` and `[first]`
**What goes wrong:** When `totalItems` is a plain `number` field and the component has `OnPush`, the paginator total count does not update after data loads.
**Why it happens:** `OnPush` only marks a component dirty on input reference changes, `@Output` events, or signal reads. A plain field mutation is invisible.
**How to avoid:** Ensure `totalItems`, `page`, and `firstRow` are signals/computed before adding `OnPush`.
**Warning signs:** Paginator shows 0 total records even when data rows appear.

### Pitfall 3: Skeleton Column Count Mismatch with `colspan` in `emptymessage`
**What goes wrong:** The `emptymessage` template uses a hardcoded `[attr.colspan]` that doesn't account for skeleton rows or the responsive signal state.
**Why it happens:** The existing `colspan` expressions (`showBudgetColumn() ? 6 : 5`) are correct for normal rows but skeletons render a fixed column set.
**How to avoid:** Skeletons are inside the `body` template, not `emptymessage`. The `emptymessage` only shows when `value.length === 0` AND `loading` is false — so when skeletons are displayed, `emptymessage` is suppressed. No `colspan` change needed for skeleton rows.

### Pitfall 4: `ngZone.run()` Navigation vs Signal Updates
**What goes wrong:** Entity list components use `ngZone.run(() => router.navigate(...))` inside `handleNavigation`. After converting to `OnPush`, signal updates inside or outside zone may not trigger CD.
**Why it happens:** Angular signals are zone-independent, but router navigation events must be in-zone.
**How to avoid:** Keep `ngZone.run()` wrapping the `router.navigate` call. Signal reads/writes do not need zone wrapping.

---

## Environment Availability

| Dependency | Required By | Available | Notes |
|------------|------------|-----------|-------|
| PrimeNG Skeleton (`primeng/skeleton`) | UI-05 skeleton rows | YES | Confirmed in `node_modules/primeng/fesm2022/primeng-skeleton.mjs` |
| Angular CDK (`@angular/cdk/layout`) | Responsive breakpoint | Check needed | Verify `grep "@angular/cdk" frontend/package.json` before using `BreakpointObserver`; fallback to `fromEvent(window, 'resize')` |
| Spring `@Scope("request")` | PERF-01 request-scoped snapshot | YES | Standard Spring, no new dependency |
| Gradle `integrationTest` task | PERF-02 test gate | YES | Confirmed: `./gradlew integrationTest` passes |

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test + Testcontainers PostgreSQL |
| Integration test task | `./gradlew integrationTest` (includes `**/*IT*`) |
| Unit test task | `./gradlew test` (excludes `*IT*`) |
| Frontend test task | `cd frontend && npm test` (Vitest) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PERF-01 | Permission queries per request reduced to 2 (authority load + matrix load) | Integration | `./gradlew integrationTest --tests "*SecuredEntityEnforcementIT*"` | YES |
| PERF-02 | All `SecuredEntityEnforcementIT` tests green after caching | Integration | `./gradlew integrationTest` | YES |
| PERF-03 | `p-table` paginator reflects correct totalRecords and page after data load | Manual smoke + Vitest component test | `cd frontend && npm test` | Partial — no dedicated component spec for entity lists |
| UI-05 | Skeleton rows appear during first fetch; columns collapse at 1023px | Manual smoke | Browser DevTools responsive mode | Manual only |

### Sampling Rate
- **Per task commit:** `./gradlew integrationTest` for backend changes; `cd frontend && npm run build` for frontend changes.
- **Per wave merge:** `./gradlew integrationTest` (full backend) + `cd frontend && npm test` (frontend unit).
- **Phase gate:** Full integration test suite green before marking phase complete.

### Wave 0 Gaps
- [ ] No Vitest spec for entity list signal/pagination behavior — acceptable as manual smoke; add if time permits.

---

## Code Examples

### Request-Scoped Snapshot Bean Skeleton

```java
// Source: Pattern from SecuredEntityCapabilityService.PermissionMatrix (this project)
// + standard Spring @Scope("request") pattern
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestPermissionSnapshot {

    private final SecurityContextBridge securityContextBridge;
    private final SecPermissionRepository secPermissionRepository;

    private Collection<String> cachedAuthorities;
    private PermissionMatrix cachedMatrix;

    public RequestPermissionSnapshot(
        SecurityContextBridge securityContextBridge,
        SecPermissionRepository secPermissionRepository
    ) {
        this.securityContextBridge = securityContextBridge;
        this.secPermissionRepository = secPermissionRepository;
    }

    public Collection<String> getAuthorities() {
        if (cachedAuthorities == null) {
            cachedAuthorities = securityContextBridge.getCurrentUserAuthorities();
        }
        return cachedAuthorities;
    }

    public PermissionMatrix getMatrix() {
        if (cachedMatrix == null) {
            Collection<String> authorities = getAuthorities();
            List<SecPermission> perms = authorities.isEmpty()
                ? List.of()
                : secPermissionRepository.findAllByAuthorityNameIn(authorities);
            cachedMatrix = new PermissionMatrix(perms);
        }
        return cachedMatrix;
    }
}
```

### Caller Update — `AttributePermissionEvaluatorImpl` (before → after)

```java
// BEFORE:
Collection<String> authorities = mergedSecurityService.getCurrentUserAuthorityNames();
if (authorities.isEmpty()) { return false; }
List<SecPermission> perms = secPermissionRepository.findByRolesAndTargets(authorities, TargetType.ATTRIBUTE, targets, action);
boolean allowed = perms.stream().anyMatch(p -> "ALLOW".equals(p.getEffect()));

// AFTER (using snapshot):
PermissionMatrix matrix = snapshot.getMatrix();
boolean allowed = matrix.isAttributePermitted(specificTarget, action)
               || matrix.isAttributePermitted(wildcardTarget, action);
```

### Angular Skeleton Loader (Department List excerpt)

```typescript
// Import addition to department-list.component.ts
import { SkeletonModule } from 'primeng/skeleton';

// In @Component imports array:
imports: [..., SkeletonModule],

// Signal additions:
readonly skeletonRows = Array(5).fill(null);
```

```html
<!-- department-list.component.html — body template -->
<ng-template pTemplate="body" let-row>
  @if (loading() && departments().length === 0) {
    <!-- skeleton first-load rows (rendered 5 times via [value]="skeletonRows" workaround
         or use @for here if skeleton is inside a separate template) -->
  } @else {
    <tr> <!-- existing row content --> </tr>
  }
</ng-template>
```

Note: PrimeNG Table's `[loading]="true"` already overlays a mask on subsequent page loads. Skeletons are most valuable only on the initial empty load. The recommended approach is to NOT set `[loading]="loading()"` on `p-table` at all and instead render skeleton rows in the body template when `loading() && items().length === 0`. For subsequent pages, rely on `p-table`'s built-in loading mask (keep `[loading]="loading()"` but add the skeleton body guard).

---

## Sources

### Primary (HIGH confidence)
- Direct code read of all 5 backend security files listed in phase scope
- Direct code read of all 3 entity list components + user-management reference
- Direct code read of `DepartmentResource`, `EmployeeResource`, `OrganizationResource`
- Direct code read of `SecureDataManagerImpl`, `DataManagerImpl`, `CrudEntityConstraint`
- `./gradlew integrationTest` live run — confirmed green

### Secondary (MEDIUM confidence)
- Spring `@Scope("request")` pattern — standard Spring Framework; no version-specific verification needed
- `ScopedProxyMode.TARGET_CLASS` with singleton callers — well-established Spring pattern
- PrimeNG Skeleton availability confirmed via `node_modules` directory listing

### Tertiary (LOW confidence — needs verification by implementer)
- `@angular/cdk/layout` `BreakpointObserver` availability — need `grep "@angular/cdk" frontend/package.json` to confirm version; fallback pattern documented

---

## Metadata

**Confidence breakdown:**
- Backend call chain and N+1 locations: HIGH — traced through source to repository layer
- D-04 status (already resolved): HIGH — confirmed by live test run
- Backend pagination status (already complete): HIGH — read all three resource controllers
- Frontend signal gap: HIGH — read all three list component TypeScript files
- PermissionMatrix reuse pattern: HIGH — read `SecuredEntityCapabilityService` which already uses it
- Skeleton loader availability: HIGH — confirmed in node_modules
- Responsive breakpoint mechanism (CDK vs resize): MEDIUM — CDK availability needs verification

**Research date:** 2026-03-28
**Valid until:** 2026-04-27 (30 days; Spring and Angular APIs are stable)
