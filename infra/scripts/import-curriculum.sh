#!/usr/bin/env bash
#
# Bulk-import a curriculum document through the public API Gateway.
#
# Usage:
#   ./infra/scripts/import-curriculum.sh document.json
#
# Environment:
#   API_BASE_URL    Gateway API base (default: http://localhost:8090/api/v1)
#   ADMIN_EMAIL     Administrator email (default: admin@mathplay.com)
#   ADMIN_PASSWORD  Overrides APP_ADMIN_PASSWORD from identity-service/.env
#   DRY_RUN         true to validate without writing (default: true)
#
# The script authenticates with the same CSRF-protected flow the browser uses,
# then POSTs the JSON document to /admin/curricula/import. It never prints the
# access token.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
IDENTITY_ENV_FILE="${IDENTITY_ENV_FILE:-${ROOT_DIR}/backend/identity-service/.env}"
if [[ -f "$IDENTITY_ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$IDENTITY_ENV_FILE"
  set +a
fi

API_BASE_URL="${API_BASE_URL:-http://localhost:8090/api/v1}"
ADMIN_EMAIL="${ADMIN_EMAIL:-${APP_ADMIN_EMAIL:-admin@mathplay.com}}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-${APP_ADMIN_PASSWORD:-}}"
DRY_RUN="${DRY_RUN:-true}"
DOCUMENT="${1:-}"

if [[ "$DRY_RUN" != "true" && "$DRY_RUN" != "false" ]]; then
  echo "DRY_RUN must be true or false" >&2
  exit 2
fi

if [[ -z "$DOCUMENT" ]]; then
  echo "Usage: $0 document.json" >&2
  exit 2
fi

if [[ ! -f "$DOCUMENT" ]]; then
  echo "Document not found: $DOCUMENT" >&2
  exit 1
fi
if [[ -z "$ADMIN_PASSWORD" ]]; then
  echo "Set APP_ADMIN_PASSWORD in ${IDENTITY_ENV_FILE} or provide ADMIN_PASSWORD" >&2
  exit 1
fi
if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to parse the login response" >&2
  exit 1
fi

cookie_jar="$(mktemp)"
payload_file="$(mktemp)"
chmod 600 "$cookie_jar" "$payload_file"
trap 'rm -f "$cookie_jar" "$payload_file"' EXIT

echo "→ Requesting CSRF token from ${API_BASE_URL}"
curl -fsS -c "$cookie_jar" "${API_BASE_URL}/auth/csrf" >/dev/null

csrf_token="$(awk '$6 == "mathzero_csrf" { print $7 }' "$cookie_jar" | tail -n 1)"
if [[ -z "$csrf_token" ]]; then
  echo "Could not read the CSRF cookie" >&2
  exit 1
fi

export ADMIN_EMAIL ADMIN_PASSWORD
python3 -c 'import json,os; print(json.dumps({"email": os.environ["ADMIN_EMAIL"], "password": os.environ["ADMIN_PASSWORD"]}))' \
  > "$payload_file"

echo "→ Signing in as ${ADMIN_EMAIL}"
login_response="$(curl -fsS \
  -b "$cookie_jar" -c "$cookie_jar" \
  --data-binary "@${payload_file}" \
  --config - "${API_BASE_URL}/auth/login" <<EOF
header = "Content-Type: application/json"
header = "X-CSRF-Token: ${csrf_token}"
EOF
)"

access_token="$(printf '%s' "$login_response" | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["accessToken"])')"
if [[ -z "$access_token" ]]; then
  echo "Login did not return an access token" >&2
  exit 1
fi

import_url="${API_BASE_URL}/admin/curricula/import"
if [[ "$DRY_RUN" == "true" ]]; then
  import_url="${import_url}?dryRun=true"
  echo "→ Validating ${DOCUMENT} (dry run)"
else
  echo "→ Importing ${DOCUMENT}"
fi

response="$(curl -fsS -X POST \
  --data-binary "@${DOCUMENT}" \
  --config - "$import_url" <<EOF
header = "Authorization: Bearer ${access_token}"
header = "Content-Type: application/json"
EOF
)"
printf '%s' "$response" | python3 -c '
import json,sys
response=json.load(sys.stdin)
print(json.dumps(response, indent=2, ensure_ascii=False))
if not response.get("data", {}).get("valid", False):
    sys.exit(1)
'
