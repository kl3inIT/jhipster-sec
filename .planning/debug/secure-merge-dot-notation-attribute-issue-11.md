---
status: awaiting_human_verify
trigger: "Investigate issue: secure-merge-dot-notation-attribute-issue-11"
created: 2026-03-28T14:41:01.7346278+07:00
updated: 2026-03-28T14:53:32.4087013+07:00
---

## Current Focus

hypothesis: The deprecated string-based save path was removed safely; only human confirmation remains if anyone depends on that deleted internal API outside this repo.
test: Ask for confirmation that no out-of-repo consumer still expects `SecureDataManager.save(String, ...)`.
expecting: If there are no external callers, the branch is ready; otherwise we would need a compatibility decision.
next_action: report the removal-based fix and verification results to the user

## Symptoms

expected: Updating secured entities via `secureDataManager.save(entityCode, id, payload, fetchPlan)` should safely handle nested relationship updates and should not crash when the mutation includes a nested reference field.
actual: The issue claims payloads like `{ "movieProfile.id": 5 }` are converted with Jackson into a typed entity where `movieProfile == null`, then secure merge reads `movieProfile.id` from the typed source entity and crashes.
errors: Reported failure occurs while resolving nested property path `movieProfile.id` during merge.
reproduction: Inspect the `ProductionEkip` update flow mentioned in the issue, especially `MovieProfileService.syncProductionEkips`, `SecureDataManagerImpl.save(String entityCode, ...)`, and `SecureMergeServiceImpl.mergeForUpdate(...)`. Confirm whether the failing path exists in current code and whether the recommended fix is correct.
started: Reported as remote GitHub issue #11 and currently open.

## Eliminated

## Evidence

- timestamp: 2026-03-28T14:41:49.1084724+07:00
  checked: `.planning/debug/knowledge-base.md`
  found: No knowledge base file exists in this repo yet.
  implication: No prior resolved debug pattern is available; investigate from first principles.

- timestamp: 2026-03-28T14:41:49.1084724+07:00
  checked: active backend source search for `ProductionEkip`, `MovieProfileService`, `syncProductionEkips`, and `movieProfile.id`
  found: No matching active backend classes or methods were found under `src/main/java`; only secure merge and related tests matched the reported merge entry points.
  implication: The issue report likely references older or external domain code; the current bug, if real, must be verified against the generic secure merge path in this repo.

- timestamp: 2026-03-28T14:44:07.0523749+07:00
  checked: secured entity REST/input path via `SecuredEntityJsonAdapter` and `SecuredEntityPayloadValidator`
  found: Active secured CRUD resources parse JSON into typed entities and only collect top-level field names; dotted keys such as `department.id` are rejected as unknown fields before reaching merge.
  implication: The reported crash is not reachable through current secured REST endpoints; any real defect must be in the lower-level `save(String entityCode, ..., Map<String,Object>)` API or an external caller using it directly.

- timestamp: 2026-03-28T14:44:07.0523749+07:00
  checked: `SecureDataManagerImpl.save(String entityCode, Object id, Map<String, Object> attributes, String fetchPlanCode)` and `SecureMergeServiceImpl.mergeForUpdate(Object entity, Object sourceEntity, Collection<String> changedAttributes)`
  found: The map-based save path converts the raw map to a typed entity, then passes the raw request key set unchanged into typed merge; typed merge reads each changed attribute path from the source entity through `BeanWrapperImpl`.
  implication: If callers use dotted keys that Jackson does not bind into the typed source entity, the merge path is exposed to nested-path failures or incorrect null writes.

- timestamp: 2026-03-28T14:47:09.5075358+07:00
  checked: focused unit test `SecureDataManagerImplTest.saveUpdate_normalizesDotNotationAssociationKeysBeforeTypedMerge`
  found: The test failed because `SecureDataManagerImpl.save(String, ...)` invoked `objectMapper.convertValue` with the raw map `{\"department.id\"=42}` instead of a nested map, proving the boundary does not normalize dotted keys before merge.
  implication: The bug is real in the public map-based save API, but the report's domain-specific route is stale; the correct fix belongs at the map-to-typed-entity boundary in `SecureDataManagerImpl`.

- timestamp: 2026-03-28T14:50:26.2667315+07:00
  checked: in-repo usage of deprecated string-based `SecureDataManager` methods
  found: `save(String entityCode, ...)` is marked `@Deprecated(since = "08.3-03")` and has no production callers under `src/main/java`; only unit tests exercise it.
  implication: Removing the deprecated save overload is safe for the current brownfield runtime and better matches the new requirement than carrying forward compatibility code for an unused path.

- timestamp: 2026-03-28T14:53:32.4087013+07:00
  checked: focused backend verification via `./gradlew test --tests com.vn.core.security.data.SecureDataManagerImplTest --tests com.vn.core.security.merge.SecureMergeServiceImplTest`
  found: The surviving typed secure data manager and merge tests passed after removing the deprecated string-based save overload.
  implication: The active brownfield backend path remains intact, and the stale issue path no longer exists in the repo.

## Resolution

root_cause: The reported failure lived only in the deprecated map-based `SecureDataManager.save(String, ...)` path, which forwarded dotted request keys directly into `ObjectMapper.convertValue(...)` and `mergeForUpdate(...)` instead of using the typed mutation flow that the active app already relies on.
fix: Remove the deprecated string-based save overload entirely rather than preserving it; current production code already uses the typed `EntityMutation` save path through `SecuredEntityJsonAdapter`.
verification:
verification: `./gradlew test --tests com.vn.core.security.data.SecureDataManagerImplTest --tests com.vn.core.security.merge.SecureMergeServiceImplTest` passed after removing the deprecated string-based save overload and its tests.
files_changed:
  - src/main/java/com/vn/core/security/data/SecureDataManager.java
  - src/main/java/com/vn/core/security/data/SecureDataManagerImpl.java
  - src/test/java/com/vn/core/security/data/SecureDataManagerImplTest.java
