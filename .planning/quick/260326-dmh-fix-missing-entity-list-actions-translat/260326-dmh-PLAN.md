---
quick: 260326-dmh
title: fix missing entity.list.actions translation and explain admin menu permission union behavior
status: complete
created: 2026-03-26
---

# Quick Task 260326-dmh Plan

1. Confirm why admin still sees entity menus after role or menu permission changes by tracing current-user authority resolution, menu-permission union logic, JWT claims, and frontend navigation caching.
2. Add the missing `entity.list.actions` translation key to the static frontend bundles so entity list headers stop rendering translation-not-found output.
3. Validate the updated JSON bundles and record the investigation outcome plus the quick-fix result in GSD artifacts and the active 07.1 UAT session.
