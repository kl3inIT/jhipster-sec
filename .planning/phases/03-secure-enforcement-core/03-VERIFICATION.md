---
phase: 03-secure-enforcement-core
verified: 2026-03-21T00:00:00Z
status: passed
score: 7/7 must-haves verified
re_verification: false
---

# Phase 3: Secure Enforcement Core Verification Report

**Phase Goal:** Implement the secure enforcement core — the data-access layer that enforces CRUD permissions, row-level policies, attribute-level permissions, and fetch-plan-driven reads across all protected entities.
**Verified:** 2026-03-21
**Status:** passed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Any future AccessConstraint implementation can be added without changing existing callers or the AccessManager pipeline | VERIFIED | `AccessManagerImpl` streams all `List<AccessConstraint<?>>` beans, filters by `supports().isAssignableFrom(context.getClass())`, and routes by type — callers pass context objects, never touch constraint list |
| 2 | A downstream plan can construct a secured query with all needed parameters in a single object without ad-hoc argument bags | VERIFIED | `SecuredLoadQuery` record holds `entityCode`, `jpql`, `parameters`, `pageable`, `sort`, `fetchPlanCode`; factory method `of(...)` exists; `SecureDataManager.loadByQuery(SecuredLoadQuery)` accepts it |
| 3 | Protected services can perform secured reads, writes, and deletes through a single data-manager contract without touching repositories directly | VERIFIED | `SecureDataManager` interface exposes `loadByQuery`, `save`, `delete`; `SecureDataManagerImpl` is the sole implementation and wires all 7 enforcement dependencies — no direct repository access needed |
| 4 | Trusted internal code can bypass enforcement through a separate, explicitly named manager rather than working around security checks | VERIFIED | `UnconstrainedDataManager` + `UnconstrainedDataManagerImpl` exist; implementation contains only `RepositoryRegistry` dependency, no `AccessManager`, `RowLevel`, `FetchPlan`, `Serializer`, or `MergeService` imports |
| 5 | Only entities registered in the allowlist catalog participate in security enforcement, preventing accidental exposure of unregistered entities | VERIFIED | `DefaultSecuredEntityCatalog.entries()` returns `List.of()` by default; `SecureDataManagerImpl.resolveEntry()` throws `IllegalArgumentException` for unknown entity codes via `catalog.findByCode(entityCode).orElseThrow(...)` |
| 6 | Permission evaluators use DENY-wins semantics from Phase 2 SecPermission records | VERIFIED | `RolePermissionServiceDbImpl.hasPermission()` checks `perms.stream().anyMatch(p -> "DENY".equals(p.getEffect()))` first and returns `false` on any DENY — `RolePermissionServiceDbImplTest.testDenyWinsOverAllow()` confirms this |
| 7 | Unit tests prove enforcement order, DENY-wins, fail-closed row policy, attribute filtering, merge rejection, and unconstrained bypass | VERIFIED | 61 `@Test` methods across 10 test files; `SecureDataManagerImplTest` uses Mockito `InOrder` to verify READ/WRITE/DELETE enforcement sequences; all critical behaviors have dedicated test methods |

**Score:** 7/7 truths verified

---

## Required Artifacts

### Plan 01 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/vn/core/security/data/SecureDataManager.java` | Central secured data access interface | VERIFIED | Contains `loadByQuery(SecuredLoadQuery)`, `save(...)`, `delete(...)` |
| `src/main/java/com/vn/core/security/data/UnconstrainedDataManager.java` | Trusted bypass data access interface | VERIFIED | Contains `load`, `loadAll`, `save`, `delete` methods |
| `src/main/java/com/vn/core/security/data/SecuredLoadQuery.java` | Query object for secured reads | VERIFIED | Record with `entityCode`, `jpql`, `parameters`, `pageable`, `sort`, `fetchPlanCode` |
| `src/main/java/com/vn/core/security/catalog/SecuredEntityCatalog.java` | Entity allowlist interface | VERIFIED | Contains `entries()`, `findByEntityClass`, `findByCode` |
| `src/main/java/com/vn/core/security/access/AccessManager.java` | Constraint pipeline interface | VERIFIED | Contains `applyRegisteredConstraints(C context)` |
| `src/main/java/com/vn/core/security/access/AccessManagerImpl.java` | Constraint dispatcher | VERIFIED | `@Component`, routes by `c.supports().isAssignableFrom(context.getClass())`, sorted by `getOrder()` |
| `src/main/java/com/vn/core/security/access/CrudEntityContext.java` | Mutable CRUD context | VERIFIED | `permitted` defaults to `false` |
| `src/main/java/com/vn/core/security/access/FetchPlanAccessContext.java` | Fetch plan context | VERIFIED | `permitted` defaults to `true` (catalog is the gate) |
| `src/main/java/com/vn/core/security/access/AttributeAccessContext.java` | Attribute access context | VERIFIED | Has `action` field, `permitted` defaults to `false` |
| `src/main/java/com/vn/core/security/access/RowLevelAccessContext.java` | Row-level context | VERIFIED | Has `List<Predicate> predicates` |
| `src/main/java/com/vn/core/security/access/CrudEntityConstraint.java` | CRUD constraint | VERIFIED | `@Component`, injects `RolePermissionService`, calls `isEntityOpPermitted` |

### Plan 02 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/vn/core/security/permission/RolePermissionServiceDbImpl.java` | DENY-wins permission lookup | VERIFIED | Contains `"DENY".equals(p.getEffect())` check; delegates to `secPermissionRepository.findByRolesAndTarget` |
| `src/main/java/com/vn/core/security/permission/EntityPermissionEvaluatorImpl.java` | Entity permission evaluator | VERIFIED | `@Component`, delegates to `rolePermissionService.isEntityOpPermitted` |
| `src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java` | Attribute permission evaluator | VERIFIED | `@Component`, normalizes to `ENTITYNAME.ATTRIBUTENAME`, permissive default when no rules |
| `src/main/java/com/vn/core/security/row/RowLevelPolicyProviderDbImpl.java` | Row policy provider | VERIFIED | `@Service`, calls `secRowPolicyRepository.findByEntityNameAndOperation`, throws `AccessDeniedException` on JAVA type and unparseable expressions |
| `src/main/java/com/vn/core/security/row/RowLevelSpecificationBuilder.java` | Specification composition | VERIFIED | `@Component`, uses `Specification.where(null)` and AND-composes via `.and(...)` |
| `src/main/java/com/vn/core/security/repository/RepositoriesRegistryImpl.java` | Dynamic repository lookup | VERIFIED | `@Component`, `new Repositories(applicationContext)` in `@PostConstruct` |
| `src/main/java/com/vn/core/security/catalog/DefaultSecuredEntityCatalog.java` | Empty default catalog | VERIFIED | `@Component`, `entries()` returns `List.of()` |
| `src/main/java/com/vn/core/security/fetch/FetchPlanBuilder.java` | Code-based fetch plan builder | VERIFIED | `add(String)`, `add(String, FetchPlan)`, `build()` returning `FetchPlan` |
| `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java` | YAML fetch plan loading | VERIFIED | `@Component`, `@PostConstruct`, `new ObjectMapper(new YAMLFactory())`, key format `entityClassName#planName` |
| `src/main/java/com/vn/core/security/fetch/FetchPlanResolverImpl.java` | Fetch plan resolver | VERIFIED | `@Component`, delegates to `fetchPlanRepository.findByEntityAndName`, throws `IllegalArgumentException` if not found |
| `src/main/resources/fetch-plans.yml` | Empty YAML default | VERIFIED | Exists with `fetch-plans: []` |
| `src/main/resources/config/application.yml` | fetch-plans config key | VERIFIED | Contains `application.fetch-plans.config: classpath:fetch-plans.yml` |
| `build.gradle` | YAML Jackson dependency | VERIFIED | Contains `implementation "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml"` |
| `src/main/java/com/vn/core/config/ApplicationProperties.java` | FetchPlans inner class | VERIFIED | `private final FetchPlans fetchPlans = new FetchPlans()`, `getFetchPlans()`, inner class with `config` defaulting to `classpath:fetch-plans.yml` |

### Plan 03 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java` | Attribute-filtered serialization | VERIFIED | `@Component`, `isAlwaysVisible("id")`, `canView` per attribute, `BeanWrapperImpl`, `LinkedHashMap`, recursive collection handling |
| `src/main/java/com/vn/core/security/merge/SecureMergeServiceImpl.java` | Write-side attribute enforcement | VERIFIED | `@Component`, skips `"id"`, throws `AccessDeniedException("No EDIT permission for ...")` on denied attributes, `setPropertyValue` for permitted |

### Plan 04 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java` | Central enforcement orchestrator | VERIFIED | `@Service @Transactional`, 7 injected dependencies, full READ/WRITE/DELETE flows with `checkCrud`, row spec, fetch plan, serializer, merge |
| `src/main/java/com/vn/core/security/data/UnconstrainedDataManagerImpl.java` | Trusted bypass data access | VERIFIED | `@Service @Transactional`, only `RepositoryRegistry` dependency, no security enforcement |

### Plan 05 Artifacts (Tests)

| Artifact | Expected | Status | Tests |
|----------|----------|--------|-------|
| `src/test/java/com/vn/core/security/permission/RolePermissionServiceDbImplTest.java` | DENY-wins permission tests | VERIFIED | 8 tests; `testDenyWinsOverAllow` confirmed |
| `src/test/java/com/vn/core/security/permission/AttributePermissionEvaluatorImplTest.java` | Attribute evaluator tests | VERIFIED | 9 tests; permissive default confirmed |
| `src/test/java/com/vn/core/security/row/RowLevelPolicyProviderDbImplTest.java` | Fail-closed row policy tests | VERIFIED | 7 tests; `testUnparseableExpressionFailsClosed` and `testJavaPolicyTypeFailsClosed` confirmed |
| `src/test/java/com/vn/core/security/row/RowLevelSpecificationBuilderTest.java` | Specification composition tests | VERIFIED | 4 tests |
| `src/test/java/com/vn/core/security/fetch/YamlFetchPlanRepositoryTest.java` | YAML fetch plan tests | VERIFIED | 5 tests |
| `src/test/java/com/vn/core/security/fetch/FetchPlanBuilderTest.java` | FetchPlanBuilder tests | VERIFIED | 5 tests |
| `src/test/java/com/vn/core/security/serialize/SecureEntitySerializerImplTest.java` | Serializer attribute filtering tests | VERIFIED | 7 tests; `testIdAlwaysVisible`, `testDeniedAttributeOmitted` confirmed |
| `src/test/java/com/vn/core/security/merge/SecureMergeServiceImplTest.java` | Merge rejection tests | VERIFIED | 5 tests; `testDeniedAttributeThrowsAccessDenied`, `testIdAttributeSkipped` confirmed |
| `src/test/java/com/vn/core/security/data/SecureDataManagerImplTest.java` | Data manager pipeline tests | VERIFIED | 6 tests; `InOrder` enforcement order verification confirmed |
| `src/test/java/com/vn/core/security/data/UnconstrainedDataManagerImplTest.java` | Bypass tests | VERIFIED | 5 tests; no security imports confirmed |

**Total test methods: 61 across 10 files**

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `SecureDataManager` | `SecuredLoadQuery` | `loadByQuery` parameter type | VERIFIED | Method signature: `loadByQuery(SecuredLoadQuery query)` |
| `SecuredEntityCatalog` | `SecuredEntityEntry` | `entries()` return type | VERIFIED | `List<SecuredEntityEntry> entries()` |
| `AccessManager` | `AccessContext` | `applyRegisteredConstraints` parameter type | VERIFIED | `<C extends AccessContext> C applyRegisteredConstraints(C context)` |
| `RolePermissionServiceDbImpl` | `SecPermissionRepository` | `findByRolesAndTarget` query | VERIFIED | `secPermissionRepository.findByRolesAndTarget(authorityNames, TargetType.ENTITY, target, op.name())` |
| `RowLevelPolicyProviderDbImpl` | `SecRowPolicyRepository` | `findByEntityNameAndOperation` | VERIFIED | `secRowPolicyRepository.findByEntityNameAndOperation(entityName, operation.name())` |
| `YamlFetchPlanRepository` | `ApplicationProperties.FetchPlans` | config path for YAML location | VERIFIED | `applicationProperties.getFetchPlans().getConfig()` |
| `SecureEntitySerializerImpl` | `AttributePermissionEvaluator` | `canView` check per attribute | VERIFIED | `attributePermissionEvaluator.canView(entityClass, attr)` |
| `SecureMergeServiceImpl` | `AttributePermissionEvaluator` | `canEdit` check per attribute | VERIFIED | `attributePermissionEvaluator.canEdit(entityClass, attr)` |
| `SecureEntitySerializerImpl` | `FetchPlan` | property iteration for serialization | VERIFIED | `fetchPlan.getProperties()` iterated in `serialize()` |
| `SecureDataManagerImpl` | `AccessManager` | `applyRegisteredConstraints` for CRUD check | VERIFIED | `accessManager.applyRegisteredConstraints(ctx)` in `checkCrud()` |
| `SecureDataManagerImpl` | `RowLevelSpecificationBuilder` | row policy composition | VERIFIED | `rowLevelSpecificationBuilder.build(entityClass, EntityOp.READ/UPDATE/DELETE)` |
| `SecureDataManagerImpl` | `FetchPlanResolver` | fetch plan resolution | VERIFIED | `fetchPlanResolver.resolve(entityClass, query.fetchPlanCode())` |
| `SecureDataManagerImpl` | `SecureEntitySerializer` | attribute-filtered serialization | VERIFIED | `secureEntitySerializer.serialize(entity, fetchPlan)` |
| `SecureDataManagerImpl` | `SecureMergeService` | write-side attribute enforcement | VERIFIED | `secureMergeService.mergeForUpdate(entity, attributes)` |

---

## Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| DATA-01 | 01, 02, 04, 05 | Secured business entity reads go through a central security-aware data access layer | SATISFIED | `SecureDataManagerImpl.loadByQuery` enforces full pipeline; `SecureDataManager` interface is the single entry point; `UnconstrainedDataManagerImpl` is the only bypass |
| DATA-02 | 01, 02, 05 | Secured reads use fetch plans defined in YAML or code builders rather than database-stored fetch-plan metadata | SATISFIED | `YamlFetchPlanRepository` loads from `fetch-plans.yml`; `FetchPlanBuilder` is the code-builder path; no database table for fetch-plan definitions exists or is referenced |
| DATA-03 | 01, 03, 05 | Unauthorized attributes are excluded from secured read payloads | SATISFIED | `SecureEntitySerializerImpl` silently omits attributes where `canView()` returns false; `testDeniedAttributeOmitted` test verified |
| DATA-04 | 01, 03, 05 | Unauthorized attribute updates are rejected or stripped before persistence | SATISFIED | `SecureMergeServiceImpl` throws `AccessDeniedException` on denied attributes (reject, not strip per D-18); `testDeniedAttributeThrowsAccessDenied` test verified |
| DATA-05 | 01, 02, 04, 05 | Row-level policies constrain read, update, and delete access for secured entities | SATISFIED | `RowLevelSpecificationBuilder` AND-composes policies into JPA `Specification`; `SecureDataManagerImpl` applies row spec in all three flows (READ, UPDATE, DELETE); fail-closed behavior on invalid policies verified |

All five DATA requirements are satisfied. No orphaned requirements.

---

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `SecureDataManagerImpl.java` | 95-103 | JPQL-to-Specification conversion logs warning and proceeds without converting the JPQL | Info | Not a blocking stub — the plan explicitly scoped this as a Phase 4 concern (plan 04 task 1 states "Phase 3 does not implement arbitrary JPQL-to-Specification conversion"); JPQL access is still guarded by `entry.jpqlAllowed()` before the warn path is reached; the row spec is still applied |

No blocker anti-patterns. The JPQL limitation is an accepted, documented, in-scope boundary between Phase 3 and Phase 4.

---

## Human Verification Required

None. All enforcement behaviors are verifiable through code inspection and unit test analysis. Visual, real-time, or external-service behaviors are not part of Phase 3 scope.

---

## Summary

Phase 3 fully achieves its goal. The secure enforcement core is complete across all four enforcement dimensions:

**CRUD enforcement:** `CrudEntityConstraint` + `AccessManagerImpl` route CRUD checks through the constraint pipeline. `SecureDataManagerImpl.checkCrud()` throws `AccessDeniedException` on denial.

**Row-level enforcement:** `RowLevelPolicyProviderDbImpl` loads policies from `SecRowPolicyRepository`, converts them to JPA Specifications, and fails closed on invalid/JAVA policy types. `RowLevelSpecificationBuilder` AND-composes all policies. `SecureDataManagerImpl` applies the composed spec in all three flows.

**Attribute-level enforcement (read):** `SecureEntitySerializerImpl` walks fetch plan properties, applies `canView()` per attribute, always includes `id`, recurses through collection references, and silently omits denied attributes.

**Attribute-level enforcement (write):** `SecureMergeServiceImpl` checks `canEdit()` per attribute, skips `id`, and throws `AccessDeniedException` on any denied attribute — no silent strip.

**Fetch plans:** `YamlFetchPlanRepository` loads from `classpath:fetch-plans.yml` (default empty, Phase 4 populates), `FetchPlanBuilder` supports code-based construction, and `FetchPlanResolverImpl` resolves by entity class + plan code.

**Bypass path:** `UnconstrainedDataManagerImpl` holds no security imports and provides raw repository access for trusted internal use.

All 61 unit tests are substantive (not placeholder), covering DENY-wins semantics, fail-closed row policy, attribute filtering, merge rejection, and enforcement ordering verified with Mockito `InOrder`.

---

_Verified: 2026-03-21_
_Verifier: Claude (gsd-verifier)_
