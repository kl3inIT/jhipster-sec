---
phase: 12
slug: production-runtime-foundation
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-02
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Spring Boot Test + MockMvc + Testcontainers PostgreSQL; live Compose runtime checks via committed Phase 12 scripts; Playwright 1.58.x remains available for later phases once a committed SPA runtime target exists |
| **Config file** | `src/test/java/com/vn/core/IntegrationTest.java`, `frontend/playwright.config.ts`, `frontend/angular.json`, `scripts/phase12-stack-smoke.sh`, `scripts/phase12-prodlike-regression.mjs` |
| **Quick run command** | `npm run phase12:stack:smoke && npm run phase12:backend:prodlike` |
| **Full suite command** | `npm run phase12:stack:smoke && npm run phase12:backend:prodlike && ./gradlew integrationTest --tests com.vn.core.web.rest.AccountResourceIT --tests com.vn.core.web.rest.UserResourceIT --tests com.vn.core.web.rest.SecuredEntityCapabilityResourceIT --tests com.vn.core.web.rest.SecuredEntityEnforcementIT` |
| **Coverage note** | `phase12:stack:smoke` and `phase12:backend:prodlike` are the primary live-stack PROD-02 proof for this milestone's narrowed scope (auth/account, admin-user, secured-entity). The Spring integration tests remain complementary in-process coverage. Mail delivery, registration, and forgot-password proof were explicitly deferred from Phase 12.` |
| **Runtime prereqs** | The committed compose app service now carries the Phase 12 JWT base64 secret env so the prod profile can boot inside Docker without ad hoc local secret export before smoke or regression runs. |
| **Estimated runtime** | ~300 seconds |

---

## Sampling Rate

- **After every runtime-contract task commit:** Run `npm run phase12:stack:smoke`
- **After every PROD-02 validation task commit:** Run `npm run phase12:stack:smoke && npm run phase12:backend:prodlike`
- **Before `/gsd:verify-work`:** Run the full suite command above so live-stack proof and the supporting Spring integration suite are both green
- **Max feedback latency:** 300 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 12-01-01 | 01 | 1 | PROD-01 | contract | `python - <<'PY' ... application-prod env contract verified ... PY` | ✅ | ⬜ pending |
| 12-01-02 | 01 | 1 | PROD-01 | compose contract | `docker compose -f D:/jhipster/src/main/docker/app.yml config > /tmp/phase12-app-compose.txt && python - <<'PY' ... compose env contract verified ... PY` | ✅ | ⬜ pending |
| 12-02-01 | 02 | 2 | PROD-02 | smoke / live stack | `npm run phase12:stack:smoke` | ✅ | ⬜ pending |
| 12-02-02 | 02 | 2 | PROD-02 | live-stack regression | `npm run phase12:backend:prodlike` | ✅ | ⬜ pending |
| 12-02-03 | 02 | 2 | PROD-02 | supporting integration | `./gradlew integrationTest --tests com.vn.core.web.rest.AccountResourceIT --tests com.vn.core.web.rest.UserResourceIT --tests com.vn.core.web.rest.SecuredEntityCapabilityResourceIT --tests com.vn.core.web.rest.SecuredEntityEnforcementIT` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Browser Validation Scope

Phase 12 does not claim browser proof through Playwright yet because the checker correctly identified that the SPA serving target is not committed in this phase. To stay honest and repeatable:

- Phase 12 runtime proof is limited to the committed Compose-launched backend + PostgreSQL + Mailpit baseline from D-04.
- `frontend/playwright.config.ts` may remain available for future work, but Phase 12 acceptance does not depend on an ad hoc `http://localhost:4200` dev-server path.
- A committed browser target or same-origin SPA runtime shape should be introduced in a later phase before Playwright becomes a required gate again.
- This narrowing still preserves D-05 because frontend API behavior remains backend-relative; it simply avoids overstating what Phase 12 proves.

---

## Wave 0 Requirements

- [x] Add one repeatable stack smoke assertion or wrapper for `npm run app:up` so Phase 12 can verify app health against the production-like Compose baseline.
- [x] Add one repeatable runtime-targeted validation command that exercises auth, account, admin-user, and secured-entity behavior against the live Compose-launched backend.
- [x] Narrow browser-validation claims until the repo has a committed SPA serving target instead of depending on an undefined dev-server path.
- [x] Treat `phase12:stack:smoke` and `phase12:backend:prodlike` as the committed primary live-stack proof while keeping Spring integration tests as supporting, in-process mirrors for the narrowed Phase 12 scope.

---

## Manual-Only Verifications

No additional manual-only checks are required for the narrowed Phase 12 milestone scope. Mail delivery, registration, and forgot-password flows are deferred from this phase.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 300s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
