#!/bin/zsh

# 현재 스크립트가 위치한 디렉토리로 이동
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

# API 키는 .env 파일에서 읽는다 (git에 커밋되지 않음 — .env.example 참고)
if [ -f "$DIR/.env" ]; then
  source "$DIR/.env"
else
  echo "[MysteryTrip] .env 없음 — 키 없이 시작합니다 (시드 데이터 폴백 모드)"
fi

echo "[MysteryTrip] Starting Spring API server on port 8080 ..."
java -jar "backend/target/mystery-trip-api-0.0.1-SNAPSHOT.jar"
