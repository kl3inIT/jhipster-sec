# Phase 6: Frontend Parity Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md - this log preserves the alternatives considered.

**Date:** 2026-03-25
**Phase:** 06-frontend-parity-foundation
**Areas discussed:** Donor and support-file scope, Language rollout, Alert and feedback behavior, User-management foundation depth

---

## Donor and support-file scope

| Option | Description | Selected |
|--------|-------------|----------|
| Minimum donor pack | Copy only the in-scope support files needed for language, alerts, request helpers, and admin/user-management foundations | X |
| Broader support copy | Bring over a larger shared/admin support layer up front | |

**User's choice:** Agent-directed minimal in-scope donor pack, with `aef-main` accepted as the shell and template reference.
**Notes:** User said `aef-main` contains the support files and delegated the decision. The resulting context reconciles that with the locked project rule: `aef-main` stays the structural template, while `angapp` remains the donor for JHipster-specific support files and translations.

---

## Language rollout

| Option | Description | Selected |
|--------|-------------|----------|
| Vietnamese default + English secondary | Ship both `vi` and `en`, default first load to Vietnamese, keep English available | X |
| English-only foundation | Wire translation plumbing now but postpone dual-language assets | |

**User's choice:** Vietnamese default, English secondary.
**Notes:** Locale should persist through the existing frontend storage flow so later visits use the chosen language rather than resetting.

---

## Alert and feedback behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Translated alert foundation | Surface backend alert headers through user-visible translated alerts adapted to the PrimeNG shell | X |
| Lightweight console or toast only | Keep the current minimal feedback behavior and defer shared alert foundations | |

**User's choice:** Agent decision.
**Notes:** Chosen direction is to restore translated shared alert behavior rather than keep `console.warn`-only notifications.

---

## User-management foundation depth

| Option | Description | Selected |
|--------|-------------|----------|
| Migrate shared user-management groundwork now | Bring user-management model, service, route, and shared request/pagination helpers into Phase 6 | X |
| Defer foundations to Phase 8 | Wait and move the shared user-management groundwork together with the screens | |

**User's choice:** Agent decision.
**Notes:** Chosen direction is to move the groundwork now so Phase 8 can focus on user-management screens instead of runtime plumbing.

---

## the agent's Discretion

- Exact file-by-file donor list inside the approved Phase 6 pack
- Exact standalone adaptations for translation and alert helpers
- Exact alert presentation choice per screen context
- Whether the language switcher UI lands in Phase 6 or Phase 7

## Deferred Ideas

- Full user-management UI delivery remains Phase 8.
- Backend-driven navigation and permission-aware shell work remains Phase 7.
- Out-of-scope legacy admin pages remain deferred.
