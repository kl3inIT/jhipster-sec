 # Phase 9: Enterprise UX And Performance Hardening - Context

**Gathered:** 2026-03-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 9 improves the enterprise frontend's consistency, responsiveness, and data-fetch efficiency without weakening the security model that Phase 08.3 just stabilized. This phase may include backend request-path optimizations when they directly reduce redundant work triggered by admin or secured-entity pages and preserve request-time permission refresh semantics.

**Outcome**
- Redundant auth, menu, capability, and secured-read permission work is removed or cached safely.
- Enterprise screens keep consistent loading, empty, and feedback states while remaining responsive on narrower widths.
- Route-level bundle costs and transition latency improve through more disciplined lazy loading and leaner feature boundaries.

</domain>

<decisions>
## Planning Inputs

### Locked Decisions
- **D-01:** Phase 9 may optimize backend secured-read permission checks as part of `PERF-01`, but it must preserve the request-time authority refresh semantics introduced in Phase 08.3.
- **D-02:** For current-user permission caching, the first implementation step is request-local reuse only. Cross-request or session-level caching is out of scope unless a later plan proves explicit invalidation for authority, permission, and menu changes.
- **D-03:** Reuse the existing bulk-permission-matrix pattern already present in capability generation rather than adding another parallel permission-aggregation model.

### Captured Todo For Planning
- **T-01:** `Cache current-user permission checks` from `.planning/todos/pending/2026-03-27-cache-current-user-permission-checks.md`
  - Problem: secured entity reloads currently repeat `jhi_authority` validation and `sec_permission` reads within a single request.
  - Candidate direction: build a request-bound permission snapshot once, then reuse it from `MergedSecurityContextBridge`, `RolePermissionServiceDbImpl`, `AttributePermissionEvaluatorImpl`, `SecureEntitySerializerImpl`, and `SecuredEntityCapabilityService`.
  - Constraint: the next request must rebuild the snapshot so live permission refresh remains correct.

### Agent's Discretion
- Exact request-local storage mechanism (request attributes vs scoped bean) as long as non-web and test contexts degrade safely
- Whether to extract a shared `PermissionMatrix` type from capability code or replace it with a richer immutable snapshot model
- Whether this work is one Phase 9 plan or split into backend snapshot extraction plus verification

</decisions>

<canonical_refs>
## Canonical References

- `.planning/ROADMAP.md`
- `.planning/REQUIREMENTS.md`
- `.planning/STATE.md`
- `.planning/todos/pending/2026-03-27-cache-current-user-permission-checks.md`
- `.planning/phases/08.3-user-registration-live-permission-refresh-entity-native-serialization-validation-hardening-and-row-policy-removal/08.3-CONTEXT.md`
- `src/main/java/com/vn/core/security/bridge/MergedSecurityContextBridge.java`
- `src/main/java/com/vn/core/security/permission/RolePermissionServiceDbImpl.java`
- `src/main/java/com/vn/core/security/permission/AttributePermissionEvaluatorImpl.java`
- `src/main/java/com/vn/core/security/serialize/SecureEntitySerializerImpl.java`
- `src/main/java/com/vn/core/service/security/SecuredEntityCapabilityService.java`
- `frontend/src/app/pages/entities/shared/service/secured-entity-capability.service.ts`

</canonical_refs>

<code_context>
## Existing Code Insights

- `MergedSecurityContextBridge#getCurrentUserAuthorities()` currently validates JWT authorities against `jhi_authority` on each call.
- `RolePermissionServiceDbImpl` and `AttributePermissionEvaluatorImpl` currently query `SecPermissionRepository` per entity or attribute check.
- `SecureEntitySerializerImpl` can multiply attribute permission checks across every fetched property in a response.
- `SecuredEntityCapabilityService` already proves the useful bulk-load shape by building a `PermissionMatrix` from `findAllByAuthorityNameIn(...)` once.

</code_context>
