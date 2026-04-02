# Requirements: JHipster Security Platform

**Defined:** 2026-04-01
**Milestone:** v1.2 CI/CD & Production Validation
**Core Value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, authority, and attribute-level access decisions.

## Milestone v1.2 Requirements

### Production Configuration And Runtime Baseline

- [x] **PROD-01**: The project can start a production-like application stack from committed configuration and compose assets without depending on dev-only shortcuts.
- [ ] **PROD-02**: The production-like stack preserves brownfield-safe auth, account, admin-user, mail, and secured-entity behavior so validation can run against a realistic runtime shape.

### CI/CD Pipeline Separation

- [ ] **CICD-01**: CI separates backend and frontend verification so failures are isolated to the lane that broke instead of blocking all feedback behind one monolithic job.
- [ ] **CICD-02**: CI can run the production-validation prerequisites needed by this milestone, including the production-like stack and benchmark entry points, in a repeatable automated flow.

### Production-Like Benchmark Baseline And Validation

- [ ] **BENCH-02**: Benchmarking uses a production-like dataset with realistic row counts, multiple secured tables, and representative relationship depth instead of a narrow single-table seed.
- [ ] **BENCH-03**: Benchmark runs measure secured vs baseline behavior under multiple concurrency levels and persist comparable summaries that can be reviewed before optimization work begins.
- [ ] **VAL-01**: The milestone establishes a trusted benchmark baseline and stack-validation report before any deeper permission-lookup optimization is treated as ready to start.

### Permission Lookup Optimization

- [ ] **PERF-05**: Permission lookup work reduces the benchmarked overhead identified in the production-like baseline without weakening CRUD, authority, attribute-level, or fetch-plan security behavior.

### Production Security Proof

- [ ] **SEC-05**: The final milestone proof demonstrates that the production-like stack, benchmark evidence, and optimized permission path still enforce the expected security outcomes under realistic runtime conditions.

## Future Requirements

### Deferred Platform Expansion

- **TEST-01**: Add reliable automated frontend coverage for user-management CRUD and role-assignment behavior across success and failure paths.
- **TEST-02**: Add automated frontend coverage for backend-driven routing, menu visibility, and permission-based access denial.
- **TEST-03**: Add automated frontend coverage for the enterprise shell and critical migrated UI components so copied JHipster support files do not regress behavior.
- **MIG-01**: Migrate additional `angapp` business domains beyond current parity scope once the production-validation baseline is proven.
- **DATA-06**: Introduce fetch-plan authoring UI only if runtime administration truly needs it.
- **API-01**: Remove remaining boundary DTOs only where public contracts and validation remain stable.
- **ADMIN-01**: Migrate legacy ops or admin utilities such as health, metrics, logs, configuration, and docs only if operational users need them in `frontend/`.

## Out of Scope

Explicitly excluded from this milestone.

| Feature | Reason |
|---------|--------|
| Literal full `angapp` clone | This milestone validates production-readiness and CI behavior, not broad legacy feature migration |
| Database-backed fetch-plan metadata | Project constraints still require fetch plans to live only in YAML or code |
| New row-policy replacement model | Row policy was retired in Phase 08.3 and is not part of v1.2 scope |
| Broad feature expansion beyond production-validation needs | v1.2 is focused on deployable runtime proof, benchmark trust, and targeted security optimization |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| PROD-01 | Phase 12 | Complete |
| PROD-02 | Phase 12 | Pending |
| CICD-01 | Phase 13 | Pending |
| CICD-02 | Phase 13 | Pending |
| BENCH-02 | Phase 14 | Pending |
| BENCH-03 | Phase 14 | Pending |
| VAL-01 | Phase 14 | Pending |
| PERF-05 | Phase 15 | Pending |
| SEC-05 | Phase 16 | Pending |

**Coverage:**
- Milestone requirements: 9 total
- Mapped to phases: 9
- Unmapped: 0

---
*Requirements defined: 2026-04-01*
*Last updated: 2026-04-01 after roadmap revision prioritizing benchmark baselines before optimization*
