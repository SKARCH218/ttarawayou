#!/bin/zsh
# Travit - stop the backend server on port 8080

echo "[Travit] Stopping backend ..."

PIDS=$(lsof -ti tcp:8080 -sTCP:LISTEN 2>/dev/null)
if [ -n "$PIDS" ]; then
  echo "$PIDS" | xargs kill -9 2>/dev/null
fi

sleep 2

if lsof -ti tcp:8080 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "WARNING: backend is still running, run this file again."
else
  echo "OK: backend stopped."
fi
