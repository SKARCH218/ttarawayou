plugins {
    // JDK 21 toolchain 자동 다운로드 (로컬에 없을 때 foojay에서 받는다)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ttarawayou-api"
