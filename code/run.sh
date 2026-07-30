#!/bin/zsh
# MysteryTrip - 백엔드(8080), 웹(3000), 디버그 웹(3030)을 각각 새 터미널 창에서 실행

DIR="$(cd "$(dirname "$0")" && pwd)"

# 각 스크립트를 새 Terminal 창에서 실행
for SCRIPT in run-backend.sh run-debug.sh run-frontend.sh; do
  osascript -e "tell application \"Terminal\" to do script \"cd '$DIR' && ./$SCRIPT\""
done