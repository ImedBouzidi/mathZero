#!/usr/bin/env bash
#
# Phase 1 smoke check: infrastructure, registry and Gateway.
# Requires: docker compose, curl. The registry and Gateway must be running.
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${ROOT_DIR}/infra/compose/docker-compose.yml"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:${GATEWAY_PORT:-8090}}"
EUREKA_URL="${EUREKA_URL:-http://localhost:${EUREKA_PORT:-8761}}"

failures=0

pass() {
  printf '  \033[32mPASS\033[0m %s\n' "$1"
}

fail() {
  printf '  \033[31mFAIL\033[0m %s\n' "$1"
  failures=$((failures + 1))
}

check_health() {
  local container="$1"
  local status
  status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || echo missing)"
  if [[ "$status" == "healthy" || "$status" == "running" ]]; then
    pass "$container is $status"
  else
    fail "$container is $status"
  fi
}

check_contains() {
  local url="$1"
  local expected="$2"
  local label="$3"
  local body
  if ! body="$(curl -fsS --max-time 10 "$url" 2>/dev/null)"; then
    fail "$label (request failed: $url)"
    return
  fi
  if [[ "$body" == *"$expected"* ]]; then
    pass "$label"
  else
    fail "$label (missing '$expected')"
  fi
}

echo "Mathematics Zero to Hero — Phase 1 smoke check"
echo

echo "Infrastructure"
check_health mathzero-postgres
check_health mathzero-rabbitmq

echo
echo "Service registry"
check_contains "${EUREKA_URL}/actuator/health" '"status":"UP"' "Eureka health is UP"

echo
echo "API Gateway"
check_contains "${GATEWAY_URL}/actuator/health" '"status":"UP"' "Gateway actuator health is UP"
check_contains "${GATEWAY_URL}/api/v1/health" '"service":"gateway-service"' "Public health endpoint responds"
check_contains "${EUREKA_URL}/eureka/apps/gateway-service" 'GATEWAY-SERVICE' "Gateway is registered in Eureka"

cors_status="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
  -X OPTIONS "${GATEWAY_URL}/api/v1/health" \
  -H 'Origin: http://localhost:4200' \
  -H 'Access-Control-Request-Method: GET' || echo 000)"
if [[ "$cors_status" == "200" || "$cors_status" == "204" ]]; then
  pass "CORS preflight for the Angular dev origin"
else
  fail "CORS preflight returned ${cors_status}"
fi

if curl -s -D - -o /dev/null --max-time 10 "${GATEWAY_URL}/api/v1/health" | grep -qi '^x-request-id:'; then
  pass "Responses expose a correlation ID"
else
  fail "Response header X-Request-Id is missing"
fi

echo
if [[ "$failures" -eq 0 ]]; then
  echo "All checks passed."
  exit 0
fi

echo "${failures} check(s) failed."
exit 1
