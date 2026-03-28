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

*Convention analysis: 2026-03-27*
