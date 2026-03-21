---
phase: 01-identity-and-authority-baseline
plan: 01
subsystem: testing
tags: [spring-security, mockmvc, integration-tests, regression-baseline, jwt, jhipster]

requires: []
provides:
  - "Integration test regression baseline for account lifecycle flows (AUTH-02)"
  - "Integration test regression baseline for admin user management and authority listing (AUTH-03)"
  - "Gradle integrationTest task fix: testClassesDirs and classpath configured for Gradle 9 compatibility"
affects:
  - "01-identity-and-authority-baseline (all subsequent plans depend on this baseline not regressing)"

tech-stack:
  added: []
  patterns:
    - "Integration test gap-fill: add error-case coverage alongside existing happy-path tests"
    - "Baseline locking: assert known 500 behaviors as-is rather than fixing them (UserNotActivatedException, activation/reset key bugs)"
    - "@WithMockUser authority override on individual test methods to bypass class-level admin annotation"

key-files:
  created: []
  modified:
    - "src/test/java/com/vn/core/web/rest/AccountResourceIT.java"
    - "src/test/java/com/vn/core/web/rest/AuthenticateControllerIT.java"
    - "src/test/java/com/vn/core/web/rest/UserResourceIT.java"
    - "src/test/java/com/vn/core/web/rest/AuthorityResourceIT.java"
    - "gradle/profile_dev.gradle"

key-decisions:
  - "Unactivated user login returns 500 (UserNotActivatedException propagates through controller, not filtered) — locked as baseline per D-13"
  - "integrationTest Gradle task required explicit testClassesDirs + classpath configuration for Gradle 9 (NO-SOURCE fix)"
  - "testChangePasswordWithIncorrectCurrentPassword added as distinct named method per plan acceptance criteria"

patterns-established:
  - "Pattern: @WithMockUser(authorities = AuthoritiesConstants.USER) on individual method overrides class-level @WithMockUser(authorities = ADMIN)"
  - "Pattern: integrationTest task in profile_dev.gradle must set testClassesDirs and classpath from sourceSets.test"

requirements-completed: [AUTH-02, AUTH-03]

duration: 13min
completed: 2026-03-21
---

# Phase 01 Plan 01: Regression Test Baseline Summary

**Integration test baseline established for auth/account/admin flows: wrong-password rejection, unactivated-user lock-in (500), non-admin 403 enforcement on admin endpoints.**

## Performance

- **Duration:** 13 min
- **Started:** 2026-03-21T07:23:13Z
- **Completed:** 2026-03-21T07:35:54Z
- **Tasks:** 2 of 2
- **Files modified:** 5

## Accomplishments

- Added `testChangePasswordWithIncorrectCurrentPassword` to `AccountResourceIT` — locks in that wrong-old-password returns 400 (AUTH-02 gap)
- Added `testAuthorizeWithNotActivatedUser` to `AuthenticateControllerIT` — locks in that unactivated user login returns 500 (known behavior baseline per D-13)
- Added `testNonAdminCannotAccessAdminEndpoints` to `UserResourceIT` — locks in that `GET /api/admin/users` returns 403 for ROLE_USER (AUTH-03 gap)
- Added `testNonAdminCannotListAuthorities` to `AuthorityResourceIT` — locks in that `GET /api/authorities` returns 403 for ROLE_USER (AUTH-03 gap)
- Fixed `integrationTest` Gradle task to set `testClassesDirs` and `classpath` from `sourceSets.test` — required for Gradle 9 (NO-SOURCE bug fix, Rule 3 auto-fix)

## Task Commits

Each task was committed atomically:

1. **Task 1: Verify and extend AccountResourceIT and AuthenticateControllerIT regression baseline** - `98a2244` (test)
2. **Task 2: Verify and extend UserResourceIT and AuthorityResourceIT regression baseline** - `ae48976` (test)

## Files Created/Modified

- `src/test/java/com/vn/core/web/rest/AccountResourceIT.java` - Added `testChangePasswordWithIncorrectCurrentPassword`
- `src/test/java/com/vn/core/web/rest/AuthenticateControllerIT.java` - Added `testAuthorizeWithNotActivatedUser` (500 baseline)
- `src/test/java/com/vn/core/web/rest/UserResourceIT.java` - Added `testNonAdminCannotAccessAdminEndpoints`
- `src/test/java/com/vn/core/web/rest/AuthorityResourceIT.java` - Added `testNonAdminCannotListAuthorities`
- `gradle/profile_dev.gradle` - Added `testClassesDirs` + `classpath` to `integrationTest` task (Gradle 9 fix)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] integrationTest Gradle task had NO-SOURCE in Gradle 9**
- **Found during:** Task 1 verification
- **Issue:** The `integrationTest` task in `gradle/profile_dev.gradle` was a custom `Test` task without `testClassesDirs` or `classpath` set. In Gradle 9, this results in `NO-SOURCE` and the task doesn't run any tests.
- **Fix:** Added `testClassesDirs = sourceSets.test.output.classesDirs` and `classpath = sourceSets.test.runtimeClasspath` to the task configuration.
- **Files modified:** `gradle/profile_dev.gradle`
- **Commit:** `98a2244`

**2. [Rule 1 - Bug] testAuthorizeWithNotActivatedUser initially asserted 401 but actual behavior is 500**
- **Found during:** Task 1 test run
- **Issue:** `UserNotActivatedException` extends `AuthenticationException` but is thrown inside the `AuthenticateController.authorize()` method (not a filter). Spring MVC's `ExceptionTranslator` handles it as a generic exception, returning 500.
- **Fix:** Updated test to assert `isInternalServerError()` per D-13 (lock in baseline, not fix the behavior).
- **Files modified:** `src/test/java/com/vn/core/web/rest/AuthenticateControllerIT.java`
- **Commit:** `98a2244`

## Known Stubs

None.

## Self-Check: PASSED

Files verified:
- `src/test/java/com/vn/core/web/rest/AccountResourceIT.java` - FOUND, contains `testChangePasswordWithIncorrectCurrentPassword`
- `src/test/java/com/vn/core/web/rest/AuthenticateControllerIT.java` - FOUND, contains `testAuthorizeWithNotActivatedUser`
- `src/test/java/com/vn/core/web/rest/UserResourceIT.java` - FOUND, contains `testNonAdminCannotAccessAdminEndpoints`
- `src/test/java/com/vn/core/web/rest/AuthorityResourceIT.java` - FOUND, contains `testNonAdminCannotListAuthorities`
- `gradle/profile_dev.gradle` - FOUND, contains `testClassesDirs`

Commits verified:
- `98a2244` - FOUND
- `ae48976` - FOUND
