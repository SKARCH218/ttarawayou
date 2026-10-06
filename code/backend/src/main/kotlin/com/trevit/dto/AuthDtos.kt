package com.trevit.dto

object AuthDtos {

    /** 회원가입 요청 */
    data class SignupRequest(
        val email: String = "",
        val password: String = "",
        val passwordConfirm: String? = null,
        val nickname: String = "",
        /** 메일 인증이 막혔을 때의 임시 가입 경로. 서버 INVITE_CODE 와 같으면 메일 인증을 건너뛴다 */
        val inviteCode: String? = null,
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
        /** LOCAL(이메일 가입) | GOOGLE — 앱 설정에서 비밀번호 변경 가능 여부를 판단한다 */
        val provider: String = "LOCAL",
    )

    /** 비밀번호 변경 요청 (설정 화면) */
    data class ChangePasswordRequest(
        val currentPassword: String = "",
        val newPassword: String = "",
    )

    /** 닉네임 변경 요청 (설정 화면) */
    data class ChangeNicknameRequest(val nickname: String = "")

    /** 회원 탈퇴 요청 — 이메일 가입자는 비밀번호 확인 필요 */
    data class WithdrawRequest(val password: String? = null)

    /** 로그인·회원가입 성공 응답 */
    data class AuthResponse(
        val token: String,
        val user: UserResponse,
    )
}
