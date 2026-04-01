# Roadmap: JHipster Security Platform

## Milestones

- [shipped] **v1.0 MVP** - Phases 1-5 shipped 2010-03-25. Archive: `.planning/milestones/v1.0-ROADMAP.md`
- [active] **v1.1 Enterprise Admin Experience** - Phases 6-11 planned. Focus: user management parity, backend-driven navigation, Jmix-style enterprise UX, security-core realignment, performance hardening, and frontend reliability.

## Progress

| Milestone | Phases | Requirements | Status | Archive |
|-----------|--------|--------------|--------|---------|
| v1.0 MVP | 1-5 | 18/18 complete | Shipped 2010-03-25 | `.planning/milestones/v1.0-ROADMAP.md` |
| v1.1 Enterprise Admin Experience | 6-10 | 16/21 complete | Ready for Review | - |

## Overview

This roadmap builds on the shipped security platform by first closing the missing JHipster frontend parity gap, then moving navigation and permissions to backend-driven contracts, then delivering full admin user management, then inserting a Jmix-style internal data-access alignment step, then extending menu-role assignment across multiple apps while moving secured entity endpoints to a raw JSON boundary, then inserting a security realignment phase for self-registration, live permission refresh, entity-native secured data flow, validation hardening, and row-policy retirement before the final UX, performance, and regression-hardening work.

## Phases

**Phase Numbering:**
- Integer phases continue across milestones by default.
- `v1.0` ended at Phase 5, so `v1.1` starts at Phase 6.

- [x] **Phase 6: Frontend Parity Foundation** - Copy and adapt the required `angapp` support files, translations, and shared admin/account infrastructure into `frontend/`. (completed 2010-03-25)
- [x] **Phase 7: Enterprise Navigation Shell** - Replace hardcoded navigation with backend-driven menu and permission-aware route control inside a Jmix-style shell. (completed 2010-03-25)
- [x] **Phase 07.1: Menu Management** - Add admin CRUD for menu definitions and role-based menu access assignment. (completed 2010-03-25)
- [x] **Phase 8: User Management Delivery** - Deliver the full frontend admin user-management surface, including role assignment. (completed 2010-03-25)
- [x] **Phase 08.1: Jmix-Style DataManager Core Alignment** - Align the internal security data-access core with a Jmix-style `DataManager` / `UnconstrainedDataManager` split while preserving the current `SecureDataManager` boundary. (completed 2010-03-26)
- [x] **Phase 08.2: Multi-App Menu Roles and Jmix-Style JSON Entity Controllers** - Extend app-scoped menu authorization to multi-app role assignment and move secured entity endpoints to raw JSON with preserved `loadByQuery` and explicit `PATCH`. (completed 2010-03-27)
- [x] **Phase 08.3: User Registration, Live Permission Refresh, Entity-Native Serialization, Validation Hardening, And Row Policy Removal** - Add frontend registration, make permissions refresh without re-login, move secured data flow toward typed entities, harden JSON validation, and retire row policy completely. (completed 2010-03-27)
- [x] **Phase 9: Enterprise UX And Performance Hardening** - Improve consistency, responsiveness, data-fetch efficiency, and route-level loading costs. (completed 2010-03-28)
- [x] **Phase 10: Performance Benchmarking and OpenAPI Documentation** - Quantify security pipeline overhead with k6 load tests and document API surface with OpenAPI annotations. (completed 2010-03-31)

## Phase Details

### Phase 6: Frontend Parity Foundation
**Goal**: `frontend/` contains the JHipster support files and i18n foundations required for the next admin and shell work instead of relying on partial ad hoc copies.
**Depends on**: Phase 5
**Requirements**: I18N-01, I18N-02
**Success Criteria** (what must be TRUE):
1. Required JHipster support files and translation assets for in-scope flows exist in `frontend/` and are wired into the Angular runtime.
2. Shared frontend services and utilities for language, alerts, request helpers, and admin or user-management foundations are aligned with the migrated shell needs.
3. The frontend can render core migrated shell and user-management UI text from copied translation assets without fallback gaps.
**Plans:** 6/6 plans complete
Plans:
- [x] 06-01-PLAN.md - Standalone i18n bootstrap, merged vi or en bundles, route-title translation, and locale precedence
- [x] 06-02-PLAN.md - Visible translated alerts, backend alert-key normalization, and donor-compatible authority helpers
- [x] 06-03-PLAN.md - Typed request or pagination helpers plus the preserved user-management model, service, and route foundation
- [x] 06-04-PLAN.md - Translated shell, login, and error surfaces with a persisted vi or en language switcher
- [x] 06-05-PLAN.md - Root or admin route mounting and route reachability coverage for the migrated foundation
- [x] 06-06-PLAN.md - Close the remaining UAT gaps for admin discoverability and locale-aware MessageService feedback

### Phase 7: Enterprise Navigation Shell
**Goal**: Navigation, route protection, and enterprise shell behavior are driven by backend-aware contracts rather than hardcoded client assumptions.
**Depends on**: Phase 6
**Requirements**: ROUTE-01, ROUTE-02, ROUTE-03, UI-04
**Success Criteria** (what must be TRUE):
1. Menu and section visibility come from backend-driven navigation or capability data instead of a hardcoded menu array.
2. Unauthorized routes redirect or deny before component content renders, and unauthorized menu entries never appear.
3. Admin and entity areas load through distinct lazy route boundaries rather than forcing the whole app shell to load eagerly.
4. The shell uses enterprise master-detail navigation patterns consistently across admin and secured-entity sections.
**Plans:** 5/5 plans complete

### Phase 07.1: Menu Management (INSERTED)
**Goal**: Admin CRUD for `SecMenuDefinition` catalog entries and role-based `SecMenuPermission` assignment via a dedicated menu definitions page and a Menu Access tab on the role permissions screen.
**Requirements**: MENU-01, MENU-02, MENU-03, MENU-04, MENU-05
**Depends on**: Phase 7
**Plans:** 3/3 plans complete
Plans:
- [x] 07.1-01-PLAN.md - Backend admin REST controllers (`SecMenuDefinition` CRUD plus sync, admin menu-permission assign and revoke) with integration tests
- [x] 07.1-02-PLAN.md - Frontend Menu Definitions list-plus-dialog page with route, nav node, and i18n
- [x] 07.1-03-PLAN.md - Menu Access tab on permission matrix with tree checkbox grants

### Phase 8: User Management Delivery
**Goal**: The new frontend exposes the full JHipster-style admin user-management experience on top of the preserved backend contracts.
**Depends on**: Phase 7
**Requirements**: UMGT-01, UMGT-02, UMGT-03
**Success Criteria** (what must be TRUE):
1. Admin can list, search, sort, and open users from the frontend using the existing backend admin APIs.
2. Admin can create, edit, activate, deactivate, and delete users without regressing the current contract or validation behavior.
3. Admin can assign and update user roles or authorities, and persisted changes affect access outcomes in the app.
**Plans:** 4/4 plans complete
Plans:
- [x] 08-01-PLAN.md - Add the optional `query` seam to the preserved admin browse endpoint without changing the existing contract shape
- [x] 08-02-PLAN.md - Deliver the query-aware list workspace, inline admin actions, preserved list context, and read-only split-page detail route
- [x] 08-03-PLAN.md - Deliver the shared split-page create/edit form with inline authority assignment and the first authority-save smoke
- [x] 08-04-PLAN.md - Prove grant-and-revoke access outcomes and close the final user-management verification gate

### Phase 08.1: Jmix-Style DataManager Core Alignment (INSERTED)
**Goal**: Align the internal data-access architecture with a Jmix-style secure-default `DataManager` core while preserving the current application-facing `SecureDataManager` contract and brownfield behavior.
**Outcome**: Protected application services keep using `SecureDataManager`, but internally secured reads and writes flow through a new `DataManager` that extends `UnconstrainedDataManager`, with `unconstrained()` as the explicit bypass path and resource permission merging aligned to default deny plus union-of-`ALLOW`.
**Depends on**: Phase 8
**Requirements**: Cross-cutting security architecture alignment with no frontend or brownfield API contract breakage
**Scope**:
1. Analyze and refactor `src/main/java/com/vn/core/security/data/**` so raw CRUD, query, load, save, and delete mechanics live in `UnconstrainedDataManagerImpl`, a new internal `DataManager` extends `UnconstrainedDataManager`, and `DataManagerImpl` centralizes authorization plus registered access-constraint application.
2. Keep `SecureDataManager` and `SecureDataManagerImpl` as the stable application-facing facade responsible for entity-code resolution, fetch-plan-driven serialization, map payload handling, secure merge orchestration, and protected-boundary row-level orchestration.
3. Align resource permission evaluation and menu permission merging with Jmix-style default deny plus union-of-`ALLOW` semantics for entity, attribute, menu, and similar resource permissions, while keeping row-level policies as a separate enforcement layer.
4. Update tests around permission accumulation, menu or resource merging, row-level restrictions, and explicit unconstrained bypass behavior without regressing auth, account, admin, or mail flows.
**Risks**:
1. Permission-semantic changes can widen access accidentally if entity, attribute, menu, and capability evaluators are not migrated together.
2. Moving authorization into `DataManagerImpl` can regress row-level filtering or fetch-plan shaping if secure and unconstrained responsibilities blur.
3. Existing tests pinned deny-wins behavior in multiple locations, so incomplete replacement could leave mixed semantics and hard-to-diagnose regressions.
**Success Criteria** (what must be TRUE):
1. There is a new internal `DataManager` abstraction that extends `UnconstrainedDataManager` and exposes `unconstrained()` for explicit bypass access.
2. `DataManagerImpl` reuses `UnconstrainedDataManagerImpl` mechanics and becomes the default authorization-enforcing path for internal secured operations.
3. `SecureDataManager` remains the stable application-facing facade and its current frontend or API-facing contract does not break.
4. Resource permissions accumulate `ALLOW` across multiple roles without a global deny-wins merge strategy for menu or similar resource-role permissions unless a specific deviation is documented.
5. Row-level restrictions continue to filter protected reads or writes correctly, while unconstrained access bypasses authorization by design.
6. Tests prove multi-role `ALLOW` accumulation, menu or resource union behavior, row-level enforcement, unconstrained bypass, and no regression in existing auth, account, admin, or mail flows.
**Plans:** 3/3 plans complete
Plans:
- [x] Recommended split: architecture alignment and interface extraction
- [x] Recommended split: secure or unconstrained refactor plus permission-semantic migration
- [x] Recommended split: regression and behavior verification

### Phase 08.2: Multi-App Menu Roles and Jmix-Style JSON Entity Controllers (INSERTED)
**Goal**: Restore the missing secured query capability while extending app-scoped menu authorization to clean multi-app role assignment and refactoring secured entity endpoints to a Jmix-style raw JSON boundary with explicit PATCH support.
**Outcome**: Roles can hold menu grants for multiple apps without losing per-app resolution, and protected entity controllers expose raw JSON request or response flows for load, query, save, and patch operations while preserving `loadByQuery`, partial-update semantics, and the existing security enforcement stack.
**Depends on**: Phase 08.1
**Requirements**: Cross-cutting menu authorization, secured-entity API alignment, and documented brownfield-safe contract evolution
**Scope**:
1. Extend the backend menu-permission model, repositories, admin APIs, and role-management flows so one role can hold app-scoped menu permissions for multiple apps without collapsing app boundaries or regressing backend-driven per-app menu resolution.
2. Refactor protected entity controller and service boundaries away from `Map<String, Object>` request signatures toward Jmix-style raw JSON request or response handling for list, loadOne, create, update, delete, and secured query-based loading flows.
3. Restore and preserve `loadByQuery` as a first-class secured capability end to end, including controller exposure, service orchestration, and secure data-layer enforcement rather than leaving it as an internal or degraded path.
4. Add explicit `PATCH` support for partial updates using raw JSON bodies that apply only provided fields, leave omitted fields untouched, and enforce attribute-level `EDIT` security on every patched field while keeping CRUD checks intact.
5. Update verification coverage and document any intentional API contract changes, especially the move to raw JSON entity boundaries and the addition of PATCH, without regressing auth, account, admin, or mail flows or the existing backend-driven navigation model.
**Risks**:
1. Multi-app menu-role assignment can accidentally flatten app scoping and leak one app's grants into another app's navigation resolution.
2. Replacing controller `Map<String, Object>` boundaries with raw JSON can silently break validation, serialization, or brownfield consumers if the contract shift is not documented and regression-tested.
3. Restoring query-based loading while refactoring the secured boundary can reintroduce the Phase 08.1 `loadByQuery` regression or weaken security if query or search flows bypass the established secure pipeline.
4. PATCH on associations or nested collections can cause unintended removals or security gaps if partial-update semantics diverge from the current secure merge behavior.
**Success Criteria** (what must be TRUE):
1. A single role can hold menu permissions for multiple apps in backend storage and admin APIs, while current-user menu resolution still returns only the requested app's allowed menu ids.
2. Admin flows can inspect and manage menu grants for a role across apps without duplicating role records or abandoning app-scoped menu permission semantics.
3. Protected entity controllers use a Jmix-style raw JSON boundary for create, update, and query operations instead of exposing `Map<String, Object>` request signatures.
4. `loadByQuery` is restored and verified end to end as a first-class secured capability, not just a leftover internal method.
5. PATCH accepts raw JSON request bodies, updates only provided fields, preserves omitted fields, and continues to enforce CRUD, row-level, attribute-level, and fetch-plan security on protected entities.
6. Brownfield auth, account, admin, and mail flows and backend-driven navigation do not regress, and any intentional API contract changes are documented before implementation handoff.
**Plans:** 4/4 plans complete

### Phase 08.3: User Registration, Live Permission Refresh, Entity-Native Serialization, Validation Hardening, And Row Policy Removal (INSERTED)
**Goal**: Restore missing self-registration in the standalone frontend, make authority changes take effect without logout or login, move the secured entity path toward typed entity-native flows, make JSON validation explicit and fail closed, and remove row policy completely from the product.
**Outcome**: The platform keeps the preserved backend registration contract while adding a real frontend register flow, request-time authorization surfaces observe updated DB authorities without forcing re-authentication, secure entity operations work on real entities internally instead of map or string carriers, JSON controllers validate explicitly, and row-policy code is removed from backend, schema, admin APIs, frontend UI, and tests.
**Depends on**: Phase 08.2
**Requirements**: PH10-01, PH10-02, PH10-03, PH10-04, PH10-05
**Scope**:
1. Re-check the existing backend registration flow and add the missing standalone frontend registration route, service, form, translations, and tests using `angapp` as the donor where it already matches the preserved backend contract.
2. Refactor authentication so request-time authorities come from current database state instead of the authority snapshot frozen into the login token or cached frontend account state, while keeping JWT as the identity carrier.
3. Replace map, string, and `JsonNode`-centric secured entity internals with a typed entity-native flow around `DataManager` or `UnconstrainedDataManager`, introducing an explicit serialization or deserialization boundary only where HTTP JSON enters or leaves the system.
4. Harden JSON-based secured controllers so payload validation is explicit, strict, and fail closed even without DTO-only boundaries, including malformed bodies, unknown fields, invalid references, and unsafe query shapes.
5. Remove row policy end to end across schema, runtime, admin APIs, frontend, tests, and supporting enums or providers so no remaining path depends on it.
**Risks**:
1. Request-time authority refresh can weaken security or break JWT identity handling if principal replacement is partial or inconsistent across controllers, method security, and current-user helper services.
2. A typed secured-entity rewrite can balloon if it tries to redesign every HTTP contract instead of confining JSON parsing and serialization to an explicit edge layer.
3. Validation hardening can break existing clients if unknown-field and malformed-input rejection is introduced without updating controller or service expectations consistently.
4. Row-policy removal touches schema, tests, enums, and admin UI simultaneously; deleting it too early can strand current secured-entity flows before typed replacements and validation are stable.
5. Frontend permission, navigation, and capability caches can keep stale state even after backend live-refresh work lands unless cache invalidation is part of the same phase.
**Success Criteria** (what must be TRUE):
1. The standalone `frontend/` app exposes a working user registration flow on top of the preserved `/api/register` backend behavior.
2. Updating a user's authorities in the database changes current-user permission outcomes, menu visibility, and secured-entity capabilities without requiring logout or login.
3. The secured entity data path operates on real typed entities internally and no longer depends on `Map<String, Object>`, raw `String`, or `JsonNode` as the core security representation.
4. JSON-based secured controllers reject malformed, unsafe, or invalid payloads explicitly instead of relying on incidental behavior.
5. Row-policy code, schema, admin UI, and tests are removed completely, and no surviving logic depends on row-policy providers or enums.
6. Brownfield auth, account, admin, and mail flows remain green after the refactor.
**Plans:** 5/5 plans complete
Plans:
- [x] 08.3-01-PLAN.md - Frontend registration parity on top of the preserved backend register flow
- [x] 08.3-02-PLAN.md - Request-time authority refresh plus frontend cache invalidation for live permission updates
- [x] 08.3-03-PLAN.md - Typed entity-native secured data flow with explicit JSON serialization and deserialization edges
- [x] 08.3-04-PLAN.md - Explicit validation hardening for JSON entity controllers and secured query payloads
- [x] 08.3-05-PLAN.md - Complete row-policy retirement across schema, backend, frontend, and tests

### Phase 9: Enterprise UX And Performance Hardening
**Goal**: The richer enterprise frontend stays consistent, responsive, and efficient under realistic admin usage.
**Depends on**: Phase 08.3
**Requirements**: UI-05, PERF-01, PERF-02, PERF-03
**Success Criteria** (what must be TRUE):
1. Key admin and entity screens share a consistent responsive layout plus predictable loading, empty, and feedback states.
2. Redundant calls for auth, menu, capability, and user-management data are removed or cached safely.
3. Initial load and route transitions improve through lazy loading, code splitting, and leaner route-level bundles.
4. Larger enterprise screens remain responsive under realistic data volume through efficient rendering and state updates.
**Plans:** 3/3 plans complete
Plans:
- [x] 09-01-PLAN.md — Backend permission caching with request-scoped snapshot bean (PERF-01, PERF-02)
- [x] 09-02-PLAN.md — Frontend entity list hardening: pagination signals, skeleton loaders, responsive columns (PERF-03, UI-05)
- [x] 09-03-PLAN.md — First-paint skeleton loader fix for department, employee, and organization entity lists

### Phase 10: Performance Benchmarking and OpenAPI Documentation for the Security Pipeline

**Goal**: Quantify the latency overhead of the @SecuredEntity pipeline with k6 load tests at multiple concurrency levels, and annotate the project-specific API surface with accurate OpenAPI documentation including response schemas, fetch-plan descriptions, and a machine-readable x-secured-entity extension.
**Requirements**: BENCH-01, OPENAPI-01
**Depends on:** Phase 9
**Success Criteria** (what must be TRUE):
1. k6 benchmark scripts compare secured vs unsecured Organization endpoints at 1, 10, and 50 virtual users and produce a Markdown report with p95 overhead calculation against the 10% target.
2. A profile-gated baseline endpoint serves Organization data via UnconstrainedDataManager (bypassing the security pipeline) for fair comparison.
3. All five in-scope resource controllers (Organization, Department, Employee, SecuredEntityCapability, MenuPermission) have @Tag, @Operation, and @ApiResponse annotations with fetch-plan descriptions and correct schema types.
4. An OperationCustomizer bean marks all @SecuredEntity-backed operations with x-secured-entity: true in the generated OpenAPI spec.
**Plans:** 4/4 plans complete

Plans:
- [x] 10-01-PLAN.md — k6 benchmark infrastructure: baseline controller, load test scripts, and README
- [x] 10-02-PLAN.md — OpenAPI annotations: OperationCustomizer bean and @Operation/@ApiResponse on 5 controllers
- [x] 10-03-PLAN.md — Refactor benchmark baseline to standard JHipster flow (gap closure)
- [x] 10-04-PLAN.md — Phase 10 requirements traceability alignment (gap closure)

### Phase 11: Security Pipeline Performance Hardening — p95 Overhead Under 10 Percent

**Goal:** Reduce the secured endpoint p95 latency overhead to under 10% of the baseline by eliminating redundant DB round-trips in permission evaluation, fixing the Criteria API ID lookup inefficiency on detail loads, reducing per-response fetch-plan and serializer overhead, and proving the outcome with a persisted Phase 10 benchmark rerun.
**Requirements**: PERF-04
**Depends on:** Phase 10
**Plans:** 2/2 plans complete

Plans:
- [x] 11-01-PLAN.md — Cache PermissionMatrix by JWT authority set, trust JWT names directly, and evict on SecPermission service-layer writes
- [x] 11-02-PLAN.md — Remove secure detail id-spec overhead, switch serializer property access to ObjectReader, and persist benchmark rerun results
