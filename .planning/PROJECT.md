# JHipster Security Platform

## What This Is

This is a brownfield JHipster security-platform migration that already shipped a standalone Angular frontend and merged security runtime in `v1.0`. `v1.1` focuses on turning that foundation into an enterprise-ready admin experience by completing JHipster user-management parity, moving navigation and permissions to backend-driven contracts, adopting a Jmix-style shell, and hardening the frontend for i18n, performance, and automated coverage.

## Core Value

Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, row-level, and attribute-level access decisions.

## Current Milestone: v1.1 Enterprise Admin Experience

**Goal:** Deliver an enterprise-grade frontend administration milestone that completes user-management parity and backend-driven navigation without weakening the platform's existing security guarantees.

**Target features:**
- Full admin user-management flows in `frontend/`, including create, update, delete, activate, deactivate, and role assignment.
- A Jmix-style enterprise shell with master-detail navigation patterns, better responsiveness, and more consistent interaction design.
- Backend-driven menu loading and permission-protected routes, plus migrated JHipster support files, i18n assets, performance optimizations, and frontend test coverage.

## Current State

`v1.0 MVP` shipped on 2026-03-25 and established the shared security platform.

The repository now includes:

- The original Spring Boot/JHipster auth, account, admin-user, mail, and authority flows preserved without regression.
- Merged security metadata administration for roles, permissions, and supported row policies.
- A secure data-access layer centered on `SecureDataManager`, with CRUD, row-level, attribute-level, and YAML/code-defined fetch-plan enforcement.
- Proof-domain entities and APIs for Organization, Department, and Employee that verify allow/deny behavior end to end.
- A standalone Angular frontend under `frontend/` for login, route protection, security administration, and protected-entity screens.

**Phase 7 complete** — Backend-driven navigation shell with menu filtering, route guards, breadcrumbs, access-denied recovery, and workspace context preservation. 12/12 must-haves verified.

**Phase 8 complete** — Full frontend admin user management now ships with browse, detail, create, edit, activation, deletion, inline authority assignment, and verified grant/revoke access outcomes. 10/10 must-haves verified; the remaining backend integration rerun is environment-blocked on local Docker availability, not product behavior.

**Phase 08.2 complete** — Secured entity endpoints now expose explicit raw-JSON `PATCH` support, secured `/query` stays first-class, role-centric menu assignment can create the first grant in a new app, and the brownfield backend safety sweep is green under Java 25.

The next phase of `v1.1` now starts from this context:

- Phase 9 now starts from a stronger admin baseline: user-management parity is in place, secured query and PATCH contracts are explicit, and route-level access outcomes are covered by focused regression tests.
- The current `frontend/` menu is already backend-driven, so the remaining work shifts toward enterprise consistency, responsiveness, and performance hardening.
- Role-based menu assignment no longer depends on pre-existing grants in an app; the permission matrix can discover assignable apps from the menu-definition catalog and seed first grants cleanly.
- The next milestone pressure is no longer feature parity for user management; it is UX polish, bundle efficiency, and broader frontend reliability coverage.

## Requirements

### Validated

- AUTH-01 through AUTH-03 shipped in `v1.0`, covering standalone frontend login plus preserved backend account and admin-user behavior.
- SEC-01 through SEC-04 shipped in `v1.0`, covering merged security metadata CRUD and authority-bridge integration.
- DATA-01 through DATA-05 shipped in `v1.0`, covering secured reads, secured writes, row policies, attribute permissions, and YAML/code-only fetch plans.
- ENT-01 through ENT-03 shipped in `v1.0`, covering proof entities, backend allow/deny coverage, and frontend capability-driven screens.
- UI-01 through UI-03 shipped in `v1.0`, covering the standalone Angular app, security-management UI, and route/error/auth handling.
- UMGT-01 through UMGT-03 validated in Phase 8: User Management Delivery, covering browse/search, full admin CRUD, inline authority assignment, and downstream access effects from saved authority changes.

### Active

- [x] ROUTE-01 through ROUTE-03: Load menu and permission context from backend data and enforce access before route render. Validated in Phase 7: Enterprise Navigation Shell
- [ ] UI-04: Adopt a Jmix-style enterprise shell. Validated in Phase 7: Enterprise Navigation Shell (breadcrumbs, access-denied recovery, workspace context)
- [ ] UI-05: Improve consistency, responsiveness, and master-detail workflows.
- [ ] I18N-01 through I18N-02: Copy the required `angapp/` support files and translation assets for in-scope frontend features.
- [ ] PERF-01 through PERF-03 and TEST-01 through TEST-03: Optimize frontend loading/data access and add reliable automated coverage for the new admin experience.

### Out of Scope

- Database-backed fetch-plan metadata remains out of scope; fetch plans stay YAML/code-defined only.
- Literal full-app cloning of `angapp` remains out of scope; `v1.1` must migrate the required JHipster support files and in-scope user-management/frontend infrastructure into `frontend/`.
- Non-user-management legacy admin pages from `angapp`, such as health, metrics, logs, docs, and configuration, remain deferred unless the new shell needs shared support pieces from them.
- Unsupported row-policy designer variants remain out of scope until real use cases justify broadening the policy model.

## Context

`v1.0` covered 5 phases, 30 plans, 43 documented tasks, 160 milestone commits, and roughly 359 files / 45,043 inserted lines across planning, backend, tests, and the new frontend. Final human UAT passed on 2026-03-25 with 3/3 checks green: login/session behavior, admin security-management persistence, and protected-entity gating.

Current context shaping `v1.1`:

- `angapp/` is the canonical donor for the missing user-management, translation, and shared support files that the first frontend milestone left behind.
- The current PrimeNG shell must evolve toward an enterprise/Jmix-style navigation model without discarding the standalone Angular structure already shipped.
- Dynamic routing and permission-aware menus need backend contracts so enterprise admin scale does not depend on hardcoded client navigation.
- Frontend test coverage must increase around user management, routing, and shared UI infrastructure because this milestone will touch foundational client files.
- Remaining non-product debt is still process-oriented: validation strategy metadata for phases 1, 3, and 4 stayed draft even though the shipped verification evidence is complete.

## Constraints

- **Compatibility**: Preserve the functional security capabilities already working in `angapp` - entity CRUD checks, attribute permissions, row policies, secure merge behavior, and fetch-plan-driven secure reads must still work after the merge.
- **Frontend structure**: The new UI must live in a standalone `frontend/` app modeled after `aef-main/aef-main` - PrimeNG Sakai plus JHipster-style Angular structure.
- **PrimeNG-first UI**: Frontend work must use official PrimeNG components and current `https://primeng.org/` examples or best practices whenever a suitable component exists; custom UI is allowed only for layout composition or gaps where PrimeNG has no suitable component.
- **Fetch plans**: Fetch plans must be defined in YAML or code builders only - database storage for fetch-plan definitions is not allowed.
- **Brownfield safety**: Existing authentication, account, admin-user, and mail flows in the current backend must not regress during the migration.
- **API boundary**: Existing JHipster account/user APIs may keep minimal boundary request/response models where dropping them would destabilize the public contract or validation model.
- **Migration source**: Required frontend support files must come from `angapp/` rather than ad hoc reinvention - the previous partial copy left parity gaps that this milestone must close.
- **Enterprise UX**: Admin and entity screens should move toward a Jmix-style master-detail experience - the user explicitly wants enterprise-grade usability and consistency.
- **Performance**: Dynamic navigation and richer UI cannot rely on excessive API chatter or eager bundle loading - enterprise usage must stay responsive.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Merge the security models into a project-native role/permission design | The project must preserve `angapp` behavior without forcing a brittle one-to-one schema copy | Confirmed in `v1.0` |
| Create a standalone Angular app under `frontend/` using the `aef-main` structure as reference | The new client needed a clear Angular/PrimeNG foundation that still fit JHipster conventions | Shipped in `v1.0` |
| Use `SecureDataManager` as the single secured data path and `UnconstrainedDataManager` as the trusted bypass | Security enforcement had to stay centralized and explicit | Validated in phases 3-5 |
| Use proof-domain entities to validate the merged security engine end to end | The platform needed real sample entities to prove CRUD, row, and attribute behavior | Validated in phase 4 |
| Use fetch plans from YAML and code builders only | Database-backed fetch-plan storage was explicitly disallowed by project constraints | Confirmed in `v1.0` |
| Remove DTOs incrementally rather than project-wide on day one | Existing JHipster user/account APIs still benefit from contract-protecting boundary models | Confirmed in `v1.0` |
| Use PrimeNG official components as the default frontend vocabulary | The project needs consistent component behavior, accessibility, and implementation patterns across phases, with custom UI only for PrimeNG gaps | Pending in `v1.1` |
| Use `angapp` as the canonical donor for user-management, i18n, and shared support files | The previous frontend milestone shipped the new app without full JHipster parity | Pending in `v1.1` |
| Replace hardcoded frontend menus with backend-driven navigation contracts | Enterprise role-based navigation must scale with backend-managed permissions | Pending in `v1.1` |
| Rework admin flows toward a Jmix-style master-detail shell rather than isolated page tweaks | The requested UX shift is structural, not cosmetic | Pending in `v1.1` |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `$gsd-transition`):
1. Requirements invalidated? -> Move to Out of Scope with reason
2. Requirements validated? -> Move to Validated with phase reference
3. New requirements emerged? -> Add to Active
4. Decisions to log? -> Add to Key Decisions
5. "What This Is" still accurate? -> Update if drifted

**After each milestone** (via `$gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check - still the right priority?
3. Audit Out of Scope - reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-03-27 after Phase 08.2 completion*
