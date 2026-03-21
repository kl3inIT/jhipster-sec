# Technology Stack

**Project:** JHipster security platform migration
**Research type:** stack
**Researched:** 2026-03-21

## Migration Position

This repo should stay anchored on the current root backend, not on `angapp`'s older application baseline. The root service already runs the target brownfield platform stack with Java 21 in `build.gradle`, Spring Boot 4.0.3 and JHipster Framework 9.0.0 in `gradle/libs.versions.toml`, PostgreSQL/Liquibase in `src/main/resources/config/application-*.yml`, and JWT/account/admin APIs in `src/main/java/com/vn/core/web/rest/AuthenticateController.java`, `src/main/java/com/vn/core/web/rest/AccountResource.java`, and `src/main/java/com/vn/core/web/rest/UserResource.java`.

`angapp` should be treated as a security-engine donor, not as the runtime baseline. Its valuable assets are the backend security modules under `angapp/src/main/java/com/mycompany/core/**`, the YAML fetch-plan approach in `angapp/src/main/resources/fetch-plans.yml`, and the security metadata model seeded by `angapp/src/main/resources/config/liquibase/changelog/20260319000100_security_metadata.xml`. Its app-level stack is older: Java 17 in `angapp/build.gradle`, Spring Boot 3.4.5 and JHipster 8.11.0 in `angapp/gradle/libs.versions.toml`.

The new frontend should follow the standalone Angular structure shown by `aef-main/aef-main`, not the generated embedded client in `angapp/src/main/webapp/**`. `aef-main/aef-main/src/main.ts`, `aef-main/aef-main/src/app.config.ts`, and `aef-main/aef-main/src/app.routes.ts` show the right direction: standalone bootstrap, route-level lazy loading, shared auth/interceptor infrastructure, and a PrimeNG-based shell.

## Recommended Stack

### Backend Runtime

| Technology | Version | Purpose | Migration decision |
|------------|---------|---------|--------------------|
| Java | 21 | Primary backend runtime | Keep the root repo level from `build.gradle`. Do not downgrade to `angapp`'s Java 17 from `angapp/build.gradle`. |
| Spring Boot | 4.0.3 | Application runtime and dependency platform | Keep the root version from `gradle/libs.versions.toml`. Port `angapp` security code into this stack and adapt APIs where needed. |
| JHipster Framework | 9.0.0 | Security/config/runtime conventions | Keep the root version from `gradle/libs.versions.toml`. Do not rebase onto `angapp`'s JHipster 8.11.0 from `angapp/gradle/libs.versions.toml`. |
| Spring Security OAuth2 Resource Server + JWT | Root current | Authentication and bearer token validation | Keep the current JWT path implemented in `src/main/java/com/vn/core/config/SecurityConfiguration.java` and `src/main/java/com/vn/core/web/rest/AuthenticateController.java`. Extend authorization below it; do not replace it. |
| Spring Data JPA + Hibernate | Root current | Persistence, entity access, specifications | Keep. The `angapp` engine already assumes JPA repositories and specifications in `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java`. |
| PostgreSQL + Liquibase | Root current | Durable storage and schema evolution | Keep. Add security metadata tables and seeds through new root Liquibase changelogs modeled on `angapp/src/main/resources/config/liquibase/changelog/20260319000100_security_metadata.xml`. |

### Security Engine Modules To Add

These should be ported into the root project as project-native backend modules, likely under `src/main/java/com/vn/core/` with package names adapted from `com.mycompany.core`:

| Module | Source reference | Purpose | Migration decision |
|--------|------------------|---------|--------------------|
| Access manager and access contexts | `angapp/src/main/java/com/mycompany/core/security/access/AccessManagerImpl.java` and `angapp/src/main/java/com/mycompany/core/security/access/*` | Central decision pipeline for CRUD, attribute, fetch-plan, and row-level checks | Add and make it the single authorization orchestration layer for entity access. |
| Entity permission evaluation | `angapp/src/main/java/com/mycompany/core/security/permission/EntityPermissionEvaluatorImpl.java` | Entity-level CRUD allow/deny checks | Add. Keep this as a backend concern, not a frontend concern. |
| Attribute permission evaluation | `angapp/src/main/java/com/mycompany/core/security/permission/AttributePermissionEvaluatorImpl.java` | View/edit control per attribute token | Add. This is required for secure partial reads and write guards. |
| Role-permission lookup | `angapp/src/main/java/com/mycompany/core/security/permission/RolePermissionServiceDbImpl.java` | Resolve permissions from `sec_role` and `sec_permission` | Add the DB-backed implementation. Do not use `RolePermissionServiceAllowAllImpl.java` in v1 production flow. |
| Row-level policy provider | `angapp/src/main/java/com/mycompany/core/security/row/RowLevelPolicyProviderDbImpl.java` | Convert persisted row-policy metadata into JPA specifications | Add, but treat the current expression DSL as a starting point, not a finished long-term policy language. |
| Secure data manager | `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java` | Centralized secure read/list/page/save/delete flow | Add and route new security-sensitive entity services through it. This is the main behavior worth migrating from `angapp`. |
| Secure merge service | `angapp/src/main/java/com/mycompany/core/merge/SecureMergeServiceImpl.java` | Apply write payloads while enforcing attribute edit permission | Add. This prevents privilege bypass on update flows. |
| Secure serializer | `angapp/src/main/java/com/mycompany/core/serialize/SecureEntitySerializerImpl.java` | Return filtered maps honoring fetch plan plus attribute visibility | Add. This is the bridge that allows minimal DTO usage on read paths. |
| Fetch-plan repository/resolver | `angapp/src/main/java/com/mycompany/core/fetch/YamlFetchPlanRepository.java` and `angapp/src/main/java/com/mycompany/core/fetch/*` | YAML-defined fetch plans with nesting and inheritance | Add and keep YAML/code-defined plans only. |
| Repository registry | `angapp/src/main/java/com/mycompany/core/repository/SpringRepositoryRegistry.java` | Resolve typed repositories for secure generic access | Add. This avoids entity-specific branching inside the secure data manager. |

### Supporting Backend Libraries To Add

| Library | Why it is needed | Evidence | Migration decision |
|---------|------------------|----------|--------------------|
| `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` | Required to parse YAML fetch-plan definitions | Present in `angapp/build.gradle`; used by `angapp/src/main/java/com/mycompany/core/fetch/YamlFetchPlanRepository.java` | Add to the root `build.gradle`. |
| Spring Data JPA Specifications | Required for row-level policy composition and secure filtered queries | Used in `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java` and `angapp/src/main/java/com/mycompany/core/security/row/RowLevelPolicyProviderDbImpl.java` | Already available through the root `spring-boot-starter-data-jpa`; keep using it. |
| MapStruct | Still useful for boundary models that remain | Used in both stacks via `build.gradle` and `angapp/build.gradle` | Keep root MapStruct for account/admin DTOs and for any transitional write DTOs. |
| Spring Security Data | Required for security-aware repository patterns already present in both apps | Present in root `build.gradle` and `angapp/build.gradle` | Keep. |

### Frontend Runtime For `frontend/`

| Technology | Version | Purpose | Migration decision |
|------------|---------|---------|--------------------|
| Angular | 21.x | Standalone frontend app runtime | Use the `aef-main/aef-main/package.json` baseline, not `angapp/package.json`'s Angular 19 line. |
| Angular standalone bootstrap | `bootstrapApplication` pattern | App startup without old root NgModule shell | Use the structure from `aef-main/aef-main/src/main.ts` and `aef-main/aef-main/src/app.config.ts`. |
| PrimeNG + PrimeIcons + PrimeFlex + PrimeUIX theme | 21.x family in `aef-main/aef-main/package.json` | Main UI component system and layout theme | Use this for v1. It matches the requested Sakai-style direction better than `angapp`'s Bootstrap UI stack. |
| `@ngx-translate/core` | 17.x | i18n loading and partial translation composition | Keep the `aef-main/aef-main/src/app/shared/language/translation.module.ts` approach. |
| `@ng-bootstrap/ng-bootstrap` | 20.x | Pragmatic reuse for JHipster account/shared pieces | Keep where already present in the reference structure, but PrimeNG should be the main widget library. |
| Tailwind/PostCSS | Current `aef-main/aef-main/package.json` and `angular.json` | Utility styling layered under PrimeNG/Sakai assets | Keep as support tooling, not as the primary design language. |
| Vitest | `aef-main/aef-main/package.json` | Frontend unit tests | Use for the new standalone frontend instead of carrying over `angapp`'s Jest builder setup. |

## Prescriptive v1 Choices

### Keep

- Keep the root backend as the deployment unit and package root: `build.gradle`, `settings.gradle`, `src/main/java/com/vn/core/**`, and `src/main/resources/config/**`.
- Keep the current JWT authentication contract and account/admin APIs exposed by `src/main/java/com/vn/core/web/rest/AuthenticateController.java`, `src/main/java/com/vn/core/web/rest/AccountResource.java`, and `src/main/java/com/vn/core/web/rest/UserResource.java`.
- Keep PostgreSQL, Liquibase, JPA/Hibernate, and the existing JHipster operational wiring already documented in `.planning/codebase/STACK.md` and `.planning/codebase/ARCHITECTURE.md`.
- Keep DTOs only where the boundary is already stable and public-facing, especially account/admin flows. The current project requirement in `.planning/PROJECT.md` already calls for minimizing DTO use by default, not deleting them blindly.

### Add

- Add a project-native security engine package set in the root app based on `angapp/src/main/java/com/mycompany/core/**`.
- Add YAML fetch-plan configuration to the root service, following `angapp/src/main/resources/fetch-plans.yml` and `angapp/src/main/java/com/mycompany/myapp/config/ApplicationProperties.java`.
- Add Liquibase changelogs for `sec_role`, `sec_permission`, and `sec_row_policy` based on `angapp/src/main/resources/config/liquibase/changelog/20260319000100_security_metadata.xml` plus project-specific seeds.
- Add a standalone `frontend/` app modeled on `aef-main/aef-main/src/app.routes.ts`, `aef-main/aef-main/src/app/core/auth/*.ts`, `aef-main/aef-main/src/app/layout/**`, and `aef-main/aef-main/src/app/pages/**`.
- Add an API proxy and environment model like `aef-main/aef-main/proxy.conf.json` and `aef-main/aef-main/src/environments/environment.development.ts` so the new frontend talks to the existing backend cleanly during migration.

### Do Not Copy Directly

- Do not copy `angapp/src/main/webapp/**` into the new repo. That is an embedded generated client with Angular 19 and Bootstrap-era JHipster structure from `angapp/package.json`, while the target is a standalone frontend.
- Do not downgrade the backend to `angapp`'s Java 17, Spring Boot 3.4.5, JHipster 8.11.0, or Undertow choice from `angapp/build.gradle` and `angapp/gradle/libs.versions.toml`.
- Do not restore database-backed fetch-plan metadata. The project requirement in `.planning/PROJECT.md` forbids it, and `angapp/src/main/resources/config/liquibase/changelog/20260321000000_drop_sec_fetch_plan_table.xml` confirms the reference app already moved away from that path.
- Do not cargo-cult `angapp` entity service/controller code as-is. For example `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java` still contains transitional DTO shaping around the secure data manager; port the security pattern, not the exact service layer.
- Do not use `RolePermissionServiceAllowAllImpl.java` outside tests or temporary scaffolding.
- Do not treat the current row-policy expression parser in `angapp/src/main/java/com/mycompany/core/security/row/RowLevelPolicyProviderDbImpl.java` as the final policy language. It is enough for v1 migration, but it is intentionally narrow.

## Alternatives Considered

| Category | Recommended | Alternative | Why not |
|----------|-------------|-------------|---------|
| Backend baseline | Root app in `build.gradle` + `gradle/libs.versions.toml` | Rebase on `angapp` application stack | Wrong direction for a brownfield migration; it would downgrade the working repo. |
| Security migration style | Port `angapp` security modules into root packages | Copy only `angapp` tables and seed data | Insufficient. The behavior lives in `angapp/src/main/java/com/mycompany/core/**`, not just in schema. |
| Frontend base | New standalone `frontend/` based on `aef-main/aef-main/**` | Re-enable/generated JHipster webapp from root or copy `angapp/src/main/webapp/**` | Conflicts with the requested standalone frontend and would pull in outdated structure/tooling. |
| Read model strategy | Fetch-plan-driven reads plus selective DTO boundaries | DTO-for-everything | Slower migration and duplicates the security projection logic already present in the secure serializer/fetch-plan flow. |

## Installation Delta

```bash
# root backend additions
./gradlew dependencies
# add jackson-dataformat-yaml to build.gradle

# new frontend baseline
cd frontend
npm install
```

## Sources

- Local project intent: `.planning/PROJECT.md`
- Current backend baseline: `.planning/codebase/STACK.md`
- Current backend structure: `.planning/codebase/ARCHITECTURE.md`
- Root backend build/runtime: `build.gradle`, `gradle/libs.versions.toml`, `src/main/java/com/vn/core/config/SecurityConfiguration.java`, `src/main/java/com/vn/core/web/rest/AuthenticateController.java`
- `angapp` security engine: `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java`, `angapp/src/main/java/com/mycompany/core/security/access/AccessManagerImpl.java`, `angapp/src/main/java/com/mycompany/core/security/permission/RolePermissionServiceDbImpl.java`, `angapp/src/main/java/com/mycompany/core/security/row/RowLevelPolicyProviderDbImpl.java`, `angapp/src/main/java/com/mycompany/core/fetch/YamlFetchPlanRepository.java`
- `angapp` metadata/config references: `angapp/src/main/resources/fetch-plans.yml`, `angapp/src/main/java/com/mycompany/myapp/config/ApplicationProperties.java`, `angapp/src/main/resources/config/liquibase/changelog/20260319000100_security_metadata.xml`, `angapp/src/main/resources/config/liquibase/changelog/20260321000000_drop_sec_fetch_plan_table.xml`, `angapp/build.gradle`, `angapp/gradle/libs.versions.toml`, `angapp/package.json`
- Frontend reference: `aef-main/aef-main/package.json`, `aef-main/aef-main/angular.json`, `aef-main/aef-main/src/main.ts`, `aef-main/aef-main/src/app.config.ts`, `aef-main/aef-main/src/app.routes.ts`, `aef-main/aef-main/src/app/core/auth/auth-jwt.service.ts`, `aef-main/aef-main/src/app/core/auth/account.service.ts`, `aef-main/aef-main/src/app/core/interceptor/auth.interceptor.ts`, `aef-main/aef-main/src/app/layout/component/main/app.layout.ts`, `aef-main/aef-main/proxy.conf.json`
