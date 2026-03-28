---
phase: 09-enterprise-ux-and-performance-hardening
plan: "01"
subsystem: security
tags: [spring, permission-caching, request-scope, performance, permission-matrix]

requires:
  - phase: 08.3-user-registration-live-permission-refresh-entity-native-serialization-validation-hardening-and-row-policy-removal
    provides: request-time authority refresh, typed secured entity flow

provides:
  - PermissionMatrix extracted as package-level class in com.vn.core.security.permission
  - RequestPermissionSnapshot request-scoped Spring bean that caches authority validation and permission matrix per request
  - MergedSecurityContextBridge delegates getCurrentUserAuthorities() to snapshot in request context
  - RolePermissionServiceDbImpl uses snapshot matrix for isEntityOpPermitted() in request context
  - AttributePermissionEvaluatorImpl uses snapshot matrix for attribute checks in request context

affects:
  - phase-10-frontend-reliability-and-regression-coverage

tech-stack:
  added: []
  patterns:
    - "Request-scoped Spring bean (ScopedProxyMode.TARGET_CLASS) caches DB lookups within HTTP request lifecycle"
    - "isRequestScopeActive() guard pattern for graceful non-web context fallback"
    - "PermissionMatrix immutable value object built from single bulk query, reused across all permission checks"

key-files:
  created:
    - src/main/java/com/vn/core/security/permission/PermissionMatrix.java
    - src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java
  modified:
    - src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java
    - src/main/java/com/vn/core/security/bridge/MergedSecurityContextBridge.java
    - src/main/java/com/vn/core/security/permission/RolePermissionServiceDbImpl.java
    - src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java
    - src/test/java/com/vn/core/security/bridge/MergedSecurityContextBridgeTest.java

key-decisions:
  - "Request-local caching only (D-01, D-02): snapshot destroyed at request end; no cross-request or session caching"
  - "Reuse existing PermissionMatrix pattern from SecuredEntityCapabilityService rather than building parallel aggregation (D-03)"
  - "SecuredEntityCapabilityService builds its own matrix independently since it already bulk-loads once per capability request"
  - "isRequestScopeActive() guard ensures non-web callers (tests, batch) fall back gracefully to direct repository queries"

patterns-established:
  - "Request-scoped snapshot: build once per request, answer all subsequent checks from in-memory matrix"
  - "Static isRequestScopeActive() guard on RequestContextHolder for safe non-web degradation"

requirements-completed: [PERF-01, PERF-02]

duration: 7min
completed: 2026-03-28
---

# Phase 9 Plan 01: Backend Permission Caching Summary

**Request-scoped PermissionMatrix eliminates N+1 authority and permission DB queries on secured entity list operations by caching authority validation and bulk permission loading once per HTTP request.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-28T07:36:29Z
- **Completed:** 2026-03-28T07:43:00Z
- **Tasks:** 3 completed
- **Files modified:** 7

## Accomplishments

### Task 1: Extract PermissionMatrix + Create RequestPermissionSnapshot

Extracted the private inner `PermissionMatrix` class from `SecuredEntityCapabilityService` into a public package-level class at `com.vn.core.security.permission.PermissionMatrix`. The extracted class preserves the same `allowedKeys` Set implementation, `isEntityPermitted`/`isAttributePermitted` methods, and `EMPTY` static constant.

Created `RequestPermissionSnapshot` as a `@Component @Scope("request")` Spring bean with `ScopedProxyMode.TARGET_CLASS`. The bean:
- Injects `AuthorityRepository` and `SecPermissionRepository` directly (no circular dependency)
- Caches validated authority names (`getAuthorities()`) and permission matrix (`getMatrix()`) as null-initialized lazily loaded fields
- Authority loading replicates the phantom-filtering logic from `MergedSecurityContextBridge` (validate JWT claims against `jhi_authority` table)
- Matrix loading calls `secPermissionRepository.findAllByAuthorityNameIn(authorities)` once and builds a `PermissionMatrix`
- Provides `static isRequestScopeActive()` guard via `RequestContextHolder.getRequestAttributes() != null`

### Task 2: Wire Snapshot into Hot-Path Callers

Updated three hot-path classes to use the request snapshot when active:

**MergedSecurityContextBridge**: In `getCurrentUserAuthorities()`, delegates to `requestPermissionSnapshot.getAuthorities()` when request scope is active. Falls back to direct `authorityRepository.findAllById()` filtering logic for non-web contexts.

**RolePermissionServiceDbImpl**: In `isEntityOpPermitted()`, uses `requestPermissionSnapshot.getMatrix().isEntityPermitted(target, op.name())` when request scope is active. Falls back to `mergedSecurityService.getCurrentUserAuthorityNames()` + `secPermissionRepository.findByRolesAndTarget()` for non-web contexts. The general-purpose `hasPermission(Collection, TargetType, String, String)` method remains unchanged.

**AttributePermissionEvaluatorImpl**: In `checkAttributePermission()`, uses `requestPermissionSnapshot.getMatrix().isAttributePermitted(specificTarget, action)` when request scope is active. Falls back to the original authority + multi-target repository query for non-web contexts.

### Task 3: Verification

- `./gradlew test` — BUILD SUCCESSFUL (40s)
- `./gradlew integrationTest` — BUILD SUCCESSFUL (3m 31s)
- `SecuredEntityEnforcementIT` has 22 passing test cases in XML results
- No `ScopeNotActiveException` or scope-related errors in test output

## Commits

| Hash | Message |
|------|---------|
| 663aafb | feat(09-01): extract PermissionMatrix and create RequestPermissionSnapshot |
| 164779e | feat(09-01): wire RequestPermissionSnapshot into permission hot-paths |
| ba80cb9 | fix(09-01): update MergedSecurityContextBridgeTest for new constructor signature |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed MergedSecurityContextBridgeTest constructor mismatch**
- **Found during:** Task 3 (verification)
- **Issue:** `MergedSecurityContextBridgeTest` instantiated `MergedSecurityContextBridge` with only `AuthorityRepository` after Task 2 added `RequestPermissionSnapshot` as a second constructor parameter, causing `compileTestJava` to fail.
- **Fix:** Added `@Mock RequestPermissionSnapshot requestPermissionSnapshot` field and updated `new MergedSecurityContextBridge(authorityRepository, requestPermissionSnapshot)` call. Since tests run without an active request context, `isRequestScopeActive()` returns false and the fallback path (which the tests verify) runs unchanged.
- **Files modified:** `src/test/java/com/vn/core/security/bridge/MergedSecurityContextBridgeTest.java`
- **Commit:** ba80cb9

## Known Stubs

None — all permission caching logic is fully wired through the request lifecycle.

## Self-Check: PASSED

Files created exist:
- FOUND: src/main/java/com/vn/core/security/permission/PermissionMatrix.java
- FOUND: src/main/java/com/vn/core/security/permission/RequestPermissionSnapshot.java

Commits exist:
- FOUND: 663aafb
- FOUND: 164779e
- FOUND: ba80cb9
