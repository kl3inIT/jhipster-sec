/**
 * Comprehensive security feature E2E tests
 *
 * Covers the full security contract for the JHipster Security Platform:
 *   1. Authentication — login, logout, session persistence, invalid credentials
 *   2. Entity-level permissions — UI gating per role (buttons, list rows, detail actions)
 *   3. Route guards — direct-URL navigation blocked for unauthorized roles
 *   4. Attribute-level permissions — fields hidden / present based on capability
 *   5. Capability cache invalidation — switching roles yields correct permissions
 *   6. Access Denied page — renders for blocked routes, provides navigation back
 *
 * Roles under test (created in beforeAll, torn down in afterAll):
 *   ROLE_PROOF_READER   — READ only on Organization, Department, Employee; DENY VIEW on Employee.salary
 *   ROLE_PROOF_EDITOR   — Full CRUD on Organization; DENY EDIT on Organization.budget
 *   ROLE_PROOF_NONE     — No permissions (empty = deny-all)
 *
 * Accounts (reused from proof-role-gating.spec.ts seed, recreated idempotently):
 *   e2e-proof-reader / Password1!
 *   e2e-proof-editor / Password1!
 *   e2e-proof-none   / Password1!
 */

import { test, expect, request, APIRequestContext, Page } from '@playwright/test';

// ─── Timeouts ────────────────────────────────────────────────────────────────

test.setTimeout(120_000);
const CAPABILITY_TIMEOUT = 20_000; // time for capability API + Angular signals to settle
const NAV_TIMEOUT = 15_000; // time for a full page navigation

// ─── Proof-role seed data ─────────────────────────────────────────────────────

const PROOF_ROLES = [
  { name: 'ROLE_PROOF_READER', displayName: 'Proof Reader', type: 'RESOURCE' },
  { name: 'ROLE_PROOF_EDITOR', displayName: 'Proof Editor', type: 'RESOURCE' },
  { name: 'ROLE_PROOF_NONE', displayName: 'Proof None', type: 'RESOURCE' },
];

const PROOF_PERMISSIONS = [
  // READER: read-only on all three entities; salary hidden
  {
    authorityName: 'ROLE_PROOF_READER',
    targetType: 'ENTITY',
    target: 'ORGANIZATION',
    action: 'READ',
    effect: 'ALLOW',
  },
  {
    authorityName: 'ROLE_PROOF_READER',
    targetType: 'ENTITY',
    target: 'DEPARTMENT',
    action: 'READ',
    effect: 'ALLOW',
  },
  {
    authorityName: 'ROLE_PROOF_READER',
    targetType: 'ENTITY',
    target: 'EMPLOYEE',
    action: 'READ',
    effect: 'ALLOW',
  },
  {
    authorityName: 'ROLE_PROOF_READER',
    targetType: 'ATTRIBUTE',
    target: 'EMPLOYEE.SALARY',
    action: 'VIEW',
    effect: 'DENY',
  },
  // EDITOR: full CRUD on Organization; budget not editable
  {
    authorityName: 'ROLE_PROOF_EDITOR',
    targetType: 'ENTITY',
    target: 'ORGANIZATION',
    action: 'CREATE',
    effect: 'ALLOW',
  },
  {
    authorityName: 'ROLE_PROOF_EDITOR',
    targetType: 'ENTITY',
    target: 'ORGANIZATION',
    action: 'READ',
    effect: 'ALLOW',
  },
  {
    authorityName: 'ROLE_PROOF_EDITOR',
    targetType: 'ENTITY',
    target: 'ORGANIZATION',
    action: 'UPDATE',
    effect: 'ALLOW',
  },
  {
    authorityName: 'ROLE_PROOF_EDITOR',
    targetType: 'ENTITY',
    target: 'ORGANIZATION',
    action: 'DELETE',
    effect: 'ALLOW',
  },
  {
    authorityName: 'ROLE_PROOF_EDITOR',
    targetType: 'ENTITY',
    target: 'DEPARTMENT',
    action: 'READ',
    effect: 'ALLOW',
  },
  {
    authorityName: 'ROLE_PROOF_EDITOR',
    targetType: 'ENTITY',
    target: 'EMPLOYEE',
    action: 'READ',
    effect: 'ALLOW',
  },
  {
    authorityName: 'ROLE_PROOF_EDITOR',
    targetType: 'ATTRIBUTE',
    target: 'ORGANIZATION.BUDGET',
    action: 'EDIT',
    effect: 'DENY',
  },
  // NONE: no permissions at all
];

const PROOF_USERS = [
  { login: 'e2e-proof-reader', email: 'e2e-reader@example.com', role: 'ROLE_PROOF_READER' },
  { login: 'e2e-proof-editor', email: 'e2e-editor@example.com', role: 'ROLE_PROOF_EDITOR' },
  { login: 'e2e-proof-none', email: 'e2e-none@example.com', role: 'ROLE_PROOF_NONE' },
];

const PASSWORD = 'Password1!';

// ─── API helpers ──────────────────────────────────────────────────────────────

async function getAdminToken(api: APIRequestContext): Promise<string> {
  const res = await api.post('/api/authenticate', {
    data: { username: 'admin', password: 'admin', rememberMe: false },
  });
  expect(res.status()).toBe(200);
  const body = await res.json();
  return body.id_token as string;
}

async function setupProofData(api: APIRequestContext, token: string): Promise<void> {
  const h = { Authorization: `Bearer ${token}` };

  for (const role of PROOF_ROLES) {
    await api.post('/api/admin/sec/roles', { data: role, headers: h });
  }
  for (const perm of PROOF_PERMISSIONS) {
    await api.post('/api/admin/sec/permissions', { data: perm, headers: h });
  }
  for (const u of PROOF_USERS) {
    const reg = await api.post('/api/register', {
      data: { login: u.login, email: u.email, password: PASSWORD, langKey: 'en' },
    });
    expect([201, 400]).toContain(reg.status());

    const getRes = await api.get(`/api/admin/users/${u.login}`, { headers: h });
    expect(getRes.status()).toBe(200);
    const existing = await getRes.json();

    const putRes = await api.put('/api/admin/users', {
      data: { ...existing, activated: true, authorities: [u.role] },
      headers: h,
    });
    expect(putRes.status()).toBe(200);
  }
}

async function teardownProofData(api: APIRequestContext, token: string): Promise<void> {
  const h = { Authorization: `Bearer ${token}` };
  for (const u of PROOF_USERS) {
    await api.delete(`/api/admin/users/${u.login}`, { headers: h });
  }
}

/** Create a test organization via admin API and return its id. Falls back to an existing org when create is forbidden. */
async function createTestOrg(api: APIRequestContext, token: string): Promise<number> {
  const res = await api.post('/api/organizations', {
    data: { code: 'E2E-TEST', name: 'E2E Test Org', ownerLogin: 'admin', budget: 9999.0 },
    headers: { Authorization: `Bearer ${token}` },
  });

  if ([200, 201].includes(res.status())) {
    testOrgCreated = true;
    const body = await res.json();
    return (body.id ?? body.body?.id) as number;
  }

  const existingRes = await api.get('/api/organizations?page=0&size=1&sort=id,asc', {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(existingRes.status()).toBe(200);
  const existingBody = (await existingRes.json()) as Array<{ id?: number }>;
  const existingId = existingBody[0]?.id;
  expect(existingId).toBeTruthy();
  testOrgCreated = false;
  return existingId as number;
}

async function deleteTestOrg(api: APIRequestContext, token: string, id: number): Promise<void> {
  await api.delete(`/api/organizations/${id}`, { headers: { Authorization: `Bearer ${token}` } });
}

// ─── Browser helpers ──────────────────────────────────────────────────────────

async function loginAs(page: Page, login: string, password = PASSWORD): Promise<void> {
  await page.goto('/login');
  await page.locator('#username').fill(login);
  await page.locator('input[type="password"]').fill(password);
  await page.getByRole('button', { name: /sign in/i }).click();
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: NAV_TIMEOUT });
}

async function loginAsAdmin(page: Page): Promise<void> {
  await loginAs(page, 'admin', 'admin');
}

async function logout(page: Page): Promise<void> {
  // Click the topbar logout button (aria-label or icon button)
  const logoutBtn = page.locator('button[aria-label="Logout"], button:has(.pi-sign-out)').first();
  await logoutBtn.click();
  await page.waitForURL((url) => url.pathname.includes('/login'), { timeout: NAV_TIMEOUT });
}

interface MockEntityCapability {
  code: string;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
  attributes: Array<{
    name: string;
    canView: boolean;
    canEdit: boolean;
  }>;
}

async function mockNavigationGrants(page: Page, allowedNodeIds: string[]): Promise<void> {
  await page.route('**/api/security/navigation-grants?*', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        appName: 'jhipster-security-platform',
        allowedNodeIds,
      }),
    });
  });
}

async function mockEntityCapabilities(
  page: Page,
  capabilities: MockEntityCapability[],
): Promise<void> {
  await page.route('**/api/security/entity-capabilities', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(capabilities),
    });
  });
}

function buildCapability(
  code: string,
  overrides: Partial<MockEntityCapability> = {},
): MockEntityCapability {
  return {
    code,
    canCreate: false,
    canRead: false,
    canUpdate: false,
    canDelete: false,
    attributes: [],
    ...overrides,
  };
}

async function loginAsAdminWithShellMocks(
  page: Page,
  allowedNodeIds: string[],
  capabilities?: MockEntityCapability[],
): Promise<void> {
  await mockNavigationGrants(page, allowedNodeIds);
  if (capabilities) {
    await mockEntityCapabilities(page, capabilities);
  }
  await loginAsAdmin(page);
}

// ─── Suite setup ─────────────────────────────────────────────────────────────

let adminToken: string;
let testOrgId: number;
let testOrgCreated = false;

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    window.sessionStorage.setItem('locale', 'en');
  });
});

test.beforeAll(async ({ playwright }) => {
  const api = await playwright.request.newContext({ baseURL: 'http://localhost:4200' });
  adminToken = await getAdminToken(api);
  await setupProofData(api, adminToken);
  testOrgId = await createTestOrg(api, adminToken);
  await api.dispose();
});

test.afterAll(async ({ playwright }) => {
  const api = await playwright.request.newContext({ baseURL: 'http://localhost:4200' });
  if (testOrgId && testOrgCreated) {
    await deleteTestOrg(api, adminToken, testOrgId);
  }
  await teardownProofData(api, adminToken);
  await api.dispose();
});

// ═════════════════════════════════════════════════════════════════════════════
// 1. Authentication
// ═════════════════════════════════════════════════════════════════════════════

test.describe('1. Authentication', () => {
  test('valid credentials redirect to dashboard', async ({ page }) => {
    await page.goto('/login');
    await page.locator('#username').fill('admin');
    await page.locator('input[type="password"]').fill('admin');
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: NAV_TIMEOUT });
    await expect(page).not.toHaveURL(/\/login/);
  });

  test('invalid credentials show error and stay on /login', async ({ page }) => {
    await page.goto('/login');
    await page.locator('#username').fill('admin');
    await page.locator('input[type="password"]').fill('wrong-password-xyz');
    await page.getByRole('button', { name: /sign in/i }).click();

    // Must not navigate away — still on /login
    await page.waitForTimeout(3000);
    await expect(page).toHaveURL(/\/login/);

    // Error message must appear
    const errorMsg = page
      .locator('.p-message-error, [class*="alert-danger"], [class*="error"]')
      .first();
    await expect(errorMsg).toBeVisible({ timeout: 5000 });
  });

  test('logout navigates immediately to /login without page reload', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/entities/organization');

    const errors: string[] = [];
    page.on('pageerror', (err) => errors.push(err.message));

    // Click logout
    const logoutBtn = page.locator('button[aria-label="Logout"], button:has(.pi-sign-out)').first();
    await logoutBtn.click();

    // Must navigate to /login within 5s — no reload required
    await expect(page).toHaveURL(/\/login/, { timeout: 5000 });
    expect(errors).toHaveLength(0);
  });

  test('after logout, protected route redirects to /login', async ({ page }) => {
    await loginAsAdmin(page);
    await logout(page);

    // Attempt direct navigation to a protected route
    await page.goto('/entities/organization');
    await expect(page).toHaveURL(/\/login/, { timeout: NAV_TIMEOUT });
  });

  test('session persists across page reload within same tab', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/entities/organization');
    await page.reload();

    // After reload, still on the org list — not redirected to /login
    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: NAV_TIMEOUT });
    await expect(page).not.toHaveURL(/\/login/);
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// 2. READER role — entity-level UI gating
// ═════════════════════════════════════════════════════════════════════════════

test.describe('2. READER — entity-level UI gating', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'e2e-proof-reader');
  });

  test('organization list: "New Organization" button is absent', async ({ page }) => {
    await page.goto('/entities/organization');
    await page.waitForSelector('p-table', { timeout: NAV_TIMEOUT });
    await expect(page.getByRole('button', { name: /new organization/i })).not.toBeVisible();
  });

  test('organization list: no Edit buttons in any row', async ({ page }) => {
    await page.goto('/entities/organization');
    await page.waitForSelector('p-table', { timeout: NAV_TIMEOUT });
    // Wait for capability to resolve before asserting absence
    await page.waitForTimeout(CAPABILITY_TIMEOUT);
    await expect(page.locator('button[aria-label="Edit"]')).not.toBeVisible();
  });

  test('organization list: no Delete buttons in any row', async ({ page }) => {
    await page.goto('/entities/organization');
    await page.waitForSelector('p-table', { timeout: NAV_TIMEOUT });
    await page.waitForTimeout(CAPABILITY_TIMEOUT);
    await expect(page.locator('button[aria-label="Delete"]')).not.toBeVisible();
  });

  test('organization detail: "Edit" button is absent', async ({ page }) => {
    await page.goto(`/entities/organization/${testOrgId}/view`);
    await expect(page.locator('p-card')).toBeVisible({ timeout: NAV_TIMEOUT });
    // Wait for capability API to resolve
    await page.waitForTimeout(CAPABILITY_TIMEOUT);
    await expect(page.getByRole('button', { name: /^edit$/i })).not.toBeVisible();
  });

  test('organization detail: page renders without crash for reader', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (err) => errors.push(err.message));

    await page.goto(`/entities/organization/${testOrgId}/view`);
    await expect(page.locator('h1').filter({ hasText: /organization/i })).toBeVisible({
      timeout: NAV_TIMEOUT,
    });

    expect(errors).toHaveLength(0);
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// 3. EDITOR role — entity-level UI gating
// ═════════════════════════════════════════════════════════════════════════════

test.describe('3. EDITOR — entity-level UI gating', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'e2e-proof-editor');
  });

  test('organization list: "New Organization" button is visible', async ({ page }) => {
    await page.goto('/entities/organization');
    await expect(page.getByRole('button', { name: /new organization/i })).toBeVisible({
      timeout: CAPABILITY_TIMEOUT,
    });
  });

  test('organization list: Edit button appears in rows when data exists', async ({ page }) => {
    await page.goto('/entities/organization');
    await page.waitForSelector('p-table', { timeout: NAV_TIMEOUT });
    // Wait for capability + data to settle
    await expect(page.locator('button[aria-label="Edit"]').first()).toBeVisible({
      timeout: CAPABILITY_TIMEOUT,
    });
  });

  test('organization list: Delete button appears in rows when data exists', async ({ page }) => {
    await page.goto('/entities/organization');
    await page.waitForSelector('p-table', { timeout: NAV_TIMEOUT });
    await expect(page.locator('button[aria-label="Delete"]').first()).toBeVisible({
      timeout: CAPABILITY_TIMEOUT,
    });
  });

  test('organization detail: "Edit" button is visible', async ({ page }) => {
    await page.goto(`/entities/organization/${testOrgId}/view`);
    await expect(page.locator('p-card')).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(page.getByRole('button', { name: /^edit$/i })).toBeVisible({
      timeout: CAPABILITY_TIMEOUT,
    });
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// 4. NONE role — entity-level UI gating
// ═════════════════════════════════════════════════════════════════════════════

test.describe('4. NONE — entity-level UI gating', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'e2e-proof-none');
  });

  test('organization list: "New Organization" button is absent', async ({ page }) => {
    await page.goto('/entities/organization');
    await page.waitForTimeout(CAPABILITY_TIMEOUT);
    await expect(page.getByRole('button', { name: /new organization/i })).not.toBeVisible();
  });

  test('organization list: no Edit or Delete buttons', async ({ page }) => {
    await page.goto('/entities/organization');
    await page.waitForTimeout(CAPABILITY_TIMEOUT);
    await expect(page.locator('button[aria-label="Edit"]')).not.toBeVisible();
    await expect(page.locator('button[aria-label="Delete"]')).not.toBeVisible();
  });

  test('organization list: shows empty state (no rows for none user)', async ({ page }) => {
    await page.goto('/entities/organization');
    await page.waitForSelector('p-table', { timeout: NAV_TIMEOUT });
    await page.waitForTimeout(3000); // let data + capability settle

    // The table body should either be empty or show the empty-message row
    const dataRows = page.locator('p-table tbody tr:not(:has(td[colspan]))');
    const count = await dataRows.count();
    // None user must not see any data rows (row-level policy / empty response)
    expect(count).toBe(0);
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// 5. Route guards — direct-URL navigation
// ═════════════════════════════════════════════════════════════════════════════

test.describe('5. Route guards — direct-URL navigation', () => {
  test('READER: /entities/organization/new redirects to /accessdenied', async ({ page }) => {
    await loginAs(page, 'e2e-proof-reader');
    await page.goto('/entities/organization/new');
    await expect(page).toHaveURL(/accessdenied/, { timeout: CAPABILITY_TIMEOUT });
  });

  test('READER: /entities/organization/:id/edit redirects to /accessdenied', async ({ page }) => {
    await loginAs(page, 'e2e-proof-reader');
    await page.goto(`/entities/organization/${testOrgId}/edit`);
    await expect(page).toHaveURL(/accessdenied/, { timeout: CAPABILITY_TIMEOUT });
  });

  test('NONE: /entities/organization/new redirects to /accessdenied', async ({ page }) => {
    await loginAs(page, 'e2e-proof-none');
    await page.goto('/entities/organization/new');
    await expect(page).toHaveURL(/accessdenied/, { timeout: CAPABILITY_TIMEOUT });
  });

  test('NONE: /entities/organization/:id/edit redirects to /accessdenied', async ({ page }) => {
    await loginAs(page, 'e2e-proof-none');
    await page.goto(`/entities/organization/${testOrgId}/edit`);
    await expect(page).toHaveURL(/accessdenied/, { timeout: CAPABILITY_TIMEOUT });
  });

  test('EDITOR: /entities/organization/new loads the form (no redirect)', async ({ page }) => {
    await loginAs(page, 'e2e-proof-editor');
    await page.goto('/entities/organization/new');
    // Allow capability check to resolve
    await page.waitForTimeout(5000);
    await expect(page).not.toHaveURL(/accessdenied/);
    await expect(page.locator('input#code')).toBeVisible({ timeout: NAV_TIMEOUT });
  });

  test('EDITOR: /entities/organization/:id/edit loads the form (no redirect)', async ({ page }) => {
    await loginAs(page, 'e2e-proof-editor');
    await page.goto(`/entities/organization/${testOrgId}/edit`);
    await page.waitForTimeout(5000);
    await expect(page).not.toHaveURL(/accessdenied/);
    await expect(page.locator('input#code')).toBeVisible({ timeout: NAV_TIMEOUT });
  });

  test('unauthenticated user: any protected route redirects to /login', async ({ page }) => {
    // Do not log in — navigate directly
    await page.goto('/entities/organization');
    await expect(page).toHaveURL(/\/login/, { timeout: NAV_TIMEOUT });
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// 6. Access Denied page
// ═════════════════════════════════════════════════════════════════════════════

test.describe('6. Access Denied page', () => {
  test('renders with a meaningful message', async ({ page }) => {
    await loginAs(page, 'e2e-proof-reader');
    await page.goto('/entities/organization/new');
    await expect(page).toHaveURL(/accessdenied/, { timeout: CAPABILITY_TIMEOUT });

    // Page must show "access denied" or "forbidden" or similar heading
    const heading = page
      .locator('h1, h2, [class*="title"]')
      .filter({ hasText: /access|denied|forbidden|permission/i });
    await expect(heading.first()).toBeVisible({ timeout: NAV_TIMEOUT });
  });

  test('provides a navigation link back to a safe page', async ({ page }) => {
    await loginAs(page, 'e2e-proof-reader');
    await page.goto(`/entities/organization/${testOrgId}/edit`);
    await expect(page).toHaveURL(/accessdenied/, { timeout: CAPABILITY_TIMEOUT });

    // There must be some navigable element (link or button) to escape the error page
    const backLink = page.locator('a, button').filter({ hasText: /home|back|dashboard/i });
    await expect(backLink.first()).toBeVisible({ timeout: NAV_TIMEOUT });
  });

  test('direct navigation to /accessdenied renders without crash', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (err) => errors.push(err.message));

    await loginAsAdmin(page);
    await page.goto('/accessdenied');
    await page.waitForTimeout(2000);

    expect(errors).toHaveLength(0);
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// 7. Attribute-level permissions
// ═════════════════════════════════════════════════════════════════════════════

test.describe('6.1 Phase 7 enterprise shell', () => {
  test('keeps shared menu sections visible while hiding unauthorized leaves', async ({ page }) => {
    await loginAsAdminWithShellMocks(page, [
      'home.dashboard',
      'entities.department',
      'security.roles',
    ]);

    await expect(
      page.locator('.layout-menuitem-root-text').filter({ hasText: 'Entities' }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(
      page.locator('.layout-menuitem-root-text').filter({ hasText: 'Security Admin' }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });

    await expect(page.getByRole('link', { name: 'Department' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Security roles' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Organization' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Employees' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'User management' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Row policies' })).toHaveCount(0);
  });

  test('redirects denied deep links to /accessdenied with blocked destination recovery', async ({
    page,
  }) => {
    await loginAsAdminWithShellMocks(page, ['home.dashboard', 'security.roles']);

    await page.goto('/admin/users');

    await expect(page).toHaveURL(/\/accessdenied$/, { timeout: NAV_TIMEOUT });
    await expect(
      page.getByText('Full user management screens land in Phase 8.', { exact: false }),
    ).toHaveCount(0);
    await expect(page.locator('p-card')).toContainText('User management');
    await expect(page.getByRole('button', { name: 'Go to Security roles' })).toBeVisible();

    await page.getByRole('button', { name: 'Go to Security roles' }).click();
    await expect(page).toHaveURL(/\/admin\/security\/roles$/, { timeout: NAV_TIMEOUT });
  });

  test('renders an in-shell denied state without firing the entity list query', async ({
    page,
  }) => {
    const organizationRequests: string[] = [];
    page.on('request', (request) => {
      if (request.url().includes('/api/organizations')) {
        organizationRequests.push(request.url());
      }
    });

    await loginAsAdminWithShellMocks(
      page,
      ['home.dashboard', 'entities.organization'],
      [
        buildCapability('organization', {
          canCreate: false,
          canRead: false,
          canUpdate: false,
          canDelete: false,
        }),
      ],
    );

    await page.goto('/entities/organization');
    await expect(page).toHaveURL(/\/entities\/organization$/, { timeout: NAV_TIMEOUT });
    await expect(page.locator('p-card')).toContainText('Organization access is limited');
    await expect(page.locator('p-table')).toHaveCount(0);
    await page.waitForTimeout(1000);

    expect(organizationRequests).toHaveLength(0);
  });

  test('shows breadcrumb alignment on permission-matrix deep routes', async ({ page }) => {
    await loginAsAdminWithShellMocks(page, ['home.dashboard', 'security.roles']);

    await page.goto('/admin/security/roles/ROLE_ADMIN/permissions');

    await expect(page).toHaveURL(/\/admin\/security\/roles\/ROLE_ADMIN\/permissions$/, {
      timeout: NAV_TIMEOUT,
    });
    const breadcrumb = page.locator('nav[aria-label="Breadcrumb"]');
    await expect(breadcrumb).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(breadcrumb).toContainText('Security Admin');
    await expect(breadcrumb).toContainText('Security roles');
    await expect(breadcrumb).toContainText('Permission matrix');
  });
});

test.describe('7. Attribute-level permissions', () => {
  test('EDITOR create form: budget field is absent (EDIT DENY on ORGANIZATION.BUDGET)', async ({
    page,
  }) => {
    await loginAs(page, 'e2e-proof-editor');
    await page.goto('/entities/organization/new');
    // Wait for capability to resolve (isCapabilityReady signal)
    await expect(page.locator('input#code')).toBeVisible({ timeout: CAPABILITY_TIMEOUT });
    // Budget input must NOT be present
    await expect(page.locator('input#budget')).not.toBeVisible();
  });

  test('EDITOR edit form: budget field is absent (EDIT DENY on ORGANIZATION.BUDGET)', async ({
    page,
  }) => {
    await loginAs(page, 'e2e-proof-editor');
    await page.goto(`/entities/organization/${testOrgId}/edit`);
    await expect(page.locator('input#code')).toBeVisible({ timeout: CAPABILITY_TIMEOUT });
    await expect(page.locator('input#budget')).not.toBeVisible();
  });

  test('ADMIN edit form: budget field IS present (no attribute restriction)', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto(`/entities/organization/${testOrgId}/edit`);
    await expect(page.locator('input#code')).toBeVisible({ timeout: CAPABILITY_TIMEOUT });
    // Admin has no DENY on budget — field must appear
    await expect(page.locator('input#budget')).toBeVisible({ timeout: CAPABILITY_TIMEOUT });
  });

  test('READER detail view: code field hidden when READ permission is denied', async ({ page }) => {
    // This test verifies attribute-level READ enforcement on the detail view.
    // Currently requires BUG-C fix (AttributePermissionEvaluatorImpl deny-default
    // + detail component field guards) to pass.
    await loginAs(page, 'e2e-proof-reader');
    await page.goto(`/entities/organization/${testOrgId}/view`);
    await expect(page.locator('p-card')).toBeVisible({ timeout: NAV_TIMEOUT });
    await page.waitForTimeout(CAPABILITY_TIMEOUT);

    // The "Code" label and its value must not be rendered when READ=DENY
    // (After BUG-C fix: @if(canViewField('code')) wraps the block)
    const codeLabel = page.locator('.flex.flex-col').filter({ hasText: /^code$/i });
    await expect(codeLabel).not.toBeVisible();
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// 8. Capability cache — cross-user session correctness
// ═════════════════════════════════════════════════════════════════════════════

test.describe('8. Capability cache invalidation', () => {
  test("logging out and back in as a different role applies the new role's permissions", async ({
    page,
  }) => {
    // Step 1: login as admin — has Update permission
    await loginAsAdmin(page);
    await page.goto(`/entities/organization/${testOrgId}/view`);
    await expect(page.locator('p-card')).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(page.getByRole('button', { name: /^edit$/i })).toBeVisible({
      timeout: CAPABILITY_TIMEOUT,
    });

    // Step 2: logout
    const logoutBtn = page.locator('button[aria-label="Logout"], button:has(.pi-sign-out)').first();
    await logoutBtn.click();
    await expect(page).toHaveURL(/\/login/, { timeout: NAV_TIMEOUT });

    // Step 3: login as proof-reader — must NOT have Update permission
    await page.locator('#username').fill('e2e-proof-reader');
    await page.locator('input[type="password"]').fill(PASSWORD);
    await page.getByRole('button', { name: /sign in/i }).click();
    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: NAV_TIMEOUT });

    await page.goto(`/entities/organization/${testOrgId}/view`);
    await expect(page.locator('p-card')).toBeVisible({ timeout: NAV_TIMEOUT });
    // Capability must be fetched fresh — no stale admin cache. Edit must be absent.
    await page.waitForTimeout(CAPABILITY_TIMEOUT);
    await expect(page.getByRole('button', { name: /^edit$/i })).not.toBeVisible();
  });

  test('reloading the page as reader still shows no Edit button (not polluted by cache)', async ({
    page,
  }) => {
    await loginAs(page, 'e2e-proof-reader');
    await page.goto(`/entities/organization/${testOrgId}/view`);
    await expect(page.locator('p-card')).toBeVisible({ timeout: NAV_TIMEOUT });
    await page.reload();
    await expect(page.locator('p-card')).toBeVisible({ timeout: NAV_TIMEOUT });
    await page.waitForTimeout(CAPABILITY_TIMEOUT);
    await expect(page.getByRole('button', { name: /^edit$/i })).not.toBeVisible();
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// 9. Permission matrix — admin management UI
// ═════════════════════════════════════════════════════════════════════════════

test.describe('9. Permission matrix — admin management', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/admin/security/roles');
    await page.waitForSelector('p-table', { timeout: NAV_TIMEOUT });
  });

  test('roles list renders and shows at least one role', async ({ page }) => {
    const rows = page.locator('p-table tbody tr').filter({ hasNot: page.locator('td[colspan]') });
    await expect(rows.first()).toBeVisible({ timeout: NAV_TIMEOUT });
    const count = await rows.count();
    expect(count).toBeGreaterThan(0);
  });

  test('Manage Permissions opens the permission matrix without crash', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (err) => errors.push(err.message));

    await page
      .getByRole('button', { name: /manage permissions/i })
      .first()
      .click();
    await page.waitForSelector('tr[style*="cursor: pointer"]', { timeout: 20_000 });

    expect(errors).toHaveLength(0);
    // Entity table must render
    const entityRows = page.locator('tr[style*="cursor: pointer"]');
    await expect(entityRows.first()).toBeVisible();
  });

  test('clicking an entity row shows the attribute permissions panel', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (err) => errors.push(err.message));

    await page
      .getByRole('button', { name: /manage permissions/i })
      .first()
      .click();
    await page.waitForSelector('tr[style*="cursor: pointer"]', { timeout: 20_000 });

    await page.locator('tr[style*="cursor: pointer"]').first().click();

    const attrHeading = page.locator('h2').filter({ hasText: /attribute permissions/i });
    const noAttrMsg = page.locator('p').filter({ hasText: /no enumerated attributes/i });
    await expect(attrHeading.or(noAttrMsg)).toBeVisible({ timeout: 5000 });
    expect(errors).toHaveLength(0);
  });

  test('toggling an entity permission checkbox does not crash the app', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (err) => errors.push(err.message));

    await page
      .getByRole('button', { name: /manage permissions/i })
      .first()
      .click();
    await page.waitForSelector('tr[style*="cursor: pointer"]', { timeout: 20_000 });

    const urlBefore = page.url();
    await page.locator('tr[style*="cursor: pointer"] p-checkbox').first().click();
    await page.waitForTimeout(2000);

    expect(page.url()).toBe(urlBefore);
    expect(errors).toHaveLength(0);
  });

  test('toggling an attribute permission checkbox does not crash the app', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (err) => errors.push(err.message));

    await page
      .getByRole('button', { name: /manage permissions/i })
      .first()
      .click();
    await page.waitForSelector('tr[style*="cursor: pointer"]', { timeout: 20_000 });

    const entityRows = page.locator('tr[style*="cursor: pointer"]');
    const rowCount = await entityRows.count();

    for (let i = 0; i < rowCount; i++) {
      await entityRows.nth(i).click();
      const heading = page.locator('h2').filter({ hasText: /attribute permissions/i });
      if (await heading.isVisible({ timeout: 2000 }).catch(() => false)) {
        const attrCheckboxes = page.locator('p-splitter p-checkbox');
        if ((await attrCheckboxes.count()) > 0) {
          const urlBefore = page.url();
          await attrCheckboxes.first().click();
          await page.waitForTimeout(2000);
          expect(page.url()).toBe(urlBefore);
          expect(errors).toHaveLength(0);
          return;
        }
      }
    }
    test.skip(true, 'No entity with attribute checkboxes found');
  });

  test('permission changes persist after page reload', async ({ page }) => {
    await page
      .getByRole('button', { name: /manage permissions/i })
      .first()
      .click();
    await page.waitForSelector('tr[style*="cursor: pointer"]', { timeout: 20_000 });

    // Read current state of the first entity/first checkbox
    const firstCheckbox = page.locator('tr[style*="cursor: pointer"] p-checkbox').first();
    const wasCheckedBefore = await firstCheckbox
      .locator('.p-checkbox-checked')
      .isVisible()
      .catch(() => false);

    // Toggle it
    await firstCheckbox.click();
    await page.waitForTimeout(1500);

    // Reload the page and navigate back to same role's permissions
    await page.reload();
    await page.waitForSelector('p-table', { timeout: NAV_TIMEOUT });
    await page
      .getByRole('button', { name: /manage permissions/i })
      .first()
      .click();
    await page.waitForSelector('tr[style*="cursor: pointer"]', { timeout: 20_000 });

    // The toggled state must be persisted
    const isCheckedAfter = await page
      .locator('tr[style*="cursor: pointer"] p-checkbox')
      .first()
      .locator('.p-checkbox-checked')
      .isVisible()
      .catch(() => false);
    expect(isCheckedAfter).toBe(!wasCheckedBefore);

    // Restore the original state (cleanup)
    await page.locator('tr[style*="cursor: pointer"] p-checkbox').first().click();
    await page.waitForTimeout(1500);
  });
});

// ═════════════════════════════════════════════════════════════════════════════
// 10. CRUD operations under correct permissions
// ═════════════════════════════════════════════════════════════════════════════

test.describe('10. CRUD operations — EDITOR can create and edit', () => {
  const testName = `E2E-CRUD-${Date.now()}`;
  let createdId: number | null = null;

  test.afterAll(async ({ playwright }) => {
    if (createdId) {
      const api = await playwright.request.newContext({ baseURL: 'http://localhost:4200' });
      await deleteTestOrg(api, adminToken, createdId);
      await api.dispose();
    }
  });

  test('EDITOR can create a new organization', async ({ page }) => {
    await loginAs(page, 'e2e-proof-editor');
    await page.goto('/entities/organization/new');
    await expect(page.locator('input#code')).toBeVisible({ timeout: CAPABILITY_TIMEOUT });

    await page.locator('input#code').fill(`CODE-${Date.now()}`);
    await page.locator('input#name').fill(testName);
    await page.locator('input#ownerLogin').fill('e2e-proof-editor');

    await page.getByRole('button', { name: /save organization/i }).click();

    // Must redirect back to list after successful save
    await expect(page).toHaveURL(/\/entities\/organization$/, { timeout: NAV_TIMEOUT });
    // New org must appear in the list
    await expect(page.locator('p-table').getByText(testName)).toBeVisible({ timeout: NAV_TIMEOUT });
  });

  test('EDITOR can edit an existing organization', async ({ page }) => {
    await loginAs(page, 'e2e-proof-editor');
    await page.goto(`/entities/organization/${testOrgId}/edit`);
    await expect(page.locator('input#name')).toBeVisible({ timeout: CAPABILITY_TIMEOUT });

    const newName = `E2E-EDITED-${Date.now()}`;
    await page.locator('input#name').fill(newName);
    await page.getByRole('button', { name: /save organization/i }).click();

    // Must redirect to list after save
    await expect(page).toHaveURL(/\/entities\/organization$/, { timeout: NAV_TIMEOUT });
    await expect(page.locator('p-table').getByText(newName)).toBeVisible({ timeout: NAV_TIMEOUT });
  });

  test('READER cannot save via form (route guard blocks before form renders)', async ({ page }) => {
    await loginAs(page, 'e2e-proof-reader');
    await page.goto('/entities/organization/new');
    // Route guard must kick in before any form input is accessible
    await expect(page).toHaveURL(/accessdenied/, { timeout: CAPABILITY_TIMEOUT });
    await expect(page.locator('input#code')).not.toBeVisible();
  });
});
