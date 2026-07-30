# 따라와유 백엔드 (Kotlin + Spring Boot) — 멀티스테이지 빌드
# 빌드 컨텍스트: code/ 루트 (backend-kotlin + frontend/public 포함 필요)
#   docker build -t ttarawayou .
#   docker run -e PORT=8080 -p 8080:8080 ttarawayou

# ---------- 1단계: Gradle 빌드 ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 정적 웹(processResources에서 static/으로 복사됨)
COPY frontend/public/ frontend/public/
COPY backend-kotlin/ backend-kotlin/

WORKDIR /workspace/backend-kotlin
RUN ./gradlew --no-daemon bootJar

# ---------- 2단계: 경량 JRE 런타임 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend-kotlin/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080
# PORT 환경변수 지원 (기본 8080)
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
