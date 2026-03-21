# JHipster Security Platform

## What This Is

This is a brownfield migration that evolves the current JHipster security service into a fuller security platform with a standalone Angular frontend. The target system must preserve the current backend auth and admin capabilities while absorbing the Jmix-style security behavior from `angapp`, including secure data access, row-level rules, attribute-level rules, and fetch-plan-driven reads.

## Core Value

Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, row-level, and attribute-level access decisions.

## Requirements

### Validated

- existing: JWT-based authentication and account lifecycle flows already exist in the current backend (`/api/authenticate`, registration, activation, password reset, password change)
- existing: Admin user and authority management APIs already exist in the current backend
- existing: Email-based activation and password-reset support already exists
- existing: PostgreSQL- and Liquibase-backed Spring Boot/JHipster security service is already running in this repository
- AUTH-02: Password change validation and account lifecycle edge cases locked in via regression tests (Validated in Phase 01: identity-and-authority-baseline)
- AUTH-03: Non-admin access denials (403) for admin user and authority endpoints locked in via regression tests (Validated in Phase 01: identity-and-authority-baseline)
- SEC-01: Admin can create, update, list, and delete merged security roles through stable backend contracts (Validated in Phase 02: security-metadata-management)
- SEC-02: Admin can create, update, list, and delete permission rules through stable backend contracts (Validated in Phase 02: security-metadata-management)
- SEC-03: Admin can create, update, list, and delete supported row policies through stable backend contracts (Validated in Phase 02: security-metadata-management)
- SEC-04: SecurityContextBridge interface and JHipsterSecurityContextBridge implementation established as the integration seam for Phase 2's security engine (Validated in Phase 01: identity-and-authority-baseline)
- DATA-01: CRUD permission evaluation (DENY-wins, fail-closed) locked in via unit tests (Validated in Phase 03: secure-enforcement-core)
- DATA-02: Row-level policy enforcement with Specification composition validated (Validated in Phase 03: secure-enforcement-core)
- DATA-03: Attribute-level read filtering (silent omission, id always visible) validated (Validated in Phase 03: secure-enforcement-core)
- DATA-04: Attribute-level write enforcement (fail-closed, AccessDeniedException) validated (Validated in Phase 03: secure-enforcement-core)
- DATA-05: Fetch-plan-driven reads via YAML repository and code builder validated (Validated in Phase 03: secure-enforcement-core)

### Active

- [ ] Merge the `angapp` security capabilities into a single project-native security model that preserves current `angapp` behavior and fits this repository cleanly
- [ ] Build a standalone `frontend/` Angular app that follows the `aef-main/aef-main` structure and integrates with backend authentication and security-management flows
- [ ] Make the merged security engine fully functional end-to-end, including CRUD permissions, row-level policies, attribute-level permissions, secure merge/write guards, and fetch-plan-based reads
- [ ] Define fetch plans only through YAML configuration or code builders; do not store fetch-plan definitions in the database
- [ ] Add sample entities and frontend/backend flows that fully exercise the security engine
- [ ] Minimize DTO usage by default and rely on fetch-plan-based reads, while keeping boundary models only where they are truly necessary

### Out of Scope

- Migrating existing `angapp` business screens and workflows in v1 - those will be handled in later phases after the shared security foundation is stable
- Restoring database-backed fetch-plan metadata such as `sec_fetch_plan` - fetch plans must remain YAML/code-defined only
- Forcing a literal one-to-one copy of either stock JHipster security or `angapp` schema design - the merge should be adapted to this project

## Context

The current repository is a backend-only JHipster/Spring Boot service with JWT authentication, user self-service endpoints, admin user management, PostgreSQL, Liquibase, and mail flows already in place. The repository has no generated frontend today, so the new `frontend/` app will become the main client entry point.

`angapp` is the reference implementation for the desired security behavior. Its important functional traits are not limited to schema: it routes business data access through a central secure data manager, applies entity CRUD permissions, attribute view/edit permissions, row-level policies, secure merge protections, and fetch-plan-based serialization. Those behaviors must survive the merge.

`aef-main/aef-main` is the reference for the new frontend structure. It combines the PrimeNG Sakai layout approach with the standard JHipster Angular organization, including `core`, `shared`, `layout`, route-based features, and i18n assets.

This project also has an existing codebase map under `.planning/codebase/`, which captures the current backend stack and architecture and should be treated as the baseline brownfield state.

## Current State

Phase 02 is complete. The backend now has merged security metadata schema, service and bridge layers, admin REST endpoints for roles, permissions, and row policies, and end-to-end integration coverage proving SEC-01, SEC-02, and SEC-03.

## Constraints

- **Compatibility**: Preserve the functional security capabilities already working in `angapp` - entity CRUD checks, attribute permissions, row policies, secure merge behavior, and fetch-plan-driven secure reads must still work after the merge
- **Frontend structure**: The new UI must live in a standalone `frontend/` app modeled after `aef-main/aef-main` - PrimeNG Sakai plus JHipster-style Angular structure
- **Fetch plans**: Fetch plans must be defined in YAML or code builders only - database storage for fetch-plan definitions is not allowed
- **Brownfield safety**: Existing authentication, account, admin-user, and mail flows in the current backend must not regress during the migration
- **API boundary**: Existing JHipster account/user APIs may keep minimal boundary request/response models where dropping them would destabilize the public contract or validation model

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Merge the security models into a project-native role/permission design | The project must preserve `angapp` behavior without forcing a brittle one-to-one schema copy | - Pending |
| Create a standalone Angular app under `frontend/` using the `aef-main` structure as reference | The user wants the Sakai + JHipster frontend combination as the new client foundation | - Pending |
| Use fetch plans from YAML and code builders only | Database-backed fetch-plan storage has already been removed and should stay removed | - Pending |
| Remove DTOs incrementally rather than project-wide on day one | Existing JHipster user/account APIs still benefit from contract-protecting boundary models | - Pending |

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
*Last updated: 2026-03-21 after Phase 02 (security-metadata-management) complete*




