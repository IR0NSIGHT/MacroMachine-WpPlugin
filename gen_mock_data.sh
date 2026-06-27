#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

cd "$SCRIPT_DIR"

SKIP_BUILD=false
for arg in "$@"; do
  [ "$arg" = "--fast" ] && SKIP_BUILD=true
done

if [ "$SKIP_BUILD" = true ]; then
  echo "=== Skipping build (--fast) ==="
else
  echo "=== Building backend ==="
  mvn -q package -DskipTests dependency:copy-dependencies
fi

BACKEND_LOG=$(mktemp)

cleanup() {
  kill "$BACKEND_PID" 2>/dev/null || true
  rm -f "$BACKEND_LOG"
}
trap cleanup EXIT INT TERM

echo "=== Starting backend server ==="
"$SCRIPT_DIR/run_backend.sh" > "$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!

echo "=== Waiting for backend to start (30s timeout) ==="
TIMEOUT=30
ELAPSED=0
while ! grep -q "REST server started:" "$BACKEND_LOG" 2>/dev/null; do
  sleep 0.5
  ELAPSED=$((ELAPSED + 1))
  if [ "$ELAPSED" -ge 60 ]; then
    echo "ERROR: Backend did not start within ${TIMEOUT}s"
    kill "$BACKEND_PID" 2>/dev/null || true
    exit 1
  fi
done

echo "=== Generating mock data ==="
node "$SCRIPT_DIR/webUI/src/mocks/data/genIO.mjs"

echo "=== Stopping backend ==="
kill "$BACKEND_PID" 2>/dev/null || true

echo "=== Done ==="
