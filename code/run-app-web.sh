#!/bin/zsh
# Travit - Compose Multiplatform 웹 앱 빌드 후 3000 포트로 서빙 (앱과 같은 화면)

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR/app-kmp" || exit 1
./gradlew :composeApp:wasmJsBrowserDistribution || exit 1
cd "$DIR"
echo "Serving Compose web app at http://localhost:3000"
npx --yes http-server "app-kmp/composeApp/build/dist/wasmJs/productionExecutable" -p 3000 -c-1
