#!/bin/bash
# 맥에서 실행 — VPN 서버(172.30.1.100)를 경유해 node2(10.8.2.5)에 최신 OGQ 브랜치를
# 빌드·배포한다. node2-deploy.sh 를 매번 scp 로 올리므로, 이 저장소에서 그 파일을
# 고치면 다음 실행 때 그대로 반영된다 (node2 쪽엔 git 으로 관리하지 않는다).
#
# 사용법:
#   ./deploy-node2.sh          지금 커밋으로 강제 배포
#   ./deploy-node2.sh --check  배포 없이 원격 상태만 확인 (git log, 서비스 상태)

set -euo pipefail

VPN_HOP=user@172.30.1.100
NODE2=playlabs@10.8.2.5
DIR="$(cd "$(dirname "$0")" && pwd)"

ssh_node2() { ssh -J "$VPN_HOP" -o ConnectTimeout=15 "$NODE2" "$@"; }
scp_node2() { scp -J "$VPN_HOP" -o ConnectTimeout=15 "$@"; }

if [ "${1:-}" = "--check" ]; then
  echo "[node2] 배포 로그 최근 10줄:"
  ssh_node2 "tail -n 10 ~/deploy.log 2>/dev/null || echo '(아직 배포 이력 없음)'"
  echo
  echo "[node2] 서비스 상태:"
  ssh_node2 "systemctl --user is-active trevit; systemctl --user status trevit --no-pager 2>/dev/null | head -4"
  exit 0
fi

echo "[1/3] 배포 스크립트를 node2 로 전송..."
scp_node2 "$DIR/node2-deploy.sh" "$NODE2:~/deploy.sh"
ssh_node2 "chmod +x ~/deploy.sh"

echo "[2/3] 빌드·배포 실행 (첫 실행은 의존성 다운로드로 수 분 걸릴 수 있음)..."
ssh_node2 "~/deploy.sh --force"

echo "[3/3] 외부에서 최종 확인..."
sleep 2
curl -sf -o /dev/null -m 10 http://112.166.208.166/ \
  && echo "완료 — http://112.166.208.166 정상 응답" \
  || echo "경고 — 외부 URL 응답 없음. deploy-node2.sh --check 로 원인을 확인할 것"
