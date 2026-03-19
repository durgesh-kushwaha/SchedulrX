#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "[validate] Backend tests"
cd "${REPO_ROOT}"
mvn -B -ntp test

echo "[validate] Frontend install/build"
cd "${REPO_ROOT}/frontend"
npm ci
npm run build

cd "${REPO_ROOT}"
if [[ "${RUN_API_SMOKE:-0}" == "1" ]]; then
  echo "[validate] API smoke tests enabled"

  API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api/v1}"
  START_BACKEND_FOR_SMOKE="${START_BACKEND_FOR_SMOKE:-1}"
  BACKEND_PID=""

  if [[ "${START_BACKEND_FOR_SMOKE}" == "1" ]]; then
    echo "[validate] Starting backend for smoke checks"
    mvn -q spring-boot:run >/tmp/exam-scheduler-smoke.log 2>&1 &
    BACKEND_PID="$!"

    cleanup() {
      if [[ -n "${BACKEND_PID}" ]]; then
        kill "${BACKEND_PID}" >/dev/null 2>&1 || true
      fi
    }
    trap cleanup EXIT

    for _ in {1..60}; do
      if curl -fsS "http://localhost:8080/actuator/health" >/dev/null 2>&1; then
        break
      fi
      sleep 1
    done
  fi

  "${REPO_ROOT}/scripts/smoke-api.sh"

  if [[ -n "${BACKEND_PID}" ]]; then
    kill "${BACKEND_PID}" >/dev/null 2>&1 || true
    BACKEND_PID=""
    trap - EXIT
  fi
else
  echo "[validate] API smoke tests skipped (set RUN_API_SMOKE=1 to enable)"
fi

echo "[validate] All checks passed."
