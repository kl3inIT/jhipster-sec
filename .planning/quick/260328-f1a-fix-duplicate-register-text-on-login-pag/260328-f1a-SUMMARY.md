# Quick Task Summary

- Quick ID: `260328-f1a`
- Task: fix duplicate register text on login page.

## Outcome

- Updated [login.component.html](/D:/jhipster/frontend/src/app/pages/login/login.component.html) to use the existing `global.messages.info.register.noaccount` prompt and `global.messages.info.register.link` CTA instead of rendering the same register label twice.
- Added [login.component.spec.ts](/D:/jhipster/frontend/src/app/pages/login/login.component.spec.ts) to lock the corrected Vietnamese footer copy so the prompt text and CTA stay distinct.

## Verification

- `npm exec ng test -- --watch=false --include="src/app/pages/login/login.component.spec.ts"`

## Residual Notes

- The workspace already had unrelated changes, so this quick task was recorded in `.planning` but not auto-committed.
