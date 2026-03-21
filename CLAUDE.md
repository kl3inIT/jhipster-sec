<!-- GSD:project-start source:PROJECT.md -->

## Project

**JHipster Security Platform**

This is a brownfield migration that evolves the current JHipster security service into a fuller security platform with a standalone Angular frontend. The target system must preserve the current backend auth and admin capabilities while absorbing the Jmix-style security behavior from `angapp`, including secure data access, row-level rules, attribute-level rules, and fetch-plan-driven reads.

**Core Value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, row-level, and attribute-level access decisions.

### Constraints

- **Compatibility**: Preserve the functional security capabilities already working in `angapp` - entity CRUD checks, attribute permissions, row policies, secure merge behavior, and fetch-plan-driven secure reads must still work after the merge
- **Frontend structure**: The new UI must live in a standalone `frontend/` app modeled after `aef-main/aef-main` - PrimeNG Sakai plus JHipster-style Angular structure
- **Fetch plans**: Fetch plans must be defined in YAML or code builders only - database storage for fetch-plan definitions is not allowed
- **Brownfield safety**: Existing authentication, account, admin-user, and mail flows in the current backend must not regress during the migration
- **API boundary**: Existing JHipster account/user APIs may keep minimal boundary request/response models where dropping them would destabilize the public contract or validation model
<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->

## Technology Stack

## Languages

- Java 21 - application code lives under `src/main/java/com/vn/core/**` and tests under `src/test/java/com/vn/core/**`; the version is set in `build.gradle`.
- Groovy (Gradle DSL) - build logic is defined in `build.gradle`, `settings.gradle`, `gradle/*.gradle`, and `buildSrc/src/main/groovy/*.gradle`.
- YAML - runtime and infrastructure configuration lives in `src/main/resources/config/*.yml` and `src/main/docker/*.yml`.
- JSON - project metadata and Node tooling configuration live in `package.json`, `package-lock.json`, `.yo-rc.json`, and `sonar-project.properties`.
- HTML/Thymeleaf - server-rendered mail templates live in `src/main/resources/templates/mail/*.html`.
- Properties/XML - logging, Gradle, and migration metadata live in `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`, `src/main/resources/logback-spring.xml`, and `src/main/resources/config/liquibase/*.xml`.

## Runtime

- JVM: Java 21, configured by `sourceCompatibility` and `targetCompatibility` in `build.gradle`.
- Gradle Wrapper: 9.4.0, pinned in `gradle/wrapper/gradle-wrapper.properties`.
- Node.js: `>=24.14.0`, declared in `package.json`; it is used for scripts, formatting, hooks, and Docker orchestration rather than a browser app.
- Container runtime: Jib builds an OCI image from `eclipse-temurin:21-jre-noble` in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
- Gradle Wrapper - backend build and test entrypoint via `gradlew` and `gradlew.bat`.
- npm - developer scripts and JS tooling via `package.json`.
- Lockfile: `package-lock.json` present; Gradle wrapper present in `gradle/wrapper/gradle-wrapper.properties`.

## Frameworks

- Spring Boot 4.0.3 - application framework, declared in `gradle/libs.versions.toml` and bootstrapped from `src/main/java/com/vn/core/JhipsterSecApp.java`.
- JHipster Framework 9.0.0 - application conventions and config helpers, declared in `gradle/libs.versions.toml`; project generation metadata lives in `.yo-rc.json`.
- Spring MVC + Tomcat + Validation + Jackson + AspectJ + Thymeleaf - enabled by starters in `build.gradle`.
- Spring Data JPA + Hibernate ORM - persistence layer configured by starters in `build.gradle` and Hibernate settings in `src/main/resources/config/application.yml`.
- Spring Security + OAuth2 Resource Server - stateless JWT security configured in `src/main/java/com/vn/core/config/SecurityConfiguration.java` and `src/main/java/com/vn/core/config/SecurityJwtConfiguration.java`.
- Spring Mail + Thymeleaf templates - outbound account emails implemented in `src/main/java/com/vn/core/service/MailService.java` and `src/main/resources/templates/mail/*.html`.
- Springdoc OpenAPI 3.0.2 - API documentation support from `gradle/libs.versions.toml` and `src/main/resources/config/application.yml`.
- MapStruct 1.6.3 - DTO/entity mapping declared in `gradle/libs.versions.toml` and used in `src/main/java/com/vn/core/service/mapper/UserMapper.java`.
- JUnit Platform - enabled by `test` and `integrationTest` in `gradle/spring-boot.gradle`.
- Spring Boot Test + Spring Security Test - declared in `build.gradle`.
- Testcontainers + PostgreSQL - integration database support declared in `build.gradle` and implemented in `src/test/java/com/vn/core/config/DatabaseTestcontainer.java`.
- ArchUnit 1.4.1 - architecture tests declared in `gradle/libs.versions.toml` and used from `src/test/java/com/vn/core/TechnicalStructureTest.java`.
- Liquibase 5.0.1 with Gradle plugin 3.1.0 - schema migration tooling configured in `gradle.properties` and `gradle/liquibase.gradle`.
- Jib 3.5.3 - container image build plugin declared in `buildSrc/gradle/libs.versions.toml` and applied by `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
- JaCoCo 0.8.14 - coverage tooling configured in `buildSrc/src/main/groovy/jhipster.code-quality-conventions.gradle`.
- SonarQube Gradle plugin 7.2.3.7755 - static analysis integration declared in `buildSrc/gradle/libs.versions.toml` and configured by `sonar-project.properties`.
- Spotless 8.3.0 - Java formatting plugin declared in `buildSrc/gradle/libs.versions.toml`.
- Modernizer 1.12.0 and NoHTTP 0.0.11 - build hygiene plugins declared in `buildSrc/gradle/libs.versions.toml`.
- Checkstyle 13.3.0 - code style enforcement configured in `checkstyle.xml` and `buildSrc/src/main/groovy/jhipster.code-quality-conventions.gradle`.
- Prettier 3.8.1 with `prettier-plugin-java` 2.8.1 and `prettier-plugin-packagejson` 3.0.2 - formatting for non-Java and Java files via `package.json` and `.prettierrc`.
- Husky 9.1.7 and lint-staged 16.3.3 - local git hook tooling configured in `package.json` and `.lintstagedrc.cjs`.

## Key Dependencies

- `tech.jhipster:jhipster-framework` 9.0.0 - JHipster runtime support used across `src/main/java/com/vn/core/config/**`.
- `org.springframework.boot:*` starters - the HTTP API, security, JPA, mail, actuator, cache, and validation stack defined in `build.gradle`.
- `org.postgresql:postgresql` - JDBC driver for the primary SQL database, declared in `build.gradle`.
- `org.liquibase:liquibase-core` - schema migration engine configured in `gradle/liquibase.gradle` and `src/main/resources/config/liquibase/master.xml`.
- `com.hazelcast:hazelcast-spring` 5.5.0 and `com.hazelcast:hazelcast-hibernate53` 5.2.0 - application cache and Hibernate second-level cache configured in `src/main/java/com/vn/core/config/CacheConfiguration.java`.
- `io.micrometer:micrometer-registry-prometheus-simpleclient` - Prometheus metrics export enabled from `build.gradle` and `src/main/resources/config/application.yml`.
- `org.springdoc:springdoc-openapi-starter-webmvc-api` 3.0.2 - API docs support for `/v3/api-docs`, declared in `gradle/libs.versions.toml`.
- `org.mapstruct:mapstruct` 1.6.3 - compile-time mapping support used by `src/main/java/com/vn/core/service/mapper/UserMapper.java`.
- `com.zaxxer:HikariCP` - datasource pooling configured through `spring.datasource.hikari` in `src/main/resources/config/application-dev.yml` and `src/main/resources/config/application-prod.yml`.
- `org.testcontainers:*` - containerized test dependencies declared in `build.gradle`.
- `com.google.cloud.tools:jib-gradle-plugin` 3.5.3 - container image packaging from Gradle without a Dockerfile, configured in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.

## Configuration

- Base runtime configuration lives in `src/main/resources/config/application.yml`.
- Profile-specific overrides live in `src/main/resources/config/application-dev.yml`, `src/main/resources/config/application-prod.yml`, `src/main/resources/config/application-secret-samples.yml`, and `src/main/resources/config/application-tls.yml`.
- The default Gradle profile is `dev`, set in `gradle.properties` and expanded into `application.yml` by `gradle/spring-boot.gradle`.
- `spring.docker.compose.enabled` is currently `false` in `src/main/resources/config/application.yml`; Docker services are started explicitly through npm scripts in `package.json` and compose files in `src/main/docker/*.yml`.
- The project is server-only: `.yo-rc.json` sets `skipClient` to `true`, and there is no generated web client under `src/main/webapp/`.
- Secrets are not externalized through `.env` files or a secret manager in the current repository state; runtime credentials are stored directly in tracked config files such as `src/main/resources/config/application-*.yml`, `gradle/liquibase.gradle`, and `src/main/docker/*.yml`.
- Primary build files are `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/*.gradle`, and `buildSrc/**`.
- Quality configuration lives in `checkstyle.xml`, `sonar-project.properties`, `.prettierrc`, `.editorconfig`, and `.lintstagedrc.cjs`.
- Generator metadata lives in `.yo-rc.json`, which defines monolith/JWT/PostgreSQL/Hazelcast generation choices.

## Platform Requirements

- JDK 21 is required by `build.gradle`.
- Gradle 9.4.0 is supplied by `gradle/wrapper/gradle-wrapper.properties`.
- Node.js 24.14.0 or newer is required by `package.json`.
- Docker Compose is required for the local dependency stacks defined in `src/main/docker/services.yml`, `src/main/docker/postgresql.yml`, `src/main/docker/monitoring.yml`, `src/main/docker/sonar.yml`, and `src/main/docker/jhipster-control-center.yml`.
- PostgreSQL is required either through the container in `src/main/docker/postgresql.yml` or an externally reachable database matching the JDBC settings in `src/main/resources/config/application-dev.yml` and `src/main/resources/config/application-prod.yml`.
- Deploy the service as a JVM artifact with `bootJar` or `bootWar` tasks described in `package.json` and `README.md`, or as a container image via Jib in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
- Production requires a PostgreSQL database, a JWT secret for `jhipster.security.authentication.jwt.base64-secret`, and optional SMTP, Prometheus, and Logstash endpoints configured through `src/main/resources/config/application-prod.yml` or environment overrides.
- The generated Jib image exposes port `8080` for HTTP and `5701/udp` for Hazelcast, as configured in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

## Naming Patterns

- Use one top-level Java type per file with PascalCase names, for example `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`, and `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`.
- Use package names and directories as lowercase technical layers: `config`, `domain`, `repository`, `security`, `service`, `service/dto`, `service/mapper`, `web/rest`, and `web/rest/errors`.
- Use Angular kebab-case filenames with role suffixes, for example `angapp/src/main/webapp/app/entities/organization/update/organization-update.component.ts`, `angapp/src/main/webapp/app/entities/organization/service/organization.service.ts`, and `angapp/src/main/webapp/app/core/auth/account.service.ts`.
- Name backend tests with `*Test`, `*Tests`, or `*IT`, for example `src/test/java/com/vn/core/service/mapper/UserMapperTest.java` and `src/test/java/com/vn/core/web/rest/UserResourceIT.java`. Name frontend tests `*.spec.ts`, for example `angapp/src/main/webapp/app/core/auth/account.service.spec.ts`.
- Use lowerCamelCase verbs for methods and helpers, for example `registerUser`, `completePasswordReset`, `partialUpdate`, `addOrganizationToCollectionIfMissing`, and `navigateToStoredUrl` in `src/main/java/com/vn/core/service/UserService.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`, and `angapp/src/main/webapp/app/core/auth/account.service.ts`.
- Keep boolean-returning methods phrased as predicates, for example `isAuthenticated`, `hasAnyAuthority`, and `compareOrganization` in `angapp/src/main/webapp/app/core/auth/account.service.ts` and `angapp/src/main/webapp/app/entities/organization/service/organization.service.ts`.
- Use `UPPER_SNAKE_CASE` for constants, for example `DEFAULT_LOGIN`, `UPDATED_EMAIL`, `DETAIL_FETCH_PLAN_CODE`, and `FIELD_ERRORS_KEY` in `src/test/java/com/vn/core/web/rest/UserResourceIT.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`, and `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`.
- Use `LOG` for SLF4J loggers and descriptive lowerCamelCase names for dependencies, for example `userRepository`, `authorityRepository`, `organizationMapper`, and `applicationConfigService` in `src/main/java/com/vn/core/service/UserService.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`, and `angapp/src/main/webapp/app/core/auth/account.service.ts`.
- Use `DTO` suffix for service payloads, `VM` suffix for request/view models, and `Mapper` suffix for mapping components, for example `AdminUserDTO`, `OrganizationDTO`, `LoginVM`, `ManagedUserVM`, `UserMapper`, and `OrganizationMapper` in `src/main/java/com/vn/core/service/dto/`, `src/main/java/com/vn/core/web/rest/vm/`, and `angapp/src/main/java/com/mycompany/myapp/service/mapper/`.
- Use Angular `IType` and `NewType` aliases for entity models, for example `IOrganization` and `NewOrganization` in `angapp/src/main/webapp/app/entities/organization/organization.model.ts`.

## Code Style

- Use `.editorconfig` and Prettier as the baseline formatter. `.editorconfig` sets LF endings, UTF-8, trimmed trailing whitespace, 2-space indentation globally, and 4-space indentation for `*.java` in `.editorconfig`.
- Keep Java and TypeScript aligned with `.prettierrc`: `printWidth: 140`, `singleQuote: true`, `tabWidth: 2`, `arrowParens: avoid`, plus `prettier-plugin-java` and `prettier-plugin-packagejson` in `.prettierrc`.
- Preserve final newlines and avoid tabs unless a file already requires them. The repo standard is spaces in `.editorconfig`.
- Root backend quality relies on Gradle JHipster quality conventions plus `checkstyle.xml`. The active custom rules are lightweight: keep HTTP URLs out of code and do not require Javadoc on every method in `checkstyle.xml`.
- `angapp/` enforces stricter frontend linting through `angapp/eslint.config.mjs`. Follow explicit return types, selector naming (`jhi-...` elements and `jhi...` attributes), `eqeqeq`, `curly`, `prefer-nullish-coalescing`, `prefer-optional-chain`, and no `console` except `warn` and `error`.
- `angapp/tsconfig.json` enables strict TypeScript, strict Angular templates, `noImplicitReturns`, and `strictNullChecks`. New frontend code should compile under that baseline.

## Import Organization

- Use the Angular `baseUrl` in `angapp/tsconfig.json` and prefer `app/...` imports instead of long relative paths, for example `app/core/auth/account.model` and `app/core/config/application-config.service`.
- Use relative imports only for same-feature siblings, for example `../organization.model` and `./organization-form.service` in `angapp/src/main/webapp/app/entities/organization/`.

## Layering Conventions

- Keep the root service aligned with the ArchUnit rule in `src/test/java/com/vn/core/TechnicalStructureTest.java`: `web` may depend on `service` and `config`, `service` may depend on `repository` and `security`, and `domain` stays at the bottom.
- Keep the same shape in `angapp/` using `angapp/src/test/java/com/mycompany/myapp/TechnicalStructureTest.java`.
- Put REST entry points in `web/rest`, business logic in `service` or `service/impl`, persistence in `repository`, and transport types in `service/dto` or `web/rest/vm`. Do not reach from controllers directly into unrelated layers when a service already exists.
- Group Angular code by feature under `angapp/src/main/webapp/app/`, for example `core/`, `shared/`, `admin/`, and `entities/organization/`.
- Keep feature-local models, routes, services, forms, components, and samples together, as shown by `angapp/src/main/webapp/app/entities/organization/organization.model.ts`, `organization.routes.ts`, `service/organization.service.ts`, `update/organization-form.service.ts`, and `organization.test-samples.ts`.

## Error Handling

- Validate request payloads at the boundary with `@Valid` and field constraints on DTOs/VMs/entities, as shown in `src/main/java/com/vn/core/web/rest/UserResource.java`, `src/main/java/com/vn/core/web/rest/AccountResource.java`, `src/main/java/com/vn/core/service/dto/AdminUserDTO.java`, and `src/main/java/com/vn/core/web/rest/vm/LoginVM.java`.
- Throw domain-specific exceptions from services and controllers instead of ad hoc status handling. Examples: `UsernameAlreadyUsedException`, `EmailAlreadyUsedException`, `InvalidPasswordException`, and `BadRequestAlertException` in `src/main/java/com/vn/core/service/` and `src/main/java/com/vn/core/web/rest/errors/`.
- Let `ExceptionTranslator` convert backend failures into RFC7807 problem details with message keys, field errors, and request paths. Root implementation lives in `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`; `angapp/` mirrors it in `angapp/src/main/java/com/mycompany/myapp/web/rest/errors/ExceptionTranslator.java`.
- In `angapp/` service implementations that read secure map payloads, translate missing entities to `Optional.empty()` or `EntityNotFoundException` at the service boundary, as shown in `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`.
- On the frontend, keep save flows symmetrical: set an `isSaving` flag, subscribe with `next` and `error`, and clear state in `finalize`, as shown in `angapp/src/main/webapp/app/entities/organization/update/organization-update.component.ts`.

## Logging

- Declare a class-local `private static final Logger LOG` and log request-level or lifecycle events at `debug` or `info`, as shown in `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/JhipsterSecApp.java`, and `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`.
- Keep cross-cutting logging in dedicated infrastructure classes such as `src/main/java/com/vn/core/aop/logging/LoggingAspect.java` and `angapp/src/main/java/com/mycompany/myapp/aop/logging/LoggingAspect.java`.
- In Angular, avoid `console.log`; `angapp/eslint.config.mjs` only permits `console.warn` and `console.error`.

## Comments

- Keep class-level Javadoc on major Spring components and tests, as shown in `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`, and `src/test/java/com/vn/core/web/rest/UserResourceIT.java`.
- Use inline comments sparingly for non-obvious runtime workarounds or generated extension hooks, for example the Hazelcast workaround in `src/main/java/com/vn/core/JhipsterSecApp.java`, test setup notes in `src/test/java/com/vn/core/web/rest/UserResourceIT.java`, and `jhipster-needle-*` placeholders in generated config classes.
- Frontend code uses lightweight block comments mainly for generated type helpers, for example `angapp/src/main/webapp/app/entities/organization/update/organization-form.service.ts`. New Angular code should follow that restraint and avoid comment noise.

## Function Design

- Favor constructor injection with final fields in Spring classes, as seen in `src/main/java/com/vn/core/service/UserService.java` and `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`.
- Favor `inject(...)` fields instead of constructors in Angular services and components, as seen in `angapp/src/main/webapp/app/core/auth/account.service.ts` and `angapp/src/main/webapp/app/entities/organization/update/organization-update.component.ts`.
- Return `Optional<T>` for backend lookups that can miss, `Page<T>` for pagination, and `ResponseEntity<T>` from controllers. Use those shapes consistently in `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/web/rest/UserResource.java`, and `angapp/src/main/java/com/mycompany/myapp/service/OrganizationService.java`.
- Return `Observable<HttpResponse<T>>` from Angular API services and plain model values from form services, as shown in `angapp/src/main/webapp/app/entities/organization/service/organization.service.ts` and `angapp/src/main/webapp/app/entities/organization/update/organization-form.service.ts`.

## Mapping Patterns

- Root user mapping is hand-written and registered as a Spring service in `src/main/java/com/vn/core/service/mapper/UserMapper.java`. Follow that pattern only where generated MapStruct support is intentionally avoided.
- `angapp/` prefers MapStruct interfaces extending `EntityMapper`, for example `angapp/src/main/java/com/mycompany/myapp/service/mapper/OrganizationMapper.java`, `DepartmentMapper.java`, and `EmployeeMapper.java`.
- When partial updates are required, add an explicit `partialUpdate(@MappingTarget ...)` contract as in `angapp/src/main/java/com/mycompany/myapp/service/mapper/OrganizationMapper.java`.
- `angapp/` also contains manual map-to-DTO shaping for secured fetch-plan results in `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`. Keep those conversions private to the service that owns the secure payload format.

## Configuration Practices

- Bind application-specific settings through typed `@ConfigurationProperties` classes such as `src/main/java/com/vn/core/config/ApplicationProperties.java` and `angapp/src/main/java/com/mycompany/myapp/config/ApplicationProperties.java` instead of scattering `@Value` fields.
- Keep profile-specific runtime config in YAML under `src/main/resources/config/` and `angapp/src/main/resources/config/`, with test overrides under `src/test/resources/config/application.yml` and `angapp/src/test/resources/config/application.yml`.
- Keep generated extension markers like `jhipster-needle-*` intact in files such as `src/main/java/com/vn/core/config/ApplicationProperties.java` and `build.gradle`.
- Resolve API URLs through `ApplicationConfigService` in `angapp/src/main/webapp/app/core/config/application-config.service.ts`. Do not hardcode raw endpoint prefixes in feature services.
- Centralize compiler and lint behavior in `angapp/tsconfig.json`, `angapp/tsconfig.spec.json`, `angapp/eslint.config.mjs`, and `.prettierrc`.

## Module Design

<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

## Pattern Overview

- Use a single Spring Boot application entry point in `src/main/java/com/vn/core/JhipsterSecApp.java` and a single Gradle root module in `settings.gradle`.
- Keep runtime code inside the `com.vn.core` package tree and enforce layer boundaries with ArchUnit in `src/test/java/com/vn/core/TechnicalStructureTest.java`.
- Expose backend-only functionality from the root app; `.yo-rc.json` sets `skipClient: true` and no root `src/main/webapp` is present.

## Layers

- Purpose: Start the Spring application, apply default profiles, and support servlet-container deployment.
- Location: `src/main/java/com/vn/core/`
- Contains: `JhipsterSecApp`, `ApplicationWebXml`, generated marker classes.
- Depends on: Spring Boot auto-configuration, `com.vn.core.config`, environment properties.
- Used by: JVM startup, servlet containers, Gradle `bootJar` / `bootWar` packaging.
- Purpose: Wire framework concerns such as security, persistence, caching, Liquibase, CORS, Jackson, async execution, and logging.
- Location: `src/main/java/com/vn/core/config/`
- Contains: `SecurityConfiguration`, `SecurityJwtConfiguration`, `DatabaseConfiguration`, `LiquibaseConfiguration`, `CacheConfiguration`, `WebConfigurer`, `ApplicationProperties`.
- Depends on: Spring Boot, JHipster properties, Hazelcast, Liquibase, JPA, JWT support.
- Used by: Every other runtime layer through Spring bean wiring.
- Purpose: Terminate HTTP requests, validate request bodies, apply endpoint-level authorization, and translate service results into HTTP responses.
- Location: `src/main/java/com/vn/core/web/rest/`
- Contains: REST controllers such as `AccountResource`, `AuthenticateController`, `UserResource`, `PublicUserResource`, `AuthorityResource`, plus request view models in `src/main/java/com/vn/core/web/rest/vm/`.
- Depends on: `com.vn.core.service`, `com.vn.core.repository` for a few account checks, Spring MVC, validation, security annotations.
- Used by: External API clients and management consumers.
- Purpose: Hold transaction-scoped business workflows for user lifecycle, password reset, account mutation, mail dispatch, and DTO translation.
- Location: `src/main/java/com/vn/core/service/`
- Contains: `UserService`, `MailService`, service-level exceptions, DTOs in `src/main/java/com/vn/core/service/dto/`, mapping helpers in `src/main/java/com/vn/core/service/mapper/`.
- Depends on: `com.vn.core.repository`, `com.vn.core.domain`, `com.vn.core.security`, Spring transactions, cache manager, password encoding.
- Used by: `src/main/java/com/vn/core/web/rest/` and selected security/config beans.
- Purpose: Authenticate users from persistence, expose authority constants and security helpers, and bridge the current principal into auditing.
- Location: `src/main/java/com/vn/core/security/`
- Contains: `DomainUserDetailsService`, `SecurityUtils`, `SpringSecurityAuditorAware`, `AuthoritiesConstants`, `UserNotActivatedException`.
- Depends on: `com.vn.core.repository`, `com.vn.core.domain`, Spring Security, JWT claim access.
- Used by: `src/main/java/com/vn/core/config/SecurityConfiguration.java`, `src/main/java/com/vn/core/web/rest/AuthenticateController.java`, `src/main/java/com/vn/core/service/UserService.java`.
- Purpose: Persist aggregates and expose query methods through Spring Data repositories.
- Location: `src/main/java/com/vn/core/repository/`
- Contains: `UserRepository`, `AuthorityRepository`.
- Depends on: `com.vn.core.domain`, JPA, entity graphs, cache annotations.
- Used by: `src/main/java/com/vn/core/service/`, `src/main/java/com/vn/core/security/`, and a few REST controllers.
- Purpose: Define JPA entities and auditing base types that match the Liquibase schema.
- Location: `src/main/java/com/vn/core/domain/`
- Contains: `User`, `Authority`, `AbstractAuditingEntity`.
- Depends on: Jakarta Persistence, Bean Validation, Hibernate cache annotations.
- Used by: repositories, services, security, and admin-oriented REST responses.

## Data Flow

- Keep HTTP state stateless with JWT bearer tokens in `src/main/java/com/vn/core/config/SecurityConfiguration.java`.
- Persist durable state in PostgreSQL through JPA and Liquibase-configured schema files under `src/main/resources/config/liquibase/`.
- Cache user lookups and Hibernate second-level data through Hazelcast in `src/main/java/com/vn/core/config/CacheConfiguration.java` and cache annotations in `src/main/java/com/vn/core/repository/UserRepository.java`.

## Key Abstractions

- Purpose: Represent authenticated principals, profile data, reset/activation state, and authority membership.
- Examples: `src/main/java/com/vn/core/domain/User.java`, `src/main/java/com/vn/core/domain/Authority.java`
- Pattern: JPA entities with validation and Hibernate cache annotations, mirrored by Liquibase tables in `src/main/resources/config/liquibase/changelog/00000000000000_initial_schema.xml`.
- Purpose: Separate external request-response shapes from persistence entities.
- Examples: `src/main/java/com/vn/core/service/dto/AdminUserDTO.java`, `src/main/java/com/vn/core/service/dto/UserDTO.java`, `src/main/java/com/vn/core/web/rest/vm/LoginVM.java`, `src/main/java/com/vn/core/web/rest/vm/ManagedUserVM.java`
- Pattern: Controllers accept DTOs/VMs, services return DTOs, and `src/main/java/com/vn/core/service/mapper/UserMapper.java` handles conversions where entity exposure is not appropriate.
- Purpose: Normalize exceptions into RFC 7807-style API responses.
- Examples: `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`, `src/main/java/com/vn/core/web/rest/errors/BadRequestAlertException.java`
- Pattern: Throw typed exceptions from service or web code and let the controller advice translate them into structured `ProblemDetailWithCause` responses.
- Purpose: Hold app-specific configuration without scattering string lookups.
- Examples: `src/main/java/com/vn/core/config/ApplicationProperties.java`
- Pattern: Bind `application.*` keys from `src/main/resources/config/application.yml` into a dedicated configuration properties class.

## Entry Points

- Location: `src/main/java/com/vn/core/JhipsterSecApp.java`
- Triggers: `./gradlew`, packaged jar startup, IDE run configuration.
- Responsibilities: Boot the app, enforce sane profile combinations, and log startup endpoints.
- Location: `src/main/java/com/vn/core/ApplicationWebXml.java`
- Triggers: WAR deployment to an external servlet container.
- Responsibilities: Reuse the same Spring Boot application source and default profile handling outside embedded Tomcat startup.
- Location: `src/main/java/com/vn/core/web/rest/`
- Triggers: HTTP requests under `/api/**`.
- Responsibilities: Authentication, account self-service, admin user management, authority management, and public user listing.
- Location: `src/main/java/com/vn/core/config/LiquibaseConfiguration.java`
- Triggers: Application startup unless the `no-liquibase` profile disables it.
- Responsibilities: Start Liquibase synchronously or asynchronously and apply changelogs rooted at `src/main/resources/config/liquibase/master.xml`.

## Error Handling

- Translate application exceptions in `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java` into structured problem responses with status, title, message key, path, and field errors.
- Handle authentication and authorization failures at the filter layer in `src/main/java/com/vn/core/config/SecurityConfiguration.java` with `BearerTokenAuthenticationEntryPoint` and `BearerTokenAccessDeniedHandler`.
- Use Bean Validation annotations on DTOs, VMs, and entities such as `src/main/java/com/vn/core/web/rest/vm/ManagedUserVM.java` and `src/main/java/com/vn/core/domain/User.java`, then let Spring surface violations through the exception translator.

## Cross-Cutting Concerns

<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:

- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.

<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.

<!-- GSD:profile-end -->
