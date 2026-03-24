# JHipster Security Platform

## What This Is

This is a brownfield migration that evolved the current JHipster security service into a fuller security platform with a shipped standalone Angular frontend. `v1.0` preserves the backend auth and admin capabilities while absorbing the Jmix-style security behavior from `angapp`, including merged security metadata management, secure data access, row-level rules, attribute-level rules, and fetch-plan-driven reads.

## Core Value

Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, row-level, and attribute-level access decisions.

## Current State

`v1.0 MVP` shipped on 2026-03-25.

The repository now includes:

- The original Spring Boot/JHipster auth, account, admin-user, mail, and authority flows preserved without regression.
- Merged security metadata administration for roles, permissions, and supported row policies.
- A secure data-access layer centered on `SecureDataManager`, with CRUD, row-level, attribute-level, and YAML/code-defined fetch-plan enforcement.
- Proof-domain entities and APIs for Organization, Department, and Employee that verify allow/deny behavior end to end.
- A standalone Angular frontend under `frontend/` for login, route protection, security administration, and protected-entity screens.

## Requirements

### Validated

- AUTH-01 through AUTH-03 shipped in `v1.0`, covering standalone frontend login plus preserved backend account and admin-user behavior.
- SEC-01 through SEC-04 shipped in `v1.0`, covering merged security metadata CRUD and authority-bridge integration.
- DATA-01 through DATA-05 shipped in `v1.0`, covering secured reads, secured writes, row policies, attribute permissions, and YAML/code-only fetch plans.
- ENT-01 through ENT-03 shipped in `v1.0`, covering proof entities, backend allow/deny coverage, and frontend capability-driven screens.
- UI-01 through UI-03 shipped in `v1.0`, covering the standalone Angular app, security-management UI, and route/error/auth handling.

### Active

- [ ] V2-01: Migrate selected existing `angapp` business screens and workflows onto the shipped security platform.
- [ ] V2-02: Expand row-policy authoring beyond the supported v1 policy subset.
- [ ] V2-03: Remove remaining legacy JHipster boundary DTOs only where API contracts and validation stay stable.
- [ ] V2-04: Decide whether later milestones actually need fetch-plan authoring UI.

### Out of Scope

- Database-backed fetch-plan metadata remains out of scope; fetch plans stay YAML/code-defined only.
- A literal one-to-one schema copy of `angapp` or stock JHipster remains out of scope; the merged security model stays project-native.
- Unsupported row-policy designer variants remain out of scope until real use cases justify broadening the policy model.

## Context

`v1.0` covered 5 phases, 30 plans, 43 documented tasks, 160 milestone commits, and roughly 359 files / 45,043 inserted lines across planning, backend, tests, and the new frontend. Final human UAT passed on 2026-03-25 with 3/3 checks green: login/session behavior, admin security-management persistence, and protected-entity gating.

Remaining non-product debt is process-oriented: validation strategy metadata for phases 1, 3, and 4 stayed draft even though the shipped verification evidence is complete.

## Constraints

- **Compatibility**: Preserve the functional security capabilities already working in `angapp` - entity CRUD checks, attribute permissions, row policies, secure merge behavior, and fetch-plan-driven secure reads must still work after the merge
- **Frontend structure**: The new UI must live in a standalone `frontend/` app modeled after `aef-main/aef-main` - PrimeNG Sakai plus JHipster-style Angular structure
- **Fetch plans**: Fetch plans must be defined in YAML or code builders only - database storage for fetch-plan definitions is not allowed
- **Brownfield safety**: Existing authentication, account, admin-user, and mail flows in the current backend must not regress during the migration
- **API boundary**: Existing JHipster account/user APIs may keep minimal boundary request/response models where dropping them would destabilize the public contract or validation model

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Merge the security models into a project-native role/permission design | The project must preserve `angapp` behavior without forcing a brittle one-to-one schema copy | Confirmed in `v1.0` |
| Create a standalone Angular app under `frontend/` using the `aef-main` structure as reference | The new client needed a clear Angular/PrimeNG foundation that still fit JHipster conventions | Shipped in `v1.0` |
| Use `SecureDataManager` as the single secured data path and `UnconstrainedDataManager` as the trusted bypass | Security enforcement had to stay centralized and explicit | Validated in phases 3-5 |
| Use proof-domain entities to validate the merged security engine end to end | The platform needed real sample entities to prove CRUD, row, and attribute behavior | Validated in phase 4 |
| Use fetch plans from YAML and code builders only | Database-backed fetch-plan storage was explicitly disallowed by project constraints | Confirmed in `v1.0` |
| Remove DTOs incrementally rather than project-wide on day one | Existing JHipster user/account APIs still benefit from contract-protecting boundary models | Confirmed in `v1.0` |

## Next Milestone Goals

- Define the next milestone in a fresh `.planning/REQUIREMENTS.md` via `$gsd-new-milestone`.
- Decide which `angapp` business workflows should be migrated first now that the shared security platform is shipped.
- Revisit richer row-policy authoring and remaining DTO boundaries against concrete product needs rather than broad cleanup goals.
- Evaluate whether any fetch-plan authoring UI is justified by real runtime administration use cases.

---
*Last updated: 2026-03-25 after v1.0 MVP milestone completion*
