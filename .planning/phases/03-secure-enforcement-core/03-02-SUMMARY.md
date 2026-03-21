---
phase: 03-secure-enforcement-core
plan: 02
subsystem: security
tags: [spring-security, jpa, specification, fetch-plan, yaml, jackson, row-policy, permissions]

requires:
  - phase: 03-01
    provides: interfaces for RolePermissionService, EntityPermissionEvaluator, AttributePermissionEvaluator, RowLevelPolicyProvider, RowPolicyDefinition, FetchPlanRepository, FetchPlanResolver, RepositoryRegistry, SecuredEntityCatalog, MergedSecurityService
  - phase: 02-security-metadata-management
    provides: SecPermissionRepository, SecRowPolicyRepository, SecPermission, SecRowPolicy domain entities

provides:
  - DENY-wins permission lookup via RolePermissionServiceDbImpl backed by SecPermissionRepository
  - Entity-level CRUD permission evaluation via EntityPermissionEvaluatorImpl
  - Attribute-level view/edit permission evaluation with permissive-default via AttributePermissionEvaluatorImpl
  - Row-policy loading and fail-closed Specification/JPQL parsing via RowLevelPolicyProviderDbImpl
  - AND-composed row policy Specifications via RowLevelSpecificationBuilder
  - Dynamic JPA repository/specification-executor lookup via RepositoriesRegistryImpl
  - Empty default secured entity catalog via DefaultSecuredEntityCatalog
  - YAML fetch plan loading via YamlFetchPlanRepository
  - Code-based fetch plan builder via FetchPlanBuilder and FetchPlans factory
  - Fetch plan resolver with IllegalArgumentException on miss via FetchPlanResolverImpl
  - Bean-introspection and @Entity detection utilities via FetchPlanMetadataTools
  - ApplicationProperties.FetchPlans binding for config path
  - Empty fetch-plans.yml seed file

affects: [03-03, 03-04, 03-05]

tech-stack:
  added: [com.fasterxml.jackson.dataformat:jackson-dataformat-yaml]
  patterns:
    - DENY-wins security semantics via String effect comparison on SecPermission records
    - Permissive attribute-level default (no rules = allowed) distinguished from entity-level default
    - Fail-closed row policy parsing — AccessDeniedException on unparseable or JAVA-type expressions
    - AND-composition of Specifications for multi-policy row filtering
    - entityClassName.toLowerCase()#planName key structure for fetch plan lookup

key-files:
  created:
    - src/main/java/com/vn/core/security/permission/RolePermissionServiceDbImpl.java
    - src/main/java/com/vn/core/security/permission/EntityPermissionEvaluatorImpl.java
    - src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java
    - src/main/java/com/vn/core/security/row/RowLevelPolicyProviderDbImpl.java
    - src/main/java/com/vn/core/security/row/RowLevelSpecificationBuilder.java
    - src/main/java/com/vn/core/security/repository/RepositoriesRegistryImpl.java
    - src/main/java/com/vn/core/security/catalog/DefaultSecuredEntityCatalog.java
    - src/main/java/com/vn/core/security/fetch/FetchPlanBuilder.java
    - src/main/java/com/vn/core/security/fetch/FetchPlans.java
    - src/main/java/com/vn/core/security/fetch/FetchPlanMetadataTools.java
    - src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java
    - src/main/java/com/vn/core/security/fetch/FetchPlanResolverImpl.java
    - src/main/resources/fetch-plans.yml
  modified:
    - build.gradle
    - src/main/java/com/vn/core/config/ApplicationProperties.java
    - src/main/resources/config/application.yml

key-decisions:
  - "AttributePermissionEvaluatorImpl uses permissive-default: empty permission list returns true (no rules = allowed), while entity-level evaluator uses DENY-default"
  - "RowLevelPolicyProviderDbImpl is fail-closed: JAVA policyType and any unparseable SPECIFICATION/JPQL expression throw AccessDeniedException"
  - "YamlFetchPlanRepository keyed as entityClassName.toLowerCase()#planName matching plan spec"
  - "DefaultSecuredEntityCatalog returns empty list — Phase 4 provides @Primary override with real entity registrations"

patterns-established:
  - "Permission evaluator pattern: normalize target to UPPER_SNAKE_CASE, delegate to hasPermission, apply DENY-wins"
  - "Attribute permission pattern: permissive default for missing rules, DENY-wins when rules exist"
  - "Row policy pattern: fail-closed — any unsupported or unparseable expression throws AccessDeniedException"
  - "Fetch plan key pattern: entityClassName.toLowerCase()#planName for registry lookups"

requirements-completed: [DATA-01, DATA-02, DATA-05]

duration: 4min
completed: 2026-03-21
---

# Phase 3 Plan 2: Enforcement Primitives Summary

**DENY-wins permission evaluators, fail-closed row policy Specifications, YAML fetch plan loading, and dynamic repository registry — all enforcement building blocks for SecureDataManager**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-21T13:14:28Z
- **Completed:** 2026-03-21T13:18:37Z
- **Tasks:** 2
- **Files modified:** 16

## Accomplishments

- All 7 permission/row/registry/catalog implementation beans created with correct interface contracts
- Fetch-plan stack (YAML loader, code builder, resolver, metadata tools) fully implemented
- ApplicationProperties extended with FetchPlans binding; application.yml and fetch-plans.yml seeded

## Task Commits

1. **Task 1: Permission evaluators, row-policy provider, specification builder, repository registry, and catalog default** - `ef636c9` (feat)
2. **Task 2: Fetch-plan stack — YAML repository, code builder, resolver, metadata tools, and ApplicationProperties config** - `67bafec` (feat)

## Files Created/Modified

- `src/main/java/com/vn/core/security/permission/RolePermissionServiceDbImpl.java` - DENY-wins entity op permission lookup via SecPermissionRepository
- `src/main/java/com/vn/core/security/permission/EntityPermissionEvaluatorImpl.java` - Thin delegator to RolePermissionService
- `src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java` - Permissive-default attribute view/edit checks
- `src/main/java/com/vn/core/security/row/RowLevelPolicyProviderDbImpl.java` - Fail-closed SPECIFICATION/JPQL policy parsing
- `src/main/java/com/vn/core/security/row/RowLevelSpecificationBuilder.java` - AND-composes row policy Specifications
- `src/main/java/com/vn/core/security/repository/RepositoriesRegistryImpl.java` - Spring Data Repositories-backed dynamic lookup
- `src/main/java/com/vn/core/security/catalog/DefaultSecuredEntityCatalog.java` - Empty default catalog for Phase 4 override
- `src/main/java/com/vn/core/security/fetch/FetchPlanBuilder.java` - Mutable code builder for FetchPlan instances
- `src/main/java/com/vn/core/security/fetch/FetchPlans.java` - @Component factory returning FetchPlanBuilder
- `src/main/java/com/vn/core/security/fetch/FetchPlanMetadataTools.java` - Introspector-based property listing and @Entity detection
- `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java` - @PostConstruct YAML loading with extends support
- `src/main/java/com/vn/core/security/fetch/FetchPlanResolverImpl.java` - Delegates to FetchPlanRepository, throws on miss
- `src/main/resources/fetch-plans.yml` - Empty seed file for Phase 4 fetch plan definitions
- `build.gradle` - Added jackson-dataformat-yaml dependency
- `src/main/java/com/vn/core/config/ApplicationProperties.java` - Added FetchPlans inner class with config path binding
- `src/main/resources/config/application.yml` - Added application.fetch-plans.config property

## Decisions Made

- AttributePermissionEvaluatorImpl uses a permissive default (empty permission list = allowed) to distinguish from entity-level permission where no records means deny. Attribute rules are opt-in restrictions.
- RowLevelPolicyProviderDbImpl rejects JAVA-type policies with AccessDeniedException per D-14. SPECIFICATION supports `field = CURRENT_USER_LOGIN` pattern. JPQL supports `{CURRENT_USER_LOGIN}` token substitution for simple equality patterns.
- YamlFetchPlanRepository uses a two-step resolve loop with cycle detection to support `extends` references. Entity class resolution is best-effort (ClassForName) so unknown entity names log a warning but don't block startup.
- DefaultSecuredEntityCatalog intentionally has no @Primary — Phase 4 will provide the @Primary override with real entity registrations.

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## Known Stubs

None — all implementations are functional. DefaultSecuredEntityCatalog intentionally returns an empty list as documented in the plan (Phase 4 populates it via @Primary override).

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

All enforcement primitives (permission evaluators, row policy provider, specification builder, repository registry, catalog, and fetch plan stack) are ready. Plan 03 (SecureDataManager) can now orchestrate these building blocks into the full data pipeline.
