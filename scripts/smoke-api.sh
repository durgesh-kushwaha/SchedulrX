#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api/v1}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required for smoke tests." >&2
  exit 1
fi

echo "[smoke] Logging in via ${API_BASE_URL}/auth/login"
LOGIN_RESPONSE="$(curl -fsS -X POST "${API_BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}")"

TOKEN="$(echo "${LOGIN_RESPONSE}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
if [[ -z "${TOKEN}" ]]; then
  echo "[smoke] Failed to extract JWT token from login response." >&2
  exit 1
fi

echo "[smoke] Fetching schedules"
curl -fsS "${API_BASE_URL}/schedules?page=0&size=5" \
  -H "Authorization: Bearer ${TOKEN}" \
  >/dev/null

echo "[smoke] Fetching analytics overview"
curl -fsS "${API_BASE_URL}/analytics/overview" \
  -H "Authorization: Bearer ${TOKEN}" \
  >/dev/null

echo "[smoke] API smoke tests passed."
