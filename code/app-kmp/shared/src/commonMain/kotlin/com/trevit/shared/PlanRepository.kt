package com.trevit.shared

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
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
    suspend fun createPlan(baseUrl: String, request: PlanRequest): PlanResponse {
        val url = baseUrl.trimEnd('/') + "/api/plan"
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            throw PlanApiException("서버 오류 (${response.status.value})")
        }
        return response.body()
    }

    /** GET {baseUrl}/api/wallet — 보유 토큰 조회 (웹 설정 화면의 "보유 N 토큰"과 같은 값) */
    suspend fun fetchWallet(baseUrl: String): Long =
        client.get(baseUrl.trimEnd('/') + "/api/wallet").body<WalletDto>().balance

    /** POST {baseUrl}/api/wallet/reset — 테스트용 토큰 충전 */
    suspend fun resetWallet(baseUrl: String): Long =
        client.post(baseUrl.trimEnd('/') + "/api/wallet/reset").body<WalletDto>().balance

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
