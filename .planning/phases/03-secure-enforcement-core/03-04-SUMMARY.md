---
phase: 03-secure-enforcement-core
plan: "04"
subsystem: security-data
tags: [secure-data-manager, enforcement-pipeline, trusted-bypass, row-policy, fetch-plan]
dependency_graph:
  requires: [03-01, 03-02, 03-03]
  provides: [SecureDataManagerImpl, UnconstrainedDataManagerImpl]
  affects: [security-data-access, enforcement-orchestration]
tech_stack:
  added: []
  patterns:
    - Central enforcement orchestrator composing CRUD/row/fetch-plan/serialize/merge
    - Trusted bypass path with no security enforcement for internal infrastructure
    - Row-constrained entity lookups for write-side access decisions
    - Unchecked generic casts with @SuppressWarnings for type-erased JPA repository access
key_files:
  created:
    - src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java
    - src/main/java/com/vn/core/security/data/UnconstrainedDataManagerImpl.java
  modified: []
decisions:
  - SecureDataManagerImpl uses @SuppressWarnings("unchecked") for generic JPA repository casts — unavoidable due to type erasure in RepositoryRegistry generic signatures
  - JPQL-to-Specification conversion deferred to Phase 4 — Phase 3 logs a warning and applies only row spec when JPQL is provided
  - UnconstrainedDataManager.delete loads entity first before delegating to repo.delete — consistent with EntityNotFoundException contract
metrics:
  duration_minutes: 2
  completed_date: "2026-03-21"
  tasks_completed: 2
  files_created: 2
  files_modified: 0
---

# Phase 03 Plan 04: Secure Data Manager Implementations Summary

**One-liner:** SecureDataManagerImpl orchestrates the full enforcement pipeline (CRUD check, row spec, query, fetch plan, serialize, merge) and UnconstrainedDataManagerImpl provides the explicit trusted bypass via RepositoryRegistry only.

## What Was Built

### SecureDataManagerImpl

`src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java`

The central enforcement orchestrator implementing `SecureDataManager`. Constructor-injects all 7 enforcement dependencies and routes data access through the complete pipeline:

- **`loadByQuery`** (READ flow per D-05): CRUD check -> row-policy spec via `rowLevelSpecificationBuilder.build` -> `specRepo.findAll(rowSpec, pageable)` -> `fetchPlanResolver.resolve` -> `secureEntitySerializer.serialize` per entity -> returns `PageImpl`
- **`save`** (WRITE flow per D-06): CRUD check (CREATE or UPDATE) -> row-constrained lookup via `specRepo.findOne(idSpec.and(rowSpec))` for UPDATE / `entityClass.getDeclaredConstructor().newInstance()` for CREATE -> `secureMergeService.mergeForUpdate` -> `repo.save` -> `fetchPlanResolver.resolve` -> `secureEntitySerializer.serialize`
- **`delete`** (DELETE flow): CRUD check -> row-constrained lookup via `specRepo.findOne(idSpec.and(rowSpec))` -> `repo.delete`
- **`resolveEntry`**: Looks up catalog entry via `catalog.findByCode`, throws `IllegalArgumentException` for unknown entity codes
- **`checkCrud`**: Applies constraints via `accessManager.applyRegisteredConstraints`, throws `AccessDeniedException` if not permitted

### UnconstrainedDataManagerImpl

`src/main/java/com/vn/core/security/data/UnconstrainedDataManagerImpl.java`

The trusted bypass path implementing `UnconstrainedDataManager`. Injects only `RepositoryRegistry` — contains no `AccessManager`, `AccessDeniedException`, row-level, fetch-plan, serializer, or merge references whatsoever.

- **`load`**: `repo.findById(id)` with `EntityNotFoundException` for missing entities
- **`loadAll`**: `repo.findAll()`
- **`save`**: `repo.save(entity)`
- **`delete`**: Loads entity first (reusing `load`) then `repo.delete(entity)`

## Enforcement Pipeline Summary

```
READ:   CRUD check -> row spec -> specRepo.findAll -> fetch plan -> serialize
WRITE:  CRUD check -> row-constrained findOne (UPDATE) | newInstance (CREATE) -> merge -> save -> fetch plan -> serialize
DELETE: CRUD check -> row-constrained findOne -> repo.delete
BYPASS: RepositoryRegistry only, no security
```

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None — both implementations are fully wired. The JPQL-to-Specification conversion is intentionally deferred (logged as warning at runtime) per the plan spec which explicitly states "Phase 4 can refine this for sample entities."

## Self-Check: PASSED

Files verified:
- `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java` — FOUND
- `src/main/java/com/vn/core/security/data/UnconstrainedDataManagerImpl.java` — FOUND

Commits verified:
- `85c3f38` feat(03-04): implement SecureDataManagerImpl with full enforcement pipeline — FOUND
- `268ac56` feat(03-04): implement UnconstrainedDataManagerImpl as trusted bypass — FOUND
