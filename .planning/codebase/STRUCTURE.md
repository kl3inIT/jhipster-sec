# Codebase Structure

**Analysis Date:** 2026-03-27

## Directory Layout

```text
[project-root]/
├── src/                           # Active Spring Boot backend source, resources, and tests
├── frontend/                      # Active standalone Angular application
├── angapp/                        # Legacy integrated JHipster app kept for migration/reference
├── aef-main/                      # UI reference workspace snapshot
├── jhipter-angular/               # Separate sample split backend/frontend workspace
├── buildSrc/                      # Shared Gradle convention plugins and build logic
├── gradle/                        # Root Gradle helper scripts and version catalogs
└── .planning/                     # GSD planning artifacts, research, and generated codebase docs
```

## Directory Purposes

**`src/main/java/com/vn/core/`:**
- Purpose: Hold all active backend runtime code for the deployable Spring Boot service.
- Contains: Bootstrapping classes, config, management, repositories, domain entities, security subsystem, services, REST resources, and AOP helpers.
- Key files: `src/main/java/com/vn/core/JhipsterSecApp.java`, `src/main/java/com/vn/core/ApplicationWebXml.java`, `src/main/java/com/vn/core/config/SecurityConfiguration.java`, `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java`

**`src/main/resources/`:**
- Purpose: Hold backend configuration, database migrations, fetch plans, mail templates, and message bundles.
- Contains: `config/application*.yml`, `config/liquibase/**`, `fetch-plans.yml`, `templates/mail/*.html`, `i18n/*.properties`, `logback-spring.xml`.
- Key files: `src/main/resources/config/liquibase/master.xml`, `src/main/resources/fetch-plans.yml`, `src/main/resources/config/application.yml`

**`src/test/java/com/vn/core/`:**
- Purpose: Hold backend integration, unit, and architecture tests for the active backend.
- Contains: REST integration tests, security enforcement tests, mapper tests, and `TechnicalStructureTest`.
- Key files: `src/test/java/com/vn/core/TechnicalStructureTest.java`, `src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java`, `src/test/java/com/vn/core/web/rest/MenuPermissionResourceIT.java`

**`frontend/src/`:**
- Purpose: Hold the active standalone Angular app.
- Contains: bootstrap files, route configuration, feature pages, shell layout, auth/core services, shared UI, app config, and translation source files.
- Key files: `frontend/src/main.ts`, `frontend/src/app/app.ts`, `frontend/src/app.routes.ts`, `frontend/src/app.config.ts`, `frontend/src/app/layout/navigation/navigation-registry.ts`

**`frontend/public/`:**
- Purpose: Hold static assets served by the standalone Angular app.
- Contains: merged translation payloads, favicon, and any browser-served assets.
- Key files: `frontend/public/i18n/en.json`, `frontend/public/i18n/vi.json`, `frontend/public/favicon.ico`

**`angapp/`:**
- Purpose: Preserve the older integrated JHipster backend plus Angular webapp that the current migration is absorbing.
- Contains: a separate Spring Boot app in `angapp/src/main/java/com/mycompany/myapp/**`, a generated Angular app in `angapp/src/main/webapp/app/**`, its own Gradle build, and its own `package.json`.
- Key files: `angapp/build.gradle`, `angapp/src/main/java/com/mycompany/myapp/AngappApp.java`, `angapp/src/main/webapp/app/app.routes.ts`

**`aef-main/aef-main/`:**
- Purpose: Provide UI/design reference material for the standalone frontend shell.
- Contains: a separate Angular workspace with its own `angular.json`, `package.json`, `src/`, and `public/`.
- Key files: `aef-main/aef-main/angular.json`, `aef-main/aef-main/src/`

**`jhipter-angular/`:**
- Purpose: Hold an additional sample/reference split application with separate backend and frontend folders.
- Contains: `jhipter-angular/backend/` and `jhipter-angular/frontend/`, each with their own manifests and source trees.
- Key files: `jhipter-angular/backend/pom.xml`, `jhipter-angular/frontend/angular.json`

**`buildSrc/`:**
- Purpose: Centralize Gradle convention plugins and shared build configuration for the active backend.
- Contains: version catalogs and Groovy convention scripts.
- Key files: `buildSrc/build.gradle`, `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`, `buildSrc/src/main/groovy/jhipster.code-quality-conventions.gradle`

**`gradle/`:**
- Purpose: Hold root Gradle helper scripts consumed by `build.gradle`.
- Contains: profile, Spring Boot, and Liquibase scripts plus the wrapper metadata.
- Key files: `gradle/spring-boot.gradle`, `gradle/liquibase.gradle`, `gradle/wrapper/gradle-wrapper.properties`

**`.planning/`:**
- Purpose: Hold project planning state, phase artifacts, research, and generated codebase docs for GSD workflows.
- Contains: `PROJECT.md`, `STATE.md`, roadmap data, phase folders, quick-task records, and `.planning/codebase/*.md`.
- Key files: `.planning/PROJECT.md`, `.planning/STATE.md`, `.planning/codebase/`

## Key File Locations

**Entry Points:**
- `src/main/java/com/vn/core/JhipsterSecApp.java`: Active backend application entry.
- `src/main/java/com/vn/core/ApplicationWebXml.java`: WAR/servlet deployment entry.
- `src/main/java/com/vn/core/web/rest/`: Active backend API surface.
- `frontend/src/main.ts`: Active standalone frontend bootstrap.
- `frontend/src/app/app.ts`: Frontend root component that initializes identity and language state.
- `frontend/src/app.routes.ts`: Top-level lazy route graph for the standalone frontend.
- `angapp/src/main/java/com/mycompany/myapp/AngappApp.java`: Legacy reference app entry, not the active root backend.

**Configuration:**
- `build.gradle`: Active backend build, plugins, dependencies, and Java version.
- `settings.gradle`: Root module definition for the active backend.
- `src/main/resources/config/application.yml`: Shared backend runtime config.
- `src/main/resources/config/application-dev.yml`: Backend development overrides.
- `src/main/resources/config/application-prod.yml`: Backend production overrides.
- `src/main/resources/fetch-plans.yml`: Source of truth for secure fetch-plan definitions.
- `src/main/resources/config/liquibase/master.xml`: Schema migration include list.
- `frontend/angular.json`: Frontend build and dev-server configuration.
- `frontend/proxy.conf.json`: Frontend proxy rules for local backend calls.
- `frontend/src/environments/environment.ts`: Frontend production environment constants.
- `frontend/scripts/merge-i18n.cjs`: Frontend translation merge script.

**Core Logic:**
- `src/main/java/com/vn/core/security/data/`: Secure data orchestration and query execution.
- `src/main/java/com/vn/core/security/access/`: CRUD and access-constraint application.
- `src/main/java/com/vn/core/security/fetch/`: Fetch-plan parsing and resolution.
- `src/main/java/com/vn/core/security/web/`: Explicit JSON adapters and payload validation for secured entity endpoints.
- `src/main/java/com/vn/core/security/serialize/`: Read-side secure serialization.
- `src/main/java/com/vn/core/security/merge/`: Write-side secure merge enforcement.
- `src/main/java/com/vn/core/service/security/`: API-facing security capability and menu-permission services.
- `frontend/src/app/core/`: Frontend auth, HTTP interceptors, request helpers, and core utilities.
- `frontend/src/app/layout/`: Shell layout, sidebar/topbar, breadcrumbs, and navigation filtering.
- `frontend/src/app/pages/admin/security/`: Frontend security-admin screens.
- `frontend/src/app/pages/entities/`: Frontend secured entity screens.

**Testing:**
- `src/test/java/com/vn/core/`: Backend tests for the active root application.
- `src/test/resources/`: Backend test config, test mail templates, and fetch-plan fixtures.
- `frontend/src/**/*.spec.ts`: Frontend unit tests colocated with the active Angular app.
- `frontend/playwright.config.ts`: Frontend end-to-end test configuration.
- `angapp/src/test/java/` and `angapp/src/main/webapp/**/*.spec.ts`: Legacy reference tests, not the active frontend test suite.

## Module Boundaries

**Active Production Backend Boundary:**
- Put active backend runtime code only under `src/main/java/com/vn/core/**` and active backend tests only under `src/test/java/com/vn/core/**`.
- Do not add new production backend features to `angapp/src/main/java/**` unless the task explicitly targets the legacy reference app.

**Business Domain vs Security Metadata Boundary:**
- Keep user and proof/business entities in `src/main/java/com/vn/core/domain/` and their repositories in `src/main/java/com/vn/core/repository/`.
- Keep permission, menu-definition, and menu-permission entities in `src/main/java/com/vn/core/security/domain/` and their repositories in `src/main/java/com/vn/core/security/repository/`.

**REST vs Service vs Enforcement Boundary:**
- Keep HTTP request parsing, pagination headers, and status handling in `src/main/java/com/vn/core/web/rest/**`.
- Keep API-oriented orchestration in `src/main/java/com/vn/core/service/**`.
- Keep reusable enforcement machinery in `src/main/java/com/vn/core/security/**`.
- Follow the layer contract enforced by `src/test/java/com/vn/core/TechnicalStructureTest.java`.

**Frontend Shell vs Feature Boundary:**
- Keep global app providers, auth, and interceptors in `frontend/src/app/core/**`.
- Keep shell navigation, breadcrumbs, and layout chrome in `frontend/src/app/layout/**`.
- Keep routed feature UI in `frontend/src/app/pages/**`.
- Keep reusable but non-shell UI helpers in `frontend/src/app/shared/**`.

**Active Frontend vs Reference Frontends Boundary:**
- Put active standalone UI work in `frontend/src/**`.
- Treat `angapp/src/main/webapp/app/**`, `aef-main/aef-main/src/**`, and `jhipter-angular/frontend/src/**` as reference material unless the task explicitly says to update them.

**Translation Source Boundary:**
- Edit translation source files in `frontend/src/i18n/**`.
- Treat `frontend/public/i18n/*.json` and `frontend/src/app/config/i18n-hash.generated.ts` as generated outputs of `frontend/scripts/merge-i18n.cjs`.

## Important Packages and Apps

**Active Backend App:**
- Package root: `src/main/java/com/vn/core/`
- Major packages: `config`, `management`, `repository`, `domain`, `service`, `security`, `web/rest`, `aop/logging`
- Use this tree for all deployable backend work.

**Active Standalone Frontend App:**
- App root: `frontend/src/app/`
- Major areas: `core`, `layout`, `pages`, `shared`, `config`
- Use this tree for all deployable frontend work.

**Legacy Reference App:**
- Roots: `angapp/src/main/java/com/mycompany/myapp/` and `angapp/src/main/webapp/app/`
- Use this tree to compare behavior and migration parity, not as the default destination for new production code.

**UI Reference Workspace:**
- Root: `aef-main/aef-main/`
- Use this workspace as a layout/styling reference for the standalone frontend shell when a task explicitly calls for it.

**Additional Split-App Reference:**
- Roots: `jhipter-angular/backend/` and `jhipter-angular/frontend/`
- Use this workspace only when a task explicitly asks for that sample/reference stack.

## Where Major Concerns Live

**Authentication and Account Flows:**
- Backend: `src/main/java/com/vn/core/web/rest/AuthenticateController.java`, `src/main/java/com/vn/core/web/rest/AccountResource.java`, `src/main/java/com/vn/core/security/DomainUserDetailsService.java`
- Frontend: `frontend/src/app/core/auth/`, `frontend/src/app/pages/login/login.component.ts`

**Classic User and Authority Admin:**
- Backend: `src/main/java/com/vn/core/web/rest/UserResource.java`, `src/main/java/com/vn/core/web/rest/AuthorityResource.java`, `src/main/java/com/vn/core/service/UserService.java`
- Frontend: `frontend/src/app/pages/admin/user-management/`

**Secured Entity Enforcement:**
- Backend orchestration: `src/main/java/com/vn/core/security/data/`, `src/main/java/com/vn/core/security/access/`, `src/main/java/com/vn/core/security/merge/`, `src/main/java/com/vn/core/security/serialize/`, `src/main/java/com/vn/core/security/web/`
- Backend entity APIs: `src/main/java/com/vn/core/web/rest/OrganizationResource.java`, `src/main/java/com/vn/core/web/rest/DepartmentResource.java`, `src/main/java/com/vn/core/web/rest/EmployeeResource.java`
- Frontend consumers: `frontend/src/app/pages/entities/`, `frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts`

**Security Metadata Administration:**
- Backend: `src/main/java/com/vn/core/web/rest/admin/security/`, `src/main/java/com/vn/core/service/security/`, `src/main/java/com/vn/core/service/dto/security/`, `src/main/java/com/vn/core/service/mapper/security/`, `src/main/java/com/vn/core/security/domain/`, `src/main/java/com/vn/core/security/repository/`
- Frontend: `frontend/src/app/pages/admin/security/`

**Menu and Navigation Permissions:**
- Backend: `src/main/java/com/vn/core/web/rest/MenuPermissionResource.java`, `src/main/java/com/vn/core/service/security/CurrentUserMenuPermissionService.java`
- Frontend: `frontend/src/app/layout/navigation/`

**Fetch Plans:**
- Definition: `src/main/resources/fetch-plans.yml`
- Runtime parsing/resolution: `src/main/java/com/vn/core/security/fetch/`
- Entity references: `src/main/java/com/vn/core/service/OrganizationService.java`, `src/main/java/com/vn/core/service/DepartmentService.java`, `src/main/java/com/vn/core/service/EmployeeService.java`

**Database Schema and Seed Data:**
- Master changelog: `src/main/resources/config/liquibase/master.xml`
- Incremental changelogs: `src/main/resources/config/liquibase/changelog/*.xml`
- CSV seed data: `src/main/resources/config/liquibase/data/*.csv`

**Operations and Local Runtime:**
- Backend/docker manifests: `src/main/docker/*.yml`
- Gradle conventions: `buildSrc/src/main/groovy/*.gradle`
- Root npm automation: `package.json`
- Frontend dev/build/test automation: `frontend/package.json`

**Localization:**
- Backend bundles: `src/main/resources/i18n/*.properties`
- Frontend source translations: `frontend/src/i18n/**`
- Frontend merged translation assets: `frontend/public/i18n/*.json`

## Naming Conventions

**Files:**
- Backend REST controllers use `*Resource.java`, for example `src/main/java/com/vn/core/web/rest/OrganizationResource.java`.
- Backend services use `*Service.java`, for example `src/main/java/com/vn/core/service/OrganizationService.java`.
- Backend repositories use `*Repository.java`, for example `src/main/java/com/vn/core/security/repository/SecPermissionRepository.java`.
- Backend DTOs and mappers use `*DTO.java` and `*Mapper.java`, for example `src/main/java/com/vn/core/service/dto/security/SecRoleDTO.java` and `src/main/java/com/vn/core/service/mapper/security/SecPermissionMapper.java`.
- Frontend route files use `*.routes.ts`, frontend services use `*.service.ts`, frontend models use `*.model.ts`, and routed screens use `*.component.ts`.

**Directories:**
- Backend directories follow technical layers, for example `src/main/java/com/vn/core/config/`, `src/main/java/com/vn/core/service/`, `src/main/java/com/vn/core/security/`, `src/main/java/com/vn/core/web/rest/`.
- Frontend feature directories group screens by area and feature, for example `frontend/src/app/pages/entities/organization/` and `frontend/src/app/pages/admin/security/roles/`.
- Frontend shell-specific code stays under `frontend/src/app/layout/`, not under feature folders.

## Where to Add New Code

**New Backend Secured Entity Feature:**
- Primary code: `src/main/java/com/vn/core/domain/`, `src/main/java/com/vn/core/repository/`, `src/main/java/com/vn/core/service/`, `src/main/java/com/vn/core/web/rest/`
- Security integration: mark the entity in `src/main/java/com/vn/core/domain/` with `@SecuredEntity`, add plan definitions to `src/main/resources/fetch-plans.yml`, and extend runtime enforcement only if the generic `src/main/java/com/vn/core/security/**` pipeline is insufficient
- Tests: `src/test/java/com/vn/core/web/rest/` plus focused service/security tests under `src/test/java/com/vn/core/`

**New Security Metadata Capability or Admin Tool:**
- Implementation: `src/main/java/com/vn/core/service/security/`
- DTOs and mappers: `src/main/java/com/vn/core/service/dto/security/` and `src/main/java/com/vn/core/service/mapper/security/`
- Persistence: `src/main/java/com/vn/core/security/domain/` and `src/main/java/com/vn/core/security/repository/`
- REST endpoints: `src/main/java/com/vn/core/web/rest/admin/security/`
- Frontend screens: `frontend/src/app/pages/admin/security/`

**New Frontend Routed Feature:**
- Implementation: `frontend/src/app/pages/<area>/<feature>/`
- Route registration: `frontend/src/app/pages/<area>/<area>.routes.ts` and, if top-level, `frontend/src/app.routes.ts`
- Menu visibility: `frontend/src/app/layout/navigation/navigation-registry.ts` when the feature needs shell navigation
- Tests: colocated `frontend/src/app/pages/<area>/<feature>/**/*.spec.ts`

**Shared Frontend Utilities:**
- Cross-app auth/request/config helpers: `frontend/src/app/core/`
- Shell-only utilities: `frontend/src/app/layout/`
- Generic reusable UI and directives: `frontend/src/app/shared/`

**Reference or Migration-Only Changes:**
- Legacy behavior references: `angapp/`
- UI references: `aef-main/aef-main/`
- Extra sample stack: `jhipter-angular/`
- Use these only when the task explicitly targets them; otherwise keep implementation work in `src/` or `frontend/`.

## Special Directories

**`src/main/resources/config/liquibase/changelog/`:**
- Purpose: Ordered schema evolution files for the active backend.
- Generated: No
- Committed: Yes

**`src/main/resources/config/tls/`:**
- Purpose: TLS material location for backend runtime configuration.
- Generated: No
- Committed: Yes

**`src/main/docker/`:**
- Purpose: Local service stacks and operational compose definitions for the active backend environment.
- Generated: No
- Committed: Yes

**`frontend/src/i18n/`:**
- Purpose: Source-of-truth translation fragments for the standalone frontend.
- Generated: No
- Committed: Yes

**`frontend/public/i18n/`:**
- Purpose: Merged translation payloads served by the frontend runtime.
- Generated: Yes, by `frontend/scripts/merge-i18n.cjs`
- Committed: Yes

**`angapp/`:**
- Purpose: Legacy application tree kept in-repo for migration parity and behavior reference.
- Generated: No
- Committed: Yes

**`.planning/codebase/`:**
- Purpose: Generated codebase map documents used by later planning and execution commands.
- Generated: Yes
- Committed: Yes

---

*Structure analysis: 2026-03-27*
