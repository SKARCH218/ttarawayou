#!/bin/zsh
# 트레빗 앱 실행 — 백엔드·에뮬레이터를 확인해 없으면 띄우고, APK를 설치·실행한다.
#
#   ./run-app.sh              APK가 있으면 그대로 설치·실행 (가장 빠름)
#   ./run-app.sh --build      코드를 고쳤을 때. 새로 빌드한 뒤 설치·실행
#   ./run-app.sh --device     USB로 연결한 실제 폰에 설치 (서버 주소 안내 포함)
#
# 앱은 백엔드(8080)에 붙어 동작한다. 에뮬레이터는 10.0.2.2:8080 으로 자동 연결되고,
# 실제 폰은 앱 홈 우상단 설정(⚙)에서 이 PC의 LAN 주소로 바꿔 줘야 한다.

set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR="$DIR/app-kmp"
PKG="com.trevit.app"
ACTIVITY="$PKG/.MainActivity"
APK="$APP_DIR/composeApp/build/outputs/apk/debug/composeApp-debug.apk"
AVD="Pixel_3a"

BUILD=0
DEVICE=0
for arg in "$@"; do
  case "$arg" in
    --build)  BUILD=1 ;;
    --device) DEVICE=1 ;;
    -h|--help) sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "알 수 없는 옵션: $arg (--build, --device 만 지원)"; exit 1 ;;
  esac
done

# ---------- Android SDK ----------
: "${ANDROID_HOME:=$HOME/Library/Android/sdk}"
export ANDROID_HOME
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"

if ! command -v adb > /dev/null; then
  echo "adb를 찾을 수 없습니다. Android SDK 위치를 확인해 주세요 (ANDROID_HOME=$ANDROID_HOME)"
  exit 1
fi

# ---------- 1) 백엔드 (8080) ----------
if curl -s -o /dev/null --max-time 3 "http://localhost:8080/api/wallet"; then
  echo "[1/4] 백엔드 이미 실행 중 (8080)"
else
  echo "[1/4] 백엔드가 꺼져 있어 새로 띄웁니다 ..."
  nohup "$DIR/run-backend.sh" > "$DIR/.backend.log" 2>&1 &
  for i in $(seq 1 40); do
    curl -s -o /dev/null --max-time 2 "http://localhost:8080/api/wallet" && break
    sleep 2
  done
  if curl -s -o /dev/null --max-time 3 "http://localhost:8080/api/wallet"; then
    echo "      백엔드 준비 완료"
  else
    echo "      백엔드가 뜨지 않았습니다 — $DIR/.backend.log 를 확인해 주세요"
    exit 1
  fi
fi

# ---------- 2) 기기 (에뮬레이터 또는 실제 폰) ----------
if [ "$DEVICE" = "1" ]; then
  echo "[2/4] USB로 연결된 기기를 찾는 중 ..."
  adb wait-for-device
else
  if adb devices | grep -q "emulator.*device$"; then
    echo "[2/4] 에뮬레이터 이미 실행 중"
  else
    echo "[2/4] 에뮬레이터($AVD)를 켭니다 — 처음이면 1분 정도 걸립니다 ..."
    nohup emulator -avd "$AVD" -no-snapshot-load -no-audio > "$DIR/.emulator.log" 2>&1 &
    adb wait-for-device
    for i in $(seq 1 60); do
      [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
      sleep 3
    done
    echo "      부팅 완료"
  fi
fi

# ---------- 3) 빌드 ----------
if [ "$BUILD" = "1" ] || [ ! -f "$APK" ]; then
  echo "[3/4] APK 빌드 중 (처음이면 몇 분 걸립니다) ..."
  # AGP가 아직 JDK 26을 지원하지 않아 21로 빌드한다 (gradle.properties 에도 고정돼 있음)
  JDK21="/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home"
  [ -d "$JDK21" ] && export JAVA_HOME="$JDK21"
  (cd "$APP_DIR" && ./gradlew :composeApp:assembleDebug -q)
else
  echo "[3/4] 기존 APK 사용 (새로 빌드하려면 --build)"
fi

# ---------- 4) 설치·실행 ----------
echo "[4/4] 설치 후 실행 ..."
adb install -r "$APK" > /dev/null
adb shell am start -n "$ACTIVITY" > /dev/null

echo ""
echo "트레빗 실행됨"

if [ "$DEVICE" = "1" ]; then
  IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "")
  echo ""
  echo "  실제 폰에서는 서버 주소를 바꿔 줘야 합니다 (10.0.2.2 는 에뮬레이터 전용):"
  echo "    앱 홈 화면 우상단 설정(⚙) → 아래 주소 입력"
  if [ -n "$IP" ]; then
    echo "    http://$IP:8080      ← 폰과 이 PC가 같은 Wi-Fi 여야 합니다"
  else
    echo "    http://<이 PC의 LAN IP>:8080   (확인: ipconfig getifaddr en0)"
  fi
  echo ""
  echo "  배포된 서버가 있다면 그 주소를 넣는 편이 편합니다 (PC를 켜둘 필요 없음)."
fi
