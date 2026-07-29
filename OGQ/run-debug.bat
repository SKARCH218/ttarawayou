@echo off
rem MysteryTrip - Debug web server (port 3030)
cd /d "%~dp0frontend"
if not exist node_modules (
  echo [MysteryTrip] Installing dependencies ...
  call npm install --no-audit --no-fund
)
echo [MysteryTrip] Starting DEBUG web server on port 3030 ...
node debug-server.js
pause
