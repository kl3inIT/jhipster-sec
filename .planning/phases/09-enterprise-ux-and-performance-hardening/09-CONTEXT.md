# Phase 9: Enterprise UX And Performance Hardening - Context

**Gathered:** 2026-03-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 9 improves the enterprise frontend's consistency, responsiveness, and data-fetch efficiency without weakening the security model that Phase 08.3 stabilized. It also fixes the integration-test blocker that prevents backend secured-entity verification from running.

**Outcome**
- Redundant backend permission work within a single request is removed via request-local caching.
- Entity list screens gain server-side pagination so they scale beyond small data volumes.
- Enterprise screens get skeleton loaders for perceived performance and responsive column/action behavior at tablet widths.
- The `SecuredEntityEnforcementIT` integration test suite unblocked by resolving the missing `SecPermissionMapper` bean at test context startup.

</domain>

<decisions>
## Planning Inputs

### Locked Decisions

- **D-01 (inherited):** Phase 9 may optimize backend secured-read permission checks as part of `PERF-01`, but it must preserve the request-time authority refresh semantics introduced in Phase 08.3. The permission snapshot is per-request only.
- **D-02 (inherited):** For current-user permission caching, the first implementation step is request-local reuse only. Cross-request or session-level caching is out of scope unless a later plan proves explicit invalidation for authority, permission, and menu changes.
- **D-03 (inherited):** Reuse the existing bulk-permission-matrix pattern already present in `SecuredEntityCapabilityService` (`findAllByAuthorityNameIn(...)` once, serve checks from that result) rather than adding another parallel permission-aggregation model.
- **D-04:** Fix the `SecPermissionMapper` bean-not-found failure at `SecuredEntityEnforcementIT` context startup in Phase 9. This is the blocker preventing integration tests from verifying backend permission caching work.
- **D-05:** Add server-side pagination to entity list endpoints for Department, Employee, and Organization using Spring Data `Pageable`. Follow the existing user-management list pattern (`page`, `size`, `sort` query params + `X-Total-Count` header). Wire the PrimeNG paginator in each entity list component.
- **D-06:** UI-05 covers both (a) skeleton loaders replacing spinners during initial entity list data fetch, and (b) responsive column/action behavior at tablet widths (768px–1024px) — column hiding for lower-priority columns, stacked or collapsed action buttons on narrower viewports.

### Captured Todo For Planning

- **T-01:** `Cache current-user permission checks` from `.planning/todos/pending/2026-03-27-cache-current-user-permission-checks.md`
  - Problem: secured entity reloads currently repeat `jhi_authority` validation and `sec_permission` reads within a single request.
  - Candidate direction: build a request-bound permission snapshot once per request, then reuse it from `MergedSecurityContextBridge`, `RolePermissionServiceDbImpl`, `AttributePermissionEvaluatorImpl`, `SecureEntitySerializerImpl`, and `SecuredEntityCapabilityService`.
  - Constraint: the next request must rebuild the snapshot so live permission refresh remains correct.

### Agent's Discretion

- Exact request-local storage mechanism (request attributes vs. request-scoped Spring bean) as long as non-web and test contexts degrade gracefully without a servlet container.
- Whether to extract a shared `PermissionSnapshot` or `PermissionMatrix` type from existing capability code or reuse the existing shape.
- Whether the backend permission caching + SecPermissionMapper fix lands in one plan or two.
- Default page size for entity list pagination (suggest 20, matching user-management).
- Specific columns to hide per entity at tablet widths — researcher should read each list component's column definitions.
- Whether skeleton rows use a fixed column count or mirror the visible column count per entity.

</decisions>

<canonical_refs>
## Canonical References

- `.planning/ROADMAP.md`
- `.planning/REQUIREMENTS.md`
- `.planning/STATE.md`
- `.planning/todos/pending/2026-03-27-cache-current-user-permission-checks.md`
- `.planning/debug/department-relation-not-displayed-in-department-and-employee-views.md`
- `.planning/phases/08.3-user-registration-live-permission-refresh-entity-native-serialization-validation-hardening-and-row-policy-removal/08.3-CONTEXT.md`
- `src/main/java/com/vn/core/security/bridge/MergedSecurityContextBridge.java`
- `src/main/java/com/vn/core/security/permission/RolePermissionServiceDbImpl.java`
- `src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java`
- `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java`
- `src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java`
- `src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java`
- `src/main/java/com/vn/core/service/mapper/security/SecPermissionMapper.java`
- `frontend/src/app/pages/entities/department/list/department-list.component.html`
- `frontend/src/app/pages/entities/employee/list/employee-list.component.html`
- `frontend/src/app/pages/entities/organization/list/organization-list.component.html`
- `frontend/src/app/pages/admin/user-management/list/user-management-list.component.html`
- `frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts`

</canonical_refs>

<code_context>
## Existing Code Insights

### Backend — Permission Redundancy (PERF-01)
- `MergedSecurityContextBridge#getCurrentUserAuthorities()` validates JWT authority names against `jhi_authority` on each call — N calls per request today.
- `RolePermissionServiceDbImpl` and `AttributePermissionEvaluatorImpl` query `SecPermissionRepository` per entity or attribute check.
- `SecureEntitySerializerImpl` multiplies attribute permission checks across every fetched property in a response.
- `SecuredEntityCapabilityService` already proves the useful bulk-load shape by building a `PermissionMatrix` from `findAllByAuthorityNameIn(...)` once — reuse this pattern.

### Backend — Test Blocker (D-04)
- `SecuredEntityEnforcementIT` Spring context fails to start with: `No qualifying bean of type 'com.vn.core.service.mapper.security.SecPermissionMapper' available`.
- `SecPermissionMapper` has `@Mapper(componentModel = "spring")` and MapStruct processor is configured — likely a test-context component-scan or MapperConfig gap rather than a production wiring issue.
- Fix must be scoped to make integration tests runnable without altering the production bean contract.

### Frontend — Entity Lists (PERF-03 + UI-05)
- Department, Employee, Organization list components currently load all records in one call with no pagination params.
- User management list already uses `page`/`size`/`sort` with `X-Total-Count` and PrimeNG `p-paginator` — use as the reference implementation.
- Loading states use capability spinner + PrimeNG table `[loading]` binding. No skeleton rows today.
- Responsive: list templates bind computed signals like `showOrganizationColumn`/`showSalaryColumn` — responsive column hiding can extend this pattern with a `breakpoint` signal.

### Frontend — Caching (already solid)
- `AccountService`, `NavigationService`, and `SecuredEntityCapabilityService` all use `shareReplay` + sessionStorage + smart auth-state invalidation.
- No redundant HTTP calls detected at the frontend layer — main PERF-01 work is backend.

</code_context>
