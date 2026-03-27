# Testing Patterns

**Analysis Date:** 2026-03-27

## Test Framework

**Runner:**
- Backend unit and integration tests use JUnit 5 through Gradle `test` and `integrationTest` in `gradle/spring-boot.gradle`, `gradle/profile_dev.gradle`, and `gradle/profile_prod.gradle`.
- Backend integration suites share the meta-annotation in `src/test/java/com/vn/core/IntegrationTest.java`.
- Backend JWT slice tests use the narrower meta-annotation in `src/test/java/com/vn/core/security/jwt/AuthenticationIntegrationTest.java`.
- Frontend unit tests use Angular's `@angular/build:unit-test` builder from `frontend/angular.json`; `frontend/tsconfig.spec.json` loads `vitest/globals`, so specs use `describe`, `it`, `expect`, and `vi`.
- Browser end-to-end tests use Playwright from `frontend/playwright.config.ts`.

**Assertion Library:**
- Backend uses AssertJ, Hamcrest JSON matchers, MockMvc result matchers, and Mockito verification helpers.
- Frontend unit tests use Vitest expectations, Angular `TestBed`, and `HttpTestingController`.
- E2E tests use Playwright `expect`.

**Run Commands:**
```bash
./gradlew test
./gradlew integrationTest
./gradlew jacocoTestReport
npm run backend:unit:test
npm --prefix frontend run test
npm --prefix frontend run test -- --watch
npm --prefix frontend run e2e
npm --prefix frontend run e2e:headed
```

## Test File Organization

**Location:**
- Backend unit and integration tests live under `src/test/java/com/vn/core/**` and mirror the runtime package layout from `src/main/java/com/vn/core/**`.
- Backend test resources live under `src/test/resources/**` for profile overrides, Liquibase changelogs, seeded security data, mail templates, and fetch plans.
- Frontend unit specs are co-located next to source under `frontend/src/app/**` and `frontend/src/*.spec.ts`.
- Playwright suites live under `frontend/e2e/*.spec.ts`.
- `angapp/` retains its own separate test stack in `angapp/package.json`, but the active frontend verification path is `frontend/`.

**Naming:**
- Backend unit tests use `*Test.java`.
- Backend integration tests use `*IT.java`.
- Shared test annotations and helpers use descriptive names such as `IntegrationTest.java`, `AuthenticationIntegrationTest.java`, and `WithUnauthenticatedMockUser.java`.
- Frontend unit and e2e files use `*.spec.ts`.

**Structure:**
```text
src/test/java/com/vn/core/...                  backend unit + integration tests
src/test/resources/config/...                  backend test profiles
src/test/resources/config/liquibase/...        test schema + seeded proof data
src/test/resources/fetch-plans-test.yml        fetch-plan fixture
frontend/src/app/.../*.spec.ts                 co-located Angular unit tests
frontend/e2e/*.spec.ts                         Playwright browser suites
```

## Test Structure

**Suite Organization:**
```java
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class SecuredEntityEnforcementIT {
  @Autowired
  private MockMvc restMockMvc;

  @Test
  @WithMockUser(username = "proof-owner", authorities = "ROLE_PROOF_READER")
  void getOrganizations_returnsOwnedRowsOnly() throws Exception {
    restMockMvc.perform(get("/api/organizations?sort=id,asc")).andExpect(status().isOk());
  }
}
```

```typescript
beforeEach(() => {
  TestBed.configureTestingModule({
    imports: [OrganizationListComponent],
    providers: [...],
  });
});

it('renders the list', async () => {
  const fixture = TestBed.createComponent(OrganizationListComponent);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
});
```

**Patterns:**
- Backend integration tests compose `@IntegrationTest`, `@AutoConfigureMockMvc`, optional `@Transactional`, and `@WithMockUser` or request-level `user(...)`, as in `src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java`.
- Backend unit tests use `@ExtendWith(MockitoExtension.class)`, `@Mock`, and `@InjectMocks`, as in `src/test/java/com/vn/core/security/data/SecureDataManagerImplTest.java` and `src/test/java/com/vn/core/service/security/SecuredEntityCapabilityServiceTest.java`.
- Angular service specs usually configure providers only, then use `firstValueFrom(...)` plus `HttpTestingController`, as in `frontend/src/app/layout/navigation/navigation.service.spec.ts`.
- Angular component specs import the real standalone component, set route and service doubles explicitly, then assert against `fixture.nativeElement`, as in `frontend/src/app/pages/entities/organization/list/organization-list.component.spec.ts`.

## Mocking

**Framework:** Mockito on the backend, `vi.fn()` and `HttpTestingController` in Angular unit tests, `page.route(...)` and `playwright.request.newContext(...)` in Playwright suites.

**Patterns:**
```java
@ExtendWith(MockitoExtension.class)
class SecureDataManagerImplTest {
  @Mock
  private DataManager dataManager;

  @InjectMocks
  private SecureDataManagerImpl secureDataManager;
}
```

```typescript
const router = { navigate: vi.fn(() => Promise.resolve(true)) };

TestBed.configureTestingModule({
  providers: [{ provide: Router, useValue: router }],
});
```

**What to Mock:**
- Mock repositories, metamodel access, collaborators, and security services in backend unit tests.
- Mock HTTP transport with `HttpTestingController` for Angular services and interceptors.
- Stub route data, router navigation, translations, and PrimeNG services in Angular component specs.
- Use `page.route(...)` in `frontend/e2e/user-management.spec.ts` when the scenario only needs shell behavior and not a live backend contract.

**What NOT to Mock:**
- Do not mock the Spring MVC stack, Liquibase schema, or PostgreSQL container in backend `*IT` suites; those tests are meant to verify the full API and security contract.
- Do not replace standalone Angular components with shallow shells in view specs; import the real component and test the rendered DOM.
- Do not re-create proof security seed data in every backend test when it already exists in `src/test/resources/config/liquibase/changelog/20260321000800_seed_proof_security_test_data.xml`.

## Fixtures and Factories

**Test Data:**
```java
// `src/test/java/com/vn/core/web/rest/UserResourceIT.java`
public static User createEntity() { ... }
public static User initTestUser() { ... }
```

```typescript
// `frontend/e2e/user-management.spec.ts`
function buildUsers(targetAuthorities: string[]): MockUser[] { ... }
```

**Location:**
- Backend factories are usually local static helpers inside each test class, for example `src/test/java/com/vn/core/web/rest/UserResourceIT.java`.
- Reusable backend sample data also lives in helper files such as `src/test/java/com/vn/core/domain/AuthorityTestSamples.java`.
- Shared backend utilities and annotations live in `src/test/java/com/vn/core/IntegrationTest.java`, `src/test/java/com/vn/core/web/rest/TestUtil.java`, `src/test/java/com/vn/core/web/rest/WithUnauthenticatedMockUser.java`, and `src/test/java/com/vn/core/security/jwt/JwtAuthenticationTestUtils.java`.
- Database seed fixtures live in `src/test/resources/config/liquibase/test-master.xml` and `src/test/resources/config/liquibase/changelog/20260321000800_seed_proof_security_test_data.xml`.
- Fetch-plan fixtures live in `src/test/resources/fetch-plans-test.yml`.
- Frontend fixture data is mostly inline per spec near the suite it serves; there is no shared fixture library in `frontend/` yet.

## Helper Infrastructure

- `src/test/java/com/vn/core/IntegrationTest.java` imports `DatabaseTestcontainer`, `JacksonConfiguration`, and async overrides so integration suites boot a consistent Spring test application.
- `src/test/java/com/vn/core/config/DatabaseTestcontainer.java` provides a reusable PostgreSQL 18.3 container and wires datasource properties through `@DynamicPropertySource`.
- `src/test/resources/junit-platform.properties` sets global JUnit timeouts and orders classes with `src/test/java/com/vn/core/config/SpringBootTestClassOrderer.java`.
- `src/test/java/com/vn/core/security/jwt/AuthenticationIntegrationTest.java` provides a targeted JWT-authentication slice without starting the entire application graph.
- `src/test/java/com/vn/core/web/rest/TestUtil.java` contains shared JSON, date, number, entity-manager, and proxy helpers for REST tests.
- Playwright suites use `playwright.request.newContext({ baseURL: 'http://localhost:4200' })` for admin setup and cleanup in `frontend/e2e/proof-role-gating.spec.ts` and `frontend/e2e/security-comprehensive.spec.ts`.

## Coverage

**Requirements:** No explicit coverage threshold is enforced for either the backend or the active `frontend/` app.

**View Coverage:**
```bash
./gradlew jacocoTestReport
npm --prefix frontend run test -- --code-coverage
```

- Backend coverage is emitted by JaCoCo from `buildSrc/src/main/groovy/jhipster.code-quality-conventions.gradle`.
- `sonar-project.properties` consumes backend JUnit reports from `build/test-results/test` and `build/test-results/integrationTest`, plus JaCoCo XML from `build/reports/jacoco/test/jacocoTestReport.xml`.
- The active `frontend/` app has no dedicated committed coverage reporter config or coverage script. Use Angular builder flags explicitly when coverage is needed.
- Legacy `angapp/` still exposes `npm --prefix angapp run test` with coverage flags in `angapp/package.json`, but that is no longer the primary frontend test path.

## Test Types

**Unit Tests:**
- Backend unit coverage is concentrated in `src/test/java/com/vn/core/security/**`, `src/test/java/com/vn/core/service/**`, `src/test/java/com/vn/core/management/**`, and mapper/domain helper tests.
- Angular unit tests cover guards, interceptors, navigation services, list and detail components, admin user-management flows, and admin security screens under `frontend/src/app/**`.

**Integration Tests:**
- Backend integration tests hit real controllers through MockMvc, testcontainers PostgreSQL, Liquibase migrations, and Spring Security annotations.
- REST contract suites such as `src/test/java/com/vn/core/web/rest/UserResourceIT.java`, `src/test/java/com/vn/core/web/rest/SecuredEntityEnforcementIT.java`, and `src/test/java/com/vn/core/web/rest/errors/ExceptionTranslatorIT.java` verify transport shape and security behavior together.
- Specialized integration slices exist for JWT authentication and metrics under `src/test/java/com/vn/core/security/jwt/**`.

**E2E Tests:**
- Playwright suites in `frontend/e2e` cover real browser flows against `http://localhost:4200`.
- Some suites seed and tear down data through live admin APIs, for example `frontend/e2e/proof-role-gating.spec.ts` and `frontend/e2e/security-comprehensive.spec.ts`.
- Other suites isolate shell behavior by mocking API routes in-browser, for example `frontend/e2e/user-management.spec.ts`.

## Common Patterns

**Async Testing:**
```typescript
const fixture = TestBed.createComponent(OrganizationListComponent);
fixture.detectChanges();
await fixture.whenStable();
fixture.detectChanges();
```

```typescript
const queryPromise = firstValueFrom(service.query());
httpMock.expectOne(req => req.url === 'api/security/menu-permissions').flush(...);
expect(await queryPromise).toEqual(...);
```

**Error Testing:**
```java
mockMvc.perform(get("/api/exception-translator-test/access-denied"))
  .andExpect(status().isForbidden())
  .andExpect(jsonPath("$.message").value("error.http.403"));
```

```typescript
permissionService.create.mockReturnValue(throwError(() => new Error('network error')));
component.flushChanges();
expect(component.pendingChanges.has('organization:CREATE')).toBe(true);
```

---

*Testing analysis: 2026-03-27*
