---
phase: 03-secure-enforcement-core
plan: "05"
subsystem: security-tests
tags: [unit-tests, deny-wins, fail-closed, attribute-filtering, enforcement-order, fetch-plans, tdd]
dependency_graph:
  requires: [03-02, 03-03, 03-04]
  provides: [unit-test-suite-phase3]
  affects: [security-enforcement-validation, DATA-01, DATA-02, DATA-03, DATA-04, DATA-05]
tech_stack:
  added: []
  patterns:
    - JUnit 5 + Mockito @ExtendWith(MockitoExtension.class) with @Mock and @InjectMocks
    - Mockito InOrder for enforcement pipeline ordering assertions
    - BeanWrapperImpl-compatible test entities with getters/setters
    - In-memory YAML test resource (fetch-plans-test.yml) for YamlFetchPlanRepository tests
    - Lambda no-op Specification to avoid Specification.where(null) ambiguity in Java 25
key_files:
  created:
    - src/test/java/com/vn/core/security/permission/RolePermissionServiceDbImplTest.java
    - src/test/java/com/vn/core/security/permission/AttributePermissionEvaluatorImplTest.java
    - src/test/java/com/vn/core/security/row/RowLevelPolicyProviderDbImplTest.java
    - src/test/java/com/vn/core/security/row/RowLevelSpecificationBuilderTest.java
    - src/test/java/com/vn/core/security/fetch/YamlFetchPlanRepositoryTest.java
    - src/test/java/com/vn/core/security/fetch/FetchPlanBuilderTest.java
    - src/test/java/com/vn/core/security/serialize/SecureEntitySerializerImplTest.java
    - src/test/java/com/vn/core/security/merge/SecureMergeServiceImplTest.java
    - src/test/java/com/vn/core/security/data/SecureDataManagerImplTest.java
    - src/test/java/com/vn/core/security/data/UnconstrainedDataManagerImplTest.java
    - src/test/resources/fetch-plans-test.yml
  modified:
    - src/main/java/com/vn/core/security/row/RowLevelSpecificationBuilder.java
decisions:
  - RowLevelSpecificationBuilder uses lambda no-op spec instead of Specification.where(null) — Spring Data 3.x with Java 25 added PredicateSpecification overload making where(null) ambiguous and then non-null-safe
  - testIdAlwaysVisible does not stub canView("id") — implementation short-circuits id check before calling canView, making the stub unnecessary and rejected by Mockito strict mode
metrics:
  duration_minutes: 12
  completed_date: "2026-03-21"
  tasks_completed: 2
  files_created: 11
  files_modified: 1
---

# Phase 3 Plan 5: Unit Test Suite for Secure Enforcement Core Summary

**One-liner:** 61-test unit suite proving DENY-wins, fail-closed row policies, attribute filtering, enforcement pipeline order, and trusted bypass across 10 Phase 3 enforcement components.

## What Was Built

10 unit test files covering every Phase 3 enforcement component, satisfying VALIDATION.md Wave 0 requirements DATA-01 through DATA-05.

### Test Files and Coverage

| File | Tests | Key Behavior Proven |
|------|-------|---------------------|
| `RolePermissionServiceDbImplTest` | 8 | DENY-wins over ALLOW, empty-auth deny, target normalization |
| `AttributePermissionEvaluatorImplTest` | 9 | Permissive default, DENY-wins, target `ENTITY.FIELD` normalization |
| `RowLevelPolicyProviderDbImplTest` | 7 | SPECIFICATION parsing, JAVA type fail-closed, unparseable fail-closed |
| `RowLevelSpecificationBuilderTest` | 4 | No-policy unconstrained spec, AND composition of multiple policies |
| `YamlFetchPlanRepositoryTest` | 5 | YAML loading, extends inheritance, missing plan returns empty |
| `FetchPlanBuilderTest` | 5 | Simple plan, nested FetchPlan, immutable properties list |
| `SecureEntitySerializerImplTest` | 7 | id always visible (D-16), denied omitted (D-15), recursive collection |
| `SecureMergeServiceImplTest` | 5 | Permitted write, denied throws AccessDeniedException (D-18), id skipped |
| `SecureDataManagerImplTest` | 6 | Pipeline order (InOrder), CRUD denial, unknown entity code |
| `UnconstrainedDataManagerImplTest` | 5 | Direct repo delegation, no security mock imports |
| **Total** | **61** | |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed Specification.where(null) ambiguity in RowLevelSpecificationBuilder**
- **Found during:** Task 1 — first compile with Java 25 / GraalVM
- **Issue:** Spring Data 3.x added a `Specification.where(PredicateSpecification<T>)` overload, making `Specification.where(null)` ambiguous. Then the same version changed the non-ambiguous cast form to reject null with `IllegalArgumentException`.
- **Fix:** Replaced `Specification.where(null)` and `Specification.where((Specification<T>) null)` with inline lambda `(root, query, cb) -> null` which is the canonical no-op specification that returns null predicate (accepted by JPA criteria API as "no constraint").
- **Files modified:** `src/main/java/com/vn/core/security/row/RowLevelSpecificationBuilder.java`
- **Commit:** 147ba91

**2. [Rule 1 - Bug] Removed unnecessary Mockito stubs caught by strict mode**
- **Found during:** Task 1 and Task 2
- **Issue:** `testDenyWinsOverAllow` stubbed `getCurrentUserAuthorityNames()` but the test calls `hasPermission()` directly (bypassing the authority lookup). `testIdAlwaysVisible` stubbed `canView("id")` but the serializer short-circuits before calling `canView` for `id`.
- **Fix:** Removed the unnecessary stubs.
- **Files modified:** Test files affected.

## Known Stubs

None — all 10 test files target behavior proven by assertions, not placeholder implementations.

## Self-Check: PASSED

All 11 created/modified files confirmed present on disk.
Commits 147ba91 and cb4fed7 confirmed in git log.
