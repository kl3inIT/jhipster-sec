# Coding Conventions

**Analysis Date:** 2026-03-21

## Naming Patterns

**Files:**
- Use one top-level Java type per file with PascalCase names, for example `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`, and `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`.
- Use package names and directories as lowercase technical layers: `config`, `domain`, `repository`, `security`, `service`, `service/dto`, `service/mapper`, `web/rest`, and `web/rest/errors`.
- Use Angular kebab-case filenames with role suffixes, for example `angapp/src/main/webapp/app/entities/organization/update/organization-update.component.ts`, `angapp/src/main/webapp/app/entities/organization/service/organization.service.ts`, and `angapp/src/main/webapp/app/core/auth/account.service.ts`.
- Name backend tests with `*Test`, `*Tests`, or `*IT`, for example `src/test/java/com/vn/core/service/mapper/UserMapperTest.java` and `src/test/java/com/vn/core/web/rest/UserResourceIT.java`. Name frontend tests `*.spec.ts`, for example `angapp/src/main/webapp/app/core/auth/account.service.spec.ts`.

**Functions:**
- Use lowerCamelCase verbs for methods and helpers, for example `registerUser`, `completePasswordReset`, `partialUpdate`, `addOrganizationToCollectionIfMissing`, and `navigateToStoredUrl` in `src/main/java/com/vn/core/service/UserService.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`, and `angapp/src/main/webapp/app/core/auth/account.service.ts`.
- Keep boolean-returning methods phrased as predicates, for example `isAuthenticated`, `hasAnyAuthority`, and `compareOrganization` in `angapp/src/main/webapp/app/core/auth/account.service.ts` and `angapp/src/main/webapp/app/entities/organization/service/organization.service.ts`.

**Variables:**
- Use `UPPER_SNAKE_CASE` for constants, for example `DEFAULT_LOGIN`, `UPDATED_EMAIL`, `DETAIL_FETCH_PLAN_CODE`, and `FIELD_ERRORS_KEY` in `src/test/java/com/vn/core/web/rest/UserResourceIT.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`, and `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`.
- Use `LOG` for SLF4J loggers and descriptive lowerCamelCase names for dependencies, for example `userRepository`, `authorityRepository`, `organizationMapper`, and `applicationConfigService` in `src/main/java/com/vn/core/service/UserService.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`, and `angapp/src/main/webapp/app/core/auth/account.service.ts`.

**Types:**
- Use `DTO` suffix for service payloads, `VM` suffix for request/view models, and `Mapper` suffix for mapping components, for example `AdminUserDTO`, `OrganizationDTO`, `LoginVM`, `ManagedUserVM`, `UserMapper`, and `OrganizationMapper` in `src/main/java/com/vn/core/service/dto/`, `src/main/java/com/vn/core/web/rest/vm/`, and `angapp/src/main/java/com/mycompany/myapp/service/mapper/`.
- Use Angular `IType` and `NewType` aliases for entity models, for example `IOrganization` and `NewOrganization` in `angapp/src/main/webapp/app/entities/organization/organization.model.ts`.

## Code Style

**Formatting:**
- Use `.editorconfig` and Prettier as the baseline formatter. `.editorconfig` sets LF endings, UTF-8, trimmed trailing whitespace, 2-space indentation globally, and 4-space indentation for `*.java` in `.editorconfig`.
- Keep Java and TypeScript aligned with `.prettierrc`: `printWidth: 140`, `singleQuote: true`, `tabWidth: 2`, `arrowParens: avoid`, plus `prettier-plugin-java` and `prettier-plugin-packagejson` in `.prettierrc`.
- Preserve final newlines and avoid tabs unless a file already requires them. The repo standard is spaces in `.editorconfig`.

**Linting:**
- Root backend quality relies on Gradle JHipster quality conventions plus `checkstyle.xml`. The active custom rules are lightweight: keep HTTP URLs out of code and do not require Javadoc on every method in `checkstyle.xml`.
- `angapp/` enforces stricter frontend linting through `angapp/eslint.config.mjs`. Follow explicit return types, selector naming (`jhi-...` elements and `jhi...` attributes), `eqeqeq`, `curly`, `prefer-nullish-coalescing`, `prefer-optional-chain`, and no `console` except `warn` and `error`.
- `angapp/tsconfig.json` enables strict TypeScript, strict Angular templates, `noImplicitReturns`, and `strictNullChecks`. New frontend code should compile under that baseline.
- `frontend/` UI must be PrimeNG-first: use official `primeng.org` components and current examples for the installed major version (`primeng` 21.x in `frontend/package.json`) whenever a suitable component exists. Custom UI is allowed only when PrimeNG has no suitable component or as thin composition around PrimeNG primitives.

## Import Organization

**Order:**
1. `package` declaration, then grouped Java imports with blank lines between logical groups, as seen in `src/main/java/com/vn/core/service/UserService.java` and `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`.
2. For Java tests, place static imports first, then project imports, then JDK and third-party imports, as seen in `src/test/java/com/vn/core/web/rest/UserResourceIT.java` and `src/test/java/com/vn/core/TechnicalStructureTest.java`.
3. For Angular, import framework packages first, then RxJS, then aliased app imports (`app/...`), then relative imports, as seen in `angapp/src/main/webapp/app/core/auth/account.service.ts` and `angapp/src/main/webapp/app/entities/organization/service/organization.service.ts`.

**Path Aliases:**
- Use the Angular `baseUrl` in `angapp/tsconfig.json` and prefer `app/...` imports instead of long relative paths, for example `app/core/auth/account.model` and `app/core/config/application-config.service`.
- Use relative imports only for same-feature siblings, for example `../organization.model` and `./organization-form.service` in `angapp/src/main/webapp/app/entities/organization/`.

## Layering Conventions

**Backend Layers:**
- Keep the root service aligned with the ArchUnit rule in `src/test/java/com/vn/core/TechnicalStructureTest.java`: `web` may depend on `service` and `config`, `service` may depend on `repository` and `security`, and `domain` stays at the bottom.
- Keep the same shape in `angapp/` using `angapp/src/test/java/com/mycompany/myapp/TechnicalStructureTest.java`.
- Put REST entry points in `web/rest`, business logic in `service` or `service/impl`, persistence in `repository`, and transport types in `service/dto` or `web/rest/vm`. Do not reach from controllers directly into unrelated layers when a service already exists.

**Frontend Features:**
- Group Angular code by feature under `angapp/src/main/webapp/app/`, for example `core/`, `shared/`, `admin/`, and `entities/organization/`.
- Keep feature-local models, routes, services, forms, components, and samples together, as shown by `angapp/src/main/webapp/app/entities/organization/organization.model.ts`, `organization.routes.ts`, `service/organization.service.ts`, `update/organization-form.service.ts`, and `organization.test-samples.ts`.

## Error Handling

**Patterns:**
- Validate request payloads at the boundary with `@Valid` and field constraints on DTOs/VMs/entities, as shown in `src/main/java/com/vn/core/web/rest/UserResource.java`, `src/main/java/com/vn/core/web/rest/AccountResource.java`, `src/main/java/com/vn/core/service/dto/AdminUserDTO.java`, and `src/main/java/com/vn/core/web/rest/vm/LoginVM.java`.
- Throw domain-specific exceptions from services and controllers instead of ad hoc status handling. Examples: `UsernameAlreadyUsedException`, `EmailAlreadyUsedException`, `InvalidPasswordException`, and `BadRequestAlertException` in `src/main/java/com/vn/core/service/` and `src/main/java/com/vn/core/web/rest/errors/`.
- Let `ExceptionTranslator` convert backend failures into RFC7807 problem details with message keys, field errors, and request paths. Root implementation lives in `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`; `angapp/` mirrors it in `angapp/src/main/java/com/mycompany/myapp/web/rest/errors/ExceptionTranslator.java`.
- In `angapp/` service implementations that read secure map payloads, translate missing entities to `Optional.empty()` or `EntityNotFoundException` at the service boundary, as shown in `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`.
- On the frontend, keep save flows symmetrical: set an `isSaving` flag, subscribe with `next` and `error`, and clear state in `finalize`, as shown in `angapp/src/main/webapp/app/entities/organization/update/organization-update.component.ts`.

## Logging

**Framework:** SLF4J on the backend, no general-purpose console logging on the frontend.

**Patterns:**
- Declare a class-local `private static final Logger LOG` and log request-level or lifecycle events at `debug` or `info`, as shown in `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/JhipsterSecApp.java`, and `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`.
- Keep cross-cutting logging in dedicated infrastructure classes such as `src/main/java/com/vn/core/aop/logging/LoggingAspect.java` and `angapp/src/main/java/com/mycompany/myapp/aop/logging/LoggingAspect.java`.
- In Angular, avoid `console.log`; `angapp/eslint.config.mjs` only permits `console.warn` and `console.error`.

## Comments

**When to Comment:**
- Keep class-level Javadoc on major Spring components and tests, as shown in `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/web/rest/errors/ExceptionTranslator.java`, and `src/test/java/com/vn/core/web/rest/UserResourceIT.java`.
- Use inline comments sparingly for non-obvious runtime workarounds or generated extension hooks, for example the Hazelcast workaround in `src/main/java/com/vn/core/JhipsterSecApp.java`, test setup notes in `src/test/java/com/vn/core/web/rest/UserResourceIT.java`, and `jhipster-needle-*` placeholders in generated config classes.

**JSDoc/TSDoc:**
- Frontend code uses lightweight block comments mainly for generated type helpers, for example `angapp/src/main/webapp/app/entities/organization/update/organization-form.service.ts`. New Angular code should follow that restraint and avoid comment noise.

## Function Design

**Size:** Keep public methods narrow and push transformations into private helpers. Examples: `clearUserCaches` in `src/main/java/com/vn/core/service/UserService.java`, `toDto`, `toEmployeeLinkDto`, and `asLong` in `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`.

**Parameters:**
- Favor constructor injection with final fields in Spring classes, as seen in `src/main/java/com/vn/core/service/UserService.java` and `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`.
- Favor `inject(...)` fields instead of constructors in Angular services and components, as seen in `angapp/src/main/webapp/app/core/auth/account.service.ts` and `angapp/src/main/webapp/app/entities/organization/update/organization-update.component.ts`.

**Return Values:**
- Return `Optional<T>` for backend lookups that can miss, `Page<T>` for pagination, and `ResponseEntity<T>` from controllers. Use those shapes consistently in `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/web/rest/UserResource.java`, and `angapp/src/main/java/com/mycompany/myapp/service/OrganizationService.java`.
- Return `Observable<HttpResponse<T>>` from Angular API services and plain model values from form services, as shown in `angapp/src/main/webapp/app/entities/organization/service/organization.service.ts` and `angapp/src/main/webapp/app/entities/organization/update/organization-form.service.ts`.

## Mapping Patterns

**Backend DTO Mapping:**
- Root user mapping is hand-written and registered as a Spring service in `src/main/java/com/vn/core/service/mapper/UserMapper.java`. Follow that pattern only where generated MapStruct support is intentionally avoided.
- `angapp/` prefers MapStruct interfaces extending `EntityMapper`, for example `angapp/src/main/java/com/mycompany/myapp/service/mapper/OrganizationMapper.java`, `DepartmentMapper.java`, and `EmployeeMapper.java`.
- When partial updates are required, add an explicit `partialUpdate(@MappingTarget ...)` contract as in `angapp/src/main/java/com/mycompany/myapp/service/mapper/OrganizationMapper.java`.

**Secure View Mapping:**
- `angapp/` also contains manual map-to-DTO shaping for secured fetch-plan results in `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`. Keep those conversions private to the service that owns the secure payload format.

## Configuration Practices

**Backend Configuration:**
- Bind application-specific settings through typed `@ConfigurationProperties` classes such as `src/main/java/com/vn/core/config/ApplicationProperties.java` and `angapp/src/main/java/com/mycompany/myapp/config/ApplicationProperties.java` instead of scattering `@Value` fields.
- Keep profile-specific runtime config in YAML under `src/main/resources/config/` and `angapp/src/main/resources/config/`, with test overrides under `src/test/resources/config/application.yml` and `angapp/src/test/resources/config/application.yml`.
- Keep generated extension markers like `jhipster-needle-*` intact in files such as `src/main/java/com/vn/core/config/ApplicationProperties.java` and `build.gradle`.

**Frontend Configuration:**
- Resolve API URLs through `ApplicationConfigService` in `angapp/src/main/webapp/app/core/config/application-config.service.ts`. Do not hardcode raw endpoint prefixes in feature services.
- Centralize compiler and lint behavior in `angapp/tsconfig.json`, `angapp/tsconfig.spec.json`, `angapp/eslint.config.mjs`, and `.prettierrc`.

## Module Design

**Exports:** Keep Java APIs explicit through class and interface names, and keep Angular exports explicit per feature file. Shared barrels are limited to utility areas such as `angapp/src/main/webapp/app/shared/sort/index.ts` and `angapp/src/main/webapp/app/shared/date/index.ts`.

**Barrel Files:** Use barrels only in shared frontend utility folders. Feature folders such as `angapp/src/main/webapp/app/entities/organization/` import concrete files directly.

---

*Convention analysis: 2026-03-21*
