# Roadmap: JHipster Security Platform

## Overview

This roadmap delivers the brownfield migration in dependency order: preserve the current auth/admin lane, establish merged security metadata and authority bridging, enforce secure data access in the backend, prove it on sample entities, and only then ship the standalone Angular frontend that consumes those stable contracts.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Identity And Authority Baseline** - Preserve current account/admin behavior while bridging existing authorities into the merged security runtime. (completed 2026-03-21)
- [x] **Phase 2: Security Metadata Management** - Deliver backend administration for merged roles, permission rules, and supported row policies. (completed 2026-03-21)
- [ ] **Phase 3: Secure Enforcement Core** - Route protected data through centralized CRUD, row-level, attribute-level, and fetch-plan enforcement.
- [ ] **Phase 4: Protected Entity Proof** - Prove the merged engine on sample entities with secured APIs and automated allow/deny coverage.
- [ ] **Phase 5: Standalone Frontend Delivery** - Ship the Angular app with auth, security administration, and protected entity screens.

## Phase Details

### Phase 1: Identity And Authority Baseline
**Goal**: Existing authentication, account, and admin behavior survives the migration and existing authority assignments feed the merged security engine correctly.
**Depends on**: Nothing (first phase)
**Requirements**: AUTH-02, AUTH-03, SEC-04
**Success Criteria** (what must be TRUE):
  1. User can still register, activate an account, reset a forgotten password, and change password after the migration without flow regressions.
  2. Admin can still manage users and base authorities without regressing the current backend behavior.
  3. Runtime access decisions reflect admin-managed authority assignments through the merged security engine instead of a disconnected role store.
**Plans:** 2/2 plans complete
Plans:
- [x] 01-01-PLAN.md — Regression test baseline for account lifecycle and admin user management
- [x] 01-02-PLAN.md — SecurityContextBridge interface, default implementation, and unit/integration tests

### Phase 2: Security Metadata Management
**Goal**: Admin can manage the merged security metadata that drives runtime authorization decisions.
**Depends on**: Phase 1
**Requirements**: SEC-01, SEC-02, SEC-03
**Success Criteria** (what must be TRUE):
  1. Admin can create, update, list, and delete merged security roles through stable backend contracts.
  2. Admin can create, update, list, and delete permission rules for entity CRUD and attribute view/edit actions through stable backend contracts.
  3. Admin can create, update, list, and delete supported row policies through stable backend contracts.
**Plans:** 4/4 plans complete
Plans:
- [x] 02-01-PLAN.md — Liquibase schema evolution, JPA entities, enums, and repositories
- [x] 02-02-PLAN.md — DTOs, MapStruct mappers, MergedSecurityService, and MergedSecurityContextBridge
- [x] 02-03-PLAN.md — Admin REST controllers (roles, permissions, row policies) and seed data
- [x] 02-04-PLAN.md — Integration tests for all three admin endpoints

### Phase 3: Secure Enforcement Core
**Goal**: Protected business data is enforced consistently through one security-aware access path using merged permissions, row policies, and fetch plans.
**Depends on**: Phase 2
**Requirements**: DATA-01, DATA-02, DATA-03, DATA-04, DATA-05
**Success Criteria** (what must be TRUE):
  1. Secured business entity reads and writes go through a central security-aware data access layer rather than bypassing enforcement.
  2. Secured read responses are shaped by fetch plans defined in YAML or code builders, not by database-stored fetch-plan metadata.
  3. Users only receive attributes they are allowed to view in secured read payloads.
  4. Unauthorized attribute changes are rejected or stripped before persistence.
  5. Row-level policies constrain which records a user can read, update, and delete for secured entities.
**Plans:** 1/5 plans executed
Plans:
- [x] 03-01-PLAN.md — Interface contracts: access pipeline, catalog, data managers, fetch plan types, permission/merge/row/serialize interfaces
- [ ] 03-02-PLAN.md — Permission evaluators, row-policy provider, fetch-plan stack, repository registry, and catalog default
- [ ] 03-03-PLAN.md — Secure serializer (attribute filtering) and secure merge service (write enforcement)
- [ ] 03-04-PLAN.md — SecureDataManager and UnconstrainedDataManager implementations
- [ ] 03-05-PLAN.md — Unit tests for all enforcement components

### Phase 4: Protected Entity Proof
**Goal**: The merged security engine is proven against real sample entities and verified by backend allow/deny coverage.
**Depends on**: Phase 3
**Requirements**: ENT-01, ENT-02
**Success Criteria** (what must be TRUE):
  1. Sample protected entities exist and exercise CRUD, row-level, and attribute-level security end to end.
  2. Secured sample-entity APIs demonstrate both allowed and denied behaviors against real scenarios.
  3. Automated backend tests cover allow and deny paths for secured entity reads and writes.
**Plans**: TBD

### Phase 5: Standalone Frontend Delivery
**Goal**: A standalone Angular frontend exposes the migrated auth and security-management experience and proves protected-entity behavior end to end.
**Depends on**: Phase 4
**Requirements**: AUTH-01, ENT-03, UI-01, UI-02, UI-03
**Success Criteria** (what must be TRUE):
  1. A standalone Angular app exists under `frontend/` and follows the `aef-main/aef-main` structural direction.
  2. User can log in from the standalone frontend and the app handles authenticated state, route protection, and expected 401/403/404 flows correctly.
  3. Admin can manage merged roles, permission rules, and row policies from the frontend end to end.
  4. Sample protected-entity screens show only the actions and fields the current user is allowed to access.
**Plans**: TBD

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Identity And Authority Baseline | 2/2 | Complete   | 2026-03-21 |
| 2. Security Metadata Management | 4/4 | Complete   | 2026-03-21 |
| 3. Secure Enforcement Core | 1/5 | In Progress|  |
| 4. Protected Entity Proof | 0/TBD | Not started | - |
| 5. Standalone Frontend Delivery | 0/TBD | Not started | - |
