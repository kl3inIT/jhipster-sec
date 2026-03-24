---
phase: 6
slug: frontend-parity-foundation
status: approved
nyquist_compliant: true
wave_0_complete: false
created: 2026-03-25
updated: 2026-03-25
---

# Phase 6 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Angular test builder with Vitest via `@angular/build:unit-test` |
| **Config file** | `frontend/angular.json` and `frontend/tsconfig.spec.json` |
| **Quick run command** | `npm --prefix frontend exec ng test -- --watch=false --include src/app/core/auth/account.service.spec.ts --include src/app/core/interceptor/notification.interceptor.spec.ts --include src/app/layout/component/menu/app.menu.spec.ts` |
| **Full suite command** | `npm --prefix frontend test -- --watch=false` |
| **Estimated runtime** | ~75 seconds |

---

## Sampling Rate

- **After every task commit:** Run `npm --prefix frontend exec ng test -- --watch=false --include src/app/core/auth/account.service.spec.ts --include src/app/core/interceptor/notification.interceptor.spec.ts --include src/app/layout/component/menu/app.menu.spec.ts`
- **After every plan wave:** Run `npm --prefix frontend test -- --watch=false`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 06-TBD-01 | TBD | TBD | I18N-01 | unit | `npm --prefix frontend exec ng test -- --watch=false --include src/app/config/translation.config.spec.ts --include src/app/core/interceptor/notification.interceptor.spec.ts --include src/app/core/util/alert.service.spec.ts` | ❌ Wave 0 | pending |
| 06-TBD-02 | TBD | TBD | I18N-02 | component | `npm --prefix frontend exec ng test -- --watch=false --include src/app/core/auth/account.service.spec.ts --include src/app/layout/component/menu/app.menu.spec.ts --include src/app/app-page-title-strategy.spec.ts` | ❌ Wave 0 | pending |

*Status: pending | green | red | flaky*

---

## Wave 0 Requirements

- [ ] `frontend/src/app/config/translation.config.spec.ts` - cover loader path, fallback language, and missing-translation behavior for I18N-01
- [ ] `frontend/src/app/core/interceptor/notification.interceptor.spec.ts` - cover `app-alert` and `app-params` header handling for I18N-01
- [ ] `frontend/src/app/core/util/alert.service.spec.ts` - cover translated alert add and dismiss behavior for I18N-01
- [ ] `frontend/src/app/core/auth/account.service.spec.ts` - extend locale precedence coverage so stored locale beats default and account fallback for I18N-02
- [ ] `frontend/src/app/layout/component/menu/app.menu.spec.ts` - verify translated labels and recomputation on language change for I18N-02
- [ ] `frontend/src/app/app-page-title-strategy.spec.ts` - cover translated route titles and `html[lang]` updates for I18N-02

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| `vi` is the default first-load locale and a stored locale overrides it on refresh | I18N-02 | Requires browser storage and live bootstrap behavior | 1. Clear browser storage. 2. Load the app and confirm Vietnamese text is the default. 3. Switch to English through the implemented locale flow. 4. Refresh and confirm English stays active. |
| Backend `app-alert` headers surface as visible translated feedback in the PrimeNG shell | I18N-01 | Requires end-to-end browser rendering with a live HTTP response | 1. Trigger an action that returns `app-alert` and `app-params` headers. 2. Confirm a visible success alert or toast appears. 3. Confirm the rendered text is translated and does not require console inspection. |

---

## Validation Sign-Off

- [x] All phase requirements have automated verification targets or explicit Wave 0 coverage
- [x] Sampling continuity: no more than 2 consecutive tasks should land without the quick verification command
- [x] Wave 0 gaps are explicitly listed for required new i18n and alert tests
- [x] No watch-mode flags
- [x] Feedback latency < 90s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-03-25
