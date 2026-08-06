#!/bin/bash
# node2(10.8.2.5)에서 직접 실행되는 배포 로직.
# git 저장소가 아닌 홈 디렉터리(~/deploy.sh)에 scp로 올려 두고 쓴다 —
# ~/ttarawayou-build 는 git reset --hard 로 매번 초기화되므로,
# 이 스크립트 자신은 그 저장소 밖에 있어야 살아남는다.
#
# 실행 방법:
#   ~/deploy.sh          평소 실행 (원격에 새 커밋 없으면 곧바로 종료)
#   ~/deploy.sh --force  같은 커밋이어도 강제로 다시 빌드·배포
#
# 실패하면 반드시 이전 jar로 롤백한다 — 배포 중 오류가 라이브 서비스를
# 끊어놓는 일은 없어야 한다.

set -uo pipefail

# ssh 비대화형 셸은 .bashrc/.profile 을 읽지 않아 PATH·JAVA_HOME 이 비어 있다.
# gradlew 부트스트랩 스크립트 자체가 이 값으로 JVM을 찾으므로 여기서 직접 지정한다.
export JAVA_HOME=/home/playlabs/jdk21
export PATH="$JAVA_HOME/bin:$PATH"

REPO=~/ttarawayou-build
RUNDIR=~/trevit
JAR="$RUNDIR/trevit-api.jar"
BRANCH=OGQ
LOG=~/deploy.log
BUILD_LOG=~/deploy-build.log
LOCK=~/deploy.lock

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG"; }

# 타이머와 수동 실행이 겹쳐 동시에 빌드하지 않도록 락을 건다
exec 200>"$LOCK"
if ! flock -n 200; then
  log "이미 다른 배포가 진행 중 — 건너뜀"
  exit 0
fi

if [ ! -d "$REPO/.git" ]; then
  log "저장소 없음: $REPO (최초 1회는 수동으로 git clone 해둘 것)"
  exit 1
fi

cd "$REPO" || exit 1
git fetch origin "$BRANCH" --quiet 2>>"$LOG"
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse "origin/$BRANCH")

if [ "${1:-}" != "--force" ] && [ "$LOCAL" = "$REMOTE" ]; then
  exit 0   # 변경 없음 — 로그도 남기지 않는다 (5분마다 도는 타이머라 조용히 넘어감)
fi

log "배포 시작: ${LOCAL:0:7} → ${REMOTE:0:7}"
: > "$BUILD_LOG"
git reset --hard "origin/$BRANCH" >>"$BUILD_LOG" 2>&1

# 1) 웹(Compose wasm) 빌드 — 백엔드 jar 가 이 결과물을 static/ 으로 품는다
cd "$REPO/code/app-kmp" || exit 1
if ! ./gradlew :composeApp:wasmJsBrowserDistribution --console=plain >>"$BUILD_LOG" 2>&1; then
  log "실패: 웹(wasm) 빌드 — 로그: $BUILD_LOG"
  exit 1
fi

# 2) 백엔드 jar 빌드
cd "$REPO/code/backend" || exit 1
if ! ./gradlew bootJar --console=plain >>"$BUILD_LOG" 2>&1; then
  log "실패: 백엔드 빌드 — 로그: $BUILD_LOG"
  exit 1
fi

NEWJAR=$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -1)
if [ -z "$NEWJAR" ]; then
  log "실패: 빌드된 jar를 찾지 못함 (build/libs 확인 필요)"
  exit 1
fi

# 3) 원자적 교체 준비 — 같은 파티션 내 mv 는 순간적으로 끝나 무중단에 가깝다
mkdir -p "$RUNDIR"
[ -f "$JAR" ] && cp "$JAR" "$JAR.bak"
cp "$NEWJAR" "$JAR.new"
mv "$JAR.new" "$JAR"

# 4) 재시작 + 헬스체크 (최대 2분 대기)
systemctl --user restart trevit
OK=0
for i in $(seq 1 24); do
  sleep 5
  if curl -sf -o /dev/null -m 5 http://127.0.0.1:9090/; then
    OK=1
    break
  fi
done

if [ "$OK" = "1" ]; then
  log "배포 성공: ${REMOTE:0:7} (기동까지 ${i}x5초)"
  rm -f "$JAR.bak"
else
  log "헬스체크 실패 — 이전 jar로 롤백한다"
  if [ -f "$JAR.bak" ]; then
    mv "$JAR.bak" "$JAR"
    systemctl --user restart trevit
    log "롤백 완료 — 서비스는 이전 커밋(${LOCAL:0:7})으로 복구됨"
  else
    log "경고: 백업 jar 가 없어 롤백 불가 — 수동 확인 필요"
  fi
  exit 1
fi
