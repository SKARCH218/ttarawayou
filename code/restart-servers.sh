#!/bin/zsh
# MysteryTrip - 모든 서버를 종료한 뒤 백엔드(8080), 웹(3000), 디버그 웹(3030)을 다시 실행

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

echo "[MysteryTrip] 1/2 Stopping running servers ..."
for PORT in 3000 3030 8080; do
  PIDS=$(lsof -ti tcp:"$PORT" -sTCP:LISTEN 2>/dev/null)
  if [ -n "$PIDS" ]; then
    echo "$PIDS" | xargs kill -9 2>/dev/null
  fi
done
sleep 2

echo "[MysteryTrip] 2/2 Restarting servers ..."
osascript -e "tell application \"Terminal\" to do script \"cd '$DIR' && ./run-backend.sh\""
osascript -e "tell application \"Terminal\" to do script \"cd '$DIR' && ./run-frontend.sh\""
osascript -e "tell application \"Terminal\" to do script \"cd '$DIR' && ./run-debug.sh\""

echo ""
echo "Three server windows opened. Backend needs about 10 seconds to boot."
echo "App: http://localhost:3000   Debug: http://localhost:3030"
sleep 5