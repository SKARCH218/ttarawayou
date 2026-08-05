@echo off
REM Build the Compose Multiplatform web app - same screens as the Android app -
REM and serve it at http://localhost:3000
cd /d "%~dp0app-kmp"
call .\gradlew.bat :composeApp:wasmJsBrowserDistribution
if errorlevel 1 (
    echo Build failed. Check the gradle output above.
    exit /b 1
)
cd /d "%~dp0"
echo.
echo Serving Compose web app at http://localhost:3000
npx --yes http-server "app-kmp\composeApp\build\dist\wasmJs\productionExecutable" -p 3000 -c-1
