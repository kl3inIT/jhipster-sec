# Milestones

## v1.1 Enterprise Admin Experience (Shipped: 2026-03-31)

**Phases completed:** 10 phases, 39 plans, 55 tasks

**Audit:** `.planning/milestones/v1.1-MILESTONE-AUDIT.md`
**Archives:** `.planning/milestones/v1.1-ROADMAP.md`, `.planning/milestones/v1.1-REQUIREMENTS.md`

**Key accomplishments:**

- Delivered enterprise admin parity in `frontend/`: backend-driven navigation, full user-management CRUD, and role/menu permission surfaces.
- Completed security-core realignment: request-time authority refresh, typed secured-entity data flow, explicit fail-closed JSON validation, and full row-policy retirement.
- Shipped Phase 9 UX/performance hardening with responsive PrimeNG list workspaces, skeleton loading patterns, and reduced redundant permission-evaluation chatter.
- Shipped benchmark and OpenAPI documentation track for secured entity APIs, including tagged operations and generated `x-secured-entity` metadata.
- Landed Phase 11 performance hardening and proved PERF-04 with load-test evidence: secured p95 overhead reduced to within the <10% target.

---

## v1.0 MVP (Shipped: 2026-03-25)

**Phases completed:** 5 phases, 30 plans, 43 tasks
**Audit:** `.planning/milestones/v1.0-MILESTONE-AUDIT.md`
**Archives:** `.planning/milestones/v1.0-ROADMAP.md`, `.planning/milestones/v1.0-REQUIREMENTS.md`

**Key accomplishments:**

- Preserved the existing backend authentication, account lifecycle, and admin-user behavior while bridging authority assignments into the merged security runtime.
- Delivered merged security metadata management for roles, permissions, and supported row policies through stable backend admin APIs.
- Built the secure enforcement core around `SecureDataManager`, with CRUD, row-level, attribute-level, and YAML/code-defined fetch-plan enforcement.
- Proved the backend security model end to end on Organization, Department, and Employee sample entities with allow/deny integration coverage.
- Shipped the standalone Angular frontend with login, route protection, security administration, and protected-entity capability gating.
- Closed final frontend UAT gaps with buffered permission-matrix saves, confirmation dialogs, faster capability reloads, and 3/3 live human-UAT tests passing.

---
