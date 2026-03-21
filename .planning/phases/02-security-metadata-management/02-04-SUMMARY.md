---
phase: 02-security-metadata-management
plan: 04
subsystem: test
tags: [integration-test, mockmvc, testcontainers, security-admin, regression]

# Dependency graph
requires:
  - phase: 02-security-metadata-management/03
    provides: SecRoleAdminResource, SecPermissionAdminResource, SecRowPolicyAdminResource, seed roles

provides:
  - SecRoleAdminResourceIT coverage for CRUD, validation, auth denial, and cascade delete
  - SecPermissionAdminResourceIT coverage for CRUD, FK validation, validation, and auth denial
  - SecRowPolicyAdminResourceIT coverage for CRUD, duplicate-code handling, validation, and auth denial
  - DatabaseConfiguration repository scan fix so security repositories are registered as Spring beans

affects:
  - 03 (Phase 3 can rely on the Phase 2 admin API surface and repository wiring)
  - phase-completion (Phase 2 verification and roadmap closeout)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Integration tests use @IntegrationTest + @AutoConfigureMockMvc + @Transactional with direct repository assertions"
    - "Admin API error assertions check RFC7807 message keys like error.nameexists, error.rolenotfound, error.codeexists, and error.validation"
    - "DatabaseConfiguration now scans both com.vn.core.repository and com.vn.core.security.repository so web-layer security controllers can boot"

key-files:
  created:
    - src/test/java/com/vn/core/web/rest/admin/security/SecRoleAdminResourceIT.java
    - src/test/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResourceIT.java
    - src/test/java/com/vn/core/web/rest/admin/security/SecRowPolicyAdminResourceIT.java
  modified:
    - src/main/java/com/vn/core/config/DatabaseConfiguration.java

key-decisions:
  - "Keep the admin-security ITs fully integrated against the real Spring context and Testcontainers database instead of mocking repositories, so SEC-01/02/03 are proven end to end"
  - "Fix the missing JPA repository scan in DatabaseConfiguration instead of weakening tests; the targeted IT failures exposed a real Phase 02 wiring defect"

patterns-established:
  - "Security admin endpoints now have the same integration-test baseline as the existing account/admin endpoints: happy path, deny path, conflict path, and validation path"

requirements-completed: [SEC-01, SEC-02, SEC-03]

# Metrics
duration: 86min
completed: 2026-03-21
---

# Phase 2 Plan 04: Admin API Integration Test Summary

**Added end-to-end integration coverage for the Phase 2 admin security APIs and fixed the missing repository-scan wiring bug that prevented the new controllers from starting in the Spring context.**

## Performance

- **Duration:** 86 min
- **Completed:** 2026-03-21T16:15:20+07:00
- **Tasks:** 2
- **Files created:** 3
- **Files modified:** 1

## Accomplishments

- SecRoleAdminResourceIT proves role CRUD, duplicate-name rejection, non-admin 403, validation 400, and permission cascade delete on role removal
- SecPermissionAdminResourceIT proves permission CRUD, invalid-role rejection, validation 400, and non-admin 403
- SecRowPolicyAdminResourceIT proves row-policy CRUD, duplicate-code rejection, validation 400, and non-admin 403
- DatabaseConfiguration now scans `com.vn.core.security.repository`, which makes SecPermissionRepository and SecRowPolicyRepository available to the Phase 2 admin controllers at runtime
- Full `test integrationTest` passed under JDK 25 after the repository-scan fix

## Verification

- `JAVA_HOME=C:\Users\admin\.jdks\temurin-25.0.2 .\gradlew compileJava`
- `JAVA_HOME=C:\Users\admin\.jdks\temurin-25.0.2 .\gradlew test --tests "com.vn.core.TechnicalStructureTest"`
- `JAVA_HOME=C:\Users\admin\.jdks\temurin-25.0.2 .\gradlew integrationTest --tests "com.vn.core.web.rest.admin.security.SecRoleAdminResourceIT" --tests "com.vn.core.web.rest.admin.security.SecPermissionAdminResourceIT" --tests "com.vn.core.web.rest.admin.security.SecRowPolicyAdminResourceIT"`
- `JAVA_HOME=C:\Users\admin\.jdks\temurin-25.0.2 .\gradlew test integrationTest`

## Issues Encountered

- The initial `02-04` run failed before any test logic executed because `DatabaseConfiguration` only enabled `com.vn.core.repository`; the new security repositories from `02-03` were not registered as beans
- Local Gradle verification also required JDK 25 because `build.gradle` currently asserts `java.specification.version == "25"`

## Deviations from Plan

- The plan expected `02-03` to be bootstrappable already. In practice, `02-04` exposed a prerequisite defect, so execution included a small cross-plan fix in `DatabaseConfiguration.java` before the tests could run

## Self-Check: PASSED

- All three admin-security integration test classes exist and pass
- Full `test integrationTest` baseline passes after the wiring fix
- Phase 2 requirements SEC-01, SEC-02, and SEC-03 are now verified through automated integration coverage

---
*Phase: 02-security-metadata-management*
*Completed: 2026-03-21*
