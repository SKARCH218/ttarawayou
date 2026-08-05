@echo off
rem Travit - start backend (8080) and the Compose web app (3000).
rem Windows Terminal이 있으면 탭 2개가 있는 한 창으로, 없으면 창 2개로 연다.
rem 주의: %~dp0은 백슬래시로 끝나 따옴표를 깨뜨리므로 -d 뒤에는 점(.)을 붙인다.
where wt >nul 2>nul
if errorlevel 1 goto :fallback

wt -w travit -d "%~dp0." --title "Backend 8080" cmd /k .\run-backend.bat
ping -n 2 127.0.0.1 >nul
wt -w travit new-tab -d "%~dp0." --title "Web 3000" cmd /k .\run-app-web.bat
exit

:fallback
start cmd /k "cd /d "%~dp0" && .\run-backend.bat"
start cmd /k "cd /d "%~dp0" && .\run-app-web.bat"
exit
