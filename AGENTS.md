<!-- GSD:project-start source:PROJECT.md -->

## Project

**JHipster Security Platform**

This is a brownfield JHipster security-platform migration that already shipped a standalone Angular frontend and merged security runtime in `v1.0`. `v1.1` carried a security-core realignment in Phase 08.3, then continues with enterprise UX, performance, and regression hardening in Phases 9 and 10.

**Core Value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, authority, and attribute-level access decisions.

### Constraints

- **Compatibility**: Preserve functional security capabilities in `angapp` for CRUD checks, attribute permissions, secure merge, and fetch-plan-driven reads. Row-policy code is not a preservation target (retired in Phase 08.3).
- **Frontend structure**: Active UI lives in standalone `frontend/`; `aef-main/aef-main` is the canonical frontend reference (PrimeNG Sakai shell).
- **PrimeNG-first UI**: Use official PrimeNG components whenever a suitable one exists; custom UI only for layout composition or gaps.
- **Fetch plans**: Defined in YAML or code builders only — no database storage.
- **Brownfield safety**: Auth, account, admin-user, and mail flows must not regress.
- **API boundary**: Keep minimal JHipster account/user API models where dropping them would break contracts.
- **Migration source**: Source frontend support files from `angapp/` when a compatible donor exists.
- **Enterprise UX**: Admin and entity screens target Jmix-style master-detail experience.
- **Performance**: No excessive API chatter or eager bundle loading.
<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->

## Technology Stack

**Backend:** Java 25 · Spring Boot 4.0.3 · JHipster 9.0.0 · Spring Data JPA + Hibernate · Liquibase 5.0.1 · Hazelcast 5.5.0 · Stateless JWT auth · PostgreSQL · MapStruct 1.6.3 · Gradle 9.4.0
**Frontend:** Angular 21.2.x · TypeScript 5.9.x · PrimeNG 21.1.x (Aura/Sakai) · @ngx-translate 17.x · Tailwind CSS 3.4.x · Node ≥24.14.0 · npm 11.9.0
**Testing:** JUnit + Spring Boot Test + Testcontainers PostgreSQL · Vitest 4.x + jsdom · Playwright 1.58.x
**Quality:** Checkstyle · Spotless · JaCoCo · Prettier 3.8.1

**Key locations:**

- Backend source: `src/main/java/com/vn/core/**`; tests: `src/test/java/com/vn/core/**`
- Frontend source: `frontend/src/app/**`
- Config: `src/main/resources/config/application*.yml`
- Fetch plans: `src/main/resources/fetch-plans.yml`
- Liquibase changelogs: `src/main/resources/config/liquibase/changelog/`
- Reference workspaces (not active): `angapp/`, `aef-main/aef-main/`, `jhipter-angular/`

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:codebase/CONVENTIONS.md -->

## Coding Conventions

**Naming:**

- Java: PascalCase files, one type per file; `DTO`/`VM`/`Mapper`/`Service` suffixes; `private static final Logger LOG`; `UPPER_SNAKE_CASE` constants
- Angular: kebab-case files with `.component.ts`/`.service.ts`/`.model.ts`/`.routes.ts` suffixes; `IType`/`NewType` model interfaces; lowerCamelCase methods; boolean predicates (`hasAnyAuthority`, `isAuthenticated`)

**Formatting:** Java 4 spaces; TS/SCSS/HTML 2 spaces; root `printWidth: 140`, frontend `printWidth: 100`; single quotes

**Imports:** Static imports first in Java tests; framework before project-local in TS; use `app/*`/`@/*`/`environments/*` for cross-feature, relative only for same-feature siblings

**Layering:**

- `web/**` — HTTP transport only; delegate to services; `@Valid` at boundary; `ExceptionTranslator` → RFC7807
- `service/**` — transactions, DTO assembly, orchestration
- `security/**` — all permission evaluation, fetch plans, secure data access; fail closed
- `repository/**` — persistence and query only
- `frontend/core/` — auth, config, interceptors
- `frontend/layout/` — shell, navigation, Sakai patterns (reference `aef-main/aef-main/`)
- `frontend/pages/` — business screens; self-contained feature folders (`model`, `service`, `list`, `detail`, `update`, `routes`)

**Error handling:**

- Backend: typed exceptions → `ExceptionTranslator`; `Optional<T>` + `ResponseUtil.wrapOrNotFound(...)` for lookups; fail closed in security
- Frontend: `handleHttpError(...)` from `shared/error/http-error.utils.ts`; symmetrical `isSaving` + `finalize(...)`

**Logging:** `LOG.debug(...)` preferred; `LoggingAspect` for cross-cutting; no `console.log` in frontend; PrimeNG `MessageService` + `addTranslatedMessage(...)` for UI toasts

**Configuration:** Typed props via `ApplicationProperties`; fetch plans in YAML only; API bases via `ApplicationConfigService.getEndpointFor(...)`; preserve `jhipster-needle-*` markers; treat `application-secret-samples.yml` / `application-tls.yml` as sensitive

**Frontend specifics:**

- `frontend/` is active; `angapp/` is legacy reference only
- Lazy `loadComponent` routes with resolvers and metadata; guards in route files, not constructors
- Signals for UI state; translation keys for all user-visible strings
- Barrel usage light; prefer direct imports

<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:codebase/ARCHITECTURE.md -->

## Architecture

**Pattern:** Split-runtime — Spring Boot backend + standalone Angular SPA. Backend in single Gradle root module; frontend built/deployed separately from `frontend/`.

**Backend layers:**

| Layer            | Package                                                              | Responsibility                                                          |
| ---------------- | -------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| Config/Bootstrap | `config/`                                                            | App startup, JWT, Hazelcast, Liquibase                                  |
| HTTP API         | `web/rest/`                                                          | REST endpoints; delegate to services                                    |
| Service          | `service/`                                                           | Transactions, DTOs, orchestration                                       |
| Security Core    | `security/`                                                          | CRUD permissions, attribute checks, fetch plans, secure merge/serialize |
| Persistence      | `domain/`, `repository/`, `security/domain/`, `security/repository/` | JPA entities, Spring Data repos, Liquibase schema                       |

**Security pipeline (entity CRUD):** Frontend service → Resource → Service → `SecureDataManagerImpl` (catalog lookup + CRUD check + fetch-plan resolution + secure merge/serialize) → Repository

**Frontend layers:**

| Layer     | Location                   | Responsibility                                     |
| --------- | -------------------------- | -------------------------------------------------- |
| Bootstrap | `main.ts`, `app.config.ts` | Providers, auth, translations                      |
| Shell     | `layout/`                  | Sakai shell, navigation, menu visibility           |
| Features  | `pages/`                   | Business screens; entity and admin feature folders |
| Core      | `core/`                    | Auth state, interceptors, config service           |
| Shared    | `shared/`                  | Error utils, reusable UI                           |

**Key abstractions:**

- `@SecuredEntity` annotation → `MetamodelSecuredEntityCatalog` → `SecureDataManagerImpl` pipeline
- Fetch plans: YAML-defined in `fetch-plans.yml`, loaded by `YamlFetchPlanRepository`, referenced by logical codes (e.g., `organization-list`)
- Security metadata: `SecPermission`, `SecMenuDefinition`, `SecMenuPermission` in dedicated tables
- Capability/nav DTOs: backend returns simplified contracts; frontend gates routes and filters menus
- Navigation flow: `navigation.service.ts` → `/api/security/menu-permissions` → filters `navigation-registry.ts` → session cache → `user-route-access.service.ts`
- Auth: stateless JWT backend; Angular signals + replayed observable for frontend identity

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
