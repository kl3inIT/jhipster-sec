# Requirements: JHipster Security Platform

**Defined:** 2026-03-25
**Milestone:** v1.1 Enterprise Admin Experience
**Core Value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, authority, and attribute-level access decisions.

## Milestone v1.1 Requirements

### User Management

- [x] **UMGT-01**: Admin can browse, search, sort, and open existing users from `frontend/` using the preserved backend admin API.
- [x] **UMGT-02**: Admin can create, edit, activate, deactivate, and delete users from `frontend/` without breaking current validation or contract behavior.
- [x] **UMGT-03**: Admin can assign and update a user's roles or authorities from the frontend, and the persisted assignment affects downstream access decisions.

### Navigation And Permissions

- [x] **ROUTE-01**: The authenticated frontend menu and navigation structure are loaded from backend-driven data rather than a hardcoded client menu definition.
- [x] **ROUTE-02**: Routes and visible navigation entries are denied before page render when the current user lacks the required backend-provided role or permission.
- [x] **ROUTE-03**: Admin and entity areas are split behind lazy-loaded route boundaries so permission-aware navigation does not force-load the entire app shell.

### Enterprise UI

- [x] **UI-04**: Admin and secured-entity screens use an enterprise-style shell with Jmix-like master-detail patterns so lists and detail or edit workflows feel consistent.
- [ ] **UI-05**: The frontend is more usable and responsive across desktop and narrower widths, with consistent actions, spacing, feedback, and loading states.

### Internationalization And Migration Parity

- [x] **I18N-01**: Required JHipster support files from `angapp/` for in-scope frontend features are migrated into `frontend/` instead of being reimplemented incompletely.
- [x] **I18N-02**: Migrated admin, user-management, and shared shell flows can render translated UI strings and preserve language-aware behavior using copied JHipster translation assets.

### Phase 08.3 Security Realignment

- [x] **PH83-01**: The standalone `frontend/` app provides a user registration flow that reuses the preserved backend `/api/register` contract and activation behavior safely.
- [x] **PH83-02**: Current-user authorities refresh from database state without forcing logout or login, and the frontend refreshes permission-dependent caches accordingly.
- [x] **PH83-03**: Secured entity data flow operates on real typed entities internally and no longer uses `String`, `Map<String, Object>`, or `JsonNode` as the core security representation.
- [x] **PH83-04**: JSON-based secured controllers validate request bodies, unknown fields, invalid references, and query shapes explicitly and fail closed.
- [x] **PH83-05**: Row policy is removed completely from schema, backend runtime, admin APIs, frontend UI, and tests, with no surviving dependency on row-policy code paths.

### Performance And Scalability

- [x] **PERF-01**: The frontend minimizes redundant API calls for auth, menu, capability, and user-management data through shared state or safe caching.
- [ ] **PERF-02**: Initial load and route transitions improve through lazy loading, code splitting, and leaner route-level bundles.
- [ ] **PERF-03**: Enterprise admin screens remain responsive under larger data sets through efficient rendering, pagination or filtering, and predictable state updates.

### Frontend Reliability

- [ ] **TEST-01**: Automated frontend tests cover user-management CRUD and role-assignment behavior across success and failure paths.
- [ ] **TEST-02**: Automated frontend tests cover backend-driven routing, menu visibility, and permission-based access denial.
- [ ] **TEST-03**: Automated frontend tests cover the enterprise shell and critical migrated UI components so copied JHipster support files do not regress behavior.

## Future Requirements

### Deferred Platform Expansion

- **MIG-01**: Migrate additional `angapp` business domains beyond user management once the new shell and parity infrastructure are proven.
- **DATA-06**: Introduce fetch-plan authoring UI only if runtime administration truly needs it.
- **API-01**: Remove remaining boundary DTOs only where public contracts and validation remain stable.
- **ADMIN-01**: Migrate legacy ops or admin utilities such as health, metrics, logs, configuration, and docs only if operational users need them in `frontend/`.

## Out of Scope

Explicitly excluded from this milestone.

| Feature | Reason |
|---------|--------|
| Literal full `angapp` clone | This milestone copies required support files and in-scope flows into `frontend/`, not every legacy page wholesale |
| Database-backed fetch-plan metadata | Project constraints still require fetch plans to live only in YAML or code |
| New row-policy replacement model | Phase 08.3 retires row policy entirely; any future replacement needs separate product definition |
| Arbitrary runtime page-layout builder | The milestone needs stable backend-driven navigation and user-management parity before higher-order UI composition |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| I18N-01 | Phase 6 | Complete |
| I18N-02 | Phase 6 | Complete |
| ROUTE-01 | Phase 7 | Complete |
| ROUTE-02 | Phase 7 | Complete |
| ROUTE-03 | Phase 7 | Complete |
| UI-04 | Phase 7 | Complete |
| UMGT-01 | Phase 8 | Complete |
| UMGT-02 | Phase 8 | Complete |
| UMGT-03 | Phase 8 | Complete |
| PH83-01 | Phase 08.3 | Complete |
| PH83-02 | Phase 08.3 | Complete |
| PH83-03 | Phase 08.3 | Complete |
| PH83-04 | Phase 08.3 | Complete |
| PH83-05 | Phase 08.3 | Complete |
| UI-05 | Phase 9 | Pending |
| PERF-01 | Phase 9 | Complete |
| PERF-02 | Phase 9 | Pending |
| PERF-03 | Phase 9 | Pending |
| TEST-01 | Phase 10 | Pending |
| TEST-02 | Phase 10 | Pending |
| TEST-03 | Phase 10 | Pending |

**Coverage:**
- Milestone requirements: 21 total
- Mapped to phases: 21
- Unmapped: 0

---
*Requirements defined: 2026-03-25*
*Last updated: 2026-03-28 after Phase 08.3 completion*
