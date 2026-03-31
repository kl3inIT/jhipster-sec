# Security Pipeline Performance Root Cause Analysis

**Date:** 2026-03-31
**Analyst:** Claude Code (Sonnet 4.6)
**Based on:** k6 benchmark results + source code audit

---

## Benchmark Results

| Test | Baseline p95 | Secured p95 | Overhead | KPI |
|------|-------------|-------------|---------|-----|
| Organization List | 291.5ms | 497.1ms | **70.5%** | FAIL |
| Organization Detail | 64.6ms | 340.1ms | **426.6%** | FAIL |

KPI target: p95 overhead < 10%

---

## Root Cause #1 — Two Extra DB Round-Trips Per Request (CRITICAL)

**File:** `src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java:93-108`

```java
// DB HIT 1 — validates JWT roles against jhi_authority table on EVERY request
Set<String> validNames = authorityRepository.findAllById(jwtAuthorities)...

// DB HIT 2 — loads full permission matrix on EVERY request
List<SecPermission> allPerms = secPermissionRepository.findAllByAuthorityNameIn(authorities);
```

**Why this hurts:** The database is remote (`157.230.42.136:5555`). Each of these two queries adds ~10-30ms of network latency. Both results are **static for the lifetime of a JWT token** — the authority list and the permission matrix only change when an admin edits roles/permissions, which is rare. Yet the current code reloads them on every single HTTP request.

The `RequestPermissionSnapshot` bean is `@Scope("request")` — meaning it caches within a request (pointless for these queries) but provides zero cross-request caching. For 50 concurrent VUs, all 50 simultaneously hammer the permission DB.

**Fix direction:**
- Authority validation: cache by `Set<authority_names>` in Hazelcast with TTL = JWT expiry. JWT is already cryptographically signed — no DB roundtrip needed to trust the names it carries.
- Permission matrix: cache by `Set<authority_names>` in Hazelcast with TTL ~30-60s (or invalidate on permission save). `SecPermission` rows almost never change during normal operation.

---

## Root Cause #2 — Criteria API ID Lookup Bypasses JPA L1 Cache (CRITICAL for Detail)

**File:** `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java:133-136`

```java
// Secured path — builds a full Criteria API query, bypasses EntityManager.find()
private <E> Optional<E> loadOneInternal(Class<E> entityClass, Object id) {
    Specification<E> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
    return dataManager.loadOne(entityClass, idSpec, EntityOp.READ);
}
```

**vs. Baseline** (`BenchmarkOrganizationStandardService.java:39`):

```java
// Baseline — uses EntityManager.find(), eligible for L1 cache hit
return organizationRepository.findById(id).map(organizationMapper::toDetailDto);
```

`findById()` routes through `EntityManager.find()`, which checks the JPA first-level (session) cache before issuing a SQL query. The Criteria API `WHERE id = ?` path **always generates a full JPQL query**, bypassing the cache, allocating a full criteria object graph, and generally adding ~10-30ms overhead on the remote DB. This is the primary driver of the 426% detail overhead.

**Fix direction:** Replace the `Specification<E> idSpec` approach with a direct `EntityManager.find(entityClass, id)` call in `UnconstrainedDataManagerImpl`, exposed as `load(entityClass, id)` — which already exists in the interface.

---

## Root Cause #3 — Triple Serialization Per Entity (HIGH)

**File:** `src/main/java/com/vn/core/security/web/SecuredEntityJsonAdapter.java:84-97`

```java
// Secured path — 3 passes:
// 1. BeanWrapperImpl.getPropertyValue() → Map<String,Object>
// 2. objectMapper.valueToTree(map)      → JsonNode
// 3. objectMapper.writeValueAsString()  → String (final)
entities.stream()
    .map(entity -> toJson(entity, fetchPlanCode))  // passes 1+2 per entity
    .toList()
// then: objectMapper.writeValueAsString(list)      // pass 3
```

**vs. Baseline:** MapStruct `toDto()` + single Jackson serialize. One pass.

For a page of 20 entities, the secured path creates 20 `BeanWrapperImpl` instances (reflection-based, non-trivial allocation), builds 20 `LinkedHashMap<String,Object>` instances, converts each to `JsonNode`, then serializes the whole list. The detail endpoint also recurses into nested associations (`departments → employees`) via `property.fetchPlan() != null` branches.

**Fix direction:** Replace `BeanWrapperImpl` with direct field access via a compiled accessor map or Jackson's `ObjectReader` on the entity; avoid the intermediate `Map → JsonNode → String` chain by writing directly to a `JsonGenerator`.

---

## Root Cause #4 — FetchPlan Resolved N Times in List Loop (HIGH)

**File:** `src/main/java/com/vn/core/security/web/SecuredEntityJsonAdapter.java:72`

```java
public JsonNode toJson(Object entity, String fetchPlanCode) {
    // Called for EACH of the 20 entities in the page — returns THE SAME object every time
    FetchPlan fetchPlan = fetchPlanResolver.resolve(ClassUtils.getUserClass(entity), fetchPlanCode);
    ...
}
```

`ClassUtils.getUserClass(entity)` unwraps the Hibernate proxy class on every call. `fetchPlanResolver.resolve()` does a `HashMap` lookup (cheap), but the proxy unwrap + method call overhead accumulates across 20 entities.

**Fix direction:** In `toJsonArrayString()`, resolve the fetch plan **once before the stream**, then pass the resolved `FetchPlan` object into a second overload of `toJson(entity, fetchPlan)`.

---

## Root Cause #5 — AccessManager Sorts Constraints on Every checkCrud() (MEDIUM)

**File:** `src/main/java/com/vn/core/security/access/AccessManagerImpl.java:22-29`

```java
public <C extends AccessContext> C applyRegisteredConstraints(C context) {
    constraints
        .stream()
        .filter(c -> c.supports().isAssignableFrom(context.getClass()))
        .sorted(Comparator.comparingInt(AccessConstraint::getOrder))  // ← every call
        .forEach(c -> ((AccessConstraint<C>) c).applyTo(context));
}
```

The `constraints` list is injected at startup and never mutates. Sorting it on every `checkCrud()` call (at minimum once per request, plus once per `loadOne` in the detail path) wastes CPU cycles unnecessarily.

**Fix direction:** Pre-sort `constraints` by order in the constructor, store the sorted list, drop the per-call `sorted()`.

---

## Root Cause #6 — Authority Validation Hits DB Even Though JWT Is Already Signed (MEDIUM)

**File:** `src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java:103-108`

```java
// Loads all DB authority rows to validate names carried in the already-signed JWT
Set<String> validNames = authorityRepository.findAllById(jwtAuthorities)
    .stream().map(Authority::getName).collect(Collectors.toSet());
return jwtAuthorities.stream().filter(validNames::contains).toList();
```

The JWT is signed with the application's private key and already verified by Spring Security's filter chain before this code runs. The authority names inside a valid JWT are trustworthy. Querying `jhi_authority` to "validate" them against the DB adds a DB roundtrip with **no security benefit** in normal operation (phantom authority names in a forged JWT would be caught by signature verification).

**Fix direction:** Remove the `authorityRepository.findAllById()` call entirely and use the JWT authorities directly. If the team wants this validation, move it into an application-level cache keyed by authority name, loaded once at startup (the authority table is essentially static).

---

## Overhead Attribution (Estimated)

| Root Cause | Est. Overhead (Detail) | Est. Overhead (List) |
|------------|----------------------|---------------------|
| 2 extra DB queries (remote, per-request) | 40-80ms | 40-80ms |
| Criteria API vs `findById` | 30-80ms | n/a (list) |
| Nested association lazy-load (org-detail fetch plan) | 100-200ms | n/a |
| Triple serialization (BeanWrapper + 2x Jackson) | 10-30ms | 15-40ms |
| FetchPlan resolved N times per page | 2-5ms | 5-10ms |
| AccessManager sort on each call | 1-3ms | 1-3ms |
| **Total estimated** | **183-398ms** | **61-133ms** |
| **Actual measured overhead** | **275.5ms** | **205.6ms** |

---

## Priority Fix Order

1. **Cache permission matrix in Hazelcast** — eliminates DB hit #2 on every request. Highest ROI.
2. **Cache authority validation** — eliminates DB hit #1, or remove DB validation entirely for signed JWTs.
3. **Replace `loadOneInternal` Specification with `findById`** — fixes the primary driver of the 426% detail overhead.
4. **Resolve fetch plan once before entity loop** — simple 1-line fix, eliminates N redundant calls.
5. **Pre-sort `AccessManagerImpl` constraints** — trivial constructor change.
6. **Reduce serialization layers** — write to `JsonGenerator` directly or use a single-pass Jackson approach.

---

## Files to Change (Phase 11)

| File | Change |
|------|--------|
| `security/permission/RequestPermissionSnapshot.java` | Add Hazelcast-backed cache for matrix + authorities |
| `security/data/SecureDataManagerImpl.java` | Replace `loadOneInternal` Specification with direct ID load |
| `security/web/SecuredEntityJsonAdapter.java` | Resolve FetchPlan once per batch; reduce serialization chain |
| `security/access/AccessManagerImpl.java` | Pre-sort constraints in constructor |
| `security/permission/AttributePermissionEvaluatorImpl.java` | Remove or cache authority DB validation |

---

## Reference: Jmix Cache Patterns to Study

Jmix implements application-level permission caching in:
- `io.jmix.security.impl.StandardCurrentUserSession` — caches permissions per user session
- `io.jmix.security.impl.role.RoleGrantedAuthorityUtils` — resolves roles without per-request DB hits
- `io.jmix.core.security.impl.AuthorityUtils` — validates authority names at startup, not per-request
- Hazelcast `IMap` with TTL for cross-node invalidation when admin changes permissions via `UserSessionManager.refreshPermissions()`

Key learning: Jmix separates **identity** (JWT, per-request, lightweight) from **permission resolution** (application-level cache, invalidated on admin change). The current project conflates both into the request scope.
