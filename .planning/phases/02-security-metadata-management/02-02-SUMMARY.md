---
phase: 02-security-metadata-management
plan: 02
subsystem: security
tags: [mapstruct, spring-security, security-bridge, dto, validation, jakarta-validation]

# Dependency graph
requires:
  - phase: 02-security-metadata-management/01
    provides: SecPermission and SecRowPolicy JPA entities, EntityMapper base interface, AuthorityRepository, SecurityContextBridge interface and JHipsterSecurityContextBridge

provides:
  - SecRoleDTO with Jakarta validation (@NotBlank, @Size, @Pattern) for Authority mapping
  - SecPermissionDTO with Jakarta validation for permission API contract
  - SecRowPolicyDTO with Jakarta validation for row policy API contract
  - SecPermissionMapper (MapStruct, extends EntityMapper) with String<->TargetType enum conversion
  - SecRowPolicyMapper (MapStruct, extends EntityMapper) for all-String field mapping
  - MergedSecurityService interface as the stable Phase 3 programming contract
  - MergedSecurityServiceImpl delegating to SecurityContextBridge
  - MergedSecurityContextBridge (@Primary) filtering phantom JWT authorities via authorityRepository.findAllById
  - 7 unit tests for MergedSecurityContextBridge covering phantom filtering and authentication states
  - Updated SecurityContextBridgeWiringIT asserting @Primary bridge wins and both beans coexist

affects:
  - 02-03 (REST controllers use SecRoleDTO, SecPermissionDTO, SecRowPolicyDTO)
  - 02-04 (REST controllers build on MergedSecurityService)
  - 03 (Phase 3 security engine programs against MergedSecurityService interface)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "@Primary bridge override: MergedSecurityContextBridge is @Primary, superseding JHipsterSecurityContextBridge without modifying Phase 1 code"
    - "DTO String enums: targetType and policyType are String in DTOs (not enums) to stay decoupled from entity enum changes"
    - "Manual Authority->DTO mapping: SecRoleDTO uses a constructor rather than MapStruct due to Authority's Persistable lifecycle pattern"

key-files:
  created:
    - src/main/java/com/vn/core/service/dto/security/SecRoleDTO.java
    - src/main/java/com/vn/core/service/dto/security/SecPermissionDTO.java
    - src/main/java/com/vn/core/service/dto/security/SecRowPolicyDTO.java
    - src/main/java/com/vn/core/service/mapper/security/SecPermissionMapper.java
    - src/main/java/com/vn/core/service/mapper/security/SecRowPolicyMapper.java
    - src/main/java/com/vn/core/security/MergedSecurityService.java
    - src/main/java/com/vn/core/security/MergedSecurityServiceImpl.java
    - src/main/java/com/vn/core/security/bridge/MergedSecurityContextBridge.java
    - src/test/java/com/vn/core/security/bridge/MergedSecurityContextBridgeTest.java
  modified:
    - src/test/java/com/vn/core/security/bridge/SecurityContextBridgeWiringIT.java

key-decisions:
  - "String types in DTOs for enum fields: targetType and policyType are String (not enums) in DTOs to decouple REST contract from entity enum changes"
  - "Manual Authority->SecRoleDTO mapping: Authority uses Persistable lifecycle which complicates MapStruct; controllers will call new SecRoleDTO(authority.getName(), ...) directly"
  - "MergedSecurityContextBridge stays lightweight: only depends on AuthorityRepository, not UserService or other heavy beans (Pitfall 4)"
  - "MergedSecurityService is the Phase 3 contract: Phase 3 enforcement code programs against this interface, not SecurityContextBridge directly"

patterns-established:
  - "@Primary override pattern: provide a @Primary bean implementing an existing interface to override Phase 1 behavior without modifying Phase 1 code"
  - "Phantom authority filtering: always validate JWT authority names against jhi_authority table before trusting them in security decisions"

requirements-completed: [SEC-01, SEC-02, SEC-03]

# Metrics
duration: 7min
completed: 2026-03-21
---

# Phase 2 Plan 02: Security Service Layer Summary

**Three Jakarta-validated DTOs, two MapStruct mappers with String-to-TargetType-enum conversion, MergedSecurityService Phase 3 contract interface, and @Primary MergedSecurityContextBridge that filters phantom JWT authority names against jhi_authority before exposing them to callers.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-21T08:44:17Z
- **Completed:** 2026-03-21T08:51:00Z
- **Tasks:** 3
- **Files modified:** 10

## Accomplishments

- Three security DTOs (SecRoleDTO, SecPermissionDTO, SecRowPolicyDTO) with full Jakarta validation annotations matching the UI-SPEC contract shapes
- SecPermissionMapper and SecRowPolicyMapper extend EntityMapper and compile with MapStruct code generation (confirmed with `./gradlew compileJava`)
- MergedSecurityContextBridge declared @Primary, supersedes JHipsterSecurityContextBridge, and validates JWT authority names against jhi_authority to drop phantom claims

## Task Commits

Each task was committed atomically:

1. **Task 1: Create security DTOs and MapStruct mappers** - `ce1c319` (feat)
2. **Task 2: Create MergedSecurityService and MergedSecurityContextBridge** - `351807d` (feat)
3. **Task 3: Create bridge unit tests and update wiring IT** - `9433c89` (test)

## Files Created/Modified

- `src/main/java/com/vn/core/service/dto/security/SecRoleDTO.java` - Role DTO with @NotBlank/@Size/@Pattern on name, @NotNull on type (String, not enum)
- `src/main/java/com/vn/core/service/dto/security/SecPermissionDTO.java` - Permission DTO with @NotBlank on authorityName/target/action, @NotNull on targetType/effect
- `src/main/java/com/vn/core/service/dto/security/SecRowPolicyDTO.java` - Row policy DTO with @NotBlank on code/entityName/operation/expression, @NotNull on policyType
- `src/main/java/com/vn/core/service/mapper/security/SecPermissionMapper.java` - MapStruct mapper with explicit String<->TargetType conversions via expression mapping
- `src/main/java/com/vn/core/service/mapper/security/SecRowPolicyMapper.java` - MapStruct mapper for all-String SecRowPolicy fields
- `src/main/java/com/vn/core/security/MergedSecurityService.java` - Phase 3 interface: getCurrentUserAuthorityNames, getCurrentUserLogin, isAuthenticated
- `src/main/java/com/vn/core/security/MergedSecurityServiceImpl.java` - @Service implementation delegating to SecurityContextBridge
- `src/main/java/com/vn/core/security/bridge/MergedSecurityContextBridge.java` - @Primary bridge filtering phantom authorities via authorityRepository.findAllById
- `src/test/java/com/vn/core/security/bridge/MergedSecurityContextBridgeTest.java` - 7 Mockito unit tests (phantom filtering, unauthenticated, anonymous detection)
- `src/test/java/com/vn/core/security/bridge/SecurityContextBridgeWiringIT.java` - Updated: @Primary assertion changed to MergedSecurityContextBridge, added testBothBridgeBeansExist

## Decisions Made

- String types for enum fields in DTOs: keeps REST contract decoupled from entity enum changes; controllers convert String to enum when invoking service methods
- Manual Authority->SecRoleDTO mapping: Authority's Persistable lifecycle pattern with `isPersisted` transient flag complicates MapStruct code generation; controllers use `new SecRoleDTO(authority.getName(), authority.getDisplayName(), authority.getType().name())`
- MergedSecurityContextBridge depends only on AuthorityRepository: avoids circular dependencies and keeps the bridge lightweight per Pitfall 4 guidance
- MergedSecurityService is the stable Phase 3 API: enforcement beans in Phase 3 will program against this interface, not the bridge directly

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- **Pre-existing JDK environment constraint:** `./gradlew compileJava` requires JDK 25 (build.gradle line 23 asserts `java.specification.version == 25`). Used `JAVA_HOME=C:/Users/admin/.jdks/temurin-25.0.2` to run build tasks. This is the same pre-existing constraint from Plan 01.

## Known Stubs

None — all DTOs, mappers, and service classes are fully wired with no placeholder data flows.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- DTOs and mappers ready for REST controller layer (Plans 03-04)
- MergedSecurityService interface ready for Phase 3 enforcement beans to program against
- MergedSecurityContextBridge @Primary is in place; Phase 3 will rely on phantom-filtered authority names automatically

## Self-Check: PASSED

All 10 files present. All 3 task commits exist (ce1c319, 351807d, 9433c89). Key content verified:
- SecRoleDTO contains @NotBlank and @Pattern(regexp = "[A-Z_]+")
- SecPermissionDTO contains @NotBlank on authorityName, target, action
- SecPermissionMapper contains @Mapper(componentModel = "spring") and extends EntityMapper
- MergedSecurityService contains getCurrentUserAuthorityNames()
- MergedSecurityContextBridge contains @Primary and authorityRepository.findAllById
- MergedSecurityContextBridgeTest: 7 tests all PASSED per test-results XML
- SecurityContextBridgeWiringIT contains MergedSecurityContextBridge.class assertion

---
*Phase: 02-security-metadata-management*
*Completed: 2026-03-21*
