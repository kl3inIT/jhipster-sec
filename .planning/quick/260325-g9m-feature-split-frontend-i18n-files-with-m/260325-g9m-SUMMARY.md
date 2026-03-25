---
quick: 260325-g9m
title: feature-split frontend i18n files with merge step like aef-main
status: complete
completed: 2026-03-25
---

# Quick Task 260325-g9m Summary

- Added `frontend/scripts/merge-i18n.cjs` to deep-merge `frontend/src/i18n/{lang}/*.json` into `frontend/public/i18n/{lang}.json`.
- Added `frontend/src/app/config/i18n-hash.generated.ts` and updated the translation loader to request hashed runtime bundles.
- Split the current English and Vietnamese translations into feature files under `frontend/src/i18n/en/` and `frontend/src/i18n/vi/`.
- Updated `frontend/package.json` so `prestart`, `prebuild`, `prewatch`, and `pretest` regenerate merged bundles automatically.

## Verification

- `npm --prefix frontend run i18n:merge`
- `npm --prefix frontend exec ng test -- --watch=false --include src/app/config/translation.config.spec.ts --include src/app/shared/error/http-error.utils.spec.ts`
- `npm --prefix frontend run build`
