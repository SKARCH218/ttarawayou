@echo off
rem Travit - stop the backend server on port 8080
echo [Travit] Stopping backend ...
powershell -NoProfile -Command "try { Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction Stop | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue } } catch {}"
ping -n 3 127.0.0.1 >nul
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { Write-Host 'WARNING: backend is still running, run this file again.' } else { Write-Host 'OK: backend stopped.' }"
pause
