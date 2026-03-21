---
phase: 04-protected-entity-proof
plan: 04
subsystem: integration-tests
tags: [integration-test, liquibase, testcontainers, proof-api, row-level, attribute-permissions]

# Dependency graph
requires:
  - phase: 04-protected-entity-proof/03
    provides: proof services and authenticated proof REST resources

provides:
  - End-to-end allow/deny integration coverage for proof entities
  - Test-only Liquibase overlay and seed data for proof security scenarios
  - Verified proof APIs and backend baseline after wiring test changelog overrides correctly

affects:
  - 05-01 (frontend work can rely on verified 401/403/404 and filtered payload behavior)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Test-only Liquibase overlays use a dedicated changelog path referenced from test config instead of shadowing the main master.xml path"
    - "Proof API integration tests combine @WithMockUser, seeded authorities/permissions/row policies, and real MockMvc requests to exercise CRUD, row-level, and attribute-level enforcement"
    - "LiquibaseConfiguration must honor LiquibaseProperties.changeLog so test and production entrypoints can diverge safely"

key-files:
  created:
    - src/test/resources/config/liquibase/test-master.xml
    - src/test/resources/config/liquibase/changelog/20260321000800_seed_proof_security_test_data.xml
    - src/test/java/com/vn/core/web/rest/proof/SecuredEntityEnforcementIT.java
  modified:
    - src/main/java/com/vn/core/config/LiquibaseConfiguration.java
    - src/test/resources/config/application.yml
    - src/test/java/com/vn/core/TechnicalStructureTest.java

key-decisions:
  - "Replace the test classpath-shadow master with test-master.xml and make LiquibaseConfiguration honor LiquibaseProperties.changeLog"
  - "Allow the domain layer to reference the @SecuredEntity marker in ArchUnit because the annotation is the intentional catalog opt-in mechanism"
  - "Keep the create-path proof assertion budget-free because SecureMergeService currently applies EDIT checks on create as well as update"

patterns-established:
  - "Secured-entity integration fixtures should seed roles, permissions, row policies, and sample rows through a dedicated test Liquibase entrypoint rather than mutating the production changelog"

requirements-completed: [ENT-02]

# Metrics
duration: 24min
completed: 2026-03-21
---

# Phase 4 Plan 04: Enforcement Integration Summary

**Added end-to-end proof-entity enforcement coverage with test-only Liquibase fixtures and verified the backend baseline after fixing the test changelog override path.**

## Performance

- **Duration:** 24 min
- **Completed:** 2026-03-21T23:24:17+07:00
- **Tasks:** 2
- **Files created:** 3
- **Files modified:** 3

## Accomplishments

- Added a dedicated test-only Liquibase entrypoint and proof seed data for authorities, permissions, row policies, and sample proof rows
- Added `SecuredEntityEnforcementIT` covering allowed reads, denied reads, row-filtered not found, denied attribute edits, denied deletes, and allowed creates through the real proof APIs
- Updated `LiquibaseConfiguration` so `spring.liquibase.change-log` is honored instead of always forcing the production `master.xml`
- Narrowly exempted the `@SecuredEntity` annotation dependency in `TechnicalStructureTest` so the proof entity opt-in marker does not violate the domain-layer architecture rule
- Verified the focused proof enforcement suite and then reran the full `test` plus `integrationTest` baseline successfully

## Verification

- `JAVA_HOME=C:\Users\admin\.jdks\temurin-25.0.2`
- `GRADLE_USER_HOME=D:\jhipster\.gradle-home`
- `./gradlew integrationTest --tests "com.vn.core.web.rest.proof.SecuredEntityEnforcementIT" --console=plain --no-daemon`
- `./gradlew test integrationTest --console=plain --no-daemon`

## Task Commits

1. **Task 1: Add proof security fixtures and test-only Liquibase seed data** - `9112047` (`test(04-04): add proof security fixtures`)
2. **Task 2: Add proof enforcement integration coverage and ArchUnit adjustment** - `76898b0` (`test(04-04): add proof enforcement integration test`)

## Issues Encountered

- The first test-only overlay reused `config/liquibase/master.xml`, and Liquibase rejected the duplicate classpath path once both main and test resources exposed the same file
- `LiquibaseConfiguration` hardcoded `classpath:config/liquibase/master.xml`, which bypassed the proof seed data until the configuration was corrected to respect `LiquibaseProperties.changeLog`
- Testcontainers needed outside-sandbox Gradle runs on this Windows environment for reliable Docker access during integration verification

## Deviations from Plan

- Added a follow-up runtime fix in `LiquibaseConfiguration` so tests can swap in `test-master.xml` cleanly
- Replaced the original test resource `master.xml` shadow with `test-master.xml` to avoid duplicate Liquibase path resolution
- Adjusted ArchUnit to ignore `com.vn.core.security.catalog.SecuredEntity` as an allowed annotation dependency from the proof domain package

## Self-Check: PASSED

- Proof roles, permissions, row policies, and sample rows now load through the test-only Liquibase entrypoint
- `SecuredEntityEnforcementIT` passes end to end against Testcontainers PostgreSQL
- The full `test` plus `integrationTest` verification passes after the proof enforcement changes

---
*Phase: 04-protected-entity-proof*
*Completed: 2026-03-21*
