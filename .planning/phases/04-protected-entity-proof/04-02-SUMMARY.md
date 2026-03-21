---
phase: 04-protected-entity-proof
plan: 02
subsystem: security-runtime
tags: [catalog, fetch-plans, secure-data-manager, yaml, metamodel, row-level]

# Dependency graph
requires:
  - phase: 04-protected-entity-proof/01
    provides: proof entities, proof repositories, proof Liquibase schema

provides:
  - Metamodel-backed secured catalog for the proof entities
  - SecureDataManager single-record read path with row-constrained id lookup
  - Fail-closed secured JPQL behavior and nested inline YAML fetch plans

affects:
  - 04-03 (proof services and REST resources can call SecureDataManager.loadOne)
  - 04-04 (integration tests can assert nested secured payloads and fail-closed reads)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Secured proof entities are discovered from the live JPA metamodel but only admitted through an explicit @SecuredEntity marker"
    - "Nested association graphs can now be expressed inline in YAML through recursive properties blocks"
    - "SecureDataManager single-record reads apply CRUD plus row-policy filters before fetch-plan serialization"

key-files:
  created:
    - src/main/java/com/vn/core/security/catalog/SecuredEntity.java
    - src/main/java/com/vn/core/security/catalog/MetamodelSecuredEntityCatalog.java
  modified:
    - src/main/java/com/vn/core/domain/proof/Organization.java
    - src/main/java/com/vn/core/domain/proof/Department.java
    - src/main/java/com/vn/core/domain/proof/Employee.java
    - src/main/java/com/vn/core/security/data/SecureDataManager.java
    - src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java
    - src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java
    - src/main/resources/fetch-plans.yml
    - src/test/java/com/vn/core/security/data/SecureDataManagerImplTest.java
    - src/test/java/com/vn/core/security/fetch/YamlFetchPlanRepositoryTest.java

key-decisions:
  - "Keep metamodel discovery fail-closed by requiring @SecuredEntity on proof entities instead of exposing every managed JPA type"
  - "Add SecureDataManager.loadOne rather than making proof services reach into repositories for single-record reads"
  - "Use inline nested YAML properties for proof associations so fetch-plan structure stays code/YAML defined and out of the database"

patterns-established:
  - "Secured read APIs can now choose between paged loadByQuery and id-scoped loadOne while staying inside the same security pipeline"

requirements-completed: [ENT-01]

# Metrics
duration: 32min
completed: 2026-03-21
---

# Phase 4 Plan 02: Runtime Wiring Summary

**Added a metamodel-gated proof catalog, nested proof fetch plans, and a fail-closed `SecureDataManager.loadOne(...)` path so the Phase 3 enforcement core can read real proof entities safely.**

## Performance

- **Duration:** 32 min
- **Completed:** 2026-03-21T22:51:13+07:00
- **Tasks:** 2
- **Files created:** 2
- **Files modified:** 9

## Accomplishments

- Added `@SecuredEntity` plus a `@Primary` metamodel-backed catalog that exposes only `organization`, `department`, and `employee`
- Extended `SecureDataManager` with `loadOne(...)` and changed the secured JPQL placeholder branch to fail closed instead of warning and continuing
- Taught `YamlFetchPlanRepository` to parse nested inline `properties` blocks and populated `fetch-plans.yml` with real proof plans
- Expanded the focused unit tests to cover `loadOne`, empty row-filtered reads, fail-closed JPQL, and recursive nested proof plans

## Verification

- `JAVA_HOME=C:\Users\admin\.jdks\temurin-25.0.2`
- `GRADLE_USER_HOME=D:\jhipster\.gradle-home`
- `.\gradlew test --tests "com.vn.core.security.fetch.YamlFetchPlanRepositoryTest" --tests "com.vn.core.security.data.SecureDataManagerImplTest" --console=plain --no-daemon`

## Task Commits

1. **Task 1: Add secured-entity opt-in annotation and a metamodel-backed primary catalog** - `01c343a` (`feat(04-02): add proof secured entity catalog`)
2. **Task 2: Add nested proof fetch plans and a secured loadOne path, and fail closed on unresolved JPQL** - `1d1ce95` (`feat(04-02): add proof fetch plans and secure loadOne`)

## Issues Encountered

- Both the Git commit step and the focused Gradle test run needed to be retried outside the sandbox because of the same Windows pipe/socket restrictions seen in wave 1; the implementation itself required no functional rework once verification ran with Java 25 and the repo-local Gradle cache

## Deviations from Plan

- None - the code changes matched the planned scope exactly; the only detour was execution-tooling recovery after sandboxed commit and test failures

## Self-Check: PASSED

- The proof entities are now cataloged through `@SecuredEntity` and `MetamodelSecuredEntityCatalog`
- `SecureDataManager.loadOne(...)` exists and is covered by unit tests
- Nested `organization-detail`, `department-detail`, and `employee-detail` fetch plans resolve from YAML
- Focused tests for `YamlFetchPlanRepositoryTest` and `SecureDataManagerImplTest` pass

---
*Phase: 04-protected-entity-proof*
*Completed: 2026-03-21*
