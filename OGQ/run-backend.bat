@echo off
rem MysteryTrip - Spring API server (port 8080)
cd /d "%~dp0"
set ODSAY_API_KEY=YOUR_ODSAY_API_KEY
rem data.go.kr (TAGO bus route API) service key - paste your key after the equals sign
set DATA_GO_KR_API_KEY=YOUR_DATA_GO_KR_API_KEY
set LMSTUDIO_BASE_URL=http://10.8.2.4:1234/v1
echo [MysteryTrip] Starting Spring API server on port 8080 ...
java -jar "backend\target\mystery-trip-api-0.0.1-SNAPSHOT.jar"
pause