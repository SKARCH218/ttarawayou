@echo off
rem MysteryTrip - stop all servers, then start backend (8080), web (3000), debug web (3030)
cd /d "%~dp0"

echo [MysteryTrip] 1/2 Stopping running servers ...
powershell -NoProfile -Command "try { Get-NetTCPConnection -LocalPort 3000,3030,8080 -State Listen -ErrorAction Stop | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue } } catch {}"
ping -n 3 127.0.0.1 >nul

echo [MysteryTrip] 2/2 Restarting servers ...
start "MysteryTrip Backend (8080)" "%~dp0run-backend.bat"
start "MysteryTrip Web (3000)" "%~dp0run-frontend.bat"
start "MysteryTrip Debug Web (3030)" "%~dp0run-debug.bat"

echo.
echo Three server windows opened. Backend needs about 10 seconds to boot.
echo App: http://localhost:3000   Debug: http://localhost:3030
ping -n 6 127.0.0.1 >nul