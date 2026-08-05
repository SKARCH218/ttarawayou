@echo off
REM Dev mode: watches the Compose code and rebuilds + reloads the browser
REM automatically whenever app code changes. Serves at http://localhost:3000
cd /d "%~dp0app-kmp"
call .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun --continuous
