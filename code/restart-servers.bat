@echo off
rem Travit - stop all servers, then start backend (8080) and Compose web (3000)
cd /d "%~dp0"

echo [Travit] 1/2 Stopping running servers ...
powershell -NoProfile -Command "try { Get-NetTCPConnection -LocalPort 3000,8080 -State Listen -ErrorAction Stop | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue } } catch {}"
ping -n 3 127.0.0.1 >nul

echo [Travit] 2/2 Restarting servers ...
start "Travit Backend (8080)" "%~dp0run-backend.bat"
start "Travit Web (3000)" "%~dp0run-app-web.bat"

echo.
echo Two server windows opened. Backend needs about 10 seconds to boot.
echo App: http://localhost:3000
ping -n 6 127.0.0.1 >nul
