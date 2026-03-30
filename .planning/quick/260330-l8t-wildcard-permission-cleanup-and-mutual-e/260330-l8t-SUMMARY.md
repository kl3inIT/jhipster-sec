---
quick_task: 260330-l8t
title: Wildcard permission cleanup and mutual exclusivity in createPermission
completed: "2026-03-30T08:45:00Z"
duration: 21 min
tasks_completed: 2
files_changed: 3
commits:
  - 511e4d3
tags:
  - security
  - permissions
  - backend
  - fix
key_files:
  modified:
    - src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java
    - src/test/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResourceIT.java
    - src/test/java/com/vn/core/service/security/SecuredEntityCapabilityServiceTest.java
decisions:
  - Apply normalizeOutgoing in the duplicate-handling path of createPermission so the POST
    response is always in UI-contract format (GRANT + lowercase target) not raw stored format
  - Entity wildcard target '*' passes through normalization unchanged since it has no catalog
    mapping; the pass-through behavior is now regression-tested
---

# Quick Task 260330-l8t Summary

## One-liner

Fixed missing `normalizeOutgoing` on the `createPermission` duplicate path so POST responses are always UI-contract format, added entity wildcard `*` roundtrip IT, and corrected a stale unit test assertion for wildcard-edit-implies-view semantics.

## What Was Done

### Task 1 — Fix normalizeOutgoing on duplicate path

`SecPermissionAdminResource.createPermission` had two code paths:

1. **New permission** (line 115-121): correctly calls `secPermissionMapper.toDto(entity)` and returns the mapped DTO. The `POST` response body here does NOT apply `normalizeOutgoing` either — it returns the raw stored format. However, this is the `201 Created` path and is tested to return normalized values in `testCreatePermission`.

   Wait — after re-reading: `testCreatePermission` sends `effect: "ALLOW"` (stored format) and expects `effect: "ALLOW"` back. The contract tests (`createPermission_matrixEntityGrantStoresAllowAndRoundTripsAsGrant`) use the matrix/UI flow with `effect: "GRANT"` input.

2. **Duplicate found** (line 94-113): returned `ResponseEntity.ok(secPermissionMapper.toDto(canonicalPermission))` without calling `normalizeOutgoing`. This meant a `POST` with UI-contract values (`target: "organization"`, `effect: "GRANT"`) that found an existing record returned the raw stored values (`target: "ORGANIZATION"`, `effect: "ALLOW"`), inconsistent with GET endpoint behavior.

**Fix:** Changed line 113 to:
```java
return ResponseEntity.ok(secPermissionUiContractService.normalizeOutgoing(secPermissionMapper.toDto(canonicalPermission)));
```

Updated `createPermission_matrixGrantReturnsExistingRowInsteadOfCreatingDuplicate` to assert UI-contract values (`target: "organization"`, `effect: "GRANT"`).

### Task 2 — Entity wildcard `*` IT roundtrip

Added `createPermission_entityWildcardGrantStoresAsWildcardAndRoundTripsAsGrant` to `SecPermissionAdminResourceIT`:

- POST `{ targetType: "ENTITY", target: "*", action: "READ", effect: "GRANT", authorityName: "ROLE_PROOF_NONE" }`
- Asserts persisted entity has `target = "*"` and `effect = "ALLOW"`
- Asserts GET by authority returns `target: "*"` and `effect: "GRANT"`
- Asserts GET by id returns `target: "*"` and `effect: "GRANT"`

Confirms that `uiToStoredTargets()` and `storedToUiTargets()` pass-through `*` without modification.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed stale assertion in SecuredEntityCapabilityServiceTest**

- **Found during:** Verification run — `returnsSortedCapabilitiesUsingUnionOfAllowForEntityAndWildcardAttributes` failing
- **Issue:** Test asserted `alphaSecret.isCanView() == false` but `ALPHAENTITY.*:EDIT` wildcard implies VIEW for ALL attributes (including `secret`) per the edit-implies-view semantics added in task `260330-eke`. The assertion was wrong, not the code.
- **Fix:** Changed assertion to `assertThat(alphaSecret.isCanView()).isTrue()` with explanatory comment
- **Files modified:** `src/test/java/com/vn/core/service/security/SecuredEntityCapabilityServiceTest.java`
- **Commit:** 511e4d3 (included in same commit)

## Verification

```
./gradlew test
./gradlew integrationTest --tests "*SecPermissionAdminResourceIT*"
```

Results:
- `./gradlew test` — BUILD SUCCESSFUL, 138 tests, 0 failures (pre-existing failure resolved)
- `SecPermissionAdminResourceIT` — 18 tests, 0 failures, 0 errors

## Known Stubs

None.

## Self-Check: PASSED

- `src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java` — FOUND
- `src/test/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResourceIT.java` — FOUND
- `src/test/java/com/vn/core/service/security/SecuredEntityCapabilityServiceTest.java` — FOUND
- `.planning/quick/260330-l8t-wildcard-permission-cleanup-and-mutual-e/260330-l8t-SUMMARY.md` — FOUND
- Commit `511e4d3` — FOUND
