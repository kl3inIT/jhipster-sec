<!-- GSD:project-start source:PROJECT.md -->

## Project

**JHipster Security Platform**

This is a brownfield JHipster security-platform migration that already shipped a standalone Angular frontend and merged security runtime in `v1.0`. `v1.1` carried a security-core realignment in Phase 08.3, then continues with enterprise UX, performance, and regression hardening in Phases 9 and 10.

**Core Value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, authority, and attribute-level access decisions.

### Constraints

- **Compatibility**: Preserve the functional security capabilities already working in `angapp` for entity CRUD checks, attribute permissions, secure merge behavior, and fetch-plan-driven secure reads. Existing row-policy code is not a preservation target; Phase 08.3 retired it.
- **Frontend structure**: The new UI must live in a standalone `frontend/` app modeled after `aef-main/aef-main`; `aef-main/aef-main` is the canonical frontend reference and PrimeNG Sakai is the canonical layout or component shell, with JHipster-style Angular structure layered on top.
- **PrimeNG-first UI**: Frontend work must use official PrimeNG components and current `https://primeng.org/` examples or best practices whenever a suitable component exists; custom UI is allowed only for layout composition or gaps where PrimeNG has no suitable component.
- **Fetch plans**: Fetch plans must be defined in YAML or code builders only. Database storage for fetch-plan definitions is not allowed.
- **Brownfield safety**: Existing authentication, account, admin-user, and mail flows in the current backend must not regress during the migration.
- **API boundary**: Existing JHipster account or user APIs may keep minimal boundary request or response models where dropping them would destabilize the public contract or validation model.
- **Migration source**: Required frontend support files should come from `angapp/` rather than ad hoc reinvention when a compatible donor implementation exists.
- **Enterprise UX**: Admin and entity screens should move toward a Jmix-style master-detail experience.
- **Performance**: Dynamic navigation and richer UI cannot rely on excessive API chatter or eager bundle loading.
<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->

# Technology Stack

**Analysis Date:** 2026-03-27

## Languages

**Primary:**

- Java 25 - backend application, security, persistence, and REST code live under `src/main/java/com/vn/core/**`; backend tests live under `src/test/java/com/vn/core/**`; the toolchain is enforced in `build.gradle`.
- TypeScript 5.9.x - the standalone Angular frontend lives under `frontend/src/**`; the compiler baseline is set in `frontend/tsconfig.json`, and the package versions are pinned by `frontend/package.json` and `frontend/package-lock.json`.

**Secondary:**

- Groovy (Gradle DSL) - build logic and plugin conventions live in `build.gradle`, `gradle/*.gradle`, and `buildSrc/src/main/groovy/*.gradle`.
- YAML - runtime config, Docker Compose manifests, Liquibase metadata, and fetch-plan definitions live in `src/main/resources/config/*.yml`, `src/main/resources/fetch-plans.yml`, and `src/main/docker/*.yml`.
- JSON - package manifests, generator metadata, Sonar config, and frontend i18n bundles live in `package.json`, `frontend/package.json`, `.yo-rc.json`, `sonar-project.properties`, and `frontend/public/i18n/*.json`.
- SCSS/CSS/HTML - Angular styling lives in `frontend/src/assets/**/*.scss` and `frontend/src/assets/tailwind.css`; server-rendered mail templates live in `src/main/resources/templates/mail/*.html`.

## Runtime

**Environment:**

- JVM: Java 25, enforced by `sourceCompatibility`, `targetCompatibility`, and `assert System.properties["java.specification.version"] == "25"` in `build.gradle`.
- Browser runtime: the frontend SPA bootstraps from `frontend/src/main.ts` using providers from `frontend/src/app.config.ts`.
- Node.js: root tooling requires `>=24.14.0` in `package.json`; the frontend package manager is pinned to `npm@11.9.0` in `frontend/package.json`.

**Package Manager:**

- Gradle Wrapper 9.4.0 - pinned in `gradle/wrapper/gradle-wrapper.properties`.
- npm - used for root developer scripts in `package.json` and frontend scripts in `frontend/package.json`.
- Lockfile: `package-lock.json` is present at repo root and `frontend/package-lock.json` is present for the Angular app.

## Frameworks

**Core:**

- Spring Boot 4.0.3 - backend application framework, declared in `gradle/libs.versions.toml` and bootstrapped from `src/main/java/com/vn/core/JhipsterSecApp.java`.
- JHipster Framework 9.0.0 - backend conventions and config helpers, declared in `gradle/libs.versions.toml`; generator metadata lives in `.yo-rc.json`.
- Spring Security OAuth2 Resource Server + Nimbus JWT - stateless auth stack wired in `src/main/java/com/vn/core/config/SecurityConfiguration.java`, `src/main/java/com/vn/core/config/SecurityJwtConfiguration.java`, and `src/main/java/com/vn/core/web/rest/AuthenticateController.java`.
- Spring Data JPA + Hibernate ORM - persistence stack configured in `build.gradle`, `src/main/java/com/vn/core/config/DatabaseConfiguration.java`, and `src/main/resources/config/application.yml`.
- Liquibase 5.0.1 - schema migration framework configured in `gradle/liquibase.gradle`, `src/main/java/com/vn/core/config/LiquibaseConfiguration.java`, and `src/main/resources/config/liquibase/master.xml`.
- Hazelcast 5.5.0 - embedded cache and Hibernate second-level cache configured in `src/main/java/com/vn/core/config/CacheConfiguration.java` and `src/main/resources/config/application.yml`.
- Angular 21.2.x - standalone frontend application configured in `frontend/angular.json`, `frontend/tsconfig.json`, and `frontend/src/main.ts`.
- PrimeNG 21.1.x + PrimeFlex + PrimeIcons + Aura theme with Sakai shell patterns - frontend UI stack declared in `frontend/package.json` and provided from `frontend/src/app.config.ts`, `frontend/src/assets/styles.scss`, and the Sakai-style shell components under `frontend/src/app/layout/**`.
- `@ngx-translate` 17.x - static i18n loading is configured in `frontend/src/app/config/translation.config.ts`; merged language bundles are generated by `frontend/scripts/merge-i18n.cjs` into `frontend/public/i18n/*.json`.

**Testing:**

- JUnit Platform + Spring Boot Test + Spring Security Test - backend test stack declared in `build.gradle` and wired via `gradle/spring-boot.gradle`.
- Testcontainers PostgreSQL 18.3 - integration database support lives in `src/test/java/com/vn/core/config/DatabaseTestcontainer.java`.
- ArchUnit 1.4.1 - backend architecture tests are declared in `gradle/libs.versions.toml`.
- Angular unit testing via `@angular/build:unit-test` with Vitest 4.x and jsdom - configured by `frontend/package.json` and `frontend/angular.json`.
- Playwright 1.58.x - frontend browser tests are configured in `frontend/playwright.config.ts`.

**Build/Dev:**

- Angular CLI / `@angular/build` 21.2.x - SPA build and dev server stack configured in `frontend/angular.json` and `frontend/package.json`.
- Jib 3.5.3 - OCI image build plugin configured in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
- JaCoCo 0.8.14, SonarQube plugin 7.2.3.7755, Spotless 8.3.0, Modernizer 1.12.0, NoHTTP 0.0.11, and Checkstyle 13.3.0 - backend quality stack configured in `buildSrc/gradle/libs.versions.toml` and `buildSrc/src/main/groovy/jhipster.code-quality-conventions.gradle`.
- Prettier 3.8.1, `prettier-plugin-java` 2.8.1, `prettier-plugin-packagejson` 3.0.2, Husky 9.1.7, and lint-staged 16.3.3 - repo formatting and hook tooling configured in `package.json`, `.prettierrc`, and `.lintstagedrc.cjs`.
- Tailwind CSS 3.4.x + PostCSS + Autoprefixer - frontend styling pipeline configured in `frontend/package.json`, `frontend/tailwind.config.js`, and `frontend/src/assets/tailwind.css`.

## Key Dependencies

**Critical:**

- `tech.jhipster:jhipster-framework` 9.0.0 - JHipster runtime support used across `src/main/java/com/vn/core/config/**`.
- `org.springframework.boot:*` starters - HTTP, security, JPA, mail, validation, actuator, and cache stack defined in `build.gradle`.
- `org.postgresql:postgresql` - primary SQL driver for runtime and Liquibase, declared in `build.gradle` and `gradle/liquibase.gradle`.
- `org.liquibase:liquibase-core` - schema migration engine used by `gradle/liquibase.gradle` and `src/main/resources/config/liquibase/master.xml`.
- `com.hazelcast:hazelcast-spring` 5.5.0 and `com.hazelcast:hazelcast-hibernate53` 5.2.0 - caching infrastructure configured in `build.gradle` and `src/main/java/com/vn/core/config/CacheConfiguration.java`.
- `org.mapstruct:mapstruct` 1.6.3 - mapper generation used in `src/main/java/com/vn/core/service/mapper/**`.
- `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` - YAML parsing support used by `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java`.
- `primeng`, `@primeuix/themes`, `@ngx-translate/core`, and `dayjs` - frontend UI, i18n, and date handling dependencies declared in `frontend/package.json` and used across `frontend/src/app/**`.

**Infrastructure:**

- `io.micrometer:micrometer-registry-prometheus-simpleclient` - Prometheus metrics export enabled from `build.gradle` and `src/main/resources/config/application.yml`.
- `org.testcontainers:*` - backend integration test infrastructure declared in `build.gradle` and used in `src/test/java/com/vn/core/config/DatabaseTestcontainer.java`.
- `@angular/build` and `@angular/cli` - frontend build and dev server dependencies declared in `frontend/package.json`.
- `@playwright/test` - frontend E2E automation dependency declared in `frontend/package.json`.

## Configuration

**Environment:**

- Backend runtime config is layered through `src/main/resources/config/application.yml`, `src/main/resources/config/application-dev.yml`, `src/main/resources/config/application-prod.yml`, `src/main/resources/config/application-secret-samples.yml`, and `src/main/resources/config/application-tls.yml`.
- Application-specific fetch plans are configured through `application.fetch-plans.config` in `src/main/resources/config/application.yml` and resolved from `src/main/resources/fetch-plans.yml` by `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java`.
- Frontend environment and dev-server config live in `frontend/src/environments/*.ts`, `frontend/angular.json`, and `frontend/proxy.conf.json`.
- Generator metadata in `.yo-rc.json` still marks the root JHipster app as `skipClient`; the active SPA is the separate `frontend/` application bootstrapped outside the root Gradle build.
- Additional reference and migration codebases are present in `angapp/`, `jhipter-angular/`, and `aef-main/`, but `settings.gradle` only includes the root backend, and no root build file wires those trees into the active deliverable.
- `aef-main/aef-main/` is the canonical frontend reference for `frontend/`, and its PrimeNG Sakai shell/layout/component patterns are the expected baseline when extending the active SPA.

**Build:**

- Primary backend build entrypoints are `build.gradle`, `settings.gradle`, `gradle/spring-boot.gradle`, `gradle/liquibase.gradle`, and `buildSrc/src/main/groovy/*.gradle`.
- Primary frontend build entrypoints are `frontend/package.json`, `frontend/angular.json`, `frontend/tsconfig.json`, `frontend/playwright.config.ts`, and `frontend/scripts/merge-i18n.cjs`.
- Docker and runtime manifests live in `src/main/docker/*.yml`, with Jib entrypoint assets in `src/main/docker/jib/`.
- Sensitive runtime values are currently tracked in files such as `src/main/resources/config/application-dev.yml`, `src/main/resources/config/application-prod.yml`, `src/main/resources/config/application-secret-samples.yml`, `src/main/resources/config/application-tls.yml`, `gradle/liquibase.gradle`, and selected `src/main/docker/*.yml`; no `.env*` files were detected at repo root.

## Platform Requirements

**Development:**

- JDK 25 is required by `build.gradle`.
- Gradle 9.4.0 is supplied by `gradle/wrapper/gradle-wrapper.properties`.
- Node.js `>=24.14.0` is required by root tooling in `package.json`; the frontend toolchain resolved in `frontend/package-lock.json` also requires a modern Node runtime.
- Docker Compose is needed for local PostgreSQL, monitoring, SonarQube, JHipster Control Center, and full app stacks defined in `src/main/docker/*.yml`.
- Local frontend development uses `ng serve` from `frontend/package.json` on port `4200` and proxies backend traffic to `http://localhost:8080` via `frontend/proxy.conf.json`.

**Production:**

- Deploy the backend as a Spring Boot jar/war from `package.json` and `README.md`, or as the Jib-built OCI image from `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
- The backend expects PostgreSQL connectivity plus a JWT base64 secret, with optional SMTP, Logstash, and TLS config supplied via Spring property overrides for keys defined in `src/main/resources/config/application-*.yml`.
- The Jib image exposes `8080` and `5701/udp` as configured in `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`.
- The Angular frontend is a separate deployable built from `frontend/` and can run behind the backend or any static host that forwards API traffic to the Spring service.

---

_Stack analysis: 2026-03-27_

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:codebase/CONVENTIONS.md -->

# Coding Conventions

**Analysis Date:** 2026-03-27

## Naming Patterns

**Files:**

- Use one top-level Java type per file with PascalCase names under `src/main/java/com/vn/core/**`, for example `src/main/java/com/vn/core/service/UserService.java` and `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java`.
- Name backend tests with `*Test.java`, `*IT.java`, or purpose-built annotation suffixes such as `src/test/java/com/vn/core/security/jwt/AuthenticationIntegrationTest.java`.
- Use kebab-case Angular filenames in `frontend/src/app/**` with role suffixes such as `.component.ts`, `.service.ts`, `.model.ts`, `.routes.ts`, and `.spec.ts`, for example `frontend/src/app/pages/entities/organization/update/organization-update.component.ts`.
- Keep route-loaded Angular components as default exports in feature folders, for example `frontend/src/app/pages/entities/organization/list/organization-list.component.ts` and `frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts`.
- Keep thin route re-export wrappers where needed, such as `frontend/src/app/app.routes.ts`.

**Functions:**

- Use lowerCamelCase verbs for Java and TypeScript methods, for example `registerUser`, `completePasswordReset`, `queryBackend`, and `navigateToWorkspaceList`.
- Phrase boolean helpers and computed UI state as predicates, for example `hasAnyAuthority`, `isAuthenticated`, `canEditAttribute`, `showListDeniedState`, and `capabilityLoaded`.

**Variables:**

- Use `private static final Logger LOG` for backend loggers, as in `src/main/java/com/vn/core/service/UserService.java` and `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java`.
- Use `UPPER_SNAKE_CASE` for shared constants in Java and TypeScript, for example `FIELD_ERRORS_KEY`, `ITEMS_PER_PAGE`, and `TOTAL_COUNT_RESPONSE_HEADER`.
- In `frontend/`, keep injected collaborators in `readonly` fields and mutable view state in `signal(...)` or `computed(...)`, as in `frontend/src/app/pages/entities/organization/list/organization-list.component.ts`.

**Types:**

- Use `DTO`, `VM`, `Mapper`, and `Service` suffixes on backend transport and mapping types, for example `AdminUserDTO`, `LoginVM`, `SecPermissionMapper`, and `OrganizationService`.
- Use `IType` interfaces and `NewType` aliases for Angular entity models, for example `IOrganization` and `NewOrganization` in `frontend/src/app/pages/entities/organization/organization.model.ts`.
- Keep request and response aliases close to the API service that owns them, for example `EntityResponseType` and `EntityArrayResponseType` in `frontend/src/app/pages/entities/organization/service/organization.service.ts`.

## Code Style

**Formatting:**

- Root formatting is driven by `.editorconfig`, `.prettierrc`, `.lintstagedrc.cjs`, and `buildSrc/src/main/groovy/jhipster.code-quality-conventions.gradle`.
- Keep Java at 4 spaces and other tracked text files at 2 spaces per `.editorconfig`.
- Root Prettier uses `printWidth: 140`, single quotes, no tabs, and `prettier-plugin-java` in `.prettierrc`.
- `frontend/` has its own `.editorconfig` and `.prettierrc`; keep TS, SCSS, and HTML at 2 spaces, single quotes, and `printWidth: 100`.
- Angular templates in `frontend/` are formatted with the Angular parser via `frontend/.prettierrc`.

**Linting:**

- Backend quality gates run through Checkstyle, NoHTTP, Spotless, Modernizer, JaCoCo, and Sonar as wired in `checkstyle.xml`, `buildSrc/src/main/groovy/jhipster.code-quality-conventions.gradle`, and `sonar-project.properties`.
- `checkstyle.xml` currently enforces NoHTTP scanning and relaxes the missing-method-Javadoc rule; do not assume a large custom Checkstyle rule set exists.
- The active `frontend/` app does not define a repo-local ESLint config or `lint` script in `frontend/package.json`; current safety comes from strict TypeScript compilation and tests.
- `angapp/` still carries a stricter Angular ESLint baseline in `angapp/eslint.config.mjs`. Use it as a migration reference when porting screens from `angapp/`, not as an automatically enforced rule set for `frontend/`.

## Import Organization

**Order:**

1. Put static imports first in Java tests, as seen in `src/test/java/com/vn/core/web/rest/UserResourceIT.java` and `src/test/java/com/vn/core/security/data/SecureDataManagerImplTest.java`.
2. Group framework imports ahead of project-local imports in TypeScript, then separate `app/...` aliases from same-feature relative imports with a blank line, as in `frontend/src/app/core/auth/account.service.ts`.
3. In Java source files, keep project-local imports near the top, then Jakarta and JDK imports, then third-party frameworks, matching files such as `src/main/java/com/vn/core/web/rest/UserResource.java`.

**Path Aliases:**

- Use `app/*`, `@/*`, and `environments/*` from `frontend/tsconfig.json` for cross-feature imports.
- Use relative imports only for same-feature siblings, for example `../organization.model` and `./organization-list.component`.
- Legacy `angapp/tsconfig.json` keeps `baseUrl: src/main/webapp/`; keep that only when editing `angapp/`.

## Layering Conventions

- Keep backend package boundaries aligned with `src/test/java/com/vn/core/TechnicalStructureTest.java`.
- `src/main/java/com/vn/core/web/**` owns HTTP transport, validation, and status translation. Controllers should delegate business rules rather than assemble security logic inline.
- `src/main/java/com/vn/core/service/**` owns transactions, DTO assembly, and application workflows, as in `src/main/java/com/vn/core/service/UserService.java` and `src/main/java/com/vn/core/service/OrganizationService.java`.
- `src/main/java/com/vn/core/security/**` owns permission evaluation, secure data access, row policies, fetch plans, and bridge code. Keep new security rules in this tree, not in generic services.
- `src/main/java/com/vn/core/repository/**` stays focused on persistence and query methods. Business branching belongs above it.
- `src/main/java/com/vn/core/domain/**` and `src/main/java/com/vn/core/security/domain/**` stay as data definitions and should not absorb controller or service behavior.
- `frontend/src/app/core/` holds app-wide concerns such as auth, config, interceptors, and request helpers.
- `frontend/src/app/layout/` owns shell structure, navigation registry, menu trees, breadcrumbs, and layout services. Keep this aligned with the Sakai-style shell pattern established by `aef-main/aef-main/src/app/layout/**` and already adapted in `frontend/src/app/layout/**`.
- `frontend/src/app/pages/` owns business screens. Keep feature folders self-contained with `model`, `service`, `list`, `detail`, `update`, and `routes` siblings, as in `frontend/src/app/pages/entities/organization/` and `frontend/src/app/pages/admin/user-management/`.
- Put route guards, capability resolvers, and navigation metadata in route files such as `frontend/src/app/pages/entities/organization/organization.routes.ts`, not inside component constructors.
- Prefer official PrimeNG components and the existing Sakai surface patterns for page composition; do not introduce a parallel layout/component system when the current `frontend/` shell or `aef-main/aef-main/` reference already covers the need.

## Error Handling

**Patterns:**

- Validate request payloads at the HTTP boundary with `@Valid`, `@Pattern`, and typed DTO or VM classes, as in `src/main/java/com/vn/core/web/rest/UserResource.java` and `src/main/java/com/vn/core/web/rest/vm/LoginVM.java`.
- Throw typed backend exceptions instead of ad hoc status branching, then let `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java` map them into RFC7807 problem responses.
- Use `Optional<T>` plus `ResponseUtil.wrapOrNotFound(...)` for lookup endpoints, as in `src/main/java/com/vn/core/web/rest/UserResource.java`.
- Fail closed in security services. The tests in `src/test/java/com/vn/core/security/data/SecureDataManagerImplTest.java` show denied CRUD and unsupported secured JPQL becoming exceptions rather than soft fallbacks.
- Centralize Angular toast-friendly error handling through `frontend/src/app/shared/error/http-error.utils.ts` instead of repeating message mapping in each component.
- Keep save flows symmetrical with `isSaving` state and `finalize(...)`, as in `frontend/src/app/pages/entities/organization/update/organization-update.component.ts` and `frontend/src/app/pages/admin/user-management/update/user-management-update.component.ts`.

## Logging

**Framework:** SLF4J on the backend, PrimeNG `MessageService` and translation-backed toast helpers on the active frontend.

**Patterns:**

- Declare backend loggers as `private static final Logger LOG`, for example in `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/service/OrganizationService.java`, and `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java`.
- Let `src/main/java/com/vn/core/aop/logging/LoggingAspect.java` handle cross-cutting entry, exit, and exception logging for repositories, services, and REST controllers.
- Keep controller and service logs at request or lifecycle granularity. The codebase favors `LOG.debug(...)` over verbose info logs.
- On the frontend, surface user-visible failures with `addTranslatedMessage(...)` and `handleHttpError(...)` from `frontend/src/app/shared/error/http-error.utils.ts`.
- Avoid `console.log` in application code. Existing `frontend/` files already prefer silent catch blocks or injected services over browser-console output.

## Comments

**When to Comment:**

- Keep class-level Javadoc on Spring services, controllers, mappers, and tests, as seen in `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`, and `src/test/java/com/vn/core/service/mapper/UserMapperTest.java`.
- Use block comments to explain non-obvious contracts, especially around security and e2e setup, as in `frontend/e2e/proof-role-gating.spec.ts` and `frontend/e2e/security-comprehensive.spec.ts`.
- Keep inline comments for permission-gated fields, generated extension points, or migration markers only, for example `frontend/src/app/pages/entities/organization/organization.model.ts` and `src/main/java/com/vn/core/config/ApplicationProperties.java`.

**JSDoc/TSDoc:**

- Backend favors Javadoc on public classes and selected methods.
- Active `frontend/` uses very little TSDoc. Prefer expressive names and short inline comments over large docblocks.

## Function Design

**Size:** Keep controllers thin, lift repeated business rules into services, and extract reusable private helpers for permission math, route parsing, and query shaping.

**Parameters:**

- Backend service and controller methods prefer typed DTOs, `Pageable`, `Optional`, and `Map<String, Object>` only at explicit dynamic-security boundaries such as `src/main/java/com/vn/core/service/OrganizationService.java`.
- Frontend services should prefer typed request models. A few generated-style methods still accept `req?: any` in `frontend/src/app/pages/entities/organization/service/organization.service.ts`; newer code in `frontend/src/app/pages/admin/user-management/service/user-management.service.ts` shows the preferred typed approach.
- Favor `inject(...)` fields in Angular services and components, as seen throughout `frontend/src/app/core/**` and `frontend/src/app/pages/**`.

**Return Values:**

- Backend queries return `Optional<T>`, `Page<T>`, or `ResponseEntity<T>`.
- Frontend API services return `Observable<HttpResponse<T>>` or `Observable<T>`.
- Keep UI state in `signal(...)` and `computed(...)` instead of mutable booleans where possible, as in `frontend/src/app/pages/entities/organization/list/organization-list.component.ts`.

## Mapping Patterns

- Keep manual mapping where entity exposure is special-cased, especially `src/main/java/com/vn/core/service/mapper/UserMapper.java`.
- Use `src/main/java/com/vn/core/service/mapper/EntityMapper.java` as the base contract for MapStruct mappers that support two-way conversion plus `partialUpdate`.
- Security admin DTO mapping lives under `src/main/java/com/vn/core/service/mapper/security/`, currently centered on `SecPermissionMapper.java`.
- When type translation is non-trivial, encode it directly in the mapper interface with explicit `@Mapping(...)` expressions, as in `src/main/java/com/vn/core/service/mapper/security/SecPermissionMapper.java`.
- On the frontend, keep transport and form models close to the feature they serve: `frontend/src/app/pages/entities/organization/organization.model.ts`, `frontend/src/app/pages/entities/organization/service/organization.service.ts`, and `frontend/src/app/pages/admin/user-management/update/user-management-form.service.ts`.

## Configuration Practices

- Bind backend application settings through typed properties classes such as `src/main/java/com/vn/core/config/ApplicationProperties.java`; avoid scattering raw `@Value` lookups beyond framework-boundary cases like `applicationName`.
- Keep runtime YAML under `src/main/resources/config/*.yml` and test overrides under `src/test/resources/config/*.yml`.
- Treat `src/main/resources/config/application-secret-samples.yml`, `src/main/resources/config/application-tls.yml`, and JWT-bearing test configs under `src/test/resources/config/` and `src/test/java/com/vn/core/security/jwt/AuthenticationIntegrationTest.java` as sensitive locations. Mention the files, not the secret values.
- Keep fetch-plan definitions in YAML files such as `src/main/resources/fetch-plans.yml` and `src/test/resources/fetch-plans-test.yml`, loaded via `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java`.
- Keep frontend environment and provider setup centralized in `frontend/src/environments/*.ts`, `frontend/src/app/app.config.ts`, `frontend/src/app/config/translation.config.ts`, and `frontend/src/app/core/config/application-config.service.ts`.
- Set API bases through `ApplicationConfigService.getEndpointFor(...)` instead of hardcoding prefixes in feature services.
- Preserve generated or migration marker comments such as `jhipster-needle-*` when editing Gradle or configuration files.

## Module Design

**Exports:**

- Backend files usually expose one concrete class or interface each.
- Angular route-loaded components and route arrays default-export the feature artifact, for example `frontend/src/app/pages/entities/organization/organization.routes.ts` and `frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts`.
- Shared helpers and services use named exports, for example `frontend/src/app/shared/error/http-error.utils.ts` and `frontend/src/app/config/translation.config.ts`.

**Barrel Files:**

- Barrel usage is intentionally light. The main example is `frontend/src/app/core/interceptor/index.ts`.
- Prefer direct imports over broad barrels so feature ownership stays obvious.

## Frontend Conventions

- The active Angular app lives in `frontend/`. Treat `angapp/` as a legacy reference workspace, not the primary implementation target.
- Keep route-loaded feature components standalone and colocated with their templates. Current files still declare `standalone: true` explicitly, for example `frontend/src/app/pages/entities/organization/list/organization-list.component.ts` and `frontend/src/app/pages/entities/organization/update/organization-update.component.ts`. Match the local file style when editing nearby code.
- Prefer lazy `loadComponent` routes with route metadata and resolvers, as in `frontend/src/app/pages/entities/organization/organization.routes.ts` and `frontend/src/app/pages/admin/security/security.routes.ts`.
- Use signals for UI state and permission gates, not ad hoc component-level subjects.
- Keep shell behavior in `layout/` and business workflows in `pages/`.
- Use translation keys, not hard-coded user-visible strings, for page titles, breadcrumbs, and toasts. Examples live in `frontend/src/app/config/translation.config.ts`, `frontend/src/app/layout/navigation/navigation.service.ts`, and the `*.component.html` files under `frontend/src/app/pages/`.

---

_Convention analysis: 2026-03-27_

<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:codebase/ARCHITECTURE.md -->

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

- Purpose: Enforce entity CRUD permissions, attribute permissions, fetch-plan-driven reads, secure merge behavior, explicit secured-JSON validation, and authority-aware menu visibility.
- Location: `src/main/java/com/vn/core/security/`
- Contains: access constraints in `src/main/java/com/vn/core/security/access/`, security-context bridges in `src/main/java/com/vn/core/security/bridge/`, entity catalog classes in `src/main/java/com/vn/core/security/catalog/`, data managers in `src/main/java/com/vn/core/security/data/`, fetch-plan types in `src/main/java/com/vn/core/security/fetch/`, merge logic in `src/main/java/com/vn/core/security/merge/`, permission evaluators in `src/main/java/com/vn/core/security/permission/`, metadata repositories in `src/main/java/com/vn/core/security/repository/`, serializers in `src/main/java/com/vn/core/security/serialize/`, and secured-JSON adapters or validators in `src/main/java/com/vn/core/security/web/`.
- Depends on: JPA metamodel access, Spring Security context, business repositories, and security metadata tables.
- Used by: `OrganizationService`, `DepartmentService`, `EmployeeService`, `SecuredEntityCapabilityService`, menu-permission services, and any future secured entity feature.

**Persistence Layer:**

- Purpose: Persist both base business entities and security metadata, backed by Liquibase-managed PostgreSQL schema.
- Location: `src/main/java/com/vn/core/domain/`, `src/main/java/com/vn/core/repository/`, `src/main/java/com/vn/core/security/domain/`, `src/main/java/com/vn/core/security/repository/`, `src/main/resources/config/liquibase/`
- Contains: business entities such as `User`, `Authority`, `Organization`, `Department`, `Employee`; business repositories such as `UserRepository`, `OrganizationRepository`, `DepartmentRepository`, `EmployeeRepository`; security metadata entities such as `SecPermission`, `SecMenuDefinition`, `SecMenuPermission`; Liquibase changelogs in `src/main/resources/config/liquibase/changelog/*.xml`, including the row-policy drop cleanup.
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
3. Entity services such as `src/main/java/com/vn/core/service/OrganizationService.java` delegate typed entity mutations and secure query requests to `src/main/java/com/vn/core/security/data/SecureDataManager.java`.
4. `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java` resolves the secured entity through `src/main/java/com/vn/core/security/catalog/MetamodelSecuredEntityCatalog.java`, checks CRUD access through `src/main/java/com/vn/core/security/data/DataManagerImpl.java`, builds typed query specifications through `src/main/java/com/vn/core/security/data/SecureQuerySpecificationFactory.java`, resolves fetch plans through `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java`, enforces write-side attribute permissions through `src/main/java/com/vn/core/security/merge/SecureMergeServiceImpl.java`, and strips denied read-side attributes through `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java`.
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
5. Security metadata repositories under `src/main/java/com/vn/core/security/repository/` persist the role, permission, menu-definition, and menu-permission tables managed by `src/main/resources/config/liquibase/changelog/20260321000200_create_sec_permission.xml`, `src/main/resources/config/liquibase/changelog/20260325000100_create_sec_navigation_grant.xml`, `src/main/resources/config/liquibase/changelog/20260325000200_rename_nav_grant_add_menu_def.xml`, `src/main/resources/config/liquibase/changelog/20260325000300_repair_sec_menu_permission_schema.xml`, `src/main/resources/config/liquibase/changelog/20260327000100_convert_sec_menu_permission_app_name_to_enum.xml`, and the cleanup in `src/main/resources/config/liquibase/changelog/20260327000200_drop_sec_row_policy.xml`.

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
- Examples: `src/main/java/com/vn/core/security/domain/SecPermission.java`, `src/main/java/com/vn/core/security/domain/SecMenuDefinition.java`, `src/main/java/com/vn/core/security/domain/SecMenuPermission.java`, `src/main/java/com/vn/core/security/repository/SecPermissionRepository.java`
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
- `src/main/java/com/vn/core/security/web/SecuredEntityPayloadValidator.java` rejects malformed, unknown-field, or unsafe secured-entity payloads before they reach the data pipeline.
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

_Architecture analysis: 2026-03-27_

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

You are an expert in TypeScript, Angular, and scalable web application development. You write functional, maintainable, performant, and accessible code following Angular and TypeScript best practices.

## TypeScript Best Practices

- Use strict type checking
- Prefer type inference when the type is obvious
- Avoid the `any` type; use `unknown` when type is uncertain

## Angular Best Practices

- Always use standalone components over NgModules
- Prefer not to add explicit `standalone: true` in new Angular decorators unless the surrounding `frontend/` files already use it; match local file style when editing nearby code.
- Use signals for state management
- Implement lazy loading for feature routes
- Do NOT use the `@HostBinding` and `@HostListener` decorators. Put host bindings inside the `host` object of the `@Component` or `@Directive` decorator instead
- Use `NgOptimizedImage` for all static images.
  - `NgOptimizedImage` does not work for inline base64 images.
- Use official PrimeNG components and current `https://primeng.org/` examples whenever a suitable component exists. Custom UI is allowed only when PrimeNG has no suitable component or as thin composition around PrimeNG primitives.

## Accessibility Requirements

- It MUST pass all AXE checks.
- It MUST follow all WCAG AA minimums, including focus management, color contrast, and ARIA attributes.

### Components

- Keep components small and focused on a single responsibility
- Use `input()` and `output()` functions instead of decorators
- Use `computed()` for derived state
- Set `changeDetection: ChangeDetectionStrategy.OnPush` in `@Component` decorator
- Prefer inline templates for small components
- Prefer Reactive forms instead of Template-driven ones
- Do NOT use `ngClass`, use `class` bindings instead
- Do NOT use `ngStyle`, use `style` bindings instead
- When using external templates/styles, use paths relative to the component TS file.

## State Management

- Use signals for local component state
- Use `computed()` for derived state
- Keep state transformations pure and predictable
- Do NOT use `mutate` on signals, use `update` or `set` instead

## Templates

- Keep templates simple and avoid complex logic
- Use native control flow (`@if`, `@for`, `@switch`) instead of `*ngIf`, `*ngFor`, `*ngSwitch`
- Use the async pipe to handle observables
- Do not assume globals like (`new Date()`) are available.

## Services

- Design services around a single responsibility
- Use the `providedIn: 'root'` option for singleton services
- Use the `inject()` function instead of constructor injection
