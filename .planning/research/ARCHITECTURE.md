# Architecture Patterns

**Domain:** JHipster security platform brownfield migration  
**Researched:** 2026-03-21

## Recommended Integrated Architecture

Keep the current JHipster authentication and admin backbone intact in `src/main/java/com/vn/core/web/rest/AccountResource.java`, `src/main/java/com/vn/core/web/rest/AuthenticateController.java`, `src/main/java/com/vn/core/web/rest/UserResource.java`, `src/main/java/com/vn/core/service/UserService.java`, and `src/main/java/com/vn/core/config/SecurityConfiguration.java`. Add the `angapp` security engine as a separate secure business-data path inside the same backend instead of blending permission checks into every controller and mapper.

The migration target should have two backend lanes:

1. **Identity/admin lane** for login, account lifecycle, JWT, user admin, mail, and authorities. This stays close to the current backend shape because it already works and is the brownfield safety boundary.
2. **Secure business-data lane** for sample entities and future business aggregates. All entity CRUD, row filtering, attribute filtering, secure merge, and fetch-plan-controlled reads run through one project-native security slice modeled on `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java`.

For this repository, the cleanest package placement is to keep the new engine under `src/main/java/com/vn/core/security/` so it still fits the existing layer contract enforced in `src/test/java/com/vn/core/TechnicalStructureTest.java`. Concretely, prefer:

- `src/main/java/com/vn/core/security/access/**`
- `src/main/java/com/vn/core/security/permission/**`
- `src/main/java/com/vn/core/security/row/**`
- `src/main/java/com/vn/core/security/fetch/**`
- `src/main/java/com/vn/core/security/serialize/**`
- `src/main/java/com/vn/core/security/merge/**`
- `src/main/java/com/vn/core/security/data/**`

That keeps the migration visibly security-centric and avoids introducing a second parallel core architecture that would fight the current `config/web/service/security/repository/domain` mental model.

## Recommended Architecture

```text
Angular frontend (`frontend/`)
  -> auth token + route/menu hints only
  -> REST resources in `src/main/java/com/vn/core/web/rest/**`
  -> application services in `src/main/java/com/vn/core/service/**`
  -> secure data pipeline in `src/main/java/com/vn/core/security/**`
       -> CRUD permission evaluation
       -> row-level specification building
       -> fetch-plan resolution from YAML/code
       -> attribute-aware serialization
       -> secure merge for updates
  -> repositories in `src/main/java/com/vn/core/repository/**`
  -> JPA entities in `src/main/java/com/vn/core/domain/**`
  -> Liquibase + YAML config in `src/main/resources/**`
```

## Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| Identity/Admin API | Preserve the existing contract from `src/main/java/com/vn/core/web/rest/AccountResource.java`, `src/main/java/com/vn/core/web/rest/AuthenticateController.java`, and `src/main/java/com/vn/core/web/rest/UserResource.java`. Keep `AdminUserDTO`, `ManagedUserVM`, and password/reset models here. | `src/main/java/com/vn/core/service/UserService.java`, `src/main/java/com/vn/core/service/MailService.java`, `src/main/java/com/vn/core/security/DomainUserDetailsService.java` |
| Secure Business API | New sample-entity resources should terminate HTTP only. They should not duplicate permission logic the way `angapp/src/main/java/com/mycompany/myapp/web/rest/OrganizationResource.java` and peers currently do. | Thin feature services in `src/main/java/com/vn/core/service/**` |
| Feature Services | Own transaction orchestration, relation syncing, and request validation for business aggregates. They call the secure data pipeline for read/update/delete and only use repositories directly for create-time assembly or aggregate-specific write coordination. | `src/main/java/com/vn/core/security/data/**`, `src/main/java/com/vn/core/repository/**` |
| Secure Data Pipeline | Project-native equivalent of `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java`, `.../merge/SecureMergeServiceImpl.java`, `.../serialize/SecureEntitySerializerImpl.java`, `.../fetch/FetchPlanResolverImpl.java`. This is the enforcement core. | Permission, row-policy, fetch-plan, serializer, merge, and repository support components |
| Security Metadata | Role, permission, and row-policy persistence equivalent to `angapp/src/main/java/com/mycompany/core/security/repository/SecPermissionRepository.java` and `.../SecRowPolicyRepository.java`. | Secure data pipeline and seed migrations |
| Repository Support | Dynamic repository lookup equivalent to `angapp/src/main/java/com/mycompany/core/repository/SpringRepositoryRegistry.java`. Keep it backend-internal so services never switch on entity type manually. | Secure data pipeline and Spring Data repositories |
| Frontend Core | Follow the `aef-main/aef-main/src/app/core/auth/account.service.ts`, `.../core/interceptor/auth.interceptor.ts`, `.../core/auth/user-route-access.service.ts`, and layout/menu pattern from `.../layout/component/menu/app.menu.ts`. | Existing auth/account/admin endpoints plus new business endpoints |
| Frontend Features | Entity pages under a `frontend/src/app/pages/entities/**` shape like `aef-main/aef-main/src/app/pages/entities/entity.routes.ts`. | Business REST resources only |

## Frontend/Backend Boundary

### Backend owns all real security decisions

The frontend may hide routes, menu items, or buttons using account authorities the same way `aef-main/aef-main/src/app/core/auth/user-route-access.service.ts` and `.../layout/component/menu/app.menu.ts` do, but that is only UX guidance. The backend must remain authoritative for:

- entity CRUD permission checks
- row-level filtering
- attribute-level visibility/editability
- allowed fetch-plan usage
- write-time merge guards

Do not move attribute or row-policy logic into Angular forms or guards. The frontend should assume fields can disappear from responses and writes can be rejected even if the route was visible.

### Keep identity/admin APIs separate from secure business APIs

Preserve the current `/api/authenticate`, `/api/account`, `/api/register`, and `/api/admin/**` lane as-is. Put sample entity APIs in separate resources under `src/main/java/com/vn/core/web/rest/` and route them from a dedicated frontend entities area modeled on `aef-main/aef-main/src/app/pages/entities/**`.

### Do not let the client choose arbitrary fetch plans yet

`angapp/src/main/java/com/mycompany/core/fetch/FetchPlanResolverImpl.java` already supports a `FetchPlanAccessContext`, but there is no actual fetch-plan access constraint implementation in the local reference. Until that exists in the merged backend, controllers should choose fetch-plan codes server-side with constants, not accept free-form `fetchPlan` query parameters from the UI.

## Where Fetch-Plan Infrastructure Should Live

If fetch plans are YAML/code-only, keep the infrastructure in three places:

1. **Runtime engine in backend security packages**  
   Put the resolver, metadata tools, builder, and repository abstractions under `src/main/java/com/vn/core/security/fetch/**`, modeled on `angapp/src/main/java/com/mycompany/core/fetch/FetchPlans.java`, `.../FetchPlanBuilder.java`, `.../YamlFetchPlanRepository.java`, and `.../FetchPlanResolverImpl.java`.

2. **Definitions in resources, not the database**  
   Store canonical named plans in `src/main/resources/fetch-plans.yml`, mirroring `angapp/src/main/resources/fetch-plans.yml`. Configure the location from `src/main/java/com/vn/core/config/ApplicationProperties.java`, following the pattern in `angapp/src/main/java/com/mycompany/myapp/config/ApplicationProperties.java`.

3. **Optional code builders close to feature services**  
   Use code builders only for compositions that are too awkward in YAML. Those builders should still use the shared engine from `src/main/java/com/vn/core/security/fetch/**`, not invent per-feature serializers.

Do **not** recreate `sec_fetch_plan` or any database-backed fetch-plan registry. The stale seed file `angapp/src/main/resources/config/liquibase/changelog/20260320000000_security_seed_fetch_plans_and_attribute_permissions.xml` shows the old direction and should be treated as migration history, not target architecture.

## Data Flow

### Identity flow

1. `frontend` login/account screens call the existing auth endpoints the way `aef-main/aef-main/src/app/pages/login/login.service.ts` and `.../core/auth/account.service.ts` expect.
2. `src/main/java/com/vn/core/web/rest/AuthenticateController.java` authenticates and issues JWT.
3. `src/main/java/com/vn/core/config/SecurityConfiguration.java` validates the bearer token and populates the Spring security context.
4. Existing account/admin flows continue to use `src/main/java/com/vn/core/service/UserService.java`.

### Secure business read flow

1. Angular entity page calls a business REST resource.
2. Resource delegates immediately to a feature service.
3. Feature service calls `SecureDataManager.loadOne(...)` or `loadPage(...)`.
4. The secure pipeline evaluates CRUD access, adds row-level `Specification` filters, resolves a server-chosen fetch plan from YAML/code, and serializes only visible attributes.
5. Resource returns the fetch-plan-shaped JSON to the frontend.

This is the behavior already visible across `angapp/src/main/java/com/mycompany/myapp/service/impl/DepartmentServiceImpl.java`, `.../EmployeeServiceImpl.java`, and `.../OrganizationServiceImpl.java`, and it is the right integration pattern for the target backend.

### Secure business write flow

1. Angular form sends a typed command payload.
2. Resource passes the command to a feature service.
3. Feature service converts the command into a narrow `Map<String, Object>` or aggregate mutation plan.
4. `SecureDataManager.save(...)` or `delete(...)` enforces CRUD permission, row access, and attribute edit rights before persistence.
5. Feature service returns a secure read model by reloading through the fetch-plan path.

This preserves the important `angapp` behavior from `angapp/src/main/java/com/mycompany/core/merge/SecureMergeServiceImpl.java` and `.../serialize/SecureEntitySerializerImpl.java`: writes are guarded by editable attributes, reads are guarded by visible attributes.

## Sample Entities To Exercise Security Cleanly

Use sample entities as security test fixtures, not as a separate architecture. The clean set is:

- **`Department`**: flat scalar entity, equivalent to `angapp/src/main/java/com/mycompany/myapp/domain/Department.java`. Use it to prove entity CRUD plus attribute-level VIEW/EDIT filtering with the smallest possible surface.
- **`Employee`**: keep the scalar fields from `angapp/src/main/java/com/mycompany/myapp/domain/Employee.java`, but add a direct association to `src/main/java/com/vn/core/domain/User.java`. That gives row policies a simple stable anchor to the authenticated principal.
- **`Organization`**: keep the aggregate-root shape from `angapp/src/main/java/com/mycompany/myapp/domain/Organization.java` and expose nested reads through fetch plans.
- **`OrganizationMembership`**: import the behavior of `angapp/src/main/java/com/mycompany/myapp/domain/OrgEmpl.java`, but rename it on merge if schema churn is still acceptable. A descriptive join entity is easier to reason about than `OrgEmpl`.

Recommended security coverage by entity:

| Entity | Security behavior it should prove |
|--------|-----------------------------------|
| `Department` | scalar attribute hide/edit rules |
| `Employee` | row-level ownership against current `User` |
| `Organization` | nested fetch-plan reads and aggregate CRUD |
| `OrganizationMembership` | nested attribute filtering inside a collection |

Do not make sample entities depend on admin-only DTO patterns from `src/main/java/com/vn/core/service/dto/AdminUserDTO.java`. For sample business APIs, prefer:

- typed request models for create/update commands when validation matters
- fetch-plan-shaped response JSON for reads
- thin service orchestration instead of mapper-heavy DTO layers

That matches the project requirement to minimize DTO usage while keeping stable boundary models only where they are genuinely useful.

## Build-Order Implications

1. **Lock the current identity/admin lane first**  
   Treat `src/main/java/com/vn/core/web/rest/AccountResource.java`, `src/main/java/com/vn/core/web/rest/UserResource.java`, and `src/main/java/com/vn/core/config/SecurityConfiguration.java` as protected brownfield boundaries. Add regression coverage before migration work spreads.

2. **Add the security engine packages before any sample API**  
   The permission evaluator, row-policy builder, fetch-plan resolver, secure serializer, merge service, and repository registry are shared primitives. Business resources should be built on them, not alongside them.

3. **Add security metadata schema and seeds before sample entities rely on it**  
   Roles, permissions, and row-policy tables must exist before any secure entity endpoint can be meaningfully exercised.

4. **Add sample entities and repositories next**  
   Bring in `Department`, `Employee`, `Organization`, and `OrganizationMembership`, plus Liquibase and repositories, before building UI. The frontend cannot validate security behavior against placeholder endpoints.

5. **Build secure business services and resources before the frontend entity screens**  
   Finish the secure backend contract first. The Angular shell can start earlier, but entity screens should wait until fetch-plan-shaped JSON and rejection behavior are stable.

6. **Build the standalone Angular app after backend contracts are real**  
   Use the `aef-main/aef-main/src/app/core/**`, `.../layout/**`, and `.../pages/**` structure, but point it at the actual merged backend rather than a speculative API.

## Build-Order Dependency Graph

```text
Existing auth/admin stability
  -> security engine packages
  -> security metadata schema + seeds
  -> sample domain + repositories
  -> secure business services/resources
  -> frontend shell/auth/account/admin
  -> frontend sample entity screens
  -> broader business migration from `angapp`
```

## Anti-Patterns To Avoid

### Duplicated permission checks in controllers and services

The `angapp` resources currently call `AccessManager` directly in controllers such as `angapp/src/main/java/com/mycompany/myapp/web/rest/DepartmentResource.java` and then call secure services that enforce again. In the merged backend, keep HTTP resources thin and let the secure business service/data path own the rules.

### Reintroducing database-backed fetch-plan definitions

The target state in `.planning/PROJECT.md` is explicit: fetch plans belong in YAML or code only. Do not rebuild `sec_fetch_plan`.

### Returning unrestricted entities from repositories

Do not let new resources bypass the secure serializer by returning raw JPA entities from `src/main/java/com/vn/core/repository/**`. That would immediately split behavior between "secured" and "unsecured" endpoints.

## Sources

- Local project context: `.planning/PROJECT.md`
- Current backend architecture: `.planning/codebase/ARCHITECTURE.md`
- Current backend boundaries: `src/main/java/com/vn/core/config/SecurityConfiguration.java`, `src/main/java/com/vn/core/web/rest/AccountResource.java`, `src/main/java/com/vn/core/web/rest/UserResource.java`, `src/test/java/com/vn/core/TechnicalStructureTest.java`
- `angapp` security engine references: `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java`, `angapp/src/main/java/com/mycompany/core/merge/SecureMergeServiceImpl.java`, `angapp/src/main/java/com/mycompany/core/serialize/SecureEntitySerializerImpl.java`, `angapp/src/main/java/com/mycompany/core/fetch/YamlFetchPlanRepository.java`, `angapp/src/main/java/com/mycompany/core/fetch/FetchPlanResolverImpl.java`, `angapp/src/main/java/com/mycompany/core/security/permission/EntityPermissionEvaluatorImpl.java`, `angapp/src/main/java/com/mycompany/core/security/permission/AttributePermissionEvaluatorImpl.java`, `angapp/src/main/java/com/mycompany/core/security/row/RowLevelSpecificationBuilder.java`
- `angapp` sample business references: `angapp/src/main/java/com/mycompany/myapp/service/impl/DepartmentServiceImpl.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/EmployeeServiceImpl.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`, `angapp/src/main/resources/fetch-plans.yml`
- Angular reference structure: `aef-main/aef-main/src/app/core/auth/account.service.ts`, `aef-main/aef-main/src/app/core/interceptor/auth.interceptor.ts`, `aef-main/aef-main/src/app/core/auth/user-route-access.service.ts`, `aef-main/aef-main/src/app/layout/component/menu/app.menu.ts`, `aef-main/aef-main/src/app/pages/entities/entity.routes.ts`
