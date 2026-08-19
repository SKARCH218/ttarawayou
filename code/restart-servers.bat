@echo off
rem Travit - stop the backend, then restart it (8080)
cd /d "%~dp0"

echo [Travit] 1/2 Stopping running backend ...
powershell -NoProfile -Command "try { Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction Stop | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue } } catch {}"
ping -n 3 127.0.0.1 >nul

echo [Travit] 2/2 Restarting backend ...
start "Travit Backend (8080)" "%~dp0run-backend.bat"

echo.
echo Backend window opened. It needs about 10 seconds to boot.
ping -n 6 127.0.0.1 >nul
