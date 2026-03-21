---
phase: 03-secure-enforcement-core
plan: "01"
subsystem: security-access-contracts
tags: [security, access-control, fetch-plan, data-manager, interfaces, type-graph]
dependency_graph:
  requires: []
  provides:
    - AccessContext
    - AccessConstraint
    - AccessManager
    - AccessManagerImpl
    - CrudEntityContext
    - FetchPlanAccessContext
    - AttributeAccessContext
    - RowLevelAccessContext
    - CrudEntityConstraint
    - SecuredEntityCatalog
    - SecuredEntityEntry
    - SecuredLoadQuery
    - SecureDataManager
    - UnconstrainedDataManager
    - FetchMode
    - FetchPlanProperty
    - FetchPlan
    - FetchPlanRepository
    - FetchPlanResolver
    - AttributeOp
    - RolePermissionService
    - EntityPermissionEvaluator
    - AttributePermissionEvaluator
    - SecureMergeService
    - RowPolicyDefinition
    - RowLevelPolicyProvider
    - SecureEntitySerializer
    - RepositoryRegistry
  affects: []
tech_stack:
  added: []
  patterns:
    - AccessConstraint pipeline pattern (supports/applyTo/getOrder)
    - Mutable AccessContext with default permitted state
    - SecuredLoadQuery as parameter object for secured reads
    - Builder pattern for SecuredEntityEntry record
    - Immutable FetchPlan class with unmodifiable properties list
key_files:
  created:
    - src/main/java/com/vn/core/security/access/AccessContext.java
    - src/main/java/com/vn/core/security/access/AccessConstraint.java
    - src/main/java/com/vn/core/security/access/AccessManager.java
    - src/main/java/com/vn/core/security/access/AccessManagerImpl.java
    - src/main/java/com/vn/core/security/access/CrudEntityContext.java
    - src/main/java/com/vn/core/security/access/FetchPlanAccessContext.java
    - src/main/java/com/vn/core/security/access/AttributeAccessContext.java
    - src/main/java/com/vn/core/security/access/RowLevelAccessContext.java
    - src/main/java/com/vn/core/security/access/CrudEntityConstraint.java
    - src/main/java/com/vn/core/security/catalog/SecuredEntityCatalog.java
    - src/main/java/com/vn/core/security/catalog/SecuredEntityEntry.java
    - src/main/java/com/vn/core/security/data/SecuredLoadQuery.java
    - src/main/java/com/vn/core/security/data/SecureDataManager.java
    - src/main/java/com/vn/core/security/data/UnconstrainedDataManager.java
    - src/main/java/com/vn/core/security/fetch/FetchMode.java
    - src/main/java/com/vn/core/security/fetch/FetchPlanProperty.java
    - src/main/java/com/vn/core/security/fetch/FetchPlan.java
    - src/main/java/com/vn/core/security/fetch/FetchPlanRepository.java
    - src/main/java/com/vn/core/security/fetch/FetchPlanResolver.java
    - src/main/java/com/vn/core/security/permission/AttributeOp.java
    - src/main/java/com/vn/core/security/permission/RolePermissionService.java
    - src/main/java/com/vn/core/security/permission/EntityPermissionEvaluator.java
    - src/main/java/com/vn/core/security/permission/AttributePermissionEvaluator.java
    - src/main/java/com/vn/core/security/merge/SecureMergeService.java
    - src/main/java/com/vn/core/security/row/RowPolicyDefinition.java
    - src/main/java/com/vn/core/security/row/RowLevelPolicyProvider.java
    - src/main/java/com/vn/core/security/serialize/SecureEntitySerializer.java
    - src/main/java/com/vn/core/security/repository/RepositoryRegistry.java
  modified: []
decisions:
  - "AttributeAccessContext uses String action (not AttributeOp) to keep the field open-coded for downstream plans that map from String REST payloads"
  - "RowLevelAccessContext uses List<Predicate> (not Specification<T>) to match plan spec and decouple row context from JPA Specification composition details"
  - "CrudEntityConstraint injects RolePermissionService (not EntityPermissionEvaluator) per plan spec — RolePermissionService.isEntityOpPermitted() is the single permission lookup point"
  - "FetchPlan is an immutable class (not a record) because it has builder-style mutation methods used by the YAML repository implementation in later plans"
  - "AccessManagerImpl uses @Component (not @Service) to match plan spec"
metrics:
  duration_seconds: 198
  completed_date: "2026-03-21"
  tasks_completed: 2
  files_created: 28
---

# Phase 3 Plan 1: Enforcement Contract Type Graph Summary

**One-liner:** Complete 28-type interface/record/enum graph across 9 security packages forming the Phase 3 enforcement contract for access, catalog, data manager, fetch plan, permission, merge, row policy, serialization, and repository registry.

## What Was Built

This plan established the full type contract that all Phase 3 implementation plans (02-05) build against. No business logic was implemented — only the contracts, parameter objects, and one concrete dispatcher (`AccessManagerImpl`).

### Task 1 — Access-context pipeline types and AccessManagerImpl

9 files in `com.vn.core.security.access`:

- `AccessContext` — marker interface for all pipeline contexts
- `AccessConstraint<C>` — generic constraint with `supports()`, `applyTo()`, `getOrder()`
- `AccessManager` — pipeline interface with `applyRegisteredConstraints(C)`
- `AccessManagerImpl` — `@Component` dispatcher: streams all constraints, filters by `isAssignableFrom`, sorts by order, casts and applies
- `CrudEntityContext` — mutable context, `permitted = false` default
- `FetchPlanAccessContext` — mutable context, `permitted = true` default (catalog allowlist is the gate)
- `AttributeAccessContext` — mutable context with `String action`, `permitted = false` default
- `RowLevelAccessContext` — mutable context with `List<Predicate>` and `addPredicate()`
- `CrudEntityConstraint` — `@Component` applying `RolePermissionService.isEntityOpPermitted()` to `CrudEntityContext`

### Task 2 — All remaining contract types (19 files)

**Catalog** (`com.vn.core.security.catalog`): `SecuredEntityCatalog` (entries/findByEntityClass/findByCode), `SecuredEntityEntry` record with Builder.

**Data** (`com.vn.core.security.data`): `SecuredLoadQuery` record with `of()` factory, `SecureDataManager` (loadByQuery/save/delete), `UnconstrainedDataManager` (load/loadAll/save/delete).

**Fetch** (`com.vn.core.security.fetch`): `FetchMode` enum, `FetchPlanProperty` record, `FetchPlan` immutable class, `FetchPlanRepository`, `FetchPlanResolver`.

**Permission additions** (`com.vn.core.security.permission`): `AttributeOp` enum (VIEW/EDIT), `RolePermissionService`, `EntityPermissionEvaluator`, `AttributePermissionEvaluator`.

**Merge** (`com.vn.core.security.merge`): `SecureMergeService` (mergeForUpdate).

**Row** (`com.vn.core.security.row`): `RowPolicyDefinition` (getSpecification), `RowLevelPolicyProvider` (getPolicies).

**Serialize** (`com.vn.core.security.serialize`): `SecureEntitySerializer` (serialize).

**Repository** (`com.vn.core.security.repository`): `RepositoryRegistry` (getRepository/getSpecificationExecutor).

## Deviations from Plan

### Auto-fixed Issues

None — plan executed exactly as written with the following intentional clarifications where the plan gave partial guidance:

1. `AttributeAccessContext`: Plan specified `String action` field. Implemented as `String action` (not `AttributeOp`) matching plan spec.
2. `RowLevelAccessContext`: Plan specified `List<jakarta.persistence.criteria.Predicate>`. Implemented using `jakarta.persistence.criteria.Predicate` import directly.
3. `CrudEntityConstraint`: Plan specified injecting `RolePermissionService`. Implemented using `RolePermissionService.isEntityOpPermitted()` as the permission check.
4. `FetchPlan`: Plan specified "immutable class" — implemented as a class (not record) with constructor, getters, and `Collections.unmodifiableList()` for the properties collection.

## Known Stubs

None. All types are contracts only. No stub data or placeholder implementations.

## Self-Check: PASSED

Files verified:
- `src/main/java/com/vn/core/security/access/AccessContext.java` — FOUND
- `src/main/java/com/vn/core/security/access/AccessManagerImpl.java` — FOUND
- `src/main/java/com/vn/core/security/access/CrudEntityConstraint.java` — FOUND
- `src/main/java/com/vn/core/security/data/SecureDataManager.java` — FOUND
- `src/main/java/com/vn/core/security/data/UnconstrainedDataManager.java` — FOUND
- `src/main/java/com/vn/core/security/catalog/SecuredEntityCatalog.java` — FOUND
- `src/main/java/com/vn/core/security/permission/RolePermissionService.java` — FOUND
- `src/main/java/com/vn/core/security/repository/RepositoryRegistry.java` — FOUND

Commits verified:
- `1a7ef97` — Task 1 (access pipeline types)
- `31eb4e8` — Task 2 (remaining contract types)
