# Phase 4: Protected Entity Proof - Research

**Researched:** 2026-03-21
**Domain:** JPA entity wiring, secured data layer integration, Liquibase schema, Integration testing with Testcontainers
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Use three new simplified entities forming a `Organization → Department → Employee` hierarchy. Preferred over porting angapp entities directly.
- **D-02:** The entity chain must include at least one multi-level association (Organization has many Departments, Department has many Employees) to exercise recursive attribute filtering and reference traversal in `SecureEntitySerializerImpl`.
- **D-03:** New Liquibase changelogs added to the root app for the three sample tables. Entities live in `com.vn.core.domain` (or a `proof` sub-package under it).
- **D-04:** Full layering per project conventions: `@RestController` → `@Service` → `SecureDataManager`. No thin controller-direct pattern.
- **D-05:** Minimal but realistic CRUD surface per entity: list, single read, save/update, and delete. No partial-update or extra endpoints needed.
- **D-06:** No DTO layer. The service returns the fetch-plan-shaped result (entity/graph shaped by `SecureDataManager`) and the controller returns it directly as JSON. No MapStruct mappers or typed DTO classes for these entities.
- **D-07:** REST endpoints require only `isAuthenticated()` at the controller level. All entity-level, row-level, and attribute-level access decisions are made by `SecureDataManager`.
- **D-08:** Phase 4 provides a `@Primary` implementation of `SecuredEntityCatalog` that discovers candidate entities from the live JPA runtime metamodel (`EntityManager.getMetamodel().getEntities()`), replacing the `DefaultSecuredEntityCatalog` empty placeholder without relying on a hand-built `List.of(...)` per proof class.
- **D-09:** Metamodel discovery is still fail-closed. The catalog filters scanned entities through an explicit secured-entity gate before exposing them to the engine. Preferred gate: a marker such as `@SecuredEntity` on the proof entities. Acceptable fallback: a local allowlist. Exposing every JPA `@Entity` automatically is explicitly rejected.
- **D-10:** `SecuredEntityCatalog` still owns runtime enforcement metadata after filtering. Approved entities are enriched into `SecuredEntityEntry` values with logical code, allowed operations, fetch-plan code(s), and `jpqlAllowed = false`. Discovery comes from metamodel scanning; security participation remains code-controlled.
- **D-11:** Fetch plans for the three entities are defined in `fetch-plans.yml` (YAML) and/or via `FetchPlanBuilder` (code). The currently empty `fetch-plans.yml` is populated in Phase 4.
- **D-12:** Row policies for the proof are `SPECIFICATION` type only. JPQL-based row policy conversion remains deferred to a future phase.
- **D-13:** The existing JPQL stub in `SecureDataManagerImpl` (currently: logs warning and skips) is changed to fail-closed: throws `AccessDeniedException` when a stored `SecRowPolicy` of type `JPQL` is encountered and no conversion is available.
- **D-14:** All allow/deny coverage lives in integration tests backed by Testcontainers + PostgreSQL.
- **D-15:** Security context for tests: `@WithMockUser` provides the principal; Liquibase test fixtures (under `src/test/resources/config/liquibase/`) seed the `sec_permission` and `sec_row_policy` rows needed for each scenario.
- **D-16:** Coverage must prove all four enforcement dimensions: CRUD allow/deny, attribute read omission, attribute write rejection (403 not silent strip), row-level restriction.
- **D-17:** All security proof tests are in a dedicated `SecuredEntityEnforcementIT` class.

### Claude's Discretion

- Exact package for sample entities (`com.vn.core.domain` directly or a `proof` sub-package)
- Whether all three entities appear in `SecuredEntityEnforcementIT` or only one/two are needed to cover the full matrix
- Whether fetch plans are YAML-only, code-only, or mixed for the three entities
- Exact names for the `@Primary` catalog implementation class

### Deferred Ideas (OUT OF SCOPE)

- JPQL-to-Specification conversion for row policies
- Frontend proof of sample-entity allow/deny behavior (ENT-03) — Phase 5
- Row policies for `Department` and `Employee` specifically, if the full matrix is proven on `Organization` alone
- Richer row-policy authoring (beyond SPECIFICATION type) — V2-02
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| ENT-01 | Sample entities exist that can exercise CRUD, row-level, and attribute-level security end to end | Domain design, fetch plan wiring, secured catalog discovery and enrichment patterns |
| ENT-02 | Secured entity APIs have automated backend tests for allow and deny scenarios | Integration test infrastructure, `@WithMockUser` + Liquibase fixture patterns, MockMvc request/response assertions |
</phase_requirements>

---

## Summary

Phase 4 builds on the fully-verified Phase 3 enforcement core by introducing three real JPA entities (`Organization`, `Department`, `Employee`) that exercise every security dimension. The entities are wired through the existing `SecureDataManager` pipeline, discovered from the live JPA metamodel by a `@Primary` secured catalog implementation, filtered fail-closed to the approved proof targets, and proven by a dedicated integration test suite (`SecuredEntityEnforcementIT`).

The central challenge is not the enforcement logic (Phase 3 handled that) but correct wiring: the entities must be JPA-persisted with Liquibase-managed tables, their fetch plans must be correctly keyed in `fetch-plans.yml`, their repositories must extend both `JpaRepository` and `JpaSpecificationExecutor` so the `RepositoryRegistry` can find them, and the integration tests must seed permission and row-policy fixture rows that match exactly what the security evaluators query for (entity simple names, authority names present in `jhi_authority`).

The JPQL fail-closed fix to `SecureDataManagerImpl` is a one-line targeted change: the current `LOG.warn` path after the `!entry.jpqlAllowed()` guard needs to throw `AccessDeniedException` instead of continuing with only the row spec applied.

**Primary recommendation:** Stand up three lean entities with flat scalar fields plus the required associations, mark or allowlist them for the secured catalog, derive the catalog candidates from the JPA metamodel, populate `fetch-plans.yml`, and write the `SecuredEntityEnforcementIT` using the existing `@IntegrationTest` + `@AutoConfigureMockMvc` + `@WithMockUser` pattern with `@Sql` or `EntityManager`-seeded fixture data for `sec_permission` and `sec_row_policy`.

---

## Standard Stack

### Core (all present in codebase — no new dependencies needed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Data JPA (`JpaRepository` + `JpaSpecificationExecutor`) | via Spring Boot 4.0.3 | Entity persistence and specification-based queries for row policies | `RepositoryRegistry` requires both interfaces; `SecureDataManagerImpl` casts to both |
| Liquibase 5.0.1 | existing | Schema management for new tables | All DB schema changes go through Liquibase per project convention |
| Testcontainers PostgreSQL | existing (`DatabaseTestcontainer.java`) | Real PostgreSQL in integration tests | Phase 3 decisions locked this as the only acceptable test persistence |
| Spring Security Test (`@WithMockUser`) | existing | Principal injection in integration tests | Used throughout existing IT suite; matches `MergedSecurityContextBridge` auth extraction |
| Spring Boot Test (`@SpringBootTest`, `MockMvc`, `@AutoConfigureMockMvc`) | existing | Full-stack IT request/response testing | Pattern established by `UserResourceIT` |
| Jackson (ObjectMapper) | existing | Request/response JSON handling in tests | Used in `UserResourceIT` for body construction |

### No New Dependencies Required

All libraries needed for Phase 4 are already declared in `build.gradle`. No new `implementation` or `testImplementation` entries are expected.

**Version verification:** All libraries confirmed present through existing codebase inspection. No npm packages involved.

---

## Architecture Patterns

### Recommended Project Structure

```
src/main/java/com/vn/core/
├── domain/
│   ├── proof/               # (optional sub-package — Claude's discretion)
│   │   ├── Organization.java
│   │   ├── Department.java
│   │   └── Employee.java
├── repository/
│   ├── proof/               # (optional sub-package)
│   │   ├── OrganizationRepository.java
│   │   ├── DepartmentRepository.java
│   │   └── EmployeeRepository.java
├── service/
│   ├── proof/
│   │   ├── OrganizationService.java
│   │   ├── DepartmentService.java
│   │   └── EmployeeService.java
├── web/rest/
│   ├── proof/
│   │   ├── OrganizationResource.java
│   │   ├── DepartmentResource.java
│   │   └── EmployeeResource.java
└── security/
    └── catalog/
        └── MetamodelSecuredEntityCatalog.java   # @Primary, EntityManager-backed
src/main/resources/
└── fetch-plans.yml                    # Populated with 3-entity plans
src/main/resources/config/liquibase/
├── master.xml                         # Updated to include new changelogs
└── changelog/
    ├── 20260321000500_create_organization.xml
    ├── 20260321000600_create_department.xml
    └── 20260321000700_create_employee.xml
src/test/java/com/vn/core/
└── security/
    └── proof/
        └── SecuredEntityEnforcementIT.java
```

### Pattern 1: Lean Proof Entity

**What:** A plain JPA entity with a Long id, 2-3 scalar fields, and (for Organization and Department) a `@OneToMany` collection to exercise recursive serialization. No Hazelcast `@Cache` annotation — these are proof entities, not production entities where cache staleness matters.
**When to use:** For all three proof entities.

```java
// Source: angapp Organization.java + root domain/User.java patterns
@Entity
@Table(name = "proof_organization")
public class Organization implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "budget")
    private BigDecimal budget;   // deliberately sensitive field for attribute-level deny test

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Department> departments = new ArrayList<>();

    // getters, setters, equals/hashCode on id
}
```

Key point: Include at least one "sensitive" field per entity (e.g., `budget` on Organization, `salary` on Employee) that can be DENY'd at attribute level in the IT.

### Pattern 2: Repository Must Extend Both Interfaces

**What:** Every proof entity repository must extend both `JpaRepository` and `JpaSpecificationExecutor` because `RepositoriesRegistryImpl` casts to both when executing row-policy constrained queries.
**When to use:** Mandatory for all three entity repositories.

```java
// Source: Phase 3 RepositoriesRegistryImpl analysis
public interface OrganizationRepository
        extends JpaRepository<Organization, Long>,
                JpaSpecificationExecutor<Organization> {
}
```

If a repository is missing `JpaSpecificationExecutor`, `RepositoriesRegistryImpl.getSpecificationExecutor()` will throw `IllegalArgumentException` at runtime and the secured read will fail.

### Pattern 3: No-DTO Service — Returns Map Directly

**What:** Phase 4 services do not have a DTO class. They construct a `SecuredLoadQuery`, call `secureDataManager.loadByQuery(query)`, and return the `Page<Map<String, Object>>` or `Map<String, Object>` directly to the controller.
**When to use:** All three entity services, per D-06.

```java
// Source: CONTEXT.md D-06, angapp OrganizationServiceImpl.java reference pattern
@Service
@Transactional
public class OrganizationService {

    private static final String LIST_PLAN = "organization-list";
    private static final String DETAIL_PLAN = "organization-detail";

    private final SecureDataManager secureDataManager;

    public Page<Map<String, Object>> list(Pageable pageable) {
        SecuredLoadQuery query = SecuredLoadQuery.of("organization", LIST_PLAN, pageable);
        return secureDataManager.loadByQuery(query);
    }

    public Map<String, Object> getOne(Long id) {
        // SecureDataManager.save() with null id creates; non-null id updates
        // For single read, use loadByQuery with a pageable of size 1 or a dedicated loadOne
        // Since Phase 3 SecureDataManager only exposes loadByQuery (not loadOne),
        // single-read returns page and the controller extracts the single result
        SecuredLoadQuery query = SecuredLoadQuery.of("organization", DETAIL_PLAN,
            PageRequest.of(0, 1));
        // NOTE: Single entity fetch strategy — see Pitfall 2 below
        return secureDataManager.loadByQuery(query)
            .getContent().stream().findFirst()
            .orElseThrow(() -> new AccessDeniedException("Not found or denied"));
    }

    public Map<String, Object> create(Map<String, Object> attributes) {
        return secureDataManager.save("organization", null, attributes, DETAIL_PLAN);
    }

    public Map<String, Object> update(Long id, Map<String, Object> attributes) {
        return secureDataManager.save("organization", id, attributes, DETAIL_PLAN);
    }

    public void delete(Long id) {
        secureDataManager.delete("organization", id);
    }
}
```

### Pattern 4: Controller Returns Map/Page Directly

**What:** Controller receives `Map<String, Object>` or `Page<Map<String, Object>>` from the service and returns `ResponseEntity<Map<String, Object>>` or `ResponseEntity<List<Map<String, Object>>>`.
**When to use:** All three entity REST controllers, per D-06 and D-07.

```java
// Source: CONTEXT.md D-06, D-07
@RestController
@RequestMapping("/api/proof/organizations")
public class OrganizationResource {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> list(Pageable pageable) {
        Page<Map<String, Object>> page = organizationService.list(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getOne(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> attributes) {
        Map<String, Object> result = organizationService.create(attributes);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id, @RequestBody Map<String, Object> attributes) {
        return ResponseEntity.ok(organizationService.update(id, attributes));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Pattern 5: @Primary Metamodel-Backed Catalog Implementation

**What:** A `@Component @Primary` class implementing `SecuredEntityCatalog` that overrides `DefaultSecuredEntityCatalog`, reads managed entities from `EntityManager.getMetamodel().getEntities()`, filters them through an explicit secured-entity gate, then enriches the approved proof entities into `SecuredEntityEntry` values.
**When to use:** Single catalog implementation for Phase 4 proof entities when discovery must stay aligned with the JPA runtime model.

```java
// Source: Phase 3 DefaultSecuredEntityCatalog.java, CONTEXT.md D-08 through D-10
@Component
@Primary
public class MetamodelSecuredEntityCatalog implements SecuredEntityCatalog {

    private final EntityManager entityManager;

    public MetamodelSecuredEntityCatalog(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<SecuredEntityEntry> entries() {
        return entityManager
            .getMetamodel()
            .getEntities()
            .stream()
            .filter(this::isSecuredProofEntity)
            .map(this::toEntry)
            .toList();
    }

    @Override
    public Optional<SecuredEntityEntry> findByEntityClass(Class<?> entityClass) {
        return entries().stream().filter(e -> e.entityClass().equals(entityClass)).findFirst();
    }

    @Override
    public Optional<SecuredEntityEntry> findByCode(String code) {
        return entries().stream().filter(e -> e.code().equals(code)).findFirst();
    }

    private boolean isSecuredProofEntity(EntityType<?> entityType) {
        Class<?> javaType = entityType.getJavaType();
        return javaType.isAnnotationPresent(SecuredEntity.class);
    }

    private SecuredEntityEntry toEntry(EntityType<?> entityType) {
        Class<?> javaType = entityType.getJavaType();
        String code = javaType.getSimpleName().toLowerCase(Locale.ROOT);
        List<String> fetchPlans = switch (code) {
            case "organization" -> List.of("organization-list", "organization-detail");
            case "department" -> List.of("department-list");
            case "employee" -> List.of("employee-list");
            default -> List.of();
        };

        return SecuredEntityEntry.builder()
            .entityClass(javaType)
            .code(code)
            .operations(EnumSet.of(EntityOp.READ, EntityOp.CREATE, EntityOp.UPDATE, EntityOp.DELETE))
            .fetchPlanCodes(fetchPlans)
            .jpqlAllowed(false)
            .build();
    }
}
```

If a dedicated `@SecuredEntity` marker feels like unnecessary ceremony for Phase 4, replace `isAnnotationPresent(...)` with a small local allowlist. The critical point is unchanged: discover through the JPA metamodel, then filter fail-closed before creating any security target.

### Pattern 6: fetch-plans.yml Population

**What:** Add fetch plan entries for the three entities to `src/main/resources/fetch-plans.yml`.
**When to use:** Phase 4 populates this file from empty (`fetch-plans: []`) to contain real plans.

```yaml
# Source: angapp fetch-plans.yml format (confirmed working with YamlFetchPlanRepository)
# Key format understood: entityClassName.toLowerCase()#planName
fetch-plans:
  - entity: com.vn.core.domain.proof.Organization
    name: organization-list
    properties:
      - id
      - code
      - name

  - entity: com.vn.core.domain.proof.Organization
    name: organization-detail
    properties:
      - id
      - code
      - name
      - budget          # sensitive field — tested with DENY attribute permission
      - name: departments
        fetchPlan: department-list

  - entity: com.vn.core.domain.proof.Department
    name: department-list
    properties:
      - id
      - code
      - name
      - name: employees
        fetchPlan: employee-list

  - entity: com.vn.core.domain.proof.Employee
    name: employee-list
    properties:
      - id
      - code
      - name
      - salary          # sensitive field — tested with DENY attribute permission
```

**CRITICAL:** The `entity` field in the YAML must match `entityClass.getName().toLowerCase(Locale.ROOT)` in `YamlFetchPlanRepository.findByEntityAndName()`. If entities live in `com.vn.core.domain.proof` package, the YAML must use that full class name.

### Pattern 7: Integration Test Structure

**What:** `SecuredEntityEnforcementIT` uses the existing `@IntegrationTest` composite annotation, `@AutoConfigureMockMvc`, `@WithMockUser` for the default principal, and per-test `@WithMockUser` overrides for different scenarios. Fixture data is inserted via `EntityManager` in `@BeforeEach` or via `@Sql` annotations.
**When to use:** All enforcement dimension tests live here.

```java
// Source: UserResourceIT.java pattern + CONTEXT.md D-13, D-14, D-16
@AutoConfigureMockMvc
@IntegrationTest
class SecuredEntityEnforcementIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private ObjectMapper om;

    @BeforeEach
    @Transactional
    void seedFixtures() {
        // 1. Seed jhi_authority row for test role (required by MergedSecurityContextBridge
        //    phantom-filter — @WithMockUser roles must exist in jhi_authority or they're stripped)
        Authority testRole = new Authority();
        testRole.setName("ROLE_TEST_VIEWER");
        em.persist(testRole);

        // 2. Seed sec_permission: ALLOW ENTITY READ for ROLE_TEST_VIEWER
        SecPermission allowRead = new SecPermission()
            .authorityName("ROLE_TEST_VIEWER")
            .targetType(TargetType.ENTITY)
            .target("ORGANIZATION")
            .action("READ")
            .effect("ALLOW");
        em.persist(allowRead);

        // 3. Seed sec_permission: DENY attribute VIEW for budget
        SecPermission denyBudget = new SecPermission()
            .authorityName("ROLE_TEST_VIEWER")
            .targetType(TargetType.ATTRIBUTE)
            .target("ORGANIZATION.BUDGET")
            .action("VIEW")
            .effect("DENY");
        em.persist(denyBudget);

        // 4. Seed sec_row_policy: SPECIFICATION type, restrict by owner field
        SecRowPolicy rowPolicy = new SecRowPolicy()
            .code("org-owner-policy")
            .entityName("Organization")     // matches entityClass.getSimpleName()
            .operation("READ")
            .policyType("SPECIFICATION")
            .expression("owner = CURRENT_USER_LOGIN");
        em.persist(rowPolicy);

        // 5. Seed Organization rows with owner field set
        Organization owned = new Organization();
        owned.setCode("ORG-A");
        owned.setName("Owned Org");
        owned.setOwner("user");    // @WithMockUser default username is "user"
        em.persist(owned);

        Organization notOwned = new Organization();
        notOwned.setCode("ORG-B");
        notOwned.setName("Not Owned Org");
        notOwned.setOwner("other-user");
        em.persist(notOwned);

        em.flush();
    }

    @Test
    @WithMockUser(authorities = "ROLE_TEST_VIEWER")
    void testCrudAllow_organizationListReturnsOk() throws Exception {
        mockMvc.perform(get("/api/proof/organizations")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_NO_PERMISSION")
    void testCrudDeny_organizationListReturns403() throws Exception {
        mockMvc.perform(get("/api/proof/organizations")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_TEST_VIEWER")
    void testAttributeRead_budgetAbsentFromResponse() throws Exception {
        mockMvc.perform(get("/api/proof/organizations")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].budget").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "ROLE_TEST_VIEWER")
    void testRowPolicy_onlyOwnedOrgsVisible() throws Exception {
        mockMvc.perform(get("/api/proof/organizations")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].code").value("ORG-A"));
    }
}
```

### Anti-Patterns to Avoid

- **Missing `JpaSpecificationExecutor` on repository:** `RepositoriesRegistryImpl.getSpecificationExecutor()` will throw at runtime. Every proof entity repository must extend both interfaces.
- **YAML entity name mismatched to class name:** `YamlFetchPlanRepository` keys on `entityClass.getName().toLowerCase()`. If the entity is in package `com.vn.core.domain.proof`, the YAML `entity` field must be `com.vn.core.domain.proof.Organization` not `Organization`. Using the wrong name causes `FetchPlanResolver` to throw `IllegalArgumentException` on startup.
- **@WithMockUser authority not in jhi_authority table:** `MergedSecurityContextBridge` does a phantom filter via `authorityRepository.findAllById(jwtAuthorities)`. If `ROLE_TEST_VIEWER` is not in the `jhi_authority` table, the authority is stripped and the user appears to have no roles — all permission checks fail unexpectedly. Fixture setup must insert the authority row.
- **SecPermission target format wrong:** `AttributePermissionEvaluatorImpl` normalizes to `ENTITYCLASS.ATTRIBUTE` both uppercased: `entityClass.getSimpleName().toUpperCase() + "." + attribute.toUpperCase()`. A fixture row with `target = "Organization.budget"` will never match; it must be `target = "ORGANIZATION.BUDGET"`.
- **SecRowPolicy entityName not matching:** `RowLevelPolicyProviderDbImpl` queries by `entityName` which is the string passed to `findByEntityNameAndOperation`. The string must match `entityClass.getSimpleName()` as used in the `SecureDataManagerImpl` row-spec build path. The entity simple name is what matters, not the fully-qualified name.
- **No `@Transactional` on test fixture setup:** Without transaction management the `em.flush()` will not persist fixture rows before the test runs. Either use `@Transactional` on the test method or wrap setup in a `@BeforeEach` with explicit `@Transactional`.
- **Write test expects silent strip:** D-15 and Phase 3 decision `SecureMergeServiceImpl` throws `AccessDeniedException` (not silent strip) on denied attributes. The attribute-write deny test must assert `status().isForbidden()` (403), not a successful response with the field absent.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Secured reads with permission checks | Custom service filter | `SecureDataManager.loadByQuery(SecuredLoadQuery)` | Full pipeline: CRUD check + row policy + fetch plan + attribute filter, all in one call |
| Attribute filtering on serialization | Custom JSON serializer | `SecureEntitySerializer` (called by `SecureDataManager`) | Handles recursive collection traversal and always-visible `id` behavior |
| Row policy specification composition | Custom `Specification` builder | `RowLevelSpecificationBuilder.build()` (called by `SecureDataManager`) | Loads from DB, AND-composes, fail-closed on invalid policies |
| Permission lookup | Custom `SecPermission` query | `RolePermissionService.isEntityOpPermitted()` (called by `CrudEntityConstraint`) | DENY-wins semantics, handles empty authority list |
| Fetch plan loading | Code-only fetch plan | Populate `fetch-plans.yml` | `YamlFetchPlanRepository` already loads from classpath; YAML is the declared standard |
| Dynamic repository lookup | `@Autowired` per entity repository in enforcement code | `RepositoryRegistry.getRepository()` / `getSpecificationExecutor()` | Proof entity services use `SecureDataManager` which uses `RepositoryRegistry` internally |

**Key insight:** The entire enforcement pipeline is wired in Phase 3. Phase 4's job is to provide real entities, metamodel-filtered catalog entries, fetch plans, and REST endpoints — not to re-implement any enforcement logic.

---

## Common Pitfalls

### Pitfall 1: Authority Phantom Filter Breaks Test Isolation

**What goes wrong:** A test uses `@WithMockUser(authorities = "ROLE_TEST_VIEWER")` but no `jhi_authority` row exists for `ROLE_TEST_VIEWER`. `MergedSecurityContextBridge.getCurrentUserAuthorities()` calls `authorityRepository.findAllById(jwtAuthorities)` and the role is absent from `jhi_authority`, so the returned collection is empty. All permission checks then fail (entity permission evaluator uses deny-by-default on empty authority set). The CRUD deny test and CRUD allow test both return 403, making it impossible to distinguish real deny from accidental authority stripping.

**Why it happens:** `MergedSecurityContextBridge` was designed for production correctness (prevent phantom JWT roles) but test fixture setup that doesn't include `jhi_authority` rows silently strips the test authority.

**How to avoid:** Always `em.persist()` an `Authority` row for every `@WithMockUser` authority name used in allow-path tests. Deny-path tests with nonexistent roles are actually fine for testing absence of permission, but the fixture must be deterministic about which path is being tested.

**Warning signs:** CRUD allow test returns 403 even with a seeded `ALLOW` `sec_permission` row.

### Pitfall 2: Single-Entity Read Via loadByQuery

**What goes wrong:** `SecureDataManager` exposes only `loadByQuery` (returns `Page`), not a `loadOne` by ID. A naive service that does `loadByQuery` without filtering by ID returns all rows visible to the user, not the requested single entity. The controller's `GET /{id}` endpoint returns multiple records or the wrong record.

**Why it happens:** Phase 3 `SecureDataManager` was intentionally kept lean — `loadByQuery` with a `SecuredLoadQuery` is the entry point. Single-entity reads need the caller to add an ID predicate.

**How to avoid:** For `GET /{id}`, either: (a) use `secureDataManager.save(entityCode, id, emptyMap, planCode)` which does a row-constrained update check and is not ideal, or (b) load by ID through a combination of row-constrained spec and the repository spec executor directly after CRUD check (acceptable since the service owns the logic), or (c) use the `SecuredLoadQuery` with a `Specification` — note Phase 3 `SecuredLoadQuery` does not carry a `Specification` field directly. The practical pattern is to use `secureDataManager.loadByQuery()` with pagination and trust that the caller filters using a JPQL (with `jpqlAllowed=true`) or via separate repository call after checking permissions. Given `jpqlAllowed=false` for proof entities (D-10), the cleanest approach is to expose a service method that calls `checkCrud` + `rowSpec` then fetches by ID using the entity's own repository directly, without re-implementing the full pipeline. Alternatively, add an `id` filter using the spec executor pattern after calling `checkCrud` manually.

**Recommended approach:** Delegate single-entity read entirely to `secureDataManager.loadByQuery()` with a limit-1 approach after accepting this is a design boundary — or do a targeted call through the entity's repository with a row-policy spec applied. Document the chosen approach clearly.

**Warning signs:** `GET /api/proof/organizations/1` returns 200 with multiple organization records.

### Pitfall 3: fetch-plans.yml Key Format

**What goes wrong:** `YamlFetchPlanRepository.findByEntityAndName()` builds key as `entityClass.getName().toLowerCase(Locale.ROOT) + "#" + name`. If the `entity` field in YAML does not exactly match the full class name (case-insensitively), the plan is registered under a different key than the resolver looks up. `FetchPlanResolverImpl` throws `IllegalArgumentException("Fetch plan not found")` at runtime, crashing every secured read.

**Why it happens:** The YAML must use the fully-qualified class name (e.g., `com.vn.core.domain.proof.Organization`), not a short name or simple name.

**How to avoid:** Verify the exact class name after deciding the package. If entities live in `com.vn.core.domain` (not a sub-package), use `com.vn.core.domain.Organization`.

**Warning signs:** Application starts but the first secured read throws `IllegalArgumentException: Fetch plan not found for entity...`.

### Pitfall 4: ArchUnit Layer Violation from Proof Classes

**What goes wrong:** `TechnicalStructureTest` enforces `web` depends on `service`, `service` depends on `repository`/`security`. If proof entities in `com.vn.core.domain.proof` are accessed directly from a `web.rest.proof` controller bypassing the service layer, ArchUnit will fail the build.

**Why it happens:** ArchUnit analyzes all classes in the `com.vn.core` package tree, including `proof` sub-packages.

**How to avoid:** Always follow the full `@RestController → @Service → SecureDataManager` pattern (D-04). The controller only depends on the service; the service holds `SecureDataManager`. No direct repository injection in the controller.

**Warning signs:** `./gradlew test` fails with ArchUnit violations after adding proof classes.

### Pitfall 5: @Transactional Lazy Loading in Tests

**What goes wrong:** The `SecureEntitySerializerImpl` accesses collection properties (e.g., `organization.getDepartments()`) via `BeanWrapperImpl.getPropertyValue(attr)`. If the entity was loaded outside a transaction (e.g., in a test's `@BeforeEach` without `@Transactional`), accessing the lazy collection throws `LazyInitializationException`.

**Why it happens:** Phase 3 serializer uses `BeanWrapper` to reflectively access properties. Collections are lazy by default in JPA.

**How to avoid:** Mark `@BeforeEach` fixture methods with `@Transactional`, or use `@OneToMany(fetch = FetchType.EAGER)` on proof entities where the test exercises association traversal. Alternatively, ensure all test reads go through MockMvc (which executes in the full Spring transaction context of the handler).

**Warning signs:** `LazyInitializationException` in serializer during IT execution.

### Pitfall 6: JPQL Fail-Closed Change Scope

**What goes wrong:** D-12 requires changing the `SecureDataManagerImpl` JPQL stub from "log warning and continue" to "throw `AccessDeniedException`". The current code at lines 95-103 is inside an `if (entry.jpqlAllowed())` block — actually it's inside the `if (query.jpql() != null && !query.jpql().isBlank())` block after the `!entry.jpqlAllowed()` guard. Reading the code carefully: if `jpqlAllowed = false` and a JPQL query is provided, it already throws `AccessDeniedException` (line 93). The stub is only reached when `jpqlAllowed = true` but JPQL-to-Spec conversion is not yet implemented. The fix is specifically for `SecRowPolicy` rows with `policyType = JPQL` in `RowLevelPolicyProviderDbImpl`. Actually rereading: `RowLevelPolicyProviderDbImpl.parseJpqlPolicy()` already handles JPQL policies by converting simple `field = value` patterns. The D-12 change described in CONTEXT.md targets `SecureDataManagerImpl` JPQL query path, not the row policy path. The targeted fix must not disrupt the `RowLevelPolicyProviderDbImpl` JPQL handling which is already operational.

**How to avoid:** Make the JPQL stub fix in `SecureDataManagerImpl` only — the block beginning at "Phase 3: JPQL-to-Specification conversion is not yet implemented" (lines 95-102). Change the `LOG.warn(...)` + implicit continue to `throw new AccessDeniedException(...)`. This is a 1-line change in the warn block.

---

## Code Examples

### Liquibase Changelog Template for Proof Entity

```xml
<!-- Source: existing changelog pattern from 20260321000200_create_sec_permission.xml -->
<?xml version="1.0" encoding="utf-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="20260321000500-1" author="dev">
        <createTable tableName="proof_organization">
            <column name="id" type="bigint" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="code" type="varchar(100)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="name" type="varchar(255)">
                <constraints nullable="false"/>
            </column>
            <column name="budget" type="decimal(19,2)"/>
            <column name="owner" type="varchar(50)"/>
        </createTable>
    </changeSet>
</databaseChangeLog>
```

Prefix tables with `proof_` to avoid naming collisions with existing tables. The `owner` column is needed for the SPECIFICATION row policy test (`owner = CURRENT_USER_LOGIN`).

### sec_permission Target Format Reference

```
ENTITY permission:    target = "ORGANIZATION",           action = "READ",  targetType = ENTITY
ATTRIBUTE permission: target = "ORGANIZATION.BUDGET",    action = "VIEW",  targetType = ATTRIBUTE
ATTRIBUTE permission: target = "ORGANIZATION.BUDGET",    action = "EDIT",  targetType = ATTRIBUTE
```

Source: `AttributePermissionEvaluatorImpl` line 48-50 confirms uppercased `ENTITYNAME.ATTRIBUTENAME` format.
Source: `RolePermissionServiceDbImpl` confirms `TargetType.ENTITY` and entity class `getSimpleName().toUpperCase()` for CRUD target.

### CRUD Permission Lookup Target for Entity

```java
// Source: CrudEntityConstraint.java → RolePermissionService.isEntityOpPermitted()
// → RolePermissionServiceDbImpl line using entityClass.getSimpleName().toUpperCase()
// Resulting sec_permission row needed:
// authority_name = "ROLE_TEST_VIEWER"
// target_type    = "ENTITY"
// target         = "ORGANIZATION"     ← entityClass.getSimpleName().toUpperCase()
// action         = "READ"             ← EntityOp.READ.name()
// effect         = "ALLOW"
```

### Row Policy Entity Name Reference

```java
// Source: RowLevelPolicyProviderDbImpl.getPolicies() line 41:
// secRowPolicyRepository.findByEntityNameAndOperation(entityName, operation.name())
// entityName comes from SecureDataManagerImpl via:
//   entityClass = entry.entityClass()
//   used as: rowLevelSpecificationBuilder.build(entityClass, op)
//   which calls: policyProvider.getPolicies(entityClass.getSimpleName(), op)
//
// Therefore sec_row_policy row must have:
// entity_name = "Organization"   ← entityClass.getSimpleName() (mixed case, not upper)
```

**Note:** The `getSimpleName()` call (not uppercased) is what the `RowLevelSpecificationBuilder` passes to the provider. The `sec_row_policy.entity_name` must match `Organization` exactly (capitalized, not `ORGANIZATION`).

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| JPQL stub logs warning and continues | JPQL stub throws `AccessDeniedException` (D-12 fix) | Phase 4 | Previously: JPQL query silently ignored if JPQL-to-Spec not implemented; After: fail-closed prevents unintended access widening |
| Empty `DefaultSecuredEntityCatalog` (returns `List.of()`) | `@Primary` metamodel-backed secured catalog with filtered proof entities | Phase 4 | All secured entity API calls now route through JPA-runtime-discovered, fail-closed catalog entries |
| Empty `fetch-plans.yml` (`fetch-plans: []`) | Populated with six+ fetch plan definitions for three entities | Phase 4 | Fetch plan resolver no longer throws on first secured read |

---

## Open Questions

1. **Single-entity read endpoint (`GET /{id}`) implementation approach**
   - What we know: `SecureDataManager` exposes `loadByQuery` (returns `Page`) and `save` (write-side). There is no `loadOne(entityCode, id)` method.
   - What's unclear: The cleanest way to implement `GET /{id}` without adding a new method to `SecureDataManager` and without violating the security pipeline contract (CRUD check + row policy must still apply).
   - Recommendation: For Phase 4, implement `GET /{id}` as a `loadByQuery` with a large pageable and the service filters to the first result, relying on the row policy to constrain access. Or: add a `loadOne(String entityCode, Object id, String fetchPlanCode)` method to `SecureDataManager` that applies CRUD + row-constrained ID lookup — this would be a small, clean addition. The planner should decide which approach fits the plan scope.

2. **Attribute-write deny test: request body structure**
   - What we know: `SecureMergeService.mergeForUpdate()` throws `AccessDeniedException` on a denied attribute. The controller's `PUT /{id}` endpoint accepts `Map<String, Object>` body. Spring's `DefaultHandlerExceptionResolver` and `ExceptionTranslator` must translate the `AccessDeniedException` to 403.
   - What's unclear: Whether `AccessDeniedException` from inside a `@Transactional @Service` called via MockMvc is correctly translated to 403 by the existing `ExceptionTranslator`, or whether it propagates differently.
   - Recommendation: Verify `ExceptionTranslator` handles `AccessDeniedException` (Spring Security exception). JHipster's `ExceptionTranslator` extends `ProblemHandlingWebMvc` which typically handles Spring Security exceptions at the filter/handler level. If `AccessDeniedException` thrown from inside a service during MockMvc execution translates to 403, the test is straightforward. Likely works because Spring's `ResponseEntityExceptionHandler` handles it.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 (JUnit Platform) + Spring Boot Test |
| Config file | `src/test/resources/junit-platform.properties` |
| Quick run command | `./gradlew test --tests "*.SecuredEntityEnforcementIT"` |
| Full suite command | `./gradlew test integrationTest` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| ENT-01 | Sample entities exist and are queryable via secured API | integration (smoke) | `./gradlew test --tests "*.SecuredEntityEnforcementIT#testCrudAllow*"` | Wave 0 |
| ENT-01 | Recursive association serialization works (Org→Dept→Emp) | integration | `./gradlew test --tests "*.SecuredEntityEnforcementIT#testNestedAssociationSerializ*"` | Wave 0 |
| ENT-02 | CRUD allow: authenticated user with permission gets 200 | integration | `./gradlew test --tests "*.SecuredEntityEnforcementIT#testCrudAllow*"` | Wave 0 |
| ENT-02 | CRUD deny: user without permission gets 403 | integration | `./gradlew test --tests "*.SecuredEntityEnforcementIT#testCrudDeny*"` | Wave 0 |
| ENT-02 | Attribute read deny: denied field absent from response JSON | integration | `./gradlew test --tests "*.SecuredEntityEnforcementIT#testAttributeRead*"` | Wave 0 |
| ENT-02 | Attribute write deny: write with denied field returns 403 | integration | `./gradlew test --tests "*.SecuredEntityEnforcementIT#testAttributeWrite*"` | Wave 0 |
| ENT-02 | Row-level policy: list returns only permitted rows | integration | `./gradlew test --tests "*.SecuredEntityEnforcementIT#testRowPolicy*"` | Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew test --tests "*.SecuredEntityEnforcementIT"` (runs the proof IT only)
- **Per wave merge:** `./gradlew test integrationTest` (full suite)
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `src/test/java/com/vn/core/security/proof/SecuredEntityEnforcementIT.java` — covers all ENT-01 and ENT-02 scenarios; does not exist yet
- [ ] Liquibase test fixture changelog or `@Sql` scripts for seeding `jhi_authority`, `sec_permission`, `sec_row_policy` rows during IT

*(No framework gaps — `@IntegrationTest`, `DatabaseTestcontainer`, `@AutoConfigureMockMvc`, and MockMvc infrastructure are all present)*

---

## Sources

### Primary (HIGH confidence)

- Direct inspection of `SecureDataManagerImpl.java` — JPQL stub location (lines 95-103), flow steps confirmed
- Direct inspection of `SecuredEntityEntry.java` — catalog entry builder shape, field names
- Direct inspection of `YamlFetchPlanRepository.java` — key format `entityClassName.toLowerCase()#planName`, `findByEntityAndName()` lookup logic
- Direct inspection of `AttributePermissionEvaluatorImpl.java` — target format `ENTITYNAME.ATTRIBUTE` (both uppercased), permissive-default behavior
- Direct inspection of `RowLevelPolicyProviderDbImpl.java` — `getSimpleName()` (not uppercased) for `entityName` in `sec_row_policy`; SPECIFICATION expression format `field = CURRENT_USER_LOGIN`
- Direct inspection of `MergedSecurityContextBridge.java` — phantom filter via `authorityRepository.findAllById()` confirming `jhi_authority` row requirement for test roles
- Direct inspection of `RepositoriesRegistryImpl.java` — dual-interface requirement (`JpaRepository` + `JpaSpecificationExecutor`) for entity repositories
- Direct inspection of `UserResourceIT.java` + `IntegrationTest.java` — IT annotation pattern, `@WithMockUser`, MockMvc wiring
- Direct inspection of `DatabaseTestcontainer.java` — Testcontainers PostgreSQL reuse pattern
- Direct inspection of `DefaultSecuredEntityCatalog.java` — placeholder returns `List.of()`, confirming `@Primary` override mechanism
- Direct inspection of angapp `fetch-plans.yml` — confirmed YAML format with nested plans

### Secondary (MEDIUM confidence)

- Spring Security `@WithMockUser` behavior with `MergedSecurityContextBridge` — inferred from code inspection of bridge implementation, not directly test-run. The phantom filter behavior is the key interaction.
- `AccessDeniedException` → 403 translation — inferred from JHipster `ExceptionTranslator` + Spring Security integration patterns. Should be verified in Wave 1 test execution.

### Tertiary (LOW confidence)

- None — all critical claims are supported by direct codebase inspection.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries confirmed present in codebase, no new dependencies
- Architecture patterns: HIGH — derived directly from Phase 3 source code inspection
- Pitfalls: HIGH — derived from actual implementation of `MergedSecurityContextBridge`, `RowLevelPolicyProviderDbImpl`, `AttributePermissionEvaluatorImpl`, and `YamlFetchPlanRepository`
- Test wiring: HIGH — `IntegrationTest`, `DatabaseTestcontainer`, `UserResourceIT` all inspected
- Single-entity read design: MEDIUM — open question about cleanest `GET /{id}` implementation

**Research date:** 2026-03-21
**Valid until:** 2026-04-21 (stable codebase — only changes if Phase 3 artifacts are modified)
