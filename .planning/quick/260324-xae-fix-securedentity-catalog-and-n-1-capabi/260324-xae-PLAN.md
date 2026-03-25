---
phase: quick
plan: 260324-xae
type: execute
wave: 1
depends_on: []
files_modified:
  - src/main/java/com/vn/core/security/catalog/SecuredEntity.java
  - src/main/java/com/vn/core/security/catalog/MetamodelSecuredEntityCatalog.java
  - src/main/java/com/vn/core/security/repository/SecPermissionRepository.java
  - src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java
autonomous: true
requirements: []
must_haves:
  truths:
    - "Secured entity catalog auto-discovers entities from @SecuredEntity annotation without hardcoded imports"
    - "Entity capabilities endpoint returns identical JSON as before (organization, department, employee with correct permissions)"
    - "Capability loading executes 2 DB queries instead of ~144"
  artifacts:
    - path: "src/main/java/com/vn/core/security/catalog/SecuredEntity.java"
      provides: "Annotation with code() and fetchPlanCodes() defaults"
    - path: "src/main/java/com/vn/core/security/catalog/MetamodelSecuredEntityCatalog.java"
      provides: "Annotation-driven catalog without hardcoded entity references"
    - path: "src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java"
      provides: "Bulk-load capability evaluation via PermissionMatrix"
  key_links:
    - from: "MetamodelSecuredEntityCatalog"
      to: "@SecuredEntity annotation"
      via: "entityClass.getAnnotation(SecuredEntity.class)"
      pattern: "getAnnotation.*SecuredEntity"
    - from: "SecuredEntityCapabilityService"
      to: "SecPermissionRepository.findAllByAuthorityNameIn"
      via: "single bulk query replaces N+1"
      pattern: "findAllByAuthorityNameIn"
---

<objective>
Fix two problems in the security capability subsystem:
1. Remove hardcoded entity references from MetamodelSecuredEntityCatalog — derive code and fetchPlanCodes from the @SecuredEntity annotation metadata.
2. Replace N+1 query pattern in SecuredEntityCapabilityService (~144 queries, ~30s) with bulk-load + in-memory PermissionMatrix (2 queries).

Purpose: Eliminate coupling to specific entity classes and fix a critical performance bottleneck.
Output: Refactored catalog and capability service with identical external behavior.
</objective>

<execution_context>
@.claude/get-shit-done/workflows/execute-plan.md
@.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@src/main/java/com/vn/core/security/catalog/SecuredEntity.java
@src/main/java/com/vn/core/security/catalog/SecuredEntityEntry.java
@src/main/java/com/vn/core/security/catalog/SecuredEntityCatalog.java
@src/main/java/com/vn/core/security/catalog/MetamodelSecuredEntityCatalog.java
@src/main/java/com/vn/core/security/repository/SecPermissionRepository.java
@src/main/java/com/vn/core/security/domain/SecPermission.java
@src/main/java/com/vn/core/security/permission/TargetType.java
@src/main/java/com/vn/core/security/permission/EntityOp.java
@src/main/java/com/vn/core/security/MergedSecurityService.java
@src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java
@src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java
@src/main/java/com/vn/core/security/permission/RolePermissionServiceDbImpl.java
@src/test/java/com/vn/core/web/rest/SecuredEntityCapabilityResourceIT.java

<interfaces>
<!-- Key types the executor needs -->

From SecuredEntityEntry.java (record — no changes needed):
```java
public record SecuredEntityEntry(
    Class<?> entityClass, String code, Set<EntityOp> operations,
    List<String> fetchPlanCodes, boolean jpqlAllowed
) { /* builder pattern */ }
```

From SecPermission.java (entity — fields used for PermissionMatrix):
```java
// Fields: authorityName, targetType (TargetType enum), target (String), action (String), effect (String)
// TargetType: ENTITY, ATTRIBUTE, ROW_POLICY
// effect: "ALLOW" or "DENY"
// Entity target format: "ORGANIZATION" (uppercase simple class name)
// Attribute target format: "ORGANIZATION.BUDGET" (uppercase ENTITY.ATTRIBUTE)
```

From MergedSecurityService.java:
```java
Collection<String> getCurrentUserAuthorityNames();
```

From EntityOp.java:
```java
// Values: READ, CREATE, UPDATE, DELETE
```

Permission semantics (from existing evaluators — MUST be preserved exactly):
- Entity-level: DENY-wins. No ALLOW record = denied. No authorities = denied.
- Attribute-level: No records = denied (deny-default). DENY-wins when records exist.
  ALLOW on "ENTITY.*" wildcard grants all attributes of that entity.
- Both use target uppercase: entityClass.getSimpleName().toUpperCase(ROOT)
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add annotation attributes and rewrite MetamodelSecuredEntityCatalog</name>
  <files>
    src/main/java/com/vn/core/security/catalog/SecuredEntity.java
    src/main/java/com/vn/core/security/catalog/MetamodelSecuredEntityCatalog.java
  </files>
  <action>
**SecuredEntity.java** — Add two annotation attributes with defaults:
```java
String code() default "";
String[] fetchPlanCodes() default {};
```
Keep existing Javadoc. Add inline Javadoc for each attribute:
- `code`: "Lowercase entity code. Defaults to lowercase simple class name if empty."
- `fetchPlanCodes`: "Fetch-plan codes. Defaults to {code}-list and {code}-detail if empty."

**MetamodelSecuredEntityCatalog.java** — Complete rewrite:
- Remove ALL imports of Organization, Department, Employee
- Remove the static PROOF_ENTRIES map entirely
- Add a `private final List<SecuredEntityEntry> cachedEntries` field
- In the constructor (takes EntityManager), scan `entityManager.getMetamodel().getEntities()`:
  1. Filter to classes annotated with `@SecuredEntity`
  2. For each, read the annotation: `entityClass.getAnnotation(SecuredEntity.class)`
  3. Derive code: if `annotation.code()` is non-empty use it, else `entityClass.getSimpleName().toLowerCase(Locale.ROOT)`
  4. Derive fetchPlanCodes: if `annotation.fetchPlanCodes().length > 0` use `List.of(annotation.fetchPlanCodes())`, else `List.of(code + "-list", code + "-detail")`
  5. Build `SecuredEntityEntry` with: entityClass, code, `EnumSet.of(READ, CREATE, UPDATE, DELETE)`, fetchPlanCodes, jpqlAllowed=false
  6. Sort by code, collect to unmodifiable list, assign to `cachedEntries`
- `entries()`: return `cachedEntries` directly (no re-scan)
- `findByEntityClass(Class<?>)`: stream over cachedEntries, filter, findFirst
- `findByCode(String)`: stream over cachedEntries, filter, findFirst

Keep @Component @Primary annotations. Keep the existing class-level Javadoc.
  </action>
  <verify>
    <automated>cd D:/jhipster && ./gradlew compileJava --no-daemon -q 2>&1 | tail -20</automated>
  </verify>
  <done>SecuredEntity annotation has code() and fetchPlanCodes() attributes. MetamodelSecuredEntityCatalog has zero hardcoded entity references, derives all metadata from annotation, caches at construction.</done>
</task>

<task type="auto">
  <name>Task 2: Add bulk query and rewrite SecuredEntityCapabilityService with PermissionMatrix</name>
  <files>
    src/main/java/com/vn/core/security/repository/SecPermissionRepository.java
    src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java
  </files>
  <action>
**SecPermissionRepository.java** — Add one method:
```java
@Query("select p from SecPermission p where p.authorityName in :authorityNames")
List<SecPermission> findAllByAuthorityNameIn(@Param("authorityNames") Collection<String> authorityNames);
```

**SecuredEntityCapabilityService.java** — Rewrite to use bulk loading:

Remove injections of `EntityPermissionEvaluator` and `AttributePermissionEvaluator`. Keep `SecuredEntityCatalog` and `EntityManager`. Add injections of `MergedSecurityService` and `SecPermissionRepository`.

Add a private static inner class `PermissionMatrix`:
- Constructor takes `List<SecPermission> permissions`
- Partition internally into entity permissions (targetType=ENTITY) and attribute permissions (targetType=ATTRIBUTE)
- Use a `Set<String>` for allowed keys and denied keys. Key format: "TARGETTYPE:TARGET:ACTION" (e.g. "ENTITY:ORGANIZATION:CREATE" or "ATTRIBUTE:ORGANIZATION.BUDGET:VIEW")
- Static `EMPTY` instance (empty sets)
- `boolean isEntityPermitted(String target, String action)`:
  - If denied key "ENTITY:target:action" exists, return false (DENY-wins)
  - Return true only if allowed key "ENTITY:target:action" exists
  - No ALLOW = denied (matches current EntityPermissionEvaluator behavior)
- `boolean isAttributePermitted(String attrTarget, String action)`:
  - Build key "ATTRIBUTE:attrTarget:action"
  - Build wildcard key using entity part: "ATTRIBUTE:" + attrTarget.split("\\.")[0] + ".*:" + action
  - If denied key exists for specific OR wildcard, return false (DENY-wins)
  - If allowed key exists for specific OR wildcard, return true
  - No matching records at all = denied (matches current deny-default behavior from AttributePermissionEvaluatorImpl)

Rewrite `getCurrentUserCapabilities()`:
1. `Collection<String> authorities = mergedSecurityService.getCurrentUserAuthorityNames()`
2. If authorities empty: build PermissionMatrix.EMPTY, map all catalog entries to all-false DTOs
3. Else: `List<SecPermission> allPerms = secPermissionRepository.findAllByAuthorityNameIn(authorities)`
4. Build `PermissionMatrix matrix = new PermissionMatrix(allPerms)`
5. Map `securedEntityCatalog.entries()` sorted by code to DTOs using `toDto(entry, matrix)`

`toDto(SecuredEntityEntry entry, PermissionMatrix matrix)`:
- `String target = entry.entityClass().getSimpleName().toUpperCase(Locale.ROOT)`
- Map EntityOp names (READ, CREATE, UPDATE, DELETE) to canRead/canCreate/canUpdate/canDelete via `matrix.isEntityPermitted(target, op)`
- Build attributes via `attributesFor(entry, matrix)`

`attributesFor(SecuredEntityEntry entry, PermissionMatrix matrix)`:
- Use `entityManager.getMetamodel().entity(entry.entityClass()).getAttributes()` sorted by name
- For each attribute: `String attrTarget = target + "." + attr.getName().toUpperCase(Locale.ROOT)`
- `canView = matrix.isAttributePermitted(attrTarget, "VIEW")`
- `canEdit = matrix.isAttributePermitted(attrTarget, "EDIT")`

CRITICAL: The permission semantics MUST match the existing behavior exactly:
- Entity: DENY-wins, no ALLOW = denied (from RolePermissionServiceDbImpl)
- Attribute: deny-default (no records = denied), DENY-wins when records exist (from AttributePermissionEvaluatorImpl)
- Wildcard: ENTITY.* pattern for attribute ALLOW (check if "ATTRIBUTE:ENTITY.*:ACTION" is ALLOWED and not DENIED)
- Target format: uppercase simple class name for entity, uppercase ENTITY.ATTRIBUTE for attributes
  </action>
  <verify>
    <automated>cd D:/jhipster && ./gradlew compileJava --no-daemon -q 2>&1 | tail -20 && ./gradlew test --tests "com.vn.core.web.rest.SecuredEntityCapabilityResourceIT" --no-daemon 2>&1 | tail -30</automated>
  </verify>
  <done>SecPermissionRepository has findAllByAuthorityNameIn bulk method. SecuredEntityCapabilityService uses 2 queries (authorities + permissions) instead of ~144. All 3 integration tests in SecuredEntityCapabilityResourceIT pass with identical behavior. No EntityPermissionEvaluator or AttributePermissionEvaluator injected in the service.</done>
</task>

</tasks>

<verification>
1. `./gradlew compileJava` succeeds with no errors
2. `./gradlew test --tests "com.vn.core.web.rest.SecuredEntityCapabilityResourceIT"` — all 3 tests pass
3. `./gradlew test --tests "com.vn.core.TechnicalStructureTest"` — ArchUnit passes (no new layer violations since entity imports removed)
4. MetamodelSecuredEntityCatalog.java has zero imports from `com.vn.core.domain`
</verification>

<success_criteria>
- @SecuredEntity annotation has code() and fetchPlanCodes() with sensible defaults
- MetamodelSecuredEntityCatalog has no hardcoded entity references — fully annotation-driven
- SecuredEntityCapabilityService uses bulk permission loading (2 queries total)
- All existing integration tests pass with identical permission results
- Commit message: "refactor(security): auto-discover secured entities from annotation, fix N+1 capability loading"
</success_criteria>

<output>
After completion, create `.planning/quick/260324-xae-fix-securedentity-catalog-and-n-1-capabi/260324-xae-SUMMARY.md`
</output>
