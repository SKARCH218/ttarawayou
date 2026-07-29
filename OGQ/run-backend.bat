@echo off
rem MysteryTrip - Spring API server (port 8080)
rem Requirements: Java 21+ only (Maven is NOT needed - Maven Wrapper builds automatically)
cd /d "%~dp0"

rem ===== API keys (paste yours after the equals sign) =====
set ODSAY_API_KEY=YOUR_ODSAY_API_KEY
rem data.go.kr (TAGO bus arrival/intercity API) service key
set DATA_GO_KR_API_KEY=YOUR_DATA_GO_KR_API_KEY
rem SK open API (TMAP) app key - REQUIRED (map, routing, places)
set TMAP_APP_KEY=YOUR_TMAP_APP_KEY
rem Local AI server (LM Studio, optional - falls back to algorithm if unreachable)
set LMSTUDIO_BASE_URL=http://10.8.2.4:1234/v1
rem ========================================================

rem First run on a new PC: build the backend automatically
rem (Maven Wrapper uses JAVA_HOME if set, otherwise java from PATH)
if not exist "backend\target\mystery-trip-api-0.0.1-SNAPSHOT.jar" (
  echo [MysteryTrip] First run - building backend, this takes a few minutes ...
  pushd backend
  call .\mvnw.cmd -q -DskipTests package
  popd
  if not exist "backend\target\mystery-trip-api-0.0.1-SNAPSHOT.jar" (
    echo [MysteryTrip] BUILD FAILED - check that Java 21+ is installed and internet is available.
    pause
    exit /b 1
  )
)

echo [MysteryTrip] Starting Spring API server on port 8080 ...
java -jar "backend\target\mystery-trip-api-0.0.1-SNAPSHOT.jar"
pause
