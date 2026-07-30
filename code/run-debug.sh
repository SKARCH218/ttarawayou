#!/bin/zsh
# MysteryTrip - Debug web server (port 3030)

# 현재 스크립트가 위치한 디렉토리 기준 frontend 로 이동
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR/frontend"

if [ ! -d node_modules ]; then
  echo "[MysteryTrip] Installing dependencies ..."
  npm install --no-audit --no-fund
fi

echo "[MysteryTrip] Starting DEBUG web server on port 3030 ..."
node debug-server.js