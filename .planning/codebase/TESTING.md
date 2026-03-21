# Testing Patterns

**Analysis Date:** 2026-03-21

## Test Framework

**Runner:**
- Backend: JUnit 5 via Gradle `test` and `integrationTest` tasks. Root configuration lives in `gradle/spring-boot.gradle` plus `gradle/profile_dev.gradle` and `gradle/profile_prod.gradle`. `angapp/` uses the same split in `angapp/build.gradle` and its `gradle/profile_*.gradle` files.
- Frontend: Jest for Angular tests in `angapp/jest.conf.js`, wired through `@angular-builders/jest` in `angapp/angular.json`.
- Config: `src/test/resources/junit-platform.properties` and `angapp/src/test/resources/junit-platform.properties` define default timeouts and custom class ordering.

**Assertion Library:**
- Backend uses AssertJ, Hamcrest, MockMvc result matchers, and Mockito, as seen in `src/test/java/com/vn/core/web/rest/UserResourceIT.java`, `src/test/java/com/vn/core/web/rest/errors/ExceptionTranslatorIT.java`, and `angapp/src/test/java/com/mycompany/myapp/service/impl/OrganizationServiceImplTest.java`.
- Frontend uses Jest `expect`, Angular `TestBed`, and `HttpTestingController`, as seen in `angapp/src/main/webapp/app/core/auth/account.service.spec.ts` and `angapp/src/main/webapp/app/entities/organization/service/organization.service.spec.ts`.

**Run Commands:**
```bash
./gradlew test integrationTest
npm run backend:unit:test
cd angapp && ./gradlew test integrationTest -x webapp -x webapp_test
cd angapp && npm test
cd angapp && npm run jest
```

## Test File Organization

**Location:**
- Backend tests are mirrored under `src/test/java/...` and `angapp/src/test/java/...`.
- Angular tests are co-located with the implementation under `angapp/src/main/webapp/app/**`, for example `account.service.ts` and `account.service.spec.ts`.

**Naming:**
- Use `*IT` for Spring integration tests that boot the application context, for example `src/test/java/com/vn/core/web/rest/AccountResourceIT.java` and `angapp/src/test/java/com/mycompany/myapp/web/rest/OrganizationResourceIT.java`.
- Use `*Test` or `*Tests` for unit tests and structural checks, for example `src/test/java/com/vn/core/service/mapper/UserMapperTest.java`, `src/test/java/com/vn/core/TechnicalStructureTest.java`, and `angapp/src/test/java/com/mycompany/core/fetch/FetchPlanBuilderTest.java`.
- Use `*.spec.ts` for Angular tests, for example `angapp/src/main/webapp/app/core/auth/account.service.spec.ts`.

**Structure:**
```text
src/test/java/com/vn/core/...                  # root backend tests
angapp/src/test/java/com/mycompany/myapp/...  # angapp backend tests
angapp/src/test/java/com/mycompany/core/...   # custom security/fetch/serializer tests
angapp/src/main/webapp/app/**/**/*.spec.ts    # Angular co-located tests
```

## Integration-Test Setup

**Root Service Setup:**
- Annotate Spring integration tests with the composite `@IntegrationTest` from `src/test/java/com/vn/core/IntegrationTest.java`.
- Use `@AutoConfigureMockMvc` and usually `@WithMockUser` for REST tests, as shown in `src/test/java/com/vn/core/web/rest/UserResourceIT.java`, `AccountResourceIT.java`, and `AuthorityResourceIT.java`.
- Root integration tests import a shared PostgreSQL Testcontainer through `src/test/java/com/vn/core/config/DatabaseTestcontainer.java`. The container is defined once, reused, and registered through `@DynamicPropertySource`.

**angapp Setup:**
- Annotate Spring integration tests with `@IntegrationTest` from `angapp/src/test/java/com/mycompany/myapp/IntegrationTest.java`.
- `angapp` uses `@EmbeddedSQL` plus `angapp/src/test/java/com/mycompany/myapp/config/SqlTestContainersSpringContextCustomizerFactory.java` to inject datasource properties when the `testprod` profile is active.
- Keep the custom container abstraction behind `angapp/src/test/java/com/mycompany/myapp/config/SqlTestContainer.java`.

**Execution Ordering:**
- Both app trees sort non-Spring tests before full context tests through `src/test/java/com/vn/core/config/SpringBootTestClassOrderer.java` and `angapp/src/test/java/com/mycompany/myapp/config/SpringBootTestClassOrderer.java`.
- Both JUnit platform property files enforce method timeouts and class ordering in `src/test/resources/junit-platform.properties` and `angapp/src/test/resources/junit-platform.properties`.

## Test Structure

**Suite Organization:**
```java
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
@IntegrationTest
class UserResourceIT {

    @Autowired
    private MockMvc restUserMockMvc;

    @BeforeEach
    void initTest() {
        user = initTestUser();
    }

    @Test
    @Transactional
    void createUser() throws Exception {
        // arrange, act with MockMvc, assert persisted state
    }
}
```

```typescript
describe('Account Service', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideHttpClientTesting(), StateStorageService],
    });
  });

  it('should call account saving endpoint with correct values', () => {
    service.save(account).subscribe();
    const testRequest = httpMock.expectOne({ method: 'POST', url: applicationConfigService.getEndpointFor('api/account') });
    testRequest.flush({});
    expect(testRequest.request.body).toEqual(account);
  });
});
```

**Patterns:**
- Arrange fixtures with static factory methods or test samples, then assert both HTTP responses and persisted state. See `src/test/java/com/vn/core/web/rest/UserResourceIT.java` and `angapp/src/test/java/com/mycompany/myapp/web/rest/OrganizationResourceIT.java`.
- Use `@Transactional` on backend tests that touch persistence so each test runs in an isolated transaction, as seen throughout `src/test/java/com/vn/core/web/rest/*.java` and `angapp/src/test/java/com/mycompany/myapp/web/rest/*.java`.
- In Angular tests, initialize `TestBed`, inject collaborators, subscribe to the observable under test, then flush the request through `HttpTestingController`.

## Mocking

**Framework:** Mockito on the backend, Jest mocks and Angular HTTP testing on the frontend.

**Patterns:**
```java
OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
SecureDataManager secureDataManager = mock(SecureDataManager.class);
when(secureDataManager.loadOne(Organization.class, 11L, "organization-detail")).thenReturn(Map.of(...));
```

```typescript
jest.mock('app/core/auth/state-storage.service');
jest.spyOn(mockRouter, 'navigateByUrl').mockImplementation(() => Promise.resolve(true));
const req = httpMock.expectOne({ method: 'GET' });
req.flush({});
```

**What to Mock:**
- Mock collaborators for isolated backend unit tests, for example repositories, mappers, and secure data services in `angapp/src/test/java/com/mycompany/myapp/service/impl/OrganizationServiceImplTest.java`.
- Mock role or security collaborators in focused custom-core tests, for example `angapp/src/test/java/com/mycompany/core/security/permission/AttributePermissionEvaluatorImplTest.java`.
- Mock Angular infrastructure services such as router, storage, and HTTP in `angapp/src/main/webapp/app/core/auth/account.service.spec.ts`.

**What NOT to Mock:**
- Do not mock the REST layer in backend integration tests. Use the real Spring context with `MockMvc`, as shown in `src/test/java/com/vn/core/web/rest/UserResourceIT.java` and `angapp/src/test/java/com/mycompany/myapp/web/rest/DepartmentResourceIT.java`.
- Do not mock persistence for root integration tests that already boot Postgres through `DatabaseTestcontainer`.

## Fixtures and Factories

**Test Data:**
```java
public static User createEntity() { ... }
public static User initTestUser() { ... }
```

```typescript
export const sampleWithRequiredData: IOrganization = { ... };
Object.freeze(sampleWithRequiredData);
```

**Location:**
- Root REST fixtures and helpers live in `src/test/java/com/vn/core/web/rest/UserResourceIT.java` and `src/test/java/com/vn/core/web/rest/TestUtil.java`.
- Domain assertion helpers and samples live under `src/test/java/com/vn/core/domain/` and `angapp/src/test/java/com/mycompany/myapp/domain/`, for example `AuthorityAsserts.java`, `OrganizationAsserts.java`, and `OrganizationTestSamples.java`.
- Angular entity fixtures live next to the feature, for example `angapp/src/main/webapp/app/entities/organization/organization.test-samples.ts`.
- Patch assertions use `createUpdateProxyForBean` from `src/test/java/com/vn/core/web/rest/TestUtil.java` and its `angapp` equivalent.

## Coverage

**Requirements:** No numeric coverage threshold is enforced in the explored Gradle or Jest config. Coverage is collected, but not gated.

**View Coverage:**
```bash
cd angapp && npm test
cd angapp && npm run jest
./gradlew testReport integrationTestReport
```

- Angular/Jest writes coverage and reports under `angapp/build/test-results/` per `angapp/jest.conf.js`.
- Backend Gradle generates aggregated HTML reports through `testReport` and `integrationTestReport` in `gradle/spring-boot.gradle` and `angapp/build.gradle`.

## Test Types

**Unit Tests:**
- Root backend unit coverage focuses on utilities, mappers, structure, and config fragments: `src/test/java/com/vn/core/service/mapper/UserMapperTest.java`, `src/test/java/com/vn/core/security/SecurityUtilsUnitTest.java`, `src/test/java/com/vn/core/config/CRLFLogConverterTest.java`, and `src/test/java/com/vn/core/TechnicalStructureTest.java`.
- `angapp` adds unit tests for custom fetch/security/serializer logic in `angapp/src/test/java/com/mycompany/core/fetch/FetchPlanBuilderTest.java`, `YamlFetchPlanRepositoryTest.java`, `SecureEntitySerializerImplTest.java`, and `AttributePermissionEvaluatorImplTest.java`.
- Angular unit tests cover services, directives, pipes, forms, and components, for example `angapp/src/main/webapp/app/core/auth/account.service.spec.ts`, `angapp/src/main/webapp/app/shared/sort/sort.directive.spec.ts`, and `angapp/src/main/webapp/app/entities/organization/update/organization-form.service.spec.ts`.

**Integration Tests:**
- Root backend integration tests focus on REST endpoints, security, mail, and user lifecycle in `src/test/java/com/vn/core/web/rest/*.java`, `src/test/java/com/vn/core/security/jwt/*.java`, `src/test/java/com/vn/core/service/UserServiceIT.java`, and `src/test/java/com/vn/core/service/MailServiceIT.java`.
- `angapp` integration tests cover generated endpoints plus newer domain endpoints in `angapp/src/test/java/com/mycompany/myapp/web/rest/OrganizationResourceIT.java`, `DepartmentResourceIT.java`, and `SpaWebFilterIT.java`.

**E2E Tests:**
- E2E packaging and server scripts exist in `package.json` and `angapp/package.json` (`ci:e2e:*` and `-Pe2e`), but no browser-level E2E specs or a framework like Cypress/Playwright are detected in the explored files.

## Common Patterns

**Async Testing:**
```typescript
it('should navigate to the previous stored url post successful authentication', () => {
  service.identity().subscribe();
  httpMock.expectOne({ method: 'GET' }).flush({});
  expect(mockRouter.navigateByUrl).toHaveBeenCalledWith('admin/users?page=0');
});
```

```java
restUserMockMvc
    .perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
    .andExpect(status().isCreated());
```

**Error Testing:**
```java
restUserMockMvc
    .perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userDTO)))
    .andExpect(status().isBadRequest());
```

```typescript
service.identity().subscribe();
httpMock.expectOne({ method: 'GET' }).error(new ProgressEvent(''));
expect(mockRouter.navigateByUrl).not.toHaveBeenCalled();
```

## Helpers and Reusable Test Utilities

**Backend Helpers:**
- `src/test/java/com/vn/core/web/rest/TestUtil.java` centralizes equality checks, date/number matchers, entity listing, and partial-update proxy generation.
- `src/test/java/com/vn/core/web/rest/WithUnauthenticatedMockUser.java` and `src/test/java/com/vn/core/security/jwt/JwtAuthenticationTestUtils.java` provide security test support. `angapp/` mirrors these helpers under `angapp/src/test/java/com/mycompany/myapp/`.

**Frontend Helpers:**
- Entity sample files such as `angapp/src/main/webapp/app/entities/organization/organization.test-samples.ts` provide frozen fixtures for service and form tests.
- `ApplicationConfigService` is injected rather than re-created in specs, which keeps URL formation behavior aligned with production code in `angapp/src/main/webapp/app/core/config/application-config.service.ts`.

## Obvious Coverage Gaps

**Root Backend:**
- Only 34 Java test files are present for 68 root `src/main/java` files. Tests heavily cover `web/rest`, security, mail, and user management, but no direct tests were detected for many config classes beyond `WebConfigurer`, `CRLFLogConverter`, and timezone handling in `src/main/java/com/vn/core/config/`.
- No frontend test suite exists for the root project. Frontend testing only exists under `angapp/`.

**angapp Backend:**
- `angapp/src/main/java/com/mycompany/core/fetch/` and `angapp/src/main/java/com/mycompany/core/serialize/` have focused unit coverage, but no tests were detected for most of `angapp/src/main/java/com/mycompany/core/security/access/`, `angapp/src/main/java/com/mycompany/core/security/row/`, `angapp/src/main/java/com/mycompany/core/security/core/`, `angapp/src/main/java/com/mycompany/core/merge/`, or `angapp/src/main/java/com/mycompany/core/repository/SpringRepositoryRegistry.java`.
- `angapp/src/main/java/com/mycompany/myapp/service/impl/OrganizationServiceImpl.java` has a dedicated unit test, but no equivalent implementation tests were detected for `EmployeeServiceImpl.java` or `DepartmentServiceImpl.java`.

**angapp Frontend:**
- There are 73 Angular `*.spec.ts` files for 314 files under `angapp/src/main/webapp/app/`. Entity areas `sec-role`, `sec-permission`, and `sec-row-policy` have implementation files but no matching `*.spec.ts` files were detected under their feature directories.

---

*Testing analysis: 2026-03-21*
