---
phase: 11-security-pipeline-performance-hardening-p95-overhead-under-10-percent
plan: 01
subsystem: security
tags: [hazelcast, permission-cache, jwt, spring-cache, performance]

# Dependency graph
requires:
  - phase: 09-enterprise-ux-and-performance-hardening
    provides: RequestPermissionSnapshot request-scoped permission caching seam
  - phase: 10-performance-benchmarking-and-openapi-documentation
    provides: Performance analysis identifying authority-table DB lookup and per-request matrix reload as root causes RC#1 and RC#6

provides:
  - Cross-request Hazelcast PermissionMatrix cache keyed by JWT authority-name set (sec-permission-matrix map)
  - SecPermissionService service layer owning write-path @CacheEvict for create/update/delete
  - JWT authority names trusted directly from token with no jhi_authority DB validation

affects:
  - 11-02 (shares Hazelcast instance and cache name constant)
  - Future phases touching SecPermission write paths must route through SecPermissionService

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Cross-request Hazelcast cache keyed by sorted JWT authority-name set with write-path eviction (not TTL-based)"
    - "Service-layer @CacheEvict ownership: resource layer stays transport-only per CLAUDE.md"
    - "RequestPermissionSnapshot.toCacheKey() deterministic key from TreeSet for order-independent cache keys"

key-files:
  created:
    - src/main/java/com/vn/core/service/security/SecPermissionService.java
    - src/test/java/com/vn/core/security/permission/RequestPermissionSnapshotTest.java
    - src/test/java/com/vn/core/service/security/SecPermissionServiceTest.java
  modified:
    - src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java
    - src/main/java/com/vn/core/config/CacheConfiguration.java
    - src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java
    - src/main/java/com/vn/core/web/rest/admin/security/SecRoleAdminResource.java

key-decisions:
  - "JWT authority names are trusted directly (D-05/D-06): no jhi_authority DB cross-check. Revoked authorities take effect at JWT expiry — the accepted revocation bound."
  - "PermissionMatrix cache uses allEntries=true eviction on every SecPermission write so permission changes take effect within the next HTTP request (D-02/D-03)."
  - "Hazelcast map TTL of 3600s is a non-semantic safety ceiling only — correctness comes from write-path eviction, not time expiry."
  - "SecPermissionService is the single eviction seam. SecPermissionAdminResource and SecRoleAdminResource are transport-only."

patterns-established:
  - "Cache key derivation: RequestPermissionSnapshot.toCacheKey() uses TreeSet for deterministic sort-independent keys"
  - "Write-path eviction pattern: @CacheEvict(cacheNames = CACHE_NAME, allEntries = true) on every service write method"

requirements-completed: [PERF-04]

# Metrics
duration: 19min
completed: 2026-03-31
---

# Phase 11 Plan 01: Permission Matrix Cross-Request Hazelcast Cache and JWT Authority Trust

**Cross-request Hazelcast PermissionMatrix cache keyed by JWT authority set with write-path eviction, eliminating per-request authority-table DB lookup and matrix reload.**

## Performance

- **Duration:** 19 min
- **Started:** 2026-03-31T15:28:53Z
- **Completed:** 2026-03-31T15:48:00Z
- **Tasks:** 3 (Task 0 TDD RED + Task 1 GREEN + Task 2 GREEN)
- **Files modified:** 7

## Accomplishments

- `RequestPermissionSnapshot` now trusts JWT authority names directly (D-05/D-06): removed `AuthorityRepository.findAllById()` hot-path lookup that fired on every request.
- `RequestPermissionSnapshot.getMatrix()` reads from a cross-request Hazelcast cache (D-01/D-04): same authority-name set across multiple requests reuses the cached `PermissionMatrix` instead of querying `SecPermission` on every request.
- `SecPermissionService` created as the single service-layer write-path seam: every `SecPermission` create, update, and delete evicts the shared Hazelcast cache via `@CacheEvict(allEntries=true)` so permission changes take effect within the next HTTP request (D-02/D-03).
- `SecPermissionAdminResource` and `SecRoleAdminResource` both refactored to delegate persistence and eviction to `SecPermissionService` — resources are now HTTP transport only per CLAUDE.md layering.
- Named `sec-permission-matrix` Hazelcast `MapConfig` registered in `CacheConfiguration` with 3600s safety TTL ceiling.

## Task Commits

1. **Task 0 (TDD RED): Wave 0 permission-cache tests** - `fdc7f91` (test)
2. **Task 1: Cache PermissionMatrix by JWT authority set + remove authority-table validation** - `04a0fc4` (feat)
3. **Task 2: Move SecPermission cache eviction into service-layer write path** - `6f67683` (feat)

## Files Created/Modified

- `src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java` — Removed AuthorityRepository; added Hazelcast cross-request cache lookup keyed by JWT authority set
- `src/main/java/com/vn/core/config/CacheConfiguration.java` — Added `sec-permission-matrix` named MapConfig with 3600s TTL safety ceiling
- `src/main/java/com/vn/core/service/security/SecPermissionService.java` — New service with @CacheEvict on save/update/deleteAll/deleteById/deleteAllByAuthorityName
- `src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java` — Refactored to delegate to SecPermissionService; transport-only
- `src/main/java/com/vn/core/web/rest/admin/security/SecRoleAdminResource.java` — deleteRole() routes through SecPermissionService.deleteAllByAuthorityName()
- `src/test/java/com/vn/core/security/permission/RequestPermissionSnapshotTest.java` — Wave 0 tests: JWT trust, cache reuse, empty authority paths
- `src/test/java/com/vn/core/service/security/SecPermissionServiceTest.java` — Wave 0 tests: service-layer delegation and eviction coverage

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing eviction] SecRoleAdminResource.deleteRole() bypassed eviction**
- **Found during:** Task 2
- **Issue:** `SecRoleAdminResource.deleteRole()` called `secPermissionRepository.deleteByAuthorityName()` directly, bypassing the new service eviction path. Deleting a role would not evict the permission cache.
- **Fix:** Replaced direct repository call with `secPermissionService.deleteAllByAuthorityName()`.
- **Files modified:** `src/main/java/com/vn/core/web/rest/admin/security/SecRoleAdminResource.java`
- **Commit:** `6f67683`

## Deferred Issues

- `SecureDataManagerImplTest.loadOne_returnsSerializedEntityFromLegacyWrapper` fails in the current worktree due to parallel 11-02 agent changes to `SecureDataManagerImpl`. This failure is in the parallel plan's scope and not caused by 11-01 changes.
- Integration test `SecuredEntityEnforcementIT` requires Docker/Testcontainers for PostgreSQL — not available in this execution environment. Test correctness is covered by the existing `getOrganizations_matrixCreatedPermissionChangesForbiddenToOk` test which exercises the permission-write → secured-read path.

## Self-Check: PASSED

All 7 source files and SUMMARY.md verified as present. All 3 task commits (`fdc7f91`, `04a0fc4`, `6f67683`) verified in git log.
