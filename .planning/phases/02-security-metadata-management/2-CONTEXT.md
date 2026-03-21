# Phase 2: Security Metadata Management - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver backend administration for the merged security metadata that drives runtime authorization decisions: roles (merged from `jhi_authority`), permission rules (entity CRUD and attribute view/edit), and row policies. Phase 2 produces stable CRUD contracts so Phase 3 can build enforcement against them. No enforcement logic, no frontend, no fetch-plan storage.

</domain>

<decisions>
## Implementation Decisions

### Role model — jhi_authority evolution (SEC-01)
- **D-01:** `jhi_authority` is the merged role table. No separate `sec_role` table. `jhi_authority.name` is the canonical role code (locked in Phase 1).
- **D-02:** Two new columns are added via Liquibase: `display_name VARCHAR(255)` (nullable, human-readable label) and `type VARCHAR(20) NOT NULL DEFAULT 'RESOURCE'` (RESOURCE or ROW_LEVEL, matching angapp's `SecRole.RoleType`).
- **D-03:** The `Authority` JPA entity gains `displayName` and `type` fields. `AuthorityDTO` or a new `SecRoleDTO` carries all three fields in REST responses.
- **D-04:** Existing `ROLE_ADMIN`, `ROLE_USER`, and `ROLE_ANONYMOUS` rows are migrated with `type = 'RESOURCE'` and `display_name = NULL` — no data loss.
- **D-05:** Admin CRUD endpoint for roles lives under `/api/admin/sec/roles` — separate from the existing JHipster `/api/admin/authorities` (which stays unchanged).

### Permission rules — sec_permission table (SEC-02)
- **D-06:** A new `sec_permission` table is created — a faithful port of angapp's `sec_permission` minus the Long role FK. Columns: `id`, `authority_name VARCHAR(50) FK → jhi_authority.name`, `target_type VARCHAR(30)`, `target VARCHAR(255)`, `action VARCHAR(50)`, `effect VARCHAR(10)`.
- **D-07:** Supported `target_type` values: `ENTITY`, `ATTRIBUTE`, `ROW_POLICY`. `FETCH_PLAN` is excluded (no database fetch-plan storage allowed).
- **D-08:** `effect` is `ALLOW` or `DENY`. DENY-wins is the locked semantic — deny on any matching rule blocks access regardless of allow rules. This is the angapp/Jmix contract.
- **D-09:** Admin CRUD endpoint lives under `/api/admin/sec/permissions`.
- **D-10:** `SecPermission` JPA entity with `SecPermissionRepository` lives in `com.vn.core.security.domain` and `com.vn.core.security.repository` respectively, mirroring angapp's package shape.

### Row policies — sec_row_policy table (SEC-03)
- **D-11:** A new `sec_row_policy` table is created as a verbatim port of angapp's schema: `id`, `code VARCHAR(100) UNIQUE`, `entity_name VARCHAR(255)`, `operation VARCHAR(20)`, `policy_type VARCHAR(20)`, `expression VARCHAR(1000)`.
- **D-12:** All three `policy_type` values are valid in the schema and admin API: `SPECIFICATION`, `JPQL`, `JAVA`. Phase 3 determines which are enforced and in what order — Phase 2 stores whatever is submitted.
- **D-13:** `SecRowPolicy` JPA entity and `SecRowPolicyRepository` live in `com.vn.core.security.domain` and `com.vn.core.security.repository`.
- **D-14:** Admin CRUD endpoint lives under `/api/admin/sec/row-policies`.

### SecurityContextBridge Phase 2 override
- **D-15:** Phase 2 delivers `MergedSecurityContextBridge` as `@Primary @Component` in `com.vn.core.security.bridge`. It supersedes `JHipsterSecurityContextBridge` without modifying it.
- **D-16:** `getCurrentUserAuthorities()` in the Phase 2 bridge loads authority names from JHipster's `Authentication` then validates them against `jhi_authority` (the merged role table). Only names that exist in `jhi_authority` are returned — stale or phantom roles are silently dropped.
- **D-17:** Phase 2 ports the angapp `SecurityService` interface into `com.vn.core.security` as `MergedSecurityService` (or a direct port). The Phase 2 bridge wires into this service rather than calling `SecurityUtils` directly, establishing the contract Phase 3 will program against.

### API conventions
- **D-18:** All three admin endpoints require `ROLE_ADMIN` authorization, consistent with existing JHipster admin endpoints.
- **D-19:** REST controllers live in `com.vn.core.web.rest.admin.security` — a sub-package that keeps security admin endpoints separate from account/user admin.
- **D-20:** DTOs live in `com.vn.core.service.dto.security` — `SecRoleDTO`, `SecPermissionDTO`, `SecRowPolicyDTO`. No direct entity exposure.

### Claude's Discretion
- Exact DTO field validation constraints (which fields are @NotNull, @Size, etc.)
- Whether partial-update (PATCH) endpoints are added alongside full-update (PUT) in Phase 2
- Enum representation in DTOs (String codes vs Java enums)
- Whether `SecRoleRepository` adds a `findByCode` alias for `findById` symmetry with angapp
- Test coverage shape: MockMvc unit tests vs Testcontainers integration tests per endpoint

</decisions>

<specifics>
## Specific Ideas

- angapp's DENY-wins evaluation: "DENY effect always blocks access; ALLOW must be present to grant permission" — this is a Jmix-style explicit-permission model, not a Spring Security role-hierarchy model. The database schema and service contracts must reflect this.
- angapp seeds three roles in Liquibase (`ACCOUNTANT_ROLE`, `STOCKKEEPER_ROLE`, `DIRECTOR_ROLE`) mapped to `jhi_authority`. Phase 2 should carry equivalent seed data to validate the admin APIs work against real rows, even if the entity names differ.
- The `authority_name` FK in `sec_permission` means deleting a role should cascade-delete its permissions. Decide: DB-level CASCADE or application-level guard.
- Phase 3 reads from the Phase 2 tables directly via `SecPermissionRepository` and `SecRowPolicyRepository` — the repository query shape matters for Phase 3. `SecPermissionRepository.findByRolesAndTarget(Collection<String>, TargetType, String, String)` is the expected query angapp provides.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase requirements and goals
- `.planning/REQUIREMENTS.md` §SEC-01, §SEC-02, §SEC-03 — the three requirements this phase must satisfy
- `.planning/ROADMAP.md` §Phase 2 — success criteria and dependency declaration

### Project constraints
- `.planning/PROJECT.md` §Constraints — brownfield safety rules; existing auth/account/admin flows must not regress

### Phase 1 outputs (integration points)
- `.planning/phases/01-identity-and-authority-baseline/1-CONTEXT.md` — locked decisions, bridge design intent, authority model constraints
- `src/main/java/com/vn/core/security/bridge/SecurityContextBridge.java` — the interface Phase 2 overrides with @Primary
- `src/main/java/com/vn/core/security/bridge/JHipsterSecurityContextBridge.java` — non-@Primary default; must not be modified

### angapp donor implementation
- `angapp/src/main/java/com/mycompany/core/security/domain/SecRole.java` — role entity shape to port
- `angapp/src/main/java/com/mycompany/core/security/domain/SecPermission.java` — permission entity shape to port
- `angapp/src/main/java/com/mycompany/core/security/domain/SecRowPolicy.java` — row policy entity shape to port
- `angapp/src/main/java/com/mycompany/core/security/repository/SecPermissionRepository.java` — query shape Phase 3 will depend on
- `angapp/src/main/java/com/mycompany/core/security/repository/SecRowPolicyRepository.java` — query shape Phase 3 will depend on
- `angapp/src/main/java/com/mycompany/core/security/core/SecurityService.java` — interface to port as MergedSecurityService
- `angapp/src/main/java/com/mycompany/core/security/core/SecurityServiceImpl.java` — implementation reference for MergedSecurityContextBridge
- `angapp/src/main/resources/config/liquibase/changelog/20260319000100_security_metadata.xml` — schema reference
- `angapp/src/main/resources/config/liquibase/changelog/20260319001000_security_seed_roles_permissions.xml` — seed data reference

### Codebase baseline
- `.planning/codebase/ARCHITECTURE.md` — layer map and ArchUnit rules; new security admin classes must comply
- `.planning/codebase/CONVENTIONS.md` — coding conventions to follow

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/com/vn/core/security/bridge/SecurityContextBridge.java`: Interface Phase 2 overrides with `@Primary MergedSecurityContextBridge`
- `src/main/java/com/vn/core/domain/Authority.java`: JPA entity to extend with `displayName` and `type` columns via Liquibase + entity field additions
- `src/main/java/com/vn/core/repository/AuthorityRepository.java`: Existing repo; Phase 2 may extend or supplement — do not break existing callers
- `src/main/java/com/vn/core/web/rest/AuthorityResource.java`: Existing authority admin endpoint at `/api/admin/authorities` — Phase 2 role admin goes to `/api/admin/sec/roles` as a separate resource, not a replacement
- `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`: Phase 2 CRUD endpoints must throw typed exceptions compatible with this translator

### Established Patterns
- `src/main/java/com/vn/core/web/rest/UserResource.java`: Admin endpoint pattern — `@PreAuthorize("hasAuthority('" + AuthoritiesConstants.ADMIN + "')")`, `ResponseEntity<T>`, `@Valid` on request bodies, RFC 7807 exceptions on conflicts
- `src/main/java/com/vn/core/service/dto/AdminUserDTO.java`: DTO pattern — constructor-mapped, validation annotations, no entity exposure
- `src/main/java/com/vn/core/service/mapper/UserMapper.java`: Hand-written mapper pattern; Phase 2 should use MapStruct interfaces extending `EntityMapper` (angapp pattern) for the new security DTOs

### Integration Points
- `jhi_authority` table: Liquibase changelog must add `display_name` and `type` columns; existing rows get default values without data loss
- `sec_permission.authority_name` FK → `jhi_authority.name`: referential integrity constraint; deletion of a role must handle cascade or guard
- `SecurityContextBridge` replacement: `MergedSecurityContextBridge` registered as `@Primary` in `com.vn.core.security.bridge` — Spring will auto-select it over the Phase 1 default

</code_context>

<deferred>
## Deferred Ideas

- `RolePermissionService` and enforcement-layer beans (`EntityPermissionEvaluator`, `AttributePermissionEvaluator`, `RowLevelPolicyProvider`) — Phase 3 builds enforcement on top of Phase 2's repository layer
- `SecureDataManager` and the central secured data pipeline — Phase 3
- `AccessManager` / `AccessConstraint` framework — Phase 3
- Row policy enforcement implementation for JPQL and JAVA policy types — Phase 3 (schema and admin API are delivered in Phase 2)
- `FetchPlan` YAML loading and `FetchPlanResolver` — Phase 3
- Frontend security administration screens — Phase 5

</deferred>

---

*Phase: 02-security-metadata-management*
*Context gathered: 2026-03-21*
