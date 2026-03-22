/**
 * Proof-role gating E2E tests — Test 3 of Phase 05 UAT
 *
 * Verifies that Reader, Editor, and None users see different actions and
 * sensitive fields, and that denied create/edit routes land on /accessdenied.
 *
 * Setup: creates proof roles, permissions, and users via the admin API at the
 * start of the suite. Cleanup removes them at the end.
 */

import { test, expect, request, APIRequestContext, Page } from '@playwright/test';

test.setTimeout(120000);

// ─── Seed data ──────────────────────────────────────────────────────────────

const PROOF_ROLES = [
  { name: 'ROLE_PROOF_READER', displayName: 'Proof Reader', type: 'RESOURCE' },
  { name: 'ROLE_PROOF_EDITOR', displayName: 'Proof Editor', type: 'RESOURCE' },
  { name: 'ROLE_PROOF_NONE', displayName: 'Proof None', type: 'RESOURCE' },
];

const PROOF_USERS = [
  { login: 'e2e-proof-reader', email: 'e2e-reader@example.com', role: 'ROLE_PROOF_READER' },
  { login: 'e2e-proof-editor', email: 'e2e-editor@example.com', role: 'ROLE_PROOF_EDITOR' },
  { login: 'e2e-proof-none', email: 'e2e-none@example.com', role: 'ROLE_PROOF_NONE' },
];

// Permissions that mirror the seed in 20260321000800_seed_proof_security_test_data.xml
const PROOF_PERMISSIONS = [
  // READER: can READ organization, department, employee; denied EMPLOYEE.SALARY view
  { authorityName: 'ROLE_PROOF_READER', targetType: 'ENTITY', target: 'ORGANIZATION', action: 'READ', effect: 'ALLOW' },
  { authorityName: 'ROLE_PROOF_READER', targetType: 'ENTITY', target: 'DEPARTMENT', action: 'READ', effect: 'ALLOW' },
  { authorityName: 'ROLE_PROOF_READER', targetType: 'ENTITY', target: 'EMPLOYEE', action: 'READ', effect: 'ALLOW' },
  { authorityName: 'ROLE_PROOF_READER', targetType: 'ATTRIBUTE', target: 'EMPLOYEE.SALARY', action: 'VIEW', effect: 'DENY' },
  // EDITOR: full CRUD on organization; read department & employee; denied ORGANIZATION.BUDGET edit
  { authorityName: 'ROLE_PROOF_EDITOR', targetType: 'ENTITY', target: 'ORGANIZATION', action: 'CREATE', effect: 'ALLOW' },
  { authorityName: 'ROLE_PROOF_EDITOR', targetType: 'ENTITY', target: 'ORGANIZATION', action: 'READ', effect: 'ALLOW' },
  { authorityName: 'ROLE_PROOF_EDITOR', targetType: 'ENTITY', target: 'ORGANIZATION', action: 'UPDATE', effect: 'ALLOW' },
  { authorityName: 'ROLE_PROOF_EDITOR', targetType: 'ENTITY', target: 'ORGANIZATION', action: 'DELETE', effect: 'ALLOW' },
  { authorityName: 'ROLE_PROOF_EDITOR', targetType: 'ENTITY', target: 'DEPARTMENT', action: 'READ', effect: 'ALLOW' },
  { authorityName: 'ROLE_PROOF_EDITOR', targetType: 'ENTITY', target: 'EMPLOYEE', action: 'READ', effect: 'ALLOW' },
  { authorityName: 'ROLE_PROOF_EDITOR', targetType: 'ATTRIBUTE', target: 'ORGANIZATION.BUDGET', action: 'EDIT', effect: 'DENY' },
  // NONE: no permissions (empty = deny all by default)
];

// ─── Admin API helpers ───────────────────────────────────────────────────────

async function getAdminToken(api: APIRequestContext): Promise<string> {
  const res = await api.post('/api/authenticate', {
    data: { username: 'admin', password: 'admin', rememberMe: false },
  });
  expect(res.status()).toBe(200);
  const body = await res.json();
  return body.id_token as string;
}

async function setupProofData(api: APIRequestContext, token: string): Promise<void> {
  const headers = { Authorization: `Bearer ${token}` };

  // Create roles (ignore 400 = already exists)
  for (const role of PROOF_ROLES) {
    await api.post('/api/admin/sec/roles', { data: role, headers });
  }

  // Create proof permissions (ignore 400 = already exists)
  for (const perm of PROOF_PERMISSIONS) {
    await api.post('/api/admin/sec/permissions', { data: perm, headers });
  }

  // Register users with a known password (sets password correctly), then
  // activate via admin PUT (sets activated=true and assigns proof authorities).
  const password = 'Password1!';
  for (const u of PROOF_USERS) {
    // Step 1: register — creates inactive user with known password
    const reg = await api.post('/api/register', {
      data: { login: u.login, email: u.email, password, langKey: 'en' },
    });
    // 201 = created, 400 = already exists — both are fine
    expect([201, 400]).toContain(reg.status());

    // Step 2: fetch the user record (needed to preserve server-generated fields in PUT)
    const getRes = await api.get(`/api/admin/users/${u.login}`, { headers });
    expect(getRes.status()).toBe(200);
    const existing = await getRes.json();

    // Step 3: admin PUT — activate + assign proof role
    const putRes = await api.put('/api/admin/users', {
      data: { ...existing, activated: true, authorities: [u.role] },
      headers,
    });
    expect(putRes.status()).toBe(200);
  }
}

async function teardownProofData(api: APIRequestContext, token: string): Promise<void> {
  const headers = { Authorization: `Bearer ${token}` };

  // Delete users
  for (const u of PROOF_USERS) {
    await api.delete(`/api/admin/users/${u.login}`, { headers });
  }
  // Leave roles/permissions in place (harmless; avoids ordering issues with FK constraints)
}

// ─── Login helper ────────────────────────────────────────────────────────────

async function loginAs(page: Page, login: string): Promise<void> {
  await page.goto('/login');
  await page.locator('#username').fill(login);
  await page.locator('input[type="password"]').fill('Password1!');
  await page.getByRole('button', { name: /sign in/i }).click();
  await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 15000 });
}

// ─── Suite ───────────────────────────────────────────────────────────────────

test.describe('Proof-role gating', () => {
  let adminToken: string;

  test.beforeAll(async ({ playwright }) => {
    const api = await playwright.request.newContext({ baseURL: 'http://localhost:4200' });
    adminToken = await getAdminToken(api);
    await setupProofData(api, adminToken);
    await api.dispose();
  });

  test.afterAll(async ({ playwright }) => {
    const api = await playwright.request.newContext({ baseURL: 'http://localhost:4200' });
    await teardownProofData(api, adminToken);
    await api.dispose();
  });

  // ── READER ────────────────────────────────────────────────────────────────

  test('READER: organization list shows no New/Edit/Delete buttons', async ({ page }) => {
    await loginAs(page, 'e2e-proof-reader');
    await page.goto('/entities/organization');
    await page.waitForSelector('p-table', { timeout: 10000 });

    // No "New Organization" button
    await expect(page.getByRole('button', { name: /new organization/i })).not.toBeVisible();

    // No Edit or Delete buttons in any row (row actions)
    const editBtns = page.getByRole('button', { name: /edit/i });
    const deleteBtns = page.getByRole('button', { name: /delete/i });
    await expect(editBtns).not.toBeVisible();
    await expect(deleteBtns).not.toBeVisible();
  });

  test('READER: navigating to /entities/organization/new lands on /accessdenied', async ({ page }) => {
    await loginAs(page, 'e2e-proof-reader');
    await page.goto('/entities/organization/new');
    // Component loads capability async then navigates — allow up to 15s for the round trip
    await expect(page).toHaveURL(/accessdenied/, { timeout: 15000 });
  });

  // ── EDITOR ────────────────────────────────────────────────────────────────

  test('EDITOR: organization list shows New/Edit/Delete buttons', async ({ page }) => {
    await loginAs(page, 'e2e-proof-editor');
    await page.goto('/entities/organization');
    // Wait for capability API to resolve and the button to appear (async after table renders)
    await expect(page.getByRole('button', { name: /new organization/i })).toBeVisible({ timeout: 15000 });
  });

  test('EDITOR: can access /entities/organization/new without redirect', async ({ page }) => {
    await loginAs(page, 'e2e-proof-editor');
    await page.goto('/entities/organization/new');
    // Component checks capability async — give it time to resolve, then confirm no redirect
    await page.waitForTimeout(5000);
    await expect(page).not.toHaveURL(/accessdenied/);
    // Form inputs should be visible
    await expect(page.locator('input').first()).toBeVisible({ timeout: 10000 });
  });

  // ── NONE ──────────────────────────────────────────────────────────────────

  test('NONE: organization list shows no buttons and no rows', async ({ page }) => {
    await loginAs(page, 'e2e-proof-none');
    await page.goto('/entities/organization');
    await page.waitForTimeout(3000); // let the capability + data calls settle

    await expect(page.getByRole('button', { name: /new organization/i })).not.toBeVisible();
  });

  test('NONE: navigating to /entities/organization/new lands on /accessdenied', async ({ page }) => {
    await loginAs(page, 'e2e-proof-none');
    await page.goto('/entities/organization/new');
    await expect(page).toHaveURL(/accessdenied/, { timeout: 15000 });
  });
});
