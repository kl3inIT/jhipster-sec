# Phase 2: Security Metadata Management - Research

**Researched:** 2026-03-21
**Domain:** Spring Boot JPA entity/repository/REST CRUD; Liquibase schema evolution; Spring Security `@Primary` bean override; MapStruct DTO mapping
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Role model — jhi_authority evolution (SEC-01)**
- D-01: `jhi_authority` is the merged role table. No separate `sec_role` table. `jhi_authority.name` is the canonical role code (locked in Phase 1).
- D-02: Two new columns are added via Liquibase: `display_name VARCHAR(255)` (nullable, human-readable label) and `type VARCHAR(20) NOT NULL DEFAULT 'RESOURCE'` (RESOURCE or ROW_LEVEL, matching angapp's `SecRole.RoleType`).
- D-03: The `Authority` JPA entity gains `displayName` and `type` fields. `AuthorityDTO` or a new `SecRoleDTO` carries all three fields in REST responses.
- D-04: Existing `ROLE_ADMIN`, `ROLE_USER`, and `ROLE_ANONYMOUS` rows are migrated with `type = 'RESOURCE'` and `display_name = NULL` — no data loss.
- D-05: Admin CRUD endpoint for roles lives under `/api/admin/sec/roles` — separate from the existing JHipster `/api/admin/authorities` (which stays unchanged).

**Permission rules — sec_permission table (SEC-02)**
- D-06: A new `sec_permission` table is created — a faithful port of angapp's `sec_permission` minus the Long role FK. Columns: `id`, `authority_name VARCHAR(50) FK → jhi_authority.name`, `target_type VARCHAR(30)`, `target VARCHAR(255)`, `action VARCHAR(50)`, `effect VARCHAR(10)`.
- D-07: Supported `target_type` values: `ENTITY`, `ATTRIBUTE`, `ROW_POLICY`. `FETCH_PLAN` is excluded (no database fetch-plan storage allowed).
- D-08: `effect` is `ALLOW` or `DENY`. DENY-wins is the locked semantic — deny on any matching rule blocks access regardless of allow rules.
- D-09: Admin CRUD endpoint lives under `/api/admin/sec/permissions`.
- D-10: `SecPermission` JPA entity with `SecPermissionRepository` lives in `com.vn.core.security.domain` and `com.vn.core.security.repository` respectively.

**Row policies — sec_row_policy table (SEC-03)**
- D-11: A new `sec_row_policy` table is created: `id`, `code VARCHAR(100) UNIQUE`, `entity_name VARCHAR(255)`, `operation VARCHAR(20)`, `policy_type VARCHAR(20)`, `expression VARCHAR(1000)`.
- D-12: All three `policy_type` values valid in schema and admin API: `SPECIFICATION`, `JPQL`, `JAVA`. Phase 3 determines enforcement.
- D-13: `SecRowPolicy` JPA entity and `SecRowPolicyRepository` live in `com.vn.core.security.domain` and `com.vn.core.security.repository`.
- D-14: Admin CRUD endpoint lives under `/api/admin/sec/row-policies`.

**SecurityContextBridge Phase 2 override**
- D-15: Phase 2 delivers `MergedSecurityContextBridge` as `@Primary @Component` in `com.vn.core.security.bridge`. It supersedes `JHipsterSecurityContextBridge` without modifying it.
- D-16: `getCurrentUserAuthorities()` loads authority names from JHipster's `Authentication` then validates them against `jhi_authority`. Only names that exist in `jhi_authority` are returned — stale or phantom roles are silently dropped.
- D-17: Phase 2 ports the angapp `SecurityService` interface into `com.vn.core.security` as `MergedSecurityService`. The Phase 2 bridge wires into this service.

**API conventions**
- D-18: All three admin endpoints require `ROLE_ADMIN` authorization.
- D-19: REST controllers live in `com.vn.core.web.rest.admin.security`.
- D-20: DTOs live in `com.vn.core.service.dto.security` — `SecRoleDTO`, `SecPermissionDTO`, `SecRowPolicyDTO`. No direct entity exposure.

### Claude's Discretion
- Exact DTO field validation constraints (which fields are @NotNull, @Size, etc.)
- Whether partial-update (PATCH) endpoints are added alongside full-update (PUT) in Phase 2
- Enum representation in DTOs (String codes vs Java enums)
- Whether `SecRoleRepository` adds a `findByCode` alias for `findById` symmetry with angapp
- Test coverage shape: MockMvc unit tests vs Testcontainers integration tests per endpoint

### Deferred Ideas (OUT OF SCOPE)
- `RolePermissionService` and enforcement-layer beans (`EntityPermissionEvaluator`, `AttributePermissionEvaluator`, `RowLevelPolicyProvider`) — Phase 3
- `SecureDataManager` and the central secured data pipeline — Phase 3
- `AccessManager` / `AccessConstraint` framework — Phase 3
- Row policy enforcement implementation for JPQL and JAVA policy types — Phase 3
- `FetchPlan` YAML loading and `FetchPlanResolver` — Phase 3
- Frontend security administration screens — Phase 5
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SEC-01 | Admin can create, update, list, and delete merged security roles | `jhi_authority` evolution via Liquibase addColumn + Authority entity extension; `SecRoleDTO`; `SecRoleAdminResource` at `/api/admin/sec/roles` |
| SEC-02 | Admin can create, update, list, and delete permission rules for entity CRUD and attribute view/edit actions | New `sec_permission` table with FK to `jhi_authority.name`; `SecPermission` entity + `SecPermissionRepository`; `SecPermissionAdminResource` at `/api/admin/sec/permissions` |
| SEC-03 | Admin can create, update, list, and delete supported row policies | New `sec_row_policy` table; `SecRowPolicy` entity + `SecRowPolicyRepository`; `SecRowPolicyAdminResource` at `/api/admin/sec/row-policies` |
</phase_requirements>

---

## Summary

Phase 2 is purely a backend metadata-management phase. It evolves the existing `jhi_authority` table in-place with two new columns (`display_name`, `type`) and creates two net-new tables (`sec_permission`, `sec_row_policy`). Each table gets a JPA entity, a Spring Data repository, a service layer, and an admin REST controller. Additionally, the `SecurityContextBridge` is replaced with a `@Primary` override (`MergedSecurityContextBridge`) that filters returned authority names against the database, and a `MergedSecurityService` is introduced as the Phase 3-facing security context contract.

The work falls into four delivery tracks that can proceed nearly independently: (1) schema/entity/repository bootstrap, (2) service layer and DTOs, (3) REST controllers, (4) bridge and security service. The angapp donor files provide complete reference implementations for all entity shapes, repository query signatures, and enum definitions. Minimal translation is required — the main adaptation is replacing angapp's `role_id` Long FK with an `authority_name` String FK that maps directly to `jhi_authority.name`.

**Primary recommendation:** Port angapp entities and repositories faithfully, adapting only the FK strategy; follow the established `UserResource`/`AdminUserDTO`/`EntityMapper` stack patterns for controllers and DTOs; use Liquibase `addColumn` (not `createTable`) for the `jhi_authority` evolution.

---

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Data JPA | (Spring Boot 4.0.3 BOM) | `SecPermissionRepository`, `SecRowPolicyRepository`, `AuthorityRepository` extensions | Already on classpath; standard persistence layer |
| MapStruct | 1.6.3 | `SecPermissionMapper`, `SecRowPolicyMapper` DTO mappings | Project pattern for angapp-style entities; see `OrganizationMapper` |
| Liquibase | 5.0.1 | `addColumn` on `jhi_authority`, `createTable` for sec_permission, sec_row_policy | Mandatory schema evolution tool for this project |
| Jakarta Validation | (Spring Boot BOM) | `@NotNull`, `@Size`, `@NotBlank` on DTOs | Existing DTO convention; see `AdminUserDTO` |
| Spring Security `@PreAuthorize` | (Spring Boot BOM) | `hasAuthority('ROLE_ADMIN')` on all new endpoints | Established pattern; see `UserResource` |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JHipster `HeaderUtil` | 9.0.0 | `createEntityCreationAlert`, `createEntityDeletionAlert` | All CRUD response headers per existing pattern |
| JHipster `ResponseUtil` | 9.0.0 | `wrapOrNotFound` for GET-by-id responses | GET-single endpoints; see `AuthorityResource` |
| JHipster `PaginationUtil` | 9.0.0 | Pagination headers on list endpoints | If list endpoints add pagination (discretion item) |
| Spring Boot Test / MockMvc | (Spring Boot BOM) | `@IntegrationTest` + `@AutoConfigureMockMvc` + `@WithMockUser` | Integration tests following `AuthorityResourceIT` pattern |
| Testcontainers PostgreSQL | (Spring Boot BOM) | Real database in integration tests | Already wired by `DatabaseTestcontainer`; no additional setup needed |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| MapStruct `EntityMapper` | Hand-written mapper like `UserMapper` | Hand-written is acceptable for simple DTOs but adds boilerplate; use MapStruct for new security entities per CONTEXT.md D-20 note |
| String authority_name FK | Long role_id FK (angapp pattern) | String FK matches `jhi_authority.name` PK directly; no join table needed; simpler cascade logic |
| `addColumn` Liquibase | Separate migration table | `addColumn` with default value preserves existing rows without data loss (D-04) |

**Installation:** No new dependencies required. MapStruct 1.6.3 is already declared in `gradle/libs.versions.toml`.

---

## Architecture Patterns

### Recommended Project Structure

New packages to create:

```
com.vn.core/
├── security/
│   ├── bridge/
│   │   ├── SecurityContextBridge.java          (Phase 1 — unchanged)
│   │   ├── JHipsterSecurityContextBridge.java  (Phase 1 — unchanged)
│   │   └── MergedSecurityContextBridge.java    (Phase 2 — @Primary)
│   ├── domain/
│   │   ├── SecPermission.java                  (new)
│   │   └── SecRowPolicy.java                   (new)
│   └── repository/
│       ├── SecPermissionRepository.java         (new)
│       └── SecRowPolicyRepository.java          (new)
├── service/
│   └── dto/
│       └── security/
│           ├── SecRoleDTO.java                 (new)
│           ├── SecPermissionDTO.java           (new)
│           └── SecRowPolicyDTO.java            (new)
│   └── mapper/
│       └── security/
│           ├── SecPermissionMapper.java        (new)
│           └── SecRowPolicyMapper.java         (new)
│   └── security/
│       ├── MergedSecurityService.java          (new interface)
│       └── MergedSecurityServiceImpl.java      (new impl)
└── web/rest/admin/security/
    ├── SecRoleAdminResource.java               (new)
    ├── SecPermissionAdminResource.java         (new)
    └── SecRowPolicyAdminResource.java          (new)
```

Liquibase changelogs to add (in date-ordered filenames):
```
src/main/resources/config/liquibase/
└── changelog/
    ├── 20260321000100_add_authority_type_columns.xml
    ├── 20260321000200_create_sec_permission.xml
    └── 20260321000300_create_sec_row_policy.xml
```

All three must be included in `master.xml` via `<include>` elements.

### Pattern 1: jhi_authority Column Evolution

**What:** `addColumn` on existing `jhi_authority` table; back-fill existing rows with defaults.
**When to use:** Evolving an existing table without destroying existing rows.

```xml
<!-- Source: Liquibase addColumn docs; pattern from project master.xml -->
<changeSet id="20260321000100-1" author="dev">
    <addColumn tableName="jhi_authority">
        <column name="display_name" type="varchar(255)"/>
    </addColumn>
</changeSet>

<changeSet id="20260321000100-2" author="dev">
    <addColumn tableName="jhi_authority">
        <column name="type" type="varchar(20)" defaultValue="RESOURCE">
            <constraints nullable="false"/>
        </column>
    </addColumn>
</changeSet>
```

Key detail: `addColumn` with `defaultValue` automatically populates existing rows (D-04). Do NOT use `defaultValueComputed` for a simple string literal.

### Pattern 2: Authority Entity Extension

**What:** Add `displayName` and `type` fields to the existing `Authority` JPA entity.
**When to use:** Extending an existing entity with new columns.

```java
// Source: existing Authority.java pattern; angapp SecRole.java for enum shape
@Column(name = "display_name", length = 255)
private String displayName;

@Enumerated(EnumType.STRING)
@Column(name = "type", nullable = false, length = 20)
private RoleType type;

public enum RoleType {
    RESOURCE,
    ROW_LEVEL
}
```

Critical constraint: `Authority` uses `String` as its `@Id` (the `name` field). It implements `Persistable<String>` with a `@PostLoad`/`@PostPersist` `isPersisted` flag. Any new constructor or builder added to `Authority` must call `setIsPersisted()` to avoid double-insert errors.

### Pattern 3: sec_permission with authority_name FK (not role_id)

**What:** Port angapp's `SecPermission` replacing the `role_id` Long FK with an `authority_name` String FK to `jhi_authority.name`.
**When to use:** Adapted port of angapp entity to fit the merged jhi_authority PK.

```java
// Source: angapp SecPermission.java (adapted)
@Column(name = "authority_name", nullable = false, length = 50)
private String authorityName;

// NOT: @ManyToOne SecRole role (angapp pattern uses Long FK)
// Phase 2 uses String FK directly — matches jhi_authority.name PK
```

The `sec_permission` Liquibase changelog must add a FK constraint:
```xml
<addForeignKeyConstraint
    baseTableName="sec_permission"
    baseColumnNames="authority_name"
    referencedTableName="jhi_authority"
    referencedColumnNames="name"
    constraintName="fk_sec_permission_authority"
    onDelete="CASCADE"/>
```

The `onDelete="CASCADE"` answers the cascade question from CONTEXT.md — DB-level cascade is simpler and avoids orphaned permission rows when a role is deleted.

### Pattern 4: SecPermissionRepository with Phase 3 query shape

**What:** The repository must expose the query shape Phase 3 depends on. The angapp query uses `p.role.code` — the Phase 2 adaptation uses `p.authorityName` directly.
**When to use:** All permission lookups in Phase 3 enforcement.

```java
// Source: angapp SecPermissionRepository.java (adapted for authority_name FK)
@Query(
    "select p from SecPermission p " +
    "where p.authorityName in :authorityNames " +
    "and p.targetType = :targetType " +
    "and p.target = :target " +
    "and p.action = :action"
)
List<SecPermission> findByRolesAndTarget(
    @Param("authorityNames") Collection<String> authorityNames,
    @Param("targetType") TargetType targetType,
    @Param("target") String target,
    @Param("action") String action
);
```

### Pattern 5: TargetType enum (project-local, FETCH_PLAN excluded)

**What:** Port `TargetType` enum from angapp, but exclude `FETCH_PLAN`.
**When to use:** Validation in `SecPermission` entity and DTO.

```java
// Source: angapp TargetType.java (adapted per D-07)
// Location: com.vn.core.security.permission.TargetType
public enum TargetType {
    ENTITY,
    ATTRIBUTE,
    ROW_POLICY
    // FETCH_PLAN excluded per D-07
}
```

Similarly, `EntityOp` from angapp is ported verbatim to `com.vn.core.security.permission.EntityOp`:
```java
public enum EntityOp { READ, CREATE, UPDATE, DELETE }
```

### Pattern 6: MapStruct EntityMapper for security DTOs

**What:** MapStruct interface extending `EntityMapper<D, E>` following angapp's `OrganizationMapper` pattern.
**When to use:** `SecPermission` and `SecRowPolicy` DTO mapping.

```java
// Source: angapp OrganizationMapper.java pattern
@Mapper(componentModel = "spring")
public interface SecPermissionMapper extends EntityMapper<SecPermissionDTO, SecPermission> {
    // toEntity / toDto / partialUpdate generated by MapStruct
}
```

`EntityMapper` interface (from `angapp`) must be copied to `com.vn.core.service.mapper.EntityMapper` — it is not in the root project yet.

### Pattern 7: MergedSecurityContextBridge as @Primary override

**What:** `@Primary @Component` bean that replaces `JHipsterSecurityContextBridge` via Spring's `@Primary` disambiguation.
**When to use:** Anywhere `SecurityContextBridge` is injected — Phase 2+ Spring context picks this bean.

```java
// Source: angapp SecurityServiceImpl.java + JHipsterSecurityContextBridge.java
@Primary
@Component
public class MergedSecurityContextBridge implements SecurityContextBridge {

    private final AuthorityRepository authorityRepository;
    // constructor injection

    @Override
    public Collection<String> getCurrentUserAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return List.of();
        Set<String> jwtAuthorities = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
        // D-16: validate against jhi_authority table; drop phantom names
        Set<String> validNames = authorityRepository.findAllById(jwtAuthorities)
            .stream().map(Authority::getName).collect(Collectors.toSet());
        return jwtAuthorities.stream().filter(validNames::contains).toList();
    }
}
```

Important: `AuthorityRepository.findAllById(Collection<String>)` is a Spring Data method available without any custom query. This avoids N+1 queries for authority validation.

### Pattern 8: Admin REST controller structure

**What:** Follows `UserResource` with `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`, `ResponseEntity<T>`, `@Valid`, and `HeaderUtil` error alerts.
**When to use:** All three new admin endpoints.

```java
// Source: UserResource.java pattern
@RestController
@RequestMapping("/api/admin/sec")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class SecPermissionAdminResource {
    // POST   /api/admin/sec/permissions
    // GET    /api/admin/sec/permissions
    // GET    /api/admin/sec/permissions/{id}
    // PUT    /api/admin/sec/permissions/{id}
    // DELETE /api/admin/sec/permissions/{id}
}
```

### Anti-Patterns to Avoid

- **Modifying `JHipsterSecurityContextBridge`:** Phase 1 contract — never touch it. Override via `@Primary` only.
- **Using `sec_role` as a separate table:** Locked out by D-01. Everything lives in `jhi_authority`.
- **Storing FETCH_PLAN in `sec_permission.target_type`:** D-07 explicitly forbids it.
- **Exposing JPA entities directly from REST responses:** D-20 mandates DTOs. Never return `SecPermission` directly.
- **Adding to `jhi_authority` without a Liquibase `addColumn`:** Creating a new changelog that uses `createTable` instead of `addColumn` would drop existing rows.
- **Ignoring cascade semantics on role deletion:** If `onDelete=CASCADE` is not set in the FK, deleting an authority will cause a FK constraint violation because `sec_permission` rows still reference it.
- **Sequence generator collision:** New entities use `@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")` — the same `sequence_generator` sequence as JHI entities. This is correct; the sequence has `incrementBy=50` so allocation is safe.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| DTO ↔ entity mapping | Manual field-by-field copy methods | MapStruct `@Mapper(componentModel="spring")` extending `EntityMapper` | Compile-time safety, `partialUpdate` support, null-safety |
| "Not Found" response | Custom 404 check | `ResponseUtil.wrapOrNotFound(Optional)` | Established JHipster pattern; handles `Optional.empty()` → 404 |
| Pagination headers | Custom header builder | `PaginationUtil.generatePaginationHttpHeaders(...)` | JHipster standard; frontend clients parse these headers |
| FK cascade on role delete | Application-level guard | DB `onDelete=CASCADE` on `sec_permission.authority_name` | Simpler, transactionally correct, no orphan risk |
| Authority validation in bridge | Loading all authorities then filtering in memory | `authorityRepository.findAllById(jwtAuthorities)` | Single IN-query vs N queries; existing cache on AuthorityRepository |

**Key insight:** The angapp donor code already solves all entity/repository/enum design problems. The task is adaptation (FK strategy change) not invention.

---

## Common Pitfalls

### Pitfall 1: Authority Persistable State
**What goes wrong:** Creating a new `Authority` (for the role admin endpoint) without calling `setIsPersisted()` after save causes Spring Data to attempt an INSERT when the entity is already in the database.
**Why it happens:** `Authority` implements `Persistable<String>` with a transient `isPersisted` flag. The flag is set by `@PostLoad`/`@PostPersist`. A freshly constructed Authority is always `isNew() = true` until persisted.
**How to avoid:** After `authorityRepository.save(authority)` the `@PostPersist` fires automatically. Do not manually construct `Authority` objects that bypass the JPA lifecycle (e.g., avoid constructing and immediately returning without going through the repository).
**Warning signs:** `DataIntegrityViolationException: duplicate key value` on a create operation that appears to be for a new record.

### Pitfall 2: ArchUnit Layer Boundary for security.domain and security.repository
**What goes wrong:** Placing `SecPermission` and `SecRowPolicy` in `com.vn.core.security.domain` and `com.vn.core.security.repository` may violate the ArchUnit layer rules in `TechnicalStructureTest` because the rule uses package patterns like `..domain..` and `..repository..`.
**Why it happens:** ArchUnit's `layeredArchitecture()` uses `..security..` for the Security layer. Sub-packages like `security.domain` match BOTH `..security..` AND `..domain..`. ArchUnit will see these classes as belonging to the Security layer, not the Domain layer, because `..security..` is a more specific match.
**How to avoid:** Verify by running `./gradlew test` after adding entities. If ArchUnit complains, consider moving entities to `com.vn.core.domain` (the existing Domain package) and repositories to `com.vn.core.repository`. Alternatively, add an explicit `ignoreDependency` in `TechnicalStructureTest` for the security sub-packages.
**Warning signs:** `ArchConditionViolationException` mentioning `SecPermission` or `SecRowPolicy` during the test run.

### Pitfall 3: Hazelcast Cache Registration for New Entities
**What goes wrong:** New entities that use Hibernate second-level cache annotations (`@Cache`) require explicit Hazelcast cache registration in `CacheConfiguration`. Missing registration causes startup warnings or silent cache misses.
**Why it happens:** `CacheConfiguration` calls `createCache(cm, Authority.class.getName())` etc. for each cached entity. New entities are not auto-discovered.
**How to avoid:** For Phase 2, do NOT add `@Cache` to `SecPermission` or `SecRowPolicy` — these are admin-managed, low-frequency entities. Only `Authority` (already cached) needs caching. Leave new entities uncached.
**Warning signs:** `com.hazelcast.core.HazelcastException: Cache ... not found` in startup logs.

### Pitfall 4: MergedSecurityContextBridge Creates Circular Dependencies
**What goes wrong:** `MergedSecurityContextBridge` depends on `AuthorityRepository`. If `AuthorityRepository` (or its cache) depends on anything in the Security layer, a circular Spring bean dependency can occur.
**Why it happens:** `AuthorityRepository` is in the Persistence layer. `MergedSecurityContextBridge` is in the Security layer. Persistence → Security would violate ArchUnit (Security may access Persistence; not vice versa). However, Spring bean wiring is separate from ArchUnit package rules — the actual circular dependency risk is with `DomainUserDetailsService` which also uses `AuthorityRepository`. Test for `BeanCurrentlyInCreationException` at startup.
**How to avoid:** Keep `MergedSecurityContextBridge` a lightweight component that only uses `AuthorityRepository`. Do not inject `UserService` or other heavy service beans into the bridge.
**Warning signs:** `BeanCurrentlyInCreationException` at application startup.

### Pitfall 5: sec_permission FK Points to jhi_authority but jhi_authority has max 50 chars
**What goes wrong:** `jhi_authority.name` is `VARCHAR(50)`. The `authority_name` FK column in `sec_permission` must also be `VARCHAR(50)`. Declaring it as `VARCHAR(100)` would pass Liquibase but create a mismatch that may cause issues on some DB dialects.
**Why it happens:** Copy-paste from angapp's `sec_role.code` which is `VARCHAR(100)`. The target PK is only 50 chars.
**How to avoid:** Declare `authority_name` as `varchar(50)` in the `sec_permission` Liquibase changelog. Matches the `@Size(max = 50)` on `Authority.name`.
**Warning signs:** FK constraint creation failure in Liquibase with "column type mismatch" error.

### Pitfall 6: SecurityContextBridgeWiringIT Breaks After Adding @Primary Bridge
**What goes wrong:** Existing `SecurityContextBridgeWiringIT` asserts `bridge instanceof JHipsterSecurityContextBridge`. After Phase 2 adds `@Primary MergedSecurityContextBridge`, this assertion will fail.
**Why it happens:** Spring now injects the `@Primary` bean, not the `@Component` default.
**How to avoid:** Update `SecurityContextBridgeWiringIT` to assert `bridge instanceof MergedSecurityContextBridge` instead. Add a new test verifying that `JHipsterSecurityContextBridge` is still present as a non-primary bean (use `@Autowired List<SecurityContextBridge> allBridges` to verify both exist).
**Warning signs:** `SecurityContextBridgeWiringIT` test failure immediately after adding the new bridge.

---

## Code Examples

Verified patterns from project source:

### Authority entity (existing — shows isPersisted lifecycle)
```java
// Source: src/main/java/com/vn/core/domain/Authority.java
@PostLoad
@PostPersist
public void updateEntityState() {
    this.setIsPersisted();
}

@Override
public boolean isNew() {
    return !this.isPersisted;
}
```

### Admin endpoint authorization pattern
```java
// Source: src/main/java/com/vn/core/web/rest/UserResource.java
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public ResponseEntity<AdminUserDTO> createUser(@Valid @RequestBody AdminUserDTO userDTO)
    throws URISyntaxException { ... }
```

### ResponseUtil for Optional GET
```java
// Source: src/main/java/com/vn/core/web/rest/AuthorityResource.java
return ResponseUtil.wrapOrNotFound(authorityRepository.findById(id));
```

### EntityMapper interface
```java
// Source: angapp/src/main/java/com/mycompany/myapp/service/mapper/EntityMapper.java
public interface EntityMapper<D, E> {
    E toEntity(D dto);
    D toDto(E entity);
    List<E> toEntity(List<D> dtoList);
    List<D> toDto(List<E> entityList);
    @Named("partialUpdate")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(@MappingTarget E entity, D dto);
}
```

### SecRowPolicyRepository query (Phase 3 dependency)
```java
// Source: angapp SecRowPolicyRepository.java (verbatim — no FK adaptation needed)
List<SecRowPolicy> findByEntityNameAndOperation(String entityName, EntityOp operation);
```

### SecPermissionRepository query (Phase 3 dependency — adapted)
```java
// Source: angapp SecPermissionRepository.java (adapted: role.code -> authorityName)
@Query("select p from SecPermission p " +
       "where p.authorityName in :authorityNames " +
       "and p.targetType = :targetType " +
       "and p.target = :target " +
       "and p.action = :action")
List<SecPermission> findByRolesAndTarget(
    @Param("authorityNames") Collection<String> authorityNames,
    @Param("targetType") TargetType targetType,
    @Param("target") String target,
    @Param("action") String action
);
```

### Integration test pattern (existing baseline to follow)
```java
// Source: src/test/java/com/vn/core/web/rest/AuthorityResourceIT.java
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = { "ROLE_ADMIN" })
class SecPermissionAdminResourceIT {
    @Autowired private MockMvc restMockMvc;
    @Autowired private SecPermissionRepository repository;
    // ...
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `@SpringBootTest` + `@AutoConfigureTestDatabase` + H2 | `@IntegrationTest` + Testcontainers PostgreSQL | Phase 1 established | All new ITs must use `@IntegrationTest` annotation, not bare `@SpringBootTest` |
| `@Primary` on default bridge | Non-`@Primary` default (`JHipsterSecurityContextBridge`) | Phase 1 locked | Phase 2 must add `@Primary` to its override; never retrofit `@Primary` onto Phase 1 |
| angapp's Long role FK in sec_permission | String authority_name FK | Phase 2 decision | Repository query uses `p.authorityName in :authorityNames` not `p.role.code in :roleCodes` |

**Deprecated/outdated in this context:**
- angapp's `sec_role` table and `SecRole` entity: not ported — `jhi_authority` is the merged table (D-01)
- angapp's `sec_fetch_plan` table: explicitly excluded from Phase 2 schema (D-07, project constraint on fetch-plan storage)
- angapp's seed data `sec_role` rows: equivalent seed data goes to `jhi_authority` inserts with `type` column populated

---

## Open Questions

1. **ArchUnit violations from security.domain and security.repository sub-packages**
   - What we know: `TechnicalStructureTest` uses `..security..`, `..domain..`, `..repository..` patterns. `com.vn.core.security.domain` matches both Security and Domain patterns simultaneously.
   - What's unclear: Whether ArchUnit's layered architecture assigns the more specific match (`..security..`) or the last-matched layer. The test uses `consideringAllDependencies()`.
   - Recommendation: Run `./gradlew test` after the first entity is placed in `security.domain`. If it fails, either move entities to `com.vn.core.domain` (simpler) or add ignores. Investigate before committing to the `security.domain` package location locked by D-10/D-13.

2. **`SecRoleDTO` vs extending existing `AuthorityDTO`**
   - What we know: There is no existing `AuthorityDTO` — `AuthorityResource` currently exposes the `Authority` entity directly. D-20 requires a `SecRoleDTO`.
   - What's unclear: Whether the new `SecRoleDTO` should be used by the existing `AuthorityResource` too (making it consistent), or only by the new `/api/admin/sec/roles` endpoint.
   - Recommendation: Keep `AuthorityResource` unchanged (brownfield safety); `SecRoleDTO` serves only the new endpoint. This is the minimum-regression path.

3. **PATCH vs PUT for update endpoints**
   - What we know: D-20 defers this to Claude's discretion. The existing `UserResource` uses `PUT` only for updates.
   - What's unclear: Whether Phase 3 callers will need partial updates.
   - Recommendation: Start with `PUT` only (consistent with existing endpoints). Add `PATCH` only if Phase 3 or test scenarios require partial updates.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 via Spring Boot Test |
| Config file | `src/test/java/com/vn/core/IntegrationTest.java` (composite annotation) |
| Quick run command | `./gradlew test --tests "com.vn.core.security.*"` |
| Full suite command | `./gradlew test integrationTest` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SEC-01 | Admin CRUD on merged roles via `/api/admin/sec/roles` | Integration (MockMvc + Testcontainers) | `./gradlew integrationTest --tests "*.SecRoleAdminResourceIT"` | Wave 0 |
| SEC-01 | `jhi_authority` gains `display_name` and `type` columns with defaults | Integration (schema) | `./gradlew integrationTest --tests "*.SecRoleAdminResourceIT"` | Wave 0 |
| SEC-01 | Non-admin cannot access role admin endpoints (403) | Integration | included in `SecRoleAdminResourceIT` | Wave 0 |
| SEC-02 | Admin CRUD on permissions via `/api/admin/sec/permissions` | Integration (MockMvc + Testcontainers) | `./gradlew integrationTest --tests "*.SecPermissionAdminResourceIT"` | Wave 0 |
| SEC-02 | FK cascade: deleting a role deletes its permissions | Integration | included in `SecPermissionAdminResourceIT` | Wave 0 |
| SEC-03 | Admin CRUD on row policies via `/api/admin/sec/row-policies` | Integration (MockMvc + Testcontainers) | `./gradlew integrationTest --tests "*.SecRowPolicyAdminResourceIT"` | Wave 0 |
| SEC-03 | `code` uniqueness constraint enforced at DB level | Integration | included in `SecRowPolicyAdminResourceIT` | Wave 0 |
| SEC-04 (updated) | `MergedSecurityContextBridge` is wired as `@Primary` | Integration | `./gradlew integrationTest --tests "*.SecurityContextBridgeWiringIT"` | Exists (update needed) |
| SEC-04 (updated) | Bridge filters phantom authority names | Unit | `./gradlew test --tests "*.MergedSecurityContextBridgeTest"` | Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew test --tests "com.vn.core.security.*" --tests "com.vn.core.web.rest.admin.*"`
- **Per wave merge:** `./gradlew test integrationTest`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/com/vn/core/web/rest/admin/security/SecRoleAdminResourceIT.java` — covers SEC-01
- [ ] `src/test/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResourceIT.java` — covers SEC-02
- [ ] `src/test/java/com/vn/core/web/rest/admin/security/SecRowPolicyAdminResourceIT.java` — covers SEC-03
- [ ] `src/test/java/com/vn/core/security/bridge/MergedSecurityContextBridgeTest.java` — covers bridge filtering (unit test)
- [ ] Update `src/test/java/com/vn/core/security/bridge/SecurityContextBridgeWiringIT.java` — assert `@Primary` bridge wins

No new test framework installation needed — JUnit 5, MockMvc, Testcontainers are all already wired.

---

## Sources

### Primary (HIGH confidence)
- `angapp/src/main/java/com/mycompany/core/security/domain/SecPermission.java` — entity field shape
- `angapp/src/main/java/com/mycompany/core/security/domain/SecRowPolicy.java` — entity field shape
- `angapp/src/main/java/com/mycompany/core/security/domain/SecRole.java` — RoleType enum values
- `angapp/src/main/java/com/mycompany/core/security/repository/SecPermissionRepository.java` — query signature
- `angapp/src/main/java/com/mycompany/core/security/repository/SecRowPolicyRepository.java` — query signature
- `angapp/src/main/java/com/mycompany/core/security/core/SecurityService.java` — MergedSecurityService interface to port
- `angapp/src/main/java/com/mycompany/core/security/core/SecurityServiceImpl.java` — currentAuthorities / currentUserId implementation
- `angapp/src/main/resources/config/liquibase/changelog/20260319000100_security_metadata.xml` — schema reference
- `angapp/src/main/resources/config/liquibase/changelog/20260319001000_security_seed_roles_permissions.xml` — seed data reference
- `src/main/java/com/vn/core/domain/Authority.java` — Persistable pattern, isPersisted lifecycle
- `src/main/java/com/vn/core/web/rest/UserResource.java` — admin endpoint pattern
- `src/main/java/com/vn/core/web/rest/AuthorityResource.java` — CRUD controller pattern
- `src/main/java/com/vn/core/security/bridge/SecurityContextBridge.java` — Phase 1 interface to implement
- `src/main/java/com/vn/core/security/bridge/JHipsterSecurityContextBridge.java` — non-@Primary default; must not be modified
- `src/test/java/com/vn/core/web/rest/AuthorityResourceIT.java` — IT test pattern to follow
- `src/test/java/com/vn/core/TechnicalStructureTest.java` — ArchUnit layer rules; new packages must comply
- `src/main/resources/config/liquibase/master.xml` — changelog registration point
- `src/main/resources/config/liquibase/changelog/00000000000000_initial_schema.xml` — sequence_generator definition, jhi_authority schema

### Secondary (MEDIUM confidence)
- `angapp/src/main/java/com/mycompany/myapp/service/mapper/EntityMapper.java` — MapStruct base interface to copy to root project
- `angapp/src/main/java/com/mycompany/myapp/service/mapper/OrganizationMapper.java` — MapStruct `@Mapper(componentModel="spring")` pattern
- `angapp/src/main/java/com/mycompany/core/security/permission/TargetType.java` — enum to port (excluding FETCH_PLAN)
- `angapp/src/main/java/com/mycompany/core/security/permission/EntityOp.java` — enum to port verbatim

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries already on classpath; no new dependencies needed
- Architecture: HIGH — donor code provides exact entity/repository shapes; adaptation is FK strategy only
- Pitfalls: HIGH — based on reading actual existing code (Authority.isPersisted, ArchUnit rules, existing IT patterns)
- ArchUnit sub-package behavior: MEDIUM — the specific matching behavior of `..security..` vs `..domain..` for sub-packages requires a runtime test to confirm

**Research date:** 2026-03-21
**Valid until:** 2026-04-21 (stable framework stack)
