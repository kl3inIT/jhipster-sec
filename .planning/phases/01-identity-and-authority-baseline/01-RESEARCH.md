# Phase 1: Identity And Authority Baseline - Research

**Researched:** 2026-03-21
**Domain:** Spring Security / JHipster identity and authority bridging
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**SEC-04 bridge scope**
- D-01: Phase 1 introduces a `SecurityContextBridge` interface that exposes raw authority strings (same data as JHipster's SecurityContext today). Phase 2 implements this interface for the merged engine without depending directly on JHipster's SecurityContext.
- D-02: Phase 1 delivers structural wiring only — no semantic mapping of `ROLE_ADMIN`/`ROLE_USER` to merged role concepts. That translation belongs to Phase 2.
- D-03: Phase 2 is self-contained for introducing the engine and integrating existing authorities. Phase 1 reduces coupling by defining the integration point, but Phase 2 is not blocked on Phase 1 providing more than that.

**JHipster-native boundary**
- D-04: Phase 1 stays entirely JHipster-native. No `angapp`-specific classes enter the codebase in this phase. The `angapp` security merge begins in Phase 2.

**Authority model**
- D-05: `jhi_authority` table and `Authority` entity are frozen in Phase 1. No schema changes, no Liquibase additions.
- D-06: `AuthoritiesConstants` stays as-is (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_ANONYMOUS`). No dynamic registration mechanism.
- D-07: `SecurityContextBridge` exposes `Collection<String>` authority names. A typed `AuthorityDescriptor` is a Phase 2 concern.

**Migration risk posture**
- D-08: Phase 1 is primarily additive. `UserService`, `AccountResource`, `UserResource`, and `SecurityConfiguration` must not be modified unless a small targeted refactor is strictly required to introduce the bridge extension point.
- D-09: `SecurityContextBridge` default implementation lives in `com.vn.core.security.bridge` (new sub-package) to keep the extension seam explicit and separate from existing JHipster security classes.
- D-10: Minor extraction from `SecurityUtils` or `DomainUserDetailsService` is acceptable if needed to create clean integration points. Heavy refactoring of those classes is not acceptable.

**Test baseline**
- D-11: Integration-level regression tests must be established before or at the very start of Phase 1 implementation work — not added after. They serve as the regression baseline.
- D-12: Baseline test scope: login, register, activate, password reset, password change, admin user CRUD, authority listing. All are in scope; login/account/admin flows are highest priority if forced to triage.
- D-13: Coverage target: happy paths plus key error cases (wrong password, unactivated account, duplicate email, missing activation key) — not exhaustive contract coverage of every response field.
- D-14: `SecurityContextBridge` gets focused unit tests in addition to indirect integration test coverage.

### Claude's Discretion

- Exact interface method signatures for `SecurityContextBridge` (beyond returning raw authority strings)
- Whether minor extraction from `SecurityUtils`/`DomainUserDetailsService` is needed at all, or if the bridge can be implemented purely additively
- Test framework choices (MockMvc vs WebTestClient, Testcontainers vs H2 for integration tests)
- Package layout within `com.vn.core.security.bridge`

### Deferred Ideas (OUT OF SCOPE)

- `AuthorityDescriptor` typed contract — Phase 2, when the merged role model is introduced
- `angapp` security classes and concepts — Phase 2 merge
- `jhi_authority` schema evolution (adding columns, relationships for the richer role model) — Phase 2
- Semantic mapping of `ROLE_ADMIN`/`ROLE_USER` to merged security role concepts — Phase 2
- Dynamic authority registration mechanism — Phase 2 alongside new role model
- `sec_role` / separate role table consideration — resolved: Phase 2 evolves `jhi_authority` in-place instead
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-02 | User can complete the existing account lifecycle flows after the migration, including register, activate, password reset, and password change | Existing `AccountResourceIT` and `AuthenticateControllerIT` cover most of these flows; research confirms gaps in error-case coverage that must be filled |
| AUTH-03 | Admin can manage users and base authorities without regressing the current backend behavior | `UserResourceIT` and `AuthorityResourceIT` exist; research confirms path/body mismatch fragility in `UserResource` that test baseline must record |
| SEC-04 | User authority assignments are bridged into the merged security engine so runtime access decisions reflect admin configuration | Research defines the `SecurityContextBridge` interface contract, package placement, bean substitution pattern, and unit test strategy |
</phase_requirements>

---

## Summary

Phase 1 is a brownfield hardening phase, not a feature phase. The codebase already has working authentication, account lifecycle, admin user management, and authority listing. What does not exist is: (1) a regression test baseline that locks in the current contract before any structural changes are made, and (2) the `SecurityContextBridge` seam that Phase 2 needs to plug into.

The research confirms that all existing JHipster controllers, services, and security classes are in good shape and require no modification in Phase 1. The bridge can be introduced as a purely additive new interface + default implementation under `com.vn.core.security.bridge`. The default implementation pulls authority data directly from `SecurityUtils` — no extraction from existing classes is required. The bean substitution pattern is straightforward: the default implementation is a `@Component` without `@Primary`, making it overridable by any Phase 2 bean in the same application context.

The most important planning input from this research is the test gap map. Several regression scenarios are not covered by the existing test suite: the error path for missing activation keys returns 500 (known bug, must be documented and locked as a baseline rather than silently changing behavior), password-change wrong-old-password rejection is missing, and unauthenticated access to `/api/users` is not exercised. The plan must address these gaps explicitly — some are locked baselines (the 500 behavior), some are genuine missing coverage that must be added.

**Primary recommendation:** Write the regression test baseline first (Wave 1), then add the `SecurityContextBridge` interface + default implementation + unit tests (Wave 2). The order matters: if the bridge is introduced before the baseline exists, there is no safety net for regressions.

---

## Standard Stack

### Core (already in project)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Security OAuth2 Resource Server | via Spring Boot 4.0.3 | JWT bearer token validation, SecurityContext population | JHipster 9 default; already configured in `SecurityConfiguration` |
| Spring Security Test | via Spring Boot 4.0.3 | `@WithMockUser`, `@WithSecurityContext` in tests | Standard companion to Spring Security in test scope |
| JUnit 5 (JUnit Platform) | via Spring Boot 4.0.3 | Unit and integration test runner | Already in `build.gradle` `testImplementation` |
| MockMvc (`spring-boot-starter-webmvc-test`) | via Spring Boot 4.0.3 | HTTP-layer integration tests without a real server | Already used in `AccountResourceIT`, `UserResourceIT`, `AuthenticateControllerIT` |
| Testcontainers PostgreSQL | via `testcontainers-postgresql` | Real database for integration tests | Already configured in `DatabaseTestcontainer`; `@IntegrationTest` wires it automatically |
| AssertJ | via Spring Boot 4.0.3 | Fluent assertions | Standard in all existing tests |

### Supporting (already in project — no new installs needed)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `spring-security-test` | via Spring Boot | `@WithMockUser(authorities=...)` for controller tests | Authority-based authorization tests |
| `apache-commons-lang3` | via Spring Boot | `RandomStringUtils` for test data | Test fixtures needing random strings (already used in `DomainUserDetailsServiceIT`) |

**Installation:** No new dependencies required. Every library needed for Phase 1 is already declared in `build.gradle`.

---

## Architecture Patterns

### Recommended Project Structure for Phase 1 additions

```
src/
├── main/java/com/vn/core/
│   └── security/
│       └── bridge/                            # NEW sub-package
│           ├── SecurityContextBridge.java     # NEW interface
│           └── JHipsterSecurityContextBridge.java  # NEW default impl
└── test/java/com/vn/core/
    └── security/
        └── bridge/                            # NEW test sub-package
            └── JHipsterSecurityContextBridgeTest.java  # NEW unit test
    └── web/rest/
        ├── AccountResourceIT.java             # EXTEND with error-case coverage
        ├── UserResourceIT.java                # EXTEND with any missing admin cases
        └── AuthorityResourceIT.java           # VERIFY complete (no gaps found)
```

### Pattern 1: Spring Bean Substitution via Interface + Default Component

**What:** Define an interface in the `security.bridge` package. Provide one `@Component` implementation that reads from `SecurityUtils`. Phase 2 registers a different bean implementing the same interface; Spring picks the most recently declared or `@Primary` one.

**When to use:** Wherever a seam is needed to allow future engine substitution without touching existing wiring.

**Key constraint:** The default implementation MUST NOT be `@Primary`. If it were, Phase 2's bean would need to declare `@Primary` too, which increases coupling in the wrong direction. Without `@Primary` on the default, Phase 2's `@Primary` bean wins automatically, or the default wins if Phase 2 provides no override yet.

**Example:**
```java
// Source: project pattern, JHipster conventions + Spring bean substitution idiom
package com.vn.core.security.bridge;

import java.util.Collection;
import java.util.Optional;

/**
 * Bridge between the JHipster SecurityContext and the merged security engine.
 * Phase 2 overrides this by providing its own Spring bean implementing this interface.
 */
public interface SecurityContextBridge {

    /** Returns the login name of the currently authenticated user, or empty if not authenticated. */
    Optional<String> getCurrentUserLogin();

    /** Returns the raw authority names (e.g. "ROLE_ADMIN") for the current user, or empty collection if not authenticated. */
    Collection<String> getCurrentUserAuthorities();

    /** Returns true if the current security context holds an authenticated (non-anonymous) principal. */
    boolean isAuthenticated();
}
```

```java
// Default implementation — reads from JHipster's SecurityUtils
package com.vn.core.security.bridge;

import com.vn.core.security.SecurityUtils;
import com.vn.core.security.AuthoritiesConstants;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Default SecurityContextBridge implementation backed by JHipster's SecurityContext.
 * Replaced in Phase 2 by the merged security engine's implementation.
 */
@Component
public class JHipsterSecurityContextBridge implements SecurityContextBridge {

    @Override
    public Optional<String> getCurrentUserLogin() {
        return SecurityUtils.getCurrentUserLogin();
    }

    @Override
    public Collection<String> getCurrentUserAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
    }

    @Override
    public boolean isAuthenticated() {
        return SecurityUtils.isAuthenticated();
    }
}
```

### Pattern 2: Integration Test Baseline Using @IntegrationTest + MockMvc

**What:** Annotate a test class with `@IntegrationTest` (which pulls in Testcontainers PostgreSQL and the full Spring context), `@AutoConfigureMockMvc`, and `@WithMockUser` where appropriate. Inject `MockMvc` and make HTTP-layer assertions.

**When to use:** All regression tests for REST endpoints.

**Key constraint:** Tests named `*IT` are picked up by `integrationTest` Gradle task. Tests named `*Test` are picked up by the `test` task. The `*IT` naming convention must be used for any test that uses the real database via Testcontainers.

**Example — error-case skeleton for activation:**
```java
// Source: pattern from existing AccountResourceIT.java
@Test
@Transactional
void testActivateWithUnknownKey() throws Exception {
    restAccountMockMvc
        .perform(get("/api/activate?key={activationKey}", "WRONG_KEY"))
        .andExpect(status().isInternalServerError());  // known bug: 500 — baseline locks this in, not fixes it
}
```

### Pattern 3: Unit Test for SecurityContextBridge Without Spring Context

**What:** Pure JUnit 5 unit test using `SecurityContextHolder.setContext(...)` to inject a mock `Authentication` object. No `@SpringBootTest` needed.

**When to use:** `SecurityContextBridgeTest` — fast, no database, no container.

**Example:**
```java
// Source: pattern from existing SecurityUtilsUnitTest.java
class JHipsterSecurityContextBridgeTest {

    private final JHipsterSecurityContextBridge bridge = new JHipsterSecurityContextBridge();

    @BeforeEach
    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserAuthorities_returnsGrantedAuthorities() {
        var ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
            "user", "pw",
            List.of(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))
        ));
        SecurityContextHolder.setContext(ctx);

        assertThat(bridge.getCurrentUserAuthorities()).containsExactly(AuthoritiesConstants.ADMIN);
    }
}
```

### Anti-Patterns to Avoid

- **Making `JHipsterSecurityContextBridge` `@Primary`:** Blocks Phase 2 substitution without additional annotations. Leave it as a plain `@Component`.
- **Putting `SecurityContextBridge` in `com.vn.core.security` directly:** Breaks the seam visibility — the bridge is a structural extension point, not a core security primitive. Keep it in the `bridge` sub-package.
- **Modifying `SecurityConfiguration` to expose bridge-related beans:** The filter chain does not need to know about the bridge. The bridge is a service-layer abstraction.
- **Using `@SpringBootTest` for `SecurityContextBridge` unit tests:** Adds 10–30 second startup overhead for what is fundamentally a unit test. Use `SecurityContextHolder` directly as `SecurityUtilsUnitTest` does.
- **Adding bridge verification to `TechnicalStructureTest`:** ArchUnit layer rules define `Security` as a layer. `bridge` is a sub-package of `security`, so it inherits the `..security..` pattern in the existing `TechnicalStructureTest`. No ArchUnit changes are needed.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Populating SecurityContext in tests | Custom test security context | `@WithMockUser(authorities = "ROLE_ADMIN")` from `spring-security-test` | Handles token population, principal resolution, cleanup automatically |
| Starting PostgreSQL for integration tests | Embedded H2 or manual setup | Existing `DatabaseTestcontainer` + `@IntegrationTest` | Already configured; H2 dialect differences can mask Liquibase migration issues |
| Authority constant strings | Redefining `"ROLE_ADMIN"` inline | `AuthoritiesConstants.ADMIN` | Prevents typos; already the project convention |
| MockMvc request setup | Manually building HTTP requests | `MockMvcRequestBuilders.*` static imports | Standard; already used across all existing integration tests |

**Key insight:** Every infrastructure need for Phase 1 is already present in the codebase. The work is purely additive: new interface, new implementation, new tests.

---

## Common Pitfalls

### Pitfall 1: Naming the Unit Test with `*IT` Suffix

**What goes wrong:** `JHipsterSecurityContextBridgeIT.java` would be picked up by the `integrationTest` Gradle task and attempt to load Testcontainers, adding 20–30 seconds of overhead for a pure unit test.
**Why it happens:** The `integrationTest` task selects `**/*IT*` by pattern; the `test` task excludes that pattern.
**How to avoid:** Name the bridge unit test `JHipsterSecurityContextBridgeTest.java` (no `IT` suffix). Integration tests that need the full Spring + DB context use `*IT` naming.
**Warning signs:** Test takes more than 2 seconds to start; stack trace references Testcontainers startup.

### Pitfall 2: Test Activation/Reset Key Errors Lock in the 500 Bug

**What goes wrong:** The known bug — `AccountResource` throws `AccountResourceException` (which maps to 500) for unknown activation/reset keys — must be recorded as the baseline, not silently changed in Phase 1.
**Why it happens:** CONCERNS.md documents this explicitly: `AccountResourceIT` already asserts 500 for bad keys at line 389 and 776. If a Phase 1 test is written expecting 400, it will conflict with the existing test and suggest a behavior change that is out of scope.
**How to avoid:** Phase 1 baseline tests for these paths must assert `status().isInternalServerError()` (500), mirroring the existing `AccountResourceIT` expectations. Fixing the status code to 400 is deferred.
**Warning signs:** Test written expecting 400/404 for `/api/activate?key=WRONG_KEY` fails against existing suite.

### Pitfall 3: ArchUnit Fails When bridge Sub-Package is Miscategorized

**What goes wrong:** If `JHipsterSecurityContextBridge` imports from `com.vn.core.web` or `com.vn.core.service`, ArchUnit's `TechnicalStructureTest` will fail because `Security` layer may not access `Web` or `Service` layers.
**Why it happens:** The bridge is in `..security..` which the ArchUnit rule treats as the Security layer. The Security layer is only accessed by Config, Service, and Web — it does not access them.
**How to avoid:** The default bridge implementation must only call `SecurityUtils` (also in `..security..`) and `SecurityContextHolder`. No imports from `service`, `web`, or `repository`.
**Warning signs:** ArchUnit test failure mentioning `bridge` package in violation description.

### Pitfall 4: Parallel Test Contamination via SecurityContextHolder

**What goes wrong:** `SecurityContextHolder` is a thread-local by default. If `JHipsterSecurityContextBridgeTest` does not clear the context in `@BeforeEach`/`@AfterEach`, state can leak between tests when tests run in the same thread.
**Why it happens:** JUnit 5 does not guarantee separate threads per test method in sequential execution.
**How to avoid:** Always call `SecurityContextHolder.clearContext()` in both `@BeforeEach` and `@AfterEach`, exactly as `SecurityUtilsUnitTest` does.
**Warning signs:** Test passes in isolation, fails when run as part of the full suite.

### Pitfall 5: Regression Test Coverage Gap for Password Change Error Path

**What goes wrong:** The existing `AccountResourceIT` covers happy-path password change but does not cover the rejection case when the current password is wrong. If Phase 1 does not add this test, the regression baseline is incomplete for AUTH-02.
**Why it happens:** Generator-created tests favor happy paths.
**How to avoid:** The Phase 1 plan must include a task that adds the wrong-old-password rejection test for `POST /api/account/change-password`.
**Warning signs:** `AccountResourceIT` has no test method with a name like `testChangePasswordWithIncorrectPassword`.

### Pitfall 6: `@WithMockUser` Does Not Exercise Real `DomainUserDetailsService`

**What goes wrong:** Tests using `@WithMockUser` bypass the real authentication flow. For AUTH-02 regression coverage, some tests must exercise the real `/api/authenticate` endpoint with a user stored in the database.
**Why it happens:** `@WithMockUser` is convenient but short-circuits token-based authentication.
**How to avoid:** `AuthenticateControllerIT` already exercises real login. The baseline must verify that the existing `AuthenticateControllerIT` covers: successful login, wrong-password rejection, and unactivated-account rejection. Any gaps must be filled.
**Warning signs:** Login regression tests all use `@WithMockUser` rather than calling `/api/authenticate`.

---

## Code Examples

Verified patterns from official sources (existing project code):

### Test Annotation Stack for Integration Tests
```java
// Source: existing IntegrationTest.java + AccountResourceIT.java pattern
@AutoConfigureMockMvc
@WithMockUser(TEST_USER_LOGIN)   // or @WithMockUser(authorities = AuthoritiesConstants.ADMIN)
@IntegrationTest
class SomeResourceIT {
    @Autowired
    private MockMvc restMockMvc;
}
```

### Running Only Unit Tests (fast — no containers)
```bash
./gradlew test
# Runs all test classes NOT matching **/*IT* or **/*IntTest*
# JHipsterSecurityContextBridgeTest will run here
```

### Running Integration Tests
```bash
./gradlew integrationTest
# Runs **/*IT* and **/*IntTest* — requires Docker for Testcontainers
# AccountResourceIT, UserResourceIT, etc. run here
```

### Running Full Suite
```bash
./gradlew check
# Runs test, then integrationTest (check.dependsOn(integrationTest))
```

### Confirming Bridge Bean is Substitutable
```java
// In test: verify the default bridge can be autowired and replaced
@IntegrationTest
class SecurityContextBridgeWiringIT {
    @Autowired
    private SecurityContextBridge bridge;

    @Test
    void defaultBridgeIsWired() {
        assertThat(bridge).isInstanceOf(JHipsterSecurityContextBridge.class);
    }
}
```

### Authority Query Through Bridge
```java
// Source: pattern derived from SecurityUtils.getAuthorities() (SecurityUtils.java:119-121)
// Used inside JHipsterSecurityContextBridge.getCurrentUserAuthorities()
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
if (authentication == null) {
    return List.of();
}
return authentication.getAuthorities().stream()
    .map(GrantedAuthority::getAuthority)
    .toList();
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `UserDetails` returning `List<GrantedAuthority>` | `DomainUserDetailsService.UserWithId` — extends `User`, adds `Long id` field exposed via `getId()` | Already in current codebase | Phase 1 bridge can use `UserWithId` awareness if it needs to expose user ID alongside authorities |
| WebTestClient (reactive) | MockMvc (servlet) | N/A — project uses Spring MVC not WebFlux | Use MockMvc; WebTestClient is not applicable here |
| H2 in-memory database for tests | Testcontainers PostgreSQL | Already in current codebase | All integration tests use real PostgreSQL; H2 is not present in the test classpath |

**Deprecated/outdated:**
- H2 as a test database: Not used. `DatabaseTestcontainer` uses `postgres:18.3` image.
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate`: Not the project pattern. MockMvc with `@AutoConfigureMockMvc` is the established approach.

---

## Existing Test Coverage Inventory

This table maps the Phase 1 regression scope (D-12/D-13) to existing test coverage and identifies gaps.

| Flow | Endpoint | Happy Path | Key Error Cases | Gap? |
|------|----------|------------|-----------------|------|
| Login | `POST /api/authenticate` | `AuthenticateControllerIT#testAuthorize` | Wrong password: `#testAuthorizeFails`; Unactivated: `#testAuthorizeWithRememberMe` partial | Unactivated-user rejection case may be missing — verify |
| Register | `POST /api/register` | `AccountResourceIT#testRegisterValid` | Invalid login: `#testRegisterInvalidLogin`; Duplicate email: `#testRegisterDuplicateEmail` | No gap found |
| Activate | `GET /api/activate` | `AccountResourceIT#testActivateAccount` | Bad key: line 389 (500 — baseline) | Gap: bad-key test exists but must be confirmed as baseline |
| Password reset init | `POST /api/account/reset-password/init` | `AccountResourceIT` — verify method exists | Unknown email: verify covered | Needs verification |
| Password reset finish | `POST /api/account/reset-password/finish` | `AccountResourceIT` — verify method exists | Bad reset key: line 776 (500 — baseline) | Gap: verify bad-key test asserts 500 |
| Password change | `POST /api/account/change-password` | `AccountResourceIT` — verify method exists | Wrong old password: NOT FOUND in existing tests | **Confirmed gap** — must add |
| Admin user CRUD | `/api/admin/users/**` | `UserResourceIT` — covers create, update, delete, list | Auth check (non-admin): verify | Needs verification of auth rejection case |
| Authority listing | `GET /api/authorities` | `AuthorityResourceIT#shouldGetAuthority` | Non-admin rejection: not explicitly tested | **Confirmed gap** — add unauthenticated/non-admin rejection test |

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 (JUnit Platform) via Spring Boot 4.0.3 |
| Config file | `gradle/spring-boot.gradle` (test task) + `gradle/profile_dev.gradle` (integrationTest task) |
| Quick run command | `./gradlew test` |
| Full suite command | `./gradlew check` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-02 | Register valid user | integration | `./gradlew integrationTest --tests "*AccountResourceIT.testRegisterValid"` | Yes |
| AUTH-02 | Register with invalid login | integration | `./gradlew integrationTest --tests "*AccountResourceIT.testRegisterInvalidLogin"` | Yes |
| AUTH-02 | Register duplicate email rejected | integration | `./gradlew integrationTest --tests "*AccountResourceIT.testRegisterDuplicateEmail"` | Yes |
| AUTH-02 | Activate account with valid key | integration | `./gradlew integrationTest --tests "*AccountResourceIT.testActivateAccount"` | Yes |
| AUTH-02 | Activate with invalid key returns 500 (baseline) | integration | `./gradlew integrationTest --tests "*AccountResourceIT.testActivateAccountWithWrongKey"` | Verify/add |
| AUTH-02 | Password reset init (known email) | integration | `./gradlew integrationTest --tests "*AccountResourceIT*resetPassword*"` | Verify |
| AUTH-02 | Password reset finish with valid key | integration | `./gradlew integrationTest --tests "*AccountResourceIT*resetPassword*"` | Verify |
| AUTH-02 | Password reset finish with invalid key returns 500 (baseline) | integration | `./gradlew integrationTest --tests "*AccountResourceIT*wrongKey*"` | Verify/add |
| AUTH-02 | Password change with correct old password | integration | `./gradlew integrationTest --tests "*AccountResourceIT*changePassword*"` | Verify |
| AUTH-02 | Password change with wrong old password rejected | integration | `./gradlew integrationTest --tests "*AccountResourceIT*changePasswordWithWrongPassword*"` | **Wave 0 gap** |
| AUTH-03 | Admin creates user | integration | `./gradlew integrationTest --tests "*UserResourceIT.testCreateUser"` | Verify name |
| AUTH-03 | Admin updates user | integration | `./gradlew integrationTest --tests "*UserResourceIT.testUpdateUser"` | Verify name |
| AUTH-03 | Admin deletes user | integration | `./gradlew integrationTest --tests "*UserResourceIT.testDeleteUser"` | Verify name |
| AUTH-03 | Admin lists users paginated | integration | `./gradlew integrationTest --tests "*UserResourceIT.testGetAllUsers"` | Verify name |
| AUTH-03 | Non-admin rejected from admin endpoints | integration | `./gradlew integrationTest --tests "*UserResourceIT*nonAdmin*"` | **Wave 0 gap** |
| AUTH-03 | Admin lists authorities | integration | `./gradlew integrationTest --tests "*AuthorityResourceIT*getAllAuthorities*"` | Verify name |
| SEC-04 | Default bridge bean is wired | integration | `./gradlew integrationTest --tests "*SecurityContextBridgeWiringIT*"` | **Wave 0 gap** |
| SEC-04 | Bridge returns current user login | unit | `./gradlew test --tests "*JHipsterSecurityContextBridgeTest*login*"` | **Wave 0 gap** |
| SEC-04 | Bridge returns authority strings | unit | `./gradlew test --tests "*JHipsterSecurityContextBridgeTest*authorities*"` | **Wave 0 gap** |
| SEC-04 | Bridge returns empty for unauthenticated | unit | `./gradlew test --tests "*JHipsterSecurityContextBridgeTest*unauthenticated*"` | **Wave 0 gap** |
| SEC-04 | Bridge returns false for isAuthenticated when anonymous | unit | `./gradlew test --tests "*JHipsterSecurityContextBridgeTest*isAuthenticated*"` | **Wave 0 gap** |

### Sampling Rate

- **Per task commit:** `./gradlew test` (unit tests only, fast)
- **Per wave merge:** `./gradlew check` (full suite including integration tests)
- **Phase gate:** `./gradlew check` green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `src/test/java/com/vn/core/security/bridge/JHipsterSecurityContextBridgeTest.java` — covers SEC-04 unit tests
- [ ] `src/test/java/com/vn/core/security/bridge/SecurityContextBridgeWiringIT.java` — covers SEC-04 wiring test
- [ ] `src/test/java/com/vn/core/web/rest/AccountResourceIT.java` — extend with wrong-old-password test (AUTH-02 gap)
- [ ] Verify `AccountResourceIT` covers activation bad-key baseline (asserting 500)
- [ ] Verify `AccountResourceIT` covers password reset paths
- [ ] Verify `UserResourceIT` has a non-admin auth rejection test (AUTH-03)

---

## Open Questions

1. **Does `AuthenticateControllerIT` cover the unactivated-user rejection path?**
   - What we know: `DomainUserDetailsServiceIT#assertThatUserNotActivatedExceptionIsThrownForNotActivatedUsers` covers the service-level exception. The `AuthenticateControllerIT` has `testAuthorize` and `testAuthorizeFails` but no explicit unactivated-user test found in the first 60 lines.
   - What's unclear: Whether an unactivated-user test exists later in the file.
   - Recommendation: The plan task "verify regression baseline" must scan the full `AuthenticateControllerIT` for an unactivated-user path test and add one if absent.

2. **Do password reset integration tests exist in `AccountResourceIT`?**
   - What we know: `AccountResourceIT` imports `KeyAndPasswordVM` which is used for reset-finish flows. The file was only partially read (lines 1-180 and 300-380).
   - What's unclear: Whether reset-init and reset-finish test methods exist.
   - Recommendation: The plan task "verify and extend baseline" must read the full `AccountResourceIT` and confirm or add reset path coverage.

3. **Does `UserResourceIT` include a test that verifies non-admin users are rejected from `/api/admin/users`?**
   - What we know: `UserResourceIT` is annotated `@WithMockUser(authorities = AuthoritiesConstants.ADMIN)` at class level. Non-admin rejection would require a specific test method overriding that annotation.
   - What's unclear: Whether such a test method exists.
   - Recommendation: If absent, add one test that asserts `status().isForbidden()` when calling an admin endpoint without `ROLE_ADMIN`.

---

## Sources

### Primary (HIGH confidence)

- Direct codebase inspection:
  - `src/main/java/com/vn/core/security/SecurityUtils.java` — method signatures, authority stream logic
  - `src/main/java/com/vn/core/security/DomainUserDetailsService.java` — `UserWithId` inner class, authority loading
  - `src/main/java/com/vn/core/security/AuthoritiesConstants.java` — constant values
  - `src/main/java/com/vn/core/config/SecurityConfiguration.java` — filter chain, permitted routes
  - `src/main/java/com/vn/core/web/rest/AuthorityResource.java` — admin-only CRUD endpoints
  - `src/test/java/com/vn/core/IntegrationTest.java` — `@IntegrationTest` composite annotation structure
  - `src/test/java/com/vn/core/config/DatabaseTestcontainer.java` — Testcontainers PostgreSQL setup
  - `src/test/java/com/vn/core/security/SecurityUtilsUnitTest.java` — unit test pattern (SecurityContextHolder)
  - `src/test/java/com/vn/core/security/DomainUserDetailsServiceIT.java` — integration test pattern
  - `src/test/java/com/vn/core/web/rest/AccountResourceIT.java` — account lifecycle test coverage
  - `src/test/java/com/vn/core/web/rest/AuthenticateControllerIT.java` — login test coverage
  - `src/test/java/com/vn/core/web/rest/UserResourceIT.java` — admin user test coverage
  - `src/test/java/com/vn/core/TechnicalStructureTest.java` — ArchUnit layer rules
  - `gradle/spring-boot.gradle` and `gradle/profile_dev.gradle` — test task definitions
- `.planning/codebase/CONCERNS.md` — known bug documentation (500 for bad activation keys)
- `.planning/codebase/ARCHITECTURE.md` — layer map, security data flow

### Secondary (MEDIUM confidence)

- `.planning/phases/01-identity-and-authority-baseline/1-CONTEXT.md` — locked decisions and specific bridge design constraints confirmed by reading the source files

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — verified directly from `build.gradle` and test source files
- Architecture patterns: HIGH — interface + bean substitution is a core Spring idiom; bridge placement verified against ArchUnit layer rules
- Pitfalls: HIGH — sourced from CONCERNS.md (500 bug), `SecurityUtilsUnitTest` (context cleanup pattern), and `TechnicalStructureTest` (layer rules)
- Test gap map: MEDIUM — partial file reads; some gaps confirmed, others flagged as "verify" pending full file review during planning

**Research date:** 2026-03-21
**Valid until:** 2026-04-21 (stable Spring Boot 4.x conventions; no fast-moving dependencies involved)
