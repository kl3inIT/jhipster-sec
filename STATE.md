# GSD State Tracker

## Active Task: Apply shadcn-style PrimeNG theme across the Angular frontend

**Status:** COMPLETED
**Started:** 2026-04-01
**Completed:** 2026-04-01
**Type:** quick
**Priority:** normal

### Goal

Reshape the current Angular + PrimeNG frontend so shared component surfaces follow a shadcn-like visual system while preserving the repo's PrimeNG-first architecture.

### Plan

1. Audit the shared theme and layout files that control most PrimeNG visuals
2. Update global tokens and component styling toward a shadcn-like appearance
3. Keep the implementation Angular-native and PrimeNG-based rather than introducing React/Radix code
4. Verify the frontend build after the theme changes

### Completion Summary

Applied a shadcn-inspired design layer to the existing Angular frontend by updating shared PrimeNG theme primitives, the topbar/sidebar shell, configurator radius tokens, and local screen overrides. The rollout now covers the permission matrix, auth screens, organization workbench detail, and user-management list so these high-traffic views share the same flatter surfaces, tighter radii, neutral borders, and lighter shadows.

**Verification:**

- ✅ Updated shared theme tokens, PrimeNG component styling, shell surfaces, and additional feature-page overrides
- ✅ `frontend`: `npm run build` passed
- ⚠️ Existing Angular bundle budget warnings remain, but no build failure was introduced by this change

### Notes

- Scope implemented as a shadcn-like theme on top of PrimeNG, not a literal `shadcn/ui` port
- This matches the repo's Angular stack and PrimeNG-first project constraint
