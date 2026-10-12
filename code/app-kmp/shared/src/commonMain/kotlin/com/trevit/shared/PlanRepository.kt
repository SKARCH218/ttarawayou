package com.trevit.shared

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** 플랜 생성 API 클라이언트. 엔진은 각 플랫폼 의존성(okhttp/darwin)에서 자동 선택된다. */
class PlanRepository(
    private val client: HttpClient = defaultHttpClient(),
) {
    /**
     * POST {baseUrl}/api/plan
     * AI 플래닝이 오래 걸릴 수 있어 타임아웃을 넉넉히 잡는다.
     */
    suspend fun createPlan(baseUrl: String, request: PlanRequest, authToken: String? = null): PlanResponse {
        val url = baseUrl.trimEnd('/') + "/api/plan"
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            // 로그인했으면 토큰을 붙인다 — 서버가 "누가 플랜을 만들었는지" 지표에 남긴다 (없어도 플랜은 만들어진다)
            authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            // 백엔드는 400에 {"message":"보유 토큰이 부족합니다 …"} 같은 이유를 준다. 못 읽으면 상태코드로 대체
            val message = runCatching { response.body<ApiErrorDto>().message }.getOrNull()
            throw PlanApiException(message ?: "서버 오류 (${response.status.value})")
        }
        return response.body()
    }

    /**
     * POST {baseUrl}/api/events — 앱에서만 알 수 있는 행동(여정 시작·하루 완주) 기록.
     * 지표용이라 실패해도 사용자 흐름을 막지 않는다 (예외를 삼킨다).
     */
    suspend fun sendEvent(baseUrl: String, type: String, authToken: String?) {
        runCatching {
            client.post(baseUrl.trimEnd('/') + "/api/events") {
                contentType(ContentType.Application.Json)
                authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(EventRequest(type))
            }
        }
    }

    /** GET {baseUrl}/api/wallet — 보유 토큰 조회 (웹 설정 화면의 "보유 N 토큰"과 같은 값) */
    suspend fun fetchWallet(baseUrl: String): Long =
        client.get(baseUrl.trimEnd('/') + "/api/wallet").body<WalletDto>().balance

    /** POST {baseUrl}/api/wallet/reset — 테스트용 토큰 충전 */
    suspend fun resetWallet(baseUrl: String): Long =
        client.post(baseUrl.trimEnd('/') + "/api/wallet/reset").body<WalletDto>().balance

    /** GET {baseUrl}/api/wallet/products — 구매 가능한 토큰 상품 목록 */
    suspend fun fetchWalletProducts(baseUrl: String): List<WalletProductDto> =
        client.get(baseUrl.trimEnd('/') + "/api/wallet/products").body()

    /** POST {baseUrl}/api/wallet/purchase — 상품 구매(결제 시뮬레이션), 새 잔액을 돌려준다 */
    suspend fun purchaseWallet(baseUrl: String, productId: String): Long {
        val response = client.post(baseUrl.trimEnd('/') + "/api/wallet/purchase") {
            contentType(ContentType.Application.Json)
            setBody(PurchaseRequest(productId))
        }
        if (!response.status.isSuccess()) {
            throw PlanApiException("구매에 실패했어요 (${response.status.value})")
        }
        return response.body<WalletDto>().balance
    }

    companion object {
        fun defaultHttpClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 180_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 180_000
            }
        }
    }
}

class PlanApiException(message: String) : Exception(message)

@kotlinx.serialization.Serializable
data class EventRequest(val type: String)
