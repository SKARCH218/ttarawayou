package com.trevit.shared

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * 회원가입·로그인 API 클라이언트 (웹 `js/auth.js` 와 같은 엔드포인트를 쓴다).
 * 실패하면 서버가 준 한글 메시지를 그대로 담아 [AuthApiException] 을 던진다.
 */
class AuthRepository(
    private val client: HttpClient = PlanRepository.defaultHttpClient(),
) {
    /** 인증코드 발송 → 코드 유효시간(초) */
    suspend fun sendCode(baseUrl: String, email: String): Long =
        client.post(url(baseUrl, "/api/auth/email/send")) {
            contentType(ContentType.Application.Json)
            setBody(SendCodeRequest(email))
        }.require<SendCodeResponse>().expiresInSeconds

    /** 인증코드 확인. 실패하면 예외 메시지에 남은 시도 횟수가 담겨 온다 */
    suspend fun verifyCode(baseUrl: String, email: String, code: String) {
        client.post(url(baseUrl, "/api/auth/email/verify")) {
            contentType(ContentType.Application.Json)
            setBody(VerifyCodeRequest(email, code))
        }.requireSuccess()
    }

    suspend fun signup(baseUrl: String, request: SignupRequest): AuthResponse =
        client.post(url(baseUrl, "/api/auth/signup")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.require()

    suspend fun login(baseUrl: String, request: LoginRequest): AuthResponse =
        client.post(url(baseUrl, "/api/auth/login")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.require()

    /** 저장된 토큰이 아직 유효한지 확인. 만료·폐기됐으면 null */
    suspend fun me(baseUrl: String, token: String): UserDto? {
        val response = client.get(url(baseUrl, "/api/auth/me")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    /** 현재 토큰만 폐기 — 실패해도 로컬 로그아웃은 진행한다 */
    suspend fun logout(baseUrl: String, token: String) {
        runCatching {
            client.post(url(baseUrl, "/api/auth/logout")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    /** 비밀번호 변경 — 현재 비밀번호가 틀리면 서버가 거절한다 */
    suspend fun changePassword(baseUrl: String, token: String, current: String, new: String) {
        client.post(url(baseUrl, "/api/auth/password")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(current, new))
        }.requireSuccess()
    }

    /** 닉네임 변경 → 바뀐 회원 정보 */
    suspend fun changeNickname(baseUrl: String, token: String, nickname: String): UserDto =
        client.post(url(baseUrl, "/api/auth/nickname")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(ChangeNicknameRequest(nickname))
        }.require()

    /** 회원 탈퇴 — 계정과 로그인 세션이 모두 삭제된다 */
    suspend fun deleteAccount(baseUrl: String, token: String, password: String?) {
        client.post(url(baseUrl, "/api/auth/withdraw")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(WithdrawRequest(password))
        }.requireSuccess()
    }

    private fun url(baseUrl: String, path: String) = baseUrl.trimEnd('/') + path

    private suspend inline fun <reified T> HttpResponse.require(): T {
        requireSuccess()
        return body()
    }

    private suspend fun HttpResponse.requireSuccess() {
        if (status.isSuccess()) return
        // 백엔드는 400/401에 {"message":"..."} 를 준다. 못 읽으면 상태코드로 대체한다
        val message = runCatching { body<ApiErrorDto>().message }.getOrNull()
        throw AuthApiException(message ?: "요청을 처리하지 못했어요. (${status.value})")
    }
}

class AuthApiException(message: String) : Exception(message)
