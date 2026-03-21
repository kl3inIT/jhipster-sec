# Architecture

**Analysis Date:** 2026-03-21

## Pattern Overview

**Overall:** Layered JHipster monolith with a stateless REST API and explicit package-level architecture enforcement.

**Key Characteristics:**
- Use a single Spring Boot application entry point in `src/main/java/com/vn/core/JhipsterSecApp.java` and a single Gradle root module in `settings.gradle`.
- Keep runtime code inside the `com.vn.core` package tree and enforce layer boundaries with ArchUnit in `src/test/java/com/vn/core/TechnicalStructureTest.java`.
- Expose backend-only functionality from the root app; `.yo-rc.json` sets `skipClient: true` and no root `src/main/webapp` is present.

## Layers

**Bootstrap / Runtime Layer:**
- Purpose: Start the Spring application, apply default profiles, and support servlet-container deployment.
- Location: `src/main/java/com/vn/core/`
- Contains: `JhipsterSecApp`, `ApplicationWebXml`, generated marker classes.
- Depends on: Spring Boot auto-configuration, `com.vn.core.config`, environment properties.
- Used by: JVM startup, servlet containers, Gradle `bootJar` / `bootWar` packaging.

**Configuration / Infrastructure Layer:**
- Purpose: Wire framework concerns such as security, persistence, caching, Liquibase, CORS, Jackson, async execution, and logging.
- Location: `src/main/java/com/vn/core/config/`
- Contains: `SecurityConfiguration`, `SecurityJwtConfiguration`, `DatabaseConfiguration`, `LiquibaseConfiguration`, `CacheConfiguration`, `WebConfigurer`, `ApplicationProperties`.
- Depends on: Spring Boot, JHipster properties, Hazelcast, Liquibase, JPA, JWT support.
- Used by: Every other runtime layer through Spring bean wiring.

**Web API Layer:**
- Purpose: Terminate HTTP requests, validate request bodies, apply endpoint-level authorization, and translate service results into HTTP responses.
- Location: `src/main/java/com/vn/core/web/rest/`
- Contains: REST controllers such as `AccountResource`, `AuthenticateController`, `UserResource`, `PublicUserResource`, `AuthorityResource`, plus request view models in `src/main/java/com/vn/core/web/rest/vm/`.
- Depends on: `com.vn.core.service`, `com.vn.core.repository` for a few account checks, Spring MVC, validation, security annotations.
- Used by: External API clients and management consumers.

**Service Layer:**
- Purpose: Hold transaction-scoped business workflows for user lifecycle, password reset, account mutation, mail dispatch, and DTO translation.
- Location: `src/main/java/com/vn/core/service/`
- Contains: `UserService`, `MailService`, service-level exceptions, DTOs in `src/main/java/com/vn/core/service/dto/`, mapping helpers in `src/main/java/com/vn/core/service/mapper/`.
- Depends on: `com.vn.core.repository`, `com.vn.core.domain`, `com.vn.core.security`, Spring transactions, cache manager, password encoding.
- Used by: `src/main/java/com/vn/core/web/rest/` and selected security/config beans.

**Security Layer:**
- Purpose: Authenticate users from persistence, expose authority constants and security helpers, and bridge the current principal into auditing.
- Location: `src/main/java/com/vn/core/security/`
- Contains: `DomainUserDetailsService`, `SecurityUtils`, `SpringSecurityAuditorAware`, `AuthoritiesConstants`, `UserNotActivatedException`.
- Depends on: `com.vn.core.repository`, `com.vn.core.domain`, Spring Security, JWT claim access.
- Used by: `src/main/java/com/vn/core/config/SecurityConfiguration.java`, `src/main/java/com/vn/core/web/rest/AuthenticateController.java`, `src/main/java/com/vn/core/service/UserService.java`.

**Persistence Layer:**
- Purpose: Persist aggregates and expose query methods through Spring Data repositories.
- Location: `src/main/java/com/vn/core/repository/`
- Contains: `UserRepository`, `AuthorityRepository`.
- Depends on: `com.vn.core.domain`, JPA, entity graphs, cache annotations.
- Used by: `src/main/java/com/vn/core/service/`, `src/main/java/com/vn/core/security/`, and a few REST controllers.

**Domain Layer:**
- Purpose: Define JPA entities and auditing base types that match the Liquibase schema.
- Location: `src/main/java/com/vn/core/domain/`
- Contains: `User`, `Authority`, `AbstractAuditingEntity`.
- Depends on: Jakarta Persistence, Bean Validation, Hibernate cache annotations.
- Used by: repositories, services, security, and admin-oriented REST responses.

## Data Flow

**Authentication Flow:**

1. `POST /api/authenticate` enters `src/main/java/com/vn/core/web/rest/AuthenticateController.java`.
2. The controller delegates credential verification to Spring Security's `AuthenticationManagerBuilder`.
3. Spring resolves users through `src/main/java/com/vn/core/security/DomainUserDetailsService.java`, which loads `User` plus authorities from `src/main/java/com/vn/core/repository/UserRepository.java`.
4. `AuthenticateController` signs a JWT using `src/main/java/com/vn/core/config/SecurityJwtConfiguration.java` and returns it in both the response body and `Authorization` header.
5. Later requests pass through the `SecurityFilterChain` in `src/main/java/com/vn/core/config/SecurityConfiguration.java`, which validates bearer tokens and populates the security context.

**User Management Flow:**

1. API requests hit controllers in `src/main/java/com/vn/core/web/rest/`, typically `AccountResource` or `UserResource`.
2. Controllers validate payloads with `@Valid` view models or DTOs from `src/main/java/com/vn/core/web/rest/vm/` and `src/main/java/com/vn/core/service/dto/`.
3. Business operations run inside `src/main/java/com/vn/core/service/UserService.java`, where transactions, password hashing, cache eviction, and authority assignment happen.
4. Persistence goes through Spring Data repositories in `src/main/java/com/vn/core/repository/`, backed by JPA entities in `src/main/java/com/vn/core/domain/`.
5. Database shape and seed data are controlled by Liquibase changelogs in `src/main/resources/config/liquibase/master.xml` and `src/main/resources/config/liquibase/changelog/00000000000000_initial_schema.xml`.

**State Management:**
- Keep HTTP state stateless with JWT bearer tokens in `src/main/java/com/vn/core/config/SecurityConfiguration.java`.
- Persist durable state in PostgreSQL through JPA and Liquibase-configured schema files under `src/main/resources/config/liquibase/`.
- Cache user lookups and Hibernate second-level data through Hazelcast in `src/main/java/com/vn/core/config/CacheConfiguration.java` and cache annotations in `src/main/java/com/vn/core/repository/UserRepository.java`.

## Key Abstractions

**User Aggregate:**
- Purpose: Represent authenticated principals, profile data, reset/activation state, and authority membership.
- Examples: `src/main/java/com/vn/core/domain/User.java`, `src/main/java/com/vn/core/domain/Authority.java`
- Pattern: JPA entities with validation and Hibernate cache annotations, mirrored by Liquibase tables in `src/main/resources/config/liquibase/changelog/00000000000000_initial_schema.xml`.

**DTO / ViewModel Boundary:**
- Purpose: Separate external request-response shapes from persistence entities.
- Examples: `src/main/java/com/vn/core/service/dto/AdminUserDTO.java`, `src/main/java/com/vn/core/service/dto/UserDTO.java`, `src/main/java/com/vn/core/web/rest/vm/LoginVM.java`, `src/main/java/com/vn/core/web/rest/vm/ManagedUserVM.java`
- Pattern: Controllers accept DTOs/VMs, services return DTOs, and `src/main/java/com/vn/core/service/mapper/UserMapper.java` handles conversions where entity exposure is not appropriate.

**Problem Details Error Contract:**
- Purpose: Normalize exceptions into RFC 7807-style API responses.
- Examples: `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`, `src/main/java/com/vn/core/web/rest/errors/BadRequestAlertException.java`
- Pattern: Throw typed exceptions from service or web code and let the controller advice translate them into structured `ProblemDetailWithCause` responses.

**Typed Application Properties:**
- Purpose: Hold app-specific configuration without scattering string lookups.
- Examples: `src/main/java/com/vn/core/config/ApplicationProperties.java`
- Pattern: Bind `application.*` keys from `src/main/resources/config/application.yml` into a dedicated configuration properties class.

## Entry Points

**Spring Boot Main:**
- Location: `src/main/java/com/vn/core/JhipsterSecApp.java`
- Triggers: `./gradlew`, packaged jar startup, IDE run configuration.
- Responsibilities: Boot the app, enforce sane profile combinations, and log startup endpoints.

**Servlet Container Bootstrap:**
- Location: `src/main/java/com/vn/core/ApplicationWebXml.java`
- Triggers: WAR deployment to an external servlet container.
- Responsibilities: Reuse the same Spring Boot application source and default profile handling outside embedded Tomcat startup.

**REST API Surface:**
- Location: `src/main/java/com/vn/core/web/rest/`
- Triggers: HTTP requests under `/api/**`.
- Responsibilities: Authentication, account self-service, admin user management, authority management, and public user listing.

**Schema Migration Bootstrap:**
- Location: `src/main/java/com/vn/core/config/LiquibaseConfiguration.java`
- Triggers: Application startup unless the `no-liquibase` profile disables it.
- Responsibilities: Start Liquibase synchronously or asynchronously and apply changelogs rooted at `src/main/resources/config/liquibase/master.xml`.

## Error Handling

**Strategy:** Throw domain- or service-specific exceptions close to the failing workflow and centralize HTTP translation in controller advice and Spring Security handlers.

**Patterns:**
- Translate application exceptions in `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java` into structured problem responses with status, title, message key, path, and field errors.
- Handle authentication and authorization failures at the filter layer in `src/main/java/com/vn/core/config/SecurityConfiguration.java` with `BearerTokenAuthenticationEntryPoint` and `BearerTokenAccessDeniedHandler`.
- Use Bean Validation annotations on DTOs, VMs, and entities such as `src/main/java/com/vn/core/web/rest/vm/ManagedUserVM.java` and `src/main/java/com/vn/core/domain/User.java`, then let Spring surface violations through the exception translator.

## Cross-Cutting Concerns

**Logging:** Use startup and operational logging in classes such as `src/main/java/com/vn/core/JhipsterSecApp.java` and dev-profile AOP tracing in `src/main/java/com/vn/core/aop/logging/LoggingAspect.java`.

**Validation:** Use Jakarta Bean Validation on request models and entities, plus explicit password-length guards in `src/main/java/com/vn/core/web/rest/AccountResource.java`.

**Authentication:** Use stateless JWT resource-server security configured by `src/main/java/com/vn/core/config/SecurityConfiguration.java` and `src/main/java/com/vn/core/config/SecurityJwtConfiguration.java`.

**Authorization:** Combine URL rules in `src/main/java/com/vn/core/config/SecurityConfiguration.java` with method-level `@PreAuthorize` checks in controllers such as `src/main/java/com/vn/core/web/rest/UserResource.java`.

**Caching:** Centralize Hazelcast setup in `src/main/java/com/vn/core/config/CacheConfiguration.java` and evict user caches from `src/main/java/com/vn/core/service/UserService.java` after mutations.

**Schema Evolution:** Add and order database changes through Liquibase files under `src/main/resources/config/liquibase/`; the domain model and changelog should evolve together.

---

*Architecture analysis: 2026-03-21*