---
phase: 11-security-pipeline-performance-hardening-p95-overhead-under-10-percent
verified: 2026-03-31T17:00:00Z
status: gaps_found
score: 5/6 must-haves verified
gaps:
  - truth: "Phase 11 produces a persisted before/after benchmark comparison proving whether secured p95 overhead is under 10% against the Phase 10 baseline, satisfying PERF-04."
    status: failed
    reason: "The benchmark-run-summary-2026-03-31.md file was updated with a Phase 11 comparison section, but the Phase 11 post-hardening numbers were never collected. The Phase 11 KPI table shows _pending_ values for secured p95 latency, p95 overhead, and the PERF-04 pass/fail verdict. The application server was unavailable during plan execution. The REQUIREMENTS.md traceability table still shows PERF-04 as 'Planned', and the requirement description includes 'persisting before-or-after benchmark proof' as a mandatory deliverable. No actual measurement exists to confirm p95 overhead is under 10%."
    artifacts:
      - path: "load-tests/results/benchmark-run-summary-2026-03-31.md"
        issue: "Section 9 PERF-04 KPI table has _pending_ values. Phase 11 secured p95 latency and overhead are not measured."
    missing:
      - "A live k6 benchmark run against the hardened binary using the existing Phase 10 scripts (org-list-benchmark.js and org-detail-benchmark.js)"
      - "Actual measured Phase 11 secured p95 latency values for list and detail endpoints"
      - "An explicit PERF-04 pass/fail statement based on real measurements, not expected-impact analysis"
      - "Update REQUIREMENTS.md traceability table: change PERF-04 from 'Planned' to 'Complete' once confirmed"
human_verification:
  - test: "Run k6 benchmark scripts against hardened binary and record results"
    expected: "The Phase 11 secured p95 overhead for organization list and detail is below 10% of the Phase 10 baseline (baseline list: 291.5ms, baseline detail: 64.6ms)"
    why_human: "Requires a running application server plus k6 tool. Cannot be verified by static code analysis. Commands documented in benchmark-run-summary-2026-03-31.md Section 9."
---

# Phase 11: Security Pipeline Performance Hardening Verification Report

**Phase Goal:** Reduce the secured endpoint p95 latency overhead to under 10% of the baseline by eliminating redundant DB round-trips in permission evaluation, fixing the Criteria API ID lookup inefficiency on detail loads, reducing per-response fetch-plan and serializer overhead, and proving the outcome with a persisted Phase 10 benchmark rerun.
**Verified:** 2026-03-31T17:00:00Z
**Status:** gaps_found
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Secured requests stop reloading the permission matrix from PostgreSQL on every HTTP request because the same JWT authority set reuses a shared cached matrix per D-01 and D-04. | VERIFIED | `RequestPermissionSnapshot.getMatrix()` checks Hazelcast `IMap.computeIfAbsent` keyed by sorted authority names. DB query only on cache miss. |
| 2 | JWT authority names are trusted directly with no `jhi_authority` database validation on the hot path per D-05 and D-06. | VERIFIED | `loadAuthorities()` reads from `SecurityContextHolder` only. No `AuthorityRepository` import or usage in the snapshot class. |
| 3 | Any SecPermission create, update, or delete makes the next secured request observe the new permission state via write-path eviction per D-02 and D-03. | VERIFIED | `SecPermissionService` carries `@CacheEvict(allEntries=true)` on save, update, deleteAll, deleteById, and deleteAllByAuthorityName. `SecPermissionAdminResource` delegates all writes to the service. |
| 4 | Organization detail reads no longer pay the Criteria API id-lookup penalty because secured single-entity loads use the direct id path per D-07. | VERIFIED | `loadOneInternal` calls `dataManager.checkCrud(entityClass, EntityOp.READ)` then `dataManager.unconstrained().load(entityClass, id)`. No Specification constructed for READ path. UPDATE and DELETE paths retain id-Specification as designed. |
| 5 | List and detail serialization keep the same attribute-filtered JSON contract while resolving the fetch plan once per response and replacing BeanWrapper property access with Jackson ObjectReader per D-08 and D-09. | VERIFIED | `SecuredEntityJsonAdapter.toJsonArrayString` resolves `FetchPlan` once before the entity loop. `SecureEntitySerializerImpl` converts to `ObjectNode` once per entity for scalar reads; `BeanWrapperImpl` retained for association traversal only. |
| 6 | Phase 11 produces a persisted before/after benchmark comparison proving whether secured p95 overhead is under 10% against the Phase 10 baseline, satisfying PERF-04. | FAILED | `benchmark-run-summary-2026-03-31.md` Section 9 PERF-04 KPI table shows `_pending_` for Phase 11 secured p95, overhead %, and pass/fail verdict. Application server was unavailable during plan execution; benchmark was not run. |

**Score:** 5/6 truths verified

---

### Required Artifacts

#### Plan 11-01 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java` | Request-scoped access point backed by a cross-request Hazelcast PermissionMatrix cache keyed by JWT authority names | VERIFIED | Contains `PermissionMatrix`, Hazelcast IMap lookup, `toCacheKey()` using TreeSet, no `AuthorityRepository` on hot path. |
| `src/main/java/com/vn/core/service/security/SecPermissionService.java` | Service-layer SecPermission write path with cache eviction on create, update, and delete | VERIFIED | Contains `@CacheEvict` on all five write methods. Service is the single eviction seam. |
| `src/main/java/com/vn/core/config/CacheConfiguration.java` | Named Hazelcast cache configuration for permission-matrix reuse | VERIFIED | Contains `MapConfig` via `initializePermissionMatrixMapConfig` registered as `sec-permission-matrix` with 3600s TTL safety ceiling. |

#### Plan 11-02 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java` | Direct-id secured read path without id Specification overhead | VERIFIED | `loadOneInternal` uses `dataManager.unconstrained().load(entityClass, id)` after `checkCrud`. No id-Specification for READ path. |
| `src/main/java/com/vn/core/security/web/SecuredEntityJsonAdapter.java` | Single fetch-plan resolution per list/detail serialization call | VERIFIED | Contains `FetchPlan` — `toJson` resolves once per detail call; `toJsonArrayString` resolves once before entity loop. |
| `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java` | Jackson ObjectReader-based property access preserving attribute filtering | VERIFIED (hybrid) | Uses `ObjectNode` (not `ObjectReader` as originally specified) for scalar reads — an accepted deviation documented in 11-02-SUMMARY. BeanWrapper retained for associations. Attribute filtering preserved. |
| `load-tests/results/benchmark-run-summary-2026-03-31.md` | Persisted Phase 11 before/after benchmark comparison against the Phase 10 baseline | FAILED | File exists and contains Phase 11 section, but KPI table shows `_pending_` values. No actual Phase 11 run data exists. |

---

### Key Link Verification

#### Plan 11-01 Key Links

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `RolePermissionServiceDbImpl.java` | `RequestPermissionSnapshot.java` | `requestPermissionSnapshot.getMatrix()` | WIRED | Line 42: `PermissionMatrix matrix = requestPermissionSnapshot.getMatrix();` inside `isRequestScopeActive()` guard. |
| `SecPermissionAdminResource.java` | `SecPermissionService.java` | service-layer SecPermission writes | WIRED | Resource injects `SecPermissionService` and delegates all writes (save, update, deleteAll, deleteById). No direct `secPermissionRepository` write calls for permission mutations. |
| `SecPermissionService.java` | `CacheConfiguration.java` | `@CacheEvict` against named permission cache | WIRED | `@CacheEvict(cacheNames = RequestPermissionSnapshot.PERMISSION_MATRIX_CACHE, allEntries = true)` on all write methods. `PERMISSION_MATRIX_CACHE = "sec-permission-matrix"` matches the MapConfig name in `CacheConfiguration`. |

#### Plan 11-02 Key Links

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `OrganizationResource.java` | `SecuredEntityJsonAdapter.java` | detail/list JSON response generation | WIRED | Lines 109, 146, 188, 226, 264, 304 use `securedEntityJsonAdapter.toJson`/`toJsonArrayString`/`toJsonString`. |
| `SecuredEntityJsonAdapter.java` | `SecureEntitySerializerImpl.java` | resolved FetchPlan passed into serializer path | WIRED | `toJsonWithPlan(entity, fetchPlan)` calls `secureEntitySerializer.serialize(entity, fetchPlan)`. FetchPlan resolved once then passed in. |
| `load-tests/scripts/org-list-benchmark.js` | `load-tests/results/benchmark-run-summary-2026-03-31.md` | persisted benchmark rerun summary | PARTIAL | Phase 11 comparison section added but contains `_pending_` values — benchmark script was not executed. |

---

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `RequestPermissionSnapshot.java` | `cachedMatrix` | Hazelcast IMap backed by `secPermissionRepository.findAllByAuthorityNameIn()` on cache miss | Yes — real DB query on cold cache | FLOWING |
| `SecureEntitySerializerImpl.java` | `entityNode` (scalar reads) | `objectMapper.convertValue(entity, ObjectNode.class)` — entity from JPA | Yes — real entity data | FLOWING |
| `SecuredEntityJsonAdapter.java` | `fetchPlan` | `fetchPlanResolver.resolve()` — YAML-backed resolver | Yes — resolved once per response | FLOWING |

---

### Behavioral Spot-Checks

Step 7b: SKIPPED — application server not running; live HTTP verification not possible. Unit and integration test results documented in SUMMARYs are the best available evidence.

| Behavior | Notes | Status |
|----------|-------|--------|
| `RequestPermissionSnapshotTest` — JWT trust + cache reuse | 11 tests present in codebase; verified by SUMMARY | PASS (unit) |
| `SecPermissionServiceTest` — service-layer delegation and eviction coverage | 7 tests present; verified by SUMMARY | PASS (unit) |
| `AccessManagerImplTest` — constructor-time sort and order preservation | 5 tests present; verified by SUMMARY | PASS (unit) |
| `SecuredEntityJsonAdapterTest` — single fetch-plan resolution for detail and list | 6 tests present; verified by SUMMARY | PASS (unit) |
| `SecureEntitySerializerImplTest` — attribute filtering, D-15/D-16 contract | 6 tests present; verified by SUMMARY | PASS (unit) |
| `SecuredEntityEnforcementIT` — integration test with Docker/Testcontainers | Skipped in execution environment (postgres:18.3 not pullable) | SKIP |
| Phase 11 k6 benchmark run | Application server unavailable | FAIL — no data |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| PERF-04 | 11-01-PLAN.md, 11-02-PLAN.md | Secured `@SecuredEntity` pipeline keeps p95 overhead under 10% by eliminating redundant permission-evaluation round-trips, replacing detail-load id-spec, reducing per-response fetch-plan/serializer overhead, and persisting before/after benchmark proof. | PARTIAL | Code-path fixes all implemented and verified (5/6 truths). Benchmark proof mandatory per requirement text is absent — "persisting before-or-after benchmark proof" not satisfied. REQUIREMENTS.md traceability table shows "Planned"; checkbox shows `[x]` — inconsistency to resolve on completion. |

**PERF-04 status note:** The requirement definition explicitly includes "persisting before-or-after benchmark proof" as a deliverable. The code optimizations are in place, but the proof artifact has `_pending_` values. PERF-04 cannot be confirmed complete until the benchmark run is executed and the numbers fill the pending table.

**Orphaned requirements check:** No orphaned requirements. Only PERF-04 maps to Phase 11 in the traceability table.

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `load-tests/results/benchmark-run-summary-2026-03-31.md` | 166-167 | `_pending_` values in PERF-04 KPI table | Blocker | PERF-04 cannot be confirmed without actual measurements. |
| `.planning/REQUIREMENTS.md` | 44 vs 101 | PERF-04 checkbox `[x]` contradicts traceability "Planned" status | Warning | Inconsistency — the requirement is only partially satisfied. Traceability table is more accurate. |

No code-level stubs, TODOs, placeholder returns, or empty implementations found in any of the seven modified source files.

---

### Human Verification Required

#### 1. Phase 11 k6 Benchmark Execution

**Test:** Start the application using the standardized profile documented in the benchmark summary, then run both k6 scripts:
```bash
./gradlew bootRun --args="--spring.profiles.active=dev,api-docs --logging.level.ROOT=INFO --logging.level.org.hibernate.SQL=WARN --logging.level.tech.jhipster=INFO --logging.level.com.vn.core=INFO --spring.datasource.hikari.maximum-pool-size=60 --spring.datasource.hikari.minimum-idle=10 --spring.jpa.properties.hibernate.generate_statistics=false"

k6 run load-tests/scripts/org-list-benchmark.js
k6 run --env ORG_ID=1501 load-tests/scripts/org-detail-benchmark.js
```

**Expected:** Phase 11 secured p95 latency is measurably lower than Phase 10 (list: was 497.1ms; detail: was 340.1ms). PERF-04 passes if secured p95 / baseline p95 - 1 < 10% for both endpoints. Based on root-cause analysis the dominant DB costs (D-01/D-04/D-05/D-06/D-07) should yield substantial improvement, but actual measurement is required.

**Why human:** Requires a running application server with a live PostgreSQL database, k6 v1.7.1 installed, and the valid organization id (1501). Cannot be exercised by static code inspection.

**After running:** Update `load-tests/results/benchmark-run-summary-2026-03-31.md` Section 9 PERF-04 KPI table with actual values, then update `.planning/REQUIREMENTS.md` traceability table from "Planned" to "Complete" (or document why the KPI was not met).

---

### Gaps Summary

**One gap blocks full goal achievement:** the benchmark proof is missing.

All six code-path optimizations planned for Phase 11 are implemented and substantively correct:

- D-01/D-04: Cross-request Hazelcast PermissionMatrix cache keyed by sorted JWT authority-name set — `RequestPermissionSnapshot.getMatrix()` is backed by `computeIfAbsent` on the named Hazelcast map.
- D-05/D-06: JWT authority names trusted directly — no `AuthorityRepository` on the hot path; authorities read from `SecurityContextHolder` only.
- D-02/D-03: Write-path eviction — `SecPermissionService` owns `@CacheEvict` on all five write methods; `SecPermissionAdminResource` is transport-only.
- D-07: Direct id load for READ — `loadOneInternal` uses `unconstrained().load()` bypassing Criteria API id-Specification.
- D-08: Single fetch-plan resolution per response — `toJsonArrayString` resolves `FetchPlan` once before the entity loop.
- D-09: Jackson `ObjectNode` scalar reads — `SecureEntitySerializerImpl` converts entity to `ObjectNode` once per serialize call; `BeanWrapperImpl` retained for typed association references only.
- D-11: Constructor-time constraint sort — `AccessManagerImpl` sorts constraints once at startup.

However, the PERF-04 requirement definition explicitly states "persisting before-or-after benchmark proof" as a mandatory deliverable alongside the code changes. The benchmark summary section is structured and ready; only the actual k6 run is missing. The REQUIREMENTS.md traceability table correctly reflects "Planned" rather than "Complete."

The gap is operational (server must be running) not a code defect. Once the benchmark is run and the KPI table is populated, this phase is complete.

---

_Verified: 2026-03-31T17:00:00Z_
_Verifier: Claude (gsd-verifier)_
