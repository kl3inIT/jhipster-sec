---
quick: 260327-usx
title: convert menu permission appName from String to enum-backed field
status: complete
created: 2026-03-27
---

# Quick Task 260327-usx Plan

1. Add a backend menu-app enum plus JPA conversion so `SecMenuPermission.appName` becomes an enum field while the `app_name` column and REST payloads keep using the existing string values.
2. Update repository, service, and admin/current-user menu-permission resources so all app-name lookups and DTO mappings convert through the enum instead of raw strings.
3. Refresh the affected backend tests to use the supported app identifiers, run the targeted test slice, then write the quick-task summary and update `.planning/STATE.md`.
