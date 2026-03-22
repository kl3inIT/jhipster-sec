import { test, expect, Page } from '@playwright/test';

const ADMIN_USER = 'admin';
const ADMIN_PASS = 'admin';

test.setTimeout(60000);

async function loginAsAdmin(page: Page): Promise<void> {
  await page.goto('/login');
  await page.locator('#username').fill(ADMIN_USER);
  await page.locator('input[type="password"]').fill(ADMIN_PASS);
  await page.getByRole('button', { name: /sign in/i }).click();
  await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 15000 });
}

async function navigateToPermissionMatrix(page: Page): Promise<void> {
  await page.goto('/admin/security/roles');
  await page.waitForSelector('p-table', { timeout: 15000 });
  // Click the first "Manage Permissions" button
  await page.getByRole('button', { name: /manage permissions/i }).first().click();
  // Wait for entity rows to be clickable — confirms catalog loaded and @if(loading) is false
  await page.waitForSelector('tr[style*="cursor: pointer"]', { timeout: 20000 });
}

test.describe('Permission Matrix — UAT checks', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await navigateToPermissionMatrix(page);
  });

  test('entity table renders after matrix loads', async ({ page }) => {
    // Entity table must be visible
    await expect(page.locator('p-table').first()).toBeVisible();
    // At least one entity row must be present
    const rows = page.locator('tr[style*="cursor: pointer"]');
    await expect(rows.first()).toBeVisible();
    const count = await rows.count();
    expect(count).toBeGreaterThan(0);
  });

  test('clicking entity row shows attribute permission panel', async ({ page }) => {
    // Capture any uncaught JS errors
    const errors: string[] = [];
    page.on('pageerror', err => errors.push(err.message));

    // Click first entity row
    const entityRow = page.locator('tr[style*="cursor: pointer"]').first();
    await entityRow.click();

    // The attribute panel must appear within 5s — either a table or "no attributes" message
    const attrHeading = page.locator('h2').filter({ hasText: 'Attribute Permissions' });
    const noAttrMsg = page.locator('p').filter({ hasText: 'no enumerated attributes' });
    await expect(attrHeading.or(noAttrMsg)).toBeVisible({ timeout: 5000 });

    // No JS crashes
    expect(errors).toHaveLength(0);
  });

  test('ticking an entity permission checkbox does not crash the app', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', err => errors.push(err.message));

    // Find the entity checkboxes in the entity table
    const entityCheckboxes = page.locator('tr[style*="cursor: pointer"] p-checkbox').first();
    await expect(entityCheckboxes).toBeVisible();

    // Remember the URL before clicking
    const urlBefore = page.url();

    // Click the first checkbox (toggle CREATE for the first entity)
    await entityCheckboxes.click();

    // Wait briefly for any async response
    await page.waitForTimeout(2000);

    // App must still be on the permission matrix page — no crash/redirect
    expect(page.url()).toBe(urlBefore);

    // No uncaught JS errors
    expect(errors).toHaveLength(0);
  });

  test('attribute permission checkboxes work after selecting an entity', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', err => errors.push(err.message));

    // Try each entity row until we find one with attributes
    const entityRows = page.locator('tr[style*="cursor: pointer"]');
    const rowCount = await entityRows.count();
    let foundAttributes = false;

    for (let i = 0; i < rowCount; i++) {
      await entityRows.nth(i).click();

      const attrHeading = page.locator('h2').filter({ hasText: 'Attribute Permissions' });
      if (await attrHeading.isVisible({ timeout: 2000 }).catch(() => false)) {
        foundAttributes = true;

        // Click first attribute checkbox
        const attrCheckboxes = page.locator('p-splitter p-checkbox');
        const attrCount = await attrCheckboxes.count();
        if (attrCount > 0) {
          const urlBefore = page.url();
          await attrCheckboxes.first().click();
          await page.waitForTimeout(2000);
          expect(page.url()).toBe(urlBefore);
          expect(errors).toHaveLength(0);
        }
        break;
      }
    }

    if (!foundAttributes) {
      test.skip(true, 'No entities with attributes found in catalog');
    }
  });
});
