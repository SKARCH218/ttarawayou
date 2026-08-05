@echo off
rem Travit - start all three servers.
rem Windows Terminal이 있으면 탭 3개가 있는 한 창으로, 없으면 예전처럼 창 3개로 연다.
rem 주의: %~dp0은 백슬래시로 끝나 따옴표를 깨뜨리므로 -d 뒤에는 점(.)을 붙인다.
where wt >nul 2>nul
if errorlevel 1 goto :fallback

wt -w travit -d "%~dp0." --title "Backend 8080" cmd /k .\run-backend.bat
ping -n 2 127.0.0.1 >nul
wt -w travit new-tab -d "%~dp0." --title "Debug 3030" cmd /k .\run-debug.bat
ping -n 2 127.0.0.1 >nul
wt -w travit new-tab -d "%~dp0." --title "Web 3000" cmd /k .\run-frontend.bat
exit

:fallback
start cmd /k "cd /d "%~dp0" && .\run-backend.bat"
start cmd /k "cd /d "%~dp0" && .\run-debug.bat"
start cmd /k "cd /d "%~dp0" && .\run-frontend.bat"
exit
