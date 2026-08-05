import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    kotlin("plugin.jpa") version "2.2.20"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.trevit"
version = "0.0.1-SNAPSHOT"
description = "따라와유 — 예산 기반 미스터리 여행 플랜 API"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // 비밀번호 BCrypt 해싱만 사용 (Security 필터체인·자동설정은 쓰지 않는다)
    implementation("org.springframework.security:spring-security-crypto")
    // 회원가입 메일 인증 — SMTP가 설정돼 있을 때만 실제로 발송한다
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

// Compose 웹 빌드 결과물이 있으면 jar의 static/ 으로 포함 → 루트(/)에서 서빙
// (먼저 code/app-kmp 에서 :composeApp:wasmJsBrowserDistribution 을 빌드해 둘 것)
tasks.processResources {
    from("../app-kmp/composeApp/build/dist/wasmJs/productionExecutable") {
        into("static")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}