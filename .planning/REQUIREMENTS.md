# Requirements: JHipster Security Platform

**Defined:** 2026-04-01
**Core Value:** Security rules must be enforced correctly in the data access layer so frontend and backend features can rely on consistent CRUD, authority, and attribute-level access decisions.

## v1 Requirements

Requirements for milestone v1.2 CI/CD & Production Validation.

### CI/CD

- [ ] **CICD-01**: Developer can run a GitHub Actions backend pipeline that builds and verifies the Spring Boot application on Java 25.
- [ ] **CICD-02**: Developer can run a GitHub Actions frontend pipeline that builds and verifies the standalone Angular frontend on Node 24.
- [ ] **CICD-03**: Developer can run an integration validation workflow that starts the production-like container stack and verifies the application becomes healthy.

### Production Deployment

- [ ] **DEPLOY-01**: Operator can start a production-like Docker Compose stack for backend, frontend, and PostgreSQL using environment-driven configuration instead of hardcoded secrets.
- [ ] **DEPLOY-02**: Operator can serve the standalone frontend through a production-ready container path that works behind VPN and Nginx Proxy Manager.
- [ ] **DEPLOY-03**: Operator can verify the production profile starts cleanly with hardened configuration suitable for a private server deployment.

### Performance

- [ ] **PERF-05**: Developer can evaluate permission lookup overhead at projected scale and replace repeated brute-force permission scans with lookup-friendly request-lifecycle structures where needed.
- [ ] **PERF-06**: Developer can identify and implement meaningful request-path optimizations without changing the security model’s authorization semantics.
- [ ] **PERF-07**: Developer can run production-topology benchmark validation and compare secured-endpoint behavior against the current performance baseline.

### Security Validation

- [ ] **SECVAL-01**: Developer can prove auth refresh behavior remains correct in the production-like environment.
- [ ] **SECVAL-02**: Developer can prove secured-entity CRUD and attribute-level permission enforcement remain correct in the production-like environment.
- [ ] **SECVAL-03**: Developer can prove authorization guarantees do not regress under production-like load and deployment conditions.

## v2 Requirements

Deferred to a future milestone.

### Frontend Reliability

- **TEST-01**: Add reliable automated frontend coverage for the new admin experience.
- **TEST-02**: Expand reliability coverage around routing, shared UI infrastructure, and registration.
- **TEST-03**: Add CI-executed browser-level validation for critical frontend flows.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Kubernetes / Helm deployment | Docker Compose is the chosen v1.2 production-like target |
| Public internet ingress redesign | Deployment is behind VPN with Nginx Proxy Manager already in place |
| Multi-node Hazelcast clustering | Not required for this milestone’s production validation |
| New end-user business features | v1.2 is operational maturity, not feature expansion |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| CICD-01 | Phase TBD | Pending |
| CICD-02 | Phase TBD | Pending |
| CICD-03 | Phase TBD | Pending |
| DEPLOY-01 | Phase TBD | Pending |
| DEPLOY-02 | Phase TBD | Pending |
| DEPLOY-03 | Phase TBD | Pending |
| PERF-05 | Phase TBD | Pending |
| PERF-06 | Phase TBD | Pending |
| PERF-07 | Phase TBD | Pending |
| SECVAL-01 | Phase TBD | Pending |
| SECVAL-02 | Phase TBD | Pending |
| SECVAL-03 | Phase TBD | Pending |

**Coverage:**
- v1 requirements: 12 total
- Mapped to phases: 0
- Unmapped: 12 ⚠️

---
*Requirements defined: 2026-04-01*
*Last updated: 2026-04-01 after initial definition*
