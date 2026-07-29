@echo off
rem MysteryTrip - Web server (port 3000)
cd /d "%~dp0frontend"
if not exist node_modules (
  echo [MysteryTrip] Installing dependencies ...
  call npm install --no-audit --no-fund
)
echo [MysteryTrip] Starting web server on port 3000 ...
node server.js
pause