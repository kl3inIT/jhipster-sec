# Project Research Summary

**Project:** JHipster security platform migration
**Domain:** Brownfield security-platform migration
**Researched:** 2026-03-21
**Confidence:** MEDIUM-HIGH

## Executive Summary

This is not a greenfield rewrite. The recommended v1 is to keep the current root backend as the runtime baseline (`build.gradle`, `gradle/libs.versions.toml`, `src/main/java/com/vn/core/**`) and port the working security engine from `angapp/src/main/java/com/mycompany/core/**` into project-native packages under `src/main/java/com/vn/core/security/**`. The standalone frontend should be rebuilt in `frontend/` from the `aef-main/aef-main/**` Angular shell, not by reviving `angapp/src/main/webapp/**`.

Experts would build this as a staged merge: preserve the current JWT/account/admin lane (`src/main/java/com/vn/core/web/rest/AuthenticateController.java`, `AccountResource.java`, `UserResource.java`), add the shared secure data pipeline first, then layer protected sample entity flows and security-admin screens on top. The migration succeeds in v1 only if role/permission metadata, row filtering, attribute filtering, secure writes, and fetch-plan-shaped reads all work together against real entities.

The main risk is sequencing. If the authority bridge, Liquibase baseline, and secure data path are not established before entity APIs and UI work, the project will ship a system that authenticates but does not actually enforce data security. Roadmap order should therefore prioritize identity/schema alignment, then backend enforcement primitives, then proof entities, and only then the standalone frontend feature area.

## Key Findings

### Recommended Stack

Stay on Java 21, Spring Boot 4.0.3, JHipster Framework 9.0.0, PostgreSQL, Liquibase, and the current JWT resource-server setup from the root app. Treat `angapp` as a donor for security modules and YAML fetch-plan behavior, not as the application baseline to copy or downgrade to. For the UI, use Angular 21 standalone bootstrap plus PrimeNG from `aef-main/aef-main/package.json`.

**Core technologies:**
- Java 21 + Spring Boot 4.0.3: keep the root deployment stack and adapt imported security code to it.
- JHipster 9 + current JWT auth: preserve the working auth/admin contract instead of rewriting identity.
- JPA/Hibernate + PostgreSQL/Liquibase: keep the existing persistence model and add security metadata through new root changelogs.
- YAML fetch plans via `jackson-dataformat-yaml`: required for `fetch-plans.yml`-driven secure reads.
- Angular 21 standalone + PrimeNG: use the `aef-main` shell for the new `frontend/` app.

### Expected Features

V1 must preserve current account and admin behavior while adding first-class security management and proving the imported security engine against real entities.

**Must have (table stakes):**
- Authentication and account lifecycle parity from `src/main/java/com/vn/core/web/rest/AuthenticateController.java` and `AccountResource.java`.
- Admin user and authority management parity from `src/main/java/com/vn/core/web/rest/UserResource.java` and `AuthorityResource.java`.
- CRUD for security roles, permissions, and row policies based on `angapp/src/main/webapp/app/entities/sec-role/**`, `sec-permission/**`, and `sec-row-policy/**`.
- Protected `Department` and `Organization` flows to prove entity CRUD, attribute filtering, row filtering, and secure serialization end to end.
- Route protection and authority-aware navigation in the standalone frontend.

**Should have (migration-critical differentiators):**
- Central secure data access through a project-native equivalent of `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java`.
- Attribute-level view/edit enforcement from `SecureEntitySerializerImpl.java` and `SecureMergeServiceImpl.java`.
- Row-level filtering and fetch-plan-driven secure reads from `RowLevelPolicyProviderDbImpl.java` and `fetch-plans.yml`.

**Defer (v2+):**
- Rebuilding broader legacy business modules from `angapp`.
- Any database-backed fetch-plan UI or `sec_fetch_plan` revival.
- Rich row-policy authoring beyond the currently implemented narrow specification subset.

### Architecture Approach

Use a two-lane backend. Keep the identity/admin lane intact around the current auth, account, user, and authority APIs, and add a separate secure business-data lane under `src/main/java/com/vn/core/security/**` for permission evaluation, row-policy specifications, fetch-plan resolution, secure serialization, and secure merge. Business resources should stay thin and call feature services, which in turn route secured entity reads and writes through the secure data pipeline rather than directly through repositories.

**Major components:**
1. Identity/admin API: preserve existing JWT, account, user, and authority contracts.
2. Secure data pipeline: centralize CRUD permission, row filtering, fetch-plan resolution, serialization, and guarded writes.
3. Security metadata layer: persist roles, permissions, and row policies for runtime evaluation.
4. Standalone frontend: reuse `aef-main` auth/layout patterns and add security-admin plus protected-entity screens only after backend contracts are stable.

### Critical Pitfalls

1. **Authority bridge drift**: keep `jhi_authority` as the runtime source, preserve `ROLE_ADMIN`, and explicitly map `sec_role.code` into granted authorities.
2. **Secure-path bypass**: forbid secured entity controllers/services from reading or writing through repositories directly; route them through the secure facade.
3. **Half-implemented row policies**: do not expose row-policy UI as complete until sample entities, seed data, and tests prove the ownership model actually works.
4. **Incomplete secure writes**: create and association updates need the same enforcement story as read/update paths before DTO reduction or broad UI rollout.
5. **Fetch-plan drift**: remove dead `sec_fetch_plan` lineage from the merged baseline and treat fetch-plan access control as incomplete until enforced in code.

## Implications for Roadmap

### Phase 1: Identity and metadata baseline
**Rationale:** Everything else depends on a stable authority bridge and clean schema.
**Delivers:** Preserved auth/admin APIs, merged Liquibase baseline for roles/permissions/row policies, normalized permission targets, and explicit `jhi_authority` to `sec_role` mapping.
**Addresses:** Account parity, user management, authority management, role/permission foundations.
**Avoids:** Authority bridge drift, dead fetch-plan schema, dev/test-only seed dependence.

### Phase 2: Secure enforcement core
**Rationale:** Protected entity work should not start before shared enforcement primitives exist.
**Delivers:** `src/main/java/com/vn/core/security/**` packages for access, permission, row, fetch, serialize, merge, and secure data orchestration.
**Addresses:** Central secure data access, attribute enforcement, row filtering, fetch-plan reads.
**Avoids:** Repository bypass and fake row-security rollout.

### Phase 3: Proof entities and secured APIs
**Rationale:** The migration needs a narrow but real proof of secure CRUD before frontend expansion.
**Delivers:** `Department` and `Organization` plus supporting entities/repositories, secure services/resources, and tests for allowed/denied read-write paths.
**Addresses:** Protected sample entity CRUD and backend validation of the imported security model.
**Avoids:** Incomplete secure writes and premature DTO removal.

### Phase 4: Standalone frontend migration
**Rationale:** UI work is safest once auth/admin and secure entity contracts are real.
**Delivers:** `frontend/` shell from `aef-main`, account/admin parity, security-admin screens, protected entity pages, and hardened 401/403/404 flows.
**Addresses:** Route protection, security metadata administration, and end-to-end migration usability.
**Avoids:** Static-authority-only UI behavior and frontend/backend contract mismatch.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2:** Row-policy model and fetch-plan authorization are materially incomplete in the donor implementation.
- **Phase 4:** Capability-aware frontend authorization needs design beyond `ROLE_ADMIN` and `ROLE_USER`.

Phases with standard patterns:
- **Phase 1:** Root auth/admin preservation and Liquibase merge follow established local patterns.
- **Phase 3:** Secure CRUD proof flows can follow the architecture already demonstrated in `angapp` once Phase 2 primitives exist.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Strongly grounded in local build files and runtime code. |
| Features | HIGH | Directly supported by current backend endpoints and working `angapp` screens. |
| Architecture | MEDIUM-HIGH | The lane split is well supported, but package adaptation to the root app is still an implementation step. |
| Pitfalls | MEDIUM | Risks are concrete, but row-policy and fetch-plan authorization behavior still need proof in the merged codebase. |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- Row-policy semantics need a concrete ownership model and production-ready seed/test coverage before they can be treated as complete.
- Fetch-plan `APPLY` authorization appears under-specified in the donor code and should be validated during Phase 2 planning.
- Frontend capability mapping for security-admin and protected business actions needs an explicit design instead of reusing only coarse authorities.

## Sources

- `.planning/research/STACK.md`
- `.planning/research/FEATURES.md`
- `.planning/research/ARCHITECTURE.md`
- `.planning/research/PITFALLS.md`
- `src/main/java/com/vn/core/web/rest/AuthenticateController.java`
- `src/main/java/com/vn/core/web/rest/AccountResource.java`
- `src/main/java/com/vn/core/web/rest/UserResource.java`
- `angapp/src/main/java/com/mycompany/core/data/SecureDataManagerImpl.java`
- `angapp/src/main/java/com/mycompany/core/merge/SecureMergeServiceImpl.java`
- `angapp/src/main/java/com/mycompany/core/serialize/SecureEntitySerializerImpl.java`
- `angapp/src/main/resources/fetch-plans.yml`
- `aef-main/aef-main/src/app/core/auth/account.service.ts`
- `aef-main/aef-main/src/app/layout/component/menu/app.menu.ts`

---
*Research completed: 2026-03-21*
*Ready for roadmap: yes*
