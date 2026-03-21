# Requirements: JHipster Security Platform

**Defined:** 2026-03-21
**Core Value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, row-level, and attribute-level access decisions.

## v1 Requirements

Requirements for the first migration milestone. Each will be mapped to a roadmap phase.

### Authentication And Account

- [ ] **AUTH-01**: User can log in from the standalone `frontend/` app using the existing JWT authentication flow
- [ ] **AUTH-02**: User can complete the existing account lifecycle flows after the migration, including register, activate, password reset, and password change
- [ ] **AUTH-03**: Admin can manage users and base authorities without regressing the current backend behavior

### Security Administration

- [ ] **SEC-01**: Admin can create, update, list, and delete merged security roles
- [ ] **SEC-02**: Admin can create, update, list, and delete permission rules for entity CRUD and attribute view/edit actions
- [ ] **SEC-03**: Admin can create, update, list, and delete row policies for the supported policy model
- [ ] **SEC-04**: User authority assignments are bridged into the merged security engine so runtime access decisions reflect admin configuration

### Secure Data Enforcement

- [ ] **DATA-01**: Secured business entity reads go through a central security-aware data access layer
- [ ] **DATA-02**: Secured reads use fetch plans defined in YAML or code builders rather than database-stored fetch-plan metadata
- [ ] **DATA-03**: Unauthorized attributes are excluded from secured read payloads
- [ ] **DATA-04**: Unauthorized attribute updates are rejected or stripped before persistence
- [ ] **DATA-05**: Row-level policies constrain read, update, and delete access for secured entities

### Sample Protected Entities

- [ ] **ENT-01**: Sample entities exist that can exercise CRUD, row-level, and attribute-level security end to end
- [ ] **ENT-02**: Secured entity APIs have automated backend tests for allow and deny scenarios
- [ ] **ENT-03**: Sample entity screens in `frontend/` reflect allowed and denied actions and field visibility correctly

### Frontend Experience

- [ ] **UI-01**: A standalone Angular app exists under `frontend/` and follows the `aef-main/aef-main` structure direction
- [ ] **UI-02**: The frontend provides end-to-end role, permission, and row-policy management screens
- [ ] **UI-03**: The frontend handles authentication state, route protection, and expected 401/403/404 flows correctly

## v2 Requirements

### Deferred Migration Work

- **V2-01**: Existing `angapp` business screens and workflows are migrated after the shared security foundation is stable
- **V2-02**: Richer row-policy authoring beyond the supported v1 policy subset is added
- **V2-03**: Remaining legacy JHipster boundary DTOs are removed only where doing so does not weaken API contracts or validation
- **V2-04**: Fetch-plan authoring UI is considered only if a later milestone truly needs runtime plan administration

## Out of Scope

Explicitly excluded from this milestone.

| Feature | Reason |
|---------|--------|
| Full migration of existing `angapp` business workflows | The user explicitly deferred those workflows beyond v1 |
| Database-backed fetch-plan metadata | The project requires fetch plans to live only in YAML or code |
| Literal one-to-one schema copy from `angapp` or stock JHipster | The merged security model must fit this repository rather than mirror either source blindly |
| Broad row-policy designer for unsupported policy types | The donor implementation does not yet prove those policy types end to end |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | TBD | Pending |
| AUTH-02 | TBD | Pending |
| AUTH-03 | TBD | Pending |
| SEC-01 | TBD | Pending |
| SEC-02 | TBD | Pending |
| SEC-03 | TBD | Pending |
| SEC-04 | TBD | Pending |
| DATA-01 | TBD | Pending |
| DATA-02 | TBD | Pending |
| DATA-03 | TBD | Pending |
| DATA-04 | TBD | Pending |
| DATA-05 | TBD | Pending |
| ENT-01 | TBD | Pending |
| ENT-02 | TBD | Pending |
| ENT-03 | TBD | Pending |
| UI-01 | TBD | Pending |
| UI-02 | TBD | Pending |
| UI-03 | TBD | Pending |

**Coverage:**
- v1 requirements: 18 total
- Mapped to phases: 0
- Unmapped: 18 ??

---
*Requirements defined: 2026-03-21*
*Last updated: 2026-03-21 after initial definition*
