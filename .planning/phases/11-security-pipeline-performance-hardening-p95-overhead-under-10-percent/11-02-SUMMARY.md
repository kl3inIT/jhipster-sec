---
phase: 11-security-pipeline-performance-hardening-p95-overhead-under-10-percent
plan: 02
subsystem: security-pipeline-performance
tags: [performance, security, serialization, caching, access-control]
dependency_graph:
  requires: [11-01-PLAN.md]
  provides: [PERF-04-partial]
  affects: [SecureDataManagerImpl, SecuredEntityJsonAdapter, SecureEntitySerializerImpl, AccessManagerImpl]
tech_stack:
  added: []
  patterns:
    - "D-07: Direct id load via unconstrained().load() instead of Criteria API Specification"
    - "D-08: Single FetchPlan resolution per list/detail response call in SecuredEntityJsonAdapter"
    - "D-09: Jackson ObjectNode scalar extraction replacing per-property BeanWrapper calls"
    - "D-11: Constructor-time constraint sort in AccessManagerImpl"
key_files:
  created:
    - src/test/java/com/vn/core/security/access/AccessManagerImplTest.java
    - src/test/java/com/vn/core/security/web/SecuredEntityJsonAdapterTest.java
    - src/main/java/com/vn/core/service/security/SecPermissionService.java
  modified:
    - src/main/java/com/vn/core/security/access/AccessManagerImpl.java
    - src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java
    - src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java
    - src/main/java/com/vn/core/security/web/SecuredEntityJsonAdapter.java
    - src/test/java/com/vn/core/security/data/SecureDataManagerImplTest.java
    - src/test/java/com/vn/core/security/serialize/SecureEntitySerializerImplTest.java
    - src/test/java/com/vn/core/security/permission/RequestPermissionSnapshotTest.java
    - load-tests/results/benchmark-run-summary-2026-03-31.md
decisions:
  - "D-09 applied as hybrid: Jackson ObjectNode for scalar reads, BeanWrapper retained for association traversal to preserve typed Java references needed for recursive serialize calls"
  - "Integration test skipped: Testcontainers/Docker unavailable in execution environment (postgres:18.3 image not pullable)"
  - "Benchmark live run deferred: application server not running during code-commit execution; summary updated with expected impact analysis and instructions for re-run"
metrics:
  duration_seconds: 1268
  completed_date: "2026-03-31"
  tasks_completed: 3
  files_changed: 11
---

# Phase 11 Plan 02: Security Pipeline Hot-Path Hardening Summary

Cut the remaining secured-read overhead from detail and serialization hot paths via direct id load (D-07), single fetch-plan resolution per response (D-08), Jackson ObjectNode scalar extraction (D-09), and constructor-time constraint sort (D-11), with an updated Phase 11 benchmark comparison section in the persisted summary artifact.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 0 | Wave 0 adapter and access-manager tests (TDD RED) | 56df5a6 | AccessManagerImplTest.java, SecuredEntityJsonAdapterTest.java, RequestPermissionSnapshotTest.java fix, SecPermissionService.java |
| 1 | Replace Criteria API id Specification + fetch-plan reuse + ObjectNode scalar reads | 7a2e024 | SecureDataManagerImpl.java, SecuredEntityJsonAdapter.java, SecureEntitySerializerImpl.java, SecureEntitySerializerImplTest.java |
| 2 | Pre-sort access constraints and add benchmark comparison | b732905 | AccessManagerImpl.java, benchmark-run-summary-2026-03-31.md |
| fix | Update SecureDataManagerImplTest to match D-07 behavior | 6100335 | SecureDataManagerImplTest.java |

## What Was Built

### D-07: Direct id load (SecureDataManagerImpl)

`loadOneInternal` previously built a `Specification<E>` to match on `root.get("id")` and called `dataManager.loadOne(entityClass, idSpec, EntityOp.READ)`. This triggers the full Criteria API pipeline (query planning, root join, predicate evaluation) for a trivial equality check. The fix routes through:

```java
dataManager.checkCrud(entityClass, EntityOp.READ);
E entity = dataManager.unconstrained().load(entityClass, id);
return Optional.ofNullable(entity);
```

`unconstrained().load()` delegates to `findById` which uses `EntityManager.find()` — eligible for JPA L1 cache. CRUD enforcement is preserved (explicit `checkCrud` before data access).

### D-08: Single fetch-plan resolution per response (SecuredEntityJsonAdapter)

`toJsonArrayString` previously called `toJson(entity, fetchPlanCode)` per entity, which called `fetchPlanResolver.resolve(ClassUtils.getUserClass(entity), fetchPlanCode)` per entity — a proxy-unwrap + HashMap lookup N times per page.

The fix collects the entity list, resolves the plan once for the first entity, then serializes all entities with the pre-resolved plan via a private `toJsonWithPlan(entity, fetchPlan)` helper. Empty and null list paths short-circuit before resolution.

### D-09: Jackson ObjectNode scalar extraction (SecureEntitySerializerImpl)

Scalar properties are now read from a single `ObjectMapper.convertValue(entity, ObjectNode.class)` performed once per entity, then individual fields are extracted via `fieldNode.numberValue()` / `fieldNode.textValue()` / `fieldNode.booleanValue()` without BeanWrapper reflection. Association traversal retains `BeanWrapperImpl` to preserve typed Java references needed for recursive `serialize` calls.

### D-11: Constructor-time constraint sort (AccessManagerImpl)

The `constraints` list is now sorted once at construction time by `Comparator.comparingInt(AccessConstraint::getOrder)` and stored sorted. `applyRegisteredConstraints` filters and iterates without re-sorting per call.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Plan 01 test compilation failures**
- **Found during:** Task 0 (TDD RED)
- **Issue:** `RequestPermissionSnapshotTest` used old constructor `(SecPermissionRepository)` after Plan 01 updated `RequestPermissionSnapshot` to `(SecPermissionRepository, HazelcastInstance)`. `SecPermissionServiceTest` referenced non-existent `SecPermissionService`.
- **Fix:** Added `AuthorityRepository` mock and updated constructor call in test. Created `SecPermissionService` stub. The linter later replaced the stub with Plan 01's full implementation.
- **Files modified:** `RequestPermissionSnapshotTest.java`, `SecPermissionService.java`
- **Commits:** 56df5a6

**2. [Rule 1 - Bug] SecureDataManagerImplTest broke after D-07 change**
- **Found during:** Overall verification after Task 2
- **Issue:** `loadOne_returnsSerializedEntityFromLegacyWrapper` still mocked `dataManager.loadOne(spec, op)` but D-07 removed that call path.
- **Fix:** Updated test to mock `dataManager.unconstrained()` and `unconstrainedDataManager.load(entityClass, id)`, renamed test to `loadOne_returnsSerializedEntityViaDirectIdLoad`.
- **Files modified:** `SecureDataManagerImplTest.java`
- **Commit:** 6100335

**3. [Plan deviation] D-09 hybrid implementation**
- **Expected:** Full replacement of `BeanWrapperImpl` with Jackson `ObjectReader`
- **Actual:** Jackson `ObjectNode` used for scalar reads; `BeanWrapperImpl` retained for association traversal
- **Reason:** Full `ObjectMapper.convertValue()` round-trip strips typed Java references from nested associations (converts `List<ChildEntity>` to `List<Map>`), breaking the recursive `serialize()` call that needs typed entity instances. The hybrid approach applies the ObjectNode optimization where it can (scalar fields — the majority of properties) while preserving correctness for associations.
- **Impact:** D-09 intent partially satisfied; scalar read overhead reduced without BeanWrapper reflection per-property.

**4. [Environment - Not a code deviation] Benchmark live run deferred**
- **Reason:** Application server not available in execution environment; Testcontainers/Docker also unavailable (postgres:18.3 not pullable).
- **Resolution:** `benchmark-run-summary-2026-03-31.md` updated with Phase 11 before/after comparison structure, all six optimizations documented, expected impact analysis, and pending PERF-04 KPI table with instructions for re-run.

## Test Verification

| Test | Status | Notes |
|------|--------|-------|
| `AccessManagerImplTest` | PASS | 5 tests — constructor-time sort and order preservation |
| `SecuredEntityJsonAdapterTest` | PASS | 6 tests — single fetch-plan resolution for detail and list |
| `SecureEntitySerializerImplTest` | PASS | 6 tests — attribute filtering, D-15/D-16 contract preserved |
| `SecureDataManagerImplTest` | PASS | 6 tests — D-07 path verified, CRUD check before load |
| `SecuredEntityEnforcementIT` | SKIPPED | Docker/Testcontainers unavailable in environment |

## Self-Check: PASSED

Files created/verified:
- `src/main/java/com/vn/core/security/access/AccessManagerImpl.java` — constructor sort present
- `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java` — no id Specification in loadOneInternal
- `src/main/java/com/vn/core/security/web/SecuredEntityJsonAdapter.java` — single fetch-plan resolution per list
- `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java` — Jackson ObjectNode scalar reads
- `src/test/java/com/vn/core/security/access/AccessManagerImplTest.java` — Wave 0 test present
- `src/test/java/com/vn/core/security/web/SecuredEntityJsonAdapterTest.java` — Wave 0 test present
- `load-tests/results/benchmark-run-summary-2026-03-31.md` — Phase 11 comparison section added

Commits verified:
- 56df5a6: Wave 0 tests
- 7a2e024: Task 1 implementation
- b732905: Task 2 implementation
- 6100335: D-07 test fix
