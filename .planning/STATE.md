---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: unknown
stopped_at: Phase 4 context gathered
last_updated: "2026-03-21T15:56:01.752Z"
progress:
  total_phases: 5
  completed_phases: 3
  total_plans: 15
  completed_plans: 14
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-21)

**Core value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, row-level, and attribute-level access decisions.
**Current focus:** Phase 04 — protected-entity-proof

## Current Position

Phase: 04 (protected-entity-proof) — EXECUTING
Plan: 4 of 4

## Performance Metrics

**Velocity:**

- Total plans completed: 0
- Average duration: 0 min
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| - | - | - | - |

**Recent Trend:**

- Last 5 plans: none
- Trend: Stable

| Phase 01-identity-and-authority-baseline P01 | 13 | 2 tasks | 5 files |
| Phase 01-identity-and-authority-baseline P02 | 4 | 2 tasks | 4 files |
| Phase 02-security-metadata-management P01 | 5 | 3 tasks | 13 files |
| Phase 02-security-metadata-management P02 | 7 | 3 tasks | 10 files |
| Phase 03 P01 | 198 | 2 tasks | 28 files |
| Phase 03 P02 | 4 | 2 tasks | 16 files |
| Phase 03 P03 | 1 | 2 tasks | 2 files |
| Phase 03 P04 | 2 | 2 tasks | 2 files |
| Phase 03 P05 | 12 | 2 tasks | 11 files |
| Phase 04 P01 | 19 | 2 tasks | 10 files |
| Phase 04 P02 | 32 | 2 tasks | 11 files |
| Phase 04 P03 | 5 | 2 tasks | 6 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Phase 1: Preserve the current auth, account, admin-user, and authority behavior while bridging it into the merged security runtime.
- Phase 3: Enforce protected business data through a central secure data pipeline with YAML/code-defined fetch plans only.
- Phase 5: Build the new client as a standalone `frontend/` Angular app using `aef-main` as the structure reference.
- [Phase 01-identity-and-authority-baseline]: Unactivated user login returns 500 (UserNotActivatedException propagates through controller) - locked as baseline per D-13
- [Phase 01-identity-and-authority-baseline]: integrationTest Gradle task required explicit testClassesDirs + classpath for Gradle 9 NO-SOURCE fix
- [Phase 01-identity-and-authority-baseline]: JHipsterSecurityContextBridge uses @Component (not @Primary) so Phase 2 can provide @Primary override without modifying Phase 1 code
- [Phase 01-identity-and-authority-baseline]: SecurityContextBridge interface exposes Collection<String> raw authority names only - no typed AuthorityDescriptor per D-01/D-07
- [Phase 02-security-metadata-management]: SecPermission uses String authorityName FK (not @ManyToOne) to stay decoupled from Authority lifecycle and compatible with SecurityContextBridge Collection<String>
- [Phase 02-security-metadata-management]: RoleType enum placed in com.vn.core.domain (not security.domain) to avoid ArchUnit layer ambiguity
- [Phase 02-security-metadata-management]: SecPermission and SecRowPolicy have no @Cache annotation - admin-managed entities where stale cache would cause incorrect security decisions
- [Phase 02-security-metadata-management]: String types for enum fields in DTOs: keeps REST contract decoupled from entity enum changes; controllers convert String to enum at the service boundary
- [Phase 02-security-metadata-management]: MergedSecurityContextBridge is @Primary and filters phantom JWT authorities via authorityRepository.findAllById - Phase 3 programs against MergedSecurityService interface, not the bridge directly
- [Phase 03-secure-enforcement-core]: Protected entity access must go through `SecureDataManager`, with `UnconstrainedDataManager` as the explicit trusted bypass path and `loadByQuery` with parameters as the standard secured query entry point
- [Phase 03-secure-enforcement-core]: Row policies execute as DB-level constraints; supported runtime forms are `SPECIFICATION` plus controlled JPQL `WHERE` fragments with built-in security-context tokens only, and unsafe runtime application fails closed with security-style access denial
- [Phase 03-secure-enforcement-core]: The secured entity catalog is code-defined and allowlisted, may derive metadata from the JPA metamodel/entity scanner, and may use optional YAML only for labels/grouping/display hints - never to create new security targets
- [Phase 03-01]: AttributeAccessContext uses String action (not AttributeOp) to keep the field open-coded for downstream plans that map from String REST payloads
- [Phase 03-01]: RowLevelAccessContext uses List<Predicate> (not Specification<T>) to match plan spec and decouple row context from JPA Specification composition
- [Phase 03-01]: CrudEntityConstraint injects RolePermissionService.isEntityOpPermitted() as the single permission lookup point in the access pipeline
- [Phase 03-02]: AttributePermissionEvaluatorImpl uses permissive-default: empty permission list returns true (no rules = allowed) while entity-level evaluator uses DENY-default
- [Phase 03-02]: RowLevelPolicyProviderDbImpl is fail-closed: JAVA policyType and any unparseable SPECIFICATION/JPQL expression throw AccessDeniedException
- [Phase 03-02]: YamlFetchPlanRepository keyed as entityClassName.toLowerCase()#planName matching plan spec
- [Phase 03-02]: DefaultSecuredEntityCatalog returns empty list — Phase 4 provides @Primary override with real entity registrations
- [Phase 03-03]: SecureEntitySerializerImpl uses @Component (not @Service) per plan spec, consistent with phase 3 pattern
- [Phase 03-03]: SecureMergeServiceImpl skips id silently (not AccessDeniedException) - identity immutability is structural, not a permission violation
- [Phase 03-04]: SecureDataManagerImpl uses @SuppressWarnings(unchecked) for generic JPA repository casts — unavoidable due to type erasure in RepositoryRegistry generic signatures
- [Phase 03-04]: JPQL-to-Specification conversion deferred to Phase 4 — Phase 3 logs a warning and applies only row spec when JPQL is provided
- [Phase 03-05]: RowLevelSpecificationBuilder uses lambda no-op spec instead of Specification.where(null) — Java 25 added ambiguous overload and null-check enforcement

### Pending Todos

None yet.

### Blockers/Concerns

- Phase 5: Frontend capability mapping beyond coarse base authorities needs explicit design before UI rollout.

## Session Continuity

Last session: 2026-03-21T14:23:02.436Z
Stopped at: Phase 4 context gathered
Resume file: .planning/phases/04-protected-entity-proof/04-CONTEXT.md
