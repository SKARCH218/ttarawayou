# 빌드 컨텍스트는 code/ (render.yaml의 dockerContext 참고)
# ── 1단계: Gradle 빌드 ──────────────────────────────────
FROM gradle:8.14-jdk21 AS build
WORKDIR /workspace
COPY backend-kotlin/ backend-kotlin/
COPY frontend/public/ frontend/public/
RUN cd backend-kotlin && gradle bootJar --no-daemon -x test \
    && cp build/libs/ttarawayou-api-0.0.1-SNAPSHOT.jar /workspace/app.jar

# ── 2단계: 경량 런타임 ──────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
ENV LMSTUDIO_ENABLED=false
EXPOSE 8080
CMD ["sh", "-c", "java -Xmx400m -jar app.jar --server.port=${PORT:-8080}"]
