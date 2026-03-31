# Phase 11: Security Pipeline Performance Hardening — p95 Overhead Under 10% — Context

**Gathered:** 2026-03-31
**Status:** Ready for planning

<domain>
## Phase Boundary

Reduce the p95 latency overhead of the `@SecuredEntity` pipeline below 10% of the unsecured baseline by fixing six root causes identified in the Phase 10 benchmark run. Current measured overhead: **70.5% on list** and **426.6% on detail** — both failing the KPI.

Root cause analysis is fully documented in `load-tests/results/performance-analysis-2026-03-31.md`. This phase implements the fixes. It does **not** change API contracts, add new endpoints, or modify the security authorization model.

</domain>

<security_constraint>
## Hard Constraint: Security Model Preservation

**Every optimization in this phase must strictly preserve the existing authorization guarantees:**

1. **Pre-load CRUD checks** run before any data access and are not bypassable by changing how rows are fetched.
2. **Per-field attribute filtering** in `SecuredEntityJsonAdapter` is preserved through any serialization refactor — no field may be returned that the current user lacks `VIEW` permission for.
3. **Cross-request permission caching** requires eviction on `SecPermission` save — TTL-only is not acceptable (permission changes must take effect within the next request).
4. No optimization may create a window where a revoked permission stays effective beyond the existing JWT-lifetime bound.

These constraints are non-negotiable and apply to all plans in this phase.

</security_constraint>

<decisions>
## Implementation Decisions

### Permission matrix caching (RC#1)
- **D-01:** Move `SecPermission` matrix loading from request-scoped to **cross-request Hazelcast cache**, keyed by the user's set of authority names (e.g., `{ROLE_ADMIN, ROLE_USER}`).
- **D-02:** Cache eviction trigger: **`SecPermission` create / update / delete only**. No TTL fallback. Permission changes must take effect within the next HTTP request — evict all cache entries on any `SecPermission` write.
- **D-03:** No additional eviction triggers needed (authority table changes and user authority assignment changes are not in scope for eviction).
- **D-04:** The `RequestPermissionSnapshot` bean stays `@Scope("request")` as the per-request access point, but it reads from the Hazelcast cache instead of hitting the DB on every request. Non-web callers continue to use the existing fallback path.

### Authority validation removal (RC#6)
- **D-05:** **Remove the `jhi_authority` DB cross-check entirely** from `RequestPermissionSnapshot`. JWT authority names are trusted (the token is already signature-verified by Spring Security's filter chain before this code runs). An authority deleted from `jhi_authority` takes effect when the user's JWT expires — this is the accepted revocation bound for this app.
- **D-06:** Use JWT authority names directly without DB validation. This eliminates DB hit #1 per request with no security regression.

### ID lookup replacement (RC#2)
- **D-07:** **Replace `loadOneInternal` entirely** — remove the `Specification<E> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id)` pattern and route all ID-only loads through `findById` (which uses `EntityManager.find()` and is eligible for JPA L1 cache). No fast-path fallback needed. The CRUD check (`checkCrud(READ)`) continues to run before the fetch, so authorization is unchanged.

### Fetch-plan resolution (RC#4)
- **D-08:** In `SecuredEntityJsonAdapter.toJsonArrayString()` (or equivalent batch entry point), **resolve the `FetchPlan` once before the entity loop**, then pass the resolved `FetchPlan` object into a second overload of `toJson(entity, fetchPlan)`. Eliminates `ClassUtils.getUserClass()` proxy unwrap + `HashMap` lookup N times per page.

### Serialization chain reduction (RC#3)
- **D-09:** **Replace `BeanWrapperImpl` with Jackson `ObjectReader`** for property access in `SecuredEntityJsonAdapter`. This eliminates the expensive reflection-based `BeanWrapperImpl` allocations while keeping the existing `Map → JsonNode → String` chain structure. Per-field attribute filtering via `AttributePermissionEvaluatorImpl` is preserved exactly.
- **D-10:** Full `JsonGenerator` streaming (direct write without intermediate objects) is **out of scope for Phase 11** — too high implementation risk. The `BeanWrapper → Jackson ObjectReader` swap is the targeted fix.

### AccessManager sort (RC#5)
- **D-11:** **Pre-sort `constraints` in `AccessManagerImpl` constructor** and store the sorted list. Remove the per-call `.sorted(Comparator.comparingInt(AccessConstraint::getOrder))`. The `constraints` list is injected at startup and never mutates. Same constraints, same order — no behavioral change.

### Claude's Discretion
- Exact Hazelcast map name and configuration (TTL seconds for the map config — set high enough that eviction is effectively write-driven, not time-driven; suggest 3600s as a ceiling, actual eviction happens via `@CacheEvict`).
- Whether eviction is implemented via Spring `@CacheEvict` on the service write methods or via a direct `IMap.clear()` call in a `SecPermissionService` post-save hook.
- Whether the Hazelcast cache is registered in `CacheConfiguration.java` as a named `MapConfig` or uses the existing `default` map config.
- Exact Jackson `ObjectReader` usage pattern (whether to use `ObjectMapper.readerFor(Map.class)` or a custom `JsonDeserializer` for property extraction).
- Test scope for integration tests — researcher should identify which existing tests cover the affected classes and whether new tests are needed.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Root cause analysis (primary input)
- `load-tests/results/performance-analysis-2026-03-31.md` — 6 root causes with file:line references, estimated overhead attribution, and priority fix order

### Files to change (per root cause analysis)
- `src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java` — RC#1 (Hazelcast cache), RC#6 (remove authority DB validation)
- `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java` — RC#2 (replace loadOneInternal Spec with findById)
- `src/main/java/com/vn/core/security/web/SecuredEntityJsonAdapter.java` — RC#3 (BeanWrapper → ObjectReader), RC#4 (fetch plan once per batch)
- `src/main/java/com/vn/core/security/access/AccessManagerImpl.java` — RC#5 (pre-sort constraints)
- `src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java` — RC#6 (verify authority validation removal doesn't break attribute checks)

### Cache infrastructure
- `src/main/java/com/vn/core/config/CacheConfiguration.java` — Hazelcast instance and map config; new cache map must be registered here
- `src/main/resources/config/application.yml` — JHipster cache TTL/backup-count properties used by CacheConfiguration

### SecPermission write paths (eviction trigger points)
- `src/main/java/com/vn/core/service/security/SecPermissionService.java` — or equivalent service; researcher must locate all create/update/delete paths for SecPermission

### Security pipeline context
- `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java` — full pipeline: CRUD check → fetch-plan → serialize/merge
- `src/main/java/com/vn/core/security/bridge/MergedSecurityContextBridge.java` — authority resolution entry point
- `src/main/java/com/vn/core/security/permission/PermissionMatrix.java` — matrix type being cached

### Benchmark baseline (before/after comparison)
- `load-tests/results/benchmark-run-summary-2026-03-31.md` — current p95 numbers; Phase 11 must re-run these benchmarks to verify KPI met
- `load-tests/scripts/org-list-benchmark.js` — list benchmark script
- `load-tests/scripts/org-detail-benchmark.js` — detail benchmark script

### Prior phase context
- `.planning/phases/09-enterprise-ux-and-performance-hardening/09-CONTEXT.md` — D-01/D-02: request-scope-only cache was the Phase 9 constraint; Phase 11 supersedes D-02 with cross-request Hazelcast cache + eviction
- `.planning/REQUIREMENTS.md` — PERF-01 through PERF-03 validated; PERF-04 is the active requirement for this phase

</canonical_refs>
