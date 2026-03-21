---
phase: 5
slug: standalone-frontend-delivery
status: approved
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-22
updated: 2026-03-22
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Karma + Jasmine (Angular default via `ng test`) |
| **Config file** | `frontend/karma.conf.js` — created by `ng new` scaffold in Plan 02 Task 1 |
| **Quick run command** | `cd frontend && ng test --watch=false --browsers=ChromeHeadless` |
| **Full suite command** | `cd frontend && ng test --watch=false --browsers=ChromeHeadless --code-coverage` |
| **Estimated runtime** | ~60 seconds |

---

## Wave 0 Bootstrap

Wave 0 is satisfied by Plan 02 Task 1, which runs `ng new --skip-tests=false`. This generates:
- `frontend/karma.conf.js` — Karma configuration
- `frontend/src/app/app.component.spec.ts` — default AppComponent test
- ChromeHeadless runner available via `ng test --watch=false --browsers=ChromeHeadless`

Plan 02 Task 1 verify includes `ng test --watch=false --browsers=ChromeHeadless` to confirm the scaffold test passes. This establishes the test infrastructure baseline before any implementation tasks run.

Additional test stubs created during execution:
- `frontend/src/app/layout/component/menu/app.menu.spec.ts` — Plan 02 Task 3 (authority-gated menu rendering)

---

## Sampling Rate

- **After every task commit:** Run `cd frontend && ng test --watch=false --browsers=ChromeHeadless`
- **After every plan wave:** Run `cd frontend && ng test --watch=false --browsers=ChromeHeadless --code-coverage`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Automated Command | Status |
|---------|------|------|-------------|-------------------|--------|
| 05-01-01 | 01 | 1 | UI-02 | `./gradlew integrationTest --tests "*.SecCatalogAdminResourceIT"` | pending |
| 05-01-02 | 01 | 1 | UI-02 | `./gradlew integrationTest --tests "*.SecPermissionAdminResourceIT"` | pending |
| 05-02-01 | 02 | 1 | UI-01 | `cd frontend && ng build && ng test --watch=false --browsers=ChromeHeadless` | pending |
| 05-02-02 | 02 | 1 | AUTH-01 | `cd frontend && ng build && ng test --watch=false --browsers=ChromeHeadless` | pending |
| 05-02-03 | 02 | 1 | UI-03 | `cd frontend && ng build && ng test --watch=false --browsers=ChromeHeadless` | pending |
| 05-03-01 | 03 | 2 | ENT-03 | `cd frontend && ng build && ng test --watch=false --browsers=ChromeHeadless` | pending |
| 05-03-02 | 03 | 2 | ENT-03 | `cd frontend && ng build && ng test --watch=false --browsers=ChromeHeadless` | pending |
| 05-03-03 | 03 | 2 | ENT-03 | `cd frontend && ng build && ng test --watch=false --browsers=ChromeHeadless` | pending |
| 05-04-01 | 04 | 2 | UI-02 | `cd frontend && ng build && ng test --watch=false --browsers=ChromeHeadless` | pending |
| 05-04-02 | 04 | 2 | UI-02 | `cd frontend && ng build && ng test --watch=false --browsers=ChromeHeadless` | pending |
| 05-05-01 | 05 | 3 | UI-02 | `cd frontend && ng build && ng test --watch=false --browsers=ChromeHeadless` | pending |

*Status: pending | green | red | flaky*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| JWT auth flow: login -> token stored -> route guard allows entry | AUTH-01 | Requires browser session with live backend | 1) Start backend. 2) Navigate to `/login`. 3) Submit valid credentials. 4) Confirm redirect to dashboard. 5) Check localStorage/sessionStorage for token. |
| 401/403/404 route handling | AUTH-01 | Requires error responses from live backend | 1) Access protected route without token -> confirm redirect to login. 2) Access forbidden route -> confirm 403 page shown. 3) Access nonexistent route -> confirm 404 page shown. |
| Permission matrix: check/uncheck permission persists across reload | UI-02 | Requires live API round-trip | 1) Open permission matrix for a role. 2) Toggle a permission on. 3) Reload page. 4) Confirm permission is still checked. |
| Field-level gating on entity detail | UI-03 | Requires two user accounts with different permissions | 1) Log in as user with limited attr permissions. 2) Open entity detail. 3) Confirm restricted fields not visible. 4) Log in as admin -> confirm all fields visible. |
| Sidebar shows Entities and Security Admin sections with correct menu items | UI-01 | Verified by unit test `app.menu.spec.ts` (Plan 02 Task 3) AND manual visual check | 1) Log in as admin. 2) Confirm sidebar shows Home, Entities (3 items), Security Admin (2 items). 3) Log in as non-admin. 4) Confirm Security Admin section is hidden. |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify with `ng test --watch=false --browsers=ChromeHeadless`
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covered by Plan 02 Task 1 scaffold (generates default spec + karma config)
- [x] No watch-mode flags in any verify command
- [x] Feedback latency < 90s
- [x] `nyquist_compliant: true` set in frontmatter
- [x] `wave_0_complete: true` set in frontmatter

**Approval:** approved
