#!/usr/bin/env bash
# 집 VPN 서버(172.30.1.100)에서 실행 — nginx 리버스 프록시를 세운다.
#
#   scp nginx-trevit.conf setup-vpn-server.sh user@172.30.1.100:~/
#   ssh user@172.30.1.100 'sudo ./setup-vpn-server.sh'

set -euo pipefail

NODE2=10.8.1.102
BACKEND_PORT=9090
FRONTEND_PORT=3000
SRC="$(cd "$(dirname "$0")" && pwd)"

[ "$(id -u)" -eq 0 ] || { echo "sudo 로 실행해 주세요"; exit 1; }

echo "[1/4] node2 에 닿는지 확인 (VPN 경유) ..."
for p in "$FRONTEND_PORT" "$BACKEND_PORT"; do
  if timeout 5 bash -c "</dev/tcp/$NODE2/$p" 2>/dev/null; then
    echo "     $NODE2:$p 연결 O"
  else
    echo "  !! $NODE2:$p 에 닿지 않습니다."
    echo "     node2 에서 setup-node2.sh 를 먼저 실행했는지, VPN 이 연결돼 있는지 확인하세요."
    exit 1
  fi
done

echo "[2/4] nginx 설치 ..."
command -v nginx > /dev/null || { apt-get update -qq && apt-get install -y -qq nginx; }

echo "[3/4] 설정 적용 ..."
cp "$SRC/nginx-trevit.conf" /etc/nginx/sites-available/trevit
ln -sf /etc/nginx/sites-available/trevit /etc/nginx/sites-enabled/trevit
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

echo "[4/4] 확인 ..."
curl -fs -o /dev/null http://localhost/ && echo "     웹 200"
curl -fs -o /dev/null http://localhost/api/wallet && echo "     API 200"

echo ""
echo "완료 — 이제 공유기에서 아래 포트를 이 서버로 포워딩하세요."
echo "  80  (필수)  →  172.30.1.100"
echo "  443 (HTTPS 쓸 때) → 172.30.1.100"
echo ""
echo "포워딩 후 밖에서 확인:  curl http://112.166.208.166/api/wallet"
