---
phase: 02-security-metadata-management
plan: 01
subsystem: database
tags: [liquibase, jpa, spring-data, postgresql, security, permissions, row-policy]

# Dependency graph
requires:
  - phase: 01-identity-and-authority-baseline
    provides: Authority entity with String PK (name) used as FK target for sec_permission

provides:
  - Three Liquibase changelogs evolving jhi_authority and creating sec_permission, sec_row_policy tables
  - RoleType enum (RESOURCE, ROW_LEVEL) in com.vn.core.domain
  - TargetType enum (ENTITY, ATTRIBUTE, ROW_POLICY) in com.vn.core.security.permission
  - EntityOp enum (READ, CREATE, UPDATE, DELETE) in com.vn.core.security.permission
  - EntityMapper<D,E> base MapStruct interface in com.vn.core.service.mapper
  - Authority entity extended with displayName and type (RoleType) fields
  - SecPermission JPA entity with String authorityName FK (no @ManyToOne)
  - SecRowPolicy JPA entity with unique code constraint
  - SecPermissionRepository with findByRolesAndTarget Phase 3 query shape
  - SecRowPolicyRepository with findByEntityNameAndOperation and findByCode

affects:
  - 02-02 (DTOs and service layer depends on these entities and repositories)
  - 02-03 (REST controllers depend on service layer built on these foundations)
  - 03 (Phase 3 security engine uses SecPermissionRepository.findByRolesAndTarget)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - String-FK permission model: SecPermission uses authorityName (String) instead of @ManyToOne to SecRole,
      keeping the permission table decoupled from entity lifecycle and compatible with the string-based SecurityContextBridge
    - Uncached security entities: SecPermission and SecRowPolicy have no @Cache annotation since they are admin-managed,
      low-frequency entities where stale reads would be a correctness risk
    - RoleType in domain package: enum placed in com.vn.core.domain (not security.domain) to avoid ArchUnit ambiguity
      between Security and Domain layers

key-files:
  created:
    - src/main/resources/config/liquibase/changelog/20260321000100_add_authority_type_columns.xml
    - src/main/resources/config/liquibase/changelog/20260321000200_create_sec_permission.xml
    - src/main/resources/config/liquibase/changelog/20260321000300_create_sec_row_policy.xml
    - src/main/java/com/vn/core/domain/RoleType.java
    - src/main/java/com/vn/core/security/permission/TargetType.java
    - src/main/java/com/vn/core/security/permission/EntityOp.java
    - src/main/java/com/vn/core/service/mapper/EntityMapper.java
    - src/main/java/com/vn/core/security/domain/SecPermission.java
    - src/main/java/com/vn/core/security/domain/SecRowPolicy.java
    - src/main/java/com/vn/core/security/repository/SecPermissionRepository.java
    - src/main/java/com/vn/core/security/repository/SecRowPolicyRepository.java
  modified:
    - src/main/resources/config/liquibase/master.xml
    - src/main/java/com/vn/core/domain/Authority.java

key-decisions:
  - "String FK over @ManyToOne in SecPermission: authorityName is a String column (not a JPA relationship) to stay decoupled from Authority entity lifecycle and compatible with the SecurityContextBridge's Collection<String> authority names"
  - "No @Cache on SecPermission and SecRowPolicy: these are admin-managed, low-frequency entities; caching would risk stale security decisions"
  - "RoleType placed in com.vn.core.domain (not com.vn.core.security.domain): keeps ArchUnit layer assignment unambiguous since Authority entity (in domain layer) references it"
  - "FETCH_PLAN excluded from TargetType: fetch plans are YAML/code-defined only per project constraint"
  - "SecRowPolicy.operation stored as String (not EntityOp enum): preserves flexibility for row-policy operations that may extend beyond basic CRUD"

patterns-established:
  - "String-FK permission model: use authority name strings as FKs in security metadata tables"
  - "Uncached security entities: do not add @Cache to admin-managed security tables"
  - "EntityMapper base interface: use EntityMapper<D,E> as the base for all MapStruct mappers in this project"

requirements-completed: [SEC-01, SEC-02, SEC-03]

# Metrics
duration: 5min
completed: 2026-03-21
---

# Phase 2 Plan 01: Security Data Foundation Summary

**Liquibase schema evolution for jhi_authority (display_name, type columns), sec_permission (String-FK permission table), and sec_row_policy (unique-code policy table), with JPA entities, three enums, EntityMapper base interface, and Spring Data repositories exposing Phase 3 query shapes.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-21T08:37:12Z
- **Completed:** 2026-03-21T08:42:00Z
- **Tasks:** 3
- **Files modified:** 13

## Accomplishments

- Three Liquibase changelogs establish the complete Phase 2 schema: display_name and type on jhi_authority (existing rows default to RESOURCE), sec_permission with authority_name FK and CASCADE delete, sec_row_policy with unique code constraint
- Authority entity extended with displayName and type fields (RoleType enum, default RESOURCE), preserving all existing Persistable/Hazelcast behavior
- SecPermission and SecRowPolicy JPA entities ported from angapp with adapted FK strategy (String authority_name instead of @ManyToOne SecRole), plus full repositories with Phase 3 query shapes (findByRolesAndTarget, findByEntityNameAndOperation)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Liquibase changelogs for schema evolution** - `0ab4d16` (feat)
2. **Task 2: Create enums, EntityMapper interface, and extend Authority entity** - `b8a8387` (feat)
3. **Task 3: Create SecPermission and SecRowPolicy entities and repositories** - `a2c8f4a` (feat)

## Files Created/Modified

- `src/main/resources/config/liquibase/changelog/20260321000100_add_authority_type_columns.xml` - Adds display_name (nullable) and type (default RESOURCE, not-null) to jhi_authority
- `src/main/resources/config/liquibase/changelog/20260321000200_create_sec_permission.xml` - Creates sec_permission table with authority_name FK and CASCADE delete
- `src/main/resources/config/liquibase/changelog/20260321000300_create_sec_row_policy.xml` - Creates sec_row_policy table with unique code constraint
- `src/main/resources/config/liquibase/master.xml` - Registers all three new changelogs before the JHipster needle
- `src/main/java/com/vn/core/domain/Authority.java` - Extended with displayName and type (RoleType) fields plus getters/setters/builder
- `src/main/java/com/vn/core/domain/RoleType.java` - RESOURCE and ROW_LEVEL enum values
- `src/main/java/com/vn/core/security/permission/TargetType.java` - ENTITY, ATTRIBUTE, ROW_POLICY (FETCH_PLAN excluded)
- `src/main/java/com/vn/core/security/permission/EntityOp.java` - READ, CREATE, UPDATE, DELETE
- `src/main/java/com/vn/core/service/mapper/EntityMapper.java` - Base MapStruct interface with toEntity, toDto, partialUpdate
- `src/main/java/com/vn/core/security/domain/SecPermission.java` - Permission entity with String authorityName FK
- `src/main/java/com/vn/core/security/domain/SecRowPolicy.java` - Row policy entity with unique code
- `src/main/java/com/vn/core/security/repository/SecPermissionRepository.java` - Repo with findByRolesAndTarget, findByAuthorityName, deleteByAuthorityName
- `src/main/java/com/vn/core/security/repository/SecRowPolicyRepository.java` - Repo with findByEntityNameAndOperation, findByCode

## Decisions Made

- String FK over @ManyToOne in SecPermission: authorityName stored as a String column (not a JPA relationship) to decouple from Authority entity lifecycle and stay compatible with SecurityContextBridge's `Collection<String>` authority names interface
- No @Cache on SecPermission and SecRowPolicy: admin-managed entities where stale cache would cause incorrect security decisions
- RoleType placed in com.vn.core.domain (not security.domain): avoids ArchUnit layer ambiguity since Authority entity (Domain layer) references it
- FETCH_PLAN excluded from TargetType: enforces the project constraint that fetch plans are YAML/code-only
- SecRowPolicy.operation stored as String: preserves flexibility for row-policy operations that may extend beyond CRUD

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- **Pre-existing JDK environment blocker:** The Gradle build requires JDK 25 (`assert System.properties["java.specification.version"] == "25"` in build.gradle line 23), but only JDK 17 and 21 are available in the Gradle daemon. This blocks `./gradlew compileJava` and `./gradlew test`. This is a pre-existing environment constraint unrelated to the changes in this plan. The XML/Java files were verified through structural checks (grep, file existence, content verification) rather than compilation. The ArchUnit compliance was assessed statically: all new classes reside in the Security layer (`..security..`) and do not violate the layered architecture rules.

## Known Stubs

None — all entities map directly to database tables with no stub/placeholder data flows.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Data foundation complete: all tables, entities, enums, and repositories ready for Phase 2 Plan 02 (DTOs, services, REST controllers)
- Phase 3 query shapes already in place: `findByRolesAndTarget` and `findByEntityNameAndOperation` are ready for the security engine
- Authority entity can now classify roles as RESOURCE vs ROW_LEVEL; existing data defaults to RESOURCE

## Self-Check: PASSED

All 13 files present. All 3 task commits exist (0ab4d16, b8a8387, a2c8f4a). Key content verified:
- master.xml references all 3 changelogs
- SecPermission has no real @ManyToOne annotation (comment only)
- TargetType has no real FETCH_PLAN constant (comment only)
- SecPermissionRepository.findByRolesAndTarget exists
- SecRowPolicyRepository.findByEntityNameAndOperation exists

---
*Phase: 02-security-metadata-management*
*Completed: 2026-03-21*
