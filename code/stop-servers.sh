#!/bin/zsh
# MysteryTrip - stop every server on ports 3000 (web), 3030 (debug), 8080 (backend)

echo "[MysteryTrip] Stopping all running servers ..."

PORTS=(3000 3030 8080)

# 각 포트를 LISTEN 중인 프로세스를 찾아 종료
for PORT in $PORTS; do
  PIDS=$(lsof -ti tcp:"$PORT" -sTCP:LISTEN 2>/dev/null)
  if [ -n "$PIDS" ]; then
    echo "$PIDS" | xargs kill -9 2>/dev/null
  fi
done

sleep 2

# 종료 확인
STILL=""
for PORT in $PORTS; do
  if lsof -ti tcp:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    STILL="$STILL $PORT"
  fi
done

if [ -n "$STILL" ]; then
  echo "WARNING: some servers are still running (ports:$STILL), run this file again."
else
  echo "OK: all servers stopped."
fi