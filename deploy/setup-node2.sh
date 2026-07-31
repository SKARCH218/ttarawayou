#!/usr/bin/env bash
# node2(10.8.1.102)에서 실행 — 백엔드(9090)와 프론트(3000)를 설치·기동한다.
#
#   tar -xzf trevit-deploy.tar.gz && cd trevit-deploy
#   sudo ./setup-node2.sh
#
# 다시 배포할 때도 그대로 실행하면 됩니다(덮어쓰고 재시작).

set -euo pipefail

BACKEND_PORT=9090     # 8080·8000 은 이미 사용 중이라 피함
FRONTEND_PORT=3000
APP_DIR=/opt/trevit
SRC="$(cd "$(dirname "$0")" && pwd)"

[ "$(id -u)" -eq 0 ] || { echo "sudo 로 실행해 주세요"; exit 1; }

echo "[1/6] 포트가 비어 있는지 확인 ..."
for p in "$BACKEND_PORT" "$FRONTEND_PORT"; do
  if ss -ltn "sport = :$p" 2>/dev/null | grep -q LISTEN; then
    echo "  !! $p 포트가 이미 사용 중입니다. 스크립트 상단에서 포트를 바꿔 주세요."
    ss -ltnp "sport = :$p" 2>/dev/null | tail -n +2
    exit 1
  fi
done
echo "     $BACKEND_PORT, $FRONTEND_PORT 사용 가능"

echo "[2/6] 필요한 패키지 ..."
if ! command -v java > /dev/null; then
  apt-get update -qq && apt-get install -y -qq openjdk-21-jre-headless
fi
if ! command -v node > /dev/null; then
  apt-get install -y -qq nodejs
fi
java -version 2>&1 | head -1
node --version

echo "[3/6] 사용자·디렉토리 ..."
id -u trevit > /dev/null 2>&1 || useradd -r -s /usr/sbin/nologin trevit
mkdir -p "$APP_DIR"

echo "[4/6] 파일 배치 ..."
cp "$SRC/trevit-api.jar" "$APP_DIR/"
rm -rf "$APP_DIR/frontend"
cp -R "$SRC/frontend" "$APP_DIR/"
# 환경변수 파일은 기존 값을 덮어쓰지 않는다 (키를 넣어 뒀을 수 있음)
if [ ! -f "$APP_DIR/trevit.env" ]; then
  cp "$SRC/.env.production.example" "$APP_DIR/trevit.env"
  echo "     $APP_DIR/trevit.env 를 새로 만들었습니다 — LLM 주소·API 키를 확인하세요"
fi
chown -R trevit:trevit "$APP_DIR"
chmod 600 "$APP_DIR/trevit.env"

echo "[5/6] 서비스 등록 ..."
cp "$SRC/trevit-backend.service" "$SRC/trevit-frontend.service" /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now trevit-backend trevit-frontend
systemctl restart trevit-backend trevit-frontend

echo "[6/6] 기동 확인 ..."
ok=1
for i in $(seq 1 30); do
  if curl -fs -o /dev/null "http://localhost:$BACKEND_PORT/api/wallet"; then ok=0; break; fi
  sleep 2
done
if [ "$ok" -ne 0 ]; then
  echo "  !! 백엔드가 응답하지 않습니다:  journalctl -u trevit-backend -n 50"
  exit 1
fi
curl -fs -o /dev/null "http://localhost:$FRONTEND_PORT/" || {
  echo "  !! 프론트가 응답하지 않습니다:  journalctl -u trevit-frontend -n 50"; exit 1; }

echo ""
echo "완료 — node2 준비됨"
echo "  백엔드  http://10.8.1.102:$BACKEND_PORT/api/wallet"
echo "  프론트  http://10.8.1.102:$FRONTEND_PORT/"
echo ""
echo "LLM(node1) 연결 확인:"
curl -fs --max-time 5 http://10.8.1.101:1234/v1/models > /dev/null \
  && echo "  10.8.1.101:1234 응답 O — AI 플랜 사용 가능" \
  || echo "  10.8.1.101:1234 응답 X — 휴리스틱으로 동작합니다(서비스는 정상)"
