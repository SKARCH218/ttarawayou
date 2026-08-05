@echo off
setlocal enabledelayedexpansion
rem Travit backend (Kotlin + Spring Boot) - port 8080
rem Windows version of run-backend.sh. API keys are read from code\.env (see .env.example).
rem Works without any keys too (seed data mode).
cd /d "%~dp0"

if exist ".env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
    set "K=%%a"
    set "K=!K:export =!"
    set "V=%%~b"
    if not "!K!"=="" set "!K!=!V!"
  )
  echo [Travit] Loaded API keys from .env
) else (
  echo [Travit] .env not found - starting without API keys, seed data mode
)

cd backend
echo [Travit] Starting Kotlin Spring backend on port 8080 ...
set "JAR="
for %%f in (build\libs\*-SNAPSHOT.jar) do set "JAR=%%f"
if defined JAR (
  java -jar "!JAR!"
) else (
  echo [Travit] No build output - running via Gradle, first run downloads dependencies ...
  call .\gradlew.bat bootRun
)
pause
