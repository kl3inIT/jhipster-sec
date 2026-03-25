---
phase: quick
plan: 260325-lae
type: execute
wave: 1
depends_on: []
files_modified:
  # Liquibase migration
  - src/main/resources/config/liquibase/changelog/20260325000200_rename_nav_grant_add_menu_def.xml
  - src/main/resources/config/liquibase/master.xml
  # Renamed Java entity + repository
  - src/main/java/com/vn/core/security/domain/SecMenuPermission.java
  - src/main/java/com/vn/core/security/repository/SecMenuPermissionRepository.java
  # New entity
  - src/main/java/com/vn/core/security/domain/SecMenuDefinition.java
  - src/main/java/com/vn/core/security/repository/SecMenuDefinitionRepository.java
  # Renamed service + DTO + resource
  - src/main/java/com/vn/core/service/security/CurrentUserMenuPermissionService.java
  - src/main/java/com/vn/core/service/dto/security/MenuPermissionResponseDTO.java
  - src/main/java/com/vn/core/web/rest/MenuPermissionResource.java
  # Delete old files
  - src/main/java/com/vn/core/security/domain/SecNavigationGrant.java
  - src/main/java/com/vn/core/security/repository/SecNavigationGrantRepository.java
  - src/main/java/com/vn/core/service/security/CurrentUserNavigationGrantService.java
  - src/main/java/com/vn/core/service/dto/security/NavigationGrantResponseDTO.java
  - src/main/java/com/vn/core/web/rest/NavigationGrantResource.java
  # Renamed tests
  - src/test/java/com/vn/core/service/security/CurrentUserMenuPermissionServiceTest.java
  - src/test/java/com/vn/core/web/rest/MenuPermissionResourceIT.java
  - src/test/java/com/vn/core/service/security/CurrentUserNavigationGrantServiceTest.java
  - src/test/java/com/vn/core/web/rest/NavigationGrantResourceIT.java
  # Frontend
  - frontend/src/app/layout/navigation/navigation.service.ts
  - frontend/src/app/layout/navigation/navigation.service.spec.ts
  - frontend/src/app/layout/navigation/navigation.constants.ts
  - frontend/e2e/security-comprehensive.spec.ts
autonomous: true
requirements: []
must_haves:
  truths:
    - "Backend entity is named SecMenuPermission with fields role, appName, menuId, effect"
    - "REST endpoint /api/security/menu-permissions returns allowedMenuIds (not allowedNodeIds)"
    - "SecMenuDefinition table exists with menuId, appName, label, description, parentMenuId, route, icon, ordering"
    - "Liquibase migration renames table/columns and creates new table without data loss"
    - "Frontend NavigationService uses new endpoint and field names"
    - "All unit and integration tests pass with renamed classes and fields"
  artifacts:
    - path: "src/main/java/com/vn/core/security/domain/SecMenuPermission.java"
      provides: "Renamed entity with role/menuId fields"
    - path: "src/main/java/com/vn/core/security/domain/SecMenuDefinition.java"
      provides: "New menu metadata entity"
    - path: "src/main/resources/config/liquibase/changelog/20260325000200_rename_nav_grant_add_menu_def.xml"
      provides: "Migration for rename + new table"
  key_links:
    - from: "MenuPermissionResource"
      to: "CurrentUserMenuPermissionService"
      via: "getAllowedMenuIds(appName)"
    - from: "frontend navigation.service.ts"
      to: "/api/security/menu-permissions"
      via: "HTTP GET with appName param"
---

<objective>
Rename SecNavigationGrant to SecMenuPermission (fields: authorityName->role, nodeId->menuId), add SecMenuDefinition table, update all backend classes/tests, update frontend NavigationService contract, and create Liquibase migration.

Purpose: Align naming with the domain model (menu permissions, not navigation grants) and add a menu definition table for backend-driven menu metadata.
Output: Renamed entity stack, new SecMenuDefinition entity, Liquibase migration, updated frontend service.
</objective>

<execution_context>
@D:/.claude/get-shit-done/workflows/execute-plan.md
@D:/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md

Existing files to rename/replace:
- src/main/java/com/vn/core/security/domain/SecNavigationGrant.java
- src/main/java/com/vn/core/security/repository/SecNavigationGrantRepository.java
- src/main/java/com/vn/core/service/security/CurrentUserNavigationGrantService.java
- src/main/java/com/vn/core/service/dto/security/NavigationGrantResponseDTO.java
- src/main/java/com/vn/core/web/rest/NavigationGrantResource.java
- src/test/java/com/vn/core/service/security/CurrentUserNavigationGrantServiceTest.java
- src/test/java/com/vn/core/web/rest/NavigationGrantResourceIT.java
- frontend/src/app/layout/navigation/navigation.service.ts
- frontend/src/app/layout/navigation/navigation.service.spec.ts
- frontend/src/app/layout/navigation/navigation.constants.ts
- frontend/e2e/security-comprehensive.spec.ts

<interfaces>
<!-- Current REST contract (will change): -->
GET /api/security/navigation-grants?appName=X
Response: { appName: string, allowedNodeIds: string[] }

<!-- New REST contract: -->
GET /api/security/menu-permissions?appName=X
Response: { appName: string, allowedMenuIds: string[] }

<!-- Field rename mapping: -->
Entity: SecNavigationGrant -> SecMenuPermission
Table: sec_navigation_grant -> sec_menu_permission
Column: authority_name -> role (Java field: authorityName -> role)
Column: node_id -> menu_id (Java field: nodeId -> menuId)
Column: app_name -> app_name (unchanged)
Column: effect -> effect (unchanged)

<!-- New entity: SecMenuDefinition -->
Table: sec_menu_definition
Columns: id (bigint PK), menu_id (varchar 150), app_name (varchar 100), label (varchar 200),
         description (varchar 500 nullable), parent_menu_id (varchar 150 nullable),
         route (varchar 300 nullable), icon (varchar 100 nullable), ordering (integer)
Unique: (app_name, menu_id)
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Liquibase migration + renamed Java entity/repository + new SecMenuDefinition entity</name>
  <files>
    src/main/resources/config/liquibase/changelog/20260325000200_rename_nav_grant_add_menu_def.xml,
    src/main/resources/config/liquibase/master.xml,
    src/main/java/com/vn/core/security/domain/SecMenuPermission.java,
    src/main/java/com/vn/core/security/repository/SecMenuPermissionRepository.java,
    src/main/java/com/vn/core/security/domain/SecMenuDefinition.java,
    src/main/java/com/vn/core/security/repository/SecMenuDefinitionRepository.java
  </files>
  <action>
    **Liquibase migration** `20260325000200_rename_nav_grant_add_menu_def.xml`:

    ChangeSet 1 - Rename table and columns on sec_navigation_grant:
    - `renameTable` from `sec_navigation_grant` to `sec_menu_permission`
    - `renameColumn` on `sec_menu_permission`: `authority_name` -> `role` (type varchar(50))
    - `renameColumn` on `sec_menu_permission`: `node_id` -> `menu_id` (type varchar(150))
    - Drop old unique constraint `ux_sec_navigation_grant_authority_app_node`, add new `ux_sec_menu_permission_role_app_menu` on (role, app_name, menu_id)
    - Drop old indexes `idx_sec_navigation_grant_app_authority` and `idx_sec_navigation_grant_app_node`, create new `idx_sec_menu_permission_app_role` on (app_name, role) and `idx_sec_menu_permission_app_menu` on (app_name, menu_id)
    - Drop old FK `fk_sec_navigation_grant_authority`, add new FK `fk_sec_menu_permission_role` from `role` referencing `jhi_authority(name)` with onDelete CASCADE

    ChangeSet 2 - Create sec_menu_definition table:
    - id: bigint, autoIncrement, PK, not null
    - menu_id: varchar(150), not null
    - app_name: varchar(100), not null
    - label: varchar(200), not null
    - description: varchar(500), nullable
    - parent_menu_id: varchar(150), nullable
    - route: varchar(300), nullable
    - icon: varchar(100), nullable
    - ordering: integer, not null, defaultValueNumeric=0
    - Unique constraint `ux_sec_menu_definition_app_menu` on (app_name, menu_id)
    - Index `idx_sec_menu_definition_app_parent` on (app_name, parent_menu_id)

    Add include to `master.xml` after the existing `20260325000100_create_sec_navigation_grant.xml` line.

    **SecMenuPermission.java** — Copy from SecNavigationGrant.java and rename:
    - Class: `SecMenuPermission`
    - @Table name: `sec_menu_permission`
    - Unique constraint name: `ux_sec_menu_permission_role_app_menu`, columns: `role, app_name, menu_id`
    - Index names: `idx_sec_menu_permission_app_role` (app_name, role), `idx_sec_menu_permission_app_menu` (app_name, menu_id)
    - Field `authorityName` -> `role`, column `role`
    - Field `nodeId` -> `menuId`, column `menu_id`
    - Update all fluent setters, getters, toString, equals accordingly
    - Keep `appName` and `effect` unchanged

    **SecMenuPermissionRepository.java** — Copy from SecNavigationGrantRepository:
    - Interface: `SecMenuPermissionRepository extends JpaRepository<SecMenuPermission, Long>`
    - Method: `findAllByAppNameAndRoleIn(String appName, Collection<String> roles)` (was authorityNameIn)
    - Method: `findAllByAppName(String appName)`

    **SecMenuDefinition.java** — New entity:
    - @Entity, @Table(name = "sec_menu_definition") with unique constraint on (app_name, menu_id)
    - Index on (app_name, parent_menu_id)
    - Fields: id (Long, sequence), menuId (String, 150), appName (String, 100), label (String, 200), description (String, 500, nullable), parentMenuId (String, 150, nullable), route (String, 300, nullable), icon (String, 100, nullable), ordering (Integer, not null)
    - Standard getters/setters, fluent setters, equals (id-based), hashCode, toString
    - Implements Serializable with @Serial serialVersionUID

    **SecMenuDefinitionRepository.java** — New repository:
    - `findAllByAppName(String appName)`
    - `findByAppNameAndMenuId(String appName, String menuId)` returning Optional

    **Delete old files** using git rm:
    - `src/main/java/com/vn/core/security/domain/SecNavigationGrant.java`
    - `src/main/java/com/vn/core/security/repository/SecNavigationGrantRepository.java`
  </action>
  <verify>
    <automated>cd D:/jhipster && ./gradlew compileJava 2>&1 | tail -5</automated>
  </verify>
  <done>SecMenuPermission entity replaces SecNavigationGrant with renamed fields. SecMenuDefinition entity exists. Liquibase migration renames table/columns and creates new table. Old entity files deleted. Java compiles.</done>
</task>

<task type="auto">
  <name>Task 2: Rename service, DTO, REST resource, and all backend tests</name>
  <files>
    src/main/java/com/vn/core/service/security/CurrentUserMenuPermissionService.java,
    src/main/java/com/vn/core/service/dto/security/MenuPermissionResponseDTO.java,
    src/main/java/com/vn/core/web/rest/MenuPermissionResource.java,
    src/test/java/com/vn/core/service/security/CurrentUserMenuPermissionServiceTest.java,
    src/test/java/com/vn/core/web/rest/MenuPermissionResourceIT.java,
    src/main/java/com/vn/core/service/security/CurrentUserNavigationGrantService.java,
    src/main/java/com/vn/core/service/dto/security/NavigationGrantResponseDTO.java,
    src/main/java/com/vn/core/web/rest/NavigationGrantResource.java,
    src/test/java/com/vn/core/service/security/CurrentUserNavigationGrantServiceTest.java,
    src/test/java/com/vn/core/web/rest/NavigationGrantResourceIT.java
  </files>
  <action>
    **CurrentUserMenuPermissionService.java** — Rename from CurrentUserNavigationGrantService:
    - Class name: `CurrentUserMenuPermissionService`
    - Inject `SecMenuPermissionRepository` (was SecNavigationGrantRepository)
    - Method: `getAllowedMenuIds(String appName)` (was getAllowedNodeIds)
    - Internal logic: use `SecMenuPermission::getMenuId` (was getNodeId), filter on `grant.getEffect()`
    - Variable names: `deniedMenuIds` (was deniedNodeIds), local `menuId` (was nodeId)

    **MenuPermissionResponseDTO.java** — Rename from NavigationGrantResponseDTO:
    - Class name: `MenuPermissionResponseDTO`
    - Field: `allowedMenuIds` (was allowedNodeIds), with getter/setter
    - Keep `appName` field unchanged
    - Update equals/hashCode/toString

    **MenuPermissionResource.java** — Rename from NavigationGrantResource:
    - Class name: `MenuPermissionResource`
    - Inject `CurrentUserMenuPermissionService`
    - @RequestMapping stays `/api/security`
    - @GetMapping changes to `/menu-permissions` (was `/navigation-grants`)
    - Method: `getMenuPermissions(@RequestParam("appName") String appName)`
    - Build `MenuPermissionResponseDTO`, call `setAllowedMenuIds(service.getAllowedMenuIds(appName))`

    **CurrentUserMenuPermissionServiceTest.java** — Rename from CurrentUserNavigationGrantServiceTest:
    - All references: `SecMenuPermission`, `SecMenuPermissionRepository`, `CurrentUserMenuPermissionService`
    - Method calls: `getAllowedMenuIds`
    - Helper method: `permission(String role, String menuId, String effect)` returning `new SecMenuPermission().role(role).appName(APP_NAME).menuId(menuId).effect(effect)`
    - Mock setup: `findAllByAppNameAndRoleIn` (was findAllByAppNameAndAuthorityNameIn)

    **MenuPermissionResourceIT.java** — Rename from NavigationGrantResourceIT:
    - Class name: `MenuPermissionResourceIT`
    - `ENTITY_API_URL = "/api/security/menu-permissions"`
    - Mock: `CurrentUserMenuPermissionService`, method `getAllowedMenuIds`
    - JSON assertions: `body.path("allowedMenuIds")` (was allowedNodeIds)
    - Field name assertions: `containsExactlyInAnyOrder("appName", "allowedMenuIds")`

    **Delete old files** using git rm:
    - `src/main/java/com/vn/core/service/security/CurrentUserNavigationGrantService.java`
    - `src/main/java/com/vn/core/service/dto/security/NavigationGrantResponseDTO.java`
    - `src/main/java/com/vn/core/web/rest/NavigationGrantResource.java`
    - `src/test/java/com/vn/core/service/security/CurrentUserNavigationGrantServiceTest.java`
    - `src/test/java/com/vn/core/web/rest/NavigationGrantResourceIT.java`
  </action>
  <verify>
    <automated>cd D:/jhipster && ./gradlew compileJava compileTestJava test --tests "com.vn.core.service.security.CurrentUserMenuPermissionServiceTest" --tests "com.vn.core.web.rest.MenuPermissionResourceIT" 2>&1 | tail -20</automated>
  </verify>
  <done>Service, DTO, REST resource, and all backend tests renamed. Old files deleted. Unit test and integration test pass with new names and field mappings.</done>
</task>

<task type="auto">
  <name>Task 3: Update frontend NavigationService, spec, constants, and e2e tests</name>
  <files>
    frontend/src/app/layout/navigation/navigation.service.ts,
    frontend/src/app/layout/navigation/navigation.service.spec.ts,
    frontend/src/app/layout/navigation/navigation.constants.ts,
    frontend/e2e/security-comprehensive.spec.ts
  </files>
  <action>
    **navigation.constants.ts**:
    - Change `NAVIGATION_STORAGE_KEY` value to `'jhipster-security-platform:menu-permissions'` (storage key change to avoid stale cache from old format)

    **navigation.service.ts**:
    - Rename interface `NavigationGrantResponse` -> `MenuPermissionResponse`
    - Change interface field: `allowedNodeIds: string[]` -> `allowedMenuIds: string[]`
    - Change `resourceUrl` to use `'api/security/menu-permissions'` (was `'api/security/navigation-grants'`)
    - In `query()`: update all references from `allowedNodeIds` to `allowedMenuIds` in the response handling
    - In `allowedNodeIds()` method: rename to `allowedMenuIds()`, map from `response.allowedMenuIds`
    - In `visibleTree()`: call `this.allowedMenuIds()` instead of `this.allowedNodeIds()`
    - In `isNodeVisible()`: call `this.allowedMenuIds()` and rename local `allowedNodeIds` -> `allowedMenuIds`
    - In `resolveFallbackRoute()`: call `this.allowedMenuIds()` and rename local var
    - In `normalizeResponse()`: update field references from `allowedNodeIds` to `allowedMenuIds`
    - In `isGrantResponse()` (rename to `isMenuPermissionResponse()`): check `candidate.allowedMenuIds`
    - In `readFromStorage()`: use renamed type guard
    - In all private methods: update variable names from nodeId/allowedNodeIds to menuId/allowedMenuIds where they refer to the response payload field

    IMPORTANT: The methods on NavigationService that are called externally (`allowedNodeIds()`, `isNodeVisible()`, `resolveFallbackRoute()`, `visibleTree()`, `getLeaf()`) need their callers updated too. Search for all imports/usages of `allowedNodeIds` from NavigationService across the frontend codebase. The method `allowedNodeIds()` is renamed to `allowedMenuIds()` so update all call sites:
    - Search for `.allowedNodeIds()` in all `.ts` files under `frontend/src/`
    - Update each call site to `.allowedMenuIds()`

    **navigation.service.spec.ts**:
    - Update all `allowedNodeIds` in mock responses to `allowedMenuIds`
    - Update URL assertions from `'api/security/navigation-grants'` to `'api/security/menu-permissions'`
    - Update any assertions that check for `allowedNodeIds` field to check `allowedMenuIds`

    **security-comprehensive.spec.ts** (e2e):
    - In `mockNavigationGrants` function (can optionally rename to `mockMenuPermissions`):
      - Change route pattern from `'**/api/security/navigation-grants?*'` to `'**/api/security/menu-permissions?*'`
      - Change response body field from `allowedNodeIds` to `allowedMenuIds`
    - Update all call sites if function is renamed
  </action>
  <verify>
    <automated>cd D:/jhipster/frontend && npx ng test --watch=false 2>&1 | tail -20</automated>
  </verify>
  <done>Frontend NavigationService uses /api/security/menu-permissions endpoint and allowedMenuIds field name. All frontend unit tests and type checks pass. E2e mocks updated to new endpoint and field names.</done>
</task>

</tasks>

<verification>
1. `cd D:/jhipster && ./gradlew test --tests "com.vn.core.service.security.CurrentUserMenuPermissionServiceTest" --tests "com.vn.core.web.rest.MenuPermissionResourceIT"` — both backend tests pass
2. `cd D:/jhipster/frontend && npx ng test --watch=false` — all frontend unit tests pass
3. `cd D:/jhipster && ./gradlew compileJava` — no compilation errors, old classes gone
4. Verify no remaining references to old names: `grep -r "SecNavigationGrant\|NavigationGrantResponse\|NavigationGrantResource\|CurrentUserNavigationGrantService\|allowedNodeIds\|navigation-grants" src/ frontend/src/ --include="*.java" --include="*.ts"` returns nothing
</verification>

<success_criteria>
- SecNavigationGrant.java deleted, SecMenuPermission.java exists with role/menuId fields
- SecMenuDefinition.java exists with all specified fields
- Liquibase migration renames table + columns and creates sec_menu_definition table
- REST endpoint is /api/security/menu-permissions returning { appName, allowedMenuIds }
- Frontend NavigationService hits new endpoint and uses allowedMenuIds
- All backend unit + integration tests pass
- All frontend unit tests pass
- No references to old names remain in src/ or frontend/src/
</success_criteria>

<output>
After completion, create `.planning/quick/260325-lae-rename-secnavigationgrant-to-secmenuperm/260325-lae-SUMMARY.md`
</output>
