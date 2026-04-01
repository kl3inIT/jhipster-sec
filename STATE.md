# GSD State Tracker

## Active Task: Adjust permission matrix responsive two-column layout

**Status:** COMPLETED
**Started:** 2026-04-01
**Completed:** 2026-04-01
**Type:** quick
**Priority:** normal

### Goal

Keep the permission matrix entity and attribute panels in a two-column layout across medium and large desktop widths, and collapse to one column only when the viewport is genuinely narrow.

### Plan

1. Inspect the permission matrix grid and current breakpoint behavior
2. Lower the two-column breakpoint to suit 1280-class displays with the existing shell
3. Verify the frontend build after the layout adjustment

### Completion Summary

Adjusted the permission matrix screen so the entity and attribute panels stay in a two-column layout on medium and large desktop widths, instead of waiting for the `xl` Tailwind breakpoint. The screen still falls back to one column on narrow viewports.

**Verification:**

- ✅ Updated permission matrix grid breakpoint and column sizing
- ✅ `frontend`: `npm run build` passed
- ⚠️ Existing Angular bundle budget warnings remain, but no build failure was introduced by this change

### Notes

- Scope limited to the permission matrix screen
- Preserve one-column behavior on narrow viewports
