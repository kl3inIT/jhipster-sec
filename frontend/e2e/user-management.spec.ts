import { Page, expect, test } from '@playwright/test';

test.setTimeout(120_000);

type MockUser = {
  id: number;
  login: string;
  firstName: string;
  lastName: string;
  email: string;
  activated: boolean;
  langKey: string;
  authorities: string[];
};

const ADMIN_USER: MockUser = {
  id: 1,
  login: 'admin',
  firstName: 'Admin',
  lastName: 'User',
  email: 'admin@example.com',
  activated: true,
  langKey: 'en',
  authorities: ['ROLE_ADMIN'],
};

const TARGET_USER: MockUser = {
  id: 2,
  login: 'e2e-user-mgmt-target',
  firstName: 'Target',
  lastName: 'User',
  email: 'target@example.com',
  activated: true,
  langKey: 'en',
  authorities: ['ROLE_USER'],
};

const ALL_AUTHORITIES = ['ROLE_ADMIN', 'ROLE_USER'];
const APP_URL = 'http://127.0.0.1:4200';

function buildUsers(targetAuthorities: string[]): MockUser[] {
  return [
    structuredClone(ADMIN_USER),
    {
      ...structuredClone(TARGET_USER),
      authorities: [...targetAuthorities],
    },
  ];
}

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    window.sessionStorage.setItem('locale', 'en');
  });
});

test.describe('user management', () => {
  test('user management grant flow changes downstream access after save', async ({ page }) => {
    let users: MockUser[] = buildUsers(['ROLE_USER']);
    let currentSessionLogin: string | null = null;

    await registerMockApi(page, {
      getCurrentSession: () => currentSessionLogin,
      setCurrentSession: login => {
        currentSessionLogin = login;
      },
      getUsers: () => users,
      setUsers: nextUsers => {
        users = nextUsers;
      },
    });

    await loginAs(page, 'admin');
    await openEditFlow(page, TARGET_USER.login);

    // Toggle the ROLE_ADMIN checkbox on the split-page authority table.
    await page.getByLabel(/ROLE_ADMIN/).check();
    await page.getByRole('button', { name: 'Save User' }).click();

    await expect(page).toHaveURL(new RegExp(`/admin/users/${TARGET_USER.login}/view$`), { timeout: 20_000 });
    expect(users.find(user => user.login === TARGET_USER.login)?.authorities).toContain('ROLE_ADMIN');

    currentSessionLogin = null;
    await page.locator('button[aria-label="Logout"], button:has(.pi-sign-out)').first().click();
    await expect(page).toHaveURL(/\/login$/, { timeout: 20_000 });

    await loginAs(page, TARGET_USER.login);
    await page.goto(`${APP_URL}/admin/users`);

    await expect(page).toHaveURL(/\/admin\/users$/, { timeout: 20_000 });
    await expect(page.getByRole('heading', { name: 'Users' })).toBeVisible({ timeout: 20_000 });
    await expect(page.getByRole('textbox', { name: 'Search by login, email, or name' })).toBeVisible();
  });

  test('user management revoke flow denies access after save', async ({ page }) => {
    let users: MockUser[] = buildUsers(['ROLE_ADMIN', 'ROLE_USER']);
    let currentSessionLogin: string | null = null;

    await registerMockApi(page, {
      getCurrentSession: () => currentSessionLogin,
      setCurrentSession: login => {
        currentSessionLogin = login;
      },
      getUsers: () => users,
      setUsers: nextUsers => {
        users = nextUsers;
      },
    });

    await loginAs(page, 'admin');
    await openEditFlow(page, TARGET_USER.login);

    await page.getByLabel(/ROLE_ADMIN/).uncheck();
    await page.getByRole('button', { name: 'Save User' }).click();

    await expect(page).toHaveURL(new RegExp(`/admin/users/${TARGET_USER.login}/view$`), { timeout: 20_000 });
    expect(users.find(user => user.login === TARGET_USER.login)?.authorities).not.toContain('ROLE_ADMIN');

    currentSessionLogin = null;
    await page.locator('button[aria-label="Logout"], button:has(.pi-sign-out)').first().click();
    await expect(page).toHaveURL(/\/login$/, { timeout: 20_000 });

    await loginAs(page, TARGET_USER.login);
    await page.goto(`${APP_URL}/admin/users`);

    await expect(page).toHaveURL(/\/accessdenied$/, { timeout: 20_000 });
    await expect(page.getByText('Access denied')).toBeVisible({ timeout: 20_000 });
  });
});

async function loginAs(page: Page, login: string): Promise<void> {
  await page.goto(`${APP_URL}/login`);
  await page.locator('#username').fill(login);
  await page.locator('input[type="password"]').fill(login === 'admin' ? 'admin' : 'Password1!');
  await page.getByRole('button', { name: /sign in/i }).click();
  await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 20_000 });
}

async function openEditFlow(page: Page, login: string): Promise<void> {
  await page.goto(`${APP_URL}/admin/users`);

  const searchInput = page.getByRole('textbox', { name: 'Search by login, email, or name' });
  await expect(searchInput).toBeVisible({ timeout: 20_000 });

  await searchInput.fill(login);
  const targetRow = page.locator('tbody tr').filter({ hasText: login }).first();
  await expect(targetRow).toBeVisible({ timeout: 20_000 });

  await page.goto(`${APP_URL}/admin/users/${login}/view`);
  await expect(page).toHaveURL(new RegExp(`/admin/users/${login}/view$`), { timeout: 20_000 });

  await page.getByRole('button', { name: 'Edit User' }).click();
  await expect(page).toHaveURL(new RegExp(`/admin/users/${login}/edit$`), { timeout: 20_000 });
}

async function registerMockApi(
  page: Page,
  state: {
    getCurrentSession: () => string | null;
    setCurrentSession: (login: string | null) => void;
    getUsers: () => MockUser[];
    setUsers: (users: MockUser[]) => void;
  },
): Promise<void> {
  await page.route('**/api/authenticate', async route => {
    const credentials = route.request().postDataJSON() as { username: string; password: string };
    if (
      (credentials.username === 'admin' && credentials.password === 'admin') ||
      (credentials.username === TARGET_USER.login && credentials.password === 'Password1!')
    ) {
      state.setCurrentSession(credentials.username);
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({ id_token: `mock-token-${credentials.username}` }),
      });
      return;
    }

    await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ message: 'Unauthorized' }) });
  });

  await page.route('**/api/account', async route => {
    const sessionLogin = state.getCurrentSession();
    if (!sessionLogin) {
      await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ message: 'Unauthorized' }) });
      return;
    }

    const sessionUser = state.getUsers().find(user => user.login === sessionLogin);
    if (!sessionUser) {
      await route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ message: 'Unauthorized' }) });
      return;
    }

    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        activated: true,
        authorities: sessionUser.authorities,
        email: sessionUser.email,
        firstName: sessionUser.firstName,
        imageUrl: null,
        langKey: sessionUser.langKey,
        lastName: sessionUser.lastName,
        login: sessionUser.login,
      }),
    });
  });

  await page.route('**/api/security/menu-permissions?*', async route => {
    const sessionLogin = state.getCurrentSession();
    const sessionUser = state.getUsers().find(user => user.login === sessionLogin);
    const authorities = sessionUser?.authorities ?? [];
    const allowedMenuIds = ['home.dashboard'];

    if (authorities.includes('ROLE_ADMIN')) {
      allowedMenuIds.push('security.users');
    }

    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        appName: 'jhipster-security-platform',
        allowedMenuIds,
      }),
    });
  });

  await page.route('**/api/security/entity-capabilities', async route => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  await page.route('**/api/authorities', async route => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ALL_AUTHORITIES.map(name => ({ name }))),
    });
  });

  await page.route('**/api/admin/users?*', async route => {
    const sessionUser = state.getUsers().find(user => user.login === state.getCurrentSession());
    if (!sessionUser?.authorities.includes('ROLE_ADMIN')) {
      await route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify({ message: 'Forbidden' }) });
      return;
    }

    const requestUrl = new URL(route.request().url());
    const query = requestUrl.searchParams.get('query')?.toLowerCase() ?? '';
    const matchingUsers = state
      .getUsers()
      .filter(user =>
        query
          ? [user.login, user.email, user.firstName, user.lastName].some(value => value.toLowerCase().includes(query))
          : true,
      )
      .map(user => ({
        ...user,
        createdBy: 'system',
        createdDate: '2026-03-25T00:00:00Z',
        lastModifiedBy: 'system',
        lastModifiedDate: '2026-03-25T00:00:00Z',
      }));

    await route.fulfill({
      contentType: 'application/json',
      headers: { 'X-Total-Count': `${matchingUsers.length}` },
      body: JSON.stringify(matchingUsers),
    });
  });

  await page.route('**/api/admin/users', async route => {
    const sessionUser = state.getUsers().find(user => user.login === state.getCurrentSession());
    if (!sessionUser?.authorities.includes('ROLE_ADMIN')) {
      await route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify({ message: 'Forbidden' }) });
      return;
    }

    if (route.request().method() === 'PUT') {
      const payload = route.request().postDataJSON() as MockUser;
      const nextUsers = state.getUsers().map(user =>
        user.login === payload.login || user.id === payload.id
          ? {
              ...user,
              ...payload,
              authorities: [...payload.authorities],
            }
          : user,
      );
      state.setUsers(nextUsers);

      const updatedUser = nextUsers.find(user => user.id === payload.id || user.login === payload.login);
      await route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(updatedUser),
      });
      return;
    }

    await route.fallback();
  });

  await page.route('**/api/admin/users/*', async route => {
    const sessionUser = state.getUsers().find(user => user.login === state.getCurrentSession());
    if (!sessionUser?.authorities.includes('ROLE_ADMIN')) {
      await route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify({ message: 'Forbidden' }) });
      return;
    }

    const login = route.request().url().split('/').pop();
    const user = state.getUsers().find(candidate => candidate.login === login);
    if (!user) {
      await route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ message: 'Not Found' }) });
      return;
    }

    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        ...user,
        createdBy: 'system',
        createdDate: '2026-03-25T00:00:00Z',
        lastModifiedBy: 'system',
        lastModifiedDate: '2026-03-25T00:00:00Z',
      }),
    });
  });
}
