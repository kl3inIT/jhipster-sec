#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/src/main/docker/app.yml"
APP_IMAGE="jhipster-sec:latest"
PHASE12_BASE_URL="${PHASE12_BASE_URL:-http://127.0.0.1:8080}"
MAILPIT_API_URL="${PHASE12_MAILPIT_API_URL:-http://127.0.0.1:8025}"

log() {
  printf '[phase12-stack-smoke] %s\n' "$1"
}

fail() {
  printf '[phase12-stack-smoke] ERROR: %s\n' "$1" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

service_container_id() {
  docker compose -f "$COMPOSE_FILE" ps -q "$1"
}

assert_service_healthy() {
  local service="$1"
  local container_id
  container_id="$(service_container_id "$service")"
  [ -n "$container_id" ] || fail "Service '$service' is not created in $COMPOSE_FILE"

  local running_state
  running_state="$(docker inspect --format '{{.State.Status}}' "$container_id")"
  [ "$running_state" = 'running' ] || fail "Service '$service' is not running (state=$running_state)"

  local health_state
  health_state="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container_id")"
  [ "$health_state" = 'healthy' ] || fail "Service '$service' is not healthy (health=$health_state)"

  log "Verified $service is running and healthy"
}

require_command docker
require_command node
require_command npm

log "Ensuring backend image $APP_IMAGE exists"
if ! docker image inspect "$APP_IMAGE" >/dev/null 2>&1; then
  log "Backend image missing; building via existing Jib production path"
  npm --prefix "$ROOT_DIR" run java:docker:prod
fi

log "Starting production-like compose stack from $COMPOSE_FILE"
docker compose -f "$COMPOSE_FILE" up -d --wait

assert_service_healthy app
assert_service_healthy postgresql
assert_service_healthy mailpit

log "Checking readiness endpoint at $PHASE12_BASE_URL/management/health/readiness"
PHASE12_BASE_URL="$PHASE12_BASE_URL" MAILPIT_API_URL="$MAILPIT_API_URL" node <<'NODE'
const baseUrl = process.env.PHASE12_BASE_URL;
const mailpitUrl = process.env.MAILPIT_API_URL;

async function fetchJson(url) {
  const response = await fetch(url, {
    headers: {
      Accept: 'application/json',
    },
  });
  const text = await response.text();
  let body;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = text;
  }
  return { response, body, text };
}

function assert(condition, message, details) {
  if (!condition) {
    const error = new Error(message);
    if (details) {
      error.details = details;
    }
    throw error;
  }
}

try {
  const readiness = await fetchJson(`${baseUrl}/management/health/readiness`);
  assert(readiness.response.ok, 'Readiness endpoint returned a non-200 response', readiness.body ?? readiness.text);
  assert(readiness.body?.status === 'UP', 'Application readiness status is not UP', readiness.body);
  assert(readiness.body?.components?.db?.status === 'UP', 'Database readiness component is not UP', readiness.body);

  const mailpit = await fetchJson(`${mailpitUrl}/api/v1/messages`);
  assert(mailpit.response.ok, 'Mailpit API did not respond successfully', mailpit.body ?? mailpit.text);

  console.log('[phase12-stack-smoke] Verified /management/health/readiness with db=UP');
  console.log('[phase12-stack-smoke] Verified Mailpit API reachability on compose-exposed port');
} catch (error) {
  console.error('[phase12-stack-smoke] ERROR:', error.message);
  if (error.details) {
    console.error(JSON.stringify(error.details, null, 2));
  }
  process.exit(1);
}
NODE

log "Stack smoke checks passed. Use 'npm run phase12:stack:down' for deterministic teardown when finished."
