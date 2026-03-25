---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: "Phase 06 shipped — PR #2"
stopped_at: Ready to plan Phase 7
last_updated: "2026-03-25T04:50:33.008Z"
last_activity: 2026-03-25
progress:
  total_phases: 5
  completed_phases: 1
  total_plans: 6
  completed_plans: 6
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-25)

**Core value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, row-level, and attribute-level access decisions.
**Current focus:** Phase 07 - enterprise-navigation-shell

## Current Position

Phase: 07 (enterprise-navigation-shell) - READY TO PLAN
Plan: -

## Performance Metrics

**Velocity:**

- Total plans completed this milestone: 6
- Average duration: 8.3 min
- Total execution time: 0.8 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 6 - Frontend Parity Foundation | 6 | 50 min | 8.3 min |
| 7 - Enterprise Navigation Shell | 0 | - | - |
| 8 - User Management Delivery | 0 | - | - |
| 9 - Enterprise UX And Performance Hardening | 0 | - | - |
| 10 - Frontend Reliability And Regression Coverage | 0 | - | - |

**Recent Trend:**

- Phase 6 completed with green `frontend` production build and focused Angular regression coverage
- Standalone i18n, alerting, admin route foundations, and translated shell surfaces are now live in `frontend/`
- Next actionable work: plan and execute Phase 7

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
- [Phase 03-02]: DefaultSecuredEntityCatalog returns empty list - Phase 4 provides @Primary override with real entity registrations
- [Phase 03-03]: SecureEntitySerializerImpl uses @Component (not @Service) per plan spec, consistent with phase 3 pattern
- [Phase 03-03]: SecureMergeServiceImpl skips id silently (not AccessDeniedException) - identity immutability is structural, not a permission violation
- [Phase 03-04]: SecureDataManagerImpl uses @SuppressWarnings(unchecked) for generic JPA repository casts - unavoidable due to type erasure in RepositoryRegistry generic signatures
- [Phase 03-04]: JPQL-to-Specification conversion deferred to Phase 4 - Phase 3 logs a warning and applies only row spec when JPQL is provided
- [Phase 03-05]: RowLevelSpecificationBuilder uses lambda no-op spec instead of Specification.where(null) - Java 25 added ambiguous overload and null-check enforcement
- [Phase 04-04]: `TechnicalStructureTest` ignores `com.vn.core.security.catalog.SecuredEntity` so proof entity opt-in annotations do not violate the domain-layer ArchUnit rule
- [Phase 04-04]: `LiquibaseConfiguration` must honor `LiquibaseProperties.changeLog` so tests can use a dedicated `test-master.xml` overlay for proof security fixtures
- [Phase 05-01]: Attribute enumeration uses EntityManager.getMetamodel() sorted alphabetically - avoids reflection on entity class
- [Phase 05-01]: authorityName filter uses null/blank check - empty string treated as no-filter to prevent accidental empty-result queries
- [Phase 05]: Angular 21 uses vitest (not Karma) via @angular/build:unit-test; test commands use ng test --watch=false without --browsers flag
- [Phase 05]: ErrorHandlerInterceptor simplified to console.warn; NotificationInterceptor simplified - EventManager/AlertService not yet wired
- [Phase 05]: Capability payloads derive from SecuredEntityCatalog plus JPA metamodel enumeration. - Keeps organization, department, and employee gating aligned with the secured entity allowlist and backend attribute names.
- [Phase 05]: Frontend entity screens will reuse one cached capability response via shareReplay(1). - List, detail, and update screens can gate from one authenticated capability fetch instead of refetching per component.
- [Phase 05]: Matrix UI payloads stay on the locked GRANT contract while the backend normalizes to runtime ALLOW and canonical stored targets. - This fixes end-to-end permission enforcement without changing the frontend matrix model from D-23 and D-24.
- [Phase 05]: Protected-entity actions stay hidden until the shared capability response loads. - List, detail, and update screens should not briefly expose actions before permission state is known.
- [Phase 05]: Create and edit routes redirect to /accessdenied before rendering forms when capability denies access. - Route-level gating must happen before form controls or sensitive inputs appear on screen.
- [Phase 05]: Use (ngModelChange) not (onChange) for PrimeNG p-checkbox with binary mode and ngModel binding
- [Phase 05-standalone-frontend-delivery]: Navigate to /login explicitly in logout() to skip two 401 round-trips caused by navigating to guarded home route
- [Phase 05-standalone-frontend-delivery]: Root-scoped services holding per-user caches must subscribe to AccountService.getAuthenticationState() and reset cache on each emission
- [Phase 05-standalone-frontend-delivery]: AttributePermissionEvaluatorImpl uses deny-default (empty perms = false) because permission matrix stores only GRANT records; empty result means no GRANT was given
- [Phase 05-standalone-frontend-delivery]: canViewField() uses capabilityLoaded() gate for optimistic display before capability resolves, then applies fieldVisibility map
- [Phase 05]: sessionStorage as warm-start layer beneath in-memory shareReplay for entity capability cache
- [Milestone v1.1]: `angapp/` is the canonical donor for in-scope JHipster support files, translations, and user-management flows that must be migrated into `frontend/`
- [Milestone v1.1]: Menu visibility and route protection should move to backend-driven navigation and permission data rather than a hardcoded client menu model
- [Milestone v1.1]: Enterprise admin usability requires a structural shell and layout upgrade, not isolated page tweaks
- [Phase 06-frontend-parity-foundation]: Translation assets ship as static `frontend/public/i18n/en.json` and `frontend/public/i18n/vi.json` bundles; the old JHipster namespace merge build step stays retired
- [Phase 06-frontend-parity-foundation]: Stored locale in `StateStorageService` overrides the default `vi`; `account.langKey` is only a fallback when no locale has been stored
- [Phase 06-frontend-parity-foundation]: `frontend/src/app.routes.ts` is the authoritative root route tree and `frontend/src/app/app.routes.ts` now re-exports it for compatibility
- [Phase 06-frontend-parity-foundation]: Menu root items must track stable ids, not translated labels, to keep language switching change-detection safe

### Pending Todos

- Plan and execute Phase 7 - Enterprise Navigation Shell.

### Blockers/Concerns

- No open milestone blockers yet.
- Frontend production build still exceeds the configured initial bundle budget warning threshold (886.96 kB vs 500 kB warning).
- Planning debt remains on validation metadata for phases 1, 3, and 4; product verification and the milestone audit both passed.

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 260324-xae | Fix @SecuredEntity catalog and N+1 capability loading | 2026-03-24 | a223896 | [260324-xae-fix-securedentity-catalog-and-n-1-capabi](.planning/quick/260324-xae-fix-securedentity-catalog-and-n-1-capabi/) |
| 260325-0ze | append Angular/TypeScript agent instructions to CLAUDE.md and AGENTS.md | 2026-03-24 | c012a5d | [260325-0ze-append-angular-typescript-agent-instruct](.planning/quick/260325-0ze-append-angular-typescript-agent-instruct/) |

## Session Continuity

Last activity: 2026-03-25
Last session: 2026-03-25T11:34:39.7370232+07:00
Stopped at: Ready to plan Phase 7
Resume file: .planning/ROADMAP.md
