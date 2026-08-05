package com.trevit.dto

object AuthDtos {

    /** 회원가입 요청 */
    data class SignupRequest(
        val email: String = "",
        val password: String = "",
        val passwordConfirm: String? = null,
        val nickname: String = "",
    )

    /** 로그인 요청 */
    data class LoginRequest(
        val email: String = "",
        val password: String = "",
    )

    /** 인증코드 발송 요청 */
    data class SendCodeRequest(val email: String = "")

    /** 인증코드 확인 요청 */
    data class VerifyCodeRequest(val email: String = "", val code: String = "")

    /** 구글 로그인 — GIS가 브라우저에 준 ID 토큰 */
    data class GoogleLoginRequest(val credential: String = "")

    /** 프론트가 어떤 로그인 수단을 그릴지 판단하는 데 쓴다 */
    data class AuthConfigResponse(val googleClientId: String)

    /** 회원 정보 (비밀번호 관련 값은 절대 담지 않는다) */
    data class UserResponse(
        val id: Long,
        val email: String,
        val nickname: String,
    )

    /** 로그인·회원가입 성공 응답 */
    data class AuthResponse(
        val token: String,
        val user: UserResponse,
    )
}
