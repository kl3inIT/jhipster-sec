# Codebase Structure

**Analysis Date:** 2026-03-21

## Directory Layout

```text
[project-root]/
|-- .planning/codebase/                 # Generated codebase maps for GSD commands
|-- buildSrc/                           # Local Gradle convention plugins used by the root build
|-- gradle/                             # Gradle shared scripts applied by `build.gradle`
|-- src/main/java/com/vn/core/          # Root application runtime code
|   |-- aop/logging/                    # Dev-profile logging aspect
|   |-- config/                         # Spring Boot and infrastructure configuration
|   |-- domain/                         # JPA entities and auditing base classes
|   |-- management/                     # Operational/security metrics helpers
|   |-- repository/                     # Spring Data repositories
|   |-- security/                       # Security services, claims, and auditor bridge
|   |-- service/                        # Transactional business services, DTOs, mappers
|   `-- web/rest/                       # REST controllers, error contracts, view models
|-- src/main/resources/                 # Application configuration, Liquibase, i18n, templates
|-- src/main/docker/                    # Compose files and container packaging assets
|-- src/test/java/com/vn/core/          # Architecture, integration, and unit tests
|-- aef-main/                           # Separate adjacent application tree, not wired into root Gradle build
|-- angapp/                             # Separate adjacent application tree, not wired into root Gradle build
`-- jhipter-angular/                    # Separate adjacent application tree, not wired into root Gradle build
```

## Directory Purposes

**`src/main/java/com/vn/core/`:**
- Purpose: Hold all root application runtime code.
- Contains: Entry points, layered packages, and no client-side source tree.
- Key files: `src/main/java/com/vn/core/JhipsterSecApp.java`, `src/main/java/com/vn/core/ApplicationWebXml.java`

**`src/main/java/com/vn/core/config/`:**
- Purpose: Place framework wiring and cross-cutting infrastructure.
- Contains: Spring `@Configuration` classes, typed properties, web and security setup.
- Key files: `src/main/java/com/vn/core/config/SecurityConfiguration.java`, `src/main/java/com/vn/core/config/SecurityJwtConfiguration.java`, `src/main/java/com/vn/core/config/LiquibaseConfiguration.java`, `src/main/java/com/vn/core/config/CacheConfiguration.java`

**`src/main/java/com/vn/core/web/rest/`:**
- Purpose: Place HTTP-facing controllers and API-specific request/response helpers.
- Contains: `*Resource` and controller classes, `errors/` for problem details, `vm/` for request models.
- Key files: `src/main/java/com/vn/core/web/rest/AccountResource.java`, `src/main/java/com/vn/core/web/rest/AuthenticateController.java`, `src/main/java/com/vn/core/web/rest/UserResource.java`

**`src/main/java/com/vn/core/service/`:**
- Purpose: Place business workflows and transaction boundaries.
- Contains: `*Service` classes, DTOs under `dto/`, mappers under `mapper/`, service-specific exceptions.
- Key files: `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/service/MailService.java`, `src/main/java/com/vn/core/service/dto/AdminUserDTO.java`, `src/main/java/com/vn/core/service/mapper/UserMapper.java`

**`src/main/java/com/vn/core/repository/`:**
- Purpose: Place Spring Data JPA access to persisted aggregates.
- Contains: Repository interfaces only.
- Key files: `src/main/java/com/vn/core/repository/UserRepository.java`, `src/main/java/com/vn/core/repository/AuthorityRepository.java`

**`src/main/java/com/vn/core/domain/`:**
- Purpose: Place JPA entities that map to Liquibase-managed tables.
- Contains: Entities and shared auditing superclass.
- Key files: `src/main/java/com/vn/core/domain/User.java`, `src/main/java/com/vn/core/domain/Authority.java`, `src/main/java/com/vn/core/domain/AbstractAuditingEntity.java`

**`src/main/java/com/vn/core/security/`:**
- Purpose: Place authentication, claim, authority, and auditing helpers that are not generic framework config.
- Contains: `UserDetailsService`, security utilities, authority constants, auditing support, plus the secure data-access contracts and implementations under `security/data/`.
- Key files: `src/main/java/com/vn/core/security/DomainUserDetailsService.java`, `src/main/java/com/vn/core/security/SecurityUtils.java`, `src/main/java/com/vn/core/security/SpringSecurityAuditorAware.java`, `src/main/java/com/vn/core/security/data/SecureDataManager.java`, `src/main/java/com/vn/core/security/data/DataManager.java`, `src/main/java/com/vn/core/security/data/UnconstrainedDataManager.java`

**`src/main/resources/config/`:**
- Purpose: Store runtime configuration overlays and Liquibase migration definitions.
- Contains: `application*.yml`, `liquibase/`, TLS sample assets.
- Key files: `src/main/resources/config/application.yml`, `src/main/resources/config/application-dev.yml`, `src/main/resources/config/application-prod.yml`, `src/main/resources/config/liquibase/master.xml`

**`src/main/docker/`:**
- Purpose: Store deployment and local dependency orchestration assets.
- Contains: Compose files for app, PostgreSQL, monitoring, Sonar, and Jib entrypoint files.
- Key files: `src/main/docker/app.yml`, `src/main/docker/services.yml`, `src/main/docker/postgresql.yml`, `src/main/docker/jib/entrypoint.sh`

**`src/test/java/com/vn/core/`:**
- Purpose: Mirror production packages for tests and architectural checks.
- Contains: Integration tests, unit tests, test utilities, ArchUnit rules, test configuration.
- Key files: `src/test/java/com/vn/core/TechnicalStructureTest.java`, `src/test/java/com/vn/core/IntegrationTest.java`, `src/test/java/com/vn/core/web/rest/UserResourceIT.java`

**`buildSrc/`:**
- Purpose: Hold local Gradle plugins shared by the root project.
- Contains: Convention plugins for code quality and Docker image packaging.
- Key files: `buildSrc/src/main/groovy/jhipster.code-quality-conventions.gradle`, `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`

**`aef-main/`, `angapp/`, `jhipter-angular/`:**
- Purpose: Adjacent application trees present in the repository workspace.
- Contains: Separate source and build files outside the root service package.
- Key files: Not part of the root app module defined by `settings.gradle`; do not place new root-service code here unless the task explicitly targets one of these trees.

## Key File Locations

**Entry Points:**
- `src/main/java/com/vn/core/JhipsterSecApp.java`: Root Spring Boot startup class.
- `src/main/java/com/vn/core/ApplicationWebXml.java`: WAR deployment bootstrap.
- `src/main/java/com/vn/core/web/rest/AuthenticateController.java`: JWT login and authentication-check endpoint.

**Configuration:**
- `build.gradle`: Root Gradle build for the active service.
- `settings.gradle`: Declares the single root project `jhipster-sec`.
- `src/main/resources/config/application.yml`: Base Spring Boot and JHipster runtime config.
- `src/main/java/com/vn/core/config/SecurityConfiguration.java`: URL authorization and resource-server setup.
- `src/main/java/com/vn/core/config/LiquibaseConfiguration.java`: Migration bootstrap.

**Core Logic:**
- `src/main/java/com/vn/core/service/UserService.java`: User lifecycle and account business rules.
- `src/main/java/com/vn/core/security/DomainUserDetailsService.java`: Persistence-backed authentication lookup.
- `src/main/java/com/vn/core/repository/UserRepository.java`: User persistence queries and cacheable authority fetches.
- `src/main/java/com/vn/core/web/rest/AccountResource.java`: Self-service account API.

**Testing:**
- `src/test/java/com/vn/core/TechnicalStructureTest.java`: Enforces package layer rules.
- `src/test/java/com/vn/core/IntegrationTest.java`: Base integration-test annotation setup.
- `src/test/java/com/vn/core/web/rest/`: REST integration tests mirrored to controller packages.

## Naming Conventions

**Files:**
- `*Resource.java`: REST endpoints, for example `src/main/java/com/vn/core/web/rest/UserResource.java`.
- `*Controller.java`: Specialized controller naming for authentication, for example `src/main/java/com/vn/core/web/rest/AuthenticateController.java`.
- `*Service.java`: Transactional workflows, for example `src/main/java/com/vn/core/service/UserService.java`.
- `*Repository.java`: Spring Data repositories, for example `src/main/java/com/vn/core/repository/UserRepository.java`.
- `*DTO.java`: Service-layer transport objects, for example `src/main/java/com/vn/core/service/dto/UserDTO.java`.
- `*VM.java`: Request/response view models tied to web APIs, for example `src/main/java/com/vn/core/web/rest/vm/LoginVM.java`.
- `*Configuration.java`: Infrastructure wiring, for example `src/main/java/com/vn/core/config/CacheConfiguration.java`.
- `*IT.java`, `*Test.java`, `*Tests.java`: Integration and unit test files, for example `src/test/java/com/vn/core/web/rest/AccountResourceIT.java`.

**Directories:**
- Package directories mirror Java packages exactly under `src/main/java/com/vn/core/` and `src/test/java/com/vn/core/`.
- Subdirectories reflect technical layers rather than feature slices at the root level: `config`, `web`, `service`, `repository`, `domain`, `security`.
- Web-layer specializations live beneath the layer directory instead of beside it, for example `src/main/java/com/vn/core/web/rest/errors/` and `src/main/java/com/vn/core/web/rest/vm/`.

## Where to Add New Code

**New Feature:**
- Primary code: start with a domain entity in `src/main/java/com/vn/core/domain/`, a repository in `src/main/java/com/vn/core/repository/`, a service in `src/main/java/com/vn/core/service/`, and a REST endpoint in `src/main/java/com/vn/core/web/rest/` if the feature is externally reachable.
- Tests: mirror the package under `src/test/java/com/vn/core/`, and add migration coverage or integration coverage where the feature changes persistence behavior.

**New Component/Module:**
- Implementation: place cross-cutting framework wiring in `src/main/java/com/vn/core/config/`; place authentication helpers in `src/main/java/com/vn/core/security/`; place API-specific contracts in `src/main/java/com/vn/core/web/rest/errors/` or `src/main/java/com/vn/core/web/rest/vm/`.

**Utilities:**
- Shared helpers: keep domain-agnostic runtime helpers close to the layer that owns them rather than creating a broad `utils` package. Use `src/main/java/com/vn/core/security/` for security helpers, `src/main/java/com/vn/core/config/` for constants and properties, and `src/test/java/com/vn/core/web/rest/TestUtil.java` style placement for test-only utilities.

## Special Directories

**`build/`:**
- Purpose: Generated compilation outputs and packaged artifacts.
- Generated: Yes
- Committed: No

**`.planning/codebase/`:**
- Purpose: Generated analysis documents consumed by other GSD commands.
- Generated: Yes
- Committed: Typically yes, as workflow artifacts

**`buildSrc/`:**
- Purpose: Versioned local Gradle plugin logic for the root build.
- Generated: No
- Committed: Yes

**`src/main/docker/`:**
- Purpose: Versioned deployment and local services definitions.
- Generated: No
- Committed: Yes

**Root client directory:**
- Purpose: Not applicable in the active root service.
- Generated: Not detected
- Committed: Not detected

---

*Structure analysis: 2026-03-21*
