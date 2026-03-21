# Phase 3: Secure Enforcement Core - Research

**Researched:** 2026-03-21
**Domain:** Spring Boot secure data enforcement over Spring Data JPA / Hibernate
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Data access abstraction
- **D-01:** Phase 3 introduces a central `SecureDataManager` as the standard access path for protected business entities. Protected services must use it instead of reading or writing directly through entity repositories.
- **D-02:** Phase 3 also introduces `UnconstrainedDataManager` as the explicit bypass path for trusted internal access. It is the only sanctioned way to skip enforcement intentionally.
- **D-03:** Secured reads standardize on `loadByQuery` with parameters as the main query entry point. The API should be query-object based rather than a loose argument bag, carrying the secured entity/catalog code, JPQL text, named parameters, pageable/sort data, and fetch-plan code.

#### Enforcement points
- **D-04:** All protected read and write enforcement runs inside the central data-manager path, not in controllers or ad hoc service code.
- **D-05:** Secured read flow is ordered as: entity-level permission check -> row-policy-constrained query execution -> fetch-plan resolution -> attribute-filtered serialization.
- **D-06:** Secured write flow is ordered as: entity-level permission check -> row-policy-constrained target lookup -> attribute edit enforcement during merge -> persistence -> secure re-read through `SecureDataManager` for the returned view.

#### Permission evaluation flow
- **D-07:** Phase 2's `DENY`-wins permission semantics remain locked for Phase 3.
- **D-08:** Enforcement is modeled around access contexts for CRUD, attribute, row-level, and secured fetch-plan application decisions, using `MergedSecurityService` as the runtime source for current authorities and security context values.
- **D-09:** Phase 3 does not reintroduce database-stored fetch-plan permissions. Fetch-plan use is controlled by the secured entity catalog plus YAML/code-defined plan registration, with attribute-level filtering still applied on the output.

#### Row policy execution strategy
- **D-10:** Row policies must execute as database-level constraints, not in-memory post-filters, so reads, updates, deletes, and counts/page totals stay aligned.
- **D-11:** Phase 3 supports `SPECIFICATION` row policies and a controlled `JPQL` row-policy form. JPQL policies are stored as managed `WHERE`-style fragments only, not as full free-form queries.
- **D-12:** Runtime tokens for JPQL row policies are limited to built-in security-context values only, such as current user id or login. Request-supplied parameters are not part of the row-policy language.
- **D-13:** If a stored row policy cannot be applied safely at runtime, enforcement fails closed with a security-style access denial rather than ignoring the policy or widening access.
- **D-14:** `JAVA` row-policy execution is not part of Phase 3's enforcement scope.

#### Attribute filtering strategy
- **D-15:** Secured read payloads silently omit attributes the current user cannot view.
- **D-16:** `id` remains readable when it is included in the fetch plan.
- **D-17:** Reference properties are traversed only when the association itself is viewable, and nested attributes are filtered recursively through the same rules.
- **D-18:** Unauthorized attribute updates are rejected during secure merge; they are not silently stripped on write.

#### Secured entity catalog
- **D-19:** Phase 3 introduces a controlled secured-entity catalog as the source of truth for what entities, attributes, operations, and fetch plans participate in the security engine.
- **D-20:** Entity and attribute metadata may be derived from the JPA entity scanner / metamodel, but only after passing through an explicit secured-catalog allowlist. The system must not expose every scanned entity automatically.
- **D-21:** The secured catalog owns permission-target selection inputs and runtime lookup metadata, including allowed operations, fetch-plan codes, repository/query adapters, and attribute target names.
- **D-22:** Optional YAML may add presentation metadata and hints for catalog entries, such as labels, grouping, ordering, and display hints for attributes or fetch plans, but YAML must not define new security targets outside the code-defined secured catalog.

### Claude's Discretion
- Exact names and package layout for the secured query object and the `SecureDataManager` / `UnconstrainedDataManager` interfaces
- The concrete built-in token set for controlled JPQL row policies, as long as it stays limited to security-context values only
- Whether the secured catalog's optional YAML metadata is loaded via typed `@ConfigurationProperties`, a dedicated repository, or another app-owned config seam
- Whether `loadByQuery` returns maps directly or a secured intermediate view object that is serialized afterward, as long as the enforcement order above remains intact

### Deferred Ideas (OUT OF SCOPE)
- Full protected sample-entity implementation and allow/deny API proof - Phase 4
- Frontend permission-target browsing and security-management UX - Phase 5
- Database-stored fetch-plan definitions or DB-managed fetch-plan APPLY permissions - out of scope for v1 by project rule
- Request-parameter-driven row-policy language - out of scope for Phase 3
- Automatic exposure of every scanned JPA entity as a security target - explicitly rejected for Phase 3
- `JAVA` row-policy execution - future phase only if the project later proves a safe need for it
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DATA-01 | Secured business entity reads go through a central security-aware data access layer | `SecureDataManager` / `UnconstrainedDataManager`, `RepositoryRegistry` backed by Spring Data `Repositories`, access-context pipeline, and protected-service boundary rules |
| DATA-02 | Secured reads use fetch plans defined in YAML or code builders rather than database-stored fetch-plan metadata | `YamlFetchPlanRepository` + `FetchPlanBuilder`/`FetchPlans` code builder stack, `ApplicationProperties.FetchPlans` typed config, catalog-owned fetch-plan allowlist, `FetchPlanMetadataTools` for property reflection |
| DATA-03 | Unauthorized attributes are excluded from secured read payloads | `SecureEntitySerializerImpl` recursive serializer design, `id` always-visible rule, association-gated traversal behavior, `BeanWrapper` property access |
| DATA-04 | Unauthorized attribute updates are rejected or stripped before persistence | Fail-closed `SecureMergeServiceImpl` with `AccessDeniedException` on edit check, row-constrained target lookup before merge, secure re-read after save |
| DATA-05 | Row-level policies constrain read, update, and delete access for secured entities | `Specification` lane via `RowLevelSpecificationBuilder`, controlled JPQL fragment lane with built-in security-context tokens, fail-closed on unsafe stored policy, count/page alignment via `JpaSpecificationExecutor` |
</phase_requirements>

## Summary

Phase 3 should be planned as a security-engine phase that ports the complete donor `angapp` enforcement core into `com.vn.core.security` under a controlled package hierarchy. The donor code is complete, production-ready, and already tested; the porting work is primarily: (1) replacing the donor `SecurityService` seam with `MergedSecurityService`, (2) adapting `ApplicationProperties` to include a `FetchPlans` inner class for YAML location config, (3) wiring the `RepositoryRegistry` backed by Spring Data `Repositories`, and (4) adding a `SecuredEntityCatalog` allowlist that the donor code did not have.

The Phase 2 seams already in the root app are: `MergedSecurityService` (authority names, login, isAuthenticated), `SecPermissionRepository.findByRolesAndTarget(Collection, TargetType, String, String)`, `SecRowPolicyRepository.findByEntityNameAndOperation(String, String)` — where the second argument is a `String` operation, not an `EntityOp` enum — `TargetType` enum (ENTITY, ATTRIBUTE, ROW_POLICY — FETCH_PLAN intentionally excluded), and `EntityOp` enum (READ, CREATE, UPDATE, DELETE). Every donor interface that wired against `SecurityService.currentAuthorities()` and `SecurityService.currentUserId()` must wire against `MergedSecurityService` equivalents instead. The root app's `MergedSecurityService` does not expose user ID directly; row-policy support for `CURRENT_USER_ID` token requires resolving the login through `UserRepository.findOneByLogin`, or the initial Phase 3 scope can restrict built-in tokens to `CURRENT_USER_LOGIN` only.

The highest-risk planning area is the controlled JPQL row-policy lane. The root `SecRowPolicyRepository.findByEntityNameAndOperation` takes `String operation`, so callers must pass `op.name()`. The root `SecPermission.effect` column is a `String`, not an enum, so `RolePermissionServiceDbImpl` must compare `"DENY"`/`"ALLOW"` strings. The ArchUnit `TechnicalStructureTest` defines `Security` as a shared layer — all new security packages under `com.vn.core.security.*` will pass without changes.

**Primary recommendation:** Port the full donor enforcement stack into `com.vn.core.security` sub-packages, wire against `MergedSecurityService` instead of donor `SecurityService`, add `ApplicationProperties.FetchPlans` inner class and `SecuredEntityCatalog` as Phase 3 additions, and use Spring Data `Repositories` helper-backed `RepositoryRegistry` implementation.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 4.0.3 (repo pinned) | Wiring, transactions, typed config, test infrastructure | Already the repo baseline; provides `@ConfigurationProperties`, `@Transactional`, `ApplicationContext` |
| Spring Data JPA | 4.0.x (via Spring Boot BOM) | `JpaSpecificationExecutor`, `Specification` composition, `Repositories` helper | `JpaSpecificationExecutor.findAll(Specification, Pageable)` ensures row-policy constraints apply to both data and count queries |
| Spring Data Commons `Repositories` | 4.0.x (via Spring Boot BOM) | Dynamic lookup of a repository by entity class | Avoids per-entity manual wiring in `RepositoryRegistry`; `new Repositories(applicationContext)` collects all Spring Data repos |
| Hibernate ORM 7.1.x | Via Spring Boot BOM | JPA persistence layer | `BeanWrapper`-based property access for serialize/merge works against any Hibernate-managed entity |
| Spring Framework `BeanWrapper` | 7.0.x (via Spring Boot BOM) | Safe bean-property read/write for serialize and merge | Used in donor `SecureEntitySerializerImpl` and `SecureMergeServiceImpl`; handles nested paths, type conversion, and `PropertyDescriptor` discovery |
| JPA Metamodel (Jakarta Persistence) | Via Hibernate | Property metadata for fetch-plan resolution | `FetchPlanMetadataTools` uses field reflection + `Introspector.getBeanInfo` — no additional dep needed |
| `jackson-dataformat-yaml` | Spring Boot-managed (via Boot BOM) | Parse YAML fetch-plan definitions | `YamlFetchPlanRepository` requires `ObjectMapper(new YAMLFactory())` — must be added to `build.gradle` |
| Spring Security | 6.x (via Spring Boot BOM) | `AccessDeniedException` for enforcement fails | Already in the repo; donor code throws `AccessDeniedException` on CRUD/attribute/row-policy denial |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JUnit Jupiter + Mockito | Spring Boot 4.0.3 stack | Unit tests for serializer, merge, evaluators, YAML repository | All four donor test files are plain unit tests — no Spring context needed |
| Spring Boot Test + MockMvc + Testcontainers PostgreSQL | Spring Boot 4.0.3 stack | Integration tests for enforcement pipeline | Reuse `@IntegrationTest` composite annotation and existing `DatabaseTestcontainer` from Phase 1/2 |
| AssertJ | Spring Boot 4.0.3 stack | Fluent assertion style in tests | Used in all donor tests; already in root test classpath |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Spring Data `Repositories` helper | Hard-coded per-entity `Map<Class<?>, JpaRepository<?,?>>` | Hard-coded maps create repeated wiring and Phase 4 onboarding churn; `Repositories` discovers all repos automatically |
| `Specification` composition for row constraints | Hibernate Filters or Envers | Hibernate Filters require session-scope enabling and complicate unit tests; Envers is for audit trails |
| `BeanWrapper` for property traversal | Raw `Field.get()` reflection | `BeanWrapper` handles type conversion, `PropertyDescriptor` discovery, and nested path resolution; raw reflection misses edge cases |
| `jackson-dataformat-yaml` for YAML parsing | SnakeYAML directly | `jackson-dataformat-yaml` wraps SnakeYAML in Jackson's tree model, which the donor uses with `JsonNode` traversal |
| Controlled JPQL fragment lane | Generic JPQL rewrite engine | Generic parsing is the primary scope trap; locked decisions mandate a minimal DSL only |

**Installation (add to `build.gradle`):**
```groovy
implementation "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml"
```

**Version verification:** Run before implementation starts:
```bash
./gradlew dependencyInsight --dependency org.springframework.data:spring-data-jpa --configuration runtimeClasspath
./gradlew dependencyInsight --dependency org.springframework.data:spring-data-commons --configuration runtimeClasspath
./gradlew dependencyInsight --dependency com.fasterxml.jackson.dataformat:jackson-dataformat-yaml --configuration runtimeClasspath
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/vn/core/security/
+-- access/        # AccessContext, AccessConstraint, AccessManager(Impl), CrudEntityContext,
|                  # FetchPlanAccessContext, AttributeAccessContext, RowLevelAccessContext,
|                  # CrudEntityConstraint
+-- catalog/       # SecuredEntityCatalog interface, SecuredEntityEntry record/class
+-- data/          # SecureDataManager, UnconstrainedDataManager, SecuredLoadQuery record, impl
+-- fetch/         # FetchPlan, FetchPlanProperty, FetchMode, FetchPlanBuilder, FetchPlans,
|                  # FetchPlanMetadataTools, FetchPlanRepository, FetchPlanResolver(Impl),
|                  # YamlFetchPlanRepository
+-- merge/         # SecureMergeService, SecureMergeServiceImpl
+-- permission/    # EntityPermissionEvaluator(Impl), AttributePermissionEvaluator(Impl),
|                  # RolePermissionService, RolePermissionServiceDbImpl, AttributeOp
|                  # EntityOp (already in repo), TargetType (already in repo)
+-- repository/    # RepositoryRegistry interface, RepositoriesRegistryImpl
+-- row/           # RowLevelPolicyProvider(DbImpl), RowLevelSpecificationBuilder,
|                  # RowPolicyDefinition
+-- serialize/     # SecureEntitySerializer interface, SecureEntitySerializerImpl
|
|  (existing packages - unchanged)
+-- bridge/        # SecurityContextBridge, JHipsterSecurityContextBridge,
|                  # MergedSecurityContextBridge (Phase 1/2)
+-- domain/        # SecPermission, SecRowPolicy, SecRole (Phase 2)
+-- repository/    # SecPermissionRepository, SecRowPolicyRepository (Phase 2)

src/main/java/com/vn/core/config/
+-- ApplicationProperties.java  # Add FetchPlans inner class

src/main/resources/
+-- fetch-plans.yml                      # Named fetch-plan definitions (Phase 4 adds entity plans)
+-- config/application.yml               # Add: application.fetch-plans.config: classpath:fetch-plans.yml
```

### Pattern 1: Security-Aware Data Manager Boundary
**What:** Protected business services call `SecureDataManager` for all entity reads/writes. Trusted internal code (bootstrap, seed, maintenance) calls `UnconstrainedDataManager`. Neither path ever reaches entity repositories directly from controllers.

**When to use:** Every protected business-entity read, update, delete, and secure re-read. Do NOT route `UserService`, `AccountResource`, `UserResource`, `AuthorityResource`, or Phase 2 security-admin endpoints through it.

**SecuredLoadQuery record (satisfies D-03):**
```java
// Claude's discretion on exact shape per locked D-03
public record SecuredLoadQuery(
    String entityCode,           // catalog code
    String jpql,                 // caller-supplied JPQL WHERE fragment (search terms, NOT row policy)
    Map<String, Object> parameters,
    Pageable pageable,
    Sort sort,
    String fetchPlanCode
) {}
```

**Enforcement order (locked D-05/D-06):**
```
READ:   checkCrud(READ) -> rowSpec = rowSpecBuilder.build(class, READ)
          -> repo.findAll(Specification.where(userSpec).and(rowSpec), pageable)
          -> fetchPlanResolver.resolve(class, planCode)
          -> serializer.serialize(entity, fetchPlan)

WRITE:  checkCrud(UPDATE) -> specRepo.findOne(idSpec.and(rowSpec)) [fail -> AccessDeniedException]
          -> mergeService.mergeForUpdate(entity, payload) [unauthorized attr -> AccessDeniedException]
          -> repo.save(entity) -> serializer.serialize(saved, fetchPlan)

DELETE: checkCrud(DELETE) -> specRepo.findOne(idSpec.and(rowSpec)) [fail -> AccessDeniedException]
          -> repo.delete(entity)
```

### Pattern 2: Access Context Pipeline
**What:** Each enforcement decision is modeled as a typed context object passed through `AccessManager.applyRegisteredConstraints`. Constraints are Spring `@Component` beans implementing `AccessConstraint<C>`. `AccessManagerImpl` collects all `AccessConstraint<?>` beans via constructor list injection and routes by `c.supports().isAssignableFrom(context.getClass())`.

**Important defaults:**
- `CrudEntityContext` starts as `permitted = false` — a missing constraint grants nothing
- `FetchPlanAccessContext` starts as `permitted = true` — catalog allowlist is the gate, not deny-by-default

**When to use:** CRUD permission check, fetch-plan access check, and any future constraint extension.

**Example (from `angapp/AccessManagerImpl.java`):**
```java
// Source: angapp/src/main/java/com/mycompany/core/security/access/AccessManagerImpl.java
@Override
@SuppressWarnings("unchecked")
public <C extends AccessContext> C applyRegisteredConstraints(C context) {
    constraints.stream()
        .filter(c -> c.supports().isAssignableFrom(context.getClass()))
        .sorted(Comparator.comparingInt(AccessConstraint::getOrder))
        .forEach(c -> ((AccessConstraint<C>) c).applyTo(context));
    return context;
}
```

### Pattern 3: Row Policy Specification Builder
**What:** `RowLevelSpecificationBuilder.build(entityClass, op)` delegates to `RowLevelPolicyProvider.getPolicies` and AND-composes all returned `RowPolicyDefinition.getSpecification()` instances. The result is AND-ed with the caller-supplied spec before the JPA query.

**Key integration point:** `SecRowPolicyRepository.findByEntityNameAndOperation(String, String)` takes a `String` operation. Always pass `op.name()`.

**Fail-closed behavior (D-13):** If a stored policy expression does not parse to a recognized form, throw `AccessDeniedException` or `SecurityException` — do not skip the policy.

**Supported SPECIFICATION DSL (donor pattern — adapt for root app):**
```
expression = "field = CURRENT_USER_ID"
  -> Specification: root.get("field").get("id") == currentUserId
expression = "field = CURRENT_USER_LOGIN"
  -> Specification: root.get("field") == currentLogin
```

### Pattern 4: Secure Serializer (Attribute Filtering)
**What:** `SecureEntitySerializerImpl` walks fetch-plan properties, checks `AttributePermissionEvaluator.canView(entityClass, attr)` for each scalar, and silently skips unauthorized scalars (D-15). For reference properties it checks `canView` on the association first, then recurses using the nested `FetchPlan` (D-17).

**`id` always-visible rule (D-16):**
```java
// Source: angapp/src/main/java/com/mycompany/core/serialize/SecureEntitySerializerImpl.java
private boolean isAlwaysVisible(String attr) {
    return "id".equals(attr.toLowerCase(Locale.ROOT));
}
```

**Collection and array references:**
```java
// Source: angapp SecureEntitySerializerImpl.serializeReference
if (refValue instanceof Collection<?> collection) {
    values.put(attr, collection.stream()
        .map(item -> serialize(item, property.getFetchPlan()))
        .filter(Objects::nonNull).toList());
}
```

### Pattern 5: Secure Merge (Write Enforcement)
**What:** `SecureMergeServiceImpl` iterates the update payload map. For each attribute it calls `AttributePermissionEvaluator.canEdit(entityClass, attr)`. If denied, throws `AccessDeniedException` immediately (D-18: reject, not strip silently). If permitted, writes via `BeanWrapper.setPropertyValue`.

**Example (from `angapp/SecureMergeServiceImpl.java`):**
```java
// Source: angapp/src/main/java/com/mycompany/core/merge/SecureMergeServiceImpl.java
if (!attributePermissionEvaluator.canEdit(entityClass, attr)) {
    throw new AccessDeniedException(
        "No EDIT permission for " + entityClass.getSimpleName() + "." + attr);
}
wrapper.setPropertyValue(attr, entry.getValue());
```

### Pattern 6: YAML Fetch Plan Repository
**What:** `YamlFetchPlanRepository` loads from `ApplicationProperties.FetchPlans.config` (default `classpath:fetch-plans.yml`) at `@PostConstruct`. Keys are stored as `entity.toLowerCase()#planCode`. Supports inheritance via `extends`, nested inline `properties`, and reference to a named plan via `fetchPlan`.

**Root app `ApplicationProperties` addition:**
```java
// Add inside ApplicationProperties.java before jhipster-needle-application-properties-property-class
public static class FetchPlans {
    private String config = "classpath:fetch-plans.yml";
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
// Add field: private final FetchPlans fetchPlans = new FetchPlans();
// Add getter: public FetchPlans getFetchPlans() { return fetchPlans; }
```

**YAML format:**
```yaml
# Source: angapp/src/main/resources/fetch-plans.yml pattern
fetch-plans:
  - entity: com.vn.core.domain.MyEntity
    name: my-entity-basic
    properties:
      - id
      - name
      - name: relatedEntity
        fetchPlan: related-entity-basic
  - entity: com.vn.core.domain.MyEntity
    name: my-entity-detail
    extends: my-entity-basic
    properties:
      - description
```

**Code builder:**
```java
// Source: angapp/src/main/java/com/mycompany/core/fetch/FetchPlans.java
fetchPlans.builder(MyEntity.class)
    .add("id")
    .add("name")
    .add("department", "department-basic")
    .build();
```

### Pattern 7: Permission Evaluators and Target Normalization
**What:** `EntityPermissionEvaluatorImpl` normalizes entity target as `entityClass.getSimpleName().toUpperCase()`. `AttributePermissionEvaluatorImpl` normalizes attribute target as `ENTITYNAME.ATTRIBUTENAME` both upper-cased. Both delegate to `RolePermissionService.hasPermission`.

**Root app `RolePermissionServiceDbImpl` — String effect adaptation:**
```java
// Root app: SecPermission.effect is String, not Effect enum
boolean hasDeny = perms.stream().anyMatch(p -> "DENY".equals(p.getEffect()));
if (hasDeny) return false;
return perms.stream().anyMatch(p -> "ALLOW".equals(p.getEffect()));
```

**Attribute target normalization (from `angapp/AttributePermissionEvaluatorImpl.java`):**
```java
// Source: angapp/src/main/java/com/mycompany/core/security/permission/AttributePermissionEvaluatorImpl.java
String target = entityClass.getSimpleName().toUpperCase(Locale.ROOT)
    + "." + attribute.toUpperCase(Locale.ROOT);
return rolePermissionService.hasPermission(
    mergedSecurityService.getCurrentUserAuthorityNames(),
    TargetType.ATTRIBUTE, target, action);
```

### Pattern 8: Secured Entity Catalog (new in Phase 3)
**What:** A code-defined Spring bean that explicitly registers which JPA entity classes participate in the security engine. It is the allowlist gate before the JPA metamodel-derived metadata is used.

**Recommended registration API:**
```java
// Claude's discretion on exact shape — satisfies D-19/D-20/D-21
@Component
public class AppSecuredEntityCatalog implements SecuredEntityCatalog {
    @Override
    public List<SecuredEntityEntry> entries() {
        return List.of(
            SecuredEntityEntry.builder()
                .entityClass(MyEntity.class)
                .code("MY_ENTITY")
                .operations(EnumSet.of(EntityOp.READ, EntityOp.UPDATE, EntityOp.DELETE))
                .fetchPlanCodes(List.of("my-entity-basic", "my-entity-detail"))
                .jpqlAllowed(true)
                .build()
        );
    }
}
```

### Anti-Patterns to Avoid
- **Controller-side entity permission checks:** Never add `@PreAuthorize` or manual permission calls in REST controllers for protected business entities. All enforcement lives inside `SecureDataManager`.
- **Direct repository injection in protected services:** Protected services must not inject entity repositories. They must use `SecureDataManager` (or explicitly declare `UnconstrainedDataManager` for trusted bypass).
- **In-memory post-filter row policies:** Do not filter entity lists after loading. The compound `Specification` must be applied before the database query executes so counts, offsets, and pagination totals are correct (D-10).
- **Silently ignoring unparseable row policies:** Per D-13, an unparseable policy expression must produce `AccessDeniedException`, not a silently skipped constraint.
- **Using `UnconstrainedDataManager` as a performance shortcut:** It exists only for trusted internal flows (bootstrap, seed, maintenance). Feature code that uses it because enforcement is inconvenient will fail code review.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Row filtering on query results | Post-load Java filter | Spring Data `Specification.where(userSpec).and(rowSpec)` | Handles count query, pagination, null safety; donor pattern is proven |
| Bean property read/write during serialize/merge | Raw `Field.get()` reflection | Spring `BeanWrapper` / `BeanWrapperImpl` | Handles getters/setters, type conversion, nested paths, collection types |
| YAML parsing for fetch plans | Custom parser | `jackson-dataformat-yaml` with `JsonNode` tree | Donor `YamlFetchPlanRepository` is complete — do not rewrite |
| Repository lookup by entity class | Manual `Map<Class<?>, JpaRepository>` | Spring Data `Repositories` helper | Auto-discovers all registered Spring Data repos from `ApplicationContext` |
| DENY-wins permission logic | Custom SQL aggregation | `RolePermissionServiceDbImpl` with `findByRolesAndTarget` + stream DENY check | Donor implementation is correct and maps directly to Phase 2 `SecPermissionRepository` |
| Fetch-plan inheritance merge | Custom deep-copy logic | Donor `YamlFetchPlanRepository.mergeInto` | Cyclic detection, deep copy, and property-level merge already implemented |
| Access constraint ordering | `if/else` chains in data manager | `AccessManagerImpl` with `List<AccessConstraint<?>>` ordered by `getOrder()` | Extensible, testable, supports future Phase 4 constraint additions |

**Key insight:** The donor `angapp` enforcement stack is a complete, tested implementation. Phase 3 is a port, not a greenfield build. The adaptation work is limited to replacing the security service seam, adapting for String-based effect/operation fields, and adding the catalog allowlist.

## Common Pitfalls

### Pitfall 1: `SecPermission.effect` is `String` in root app, not an `Effect` enum
**What goes wrong:** Donor `RolePermissionServiceDbImpl` compares `p.getEffect() == SecPermission.Effect.DENY`. The root app `SecPermission.effect` is a plain `String` column.
**Why it happens:** Phase 2 decision (recorded in STATE.md) to keep REST contract decoupled from entity enum changes.
**How to avoid:** Root app implementation must use `"DENY".equals(p.getEffect())` and `"ALLOW".equals(p.getEffect())`.
**Warning signs:** DENY check is always false; all permissions appear as ALLOW regardless of stored effect.

### Pitfall 2: `SecRowPolicyRepository.findByEntityNameAndOperation` takes `String operation`
**What goes wrong:** Passing `EntityOp` enum directly causes a type mismatch; the Spring Data query will not execute correctly.
**Why it happens:** Phase 2 stored `operation` as a plain `String` column.
**How to avoid:** Always call `findByEntityNameAndOperation(entityClass.getSimpleName(), op.name())`.
**Warning signs:** Zero row policies returned for entities that definitely have policies configured.

### Pitfall 3: `MergedSecurityService` does not expose `currentUserId()`
**What goes wrong:** Donor `RowLevelPolicyProviderDbImpl` calls `securityService.currentUserId()`. `MergedSecurityService` only has `getCurrentUserLogin()`. Compiling against the donor signature will fail.
**Why it happens:** `SecurityContextBridge` (Phase 1/2) exposes only login; JHipster JWT `sub` claim is the login string, not a numeric ID.
**How to avoid:** Two options — (a) limit built-in row-policy token set to `CURRENT_USER_LOGIN` only for Phase 3; (b) extend `MergedSecurityService` with `Optional<Long> getCurrentUserId()` backed by `UserRepository.findOneByLogin(login).map(User::getId)`. Recommended: add the method to `MergedSecurityService` to keep `UserRepository` out of the row package.
**Warning signs:** `NoSuchMethodError` at runtime or `NullPointerException` in the Specification lambda when user ID is expected.

### Pitfall 4: ArchUnit `TechnicalStructureTest` security layer rules
**What goes wrong:** New classes in `com.vn.core.security.*` that depend on `com.vn.core.web.*` will fail the ArchUnit check.
**Why it happens:** The current `TechnicalStructureTest` allows Security to be accessed by Config/Service/Web but does not permit Security to depend on Web.
**How to avoid:** Keep all new Phase 3 classes under `com.vn.core.security.*`. This is correct — the data manager and its impl belong in `security.data`, not in `web` or `service`.
**Warning signs:** `TechnicalStructureTest.respectsTechnicalArchitectureLayers` fails during `./gradlew test`.

### Pitfall 5: `FetchPlanAccessContext` defaults to `permitted = true`
**What goes wrong:** If no constraint processes `FetchPlanAccessContext`, any fetch-plan code is allowed. This is different from `CrudEntityContext` which defaults to `permitted = false`.
**Why it happens:** Donor design: fetch-plan access is catalog-controlled, not deny-by-default. The catalog entry lists allowed plan codes.
**How to avoid:** Implement a `CatalogFetchPlanConstraint` that reads the catalog entry for the entity and calls `context.setPermitted(false)` when the plan code is not in the allowed list. This satisfies D-09.
**Warning signs:** Callers can request arbitrary fetch-plan codes and get results.

### Pitfall 6: Pagination count mismatch from post-load filtering
**What goes wrong:** `Page.getTotalElements()` reports more records than the user can see when row-policy filtering happens after `findAll()` returns.
**Why it happens:** `JpaSpecificationExecutor.findAll(spec, pageable)` runs both the data SELECT and the COUNT query with the same WHERE clause only if the `Specification` is passed correctly.
**How to avoid:** Always pass the combined `Specification.where(userSpec).and(rowSpec)` to the executor before any result processing.
**Warning signs:** Frontend pagination shows incorrect total counts or blank final pages.

### Pitfall 7: Cyclic fetch-plan inheritance in YAML
**What goes wrong:** `plan-a extends plan-b` and `plan-b extends plan-a` causes infinite recursion.
**Why it happens:** `YamlFetchPlanRepository.resolve` detects cycles via `LinkedHashSet<String> resolvingStack` and throws `IllegalArgumentException`.
**How to avoid:** Keep fetch-plan inheritance flat (at most one level of extension). The `@PostConstruct` loading will surface cycles at startup, not at request time.
**Warning signs:** `IllegalArgumentException: Cyclic fetch plan inheritance detected` on startup.

### Pitfall 8: Lazy-loaded association access during secure serialization
**What goes wrong:** `BeanWrapper.getPropertyValue(attr)` on a lazy association outside a transaction triggers `LazyInitializationException`.
**Why it happens:** The fetch plan drives attribute filtering but does not configure Hibernate loading. If the entity is detached when serialization runs, lazy proxies cannot be accessed.
**How to avoid:** Mark `SecureDataManager` read methods `@Transactional(readOnly = true)` as the donor does. The entity stays attached throughout serialization within the transaction.
**Warning signs:** `org.hibernate.LazyInitializationException` during serialization of reference properties.

## Code Examples

Verified patterns from direct file reads of donor source (all HIGH confidence):

### Complete SecureDataManager read page flow
```java
// Source: angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java
@Override
@Transactional(readOnly = true)
public <T> Page<Map<String, Object>> loadPage(
    Class<T> entityClass,
    Specification<T> userSpec,
    Pageable pageable,
    String fetchPlanCode
) {
    checkCrud(entityClass, EntityOp.READ);
    Specification<T> rowSpec = rowSpecBuilder.build(entityClass, EntityOp.READ);
    JpaSpecificationExecutor<T> repo = repositoryRegistry.specRepository(entityClass);
    FetchPlan fetchPlan = fetchPlanResolver.resolve(entityClass, fetchPlanCode);
    Specification<T> spec = Specification.where(userSpec).and(rowSpec);
    return repo.findAll(spec, pageable).map(entity -> serializer.serialize(entity, fetchPlan));
}
```

### Complete SecureDataManager save flow
```java
// Source: angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java
@Override
public <T> Map<String, Object> save(
    Class<T> entityClass, Object id, Map<String, Object> payload, String fetchPlanCode
) {
    checkCrud(entityClass, EntityOp.UPDATE);
    JpaRepository<T, Object> repo = repositoryRegistry.repository(entityClass);
    JpaSpecificationExecutor<T> specRepo = repositoryRegistry.specRepository(entityClass);
    Specification<T> rowSpec = rowSpecBuilder.build(entityClass, EntityOp.UPDATE);
    Specification<T> idSpec = (root, query, cb) -> cb.equal(root.get("id"), id);
    T entity = specRepo.findOne(Specification.where(idSpec).and(rowSpec))
        .orElseThrow(() -> new AccessDeniedException("Entity not found or row-level denied"));
    mergeService.mergeForUpdate(entity, payload);
    T saved = repo.save(entity);
    FetchPlan fetchPlan = fetchPlanResolver.resolve(entityClass, fetchPlanCode);
    return serializer.serialize(saved, fetchPlan);
}
```

### Row-policy builder root app adaptation (fail-closed per D-13)
```java
// Based on: angapp/src/main/java/com/mycompany/core/security/row/RowLevelPolicyProviderDbImpl.java
// Root app adaptation: MergedSecurityService, op.name(), String effect, fail-closed
private <T> RowPolicyDefinition<T> buildSpecificationPolicy(
    Class<T> entityClass, EntityOp op, SecRowPolicy p
) {
    String expr = p.getExpression();
    String[] parts = expr.split("=");
    if (parts.length != 2) {
        // D-13: fail closed -- throw, do not skip
        throw new AccessDeniedException("Unparseable row policy expression: " + p.getCode());
    }
    String field = parts[0].trim();
    String valueToken = parts[1].trim();
    return switch (valueToken) {
        case "CURRENT_USER_LOGIN" -> {
            String login = mergedSecurityService.getCurrentUserLogin()
                .orElseThrow(() -> new AccessDeniedException("Unauthenticated: cannot resolve CURRENT_USER_LOGIN"));
            Specification<T> spec = (root, query, cb) -> cb.equal(root.get(field), login);
            yield new RowPolicyDefinition<>(p.getCode(), op, spec);
        }
        case "CURRENT_USER_ID" -> {
            Long userId = mergedSecurityService.getCurrentUserId()  // requires MergedSecurityService extension
                .orElseThrow(() -> new AccessDeniedException("Unauthenticated: cannot resolve CURRENT_USER_ID"));
            Specification<T> spec = (root, query, cb) -> cb.equal(root.get(field).get("id"), userId);
            yield new RowPolicyDefinition<>(p.getCode(), op, spec);
        }
        default -> throw new AccessDeniedException(
            "Unknown token in row policy: " + valueToken + " (" + p.getCode() + ")");
    };
}
```

### YAML fetch-plan format with inheritance
```yaml
# Source: angapp/src/main/resources/fetch-plans.yml (adapted for root app)
fetch-plans:
  - entity: com.vn.core.domain.MyEntity
    name: my-entity-basic
    properties:
      - id
      - name
      - name: department
        fetchPlan: department-basic
  - entity: com.vn.core.domain.MyEntity
    name: my-entity-detail
    extends: my-entity-basic
    properties:
      - description
      - createdDate
```

### ApplicationProperties.FetchPlans addition
```java
// Source: angapp/src/main/java/com/mycompany/myapp/config/ApplicationProperties.java (pattern)
// Add to root app ApplicationProperties.java:
private final FetchPlans fetchPlans = new FetchPlans();

public FetchPlans getFetchPlans() {
    return fetchPlans;
}

public static class FetchPlans {
    private String config = "classpath:fetch-plans.yml";
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
```

### `id` always-visible rule in serializer
```java
// Source: angapp/src/main/java/com/mycompany/core/serialize/SecureEntitySerializerImpl.java
private boolean isAlwaysVisible(String attr) {
    return "id".equals(attr.toLowerCase(Locale.ROOT));
}

// In serialize():
if (!property.isReference()) {
    if (isAlwaysVisible(attr) || attributePermissionEvaluator.canView(entityClass, attr)) {
        values.put(attr, wrapper.getPropertyValue(attr));
    }
    continue;  // do not add to values if not permitted -- D-15 silent omit
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Hibernate Filters for row-level security | Spring Data `Specification` composition | Spring Data 2.0+ | Specifications apply to count queries and are easier to unit-test |
| Controller `@PreAuthorize` for entity-level CRUD | Central `SecureDataManager` enforcement | Jmix-style pattern | Consistent enforcement regardless of which controller calls the data access |
| DB-stored fetch-plan definitions | YAML / code-builder fetch plans | Phase decision (D-09) | Plans are version-controlled; eliminates runtime schema coupling |
| Post-load Java filtering | Pre-query `Specification` row constraints | D-10 requirement | Fixes pagination count alignment |

**Deprecated/outdated:**
- `SecurityService.currentUserId()` (donor interface): Not in root app `MergedSecurityService`. Requires either extending the interface or limiting token set to login-only in Phase 3.
- `SecPermission.Effect` enum comparison (donor `RolePermissionServiceDbImpl`): Root app uses `String` effect. Compare as strings `"DENY"` / `"ALLOW"`.
- `@Primary` on `RowLevelPolicyProviderDbImpl` (donor): Root app Phase 3 has only one provider, so `@Primary` is not needed unless a second is added.

## Open Questions

1. **CURRENT_USER_ID token vs CURRENT_USER_LOGIN only**
   - What we know: `MergedSecurityService` has `getCurrentUserLogin()` but not `getCurrentUserId()`. Getting user ID requires `UserRepository.findOneByLogin`. The `UserRepository` is in `com.vn.core.repository` (Persistence layer).
   - What's unclear: Whether to extend `MergedSecurityService` (Security layer) to cache/expose user ID, or to restrict Phase 3 tokens to login only.
   - Recommendation: Extend `MergedSecurityService` with `Optional<Long> getCurrentUserId()` backed by `UserRepository.findOneByLogin`. This keeps `UserRepository` out of `security.row` and stays within the existing ArchUnit layer rules (Security may access Persistence). The row-policy provider then calls `mergedSecurityService.getCurrentUserId()` with no direct repository dep.

2. **`CatalogFetchPlanConstraint` to enforce D-09**
   - What we know: `FetchPlanAccessContext` defaults to `permitted = true`. Without a catalog-checking constraint, any plan code string can be used.
   - What's unclear: Whether to validate the plan code in the resolver before `AccessManager` (simpler) or as an `AccessConstraint<FetchPlanAccessContext>` bean (extensible).
   - Recommendation: Implement as an `AccessConstraint<FetchPlanAccessContext>` — this is consistent with the pipeline pattern and is the extensible path for Phase 4.

3. **`SecuredEntityCatalog` empty state during Phase 3**
   - What we know: Phase 3 delivers the catalog infrastructure; Phase 4 registers actual sample entities.
   - What's unclear: Whether the catalog should have zero entries allowed (valid empty state) or should require at least one entry.
   - Recommendation: Empty catalog is valid — the enforcement engine simply finds no matching entity and returns `AccessDeniedException` for any call. Phase 3 integration tests can register test-only catalog entries.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter) + Mockito + AssertJ |
| Config file | `build.gradle` (via Spring Boot 4.0.3 BOM; `integrationTest` task in `gradle/spring-boot.gradle`) |
| Quick run command | `./gradlew test` |
| Full suite command | `./gradlew test integrationTest` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DATA-01 | `SecureDataManager.loadPage` applies CRUD, row policy, and serialization in order | Unit | `./gradlew test --tests "*.SecureDataManagerImplTest"` | Wave 0 |
| DATA-01 | `UnconstrainedDataManager` bypasses all enforcement | Unit | `./gradlew test --tests "*.UnconstrainedDataManagerImplTest"` | Wave 0 |
| DATA-02 | YAML fetch-plan repository resolves plans by entity + code | Unit | `./gradlew test --tests "*.YamlFetchPlanRepositoryTest"` | Port from angapp |
| DATA-02 | Code-builder `FetchPlans` composes nested plans | Unit | `./gradlew test --tests "*.FetchPlanBuilderTest"` | Port from angapp |
| DATA-03 | Serializer omits unauthorized scalar, always exposes `id` | Unit | `./gradlew test --tests "*.SecureEntitySerializerImplTest"` | Port from angapp |
| DATA-03 | Serializer recurses into collection references using nested plan | Unit | (same test class) | Port from angapp |
| DATA-04 | Secure merge throws `AccessDeniedException` for unauthorized attribute | Unit | `./gradlew test --tests "*.SecureMergeServiceImplTest"` | Wave 0 |
| DATA-04 | Attribute permission evaluator normalizes target to uppercase | Unit | `./gradlew test --tests "*.AttributePermissionEvaluatorImplTest"` | Port from angapp |
| DATA-05 | Row-policy specification builder AND-composes multiple policies | Unit | `./gradlew test --tests "*.RowLevelSpecificationBuilderTest"` | Wave 0 |
| DATA-05 | Row-policy provider fails closed on unparseable expression | Unit | `./gradlew test --tests "*.RowLevelPolicyProviderDbImplTest"` | Wave 0 |
| DATA-01 through DATA-05 | Full enforcement pipeline on live database with real Specification query | Integration | `./gradlew integrationTest --tests "*.SecureDataManagerIT"` | Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew test`
- **Per wave merge:** `./gradlew test integrationTest`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/com/vn/core/security/data/SecureDataManagerImplTest.java` — covers DATA-01 unit enforcement order
- [ ] `src/test/java/com/vn/core/security/data/UnconstrainedDataManagerImplTest.java` — covers DATA-01 bypass
- [ ] `src/test/java/com/vn/core/security/merge/SecureMergeServiceImplTest.java` — covers DATA-04
- [ ] `src/test/java/com/vn/core/security/row/RowLevelSpecificationBuilderTest.java` — covers DATA-05 composition
- [ ] `src/test/java/com/vn/core/security/row/RowLevelPolicyProviderDbImplTest.java` — covers DATA-05 fail-closed
- [ ] `src/test/java/com/vn/core/security/data/SecureDataManagerIT.java` — covers DATA-01 through DATA-05 end-to-end on PostgreSQL
- [ ] Port `angapp/src/test/.../fetch/YamlFetchPlanRepositoryTest.java` to `src/test/java/com/vn/core/security/fetch/`
- [ ] Port `angapp/src/test/.../fetch/FetchPlanBuilderTest.java` to `src/test/java/com/vn/core/security/fetch/`
- [ ] Port `angapp/src/test/.../serialize/SecureEntitySerializerImplTest.java` to `src/test/java/com/vn/core/security/serialize/`
- [ ] Port `angapp/src/test/.../permission/AttributePermissionEvaluatorImplTest.java` to `src/test/java/com/vn/core/security/permission/`

## Sources

### Primary (HIGH confidence — direct file reads from this session)
- `angapp/src/main/java/com/mycompany/core/data/SecureDataManager.java` — interface contract
- `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java` — enforcement ordering (canonical reference)
- `angapp/src/main/java/com/mycompany/core/merge/SecureMergeService.java` + `SecureMergeServiceImpl.java` — write enforcement contract and fail-closed behavior
- `angapp/src/main/java/com/mycompany/core/serialize/SecureEntitySerializerImpl.java` — attribute filtering, id-always-visible, collection recursion
- `angapp/src/main/java/com/mycompany/core/fetch/` (all 10 files) — fetch plan model, builder, YAML repo, resolver, metadata tools
- `angapp/src/main/java/com/mycompany/core/security/` (all sub-packages) — access context pipeline, constraint beans, permission evaluators, row-policy provider and builder
- `angapp/src/main/resources/fetch-plans.yml` — YAML format with nested reference and inline properties examples
- `angapp/src/test/java/com/mycompany/core/` (4 test files) — expected behavior for YAML, builder, serializer, attribute evaluator
- `src/main/java/com/vn/core/security/MergedSecurityService.java` — root app security contract (login + authorities + isAuthenticated only; no currentUserId)
- `src/main/java/com/vn/core/security/repository/SecPermissionRepository.java` — `findByRolesAndTarget` signature confirmed
- `src/main/java/com/vn/core/security/repository/SecRowPolicyRepository.java` — `String operation` parameter confirmed
- `src/main/java/com/vn/core/security/domain/SecPermission.java` — `String effect` field confirmed
- `src/main/java/com/vn/core/security/domain/SecRowPolicy.java` — String policyType/operation/expression fields confirmed
- `src/main/java/com/vn/core/security/permission/EntityOp.java` + `TargetType.java` — enum values confirmed
- `src/main/java/com/vn/core/config/ApplicationProperties.java` — FetchPlans inner class must be added
- `src/test/java/com/vn/core/TechnicalStructureTest.java` — ArchUnit layer rules; Security allowed to access Persistence
- `src/test/java/com/vn/core/IntegrationTest.java` — `@IntegrationTest` composite annotation shape
- `.planning/phases/03-secure-enforcement-core/03-CONTEXT.md` — locked decisions D-01 through D-22
- `.planning/STATE.md` — String-effect pattern decision confirmed

### Secondary (MEDIUM confidence — cross-referenced)
- `.planning/REQUIREMENTS.md` — DATA-01 through DATA-05 requirement text
- `angapp/src/main/java/com/mycompany/myapp/config/ApplicationProperties.java` — FetchPlans inner class pattern to port

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries are repo-pinned or BOM-managed; donor already uses them all
- Architecture patterns: HIGH — all patterns read directly from donor source files and root app Phase 2 seams; no inference
- Integration points: HIGH — `SecRowPolicyRepository` String param, `SecPermission.effect` String, and `MergedSecurityService` gap all confirmed from direct file reads
- Pitfalls: HIGH — each pitfall traced to a specific code observation; no speculation
- Validation architecture: HIGH — test patterns ported from donor test files; gaps listed from what provably does not yet exist in root app

**Research date:** 2026-03-21
**Valid until:** 2026-04-21 (stable libraries; minor risk from Spring Boot patch version only)
