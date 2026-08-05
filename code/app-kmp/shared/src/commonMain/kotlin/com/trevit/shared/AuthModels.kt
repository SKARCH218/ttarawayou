package com.trevit.shared

import kotlinx.serialization.Serializable

/**
 * 백엔드 AuthDtos.kt 와 1:1 대응하는 직렬화 모델.
 * (backend/src/main/kotlin/com/trevit/dto/AuthDtos.kt)
 */

/** POST /api/auth/signup — 메일 인증을 마친 이메일만 통과한다 */
@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val passwordConfirm: String? = null,
    val nickname: String,
)

/** POST /api/auth/login */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

/** POST /api/auth/email/send */
@Serializable
data class SendCodeRequest(val email: String)

/** POST /api/auth/email/verify */
@Serializable
data class VerifyCodeRequest(val email: String, val code: String)

/** 회원 정보 — 비밀번호 관련 값은 담기지 않는다 */
@Serializable
data class UserDto(
    val id: Long,
    val email: String,
    val nickname: String,
)

/** 로그인·회원가입 성공 응답 */
@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto,
)

/** POST /api/auth/email/send 응답 */
@Serializable
data class SendCodeResponse(val expiresInSeconds: Long = 0)

/** 실패 응답 — 사용자에게 그대로 보여줄 한글 문구가 담긴다 */
@Serializable
data class ApiErrorDto(val message: String? = null)
