# GSD State Tracker

## Active Task: Move PrimeNG component styling into token-based shadcn-like presets

**Status:** COMPLETED
**Started:** 2026-04-01
**Completed:** 2026-04-01
**Type:** quick
**Priority:** normal

### Goal

Move the shadcn-like visual system from broad CSS overrides into PrimeNG component variables so the Angular frontend stays PrimeNG-native while core component styling is driven by official theme tokens.

### Plan

1. Inventory the PrimeNG components actually used in `frontend/src/app`
2. Promote their styling into `components` tokens inside the shared PrimeUIX preset
3. Keep only the structural SCSS that cannot be expressed cleanly with PrimeNG tokens
4. Verify the frontend build after the token migration

### Completion Summary

Moved the shared shadcn-like styling into PrimeUIX `components` tokens for the PrimeNG widgets that drive this project: buttons, cards, inputs, selects, multiselects, datatables, treetables, paginators, dialogs, tabs, tags, messages, toasts, checkboxes, confirm dialogs, password overlays, and input-number controls. The configurator now reapplies the same token layer when switching preset or primary color, while `_primeng-theme.scss` was reduced to structural adjustments that PrimeNG token APIs do not expose cleanly.

**Verification:**

- ✅ Added token-based component theming in `frontend/src/app/theme/app-theme.preset.ts`
- ✅ Wired the configurator to preserve those tokens during runtime preset changes
- ✅ Reduced overlapping CSS overrides in `frontend/src/assets/app/_primeng-theme.scss`
- ✅ `frontend`: `npm run build` passed
- ⚠️ Existing Angular bundle budget warnings remain, but no build failure was introduced by this pass

### Notes

- Scope remains a shadcn-like PrimeNG theme layer, not a literal React `shadcn/ui` port
- Tokens were prioritized for the PrimeNG components actually used by the Angular frontend in this repo
