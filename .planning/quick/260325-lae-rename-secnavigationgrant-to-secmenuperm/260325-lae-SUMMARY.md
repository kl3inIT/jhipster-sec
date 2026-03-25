---
phase: quick
plan: 260325-lae
subsystem: security-domain
tags: [rename, backend, frontend, liquibase, entity, navigation, menu-permissions]
dependency_graph:
  requires: []
  provides:
    - SecMenuPermission entity with role/menuId fields
    - SecMenuDefinition entity for menu metadata
    - GET /api/security/menu-permissions endpoint
    - Frontend NavigationService on new endpoint and field names
  affects:
    - frontend/src/app/layout/navigation/navigation.service.ts
    - src/main/java/com/vn/core/security/domain/
    - src/main/java/com/vn/core/web/rest/
tech_stack:
  added: []
  patterns:
    - Liquibase renameTable/renameColumn migration for in-place schema rename
    - JPA entity fluent setter pattern (entity.field(value).anotherField(value))
key_files:
  created:
    - src/main/java/com/vn/core/security/domain/SecMenuPermission.java
    - src/main/java/com/vn/core/security/repository/SecMenuPermissionRepository.java
    - src/main/java/com/vn/core/security/domain/SecMenuDefinition.java
    - src/main/java/com/vn/core/security/repository/SecMenuDefinitionRepository.java
    - src/main/java/com/vn/core/service/security/CurrentUserMenuPermissionService.java
    - src/main/java/com/vn/core/service/dto/security/MenuPermissionResponseDTO.java
    - src/main/java/com/vn/core/web/rest/MenuPermissionResource.java
    - src/main/resources/config/liquibase/changelog/20260325000200_rename_nav_grant_add_menu_def.xml
    - src/test/java/com/vn/core/service/security/CurrentUserMenuPermissionServiceTest.java
    - src/test/java/com/vn/core/web/rest/MenuPermissionResourceIT.java
  modified:
    - src/main/resources/config/liquibase/master.xml
    - frontend/src/app/layout/navigation/navigation.service.ts
    - frontend/src/app/layout/navigation/navigation.service.spec.ts
    - frontend/src/app/layout/navigation/navigation.constants.ts
    - frontend/e2e/security-comprehensive.spec.ts
  deleted:
    - src/main/java/com/vn/core/security/domain/SecNavigationGrant.java
    - src/main/java/com/vn/core/security/repository/SecNavigationGrantRepository.java
    - src/main/java/com/vn/core/service/security/CurrentUserNavigationGrantService.java
    - src/main/java/com/vn/core/service/dto/security/NavigationGrantResponseDTO.java
    - src/main/java/com/vn/core/web/rest/NavigationGrantResource.java
    - src/test/java/com/vn/core/service/security/CurrentUserNavigationGrantServiceTest.java
    - src/test/java/com/vn/core/web/rest/NavigationGrantResourceIT.java
decisions:
  - Liquibase uses renameTable/renameColumn changesets (not drop+create) to preserve existing data
  - Storage key updated from 'navigation-grants' to 'menu-permissions' to force cache invalidation on stale sessionStorage
metrics:
  duration: 18 min
  completed: 2026-03-25
  tasks_completed: 3
  files_changed: 18
---

# Quick Task 260325-lae: Rename SecNavigationGrant to SecMenuPermission

**One-liner:** Full rename of SecNavigationGrant to SecMenuPermission (authorityName->role, nodeId->menuId) with Liquibase migration, new SecMenuDefinition entity, and frontend endpoint/field name alignment.

## What Was Done

Renamed the entire navigation grant stack to menu permission stack across backend and frontend to align with the domain model.

### Task 1: Liquibase migration + renamed Java entity/repository + new SecMenuDefinition entity (commit 8fee03f)

- Created `20260325000200_rename_nav_grant_add_menu_def.xml` with 10 changesets: renameTable, renameColumn x2, drop/add unique constraint, drop/add indexes x2, drop/add FK, create sec_menu_definition table, unique constraint, index
- Created `SecMenuPermission.java` with `role` (was `authorityName`) and `menuId` (was `nodeId`) fields
- Created `SecMenuPermissionRepository` with `findAllByAppNameAndRoleIn()` (was `findAllByAppNameAndAuthorityNameIn`)
- Created `SecMenuDefinition.java` with menuId, appName, label, description, parentMenuId, route, icon, ordering fields
- Created `SecMenuDefinitionRepository` with `findAllByAppName()` and `findByAppNameAndMenuId()`
- Deleted `SecNavigationGrant.java` and `SecNavigationGrantRepository.java` via git rm

### Task 2: Service, DTO, REST resource, and tests renamed (commit 7c41e65)

- Created `CurrentUserMenuPermissionService` with `getAllowedMenuIds(String appName)` using `SecMenuPermissionRepository.findAllByAppNameAndRoleIn`
- Created `MenuPermissionResponseDTO` with `allowedMenuIds` field (was `allowedNodeIds`)
- Created `MenuPermissionResource` at `GET /api/security/menu-permissions` (was `/navigation-grants`)
- Created `CurrentUserMenuPermissionServiceTest` and `MenuPermissionResourceIT` with new names and assertions
- Deleted old service, DTO, resource, and test files via git rm

### Task 3: Frontend NavigationService, spec, constants, and e2e (commit d877771)

- Renamed interface `NavigationGrantResponse` to `MenuPermissionResponse` with `allowedMenuIds` field
- Changed `resourceUrl` to `api/security/menu-permissions`
- Renamed `allowedNodeIds()` method to `allowedMenuIds()` and updated all internal callers
- Renamed `isGrantResponse()` type guard to `isMenuPermissionResponse()` checking `allowedMenuIds`
- Updated `NAVIGATION_STORAGE_KEY` to `'jhipster-security-platform:menu-permissions'`
- Updated `navigation.service.spec.ts`: all URL/field assertions use new names
- Updated `security-comprehensive.spec.ts`: renamed `mockNavigationGrants` to `mockMenuPermissions`, updated route pattern and response field

## Verification Results

- `./gradlew compileJava compileTestJava` — BUILD SUCCESSFUL
- `./gradlew test --tests "CurrentUserMenuPermissionServiceTest"` — PASSED
- `./gradlew integrationTest --tests "MenuPermissionResourceIT"` — PASSED
- `npx ng test --watch=false` — 84 passed, 4 pre-existing failures in http-error.utils.spec.ts (unrelated path issue)
- No remaining references to old names in `src/` or `frontend/src/`

## Deviations from Plan

### Pre-existing failure logged (out of scope)

`frontend/src/app/shared/error/http-error.utils.spec.ts` fails with path resolution error (`frontend/frontend/public/i18n/en.json`) — pre-existing issue unrelated to this rename task. Logged for tracking; not fixed.

## Known Stubs

None — all renamed classes are fully wired with correct data sources.

## Self-Check: PASSED

- SecMenuPermission.java: FOUND
- SecMenuDefinition.java: FOUND
- SecMenuPermissionRepository.java: FOUND
- SecMenuDefinitionRepository.java: FOUND
- CurrentUserMenuPermissionService.java: FOUND
- MenuPermissionResponseDTO.java: FOUND
- MenuPermissionResource.java: FOUND
- 20260325000200_rename_nav_grant_add_menu_def.xml: FOUND
- Commits 8fee03f, 7c41e65, d877771: FOUND in git log
- Old files SecNavigationGrant.java, NavigationGrantResource.java, etc.: DELETED
