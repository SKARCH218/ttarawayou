@echo off
rem MysteryTrip - stop every server on ports 3000 (web), 3030 (debug), 8080 (backend)
echo [MysteryTrip] Stopping all running servers ...
powershell -NoProfile -Command "try { Get-NetTCPConnection -LocalPort 3000,3030,8080 -State Listen -ErrorAction Stop | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue } } catch {}"
ping -n 3 127.0.0.1 >nul
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 3000,3030,8080 -State Listen -ErrorAction SilentlyContinue) { Write-Host 'WARNING: some servers are still running, run this file again.' } else { Write-Host 'OK: all servers stopped.' }"
pause