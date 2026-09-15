package com.trevit.shared

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header

/**
 * OSM 래스터 타일 다운로드 — 앱 Journey 화면의 지도 렌더링용 (키 불필요).
 *
 * OSM 기본 지도는 상점·POI 아이콘이 많아 다소 복잡하지만, 화면에서 그 위에 옅은 반투명 막을
 * 씌워 배경을 가라앉히므로(간략화) 경로·정류장 마커가 잘 보인다.
 * 엔진은 각 플랫폼 의존성(okhttp/darwin/js)에서 자동 선택된다.
 */
object TileFetcher {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 8_000
        }
    }

    suspend fun fetch(z: Int, x: Int, y: Int, dark: Boolean = false): ByteArray =
        client.get("https://tile.openstreetmap.org/$z/$x/$y.png") {
            // OSM 타일 정책상 식별 가능한 User-Agent가 필요하다
            header("User-Agent", "Travit/1.0 (mystery travel app; travit.p-e.kr)")
        }.body()
}
