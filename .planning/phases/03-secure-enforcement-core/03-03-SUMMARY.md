---
phase: 03-secure-enforcement-core
plan: 03
subsystem: security
tags: [attribute-permissions, serialization, merge, fetch-plan, access-control]

# Dependency graph
requires:
  - phase: 03-01
    provides: SecureEntitySerializer and SecureMergeService interfaces, FetchPlan/FetchPlanProperty records, AttributePermissionEvaluator interface
  - phase: 03-02
    provides: AttributePermissionEvaluatorImpl (the concrete bean this plan uses via constructor injection)
provides:
  - SecureEntitySerializerImpl — attribute-filtered read-side serialization with recursive fetch-plan traversal
  - SecureMergeServiceImpl — fail-closed write-side attribute enforcement using AccessDeniedException
affects: [03-04, 03-05]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Read-side fail-open: denied attributes silently omitted from serialized output (D-15)"
    - "id always-visible rule in serializer: canView check bypassed for id attribute (D-16)"
    - "Write-side fail-closed: denied attribute edits throw AccessDeniedException rather than silent strip (D-18)"
    - "Identity immutability in merge: id attribute always skipped, never writable through mergeForUpdate"
    - "BeanWrapperImpl for reflective property access on both read and write paths"
    - "Recursive fetch-plan traversal: associations walk nested FetchPlan, collections map individually"

key-files:
  created:
    - src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java
    - src/main/java/com/vn/core/security/merge/SecureMergeServiceImpl.java
  modified: []

key-decisions:
  - "SecureEntitySerializerImpl uses @Component (not @Service) per plan spec, consistent with phase 3 pattern"
  - "id always-visible guard in serializer uses isAlwaysVisible(attr) helper with Locale.ROOT lowercasing"
  - "SecureMergeServiceImpl skips id silently (not an AccessDeniedException) — identity immutability is structural, not a permission violation"
  - "Reference/association detection uses property.fetchPlan() != null (null means scalar per FetchPlanProperty contract)"

patterns-established:
  - "Serialize via BeanWrapperImpl + canView: read path never throws on denied attributes"
  - "Merge via BeanWrapperImpl + canEdit: write path throws AccessDeniedException on denied attributes"

requirements-completed: [DATA-03, DATA-04]

# Metrics
duration: 1min
completed: 2026-03-21
---

# Phase 03 Plan 03: Secure Serializer and Merge Service Summary

**Attribute-level enforcement implemented: read-side serializer silently filters denied fields, write-side merge rejects unauthorized edits with AccessDeniedException.**

## Performance

- **Duration:** 1 min
- **Started:** 2026-03-21T13:21:09Z
- **Completed:** 2026-03-21T13:22:10Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Implemented `SecureEntitySerializerImpl` with recursive fetch-plan traversal, id always-visible rule (D-16), silent omission of denied attributes (D-15), and collection/association support
- Implemented `SecureMergeServiceImpl` with fail-closed rejection of unauthorized attribute writes via `AccessDeniedException` (D-18) and identity-field skip guard

## Task Commits

1. **Task 1: SecureEntitySerializerImpl** - `5ef137c` (feat)
2. **Task 2: SecureMergeServiceImpl** - `d2145ef` (feat)

## Files Created/Modified

- `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java` - @Component serializer implementing SecureEntitySerializer; recursively walks FetchPlan properties with canView checks; id always included; collections and null references handled
- `src/main/java/com/vn/core/security/merge/SecureMergeServiceImpl.java` - @Component merge service implementing SecureMergeService; skips id; throws AccessDeniedException on canEdit denial; sets permitted attributes via BeanWrapperImpl

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check: PASSED

- `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java` — FOUND
- `src/main/java/com/vn/core/security/merge/SecureMergeServiceImpl.java` — FOUND
- Commit `5ef137c` — FOUND
- Commit `d2145ef` — FOUND
