---
quick: 260331-pmx
title: refresh permission matrix entity visual treatment and restore wildcard label
status: complete
created: 2026-03-31
---

# Quick Task 260331-pmx Plan

1. Review the active permission-matrix entity and attribute table markup/styles to identify why the CRUD grid looks visually noisy and hard to scan.
2. Restyle the matrix with a cleaner PrimeNG-friendly table treatment that preserves the existing CRUD, wildcard, pending-change, and row-selection behavior.
3. Restore the missing `security.permissionMatrix.entity.wildcard` translation key in the source bundles, regenerate merged i18n output, and run targeted frontend validation.

## Verification

- `.\node_modules\.bin\ng.cmd test --watch=false --include src/app/pages/admin/security/permission-matrix/permission-matrix.component.spec.ts`
