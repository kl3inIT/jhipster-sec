---
phase: 5
slug: standalone-frontend-delivery
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-22
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Karma + Jasmine (Angular default via `ng test`) |
| **Config file** | `frontend/karma.conf.js` — Wave 0 installs |
| **Quick run command** | `cd frontend && ng test --watch=false --browsers=ChromeHeadless` |
| **Full suite command** | `cd frontend && ng test --watch=false --browsers=ChromeHeadless --code-coverage` |
| **Estimated runtime** | ~60 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd frontend && ng test --watch=false --browsers=ChromeHeadless`
- **After every plan wave:** Run `cd frontend && ng test --watch=false --browsers=ChromeHeadless --code-coverage`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 05-01-01 | 01 | 0 | UI-01 | build | `cd frontend && ng build` | ❌ W0 | ⬜ pending |
| 05-01-02 | 01 | 1 | AUTH-01 | unit | `cd frontend && ng test --watch=false --include=**/auth*` | ❌ W0 | ⬜ pending |
| 05-02-01 | 02 | 1 | AUTH-01 | e2e-manual | See Manual Verifications | — | ⬜ pending |
| 05-03-01 | 03 | 2 | UI-02 | unit | `cd frontend && ng test --watch=false --include=**/permission*` | ❌ W0 | ⬜ pending |
| 05-04-01 | 04 | 3 | ENT-03 | unit | `cd frontend && ng test --watch=false --include=**/entity*` | ❌ W0 | ⬜ pending |
| 05-05-01 | 05 | 3 | UI-03 | unit | `cd frontend && ng test --watch=false --include=**/row-policy*` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `frontend/` scaffold exists with Angular CLI project (`package.json`, `angular.json`, `karma.conf.js`)
- [ ] `frontend/src/app/core/auth/auth.service.spec.ts` — stubs for AUTH-01
- [ ] `frontend/src/app/admin/security/permission-matrix/permission-matrix.component.spec.ts` — stubs for UI-02
- [ ] `frontend/src/app/entities/*/list/*.component.spec.ts` — stubs for ENT-03 / UI-03
- [ ] ChromeHeadless available or installed for CI

*Wave 0 creates the Angular app scaffold and test stubs before implementation begins.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| JWT auth flow: login → token stored → route guard allows entry | AUTH-01 | Requires browser session with live backend | 1) Start backend. 2) Navigate to `/login`. 3) Submit valid credentials. 4) Confirm redirect to dashboard. 5) Check localStorage/sessionStorage for token. |
| 401/403/404 route handling | AUTH-01 | Requires error responses from live backend | 1) Access protected route without token → confirm redirect to login. 2) Access forbidden route → confirm 403 page shown. 3) Access nonexistent route → confirm 404 page shown. |
| Permission matrix: check/uncheck permission persists across reload | UI-02 | Requires live API round-trip | 1) Open permission matrix for a role. 2) Toggle a permission on. 3) Reload page. 4) Confirm permission is still checked. |
| Field-level gating on entity detail | UI-03 | Requires two user accounts with different permissions | 1) Log in as user with limited attr permissions. 2) Open entity detail. 3) Confirm restricted fields not visible. 4) Log in as admin — confirm all fields visible. |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
