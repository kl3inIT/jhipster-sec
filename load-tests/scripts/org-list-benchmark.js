import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

import { authenticate, authHeaders } from './auth.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    secured_1vu: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      startTime: '0s',
      exec: 'securedList',
      tags: { endpoint: 'secured' },
    },
    baseline_1vu: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      startTime: '0s',
      exec: 'baselineList',
      tags: { endpoint: 'baseline' },
    },
    secured_10vu: {
      executor: 'constant-vus',
      vus: 10,
      duration: '30s',
      startTime: '35s',
      exec: 'securedList',
      tags: { endpoint: 'secured' },
    },
    baseline_10vu: {
      executor: 'constant-vus',
      vus: 10,
      duration: '30s',
      startTime: '35s',
      exec: 'baselineList',
      tags: { endpoint: 'baseline' },
    },
    secured_50vu: {
      executor: 'constant-vus',
      vus: 50,
      duration: '30s',
      startTime: '70s',
      exec: 'securedList',
      tags: { endpoint: 'secured' },
    },
    baseline_50vu: {
      executor: 'constant-vus',
      vus: 50,
      duration: '30s',
      startTime: '70s',
      exec: 'baselineList',
      tags: { endpoint: 'baseline' },
    },
  },
  thresholds: {
    'http_req_duration{endpoint:secured}': ['p(95)<500'],
    'http_req_duration{endpoint:baseline}': ['p(95)<500'],
  },
};

export function setup() {
  const token = authenticate('admin', 'admin');
  return { token };
}

export function securedList(data) {
  const res = http.get(`${BASE_URL}/api/organizations?page=0&size=20`, {
    headers: authHeaders(data.token),
    tags: { endpoint: 'secured' },
  });

  check(res, {
    'secured status 200': r => r.status === 200,
  });

  sleep(1);
}

export function baselineList(data) {
  const res = http.get(`${BASE_URL}/api/benchmark/organizations?page=0&size=20`, {
    headers: authHeaders(data.token),
    tags: { endpoint: 'baseline' },
  });

  check(res, {
    'baseline status 200': r => r.status === 200,
  });

  sleep(1);
}

export function handleSummary(data) {
  const secP50 = getMetricValue(data, 'secured', 'p(50)');
  const secP95 = getMetricValue(data, 'secured', 'p(95)');
  const secP99 = getMetricValue(data, 'secured', 'p(99)');

  const baseP50 = getMetricValue(data, 'baseline', 'p(50)');
  const baseP95 = getMetricValue(data, 'baseline', 'p(95)');
  const baseP99 = getMetricValue(data, 'baseline', 'p(99)');

  const overheadP95 = calculateOverhead(secP95, baseP95);
  const targetMet = overheadP95 === null ? 'UNKNOWN' : overheadP95 <= 10 ? 'PASS' : 'FAIL';
  const overheadText = overheadP95 === null ? 'n/a' : overheadP95.toFixed(1);

  const md = [
    '# Benchmark: Organization List',
    '',
    `**Date:** ${new Date().toISOString().slice(0, 10)}`,
    '**Target:** p95 overhead < 10% (D-08)',
    `**Result:** ${targetMet} (${overheadText}% overhead)`,
    '',
    '| Metric | Secured | Baseline | Overhead |',
    '|--------|---------|----------|----------|',
    `| p50 (ms) | ${fmt(secP50)} | ${fmt(baseP50)} | ${pct(secP50, baseP50)} |`,
    `| p95 (ms) | ${fmt(secP95)} | ${fmt(baseP95)} | ${pct(secP95, baseP95)} |`,
    `| p99 (ms) | ${fmt(secP99)} | ${fmt(baseP99)} | ${pct(secP99, baseP99)} |`,
    '',
    overheadP95 !== null && overheadP95 > 10
      ? '> **WARNING:** p95 overhead exceeds 10% target.'
      : '> p95 overhead within 10% target.',
    '',
  ].join('\n');

  return {
    'load-tests/results/org-list-summary.md': md,
    'load-tests/results/org-list-raw.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function getMetricValue(data, endpoint, percentile) {
  const key = `http_req_duration{endpoint:${endpoint}}`;
  return data.metrics[key]?.values?.[percentile] ?? 'n/a';
}

function calculateOverhead(secured, baseline) {
  if (typeof secured !== 'number' || typeof baseline !== 'number' || baseline === 0) {
    return null;
  }

  return ((secured - baseline) / baseline) * 100;
}

function fmt(value) {
  return typeof value === 'number' ? value.toFixed(1) : String(value);
}

function pct(current, base) {
  if (typeof current !== 'number' || typeof base !== 'number' || base === 0) {
    return 'n/a';
  }

  return `${(((current - base) / base) * 100).toFixed(1)}%`;
}
