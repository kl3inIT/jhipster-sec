# Domain Pitfalls

**Domain:** Brownfield JHipster security migration (`src/main/java/com/vn/core/**` + `angapp` security core + `aef-main/aef-main`)
**Researched:** 2026-03-21

## Critical Pitfalls

### Pitfall 1: Breaking the authority bridge while merging role models
**What goes wrong:** The current backend authenticates with `jhi_authority` values and gates admin APIs on `ROLE_ADMIN`, while the `angapp` permission engine evaluates `sec_permission.role.code` against Spring authorities. If the migration replaces `ROLE_ADMIN` / `ROLE_USER` with only `sec_role` rows, or treats `sec_role` as independent from `jhi_authority`, login still works but admin APIs and the new security engine disagree on who can do what.
**Why it happens:** `src/main/java/com/vn/core/config/SecurityConfiguration.java`, `src/main/java/com/vn/core/web/rest/UserResource.java`, `src/main/java/com/vn/core/service/UserService.java`, and `src/main/java/com/vn/core/web/rest/AuthenticateController.java` are built around `ROLE_ADMIN` / `ROLE_USER`; `angapp/src/main/java/com/mycompany/core/security/permission/RolePermissionServiceDbImpl.java` expects role codes to already be present in the authenticated authority set; `angapp/src/main/resources/config/liquibase/changelog/20260319001000_security_seed_roles_permissions.xml` works around that by inserting extra authority names into `jhi_authority`.
**Consequences:** `/api/admin/**` becomes inaccessible, seeded security metadata silently denies everything, and frontend route guards keep checking the wrong authority names.
**Prevention:** Keep `jhi_authority` as the runtime authority source, preserve `ROLE_ADMIN` for platform administration, and define an explicit migration rule for how `sec_role.code` becomes a granted authority. Do not make this an implicit naming convention.
**Detection:** Admin login succeeds but `/api/admin/users` returns `403`; business endpoints return empty data or `AccessDeniedException`; JWTs contain only legacy roles or only new role codes, not the bridge set.
**Phase owner:** Phase 1 - identity, authority, and Liquibase merge.

### Pitfall 2: Preserving auth while losing secure data enforcement
**What goes wrong:** Controllers or services call repositories directly instead of going through the secure data manager. Authentication still looks correct, but CRUD permission checks, row filters, fetch-plan filtering, and secure merge protections are skipped.
**Why it happens:** `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java` is the only place where CRUD checks, row specs, fetch-plan resolution, serialization, and guarded updates are composed. Direct repository writes already exist in `angapp/src/main/java/com/mycompany/myapp/service/impl/DepartmentServiceImpl.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/EmployeeServiceImpl.java`, and `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java` for create flows and join-table maintenance.
**Consequences:** Hidden fields leak, protected fields become writable, row-policy restrictions are bypassed, and later audits will show "security works on some endpoints, not others".
**Prevention:** Make repository access from web/service layers an explicit anti-pattern for secured entities. New business controllers should depend on a secure facade, not repositories. Add tests that prove the same actor gets different results when secure enforcement is bypassed.
**Detection:** A user can update data through create/update endpoints that should be denied, or sees attributes in create/read responses that are absent when the same entity is loaded through the secure manager.
**Phase owner:** Phase 2 - secure read/write path adoption.

### Pitfall 3: Shipping a no-op or half-on row-policy system
**What goes wrong:** The merged app advertises row-level security, but policy enforcement is either absent or too weak to represent the target rules.
**Why it happens:** `angapp/src/main/java/com/mycompany/core/security/row/RowLevelPolicyProviderDbImpl.java` supports only a tiny DSL of `field = CURRENT_USER_ID`, assumes `field` is an association with an `id`, and returns no policy when the current user id is missing. No row-policy seed data was found in `angapp/src/main/resources/config/liquibase/changelog/*.xml`, and sample entities such as `angapp/src/main/java/com/mycompany/myapp/domain/Department.java` and `angapp/src/main/java/com/mycompany/myapp/domain/Organization.java` have no user-owned field that matches the provider's current assumption.
**Consequences:** Row policies are effectively inert, or they collapse queries to zero rows for the wrong reason. The roadmap then overestimates how much security survived the merge.
**Prevention:** Treat row-policy support as incomplete until there is both schema support on sample entities and seeded policies that are exercised by tests. Decide whether row policies are based on ownership, org membership, or role-driven scopes before exposing them in UI.
**Detection:** `sec_row_policy` exists but has no meaningful data; adding a policy yields no effect or hides every row; authenticated users without `USER_ID_CLAIM` lose access unexpectedly.
**Phase owner:** Phase 2 - row-policy design and enforcement.

### Pitfall 4: Secure writes are incomplete, especially on create and association updates
**What goes wrong:** Update paths are partially guarded, but create flows and relationship mutations stay outside secure merge rules.
**Why it happens:** `angapp/src/main/java/com/mycompany/core/merge/SecureMergeServiceImpl.java` only protects merge-by-payload updates. `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java` has `save(... UPDATE ...)` but no secure create path. `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java` saves the aggregate directly and then mutates `employeeIds` through `syncEmployees(...)` with raw repositories; `DepartmentServiceImpl` and `EmployeeServiceImpl` also create entities directly through repositories.
**Consequences:** Unauthorized users can create rows they should not create, attach cross-scope relations, or set fields that later reads would have hidden. This is exactly the sort of "looks secure on reads, unsafe on writes" drift that causes rewrites.
**Prevention:** Add secure create semantics before reducing DTOs or widening frontend coverage. Association writes need their own permission and row-policy story; do not treat join-table edits as harmless side effects.
**Detection:** A user denied `EDIT` on a field can still influence it during creation or relation sync; create responses are filtered, but persisted state contains unauthorized changes.
**Phase owner:** Phase 3 - secure create/update/delete semantics.

### Pitfall 5: Fetch-plan migration can regress into dead metadata or unenforced plan access
**What goes wrong:** The merged system says fetch plans are YAML-only, but Liquibase still seeds database fetch-plan records, or plan-level access control is never enforced.
**Why it happens:** `angapp/src/main/resources/config/liquibase/changelog/20260319000100_security_metadata.xml` creates `sec_fetch_plan`; multiple later changelogs insert/update it; `angapp/src/main/resources/config/liquibase/changelog/20260321000000_drop_sec_fetch_plan_table.xml` drops it again. At runtime, fetch plans are now loaded from `angapp/src/main/resources/fetch-plans.yml` by `angapp/src/main/java/com/mycompany/core/fetch/YamlFetchPlanRepository.java`. `angapp/src/main/java/com/mycompany/core/fetch/FetchPlanResolverImpl.java` checks `FetchPlanAccessContext`, but no fetch-plan constraint implementation was found under `angapp/src/main/java/com/mycompany/core/security/access/**`, and `FetchPlanAccessContext` defaults to permitted in `angapp/src/main/java/com/mycompany/core/security/access/FetchPlanAccessContext.java`.
**Consequences:** Migration scripts carry obsolete schema, YAML and DB definitions drift, and any authenticated caller can request richer fetch plans once more plans are added.
**Prevention:** Remove the DB fetch-plan lineage from the merged Liquibase baseline instead of replaying it and dropping it later. Treat fetch-plan `APPLY` permission as missing until a real constraint exists and is tested.
**Detection:** Liquibase still references `sec_fetch_plan`; changing YAML has no obvious authorization boundary; requesting a richer plan never fails.
**Phase owner:** Phase 1 for schema cleanup, Phase 2 for fetch-plan authorization.

## Moderate Pitfalls

### Pitfall 6: Permission target normalization drift
**What goes wrong:** Permissions are seeded, but exact string matching makes some of them ineffective after migration.
**Why it happens:** `angapp/src/main/java/com/mycompany/core/security/permission/AttributePermissionEvaluatorImpl.java` builds targets as `ENTITY.ATTRIBUTE` using uppercase entity and uppercase attribute. Earlier seeds in `angapp/src/main/resources/config/liquibase/changelog/20260320000000_security_seed_fetch_plans_and_attribute_permissions.xml` use uppercase tokens like `DEPARTMENT.NAME`; the later "fix" in `angapp/src/main/resources/config/liquibase/changelog/20260320000001_security_fix_attribute_permission_targets.xml` inserts mixed-case targets like `DEPARTMENT.id` and `ORGANIZATION.code`, which will not match exact queries in `angapp/src/main/java/com/mycompany/core/security/repository/SecPermissionRepository.java`.
**Consequences:** Teams think permissions were repaired, but only some attributes are actually protected. `id` stays visible only because `SecureEntitySerializerImpl` special-cases it.
**Prevention:** Normalize target tokens in one place and migrate existing data to that canonical form. Do not rely on manual casing discipline in change logs.
**Detection:** Permission rows exist for an attribute, but evaluator calls still deny or ignore them; only `id` survives because of serializer fallback.
**Phase owner:** Phase 1 - metadata normalization.

### Pitfall 7: Reducing DTOs too early breaks public contracts and reopens over-posting
**What goes wrong:** DTO cleanup is done as a blanket refactor, and stable account/admin contracts become entity- or map-based before the secure engine fully covers those cases.
**Why it happens:** Current account/admin flows intentionally keep boundary DTOs in `src/main/java/com/vn/core/service/dto/AdminUserDTO.java`, `src/main/java/com/vn/core/service/dto/UserDTO.java`, `src/main/java/com/vn/core/web/rest/AccountResource.java`, and `src/main/java/com/vn/core/web/rest/UserResource.java`. Those classes protect validation, partial-update behavior, and safe field exposure. The project brief in `.planning/PROJECT.md` already warns that DTO removal should be incremental.
**Consequences:** `/api/account` and `/api/admin/users` become unstable, validation moves into entities, and writes can start accepting fields that were previously ignored on purpose.
**Prevention:** Keep DTOs for identity, registration, password reset, and admin-user management until the secure data layer has a proven replacement. Limit DTO reduction to business entities first.
**Detection:** Frontend account forms begin posting fields that should be ignored, or admin endpoints start returning entities with unexpected relationships or missing validation errors.
**Phase owner:** Phase 3 - controlled DTO contraction.

### Pitfall 8: Production bootstrap differs from dev/test bootstrap
**What goes wrong:** The system works in local environments but denies everything or misses mappings in production.
**Why it happens:** Important seed data in `angapp/src/main/resources/config/liquibase/changelog/20260319001000_security_seed_roles_permissions.xml`, `20260320000000_security_seed_fetch_plans_and_attribute_permissions.xml`, `20260320000001_security_fix_attribute_permission_targets.xml`, `20260320010001_security_employee_and_org_links.xml`, and `20260320150000_security_fix_organization_detail_fetch_plan.xml` is guarded by `context="dev,test"`.
**Consequences:** Production starts with tables but without role mappings, sample permissions, or fetch-plan permissions. The engine then behaves as deny-by-default with confusing symptoms.
**Prevention:** Separate sample/demo data from required bootstrap data. Anything required for production behavior must move out of `dev,test` contexts.
**Detection:** A fresh production-like database authenticates users but every secured business call is denied or empty.
**Phase owner:** Phase 1 - Liquibase baseline hardening.

### Pitfall 9: Frontend authority checks are too static for the merged security model
**What goes wrong:** The new frontend looks integrated, but route guards and menus only understand coarse Spring authorities.
**Why it happens:** `aef-main/aef-main/src/app/core/auth/account.service.ts`, `aef-main/aef-main/src/app/core/auth/user-route-access.service.ts`, and `aef-main/aef-main/src/app/shared/auth/has-any-authority.directive.ts` only check the `authorities` array returned by `/api/account`. `aef-main/aef-main/src/app/config/authority.constants.ts` defines only `ROLE_ADMIN` and `ROLE_USER`. `aef-main/aef-main/src/app/layout/component/menu/app.menu.ts` hardcodes admin menu items and business catalog items without any entity- or permission-aware filtering.
**Consequences:** Users see screens they cannot use, hidden security-management features never appear, and business permissions are reduced to "admin vs non-admin" in the client.
**Prevention:** Preserve coarse authorities for shell navigation, but model security-management UI and business actions separately from `ROLE_ADMIN`. The frontend needs explicit capability mapping, not only authority-string reuse.
**Detection:** Users can navigate to entity pages that always fail with `403`, or cannot see security-admin screens even though backend permissions exist.
**Phase owner:** Phase 4 - frontend authorization integration.

## Minor Pitfalls

### Pitfall 10: The `aef-main` entity models are narrower than the secured backend payloads
**What goes wrong:** The frontend assumes fixed DTO-shaped responses while the merged backend starts returning filtered or nested secure payloads.
**Why it happens:** `aef-main/aef-main/src/app/pages/entities/organization/organization.model.ts` has only `id`, `code`, `name`, `description`, while `angapp/src/main/java/com/mycompany/myapp/service/dto/OrganizationDTO.java` also carries `employeeIds` and nested `emplList`. `aef-main/aef-main/src/app/pages/entities/organization/update/organization-update.component.ts` and `organization-form.service.ts` only bind the narrow shape.
**Consequences:** Nested data is dropped on round-trip, association editing is impossible, or partial secure responses get written back as null-like updates.
**Prevention:** Freeze the frontend contract per phase. Do not point `aef-main` screens at secured endpoints until the request/response model is agreed.
**Detection:** Editing an organization removes employee links, or detail pages silently ignore data present in the API response.
**Phase owner:** Phase 4 - frontend contract alignment.

### Pitfall 11: Error routing in the reference frontend is incomplete for security failures
**What goes wrong:** Security-driven `401/403/404` responses produce broken navigation instead of a clean UX.
**Why it happens:** `aef-main/aef-main/src/app.config.ts` routes `403` to `/accessdenied`, `404` to `/404`, and other failures to `/error`, but `aef-main/aef-main/src/app.routes.ts` only defines `accessdenied` and no `/404` or `/error` route.
**Consequences:** Row-policy-hidden records and auth failures degrade into redirect loops or blank shells, which makes backend security regressions harder to diagnose.
**Prevention:** Add the missing routes before wiring secured business endpoints into the new frontend.
**Detection:** A denied or missing record sends the user back to home instead of a stable error page.
**Phase owner:** Phase 4 - frontend shell hardening.

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Phase 1 - Identity and schema merge | `ROLE_ADMIN` / `ROLE_USER` drift away from `sec_role.code`, and Liquibase carries dead `sec_fetch_plan` history | Keep an explicit authority bridge, preserve current admin roles, remove obsolete fetch-plan table history from the merged baseline |
| Phase 1 - Metadata normalization | Permission target casing drifts between code and seed data | Canonicalize `target` generation and migrate existing rows to the same case |
| Phase 2 - Secure enforcement core | Teams bypass `SecureDataManagerImpl` or assume row policies already work | Route secured entities through one secure access facade and add row-policy tests before exposing the feature |
| Phase 2 - Fetch-plan access | Fetch-plan `APPLY` permissions are defined in data but not enforced in code | Add a real fetch-plan constraint and prove denial paths with tests |
| Phase 3 - Secure writes and DTO reduction | Create and association writes bypass secure merge; public DTOs are removed too early | Implement secure create/association rules first, then reduce DTOs only for business entities |
| Phase 4 - Frontend integration | `aef-main` only understands static authorities and narrow DTOs | Add capability-aware guards, align request/response contracts, and harden error routes before broad UI rollout |

## Sources

- Local project brief: `.planning/PROJECT.md`
- Current backend auth/account/admin APIs: `src/main/java/com/vn/core/web/rest/AccountResource.java`, `src/main/java/com/vn/core/web/rest/UserResource.java`, `src/main/java/com/vn/core/web/rest/AuthenticateController.java`, `src/main/java/com/vn/core/config/SecurityConfiguration.java`, `src/main/java/com/vn/core/service/UserService.java`
- `angapp` security core: `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java`, `angapp/src/main/java/com/mycompany/core/merge/SecureMergeServiceImpl.java`, `angapp/src/main/java/com/mycompany/core/serialize/SecureEntitySerializerImpl.java`, `angapp/src/main/java/com/mycompany/core/security/row/RowLevelPolicyProviderDbImpl.java`, `angapp/src/main/java/com/mycompany/core/security/permission/AttributePermissionEvaluatorImpl.java`, `angapp/src/main/java/com/mycompany/core/security/permission/RolePermissionServiceDbImpl.java`
- `angapp` service seams and DTOs: `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/DepartmentServiceImpl.java`, `angapp/src/main/java/com/mycompany/myapp/service/impl/EmployeeServiceImpl.java`, `angapp/src/main/java/com/mycompany/myapp/service/dto/OrganizationDTO.java`
- Liquibase/security metadata: `angapp/src/main/resources/config/liquibase/master.xml`, `angapp/src/main/resources/config/liquibase/changelog/20260319000100_security_metadata.xml`, `angapp/src/main/resources/config/liquibase/changelog/20260319001000_security_seed_roles_permissions.xml`, `angapp/src/main/resources/config/liquibase/changelog/20260320000000_security_seed_fetch_plans_and_attribute_permissions.xml`, `angapp/src/main/resources/config/liquibase/changelog/20260320000001_security_fix_attribute_permission_targets.xml`, `angapp/src/main/resources/config/liquibase/changelog/20260320010001_security_employee_and_org_links.xml`, `angapp/src/main/resources/config/liquibase/changelog/20260320150000_security_fix_organization_detail_fetch_plan.xml`, `angapp/src/main/resources/config/liquibase/changelog/20260321000000_drop_sec_fetch_plan_table.xml`, `angapp/src/main/resources/fetch-plans.yml`
- Frontend references: `aef-main/aef-main/src/app/core/auth/account.service.ts`, `aef-main/aef-main/src/app/core/auth/user-route-access.service.ts`, `aef-main/aef-main/src/app/shared/auth/has-any-authority.directive.ts`, `aef-main/aef-main/src/app/config/authority.constants.ts`, `aef-main/aef-main/src/app/pages/entities/organization/organization.model.ts`, `aef-main/aef-main/src/app/pages/entities/organization/update/organization-update.component.ts`, `aef-main/aef-main/src/app.config.ts`, `aef-main/aef-main/src/app.routes.ts`
