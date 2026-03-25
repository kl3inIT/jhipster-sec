---
phase: 07-enterprise-navigation-shell
plan: 01
subsystem: api
tags: [spring-boot, liquibase, security, navigation, rest]
requires:
  - phase: 02-security-metadata-management
    provides: authority-backed security persistence and admin-managed security schemas
provides:
  - app-scoped navigation grant persistence separate from entity and attribute permissions
  - current-user REST contract for allowed shell leaf ids
  - backend proof for no-authority, allow-union, and deny-wins navigation resolution
affects: [07-02 navigation service, 07-03 route guards, 07-05 backend regression coverage]
tech-stack:
  added: []
  patterns: [dedicated app-scoped navigation grant model, deny-wins current-user leaf resolution]
key-files:
  created:
    - src/main/resources/config/liquibase/changelog/20260325000100_create_sec_navigation_grant.xml
    - src/main/java/com/vn/core/security/domain/SecNavigationGrant.java
    - src/main/java/com/vn/core/security/repository/SecNavigationGrantRepository.java
    - src/main/java/com/vn/core/service/dto/security/NavigationGrantResponseDTO.java
    - src/main/java/com/vn/core/service/security/CurrentUserNavigationGrantService.java
    - src/main/java/com/vn/core/web/rest/NavigationGrantResource.java
    - src/test/java/com/vn/core/service/security/CurrentUserNavigationGrantServiceTest.java
    - src/test/java/com/vn/core/web/rest/NavigationGrantResourceIT.java
  modified:
    - src/main/resources/config/liquibase/master.xml
    - .planning/phases/07-enterprise-navigation-shell/07-01-SUMMARY.md
key-decisions:
  - "Navigation grants stay separate from `SecPermission` and `TargetType` so frontend shell ids do not bleed into entity and attribute enforcement."
  - "Current-user navigation lookup merges authority grants with deny-wins semantics and alphabetized output for a stable REST contract."
patterns-established:
  - "App-scoped shell grants are keyed by `authority_name`, `app_name`, and `node_id`."
  - "Navigation visibility contracts return only `appName` and sorted `allowedNodeIds`, leaving tree ownership in the frontend."
requirements-completed: [ROUTE-01, ROUTE-02]
duration: 7min
completed: 2026-03-25
---

# Phase 07 Plan 01: Navigation Grant Contract Summary

**Dedicated backend navigation-grant persistence and current-user REST lookup for app-scoped shell leaf visibility**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-25T06:06:08Z
- **Completed:** 2026-03-25T06:12:52Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments

- Added a dedicated `sec_navigation_grant` Liquibase schema and JPA model keyed by authority, app, and frontend node id.
- Implemented current-user app-scoped navigation lookup with empty-authority short-circuiting, allow-union behavior, and deny-wins filtering.
- Exposed `/api/security/navigation-grants` plus focused backend tests for authenticated payload shape and current-user resolution rules.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add app-scoped navigation-grant persistence and domain wiring** - `2fe0a6e` (feat)
2. **Task 2: Expose the current-user navigation-grant API and backend tests** - `2f2c2c6` (feat)

## Files Created/Modified

- `src/main/resources/config/liquibase/master.xml` - Wires the navigation-grant changelog into the main Liquibase chain.
- `src/main/resources/config/liquibase/changelog/20260325000100_create_sec_navigation_grant.xml` - Defines the table, uniqueness rule, indexes, and authority foreign key.
- `src/main/java/com/vn/core/security/domain/SecNavigationGrant.java` - App-scoped JPA entity for shell node grants.
- `src/main/java/com/vn/core/security/repository/SecNavigationGrantRepository.java` - Repository methods for app-scoped current-user and admin lookup paths.
- `src/main/java/com/vn/core/service/dto/security/NavigationGrantResponseDTO.java` - REST payload for `appName` plus `allowedNodeIds`.
- `src/main/java/com/vn/core/service/security/CurrentUserNavigationGrantService.java` - Current-user authority merge with deny-wins and stable sorting.
- `src/main/java/com/vn/core/web/rest/NavigationGrantResource.java` - Authenticated read-only endpoint for frontend shell visibility.
- `src/test/java/com/vn/core/service/security/CurrentUserNavigationGrantServiceTest.java` - Unit coverage for no-authority, allow-union, and deny-wins behavior.
- `src/test/java/com/vn/core/web/rest/NavigationGrantResourceIT.java` - Resource coverage for authenticated success and unauthenticated rejection.
- `.planning/phases/07-enterprise-navigation-shell/07-01-SUMMARY.md` - Execution summary for this plan.

## Decisions Made

- Kept navigation grants as a dedicated security model rather than extending `SecPermission`, which preserves the existing entity and attribute permission contract.
- Returned alphabetized allowed node ids from the service so the frontend receives a stable, deterministic contract regardless of database row order.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- The workspace defaulted Gradle to Java 17 while `build.gradle` asserts Java 25. Verification passed after rerunning the backend tests with `JAVA_HOME=C:\Users\admin\.jdks\temurin-25.0.2`.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The frontend navigation service can now query a stable app-scoped backend contract without overloading the entity permission model.
- Route metadata and menu filtering work can build directly on `allowedNodeIds` and deny-wins semantics with no known backend blockers.

---
*Phase: 07-enterprise-navigation-shell*
*Completed: 2026-03-25*
