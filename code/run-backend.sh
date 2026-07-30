#!/bin/zsh
# 트레빗 백엔드 (Kotlin + Spring Boot) — 8080 포트
# 웹 데모까지 이 서버가 함께 서빙하므로 이것만 띄우면 http://localhost:8080 에서 바로 쓸 수 있다.

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR/backend"

# API 키는 code/.env 에서 읽는다 (git 제외 — .env.example 참고).
# 키가 하나도 없어도 수도권 시드 데이터로 전체 플로우가 동작한다.
if [ -f "$DIR/.env" ]; then
  source "$DIR/.env"
else
  echo "[트레빗] .env 없음 — 키 없이 시작합니다 (시드 데이터 모드)"
fi

echo "[트레빗] Kotlin Spring 백엔드 시작 (8080) ..."
JAR=$(ls build/libs/*-SNAPSHOT.jar 2>/dev/null | head -1)

if [ -n "$JAR" ]; then
  exec java -jar "$JAR"
else
  # 빌드 산출물이 없으면 Gradle로 바로 실행 (최초 실행 시 의존성 내려받느라 조금 걸린다)
  echo "[트레빗] 빌드 산출물이 없어 Gradle로 실행합니다 ..."
  exec ./gradlew bootRun
fi
