---
phase: 04-protected-entity-proof
plan: 01
subsystem: database
tags: [liquibase, jpa, proof-domain, repositories, specifications]

# Dependency graph
requires:
  - phase: 03-secure-enforcement-core/04
    provides: SecureDataManager repository-registry contract and secured entity baseline

provides:
  - Liquibase proof schema for Organization, Department, and Employee
  - Proof JPA entities with ownerLogin, budget, and salary fields for row and attribute checks
  - Proof repositories exposing JpaRepository and JpaSpecificationExecutor for secured access

affects:
  - 04-02 (secured catalog registration and loadOne wiring)
  - 04-03 (proof services and REST resources)
  - 04-04 (integration seed data and end-to-end enforcement tests)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Proof entities follow the root-app JPA pattern with sequence-generated ids, Bean Validation annotations, and explicit table/column mappings"
    - "Protected entity repositories extend both JpaRepository and JpaSpecificationExecutor so SecureDataManager can combine CRUD and row-policy queries through RepositoryRegistry"

key-files:
  created:
    - src/main/resources/config/liquibase/changelog/20260321000500_create_proof_organization.xml
    - src/main/resources/config/liquibase/changelog/20260321000600_create_proof_department.xml
    - src/main/resources/config/liquibase/changelog/20260321000700_create_proof_employee.xml
    - src/main/java/com/vn/core/domain/proof/Organization.java
    - src/main/java/com/vn/core/domain/proof/Department.java
    - src/main/java/com/vn/core/domain/proof/Employee.java
    - src/main/java/com/vn/core/repository/proof/OrganizationRepository.java
    - src/main/java/com/vn/core/repository/proof/DepartmentRepository.java
    - src/main/java/com/vn/core/repository/proof/EmployeeRepository.java
  modified:
    - src/main/resources/config/liquibase/master.xml

key-decisions:
  - "Keep proof-domain fields explicit in the persistence model: ownerLogin drives row-policy checks, while budget and salary remain first-class sensitive attributes for later enforcement proof"
  - "Expose JpaSpecificationExecutor on every proof repository so the Phase 3 security pipeline can apply row-constrained lookups without custom repository adapters"

patterns-established:
  - "Phase 4 proof data lives in isolated proof packages under domain and repository, minimizing cross-talk with the baseline auth/admin model"

requirements-completed: [ENT-01]

# Metrics
duration: 19min
completed: 2026-03-21
---

# Phase 4 Plan 01: Proof Persistence Baseline Summary

**Created the proof-domain Liquibase schema plus Organization, Department, and Employee JPA types with spec-capable repositories, giving Phase 4 a real secured-entity baseline to build and test against.**

## Performance

- **Duration:** 19 min
- **Completed:** 2026-03-21T22:19:23+07:00
- **Tasks:** 2
- **Files created:** 9
- **Files modified:** 1

## Accomplishments

- Added `proof_organization`, `proof_department`, and `proof_employee` changelogs and registered them in the main Liquibase chain
- Created the `Organization -> Department -> Employee` proof hierarchy with the exact security-relevant fields required by Phase 4
- Added Spring Data proof repositories that support both CRUD and specification-backed row filtering
- Verified the new proof domain compiles with Java 25 through `./gradlew compileJava --console=plain --no-daemon`

## Verification

- `JAVA_HOME=C:\Users\admin\.jdks\temurin-25.0.2`
- `GRADLE_USER_HOME=D:\jhipster\.gradle-home`
- `.\gradlew compileJava --console=plain --no-daemon`

## Task Commits

1. **Task 1: Create Liquibase proof schema changelogs and register them in master.xml** - `82d6c17` (`feat(04-01): add proof schema changelogs`)
2. **Task 2: Create proof entities and repositories in isolated proof packages** - `fc129e2` (`feat(04-01): add proof entities and repositories`)

## Issues Encountered

- The first local `compileJava` attempt failed inside the sandbox because Gradle could not open the network socket required for dependency resolution; rerunning the compile outside the sandbox with Java 25 and a repo-local `GRADLE_USER_HOME` resolved verification cleanly

## Deviations from Plan

- None - plan executed as specified

## Self-Check: PASSED

- Liquibase master now includes all three proof changelogs in order
- Proof entities and repositories exist under `com.vn.core.domain.proof` and `com.vn.core.repository.proof`
- `compileJava` passes with the proof-domain baseline in place

---
*Phase: 04-protected-entity-proof*
*Completed: 2026-03-21*
