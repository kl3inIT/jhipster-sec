import { test, expect, Page } from '@playwright/test';

const ADMIN_USER = 'admin';
const ADMIN_PASS = 'admin';

async function loginAsAdmin(page: Page): Promise<void> {
  await page.goto('/login');
  await page.locator('#username').fill(ADMIN_USER);
  await page.locator('#password').fill(ADMIN_PASS);
  await page.getByRole('button', { name: /sign in/i }).click();
  await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 10000 });
}

async function navigateToPermissionMatrix(page: Page): Promise<void> {
  // Navigate to roles list and open permission matrix for the first role
  await page.goto('/admin/security/roles');
  // Wait for the role list to load
  await page.waitForSelector('p-table', { timeout: 10000 });
  // Click the first Permissions button/link
  const permissionsLink = page.getByRole('link', { name: /permissions/i }).first();
  await permissionsLink.click();
  // Wait for the permission matrix to load (spinner gone, table visible)
  await page.waitForSelector('p-splitter', { timeout: 10000 });
  await expect(page.locator('p-progressspinner')).not.toBeVisible({ timeout: 10000 });
}

test.describe('Permission Matrix — UAT checks', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('entity table renders after matrix loads', async ({ page }) => {
    await navigateToPermissionMatrix(page);
    // Entity table must be visible
    await expect(page.locator('p-table').first()).toBeVisible();
    // At least one entity row must be present
    const rows = page.locator('p-table tr[style*="cursor: pointer"]');
    await expect(rows.first()).toBeVisible({ timeout: 5000 });
  });

  test('clicking entity row shows attribute permission panel', async ({ page }) => {
    await navigateToPermissionMatrix(page);
    // Click first entity row
    const entityRow = page.locator('p-table tr[style*="cursor: pointer"]').first();
    await entityRow.click();
    // The attribute panel must appear — either the table or a "no attributes" message
    const attributePanel = page.locator('p-splitter > ng-template').nth(1);
    const attrTable = page.locator('h2:has-text("Attribute Permissions")');
    const noAttrMsg = page.locator('text=no enumerated attributes');
    await expect(attrTable.or(noAttrMsg)).toBeVisible({ timeout: 5000 });
  });

  test('ticking an entity permission checkbox does not crash the app', async ({ page }) => {
    await navigateToPermissionMatrix(page);

    // Capture any uncaught errors
    const errors: string[] = [];
    page.on('pageerror', err => errors.push(err.message));

    // Find an unchecked CREATE checkbox and click it
    const createCheckboxes = page.locator('tr[style*="cursor: pointer"] td:nth-child(2) p-checkbox');
    const count = await createCheckboxes.count();
    expect(count).toBeGreaterThan(0);

    // Click the first checkbox (toggle it)
    await createCheckboxes.first().click();

    // Wait briefly for any async response
    await page.waitForTimeout(1000);

    // App must still be on the permission matrix page — no crash/redirect
    expect(page.url()).toContain('/permissions/');

    // No uncaught JS errors
    expect(errors).toHaveLength(0);
  });

  test('attribute permission checkboxes work after selecting an entity', async ({ page }) => {
    await navigateToPermissionMatrix(page);

    const errors: string[] = [];
    page.on('pageerror', err => errors.push(err.message));

    // Select first entity row that has attributes
    const entityRows = page.locator('tr[style*="cursor: pointer"]');
    let entityWithAttributes = false;
    const rowCount = await entityRows.count();

    for (let i = 0; i < rowCount; i++) {
      await entityRows.nth(i).click();
      await page.waitForTimeout(300);

      const attrTable = page.locator('h2:has-text("Attribute Permissions")');
      if (await attrTable.isVisible()) {
        entityWithAttributes = true;
        // Click first attribute checkbox
        const attrCheckboxes = page.locator('ng-template[ptemplate="body"] p-checkbox, p-table:last-of-type p-checkbox');
        if (await attrCheckboxes.count() > 0) {
          await attrCheckboxes.first().click();
          await page.waitForTimeout(1000);
          expect(page.url()).toContain('/permissions/');
          expect(errors).toHaveLength(0);
        }
        break;
      }
    }

    if (!entityWithAttributes) {
      test.skip(true, 'No entities with attributes found in catalog');
    }
  });
});
