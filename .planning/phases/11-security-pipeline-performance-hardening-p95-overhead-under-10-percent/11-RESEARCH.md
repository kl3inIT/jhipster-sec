# Phase 11: Security Pipeline Performance Hardening — p95 Overhead Under 10% — Research

**Researched:** 2026-03-31
**Domain:** Java / Spring Boot / Hazelcast caching / JPA / Jackson serialization — backend performance hardening
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**D-01:** Move `SecPermission` matrix loading from request-scoped to **cross-request Hazelcast cache**, keyed by the user's set of authority names (e.g., `{ROLE_ADMIN, ROLE_USER}`).

**D-02:** Cache eviction trigger: **`SecPermission` create / update / delete only**. No TTL fallback. Permission changes must take effect within the next HTTP request — evict all cache entries on any `SecPermission` write.

**D-03:** No additional eviction triggers needed (authority table changes and user authority assignment changes are not in scope for eviction).

**D-04:** The `RequestPermissionSnapshot` bean stays `@Scope("request")` as the per-request access point, but it reads from the Hazelcast cache instead of hitting the DB on every request. Non-web callers continue to use the existing fallback path.

**D-05:** **Remove the `jhi_authority` DB cross-check entirely** from `RequestPermissionSnapshot`. JWT authority names are trusted (the token is already signature-verified by Spring Security's filter chain before this code runs). An authority deleted from `jhi_authority` takes effect when the user's JWT expires — this is the accepted revocation bound for this app.

**D-06:** Use JWT authority names directly without DB validation. This eliminates DB hit #1 per request with no security regression.

**D-07:** **Replace `loadOneInternal` entirely** — remove the `Specification<E> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id)` pattern and route all ID-only loads through `findById` (which uses `EntityManager.find()` and is eligible for JPA L1 cache). No fast-path fallback needed. The CRUD check (`checkCrud(READ)`) continues to run before the fetch, so authorization is unchanged.

**D-08:** In `SecuredEntityJsonAdapter.toJsonArrayString()`, **resolve the `FetchPlan` once before the entity loop**, then pass the resolved `FetchPlan` object into a second overload of `toJson(entity, fetchPlan)`. Eliminates `ClassUtils.getUserClass()` proxy unwrap + `HashMap` lookup N times per page.

**D-09:** **Replace `BeanWrapperImpl` with Jackson `ObjectReader`** for property access in `SecuredEntityJsonAdapter`. This eliminates the expensive reflection-based `BeanWrapperImpl` allocations while keeping the existing `Map → JsonNode → String` chain structure. Per-field attribute filtering via `AttributePermissionEvaluatorImpl` is preserved exactly.

**D-10:** Full `JsonGenerator` streaming (direct write without intermediate objects) is **out of scope for Phase 11** — too high implementation risk. The `BeanWrapper → Jackson ObjectReader` swap is the targeted fix.

**D-11:** **Pre-sort `constraints` in `AccessManagerImpl` constructor** and store the sorted list. Remove the per-call `.sorted(Comparator.comparingInt(AccessConstraint::getOrder))`. The `constraints` list is injected at startup and never mutates. Same constraints, same order — no behavioral change.

### Hard Security Constraints (non-negotiable)

1. Pre-load CRUD checks run before any data access and are not bypassable.
2. Per-field attribute filtering in `SecuredEntityJsonAdapter` is preserved through any serialization refactor — no field may be returned that the current user lacks `VIEW` permission for.
3. Cross-request permission caching requires eviction on `SecPermission` save — TTL-only is not acceptable.
4. No optimization may create a window where a revoked permission stays effective beyond the existing JWT-lifetime bound.

### Claude's Discretion

- Exact Hazelcast map name and configuration (TTL seconds — suggest 3600s as ceiling, actual eviction is write-driven via `@CacheEvict`).
- Whether eviction is implemented via Spring `@CacheEvict` on service write methods or via a direct `IMap.clear()` call in a `SecPermissionService` post-save hook.
- Whether the Hazelcast cache is registered in `CacheConfiguration.java` as a named `MapConfig` or uses the existing `default` map config.
- Exact Jackson `ObjectReader` usage pattern (whether to use `ObjectMapper.readerFor(Map.class)` or a custom `JsonDeserializer` for property extraction).
- Test scope for integration tests — which existing tests cover the affected classes and whether new tests are needed.

### Deferred Ideas (OUT OF SCOPE)

- Full `JsonGenerator` streaming (D-10 explicitly deferred — too high implementation risk).
- Authority-table change or user-authority-assignment change cache eviction (D-03 explicitly excluded).
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PERF-04 | Secured endpoint p95 latency overhead under 10% of unsecured baseline (currently 70.5% list / 426.6% detail) | Six root causes identified in `performance-analysis-2026-03-31.md`. Decisions D-01 through D-11 provide exact fix for each root cause. Benchmark re-run using existing k6 scripts validates KPI after implementation. |
</phase_requirements>

---

## Summary

Phase 11 is a targeted backend performance hardening sprint with zero API contract changes. The Phase 10 benchmark run produced hard numbers: **70.5% overhead on list**, **426.6% overhead on detail** — both failing the p95 < 10% KPI. A follow-up root cause analysis identified six causes with file:line precision.

The dominant costs are two unnecessary DB round-trips per request (one to validate JWT authority names against `jhi_authority`, one to reload the permission matrix). Both can be eliminated with a write-invalidated Hazelcast cache keyed by the user's authority-name set. The detail overhead is primarily driven by the `loadOneInternal` Criteria API path bypassing the JPA L1 cache — replacing it with `dataManager.unconstrained().load(entityClass, id)` (which calls `findById`) requires a single-line change. The remaining gains come from resolving the `FetchPlan` once per batch instead of once per entity, pre-sorting `AccessManagerImpl` constraints in the constructor, and swapping `BeanWrapperImpl` for Jackson `ObjectReader` in `SecureEntitySerializerImpl`.

The code base is well-structured for these changes. `RequestPermissionSnapshot` already has the right shape for caching — it just needs its `loadAuthorities()` and `getMatrix()` calls to go through a Hazelcast `IMap` instead of the DB. `UnconstrainedDataManagerImpl.load(Class, Object id)` already uses `findById`, so `loadOneInternal` can simply delegate to it through `dataManager.unconstrained().load(entityClass, id)`. All security invariants are preserved: CRUD checks still run before any data access, attribute filtering is untouched, and cache eviction fires on every `SecPermission` write path.

**Primary recommendation:** Implement all six fixes in a single wave with a verification benchmark run at the end. Every fix is a narrow, low-risk change to an isolated method; no architectural changes or new dependencies are required.

---

## Standard Stack

### Core (already in project — no new dependencies needed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Hazelcast | 5.5.0 | Cross-request permission matrix cache | Already the project cache layer; `HazelcastCacheManager` active |
| Spring Cache (`@CacheEvict`, `@Cacheable`) | Spring Boot 4.0.3 | Declarative eviction on `SecPermission` writes | Already `@EnableCaching` in `CacheConfiguration`; pattern used by `UserRepository` |
| Jackson `ObjectMapper` | 2.x (via Spring Boot) | Property-map building in serializer | Already injected into `SecuredEntityJsonAdapter`; `ObjectMapper.convertValue()` is the existing path |
| JPA `findById` / `EntityManager.find()` | Spring Data JPA (via Spring Boot 4) | L1-cache-eligible ID lookup | Already used by `UnconstrainedDataManagerImpl.load()` |

### No New Dependencies

All fixes use libraries already on the classpath. No new `build.gradle` entries are needed.

---

## Architecture Patterns

### Pattern 1: Hazelcast-Backed Cross-Request Cache in `RequestPermissionSnapshot`

**What:** `RequestPermissionSnapshot` stays `@Scope("request")` but its lazy-loading methods hit a Hazelcast `IMap<String, PermissionMatrix>` keyed by a canonical string of the sorted authority-name set (e.g., `"ROLE_ADMIN|ROLE_USER"`). On cache miss, the DB query runs as before and the result is stored.

**Eviction:** A new or existing `SecPermissionService` (or directly on `SecPermissionAdminResource` via the service layer) applies `@CacheEvict(cacheNames = "sec-permission-matrix", allEntries = true)` on every create, update, and delete. The `SecPermissionAdminResource` already funnels all writes through `secPermissionRepository.save()` / `secPermissionRepository.deleteAll()`.

**Key design:**

```java
// Cache key: sorted, pipe-joined authority names for stable, unique mapping
private String buildCacheKey(Set<String> authorities) {
    return authorities.stream().sorted().collect(Collectors.joining("|"));
}
```

**Cache map registration in `CacheConfiguration.java`:**

```java
// Source: CacheConfiguration.java pattern — mirrors initializeDomainMapConfig
private MapConfig initializePermissionMatrixMapConfig() {
    MapConfig mapConfig = new MapConfig("sec-permission-matrix");
    mapConfig.setTimeToLiveSeconds(3600);  // ceiling TTL; eviction is write-driven
    mapConfig.setBackupCount(0);           // no redundancy needed for derived data
    return mapConfig;
}
// Add in hazelcastInstance(): config.addMapConfig(initializePermissionMatrixMapConfig());
```

**Spring Cache wiring — use `@CacheEvict` on SecPermission write paths:**

```java
@CacheEvict(cacheNames = "sec-permission-matrix", allEntries = true)
public SecPermission save(SecPermission permission) { ... }

@CacheEvict(cacheNames = "sec-permission-matrix", allEntries = true)
public void delete(Long id) { ... }
```

**Alternative (direct IMap):** Inject `HazelcastInstance` into the service and call `hazelcastInstance.getMap("sec-permission-matrix").clear()`. Both are valid; Spring `@CacheEvict` is cleaner and consistent with the existing `UserRepository` eviction pattern.

**Confidence:** HIGH (Hazelcast Spring cache integration documented, project already uses `@Cacheable` in `UserRepository`)

---

### Pattern 2: Authority Validation Removal (RC#6)

**What:** In `RequestPermissionSnapshot.loadAuthorities()`, delete the `authorityRepository.findAllById()` call entirely. Read JWT authority names directly from `SecurityContextHolder`.

**Current code (to remove):**

```java
// Lines 103-108 of RequestPermissionSnapshot.java — DELETE THESE
Set<String> validNames = authorityRepository
    .findAllById(jwtAuthorities)
    .stream()
    .map(Authority::getName)
    .collect(Collectors.toSet());
return jwtAuthorities.stream().filter(validNames::contains).toList();
```

**Replacement:**

```java
// Trust JWT: the token is already signature-verified before this code runs
return List.copyOf(jwtAuthorities);
```

The `AuthorityRepository` field and import can be removed from `RequestPermissionSnapshot` entirely. `AttributePermissionEvaluatorImpl` does not depend on the DB validation path — it reads from `RequestPermissionSnapshot.getMatrix()` (request scope active) or from `mergedSecurityService` (non-web fallback), neither of which needs the authority cross-check.

**Confidence:** HIGH (logic verified by reading `RequestPermissionSnapshot.java` and `AttributePermissionEvaluatorImpl.java`)

---

### Pattern 3: Replace `loadOneInternal` Criteria API with `unconstrained().load()`

**What:** In `SecureDataManagerImpl.loadOneInternal()`, replace the Specification-based load with a direct call to `dataManager.unconstrained().load(entityClass, id)`.

**Current:**

```java
private <E> Optional<E> loadOneInternal(Class<E> entityClass, Object id) {
    Specification<E> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
    return dataManager.loadOne(entityClass, idSpec, EntityOp.READ);
}
```

**Replacement:**

```java
private <E> Optional<E> loadOneInternal(Class<E> entityClass, Object id) {
    // unconstrained().load() routes through findById -> EntityManager.find() -> eligible for L1 cache
    try {
        return Optional.ofNullable(dataManager.unconstrained().load(entityClass, id));
    } catch (jakarta.persistence.EntityNotFoundException e) {
        return Optional.empty();
    }
}
```

Note: `UnconstrainedDataManagerImpl.load()` throws `EntityNotFoundException` when the entity is not found (line 42 of `UnconstrainedDataManagerImpl.java`). The caller (`loadOne(entityClass, id)` public method) returns `Optional<E>`, so the `EntityNotFoundException` must be caught and converted to `Optional.empty()`. The CRUD check (`resolveEntry(entityClass)`) already runs in `loadOne()` before `loadOneInternal()` is called — authorization is unchanged.

**Confidence:** HIGH (code verified in `SecureDataManagerImpl.java:133-136` and `UnconstrainedDataManagerImpl.java:40-44`)

---

### Pattern 4: Fetch-Plan Resolution Once Per Batch (RC#4)

**What:** In `SecuredEntityJsonAdapter.toJsonArrayString()`, resolve the `FetchPlan` once before the stream using the first entity's class, then add a second `toJson(Object entity, FetchPlan fetchPlan)` overload that accepts an already-resolved plan.

**Current `toJsonArrayString()`:**

```java
// toJson(entity, fetchPlanCode) called per entity — FetchPlan resolved N times
.map(entity -> toJson(entity, fetchPlanCode))
```

**Pattern:**

```java
public String toJsonArrayString(Iterable<?> entities, String fetchPlanCode) {
    if (entities == null) { ... }
    List<?> list = StreamSupport.stream(entities.spliterator(), false).toList();
    if (list.isEmpty()) { return objectMapper.writeValueAsString(objectMapper.createArrayNode()); }

    // Resolve once — all entities in a batch share the same class and fetch plan code
    Class<?> entityClass = ClassUtils.getUserClass(list.getFirst());
    FetchPlan fetchPlan = fetchPlanResolver.resolve(entityClass, fetchPlanCode);

    return objectMapper.writeValueAsString(
        objectMapper.valueToTree(list.stream().map(entity -> toJson(entity, fetchPlan)).toList())
    );
}

// New overload — accepts pre-resolved FetchPlan, skips resolve() call
public JsonNode toJson(Object entity, FetchPlan fetchPlan) {
    if (entity == null) { return objectMapper.nullNode(); }
    return objectMapper.valueToTree(secureEntitySerializer.serialize(entity, fetchPlan));
}
```

The original `toJson(Object entity, String fetchPlanCode)` method stays for single-entity callers (e.g., `toJsonString`).

**Confidence:** HIGH (pattern is straightforward; `FetchPlanResolver.resolve()` is pure and deterministic for the same class+code pair)

---

### Pattern 5: Pre-Sort `AccessManagerImpl` Constraints (RC#5)

**What:** Sort the injected `constraints` list in the constructor and store the sorted copy. Remove the per-call `.sorted()`.

**Current:**

```java
public AccessManagerImpl(List<AccessConstraint<?>> constraints) {
    this.constraints = constraints;  // unsorted injection
}

// Per call:
.sorted(Comparator.comparingInt(AccessConstraint::getOrder))
```

**Replacement:**

```java
public AccessManagerImpl(List<AccessConstraint<?>> constraints) {
    this.constraints = constraints.stream()
        .sorted(Comparator.comparingInt(AccessConstraint::getOrder))
        .toList();  // immutable, sorted
}

// Remove the per-call .sorted() in applyRegisteredConstraints()
```

**Confidence:** HIGH (trivial constructor change; constraints are Spring beans, injected once at startup and never mutated)

---

### Pattern 6: Replace `BeanWrapperImpl` with Jackson `ObjectReader` in `SecureEntitySerializerImpl` (RC#3)

**What:** Replace `BeanWrapperImpl wrapper = new BeanWrapperImpl(entity)` + `wrapper.getPropertyValue(attr)` with Jackson `ObjectMapper.convertValue(entity, Map.class)` to build a property map once per entity, then access fields from the `Map`.

**Current flow (SecureEntitySerializerImpl.serialize):**
1. `BeanWrapperImpl(entity)` — reflection-based wrapper allocation
2. `wrapper.getPropertyValue(attr)` — per-property reflection call

**Replacement approach:**

```java
// Source: Jackson ObjectMapper.convertValue — converts entity to Map<String,Object> in one pass
// This replaces BeanWrapperImpl and eliminates per-property reflection calls
@SuppressWarnings("unchecked")
private Map<String, Object> toPropertyMap(Object entity, ObjectMapper objectMapper) {
    return objectMapper.convertValue(entity, Map.class);
}
```

Then in `serialize()`, call `toPropertyMap()` once per entity and do `propertyMap.get(attr)` instead of `wrapper.getPropertyValue(attr)`. All attribute permission checks and the existing `Map → JsonNode → String` chain remain identical.

**Important:** `objectMapper.convertValue()` performs full Jackson serialization internally (uses registered serializers, handles types correctly). This is cleaner than `BeanWrapperImpl` for complex types and avoids the BeanWrapper reflection overhead. The `SecureEntitySerializer` interface returns `Map<String, Object>` — the interface contract is unchanged.

**Caveat:** `convertValue` will serialize associations too, then the attribute filtering discards unauthorized ones. For Hibernate lazy proxies, `ClassUtils.getUserClass()` unwrap is needed before `convertValue`. The existing code already uses `ClassUtils.getUserClass()` in `SecuredEntityJsonAdapter.toJson()` — apply the same pattern here.

**Confidence:** MEDIUM-HIGH (Jackson `convertValue` is the idiomatic "entity to map" conversion; BeanWrapper is explicitly identified as the performance cost; the approach preserves the `Map → JsonNode → String` chain per D-10)

---

### Anti-Patterns to Avoid

- **Don't use TTL-only eviction for the permission cache.** The CONTEXT.md constraint is write-triggered eviction only. TTL at 3600s is a ceiling/safety net, not the primary eviction mechanism.
- **Don't move permission matrix loading outside `RequestPermissionSnapshot`.** The request-scoped bean stays as the per-request access point (D-04). The Hazelcast map is the backing store, not a replacement for the bean.
- **Don't remove the `checkCrud(READ)` call** when replacing `loadOneInternal`. The CRUD check in `SecureDataManagerImpl.loadOne()` (via `resolveEntry()` and ultimately `dataManager.checkCrud()`) runs before `loadOneInternal()` — the sequence of calls preserves this.
- **Don't evict only the single key** when a `SecPermission` changes. Authority sets share a combined key (e.g., `ROLE_ADMIN|ROLE_USER`). One permission change affects many possible key combinations — use `allEntries = true`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Cache eviction on entity write | Manual `@PostPersist` / `@PostUpdate` JPA listeners | Spring `@CacheEvict` on service methods | Already `@EnableCaching` in project; cleaner transactional boundary |
| Permission matrix cache invalidation | Custom event bus or Pub/Sub | Spring `@CacheEvict(allEntries=true)` | `allEntries=true` clears all authority-key variants in one call |
| Sorted constraint list | Per-call `Collections.sort()` | Pre-sort in constructor, store with `List.copyOf()` | Immutable sorted list; zero per-call overhead |
| Property access on JPA entity | New reflection framework or custom introspection | Jackson `ObjectMapper.convertValue()` | Jackson is already on classpath; handles proxies, type conversion |

---

## Common Pitfalls

### Pitfall 1: `@CacheEvict` Must Be on a Spring-Managed Bean Method (Not a Repository)

**What goes wrong:** Placing `@CacheEvict` directly on `SecPermissionRepository` save/delete methods does not work — Spring AOP proxies only intercept calls to Spring-managed beans from external callers.

**Why it happens:** `SecPermissionAdminResource` currently calls `secPermissionRepository.save()` and `secPermissionRepository.deleteAll()` directly. The eviction annotation would need to be on a service that wraps these calls.

**How to avoid:** Add a `SecPermissionService` (a thin Spring `@Service`) wrapping `save()` and `delete()` calls, and apply `@CacheEvict` there. `SecPermissionAdminResource` calls the service instead of the repository directly. The `createPermission`, `updatePermission`, and `deletePermission` handlers in `SecPermissionAdminResource` are the three write paths to route through the service.

**Warning signs:** Permission changes don't clear the cache (permissions appear stale after admin edits).

---

### Pitfall 2: `UnconstrainedDataManagerImpl.load()` Throws Instead of Returning Empty

**What goes wrong:** `UnconstrainedDataManagerImpl.load(entityClass, id)` throws `EntityNotFoundException` when the entity is absent — not `Optional.empty()`. `SecureDataManagerImpl.loadOneInternal()` returns `Optional<E>`, so callers expect an empty Optional for missing entities.

**Why it happens:** The `UnconstrainedDataManager.load()` contract was designed for trusted internal use where absence is an error. The `SecureDataManagerImpl` public API returns `Optional`.

**How to avoid:** Wrap the `dataManager.unconstrained().load()` call in a try/catch for `EntityNotFoundException` and return `Optional.empty()`.

**Warning signs:** `NoSuchElementException` or `EntityNotFoundException` surface for valid "not found" scenarios in secured detail endpoints.

---

### Pitfall 3: Jackson `convertValue` on Hibernate Lazy Proxies

**What goes wrong:** Calling `objectMapper.convertValue(entity, Map.class)` on a Hibernate entity that has uninitialized lazy associations triggers lazy loading for every association — even ones that will be filtered out by attribute permissions or excluded by the fetch plan.

**Why it happens:** Jackson's default `BeanSerializer` traverses all getters. Hibernate interceptors initialize lazy proxies on getter access.

**How to avoid:** The `SecureEntitySerializerImpl.serialize()` method already walks only the properties declared in `FetchPlan.getProperties()` — it does not call `convertValue` on the whole entity. The `BeanWrapperImpl` replacement should preserve this property-by-property approach: only access `propertyMap.get(attr)` for attrs declared in the fetch plan, not convert the full entity upfront. Alternatively, apply Hibernate's `Hibernate.isInitialized()` guard for association properties before accessing them.

**Warning signs:** N+1 lazy-load queries appear in SQL logs after the serializer change; response time increases instead of decreasing.

---

### Pitfall 4: Hazelcast Cache Name Must Match `@CacheEvict` Exactly

**What goes wrong:** Cache is declared as `"sec-permission-matrix"` in `CacheConfiguration.addMapConfig()` but the `@CacheEvict` annotation uses `"secPermissionMatrix"` (camelCase) — Spring Cache treats these as different caches.

**Why it happens:** Hazelcast map names are case-sensitive strings; Spring Cache uses the same string as the map name.

**How to avoid:** Define the cache name as a public constant (e.g., `public static final String PERMISSION_MATRIX_CACHE = "sec-permission-matrix"`) and reference it in both `CacheConfiguration` and `@CacheEvict`.

---

### Pitfall 5: `deleteInternal` in `SecureDataManagerImpl` Also Uses a Criteria API `idSpec`

**What goes wrong:** D-07 targets `loadOneInternal` for the read path. But `SecureDataManagerImpl.deleteInternal()` and `saveInternal()` (for UPDATE, line 149) also build Criteria API `idSpec` Specifications. These are not the primary performance bottleneck (they're write paths, not read-hot), but inconsistency is confusing.

**Why it happens:** Same pattern used throughout; root cause analysis only called out the read path because it's on the hot path.

**How to avoid:** For Phase 11, only fix `loadOneInternal`. Leave `deleteInternal` and `saveInternal` using the existing Specification path (writes are infrequent). Document this explicitly so it's not accidentally "fixed" in a way that changes authorization semantics.

---

## Runtime State Inventory

> Phase 11 is a pure code/performance optimization phase with no rename, rebrand, or migration.

**Not applicable.** No stored data, service configs, OS registrations, secrets, or build artifacts reference any renamed entity. Skipped per phase-type exclusion.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| k6 | Benchmark re-run (Phase 11 verification) | Confirmed | v1.7.1 (installed at `C:\Users\admin\tools\k6\v1.7.1\`) | — |
| PostgreSQL (remote) | Integration tests, benchmark | Confirmed | Remote at `157.230.42.136:5555` | — |
| Gradle | Build and test | Confirmed | 9.4.0 | — |
| Java 25 | Build and test | Confirmed (per STATE.md note) | 25 | — |

**k6 benchmark re-run:** Use the same commands documented in `benchmark-run-summary-2026-03-31.md` Section 7. The valid `ORG_ID=1501` must be used for the detail benchmark.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test + Mockito |
| Config file | Gradle `test` task (unit), `integrationTest` task (`*IT` suites) |
| Quick run command | `./gradlew test --tests "com.vn.core.security.*"` |
| Full suite command | `./gradlew test integrationTest` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PERF-04 | Permission matrix loaded from Hazelcast cache, not DB, on second request | unit | `./gradlew test --tests "com.vn.core.security.permission.RequestPermissionSnapshotTest"` | ❌ Wave 0 |
| PERF-04 | Authority validation removed — JWT names used directly, no DB hit | unit | `./gradlew test --tests "com.vn.core.security.permission.RequestPermissionSnapshotTest"` | ❌ Wave 0 |
| PERF-04 | Cache evicted after `SecPermission` save/update/delete | unit | `./gradlew test --tests "com.vn.core.service.security.SecPermissionServiceTest"` | ❌ Wave 0 |
| PERF-04 | `loadOneInternal` routes through `findById`, not Criteria API | unit | `./gradlew test --tests "com.vn.core.security.data.SecureDataManagerImplTest"` | ✅ (needs new test case for findById routing) |
| PERF-04 | `toJsonArrayString` resolves FetchPlan once, not N times | unit | `./gradlew test --tests "com.vn.core.security.web.SecuredEntityJsonAdapterTest"` | ❌ Wave 0 |
| PERF-04 | `AccessManagerImpl` constraints pre-sorted in constructor | unit | `./gradlew test --tests "com.vn.core.security.access.AccessManagerImplTest"` | ❌ Wave 0 |
| PERF-04 | `SecureEntitySerializerImpl` serializes same fields with same permission filtering after BeanWrapper removal | unit | `./gradlew test --tests "com.vn.core.security.serialize.SecureEntitySerializerImplTest"` | ✅ (existing; verify still passes) |
| PERF-04 | End-to-end: p95 overhead < 10% on list and detail endpoints | manual benchmark | `k6 run load-tests/scripts/org-list-benchmark.js` + `k6 run --env ORG_ID=1501 load-tests/scripts/org-detail-benchmark.js` | ✅ (scripts exist) |

### Sampling Rate

- **Per task commit:** `./gradlew test --tests "com.vn.core.security.*"`
- **Per wave merge:** `./gradlew test integrationTest`
- **Phase gate:** Full suite green + benchmark re-run showing p95 overhead < 10% before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `src/test/java/com/vn/core/security/permission/RequestPermissionSnapshotTest.java` — covers PERF-04 (cache hit on second call, authority removal, cache-miss → DB path)
- [ ] `src/test/java/com/vn/core/service/security/SecPermissionServiceTest.java` — covers PERF-04 (cache eviction on save/delete)
- [ ] `src/test/java/com/vn/core/security/web/SecuredEntityJsonAdapterTest.java` — covers PERF-04 (fetch plan resolved once; existing adapter tests may not cover `toJsonArrayString`)
- [ ] `src/test/java/com/vn/core/security/access/AccessManagerImplTest.java` — covers PERF-04 (constraints applied in order after constructor pre-sort)

---

## Code Examples

### Example 1: Hazelcast-Backed Permission Matrix Cache Key

```java
// Source: pattern from existing CacheConfiguration.java + UserRepository @Cacheable usage
// Key is deterministic: sorted authority names joined by pipe
private String buildCacheKey(Set<String> jwtAuthorities) {
    return jwtAuthorities.stream()
        .sorted()
        .collect(Collectors.joining("|"));
}
```

### Example 2: Cache Map Registration in `CacheConfiguration`

```java
// Source: mirrors initializeDomainMapConfig() pattern in CacheConfiguration.java
private MapConfig initializePermissionMatrixMapConfig() {
    MapConfig mapConfig = new MapConfig("sec-permission-matrix");
    mapConfig.setTimeToLiveSeconds(3600);  // safety ceiling; eviction is write-driven
    mapConfig.setBackupCount(0);
    return mapConfig;
}
```

Add `config.addMapConfig(initializePermissionMatrixMapConfig())` in `hazelcastInstance()`.

### Example 3: Spring `@CacheEvict` on SecPermissionService

```java
// Source: pattern from UserRepository (uses @Cacheable on USERS_BY_LOGIN_CACHE)
@Service
@Transactional
public class SecPermissionService {

    public static final String PERMISSION_MATRIX_CACHE = "sec-permission-matrix";

    private final SecPermissionRepository secPermissionRepository;
    private final CacheManager cacheManager;  // or use @CacheEvict annotation

    @CacheEvict(cacheNames = PERMISSION_MATRIX_CACHE, allEntries = true)
    public SecPermission save(SecPermission permission) {
        return secPermissionRepository.save(permission);
    }

    @CacheEvict(cacheNames = PERMISSION_MATRIX_CACHE, allEntries = true)
    public void delete(Long id) {
        secPermissionRepository.deleteById(id);
    }

    @CacheEvict(cacheNames = PERMISSION_MATRIX_CACHE, allEntries = true)
    public void deleteAll(Iterable<SecPermission> permissions) {
        secPermissionRepository.deleteAll(permissions);
    }
}
```

### Example 4: `RequestPermissionSnapshot.getMatrix()` Reading from Hazelcast

```java
// Inject HazelcastInstance or use Spring CacheManager
// Pattern: check IMap first, compute on miss, store result
public PermissionMatrix getMatrix() {
    if (cachedMatrix == null) {
        Collection<String> authorities = getAuthorities();
        if (authorities.isEmpty()) {
            cachedMatrix = PermissionMatrix.EMPTY;
        } else {
            String cacheKey = buildCacheKey(new HashSet<>(authorities));
            IMap<String, PermissionMatrix> cache = hazelcastInstance.getMap("sec-permission-matrix");
            cachedMatrix = cache.computeIfAbsent(cacheKey, k -> {
                List<SecPermission> allPerms = secPermissionRepository.findAllByAuthorityNameIn(authorities);
                return new PermissionMatrix(allPerms);
            });
        }
    }
    return cachedMatrix;
}
```

Note: `IMap.computeIfAbsent()` is available in Hazelcast 5.x and is atomic.

### Example 5: `loadOneInternal` Replacement

```java
// Source: SecureDataManagerImpl.java:133-136 — replace Specification with unconstrained().load()
private <E> Optional<E> loadOneInternal(Class<E> entityClass, Object id) {
    try {
        return Optional.ofNullable(dataManager.unconstrained().load(entityClass, id));
    } catch (jakarta.persistence.EntityNotFoundException e) {
        return Optional.empty();
    }
}
```

### Example 6: Pre-Sorted `AccessManagerImpl` Constructor

```java
// Source: AccessManagerImpl.java:16-17 — sort once in constructor
public AccessManagerImpl(List<AccessConstraint<?>> constraints) {
    this.constraints = constraints.stream()
        .sorted(Comparator.comparingInt(AccessConstraint::getOrder))
        .toList();
}

// In applyRegisteredConstraints — remove the .sorted() call:
constraints
    .stream()
    .filter(c -> c.supports().isAssignableFrom(context.getClass()))
    // .sorted(...) REMOVED
    .forEach(c -> ((AccessConstraint<C>) c).applyTo(context));
```

---

## Write Path Audit: All SecPermission Mutations

The eviction must cover all paths that write to `sec_permission`. From reading `SecPermissionAdminResource.java`:

| Handler | Operation | Lines | Notes |
|---------|-----------|-------|-------|
| `createPermission` | `secPermissionRepository.save(entity)` | ~117-119 | Primary create path |
| `createPermission` | `secPermissionRepository.save(canonicalPermission)` | ~103 | Dedup/normalize path |
| `createPermission` | `secPermissionRepository.deleteAll(duplicatesToDelete)` | ~110 | Cleanup path |
| `createPermission` | `deleteRedundantSpecificPermissions()` → `secPermissionRepository.deleteSpecificEntityPermissions/deleteSpecificAttributePermissions` | ~194-206 | Wildcard cleanup |
| `updatePermission` | `secPermissionRepository.save(entity)` | ~184 | Update path |
| `deletePermission` | `secPermissionRepository.deleteAll(...)` | ~224 | Delete path |

A `SecPermissionService` wrapping all of these with `@CacheEvict(allEntries=true)` is the safest approach. Alternatively, direct `IMap.clear()` in `SecPermissionAdminResource` after each write path works but requires injecting `HazelcastInstance` into the resource.

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Per-request authority DB validation | Remove authority DB validation; trust JWT | Phase 11 (this phase) | Eliminates ~10-30ms DB hit #1 per request |
| Per-request permission matrix DB load | Cross-request Hazelcast cache | Phase 11 (this phase) | Eliminates ~10-30ms DB hit #2 per request |
| Criteria API ID lookup | `findById` → JPA L1 cache eligible | Phase 11 (this phase) | Primary fix for 426% detail overhead |
| `BeanWrapperImpl` property access | Jackson `convertValue`-backed property map | Phase 11 (this phase) | Eliminates per-entity reflection overhead |
| FetchPlan resolved per entity in batch | Resolved once before loop | Phase 11 (this phase) | Eliminates N redundant `ClassUtils.getUserClass()` + HashMap lookup |
| Constraints sorted per `checkCrud()` call | Pre-sorted in constructor | Phase 11 (this phase) | Eliminates CPU cost of per-call sort |

**Deprecated/outdated after Phase 11:**
- `AuthorityRepository` injection in `RequestPermissionSnapshot`: removed entirely.
- `Specification<E> idSpec` in `loadOneInternal`: removed.
- `BeanWrapperImpl` in `SecureEntitySerializerImpl`: removed.

---

## Open Questions

1. **Does `PermissionMatrix` need to implement `Serializable` for Hazelcast distributed caching?**
   - What we know: In dev mode (`127.0.0.1` only, no multicast), Hazelcast runs single-node so objects don't need to cross the wire. In production, cross-node caching requires serialization.
   - What's unclear: Whether this app runs Hazelcast in clustered mode in production.
   - Recommendation: Add `implements Serializable` to `PermissionMatrix` proactively (it only holds a `Set<String>` — trivial to serialize). Zero risk, prevents a hidden production failure.

2. **Should `SecPermissionService` be introduced or should eviction use direct `HazelcastInstance.getMap().clear()` in `SecPermissionAdminResource`?**
   - What we know: `SecPermissionAdminResource` directly calls the repository today. A `SecPermissionService` wrapper is cleaner (standard Spring layering) but adds a new file.
   - Recommendation: Introduce `SecPermissionService` — it's the idiomatic approach and consolidates all write paths into one eviction point. The admin resource then depends on the service, matching the project's architecture convention.

3. **What is the valid `ORG_ID` for the detail benchmark re-run?**
   - What we know: `ORG_ID=1501` was valid on 2026-03-31 per `benchmark-run-summary-2026-03-31.md`.
   - What's unclear: Whether the database state changes between now and the Phase 11 benchmark run.
   - Recommendation: Verify with `GET /api/benchmark/organizations-standard/1501` before running the benchmark. If 404, query for a valid ID.

---

## Sources

### Primary (HIGH confidence)

- Source code: `src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java` — exact lines of DB hits identified
- Source code: `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java` — `loadOneInternal` Criteria API pattern confirmed
- Source code: `src/main/java/com/vn/core/security/web/SecuredEntityJsonAdapter.java` — `toJsonArrayString` per-entity FetchPlan resolve confirmed
- Source code: `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java` — `BeanWrapperImpl` usage confirmed
- Source code: `src/main/java/com/vn/core/security/access/AccessManagerImpl.java` — per-call `.sorted()` confirmed
- Source code: `src/main/java/com/vn/core/config/CacheConfiguration.java` — Hazelcast 5.5.0 instance name `jhipster-sec`, `MapConfig` registration pattern
- Source code: `src/main/java/com/vn/core/security/data/UnconstrainedDataManagerImpl.java` — `load()` uses `findById`, throws `EntityNotFoundException`
- Source code: `src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java` — all write paths inventoried
- `load-tests/results/performance-analysis-2026-03-31.md` — root cause analysis with overhead attribution
- `load-tests/results/benchmark-run-summary-2026-03-31.md` — baseline numbers and benchmark commands

### Secondary (MEDIUM confidence)

- `gradle/libs.versions.toml` — Hazelcast 5.5.0 version confirmed
- Existing test suite: `SecureEntitySerializerImplTest`, `SecureDataManagerImplTest`, `AttributePermissionEvaluatorImplTest` — test gaps identified

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries already in project, verified in `build.gradle` and `libs.versions.toml`
- Architecture: HIGH — all changes verified against actual source code; no speculative patterns
- Pitfalls: HIGH — pitfalls derived from reading actual implementation code, not from general knowledge
- Test gaps: HIGH — confirmed by globbing `src/test/` directory

**Research date:** 2026-03-31
**Valid until:** 2026-04-30 (stable Spring/Hazelcast APIs; no fast-moving dependencies involved)
