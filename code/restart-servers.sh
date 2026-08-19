#!/bin/zsh
# Travit - 백엔드(8080)를 종료한 뒤 다시 실행

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

echo "[Travit] 1/2 Stopping running backend ..."
PIDS=$(lsof -ti tcp:8080 -sTCP:LISTEN 2>/dev/null)
if [ -n "$PIDS" ]; then
  echo "$PIDS" | xargs kill -9 2>/dev/null
fi
sleep 2

echo "[Travit] 2/2 Restarting backend ..."
osascript -e "tell application \"Terminal\" to do script \"cd '$DIR' && ./run-backend.sh\""

echo ""
echo "Backend window opened. It needs about 10 seconds to boot."
sleep 5
