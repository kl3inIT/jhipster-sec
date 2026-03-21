# Phase 1: Identity And Authority Baseline - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Preserve the existing authentication, account lifecycle, and admin user management flows without regression. Introduce a `SecurityContextBridge` interface and its JHipster-native default implementation as the structural integration point for Phase 2's merged security engine. No semantic role mapping, no `angapp` classes, no schema changes.

</domain>

<decisions>
## Implementation Decisions

### SEC-04 bridge scope
- **D-01:** Phase 1 introduces a `SecurityContextBridge` interface that exposes raw authority strings (same data as JHipster's SecurityContext today). Phase 2 implements this interface for the merged engine without depending directly on JHipster's SecurityContext.
- **D-02:** Phase 1 delivers structural wiring only — no semantic mapping of `ROLE_ADMIN`/`ROLE_USER` to merged role concepts. That translation belongs to Phase 2.
- **D-03:** Phase 2 is self-contained for introducing the engine and integrating existing authorities. Phase 1 reduces coupling by defining the integration point, but Phase 2 is not blocked on Phase 1 providing more than that.

### JHipster-native boundary
- **D-04:** Phase 1 stays entirely JHipster-native. No `angapp`-specific classes enter the codebase in this phase. The `angapp` security merge begins in Phase 2.

### Authority model
- **D-05:** `jhi_authority` table and `Authority` entity are frozen in Phase 1. No schema changes, no Liquibase additions.
- **D-06:** `AuthoritiesConstants` stays as-is (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_ANONYMOUS`). No dynamic registration mechanism.
- **D-07:** `SecurityContextBridge` exposes `Collection<String>` authority names. A typed `AuthorityDescriptor` is a Phase 2 concern.

### Migration risk posture
- **D-08:** Phase 1 is primarily additive. `UserService`, `AccountResource`, `UserResource`, and `SecurityConfiguration` must not be modified unless a small targeted refactor is strictly required to introduce the bridge extension point.
- **D-09:** `SecurityContextBridge` default implementation lives in `com.vn.core.security.bridge` (new sub-package) to keep the extension seam explicit and separate from existing JHipster security classes.
- **D-10:** Minor extraction from `SecurityUtils` or `DomainUserDetailsService` is acceptable if needed to create clean integration points. Heavy refactoring of those classes is not acceptable.

### Test baseline
- **D-11:** Integration-level regression tests must be established before or at the very start of Phase 1 implementation work — not added after. They serve as the regression baseline.
- **D-12:** Baseline test scope: login, register, activate, password reset, password change, admin user CRUD, authority listing. All are in scope; login/account/admin flows are highest priority if forced to triage.
- **D-13:** Coverage target: happy paths plus key error cases (e.g. wrong password, unactivated account, duplicate email, missing activation key) — not exhaustive contract coverage of every response field.
- **D-14:** `SecurityContextBridge` gets focused unit tests in addition to indirect integration test coverage.

### Claude's Discretion
- Exact interface method signatures for `SecurityContextBridge` (beyond returning raw authority strings)
- Whether minor extraction from `SecurityUtils`/`DomainUserDetailsService` is needed at all, or if the bridge can be implemented purely additively
- Test framework choices (MockMvc vs WebTestClient, Testcontainers vs H2 for integration tests)
- Package layout within `com.vn.core.security.bridge`

</decisions>

<specifics>
## Specific Ideas

- The `SecurityContextBridge` seam must be clean enough that Phase 2 can swap in its own implementation by providing a Spring bean — default implementation auto-wires unless overridden.
- Phase 2 will evolve `jhi_authority` into a richer role model in-place (no separate `sec_role` table); `Authority.name` becomes the canonical role code. Phase 1 should not take any action that would make that evolution harder.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase requirements and goals
- `.planning/REQUIREMENTS.md` §AUTH-02, §AUTH-03, §SEC-04 — the three requirements this phase must satisfy
- `.planning/ROADMAP.md` §Phase 1 — success criteria and dependency declaration

### Project constraints
- `.planning/PROJECT.md` §Constraints — brownfield safety rules; existing auth/account/admin flows must not regress

### Codebase baseline
- `.planning/codebase/ARCHITECTURE.md` — full layer map, auth data flow, error handling strategy; read before touching any existing class
- `.planning/codebase/CONVENTIONS.md` — coding conventions to follow
- `.planning/codebase/CONCERNS.md` — known concerns and risk areas

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/com/vn/core/security/SecurityUtils.java`: Utility methods for extracting current principal and authorities from the SecurityContext — candidate source for logic to expose via `SecurityContextBridge`
- `src/main/java/com/vn/core/security/DomainUserDetailsService.java`: Loads `User` + authority names from `UserRepository` and builds `UserDetails` — the data flow the bridge will wrap
- `src/main/java/com/vn/core/security/AuthoritiesConstants.java`: Canonical authority name constants; use these in bridge implementation and tests

### Established Patterns
- `com.vn.core.config.SecurityConfiguration`: URL-based + `@PreAuthorize` method-level security; do not modify, new bridge must work within existing filter chain
- `com.vn.core.web.rest.errors.ExceptionTranslator`: RFC 7807 error contract for all REST responses; new code must throw typed exceptions compatible with this translator
- `com.vn.core.domain.AbstractAuditingEntity`: Auditing base — not directly relevant to Phase 1 but follow this pattern for any new entities introduced

### Integration Points
- `SecurityContextBridge` default impl connects to `SecurityUtils` (current principal/authorities) and must be registered as a Spring bean in `com.vn.core.security.bridge`
- Phase 2 will override the bridge bean; the default must not be `@Primary` or otherwise block substitution
- New integration tests connect to the existing `AccountResource`, `AuthenticateController`, `UserResource`, and `AuthorityResource` endpoints

</code_context>

<deferred>
## Deferred Ideas

- `AuthorityDescriptor` typed contract — Phase 2, when the merged role model is introduced
- `angapp` security classes and concepts — Phase 2 merge
- `jhi_authority` schema evolution (adding columns, relationships for the richer role model) — Phase 2
- Semantic mapping of `ROLE_ADMIN`/`ROLE_USER` to merged security role concepts — Phase 2
- Dynamic authority registration mechanism — Phase 2 alongside new role model
- `sec_role` / separate role table consideration — resolved: Phase 2 evolves `jhi_authority` in-place instead

</deferred>

---

*Phase: 01-identity-and-authority-baseline*
*Context gathered: 2026-03-21*
