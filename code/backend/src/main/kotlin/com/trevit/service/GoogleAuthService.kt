package com.trevit.service

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

/**
 * 구글 로그인 — 브라우저(Google Identity Services)가 준 ID 토큰을 서버가 검증한다.
 * 클라이언트 시크릿이 필요 없도록 tokeninfo 엔드포인트로 확인하고,
 * aud(우리 클라이언트 ID)와 이메일 인증 여부까지 직접 대조한다.
 * GOOGLE_CLIENT_ID 가 없으면 기능 자체를 끈다 (프론트에도 버튼이 뜨지 않는다).
 */
@Service
class GoogleAuthService(
    @Value("\${google.client-id:}") clientId: String?,
) {
    private val log = LoggerFactory.getLogger(GoogleAuthService::class.java)

    val clientId: String = clientId?.trim().orEmpty()

    private val http: RestClient = RestClient.builder()
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(3000)
            setReadTimeout(3000)
        })
        .build()

    data class GoogleUser(val sub: String, val email: String, val name: String?)

    fun enabled(): Boolean = clientId.isNotEmpty()

    /** ID 토큰을 검증하고 구글 계정 정보를 돌려준다. 검증 실패는 401로 이어진다 */
    fun verify(credential: String): GoogleUser {
        if (!enabled()) throw IllegalArgumentException("구글 로그인이 설정되지 않았어요.")
        val token = credential.trim()
        if (token.isEmpty()) throw UnauthorizedException("구글 로그인에 실패했어요.")

        val body: JsonNode = try {
            http.get()
                .uri("https://oauth2.googleapis.com/tokeninfo?id_token={t}", token)
                .retrieve()
                .body(JsonNode::class.java)
                ?: throw UnauthorizedException("구글 로그인에 실패했어요.")
        } catch (e: UnauthorizedException) {
            throw e
        } catch (e: Exception) {
            log.warn("구글 토큰 검증 실패: {}", e.message)
            throw UnauthorizedException("구글 인증을 확인하지 못했어요. 잠시 후 다시 시도해 주세요.")
        }

        // 이 토큰이 정말 우리 앱을 위해 발급됐는지 (aud) — 빠뜨리면 다른 앱 토큰도 통과한다
        val aud = body.path("aud").asText("")
        if (aud != clientId) {
            log.warn("구글 토큰의 aud 불일치")
            throw UnauthorizedException("구글 로그인에 실패했어요.")
        }
        val issuer = body.path("iss").asText("")
        if (issuer != "accounts.google.com" && issuer != "https://accounts.google.com") {
            throw UnauthorizedException("구글 로그인에 실패했어요.")
        }
        if (body.path("email_verified").asText("") != "true") {
            throw UnauthorizedException("구글에서 이메일 인증이 되지 않은 계정이에요.")
        }

        val sub = body.path("sub").asText("")
        val email = body.path("email").asText("").lowercase()
        if (sub.isEmpty() || email.isEmpty()) throw UnauthorizedException("구글 계정 정보를 읽지 못했어요.")

        return GoogleUser(sub = sub, email = email, name = body.path("name").asText(null))
    }
}
