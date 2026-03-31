import http from 'k6/http';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function authenticate(username, password) {
  const res = http.post(
    `${BASE_URL}/api/authenticate`,
    JSON.stringify({ username, password, rememberMe: true }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (res.status !== 200) {
    throw new Error(`Authentication failed: ${res.status} ${res.body}`);
  }

  return res.json('id_token');
}

export function authHeaders(token) {
  return {
    Authorization: `Bearer ${token}`,
    Accept: 'application/json',
  };
}
