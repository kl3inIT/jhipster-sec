# Codebase Concerns

**Analysis Date:** 2026-03-27

## Tech Debt

**Simplified fetch-plan runtime versus legacy `angapp`:**
- Issue: The current fetch-plan stack is intentionally narrow. `src/main/java/com/vn/core/security/fetch/FetchPlanBuilder.java` only supports direct properties and placeholder nested plans, while `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java` loads a small YAML subset from `src/main/resources/fetch-plans.yml`. The legacy implementation in `angapp/src/main/java/com/mycompany/core/fetch/FetchPlanBuilder.java` is materially richer and is still referenced by brownfield migration expectations.
- Files: `src/main/java/com/vn/core/security/fetch/FetchPlanBuilder.java`, `src/main/java/com/vn/core/security/fetch/YamlFetchPlanRepository.java`, `src/main/java/com/vn/core/security/fetch/FetchPlanResolverImpl.java`, `src/main/resources/fetch-plans.yml`, `angapp/src/main/java/com/mycompany/core/fetch/FetchPlanBuilder.java`
- Impact: More complex fetch-plan-driven reads from `angapp` cannot be migrated safely without either flattening behavior or reintroducing missing builder/repository features. Missing or renamed plans fail at runtime.
- Fix approach: Treat fetch-plan parity as a migration phase of its own. Add a compatibility checklist against `angapp`, expand the builder/repository API, and add parity tests before moving additional secured entities.

**Dynamic JSON entity APIs instead of validated request models:**
- Issue: The secured proof entity APIs accept raw JSON bodies, convert them to `JsonNode` or `Map<String, Object>`, and merge them into entities through `BeanWrapper`. There is no typed boundary model or field allowlist comparable to the account and admin-user APIs.
- Files: `src/main/java/com/vn/core/web/rest/OrganizationResource.java`, `src/main/java/com/vn/core/web/rest/DepartmentResource.java`, `src/main/java/com/vn/core/web/rest/EmployeeResource.java`, `src/main/java/com/vn/core/service/OrganizationService.java`, `src/main/java/com/vn/core/service/DepartmentService.java`, `src/main/java/com/vn/core/service/EmployeeService.java`, `src/main/java/com/vn/core/security/merge/SecureMergeServiceImpl.java`
- Impact: Contract drift, weak validation, and runtime-only failures become more likely as the migrated domain grows. Planning new features is harder because the API contract is implicit in code paths instead of explicit in DTOs.
- Fix approach: Introduce typed request models or command DTOs per secured entity, keep the secure merge rules behind the service boundary, and validate association payloads explicitly.

**Security-table schema is still bare-minimum:**
- Issue: The permission and row-policy tables were added quickly and still lack some of the data-shape guarantees already present on menu-permission tables. `sec_permission` has no uniqueness constraint for role/target/action, and `sec_row_policy` only enforces unique `code`.
- Files: `src/main/resources/config/liquibase/changelog/20260321000200_create_sec_permission.xml`, `src/main/resources/config/liquibase/changelog/20260321000300_create_sec_row_policy.xml`, `src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java`
- Impact: Duplicate permission rows and overlapping policy rows can accumulate, making admin behavior harder to reason about and increasing the risk of dirty migration data.
- Fix approach: Add composite uniqueness where the runtime assumes logical uniqueness, and add migration cleanup scripts before importing more legacy security data.

**Toolchain and documentation drift:**
- Issue: The actual build now requires Java 25, but older repo guidance still describes Java 21-era assumptions.
- Files: `build.gradle`, `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`, `README.md`, `AGENTS.md`
- Impact: Local setup, CI images, and planning artifacts can be wrong even when the code is correct.
- Fix approach: Regenerate repo docs and planning artifacts from the current build files, then align devcontainer and CI images with the Java 25 requirement.

## Known Bugs

**Dev profile auto-loads tracked sample-secret configuration:**
- Symptoms: Running the default `dev` profile pulls in the `secret-samples` profile group and the tracked secret-sample file, so local startup can silently use committed sample credentials and JWT material unless overridden.
- Files: `src/main/resources/config/application.yml`, `src/main/resources/config/application-secret-samples.yml`, `src/main/resources/config/application-dev.yml`
- Trigger: Starting the backend through the default `dev` profile path such as `./gradlew`.
- Workaround: Override datasource and JWT settings externally and remove `secret-samples` from the `dev` profile group before normal team usage.

**Menu-definition sync is insert-only and preserves stale metadata:**
- Symptoms: Syncing from the frontend registry seeds missing menu definitions but never updates changed labels, routes, icons, or ordering for existing rows.
- Files: `src/main/java/com/vn/core/web/rest/admin/security/SecMenuDefinitionAdminResource.java`, `frontend/src/app/layout/navigation/navigation-registry.ts`
- Trigger: Updating navigation metadata in the registry and calling `/api/admin/sec/menu-definitions/sync`.
- Workaround: Delete the affected rows manually before syncing, or update them through the admin CRUD UI/API.

## Security Considerations

**Tracked credentials and TLS material live in the repository:**
- Risk: Database credentials, JWT material, and TLS artifacts are present in tracked config locations instead of an external secret store.
- Files: `src/main/resources/config/application-dev.yml`, `src/main/resources/config/application-prod.yml`, `src/main/resources/config/application-secret-samples.yml`, `gradle/liquibase.gradle`, `src/main/resources/config/tls/keystore.p12`
- Current mitigation: None in the repository state beyond profile separation.
- Recommendations: Remove secrets from version control, rotate anything already committed, treat `src/main/resources/config/tls/keystore.p12` as sensitive, and move runtime secrets to environment or secret-manager injection.

**Sensitive payloads are logged in debug mode:**
- Risk: The secured entity services log raw request payloads, which can include sensitive fields such as salary or future protected attributes. The `dev` profile enables verbose logging for `com.vn.core`.
- Files: `src/main/java/com/vn/core/service/OrganizationService.java`, `src/main/java/com/vn/core/service/DepartmentService.java`, `src/main/java/com/vn/core/service/EmployeeService.java`, `src/main/resources/config/application-dev.yml`
- Current mitigation: None beyond log-level choice per environment.
- Recommendations: Stop logging raw entity payloads, log only identifiers or field counts, and keep sensitive-attribute values out of structured or debug logs.

**JWT persistence uses browser storage:**
- Risk: `rememberMe` stores the authentication token in `localStorage`, which remains an XSS-sensitive storage location.
- Files: `frontend/src/app/core/auth/state-storage.service.ts`, `frontend/src/app/core/auth/auth-jwt.service.ts`, `frontend/src/app/core/interceptor/auth.interceptor.ts`
- Current mitigation: Same-origin API attachment only.
- Recommendations: Prefer shorter-lived tokens plus refresh flow or server-side session cookies, and pair the current approach with stronger CSP and XSS hardening.

**Query filters and sort fields are not allowlisted on secured entity endpoints:**
- Risk: The secured query path and pageable sort builder accept client-supplied field names without checking attribute visibility, supported operators, or an allowed-property list.
- Files: `src/main/java/com/vn/core/security/data/SecureQuerySpecificationFactory.java`, `src/main/java/com/vn/core/web/rest/OrganizationResource.java`, `src/main/java/com/vn/core/web/rest/DepartmentResource.java`, `src/main/java/com/vn/core/web/rest/EmployeeResource.java`
- Current mitigation: CRUD permission checks and row policies still apply.
- Recommendations: Add per-entity allowlists similar to `src/main/java/com/vn/core/web/rest/UserResource.java` and validate filterable/sortable fields against the secured catalog plus attribute permissions.

**Prometheus metrics are public by default:**
- Risk: `/management/prometheus` is explicitly permitted without authentication.
- Files: `src/main/java/com/vn/core/config/SecurityConfiguration.java`, `src/main/resources/config/application.yml`
- Current mitigation: Deployment network controls only.
- Recommendations: Put the endpoint behind network policy, gateway auth, or disable it outside controlled environments.

## Performance Bottlenecks

**Attribute permission checks can fan out into many database reads during serialization:**
- Problem: `SecureEntitySerializerImpl` checks view permission attribute-by-attribute and recursively across associations, while `AttributePermissionEvaluatorImpl` performs repository lookups for each target/action combination.
- Files: `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java`, `src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java`, `src/main/java/com/vn/core/security/repository/SecPermissionRepository.java`
- Cause: No request-scope permission matrix is reused in the read path.
- Improvement path: Reuse an in-memory permission matrix like `src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java` or cache evaluated attribute decisions per request and entity.

**Security tables are queried by unindexed columns:**
- Problem: The hot read paths query `sec_permission` by authority, target type, target, and action, and query `sec_row_policy` by entity and operation, but the corresponding Liquibase changelogs do not create matching composite indexes.
- Files: `src/main/resources/config/liquibase/changelog/20260321000200_create_sec_permission.xml`, `src/main/resources/config/liquibase/changelog/20260321000300_create_sec_row_policy.xml`, `src/main/java/com/vn/core/security/repository/SecPermissionRepository.java`, `src/main/java/com/vn/core/security/repository/SecRowPolicyRepository.java`
- Cause: Schema started from correctness-first tables without read-path tuning.
- Improvement path: Add composite indexes for the actual repository predicates before scaling the number of roles, permissions, or policies.

**Admin permission matrix pulls whole datasets and does heavy client-side tree work:**
- Problem: The permission-matrix screen loads the full secured catalog, the full role permission set, and all menu definitions, then builds a large client-side tree and pending-change map in one component.
- Files: `frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts`, `frontend/src/app/pages/admin/security/menu-definitions/service/sec-menu-definition.service.ts`, `src/main/java/com/vn/core/web/rest/admin/security/SecMenuDefinitionAdminResource.java`
- Cause: No pagination, no incremental loading, and no component decomposition.
- Improvement path: Keep app-scoped menu reads by default, paginate admin datasets, and split menu/entity permission logic into smaller components or server-prepared view models.

**Entity update forms use 1000-row reference-data fetches:**
- Problem: Department and employee forms fetch up to 1000 organizations or departments into client memory just to populate selectors.
- Files: `frontend/src/app/pages/entities/department/update/department-update.component.ts`, `frontend/src/app/pages/entities/employee/update/employee-update.component.ts`
- Cause: No search/autocomplete endpoint for related entities.
- Improvement path: Replace full dropdown loads with paged search endpoints and async autocomplete widgets.

## Brownfield Migration Risks

**Three implementation lineages are still present in one repo:**
- Issue: The new Angular shell in `frontend/`, the legacy `angapp/` codebase, and reference app directories such as `aef-main/` and `jhipter-angular/` coexist.
- Files: `frontend/src/app/pages/entities/organization/update/organization-update.component.ts`, `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`, `aef-main/`, `jhipter-angular/`
- Impact: Teams can easily copy from the wrong source of truth, and parity bugs can hide behind “it works in one of the apps.”
- Fix approach: Declare a single canonical source for each area, document intentional gaps, and retire reference directories from day-to-day implementation paths once their behavior is codified in tests.

**Current secured backend only covers proof entities, not the richer legacy migration surface:**
- Issue: The root app’s secured data path currently centers on `Organization`, `Department`, and `Employee`, while the legacy app still embodies broader security behavior and richer DTO/service logic.
- Files: `src/main/java/com/vn/core/domain/Organization.java`, `src/main/java/com/vn/core/domain/Department.java`, `src/main/java/com/vn/core/domain/Employee.java`, `src/main/java/com/vn/core/service/OrganizationService.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`
- Impact: Migration progress can look complete around proof flows while still missing behavior required for real `angapp` parity.
- Fix approach: Track migration feature parity by behavior, not by the presence of a similarly named endpoint or page.

**Permission vocabulary still differs across admin surfaces:**
- Issue: Entity permissions use UI normalization between `GRANT` and stored `ALLOW`, while menu permissions use `ALLOW|DENY` directly and row policies have a different contract entirely.
- Files: `src/main/java/com/vn/core/service/security/SecPermissionUiContractService.java`, `src/main/java/com/vn/core/web/rest/admin/security/SecPermissionAdminResource.java`, `src/main/java/com/vn/core/web/rest/admin/security/AdminMenuPermissionResource.java`, `frontend/src/app/pages/admin/security/permission-matrix/permission-matrix.component.ts`
- Impact: Brownfield migration code has to remember surface-specific enums and translations, which increases integration mistakes and test fragility.
- Fix approach: Consolidate on a shared effect vocabulary or expose one adapter layer for all security-admin APIs.

## Fragile Areas

**Association writes rely on unproven `BeanWrapper` coercion:**
- Files: `src/main/java/com/vn/core/security/merge/SecureMergeServiceImpl.java`, `frontend/src/app/pages/entities/department/update/department-update.component.ts`, `frontend/src/app/pages/entities/employee/update/employee-update.component.ts`, `src/test/java/com/vn/core/security/merge/SecureMergeServiceImplTest.java`
- Why fragile: The frontend sends nested `organization` and `department` objects, while the merge layer only calls `wrapper.setPropertyValue(...)`. The current tests only cover scalar fields, so relation writes are an unverified path.
- Safe modification: Introduce explicit association resolution by ID in the service layer before expanding department or employee writes.
- Test coverage: No backend integration test currently proves department/employee create or update with related-entity payloads.

**Row-level policy logic is global, not role-scoped:**
- Files: `src/main/java/com/vn/core/security/domain/SecRowPolicy.java`, `src/main/resources/config/liquibase/changelog/20260321000300_create_sec_row_policy.xml`, `src/main/java/com/vn/core/security/row/RowLevelPolicyProviderDbImpl.java`, `src/main/java/com/vn/core/service/dto/security/SecRowPolicyDTO.java`
- Why fragile: Policies are selected only by entity and operation, with no authority or role discriminator. Any added policy affects every user reaching that entity/op path.
- Safe modification: Do not expand row-policy usage until role scoping is added end-to-end in schema, repository, DTOs, admin API, and frontend.
- Test coverage: Existing tests prove expression parsing and application, not role-scoped selection because the model does not support it.

**Fetch-plan resolution depends on string codes with no compile-time protection:**
- Files: `src/main/java/com/vn/core/security/catalog/MetamodelSecuredEntityCatalog.java`, `src/main/java/com/vn/core/security/fetch/FetchPlanResolverImpl.java`, `src/main/resources/fetch-plans.yml`
- Why fragile: New `@SecuredEntity` types assume default fetch-plan codes, but missing YAML definitions fail only at runtime.
- Safe modification: Add startup validation that every secured entity has its declared/default fetch plans present.
- Test coverage: Fetch-plan unit tests exist, but there is no repository-wide parity check between catalog entries and YAML definitions.

**Navigation sync and runtime permissions can drift apart:**
- Files: `frontend/src/app/layout/navigation/navigation-registry.ts`, `src/main/java/com/vn/core/web/rest/admin/security/SecMenuDefinitionAdminResource.java`, `frontend/src/app/layout/navigation/navigation.service.ts`
- Why fragile: The frontend registry is code-defined, menu definitions are database-defined, sync is insert-only, and runtime visibility depends on both DB permissions and cached client-side menu IDs.
- Safe modification: Treat registry sync as an upsert with drift detection, and version the navigation cache key when menu structure changes.
- Test coverage: There is no end-to-end test proving registry change -> sync -> runtime visibility update.

## Scaling Limits

**Capability and catalog endpoints assume a very small secured domain:**
- Current capacity: 3 secured proof entities with limited attributes.
- Limit: `src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java` and `src/main/java/com/vn/core/web/rest/admin/security/SecCatalogAdminResource.java` walk the metamodel and build full in-memory lists for every request.
- Scaling path: Cache catalog metadata, cache per-user permission matrices, and avoid recomputing attribute lists on every request.

**Reference-data dropdowns cap out at the first 1000 rows:**
- Current capacity: `frontend/src/app/pages/entities/department/update/department-update.component.ts` and `frontend/src/app/pages/entities/employee/update/employee-update.component.ts` request `size: 1000`.
- Limit: Larger organization or department datasets will silently omit selectable options past that limit.
- Scaling path: Add search endpoints and async pickers instead of fixed-size bulk loads.

**Security admin endpoints are designed for low-volume interactive use:**
- Current capacity: Unpaginated list endpoints for menu definitions and row policies.
- Limit: `src/main/java/com/vn/core/web/rest/admin/security/SecMenuDefinitionAdminResource.java` and `src/main/java/com/vn/core/web/rest/admin/security/SecRowPolicyAdminResource.java` return full datasets, which will degrade both API and UI as the security catalog grows.
- Scaling path: Add pagination, filtering, and server-side search before importing production-scale security data.

## Dependencies at Risk

**Java 25 toolchain requirement is ahead of the repo’s written assumptions:**
- Risk: The build and container image now depend on Java 25, while repo guidance and prior planning assumptions still describe Java 21-era expectations.
- Impact: `build.gradle`, `buildSrc/src/main/groovy/jhipster.docker-conventions.gradle`, `README.md`, and `AGENTS.md` can pull teams and automation in different directions.
- Migration plan: Update all repo-local setup guidance, container bases, CI workers, and planning docs as one change set instead of leaving version drift in place.

## Missing Critical Features

**Role-aware row policies are not implemented:**
- Problem: The current row-policy model cannot express “this policy belongs to these roles,” which is core brownfield migration behavior from the legacy security model.
- Blocks: Safe migration of Jmix-style role-specific row restrictions from `angapp` into the root app.
- Files: `src/main/java/com/vn/core/security/domain/SecRowPolicy.java`, `src/main/resources/config/liquibase/changelog/20260321000300_create_sec_row_policy.xml`, `src/main/java/com/vn/core/security/row/RowLevelPolicyProviderDbImpl.java`

**JPQL secured-query support is explicitly absent:**
- Problem: `SecureDataManagerImpl` rejects JPQL secured queries even if a catalog entry were to allow them, and `MetamodelSecuredEntityCatalog` hardcodes `jpqlAllowed(false)`.
- Blocks: Migration of any legacy use case that depends on query translation beyond simple top-level equality filters.
- Files: `src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java`, `src/main/java/com/vn/core/security/catalog/MetamodelSecuredEntityCatalog.java`, `src/main/java/com/vn/core/security/data/SecureQuerySpecificationFactory.java`

**Workspace filtering is only partially migrated:**
- Problem: The backend exposes `/query` endpoints and there is client workspace-context plumbing, but the new entity list pages still use plain GET list endpoints with sort/page only and no real filter UI.
- Blocks: Brownfield parity for richer secured list filtering and return-to-workspace flows.
- Files: `src/main/java/com/vn/core/web/rest/vm/SecuredEntityQueryVM.java`, `src/main/java/com/vn/core/web/rest/OrganizationResource.java`, `src/main/java/com/vn/core/web/rest/DepartmentResource.java`, `src/main/java/com/vn/core/web/rest/EmployeeResource.java`, `frontend/src/app/pages/entities/shared/service/workspace-context.service.ts`, `frontend/src/app/pages/entities/organization/list/organization-list.component.ts`, `frontend/src/app/pages/entities/department/list/department-list.component.ts`, `frontend/src/app/pages/entities/employee/list/employee-list.component.ts`

## Operational Hazards

**Default local startup points at non-local configuration unless overridden:**
- Risk: The repo’s default profile chain and tracked config make it easy to run against shared infrastructure by accident.
- Files: `src/main/resources/config/application.yml`, `src/main/resources/config/application-dev.yml`, `gradle/liquibase.gradle`
- Mitigation path: Default to empty/local-only config, require explicit opt-in for remote hosts, and keep sample values out of the active dev profile group.

**Integration tests are optimized for convenience over isolation:**
- Risk: Testcontainers reuse is enabled and test profiles carry a “temp relief” TODO around DDL handling, which increases the chance of state leakage or configuration drift between runs.
- Files: `src/test/java/com/vn/core/config/DatabaseTestcontainer.java`, `src/test/resources/config/application-testdev.yml`, `src/test/resources/config/application-testprod.yml`
- Mitigation path: Disable container reuse for CI, remove the temporary Hibernate relief, and keep schema creation strictly under Liquibase plus reproducible seed data.

**Playwright flows depend on seeded default admin credentials and local ports:**
- Risk: E2E scripts assume a local frontend on `http://localhost:4200` and a usable default admin account, which is brittle across environments.
- Files: `frontend/playwright.config.ts`, `frontend/e2e/security-comprehensive.spec.ts`, `frontend/e2e/permission-matrix.spec.ts`
- Mitigation path: Parameterize admin credentials and base URLs through environment variables, and add explicit environment readiness checks before test execution.

## Test Coverage Gaps

**Department and employee REST writes are largely unverified:**
- What's not tested: Direct create, update, patch, delete, and detail-path behavior for `DepartmentResource` and `EmployeeResource`.
- Files: `src/main/java/com/vn/core/web/rest/DepartmentResource.java`, `src/main/java/com/vn/core/web/rest/EmployeeResource.java`, `src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java`
- Risk: Relation writes, attribute enforcement, and error translation for those endpoints can regress without detection.
- Priority: High

**Association merge behavior has no realistic backend tests:**
- What's not tested: Nested relation payloads sent by the new Angular forms into `SecureMergeServiceImpl`.
- Files: `src/main/java/com/vn/core/security/merge/SecureMergeServiceImpl.java`, `src/test/java/com/vn/core/security/merge/SecureMergeServiceImplTest.java`, `frontend/src/app/pages/entities/department/update/department-update.component.ts`, `frontend/src/app/pages/entities/employee/update/employee-update.component.ts`
- Risk: Department and employee writes may fail only in integration or production paths.
- Priority: High

**Frontend admin security surface has wide unit-test holes:**
- What's not tested: Roles list, row-policy UI, menu-definition dialogs/lists, menu-permission services, and catalog/permission services mostly ship without focused specs.
- Files: `frontend/src/app/pages/admin/security/roles/list/role-list.component.ts`, `frontend/src/app/pages/admin/security/roles/dialog/role-dialog.component.ts`, `frontend/src/app/pages/admin/security/row-policies/list/row-policy-list.component.ts`, `frontend/src/app/pages/admin/security/row-policies/dialog/row-policy-dialog.component.ts`, `frontend/src/app/pages/admin/security/menu-definitions/list/menu-definition-list.component.ts`, `frontend/src/app/pages/admin/security/menu-definitions/dialog/menu-definition-dialog.component.ts`, `frontend/src/app/pages/admin/security/shared/service/sec-catalog.service.ts`, `frontend/src/app/pages/admin/security/shared/service/sec-permission.service.ts`, `frontend/src/app/pages/admin/security/shared/service/admin-menu-permission.service.ts`
- Risk: Brownfield admin flows can drift while only the large permission-matrix component remains covered.
- Priority: Medium

**Department and employee frontend flows are only partially covered:**
- What's not tested: Department routes, department list/detail/service, employee list/detail/service/routes, and workspace-filter behavior.
- Files: `frontend/src/app/pages/entities/department/department.routes.ts`, `frontend/src/app/pages/entities/department/list/department-list.component.ts`, `frontend/src/app/pages/entities/department/detail/department-detail.component.ts`, `frontend/src/app/pages/entities/department/service/department.service.ts`, `frontend/src/app/pages/entities/employee/employee.routes.ts`, `frontend/src/app/pages/entities/employee/list/employee-list.component.ts`, `frontend/src/app/pages/entities/employee/detail/employee-detail.component.ts`, `frontend/src/app/pages/entities/employee/service/employee.service.ts`, `frontend/src/app/pages/entities/shared/service/workspace-context.service.ts`
- Risk: The new Angular app can appear complete while entity CRUD and navigation-state behavior remain weakly protected by tests.
- Priority: Medium

**Playwright coverage still skips some permission scenarios:**
- What's not tested: Some attribute-matrix scenarios are skipped when no suitable entity shape is found, and there is no dedicated E2E coverage for row-policy admin or menu-definition drift.
- Files: `frontend/e2e/permission-matrix.spec.ts`, `frontend/e2e/security-comprehensive.spec.ts`, `frontend/e2e/proof-role-gating.spec.ts`
- Risk: CI can stay green while critical security-admin scenarios remain unproven.
- Priority: Medium

---

*Concerns audit: 2026-03-27*
