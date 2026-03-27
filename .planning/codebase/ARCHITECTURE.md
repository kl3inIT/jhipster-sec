# Architecture

**Analysis Date:** 2026-03-27

## Pattern Overview

**Overall:** Brownfield split-runtime architecture with one active Spring Boot backend in `src/main/java/com/vn/core/**` and one active standalone Angular shell in `frontend/src/**`, plus in-repo legacy/reference applications used as migration guides.

**Key Characteristics:**
- Keep the deployable backend in a single Gradle root module defined by `settings.gradle`, with runtime entry points in `src/main/java/com/vn/core/JhipsterSecApp.java` and `src/main/java/com/vn/core/ApplicationWebXml.java`.
- Route secured business entity reads and writes through the `src/main/java/com/vn/core/security/**` subsystem instead of direct controller-to-repository CRUD.
- Keep frontend delivery separate from the backend build: `frontend/angular.json` and `frontend/package.json` define the standalone SPA, while root `package.json` still drives backend and Docker workflows.
- Preserve classic JHipster account, authentication, user, authority, and mail flows under `src/main/java/com/vn/core/web/rest/AccountResource.java`, `src/main/java/com/vn/core/web/rest/AuthenticateController.java`, `src/main/java/com/vn/core/web/rest/UserResource.java`, and `src/main/java/com/vn/core/service/UserService.java`.
- Treat `angapp/`, `aef-main/aef-main/`, and `jhipter-angular/` as adjacent reference or migration source trees; they are not wired into the active root runtime.
- Within those reference trees, `aef-main/aef-main/` is the canonical frontend reference, and its PrimeNG Sakai shell/layout/component patterns define the baseline structure for `frontend/`.

## Layers

**Bootstrap and Configuration:**
- Purpose: Start the backend application, bind configuration properties, and wire HTTP, JWT, caching, Jackson, Liquibase, and web concerns.
- Location: `src/main/java/com/vn/core/`, `src/main/java/com/vn/core/config/`, `src/main/resources/config/`
- Contains: `JhipsterSecApp`, `ApplicationWebXml`, `ApplicationProperties`, `SecurityConfiguration`, `SecurityJwtConfiguration`, `LiquibaseConfiguration`, `WebConfigurer`, `CacheConfiguration`, `LoggingConfiguration`.
- Depends on: Spring Boot, JHipster properties, Micrometer, Liquibase, JWT, Hazelcast, servlet APIs.
- Used by: Every backend runtime layer through Spring bean wiring.

**HTTP API Layer:**
- Purpose: Expose account/auth/admin endpoints, secured entity CRUD/query endpoints, and security-metadata management APIs.
- Location: `src/main/java/com/vn/core/web/rest/`, `src/main/java/com/vn/core/web/rest/admin/security/`, `src/main/java/com/vn/core/web/rest/vm/`, `src/main/java/com/vn/core/web/rest/errors/`
- Contains: `AccountResource`, `AuthenticateController`, `UserResource`, `AuthorityResource`, `OrganizationResource`, `DepartmentResource`, `EmployeeResource`, `MenuPermissionResource`, `SecuredEntityCapabilityResource`, admin resources such as `SecPermissionAdminResource` and `SecRoleAdminResource`.
- Depends on: `src/main/java/com/vn/core/service/**`, `src/main/java/com/vn/core/service/security/**`, `src/main/java/com/vn/core/repository/**` for a few legacy flows, validation, and Spring Security method authorization.
- Used by: The standalone Angular app in `frontend/src/**`, admin clients, and JHipster-style account consumers.

**Application Service Layer:**
- Purpose: Hold transactional workflows and API-facing orchestration for users, mail, secured entities, capability aggregation, and menu-permission resolution.
- Location: `src/main/java/com/vn/core/service/`, `src/main/java/com/vn/core/service/security/`, `src/main/java/com/vn/core/service/dto/`, `src/main/java/com/vn/core/service/dto/security/`, `src/main/java/com/vn/core/service/mapper/`, `src/main/java/com/vn/core/service/mapper/security/`
- Contains: `UserService`, `MailService`, `OrganizationService`, `DepartmentService`, `EmployeeService`, `CurrentUserMenuPermissionService`, `SecPermissionUiContractService`, `SecuredEntityCapabilityService`, DTOs, and mappers.
- Depends on: `src/main/java/com/vn/core/repository/**`, `src/main/java/com/vn/core/domain/**`, `src/main/java/com/vn/core/security/**`, and Spring transactions.
- Used by: `src/main/java/com/vn/core/web/rest/**` and security/config beans.

**Security Enforcement Core:**
- Purpose: Enforce entity CRUD permissions, attribute permissions, row-level policies, fetch-plan-driven reads, secure merge behavior, and authority-aware menu visibility.
- Location: `src/main/java/com/vn/core/security/`
- Contains: access constraints in `src/main/java/com/vn/core/security/access/`, security-context bridges in `src/main/java/com/vn/core/security/bridge/`, entity catalog classes in `src/main/java/com/vn/core/security/catalog/`, data managers in `src/main/java/com/vn/core/security/data/`, fetch-plan types in `src/main/java/com/vn/core/security/fetch/`, merge logic in `src/main/java/com/vn/core/security/merge/`, permission evaluators in `src/main/java/com/vn/core/security/permission/`, metadata repositories in `src/main/java/com/vn/core/security/repository/`, row policy providers in `src/main/java/com/vn/core/security/row/`, and serializers in `src/main/java/com/vn/core/security/serialize/`.
- Depends on: JPA metamodel access, Spring Security context, business repositories, and security metadata tables.
- Used by: `OrganizationService`, `DepartmentService`, `EmployeeService`, `SecuredEntityCapabilityService`, menu-permission services, and any future secured entity feature.

**Persistence Layer:**
- Purpose: Persist both base business entities and security metadata, backed by Liquibase-managed PostgreSQL schema.
- Location: `src/main/java/com/vn/core/domain/`, `src/main/java/com/vn/core/repository/`, `src/main/java/com/vn/core/security/domain/`, `src/main/java/com/vn/core/security/repository/`, `src/main/resources/config/liquibase/`
- Contains: business entities such as `User`, `Authority`, `Organization`, `Department`, `Employee`; business repositories such as `UserRepository`, `OrganizationRepository`, `DepartmentRepository`, `EmployeeRepository`; security metadata entities such as `SecPermission`, `SecRowPolicy`, `SecMenuDefinition`, `SecMenuPermission`; Liquibase changelogs in `src/main/resources/config/liquibase/changelog/*.xml`.
- Depends on: Spring Data JPA, Hibernate, Liquibase.
- Used by: Services and the security/data subsystem.

**Management and Observability:**
- Purpose: Capture operational signals that are orthogonal to business flows.
- Location: `src/main/java/com/vn/core/management/`, `src/main/java/com/vn/core/aop/logging/`
- Contains: `SecurityMetersService` for invalid-token counters and `LoggingAspect` for dev-profile entry/exit/error logging.
- Depends on: Micrometer, AspectJ, Spring profiles.
- Used by: JWT decoding, repositories, services, and REST controllers.

**Standalone Frontend Shell:**
- Purpose: Provide the current UI shell, route orchestration, auth state, navigation filtering, admin screens, and secured-entity screens.
- Location: `frontend/src/app/`, `frontend/src/i18n/`, `frontend/public/i18n/`
- Contains: root bootstrap in `frontend/src/main.ts`, root component in `frontend/src/app/app.ts`, router config in `frontend/src/app.routes.ts`, app providers in `frontend/src/app.config.ts`, auth/interceptor services in `frontend/src/app/core/**`, Sakai-style shell layout in `frontend/src/app/layout/**`, feature pages in `frontend/src/app/pages/**`, and reusable UI in `frontend/src/app/shared/**`.
- Depends on: Angular Router, HttpClient, `@ngx-translate/core`, PrimeNG, and backend APIs under `/api/**`.
- Used by: Browser clients running the standalone app defined by `frontend/angular.json`.
- Reference baseline: `aef-main/aef-main/src/app/layout/**`, `aef-main/aef-main/src/app/pages/**`, and `aef-main/aef-main/src/app.config.ts` provide the canonical frontend shell and component layout reference for this layer.

**Reference and Legacy Workspaces:**
- Purpose: Preserve migration source behavior and UI references without coupling them into the active runtime.
- Location: `angapp/`, `aef-main/aef-main/`, `jhipter-angular/`
- Contains: a full legacy JHipster app in `angapp/src/main/java/com/mycompany/myapp/**` and `angapp/src/main/webapp/app/**`, the canonical frontend reference workspace in `aef-main/aef-main/`, and a separate sample split backend/frontend app in `jhipter-angular/backend/` and `jhipter-angular/frontend/`.
- Depends on: Their own manifests and build systems.
- Used by: Manual comparison and migration work; not by the active root backend or `frontend/` shell.
- Frontend guidance split: `aef-main/aef-main/` is the primary shell/layout/component reference, while `angapp/` remains the primary behavior donor for JHipster-specific support files and migrated admin flows.

## Data Flow

**Secured Entity CRUD and Query Flow:**

1. The standalone frontend calls entity APIs from feature services such as `frontend/src/app/pages/entities/organization/service/organization.service.ts`.
2. Backend resources such as `src/main/java/com/vn/core/web/rest/OrganizationResource.java`, `src/main/java/com/vn/core/web/rest/DepartmentResource.java`, and `src/main/java/com/vn/core/web/rest/EmployeeResource.java` accept pageable parameters or raw JSON request bodies and delegate to their application services.
3. Entity services such as `src/main/java/com/vn/core/service/OrganizationService.java` convert JSON into attribute maps and call `src/main/java/com/vn/core/security/data/SecureDataManager.java`.
4. `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java` resolves the secured entity through `src/main/java/com/vn/core/security/catalog/MetamodelSecuredEntityCatalog.java`, checks CRUD access through `src/main/java/com/vn/core/security/data/DataManagerImpl.java`, composes row-policy specifications through `src/main/java/com/vn/core/security/row/RowLevelSpecificationBuilder.java`, resolves fetch plans through `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java`, enforces write-side attribute permissions through `src/main/java/com/vn/core/security/merge/SecureMergeServiceImpl.java`, and strips denied read-side attributes through `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java`.
5. Low-level repository resolution is generic: `src/main/java/com/vn/core/security/repository/RepositoriesRegistryImpl.java` discovers the matching Spring Data repository for each secured entity class.
6. The resource returns JSON payloads shaped by fetch plans from `src/main/resources/fetch-plans.yml`, so the frontend sees only the allowed attributes for the active user.

**Menu Visibility and Route Authorization Flow:**

1. `frontend/src/app/layout/navigation/navigation.service.ts` calls `/api/security/menu-permissions` with the app name from `frontend/src/app/layout/navigation/navigation.constants.ts`.
2. `src/main/java/com/vn/core/web/rest/MenuPermissionResource.java` delegates to `src/main/java/com/vn/core/service/security/CurrentUserMenuPermissionService.java`.
3. `CurrentUserMenuPermissionService` resolves current authorities through `src/main/java/com/vn/core/security/MergedSecurityServiceImpl.java` and loads `SecMenuPermission` rows from `src/main/java/com/vn/core/security/repository/SecMenuPermissionRepository.java`.
4. The frontend filters `frontend/src/app/layout/navigation/navigation-registry.ts` to a visible menu tree and caches the result in session storage.
5. `frontend/src/app/core/auth/user-route-access.service.ts` combines account identity, menu visibility, and optional route authorities to allow navigation or redirect to `/accessdenied`.

**Security Metadata Administration Flow:**

1. Admin pages under `frontend/src/app/pages/admin/security/**` and `frontend/src/app/pages/admin/user-management/**` call `/api/admin/sec/**` and legacy user APIs.
2. Admin controllers in `src/main/java/com/vn/core/web/rest/admin/security/**` validate admin access with `@PreAuthorize("hasAuthority(...)")`.
3. DTOs in `src/main/java/com/vn/core/service/dto/security/` and mappers in `src/main/java/com/vn/core/service/mapper/security/` shape the request and response contracts.
4. `src/main/java/com/vn/core/service/security/SecPermissionUiContractService.java` translates UI-friendly entity and attribute targets to stored permission targets and back again.
5. Security metadata repositories under `src/main/java/com/vn/core/security/repository/` persist the role, permission, row-policy, menu-definition, and menu-permission tables added by `src/main/resources/config/liquibase/changelog/20260321000200_create_sec_permission.xml`, `src/main/resources/config/liquibase/changelog/20260321000300_create_sec_row_policy.xml`, `src/main/resources/config/liquibase/changelog/20260325000100_create_sec_navigation_grant.xml`, `src/main/resources/config/liquibase/changelog/20260325000200_rename_nav_grant_add_menu_def.xml`, and `src/main/resources/config/liquibase/changelog/20260325000300_repair_sec_menu_permission_schema.xml`.

**Authentication and Session State:**
- Backend HTTP state is stateless JWT, configured by `src/main/java/com/vn/core/config/SecurityConfiguration.java` and `src/main/java/com/vn/core/config/SecurityJwtConfiguration.java`.
- Frontend identity state is cached in `frontend/src/app/core/auth/account.service.ts` using Angular signals plus a replayed observable.
- Frontend route and capability caches use session storage in `frontend/src/app/layout/navigation/navigation.service.ts` and `frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts`.
- Translation assets are merged from `frontend/src/i18n/**` into `frontend/public/i18n/*.json` by `frontend/scripts/merge-i18n.cjs`, then loaded through `frontend/src/app/config/translation.config.ts`.

## Key Abstractions

**Secured Entity Catalog:**
- Purpose: Define which JPA entities participate in secured CRUD and which fetch-plan codes belong to them.
- Examples: `src/main/java/com/vn/core/security/catalog/SecuredEntity.java`, `src/main/java/com/vn/core/security/catalog/MetamodelSecuredEntityCatalog.java`, `src/main/java/com/vn/core/domain/Organization.java`, `src/main/java/com/vn/core/domain/Department.java`, `src/main/java/com/vn/core/domain/Employee.java`
- Pattern: Annotate business entities with `@SecuredEntity`; the live JPA metamodel is scanned once at startup to build the runtime catalog.

**Secure Data Manager Pipeline:**
- Purpose: Centralize secure reads, writes, and deletes for protected entities.
- Examples: `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java`, `src/main/java/com/vn/core/security/data/DataManagerImpl.java`, `src/main/java/com/vn/core/security/data/UnconstrainedDataManagerImpl.java`, `src/main/java/com/vn/core/security/data/SecuredLoadQuery.java`
- Pattern: Application services call a single orchestration layer that combines CRUD checks, row policies, fetch-plan resolution, merge, repository access, and serialization.

**Fetch Plans:**
- Purpose: Declare allowed read shapes outside the database so read responses can be controlled consistently.
- Examples: `src/main/resources/fetch-plans.yml`, `src/main/java/com/vn/core/security/fetch/FetchPlan.java`, `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java`, `src/main/java/com/vn/core/security/fetch/FetchPlanResolverImpl.java`
- Pattern: Keep plans in YAML keyed by entity class and plan name; services reference them by logical codes such as `organization-list` and `organization-detail`.

**Security Metadata Domain:**
- Purpose: Persist user-manageable security rules that drive runtime enforcement and frontend admin tooling.
- Examples: `src/main/java/com/vn/core/security/domain/SecPermission.java`, `src/main/java/com/vn/core/security/domain/SecRowPolicy.java`, `src/main/java/com/vn/core/security/domain/SecMenuDefinition.java`, `src/main/java/com/vn/core/security/domain/SecMenuPermission.java`, `src/main/java/com/vn/core/security/repository/SecPermissionRepository.java`
- Pattern: Keep security metadata separate from business entities and access it through dedicated admin endpoints and service helpers.

**UI Capability and Navigation Contracts:**
- Purpose: Expose current-user permissions to the standalone frontend without embedding backend internals in the client.
- Examples: `src/main/java/com/vn/core/service/dto/security/SecuredEntityCapabilityDTO.java`, `src/main/java/com/vn/core/service/dto/security/MenuPermissionResponseDTO.java`, `src/main/java/com/vn/core/web/rest/SecuredEntityCapabilityResource.java`, `frontend/src/app/pages/entities/shared/secured-entity-capability.model.ts`, `frontend/src/app/layout/navigation/navigation.model.ts`
- Pattern: Backend returns simplified capability/menu DTOs; the frontend uses them to gate routes, hide menu items, and adapt feature pages.

**Legacy Boundary DTOs and VMs:**
- Purpose: Preserve stable request and response contracts for classic JHipster endpoints.
- Examples: `src/main/java/com/vn/core/service/dto/AdminUserDTO.java`, `src/main/java/com/vn/core/service/dto/UserDTO.java`, `src/main/java/com/vn/core/web/rest/vm/LoginVM.java`, `src/main/java/com/vn/core/web/rest/vm/ManagedUserVM.java`, `src/main/java/com/vn/core/web/rest/vm/SecuredEntityQueryVM.java`
- Pattern: Use DTOs and VMs at the REST boundary for account/admin flows; use raw JSON plus secure map serialization for the newer secured-entity CRUD endpoints.

## Entry Points

**Backend Application Startup:**
- Location: `src/main/java/com/vn/core/JhipsterSecApp.java`
- Triggers: `./gradlew`, packaged JAR execution, IDE runs, and root npm scripts such as `npm run backend:start`.
- Responsibilities: Start Spring Boot, set the default profile, disable devtools restart for Hazelcast compatibility, and log the application URLs.

**Servlet Deployment Entry:**
- Location: `src/main/java/com/vn/core/ApplicationWebXml.java`
- Triggers: WAR deployment to an external servlet container.
- Responsibilities: Reuse the same Spring Boot application source outside the embedded server path.

**Backend HTTP Surface:**
- Location: `src/main/java/com/vn/core/web/rest/` and `src/main/java/com/vn/core/web/rest/admin/security/`
- Triggers: HTTP requests under `/api/**`.
- Responsibilities: Account/auth flows, user and authority admin, secured business entity CRUD/query, capability lookup, menu-permission lookup, and security metadata administration.

**Schema Bootstrap:**
- Location: `src/main/java/com/vn/core/config/LiquibaseConfiguration.java` and `src/main/resources/config/liquibase/master.xml`
- Triggers: Application startup unless Liquibase is disabled by profile.
- Responsibilities: Apply base schema and the incremental security/proof-entity changelogs.

**Standalone Frontend Bootstrap:**
- Location: `frontend/src/main.ts`, `frontend/src/app/app.ts`, `frontend/src/app.config.ts`
- Triggers: `npm start`, `npm build`, and `npm test` from `frontend/package.json`.
- Responsibilities: Bootstrap the Angular app, wire providers, initialize translations, set the backend endpoint prefix, load the current account, and render the root router outlet.

**Frontend Routing Entry:**
- Location: `frontend/src/app.routes.ts`, `frontend/src/app/pages/admin/admin.routes.ts`, `frontend/src/app/pages/entities/entity.routes.ts`
- Triggers: Browser navigation.
- Responsibilities: Lazy-load feature areas, bind route metadata, enforce route access, and surface feature-specific titles and breadcrumbs.

## Error Handling

**Strategy:** Fail closed for write-side permission and policy violations, omit denied read-side attributes during secure serialization, and centralize transport-level translation at the backend and router/interceptor layer at the frontend.

**Patterns:**
- `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java` converts backend exceptions and validation failures into structured problem responses.
- `src/main/java/com/vn/core/config/SecurityConfiguration.java` handles authentication and authorization failures with `BearerTokenAuthenticationEntryPoint` and `BearerTokenAccessDeniedHandler`.
- `src/main/java/com/vn/core/security/merge/SecureMergeServiceImpl.java` throws `AccessDeniedException` when callers attempt to edit attributes without `EDIT` permission.
- `src/main/java/com/vn/core/security/row/RowLevelPolicyProviderDbImpl.java` rejects unsupported or malformed row policies instead of silently bypassing them.
- `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java` drops denied attributes from read responses while preserving `id` when requested by the fetch plan.
- `frontend/src/app.config.ts` maps navigation-time HTTP failures to `/login`, `/accessdenied`, `/404`, or `/error`.
- `frontend/src/app/core/interceptor/index.ts` registers auth, auth-expired, error-handler, and notification interceptors for runtime HTTP behavior.

## Cross-Cutting Concerns

**Logging:** Use class-local SLF4J loggers throughout the backend and the dev-only logging aspect in `src/main/java/com/vn/core/aop/logging/LoggingAspect.java`. Frontend runtime logging is minimal and flows mostly through interceptors and guarded `console.error`.

**Validation:** Apply Bean Validation and `@Valid` at REST boundaries such as `src/main/java/com/vn/core/web/rest/UserResource.java`, `src/main/java/com/vn/core/web/rest/AccountResource.java`, and admin security resources. Frontend forms live in feature folders such as `frontend/src/app/pages/admin/user-management/update/` and entity `update/` folders.

**Authentication:** Keep backend auth in `src/main/java/com/vn/core/config/SecurityConfiguration.java`, `src/main/java/com/vn/core/config/SecurityJwtConfiguration.java`, `src/main/java/com/vn/core/security/DomainUserDetailsService.java`, and `src/main/java/com/vn/core/web/rest/AuthenticateController.java`. Frontend auth state and token handling live in `frontend/src/app/core/auth/**` and `frontend/src/app/core/interceptor/auth.interceptor.ts`.

**Authorization:** Backend authorization spans method security, entity permission evaluation, row policies, attribute evaluation, and menu permissions across `src/main/java/com/vn/core/security/**`. Frontend authorization spans `frontend/src/app/core/auth/user-route-access.service.ts`, `frontend/src/app/layout/navigation/navigation.service.ts`, and capability resolvers in entity route files.

**Persistence and Migrations:** Schema ownership lives in `src/main/resources/config/liquibase/master.xml` and `src/main/resources/config/liquibase/changelog/*.xml`. Fetch-plan definitions live outside the database in `src/main/resources/fetch-plans.yml`.

**Configuration:** Application property binding is centralized in `src/main/java/com/vn/core/config/ApplicationProperties.java`. Sensitive runtime configuration files live under `src/main/resources/config/application-*.yml` and `src/main/resources/config/tls/`; document their locations only and do not copy secret values into planning artifacts.

**Frontend Shell and Navigation:** Shell layout is isolated in `frontend/src/app/layout/**`, with static navigation metadata in `frontend/src/app/layout/navigation/navigation-registry.ts` and PrimeNG layout state in `frontend/src/app/layout/service/layout.service.ts`.

**Localization:** Backend message bundles live in `src/main/resources/i18n/`. Frontend translation sources live in `frontend/src/i18n/**`, merged assets live in `frontend/public/i18n/*.json`, and cache-busting is driven by `frontend/src/app/config/i18n-hash.generated.ts`.

**Monitoring:** Invalid JWT counters live in `src/main/java/com/vn/core/management/SecurityMetersService.java`, and operational compose stacks live in `src/main/docker/*.yml`.

---

*Architecture analysis: 2026-03-27*
