# JHipster Security Platform

## What This Is

This is a brownfield JHipster security-platform migration that already shipped a standalone Angular frontend and merged security runtime in `v1.0`. `v1.1` carried a security-core realignment in Phase 08.3, then continues with enterprise UX, performance, and regression hardening in Phases 9 and 10.

## Core Value

Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, authority, and attribute-level access decisions.

## Current Milestone: v1.1 Enterprise Admin Experience

**Goal:** Deliver an enterprise-grade frontend administration milestone without weakening the platform's security guarantees, now that Phase 08.3 has already repaired the remaining auth and secured-data foundations that Phase 9 depends on.

**Target features:**
- Full admin user-management flows in `frontend/`, including create, update, delete, activate, deactivate, and role assignment.
- A real standalone frontend registration flow on top of the preserved backend `/api/register` behavior.
- Live permission refresh so changed authorities affect current-user outcomes without forcing logout or login.
- A typed secured-entity flow with explicit JSON serialization or deserialization edges and strict JSON validation.
- Complete row-policy retirement across schema, backend, frontend, and tests.
- A Jmix-style enterprise shell with better responsiveness, consistent interaction design, performance optimizations, and focused frontend coverage.

## Current State

`v1.0 MVP` shipped on 2026-03-25 and established the shared security platform.

The repository now includes:

- The original Spring Boot or JHipster auth, account, admin-user, mail, and authority flows preserved without regression.
- Merged security metadata administration for roles, permissions, menus, and secured entities.
- A secure data-access layer centered on `SecureDataManager`, with CRUD, attribute-level, request-time authority refresh, and YAML or code-defined fetch-plan enforcement after the Phase 08.3 cleanup.
- Proof-domain entities and APIs for Organization, Department, and Employee that verify allow or deny behavior end to end.
- A standalone Angular frontend under `frontend/` for login, route protection, security administration, and protected-entity screens.

**Phase 7 complete** - Backend-driven navigation shell with menu filtering, route guards, breadcrumbs, access-denied recovery, and workspace context preservation. 12/12 must-haves verified.

**Phase 8 complete** - Full frontend admin user management now ships with browse, detail, create, edit, activation, deletion, inline authority assignment, and verified grant or revoke access outcomes. 10/10 must-haves verified.

**Phase 08.2 complete** - Secured entity endpoints now expose explicit raw-JSON `PATCH` support, secured `/query` stays first-class, role-centric menu assignment can create the first grant in a new app, and the brownfield backend safety sweep is green under Java 25.

**Phase 08.3 complete** - The standalone frontend now has registration parity, current-user authorities refresh from database state without re-authentication, secured entity internals operate through typed entity mutations behind explicit JSON adapters, validation is fail-closed, and row policy is fully retired.

The next part of `v1.1` now starts from this context:

- The backend and standalone frontend now share the preserved registration contract end to end.
- Current-user menu and capability state can refresh without forcing logout or login, so Phase 9 can focus on responsiveness and redundant work reduction instead of stale-auth repair.
- The secured entity pipeline now uses typed entity mutations internally with explicit JSON parsing and serialization edges.
- Validation hardening and row-policy retirement are complete, so the remaining milestone work is UX, performance, and regression coverage.

## Requirements

### Validated

- AUTH-01 through AUTH-03 shipped in `v1.0`, covering standalone frontend login plus preserved backend account and admin-user behavior.
- SEC-01 through SEC-04 shipped in `v1.0`, covering merged security metadata CRUD and authority-bridge integration.
- DATA-01 through DATA-05 shipped in `v1.0`, covering secured reads, secured writes, attribute permissions, and fetch-plan enforcement.
- ENT-01 through ENT-03 shipped in `v1.0`, covering proof entities, backend allow or deny coverage, and frontend capability-driven screens.
- UI-01 through UI-03 shipped in `v1.0`, covering the standalone Angular app, security-management UI, and route or error or auth handling.
- UMGT-01 through UMGT-03 validated in Phase 8: User Management Delivery, covering browse or search, full admin CRUD, inline authority assignment, and downstream access effects from saved authority changes.
- PH83-01 through PH83-05 validated in Phase 08.3, covering standalone registration, live-authority refresh, typed secured flow, explicit JSON validation, and complete row-policy retirement.

### Active

- [ ] UI-05: Improve consistency, responsiveness, and master-detail workflows.
- [ ] PERF-01 through PERF-03 and TEST-01 through TEST-03: Optimize frontend loading or data access and add reliable automated coverage for the new admin experience.

### Out of Scope

- Database-backed fetch-plan metadata remains out of scope; fetch plans stay YAML or code-defined only.
- Literal full-app cloning of `angapp` remains out of scope; `v1.1` must migrate the required JHipster support files and in-scope user-management or frontend infrastructure into `frontend/`.
- Non-user-management legacy admin pages from `angapp`, such as health, metrics, logs, docs, and configuration, remain deferred unless the new shell needs shared support pieces from them.
- Any future replacement for row policy remains out of scope for this milestone; Phase 08.3 retires the current implementation rather than replacing it with a new policy system.

## Context

`v1.0` covered 5 phases, 30 plans, 43 documented tasks, 160 milestone commits, and roughly 359 files / 45,043 inserted lines across planning, backend, tests, and the new frontend. Final human UAT passed on 2026-03-25 with 3/3 checks green: login or session behavior, admin security-management persistence, and protected-entity gating.

Current context shaping `v1.1`:

- `angapp/` is still the canonical donor for missing account or registration flows plus shared support files that the standalone frontend has not finished migrating.
- `aef-main/aef-main/` remains the canonical frontend reference for `frontend/`, especially for standalone bootstrap, route or layout structure, and shared UI composition.
- PrimeNG Sakai remains the canonical frontend shell, layout, and component baseline; new frontend work should extend that system instead of introducing a competing layout language.
- Dynamic routing and permission-aware menus already use backend contracts, but current-user permission state still needs request-time refresh instead of login-time snapshots.
- The current PrimeNG shell must evolve toward an enterprise or Jmix-style navigation model without discarding the standalone Angular structure already shipped.
- Frontend test coverage must increase around registration, permission refresh, routing, and shared UI infrastructure because the next phases touch foundational client files.
- The secure entity stack now has the internal `DataManager` split but still needs typed serialization and deserialization boundaries plus stricter JSON validation.
- Row policy is no longer a target capability. It is a cleanup target.

## Constraints

- **Compatibility**: Preserve the functional security capabilities already working in `angapp` for entity CRUD checks, attribute permissions, secure merge behavior, and fetch-plan-driven secure reads. Existing row-policy code is not a preservation target; Phase 08.3 retires it.
- **Frontend structure**: The new UI must live in a standalone `frontend/` app modeled after `aef-main/aef-main`; `aef-main/aef-main` is the canonical frontend reference and PrimeNG Sakai is the canonical layout or component shell, with JHipster-style Angular structure layered on top.
- **PrimeNG-first UI**: Frontend work must use official PrimeNG components and current `https://primeng.org/` examples or best practices whenever a suitable component exists; custom UI is allowed only for layout composition or gaps where PrimeNG has no suitable component.
- **Fetch plans**: Fetch plans must be defined in YAML or code builders only. Database storage for fetch-plan definitions is not allowed.
- **Brownfield safety**: Existing authentication, account, admin-user, and mail flows in the current backend must not regress during the migration.
- **API boundary**: Existing JHipster account or user APIs may keep minimal boundary request or response models where dropping them would destabilize the public contract or validation model.
- **Migration source**: Required frontend support files should come from `angapp/` rather than ad hoc reinvention when a compatible donor implementation exists.
- **Enterprise UX**: Admin and entity screens should move toward a Jmix-style master-detail experience.
- **Performance**: Dynamic navigation and richer UI cannot rely on excessive API chatter or eager bundle loading.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Merge the security models into a project-native role or permission design | The project must preserve `angapp` behavior without forcing a brittle one-to-one schema copy | Confirmed in `v1.0` |
| Create a standalone Angular app under `frontend/` using `aef-main/aef-main` as the canonical frontend reference | The new client needed a clear Angular or PrimeNG foundation that still fit JHipster conventions | Shipped in `v1.0` |
| Keep PrimeNG Sakai as the canonical frontend shell, layout, and component baseline | Enterprise UX work needs a stable visual and structural system instead of multiple competing frontend patterns | Active in `v1.1` |
| Use `SecureDataManager` as the stable application-facing secured facade and `UnconstrainedDataManager` as the explicit bypass | Security enforcement had to stay centralized and explicit | Validated in phases 3-5 and 08.1 |
| Use proof-domain entities to validate the merged security engine end to end | The platform needed real sample entities to prove CRUD and attribute behavior | Validated in phase 4 |
| Use fetch plans from YAML and code builders only | Database-backed fetch-plan storage was explicitly disallowed by project constraints | Confirmed in `v1.0` |
| Remove DTOs incrementally rather than project-wide on day one | Existing JHipster user or account APIs still benefit from contract-protecting boundary models | Confirmed in `v1.0` |
| Use `angapp` as the canonical donor for user-management, registration, i18n, and shared support files | The standalone frontend still has migration gaps in account-facing flows | Active in `v1.1` |
| Replace hardcoded frontend menus with backend-driven navigation contracts | Enterprise role-based navigation must scale with backend-managed permissions | Validated in Phase 7 |
| Refresh current-user authorities from database state at request time instead of trusting the login-time snapshot in JWT or frontend caches | Permission changes must take effect without forcing logout or login | Validated in Phase 08.3 |
| Move secured entity internals toward typed entity-native flow and keep JSON parsing or serialization in explicit edge adapters | Map or string carriers were the main source of drift and validation gaps in the secured entity path | Validated in Phase 08.3 |
| Retire row policy completely instead of expanding it further | The current implementation was incomplete, broadened complexity, and blocked the simpler security model the project actually needs | Completed in Phase 08.3 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition**:
1. Requirements invalidated? -> Move to Out of Scope with reason
2. Requirements validated? -> Move to Validated with phase reference
3. New requirements emerged? -> Add to Active
4. Decisions to log? -> Add to Key Decisions
5. "What This Is" still accurate? -> Update if drifted

**After each milestone**:
1. Full review of all sections
2. Core Value check - still the right priority?
3. Audit Out of Scope - reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-03-28 after Phase 08.3 completion and Phase 9 handoff*
