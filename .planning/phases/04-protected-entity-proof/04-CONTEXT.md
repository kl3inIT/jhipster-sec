# Phase 4: Protected Entity Proof - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Prove the merged security engine on sample entities with secured APIs and automated allow/deny coverage. Phase 4 introduces three sample entities forming a multi-level hierarchy (`Organization → Department → Employee`), registers them in the secured-entity catalog, wires their APIs through `SecureDataManager`, and proves all four enforcement dimensions (CRUD, attribute read, attribute write, row-level) in a dedicated integration test suite. Frontend proof (ENT-03) is Phase 5.

</domain>

<decisions>
## Implementation Decisions

### Sample entities
- **D-01:** Use three new simplified entities forming a `Organization → Department → Employee` hierarchy. Preferred over porting angapp entities directly, to keep the proof clean and isolated from angapp complexity.
- **D-02:** The entity chain must include at least one multi-level association (Organization has many Departments, Department has many Employees) to exercise recursive attribute filtering and reference traversal in `SecureEntitySerializerImpl`.
- **D-03:** New Liquibase changelogs added to the root app for the three sample tables. Entities live in `com.vn.core.domain` (or a `proof` sub-package under it) following the root app's package conventions.

### API surface
- **D-04:** Full layering per project conventions: `@RestController` → `@Service` → `SecureDataManager`. No thin controller-direct pattern.
- **D-05:** Minimal but realistic CRUD surface per entity: list, single read, save/update, and delete. No partial-update or extra endpoints needed.
- **D-06:** No DTO layer. Follow the Jmix style: the service returns the fetch-plan-shaped result (entity/graph shaped by `SecureDataManager`) and the controller returns it directly as JSON. No MapStruct mappers or typed DTO classes for these entities.
- **D-07:** REST endpoints require only `isAuthenticated()` at the controller level. All entity-level, row-level, and attribute-level access decisions are made by `SecureDataManager`.

### Secured entity catalog
- **D-08:** Phase 4 provides a `@Primary` implementation of `SecuredEntityCatalog` that discovers candidate entities from the live JPA runtime metamodel (`EntityManager.getMetamodel().getEntities()`), replacing the `DefaultSecuredEntityCatalog` empty placeholder without relying on a hand-built `List.of(...)` per proof class.
- **D-09:** Metamodel discovery is still fail-closed. The catalog filters scanned entities through an explicit secured-entity gate before exposing them to the engine. Preferred gate: a marker such as `@SecuredEntity` on the proof entities. Acceptable fallback: a local allowlist. Exposing every JPA `@Entity` automatically is explicitly rejected.
- **D-10:** `SecuredEntityCatalog` still owns runtime enforcement metadata after filtering. Approved entities are enriched into `SecuredEntityEntry` values with logical code, allowed operations, fetch-plan code(s), and `jpqlAllowed = false`. Discovery comes from metamodel scanning; security participation remains code-controlled.
- **D-11:** Fetch plans for the three entities are defined in `fetch-plans.yml` (YAML) and/or via `FetchPlanBuilder` (code). The currently empty `fetch-plans.yml` is populated in Phase 4.

### Row-policy scope
- **D-12:** Row policies for the proof are `SPECIFICATION` type only. JPQL-based row policy conversion remains deferred to a future phase.
- **D-13:** The existing JPQL stub in `SecureDataManagerImpl` (currently: logs warning and skips) is changed to fail-closed: throws `AccessDeniedException` when a stored `SecRowPolicy` of type `JPQL` is encountered and no conversion is available. This prevents unintentional access widening.

### Test strategy
- **D-14:** All allow/deny coverage lives in integration tests backed by Testcontainers + PostgreSQL (`DatabaseTestcontainer` already exists in the test tree). Unit tests alone cannot prove the full pipeline.
- **D-15:** Security context for tests: `@WithMockUser` provides the principal; Liquibase test fixtures (under `src/test/resources/config/liquibase/`) seed the `sec_permission` and `sec_row_policy` rows needed for each scenario.
- **D-16:** Coverage must prove all four enforcement dimensions:
  - CRUD: one allow case (principal has permission, reads/writes successfully) + one deny case (no permission, `AccessDeniedException` returned as 403)
  - Attribute read: denied field is absent from the JSON response
  - Attribute write: denied field in request body triggers `AccessDeniedException` (403), not silent strip
  - Row-level: row policy restricts which records appear in list results
- **D-17:** All security proof tests are in a dedicated `SecuredEntityEnforcementIT` rather than scattered across per-entity IT classes. This keeps the enforcement proof isolated and legible.

### Claude's Discretion
- Exact package for sample entities (`com.vn.core.domain` directly or a `proof` sub-package)
- Whether all three entities appear in `SecuredEntityEnforcementIT` or only one/two are needed to cover the full matrix
- Whether fetch plans are YAML-only, code-only, or mixed for the three entities
- Exact names for the `@Primary` catalog implementation class

</decisions>

<specifics>
## Specific Ideas

- The three-entity chain exists to force multi-level reference traversal through `SecureEntitySerializerImpl` — this is the key proof the recursive filtering works, not just flat field omission
- The intended catalog pipeline for the proof is `JPA Metamodel -> secured-entity filter -> SecuredEntityEntry enrichment -> runtime enforcement metadata lookup`
- If we want the opt-in to be visible on the entities themselves, a dedicated marker such as `@SecuredEntity` is the cleaner Jmix-style gate than a hidden class list
- Do not use `ClassPathScanningCandidateComponentProvider` or any raw classpath `@Entity` scan for the security catalog; the runtime source of truth must stay aligned with the JPA metamodel
- Row-policy proof should be a `SPECIFICATION` seeded via the catalog entry's inline spec, not via a `SecRowPolicy` DB row, if that simplifies test setup without losing coverage — though a DB-seeded row policy exercising the full `RowLevelPolicyProviderDbImpl` path is preferred
- The JPQL fail-closed change is a correctness fix, not a new feature — it should be a small targeted change to `SecureDataManagerImpl`

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase requirements and roadmap
- `.planning/REQUIREMENTS.md` — `ENT-01` and `ENT-02` define the sample-entity and test-coverage requirements for this phase
- `.planning/ROADMAP.md` — `Phase 4: Protected Entity Proof` defines the phase goal, dependency on Phase 3, and success criteria
- `.planning/PROJECT.md` — `Constraints` and `Core Value` lock fetch-plan YAML/code-only rule and the Jmix-style access philosophy

### Prior phase outputs
- `.planning/phases/03-secure-enforcement-core/03-CONTEXT.md` — locks the enforcement pipeline shape, `DefaultSecuredEntityCatalog` empty placeholder pattern, JPQL deferral, and `@Primary` override expectation
- `.planning/phases/03-secure-enforcement-core/03-VERIFICATION.md` — full list of Phase 3 artifacts and their locations; Phase 4 must build on these without modifying them except for the JPQL stub fix

### Root app enforcement components (Phase 3 outputs)
- `src/main/java/com/vn/core/security/data/SecureDataManager.java` — central secured data access interface Phase 4 services must use
- `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java` — contains the JPQL stub to be changed to fail-closed (D-12)
- `src/main/java/com/vn/core/security/catalog/SecuredEntityCatalog.java` — interface Phase 4 must provide a `@Primary` implementation of
- `src/main/java/com/vn/core/security/catalog/DefaultSecuredEntityCatalog.java` — the empty placeholder that Phase 4's `@Primary` replaces
- `src/main/java/com/vn/core/security/catalog/SecuredEntityEntry.java` — entry shape Phase 4 catalog must populate
- `src/main/java/com/vn/core/security/fetch/FetchPlanMetadataTools.java` — existing metadata helper; useful for property helpers, but not a substitute for JPA-metamodel-backed catalog discovery
- `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java` — loaded from `classpath:fetch-plans.yml`; Phase 4 populates the file
- `src/main/resources/fetch-plans.yml` — currently empty (`fetch-plans: []`); Phase 4 adds entries for sample entities
- `src/main/java/com/vn/core/security/row/RowLevelPolicyProviderDbImpl.java` — loads `SecRowPolicy` rows; SPECIFICATION type must work correctly for row-policy proof

### angapp donor patterns
- `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java` — reference for how a service integrates with `SecureDataManager` (fetch-plan codes, `loadOne`, map-to-response pattern)
- `angapp/src/main/java/com/mycompany/myapp/domain/Organization.java` — reference entity shape for the proof entities
- `angapp/src/main/java/com/mycompany/myapp/domain/Department.java` — reference entity shape
- `angapp/src/main/java/com/mycompany/myapp/domain/Employee.java` — reference entity shape
- `angapp/src/main/resources/fetch-plans.yml` — donor YAML fetch-plan format and examples

### Existing test infrastructure
- `src/test/java/com/vn/core/config/DatabaseTestcontainer.java` — Testcontainers PostgreSQL setup; Phase 4 ITs extend same pattern as Phase 1/2 ITs
- `src/test/java/com/vn/core/web/rest/UserResourceIT.java` — reference for IT structure, `@WithMockUser`, `MockMvc` usage, and `@AutoConfigureMockMvc`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/com/vn/core/security/data/SecureDataManager.java`: exposes `loadByQuery(SecuredLoadQuery)`, `save(...)`, `delete(...)` — Phase 4 services call these directly
- `src/main/java/com/vn/core/security/data/SecuredLoadQuery.java`: query object carrying `entityCode`, `jpql`, `parameters`, `pageable`, `sort`, `fetchPlanCode` — Phase 4 services construct these
- `jakarta.persistence.EntityManager` + JPA metamodel: the preferred runtime-safe way to discover managed entity types and attributes for the secured catalog
- `src/main/java/com/vn/core/security/fetch/FetchPlanBuilder.java` + `FetchPlans.java`: code-builder API if YAML alone is not sufficient for a given fetch plan
- `src/test/java/com/vn/core/config/DatabaseTestcontainer.java`: drop-in Testcontainers support for ITs
- `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`: the strongest reference for how a Phase 4 service should be structured

### Established Patterns
- `DefaultSecuredEntityCatalog` returns `List.of()` — Phase 4 `@Primary` override is the only catalog the engine will see at runtime
- Phase 3 already locked the rule that catalog metadata may be derived from the JPA metamodel only after an explicit allowlist or secured-entity filter; Phase 4 should preserve that shape
- Phase 2 established that `SecPermission` and `SecRowPolicy` have no cache annotation — seeded test fixture rows will always be fresh reads
- Root app ArchUnit rule (`TechnicalStructureTest`): `web` depends on `service`, `service` depends on `repository`/`security` — Phase 4 controller → service → `SecureDataManager` fits the allowed dependency chain
- `MergedSecurityContextBridge` is `@Primary` and the engine reads authorities through it — `@WithMockUser` roles must match authority names present in the seeded `SecPermission` rows

### Integration Points
- Phase 4 catalog `@Primary` replaces `DefaultSecuredEntityCatalog` without modifying it — Spring picks up the `@Primary` bean automatically
- The catalog implementation should inject `EntityManager`, read `getMetamodel().getEntities()`, filter to approved proof entities, then map them into `SecuredEntityEntry` values
- `YamlFetchPlanRepository` reads `fetch-plans.yml` at startup via `@PostConstruct`; Phase 4 entries populate this file
- `RowLevelPolicyProviderDbImpl` loads from `SecRowPolicyRepository` by entity name and operation — seeded test rows must use the correct entity name string matching `SecuredEntityEntry`
- `SecureDataManagerImpl.checkCrud()` throws `AccessDeniedException` when `CrudEntityContext.isPermitted()` is false; this is what the CRUD deny test must catch

</code_context>

<deferred>
## Deferred Ideas

- JPQL-to-Specification conversion for row policies — reserved for a future phase; token set (`{currentUserId}`, `{currentUserLogin}`) is already decided but not implemented
- Frontend proof of sample-entity allow/deny behavior (ENT-03) — Phase 5
- Generic admin catalog endpoint such as `/api/admin/sec/catalog/entities` for frontend browsing — future admin/frontend phase, not required for Phase 4 proof
- Row policies for `Department` and `Employee` specifically, if the full matrix is proven on `Organization` alone — Claude's discretion on which entities carry row policies
- Richer row-policy authoring (beyond SPECIFICATION type) — V2-02 in REQUIREMENTS.md

</deferred>

---

*Phase: 04-protected-entity-proof*
*Context gathered: 2026-03-21*
