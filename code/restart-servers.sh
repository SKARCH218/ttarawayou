#!/bin/zsh
# Travit - 모든 서버를 종료한 뒤 백엔드(8080)와 Compose 웹(3000)을 다시 실행

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

echo "[Travit] 1/2 Stopping running servers ..."
for PORT in 3000 8080; do
  PIDS=$(lsof -ti tcp:"$PORT" -sTCP:LISTEN 2>/dev/null)
  if [ -n "$PIDS" ]; then
    echo "$PIDS" | xargs kill -9 2>/dev/null
  fi
done
sleep 2

echo "[Travit] 2/2 Restarting servers ..."
osascript -e "tell application \"Terminal\" to do script \"cd '$DIR' && ./run-backend.sh\""
osascript -e "tell application \"Terminal\" to do script \"cd '$DIR' && ./run-app-web.sh\""

echo ""
echo "Two server windows opened. Backend needs about 10 seconds to boot."
echo "App: http://localhost:3000"
sleep 5
