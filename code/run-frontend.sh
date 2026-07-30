#!/bin/zsh
# MysteryTrip - Web server (port 3000)

# 현재 스크립트가 위치한 디렉토리 기준 frontend 로 이동
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR/frontend"

if [ ! -d node_modules ]; then
  echo "[MysteryTrip] Installing dependencies ..."
  npm install --no-audit --no-fund
fi

echo "[MysteryTrip] Starting web server on port 3000 ..."
node server.js
