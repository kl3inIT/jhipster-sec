#!/usr/bin/env node

import process from 'node:process';

const PHASE12_BASE_URL = process.env.PHASE12_BASE_URL ?? 'http://127.0.0.1:8080';
const ADMIN_USERNAME = process.env.PHASE12_ADMIN_USERNAME ?? 'admin';
const ADMIN_PASSWORD = process.env.PHASE12_ADMIN_PASSWORD ?? 'admin';
const PROOF_READER_LOGIN = process.env.PHASE12_PROOF_READER_LOGIN ?? 'user';
const PROOF_READER_PASSWORD = process.env.PHASE12_PROOF_READER_PASSWORD ?? 'user';
const PROOF_READER_EMAIL = process.env.PHASE12_PROOF_READER_EMAIL ?? 'user@localhost';
const REQUEST_TIMEOUT_MS = Number(process.env.PHASE12_REQUEST_TIMEOUT_MS ?? '30000');

const scriptReferences = [
  'AccountResourceIT',
  'UserResourceIT',
  'SecuredEntityCapabilityResourceIT',
  'SecuredEntityEnforcementIT',
];

function log(message) {
  console.log(`[phase12-prodlike-regression] ${message}`);
}

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    if (details !== undefined) {
      error.details = details;
    }
    throw error;
  }
}

function withTimeout(signal) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(new Error(`Request exceeded ${REQUEST_TIMEOUT_MS}ms`)), REQUEST_TIMEOUT_MS);

  if (signal) {
    if (signal.aborted) {
      controller.abort(signal.reason);
    } else {
      signal.addEventListener('abort', () => controller.abort(signal.reason), { once: true });
    }
  }

  return {
    signal: controller.signal,
    clear: () => clearTimeout(timeout),
  };
}

async function fetchText(url, options = {}) {
  const { signal, clear } = withTimeout(options.signal);
  try {
    const response = await fetch(url, {
      ...options,
      signal,
      headers: {
        Accept: 'application/json',
        ...(options.headers ?? {}),
      },
    });
    const text = await response.text();
    return { response, text };
  } finally {
    clear();
  }
}

function parseJson(text) {
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

async function requestJson(url, options = {}) {
  const { response, text } = await fetchText(url, options);
  return { response, text, body: parseJson(text) };
}

async function expectJson(url, options = {}, expectedStatus) {
  const result = await requestJson(url, options);
  if (expectedStatus !== undefined) {
    assert(result.response.status === expectedStatus, `Expected ${expectedStatus} from ${url} but received ${result.response.status}`, result.body ?? result.text);
  } else {
    assert(result.response.ok, `Expected successful response from ${url} but received ${result.response.status}`, result.body ?? result.text);
  }
  assert(result.body !== null, `Expected JSON body from ${url}`, result.text);
  return result.body;
}

async function authenticate(username, password) {
  const body = await expectJson(
    `${PHASE12_BASE_URL}/api/authenticate`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password, rememberMe: false }),
    },
    200,
  );

  assert(typeof body.id_token === 'string' && body.id_token.length > 20, `Authentication for ${username} did not return id_token`, body);
  return body.id_token;
}

function authHeaders(token, extra = {}) {
  return {
    Authorization: `Bearer ${token}`,
    ...extra,
  };
}

async function ensureProofReaderAuthority(adminToken) {
  const authorities = await expectJson(`${PHASE12_BASE_URL}/api/authorities`, {
    headers: authHeaders(adminToken),
  });

  const exists = authorities.some(authority => authority?.name === 'ROLE_PROOF_READER');
  if (exists) {
    log('ROLE_PROOF_READER already exists');
    return;
  }

  await expectJson(
    `${PHASE12_BASE_URL}/api/authorities`,
    {
      method: 'POST',
      headers: authHeaders(adminToken, { 'Content-Type': 'application/json' }),
      body: JSON.stringify({
        name: 'ROLE_PROOF_READER',
        displayName: 'Proof Reader',
        type: 'RESOURCE',
      }),
    },
    201,
  );

  log('Created ROLE_PROOF_READER authority for runtime regression setup');
}

async function ensureProofReaderPermissions(adminToken) {
  const permissions = [
    { targetType: 'ENTITY', target: 'organization', action: 'READ', effect: 'GRANT' },
    { targetType: 'ENTITY', target: 'department', action: 'READ', effect: 'GRANT' },
    { targetType: 'ENTITY', target: 'employee', action: 'READ', effect: 'GRANT' },
    { targetType: 'ATTRIBUTE', target: 'organization.budget', action: 'VIEW', effect: 'DENY' },
    { targetType: 'ATTRIBUTE', target: 'employee.salary', action: 'VIEW', effect: 'DENY' },
  ];

  for (const permission of permissions) {
    const response = await requestJson(`${PHASE12_BASE_URL}/api/admin/sec/permissions`, {
      method: 'POST',
      headers: authHeaders(adminToken, { 'Content-Type': 'application/json' }),
      body: JSON.stringify({
        authorityName: 'ROLE_PROOF_READER',
        ...permission,
      }),
    });

    assert(
      response.response.status === 200 || response.response.status === 201,
      `Failed to upsert permission ${permission.targetType}:${permission.target}:${permission.action}`,
      response.body ?? response.text,
    );
  }

  log('Ensured ROLE_PROOF_READER permissions match SecuredEntityCapabilityResourceIT and SecuredEntityEnforcementIT expectations');
}

async function ensureProofReaderUser(adminToken) {
  const existing = await requestJson(`${PHASE12_BASE_URL}/api/admin/users/${encodeURIComponent(PROOF_READER_LOGIN)}`, {
    headers: authHeaders(adminToken),
  });

  assert(existing.response.ok, `Failed to fetch existing admin user ${PROOF_READER_LOGIN}`, existing.body ?? existing.text);
  const currentUser = existing.body;

  await expectJson(
    `${PHASE12_BASE_URL}/api/admin/users/${encodeURIComponent(PROOF_READER_LOGIN)}`,
    {
      method: 'PUT',
      headers: authHeaders(adminToken, { 'Content-Type': 'application/json' }),
      body: JSON.stringify({
        id: currentUser.id,
        login: currentUser.login,
        firstName: currentUser.firstName,
        lastName: currentUser.lastName,
        email: currentUser.email ?? PROOF_READER_EMAIL,
        activated: true,
        langKey: currentUser.langKey,
        imageUrl: currentUser.imageUrl ?? '',
        createdBy: currentUser.createdBy,
        createdDate: currentUser.createdDate,
        lastModifiedBy: currentUser.lastModifiedBy,
        lastModifiedDate: currentUser.lastModifiedDate,
        authorities: ['ROLE_PROOF_READER'],
      }),
    },
    200,
  );

  log(`Updated ${PROOF_READER_LOGIN} authorities via admin-user runtime flow derived from UserResourceIT`);
}

async function verifyAdminUserBrowse(adminToken) {
  const users = await expectJson(`${PHASE12_BASE_URL}/api/admin/users?sort=id,desc&query=${encodeURIComponent(PROOF_READER_LOGIN)}`, {
    headers: authHeaders(adminToken),
  });

  assert(Array.isArray(users), 'Admin user browse did not return an array', users);
  assert(users.some(user => user?.login === PROOF_READER_LOGIN), `Admin user browse missing ${PROOF_READER_LOGIN}`, users);
  log('Verified admin-user browse on /api/admin/users');
}

async function verifyAccountFlow(token) {
  const account = await expectJson(`${PHASE12_BASE_URL}/api/account`, {
    headers: authHeaders(token),
  });

  assert(account.login === PROOF_READER_LOGIN, 'Account lookup returned unexpected login', account);
  assert(Array.isArray(account.authorities), 'Account lookup did not expose authorities array', account);
  assert(account.authorities.includes('ROLE_PROOF_READER'), 'Account lookup missing ROLE_PROOF_READER', account);
  log('Verified auth/account runtime flow on /api/authenticate and /api/account');
}

async function verifyCapabilities(token) {
  const capabilities = await expectJson(`${PHASE12_BASE_URL}/api/security/entity-capabilities`, {
    headers: authHeaders(token),
  });

  assert(Array.isArray(capabilities), 'Capability endpoint did not return an array', capabilities);
  const organization = capabilities.find(capability => capability?.code === 'organization');
  assert(organization, 'Capability payload missing organization entry', capabilities);
  assert(organization.canRead === true, 'Organization canRead should be true for ROLE_PROOF_READER', organization);
  assert(organization.canCreate === false, 'Organization canCreate should be false for ROLE_PROOF_READER', organization);
  assert(organization.canUpdate === false, 'Organization canUpdate should be false for ROLE_PROOF_READER', organization);
  const budgetAttribute = Array.isArray(organization.attributes)
    ? organization.attributes.find(attribute => attribute?.name === 'budget')
    : undefined;
  assert(budgetAttribute?.canView === false, 'Organization budget should stay denied for ROLE_PROOF_READER', organization);
  log('Verified secured-entity capability runtime flow on /api/security/entity-capabilities');
}

async function verifyOrganizationAccess(token) {
  const organizations = await expectJson(`${PHASE12_BASE_URL}/api/organizations?sort=id,asc`, {
    headers: authHeaders(token),
  });

  assert(Array.isArray(organizations), 'Organization browse did not return an array', organizations);
  for (const organization of organizations) {
    assert(!Object.hasOwn(organization, 'budget'), 'Denied organization budget leaked in secured list response', organization);
  }

  const createAttempt = await requestJson(`${PHASE12_BASE_URL}/api/organizations`, {
    method: 'POST',
    headers: authHeaders(token, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({
      code: `ORG-PHASE12-${Date.now()}`,
      name: 'Phase 12 Forbidden Create',
      ownerLogin: PROOF_READER_LOGIN,
    }),
  });

  assert(createAttempt.response.status === 403, 'Proof reader organization create should stay forbidden', createAttempt.body ?? createAttempt.text);
  log('Verified secured organization allow/deny behavior on /api/organizations list and create');
}


async function main() {
  log(`Starting live-stack PROD-02 regression against ${PHASE12_BASE_URL}`);
  log(`Mirroring brownfield-safe targets: ${scriptReferences.join(', ')}`);

  const adminToken = await authenticate(ADMIN_USERNAME, ADMIN_PASSWORD);
  await ensureProofReaderAuthority(adminToken);
  await ensureProofReaderPermissions(adminToken);
  await ensureProofReaderUser(adminToken);
  await verifyAdminUserBrowse(adminToken);

  const proofReaderToken = await authenticate(PROOF_READER_LOGIN, PROOF_READER_PASSWORD);
  await verifyAccountFlow(proofReaderToken);
  await verifyCapabilities(proofReaderToken);
  await verifyOrganizationAccess(proofReaderToken);

  log('Live-stack regression checks passed for auth/account, admin-user, and secured-entity flows');
}

main().catch(error => {
  console.error(`[phase12-prodlike-regression] ERROR: ${error.message}`);
  if (error.details !== undefined) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
});
