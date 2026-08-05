#!/bin/zsh
# Travit - 백엔드(8080)와 Compose 웹(3000)을 각각 새 터미널 창에서 실행

DIR="$(cd "$(dirname "$0")" && pwd)"

# 각 스크립트를 새 Terminal 창에서 실행
for SCRIPT in run-backend.sh run-app-web.sh; do
  osascript -e "tell application \"Terminal\" to do script \"cd '$DIR' && ./$SCRIPT\""
done
