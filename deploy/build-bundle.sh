#!/bin/zsh
# 개발 PC에서 실행 — 배포용 묶음(trevit-deploy.tar.gz)을 만든다.
#
#   ./build-bundle.sh
#   scp trevit-deploy.tar.gz playlabs@10.8.2.5:~/
#
# 묶음 안: 백엔드 jar, 프론트(정적 웹 + Express), systemd 유닛, 환경변수 예시, 설치 스크립트

set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/.." && pwd)"
OUT="$DIR/trevit-deploy.tar.gz"
STAGE="$(mktemp -d)/trevit-deploy"

echo "[1/3] 백엔드 빌드 ..."
(cd "$ROOT/code/backend" && ./gradlew bootJar -q)
JAR=$(ls "$ROOT/code/backend/build/libs/"*-SNAPSHOT.jar | head -1)

echo "[2/3] 파일 모으는 중 ..."
mkdir -p "$STAGE/frontend"
cp "$JAR" "$STAGE/trevit-api.jar"
cp -R "$ROOT/code/frontend/public" "$STAGE/frontend/"
# 개발 도구가 남긴 내부 상태는 배포에 넣지 않는다
find "$STAGE/frontend" -name ".omc" -type d -prune -exec rm -rf {} + 2>/dev/null || true
cp "$ROOT/code/frontend/server.js" "$ROOT/code/frontend/package.json" "$STAGE/frontend/"
cp "$DIR/trevit-backend.service" "$DIR/trevit-frontend.service" "$STAGE/"
cp "$DIR/.env.production.example" "$STAGE/"
cp "$DIR/setup-node2.sh" "$STAGE/"
chmod +x "$STAGE/setup-node2.sh"

echo "[3/3] 압축 ..."
tar -czf "$OUT" -C "$(dirname "$STAGE")" trevit-deploy
rm -rf "$(dirname "$STAGE")"

echo ""
echo "완성: $OUT  ($(du -h "$OUT" | cut -f1))"
echo ""
echo "다음 단계:"
echo "  scp $OUT playlabs@10.8.2.5:~/"
echo "  ssh playlabs@10.8.2.5"
echo "    tar -xzf trevit-deploy.tar.gz && cd trevit-deploy && sudo ./setup-node2.sh"
