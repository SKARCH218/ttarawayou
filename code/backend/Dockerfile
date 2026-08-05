# 빌드 컨텍스트는 code/ (render.yaml의 dockerContext 참고)
# ── 1단계: Gradle 빌드 ──────────────────────────────────
FROM gradle:8.14-jdk21 AS build
WORKDIR /workspace
COPY backend/ backend/
# 웹은 Compose Multiplatform(wasm) 앱으로 대체됨 — 미리 빌드해 뒀다면 함께 포함
# (없으면 API 전용으로 동작. 빌드: code/app-kmp 에서 :composeApp:wasmJsBrowserDistribution)
COPY app-kmp/composeApp/build/dist/wasmJs/productionExecutabl[e] app-kmp/composeApp/build/dist/wasmJs/productionExecutable/
RUN cd backend && gradle bootJar --no-daemon -x test \
    && cp build/libs/*-SNAPSHOT.jar /workspace/app.jar

# ── 2단계: 경량 런타임 ──────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
ENV LMSTUDIO_ENABLED=false
EXPOSE 8080
CMD ["sh", "-c", "java -Xmx400m -jar app.jar --server.port=${PORT:-8080}"]
