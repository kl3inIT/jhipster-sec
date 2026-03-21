# Phase 3: Secure Enforcement Core - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement the central security-aware data access layer that all protected business-entity reads and writes must use. Phase 3 establishes the `SecureDataManager` / `UnconstrainedDataManager` split, secured `loadByQuery` with parameters, consistent CRUD + attribute + row-level enforcement, YAML/code-defined fetch-plan resolution, and a controlled secured-entity catalog for permission target selection. Phase 4 proves these behaviors on sample entities and secured APIs; Phase 3 defines and delivers the shared enforcement core only.

</domain>

<decisions>
## Implementation Decisions

### Data access abstraction
- **D-01:** Phase 3 introduces a central `SecureDataManager` as the standard access path for protected business entities. Protected services must use it instead of reading or writing directly through entity repositories.
- **D-02:** Phase 3 also introduces `UnconstrainedDataManager` as the explicit bypass path for trusted internal access. It is the only sanctioned way to skip enforcement intentionally.
- **D-03:** Secured reads standardize on `loadByQuery` with parameters as the main query entry point. The API should be query-object based rather than a loose argument bag, carrying the secured entity/catalog code, JPQL text, named parameters, pageable/sort data, and fetch-plan code.

### Enforcement points
- **D-04:** All protected read and write enforcement runs inside the central data-manager path, not in controllers or ad hoc service code.
- **D-05:** Secured read flow is ordered as: entity-level permission check -> row-policy-constrained query execution -> fetch-plan resolution -> attribute-filtered serialization.
- **D-06:** Secured write flow is ordered as: entity-level permission check -> row-policy-constrained target lookup -> attribute edit enforcement during merge -> persistence -> secure re-read through `SecureDataManager` for the returned view.

### Permission evaluation flow
- **D-07:** Phase 2's `DENY`-wins permission semantics remain locked for Phase 3.
- **D-08:** Enforcement is modeled around access contexts for CRUD, attribute, row-level, and secured fetch-plan application decisions, using `MergedSecurityService` as the runtime source for current authorities and security context values.
- **D-09:** Phase 3 does not reintroduce database-stored fetch-plan permissions. Fetch-plan use is controlled by the secured entity catalog plus YAML/code-defined plan registration, with attribute-level filtering still applied on the output.

### Row policy execution strategy
- **D-10:** Row policies must execute as database-level constraints, not in-memory post-filters, so reads, updates, deletes, and counts/page totals stay aligned.
- **D-11:** Phase 3 supports `SPECIFICATION` row policies and a controlled `JPQL` row-policy form. JPQL policies are stored as managed `WHERE`-style fragments only, not as full free-form queries.
- **D-12:** Runtime tokens for JPQL row policies are limited to built-in security-context values only, such as current user id or login. Request-supplied parameters are not part of the row-policy language.
- **D-13:** If a stored row policy cannot be applied safely at runtime, enforcement fails closed with a security-style access denial rather than ignoring the policy or widening access.
- **D-14:** `JAVA` row-policy execution is not part of Phase 3's enforcement scope.

### Attribute filtering strategy
- **D-15:** Secured read payloads silently omit attributes the current user cannot view.
- **D-16:** `id` remains readable when it is included in the fetch plan.
- **D-17:** Reference properties are traversed only when the association itself is viewable, and nested attributes are filtered recursively through the same rules.
- **D-18:** Unauthorized attribute updates are rejected during secure merge; they are not silently stripped on write.

### Secured entity catalog
- **D-19:** Phase 3 introduces a controlled secured-entity catalog as the source of truth for what entities, attributes, operations, and fetch plans participate in the security engine.
- **D-20:** Entity and attribute metadata may be derived from the JPA entity scanner / metamodel, but only after passing through an explicit secured-catalog allowlist. The system must not expose every scanned entity automatically.
- **D-21:** The secured catalog owns permission-target selection inputs and runtime lookup metadata, including allowed operations, fetch-plan codes, repository/query adapters, and attribute target names.
- **D-22:** Optional YAML may add presentation metadata and hints for catalog entries, such as labels, grouping, ordering, and display hints for attributes or fetch plans, but YAML must not define new security targets outside the code-defined secured catalog.

### the agent's Discretion
- Exact names and package layout for the secured query object and the `SecureDataManager` / `UnconstrainedDataManager` interfaces
- The concrete built-in token set for controlled JPQL row policies, as long as it stays limited to security-context values only
- Whether the secured catalog's optional YAML metadata is loaded via typed `@ConfigurationProperties`, a dedicated repository, or another app-owned config seam
- Whether `loadByQuery` returns maps directly or a secured intermediate view object that is serialized afterward, as long as the enforcement order above remains intact

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase requirements and roadmap
- `.planning/REQUIREMENTS.md` - `DATA-01`, `DATA-02`, `DATA-03`, `DATA-04`, `DATA-05` define the secure-enforcement requirements for this phase
- `.planning/ROADMAP.md` - `Phase 3: Secure Enforcement Core` defines the phase goal, dependency, and success criteria
- `.planning/PROJECT.md` - `Core Value`, `Context`, and `Constraints` lock the brownfield migration goals, Jmix-aligned security direction, and YAML/code-only fetch-plan rule

### Prior phase outputs
- `.planning/phases/01-identity-and-authority-baseline/1-CONTEXT.md` - locks the `SecurityContextBridge` seam and raw-authority contract that Phase 3 must build on through `MergedSecurityService`
- `.planning/phases/02-security-metadata-management/2-CONTEXT.md` - locks the merged role/permission/row-policy model, `DENY`-wins behavior, and the expectation that Phase 3 programs against `MergedSecurityService`

### Current repository enforcement seams
- `src/main/java/com/vn/core/security/MergedSecurityService.java` - current runtime security-service contract for authority and principal access
- `src/main/java/com/vn/core/security/MergedSecurityServiceImpl.java` - current bridge-backed implementation that Phase 3 enforcement layers build on
- `src/main/java/com/vn/core/security/repository/SecPermissionRepository.java` - repository query shape for role-permission lookup
- `src/main/java/com/vn/core/security/repository/SecRowPolicyRepository.java` - repository seam for row-policy lookup by entity and operation
- `src/main/java/com/vn/core/security/domain/SecRowPolicy.java` - stored row-policy fields and supported persisted type values
- `src/main/java/com/vn/core/security/permission/TargetType.java` - confirms the current repo intentionally excludes database fetch-plan target storage

### angapp donor implementation to preserve philosophically
- `angapp/src/main/java/com/mycompany/core/data/SecureDataManager.java` - central secured data-access contract
- `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java` - donor enforcement ordering for CRUD, row-policy, fetch-plan, serialization, and secure save/delete flows
- `angapp/src/main/java/com/mycompany/core/merge/SecureMergeService.java` - secure merge contract for write-time attribute enforcement
- `angapp/src/main/java/com/mycompany/core/merge/SecureMergeServiceImpl.java` - donor write-side attribute enforcement behavior
- `angapp/src/main/java/com/mycompany/core/serialize/SecureEntitySerializerImpl.java` - donor read-side attribute filtering and recursive reference shaping
- `angapp/src/main/java/com/mycompany/core/fetch/FetchPlanResolver.java` - secured fetch-plan resolution contract
- `angapp/src/main/java/com/mycompany/core/fetch/FetchPlanResolverImpl.java` - donor fetch-plan resolution flow
- `angapp/src/main/java/com/mycompany/core/fetch/FetchPlanRepository.java` - fetch-plan repository seam
- `angapp/src/main/java/com/mycompany/core/fetch/YamlFetchPlanRepository.java` - YAML-backed fetch-plan storage pattern to preserve
- `angapp/src/main/java/com/mycompany/core/fetch/FetchPlans.java` - code-builder fetch-plan API to preserve
- `angapp/src/main/resources/fetch-plans.yml` - donor YAML fetch-plan format and examples
- `angapp/src/main/java/com/mycompany/core/security/permission/RolePermissionService.java` - donor permission-evaluation abstraction
- `angapp/src/main/java/com/mycompany/core/security/permission/RolePermissionServiceDbImpl.java` - donor `DENY`-wins permission lookup behavior
- `angapp/src/main/java/com/mycompany/core/security/row/RowLevelPolicyProviderDbImpl.java` - donor row-policy loading/execution baseline
- `angapp/src/main/java/com/mycompany/core/repository/RepositoryRegistry.java` - donor repository-registry seam for central data-manager access

### Donor tests that pin intended behavior
- `angapp/src/test/java/com/mycompany/core/fetch/YamlFetchPlanRepositoryTest.java` - expected YAML fetch-plan resolution behavior
- `angapp/src/test/java/com/mycompany/core/fetch/FetchPlanBuilderTest.java` - expected code-builder fetch-plan composition behavior
- `angapp/src/test/java/com/mycompany/core/serialize/SecureEntitySerializerImplTest.java` - expected attribute-filtered secured serialization behavior
- `angapp/src/test/java/com/mycompany/core/security/permission/AttributePermissionEvaluatorImplTest.java` - expected normalized attribute-target permission behavior

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/com/vn/core/security/MergedSecurityService.java`: already provides the runtime seam for current authorities, login, and authentication state
- `src/main/java/com/vn/core/security/repository/SecPermissionRepository.java`: already exposes the authority/target/action lookup pattern needed for Phase 3 evaluators
- `src/main/java/com/vn/core/security/repository/SecRowPolicyRepository.java`: already exposes the entity + operation lookup seam for row-policy enforcement
- `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java`: strongest donor reference for central secured read/write orchestration
- `angapp/src/main/java/com/mycompany/core/merge/SecureMergeServiceImpl.java`: strongest donor reference for rejecting unauthorized attribute edits during merge
- `angapp/src/main/java/com/mycompany/core/serialize/SecureEntitySerializerImpl.java`: strongest donor reference for omitting unreadable fields while keeping `id`
- `angapp/src/main/java/com/mycompany/core/fetch/YamlFetchPlanRepository.java` and `angapp/src/main/java/com/mycompany/core/fetch/FetchPlans.java`: donor pattern for YAML + code-builder fetch plans
- `angapp/src/main/java/com/mycompany/core/repository/RepositoryRegistry.java`: donor pattern for central repository resolution inside the data manager

### Established Patterns
- Phase 2 already established `DENY`-wins, authority-name-based permission lookup, and DB-managed row-policy metadata
- The current root app still follows strict service/repository/web layering, so the secure data layer must fit inside `com.vn.core` without controller-side security branching
- The root repository intentionally excludes DB-stored fetch-plan permissions, so fetch-plan control must stay catalog/config-driven rather than metadata-table-driven
- Donor secured services shape DTOs from secure map payloads after `SecureDataManager` reads; Phase 4 sample entities should reuse that pattern

### Integration Points
- A new secure data package in the root app will sit between protected services and repositories, using the Phase 2 security metadata tables and `MergedSecurityService`
- The secured entity catalog will sit between the JPA metamodel/entity scanner and both runtime enforcement and future permission-target administration
- `UnconstrainedDataManager` provides the explicit internal bypass for trusted bootstrap, metadata, and maintenance flows that must not go through security enforcement
- Phase 4 protected sample entities and APIs are the first intended consumers of the Phase 3 enforcement core

</code_context>

<specifics>
## Specific Ideas

- Preserve the angapp / Jmix philosophy rather than collapsing back to ad hoc repository checks in controllers or services
- `loadByQuery` with parameters is the standard secured query entry point for protected reads
- `UnconstrainedDataManager` must exist as a named, explicit bypass path so trusted internal access is visible in code review
- The intended catalog pipeline is: JPA entity scanner / metamodel -> controlled secured-entity catalog allowlist -> permission-target selection and runtime enforcement metadata
- Optional YAML metadata is for presentation and hints only; it may not create new entities, attributes, or fetch-plan targets outside the code-defined secured catalog
- Controlled JPQL row policies are managed `WHERE` fragments with built-in security-context tokens only and fail closed on unsafe runtime application

</specifics>

<deferred>
## Deferred Ideas

- Full protected sample-entity implementation and allow/deny API proof - Phase 4
- Frontend permission-target browsing and security-management UX - Phase 5
- Database-stored fetch-plan definitions or DB-managed fetch-plan APPLY permissions - out of scope for v1 by project rule
- Request-parameter-driven row-policy language - out of scope for Phase 3
- Automatic exposure of every scanned JPA entity as a security target - explicitly rejected for Phase 3
- `JAVA` row-policy execution - future phase only if the project later proves a safe need for it

</deferred>

---

*Phase: 03-secure-enforcement-core*
*Context gathered: 2026-03-21*
