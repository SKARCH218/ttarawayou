package com.trevit.controller

import com.trevit.dto.AuthDtos.AuthConfigResponse
import com.trevit.dto.AuthDtos.AuthResponse
import com.trevit.dto.AuthDtos.GoogleLoginRequest
import com.trevit.dto.AuthDtos.LoginRequest
import com.trevit.dto.AuthDtos.SendCodeRequest
import com.trevit.dto.AuthDtos.SignupRequest
import com.trevit.dto.AuthDtos.UserResponse
import com.trevit.dto.AuthDtos.VerifyCodeRequest
import com.trevit.service.AuthService
import com.trevit.service.EmailVerificationService
import com.trevit.service.GoogleAuthService
import com.trevit.service.UnauthorizedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val emailVerification: EmailVerificationService,
    private val google: GoogleAuthService,
) {

    /** GET /api/auth/config — 구글 로그인 버튼을 그릴지 프론트가 판단한다 */
    @GetMapping("/config")
    fun config(): AuthConfigResponse = AuthConfigResponse(googleClientId = google.clientId)

    /** POST /api/auth/email/send — 가입용 인증코드 발송. 코드는 응답에 담지 않는다 */
    @PostMapping("/email/send")
    fun sendCode(@RequestBody request: SendCodeRequest): Map<String, Long> =
        mapOf("expiresInSeconds" to emailVerification.sendCode(request.email))

    /** POST /api/auth/email/verify — 인증코드 확인 */
    @PostMapping("/email/verify")
    fun verifyCode(@RequestBody request: VerifyCodeRequest): Map<String, Boolean> {
        emailVerification.verifyCode(request.email, request.code)
        return mapOf("verified" to true)
    }

    /** POST /api/auth/signup — 메일 인증을 마친 이메일만 가입된다 */
    @PostMapping("/signup")
    fun signup(@RequestBody request: SignupRequest): AuthResponse = authService.signup(request)

    /** POST /api/auth/google — 구글 ID 토큰으로 로그인(첫 로그인이면 가입까지) */
    @PostMapping("/google")
    fun google(@RequestBody request: GoogleLoginRequest): AuthResponse =
        authService.loginWithGoogle(request.credential)

    /** POST /api/auth/login */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): AuthResponse = authService.login(request)

    /** GET /api/auth/me — 저장된 토큰이 아직 유효한지 확인 */
    @GetMapping("/me")
    fun me(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?): UserResponse =
        authService.requireUser(authorization)

    /** POST /api/auth/logout — 현재 토큰만 폐기 */
    @PostMapping("/logout")
    fun logout(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?): Map<String, Boolean> {
        authService.logout(authorization)
        return mapOf("ok" to true)
    }

    /** 입력값 오류 → 400 + 사용자에게 그대로 보여줄 메시지 */
    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "잘못된 요청")))

    /** 인증 실패 → 401 */
    @ExceptionHandler(UnauthorizedException::class)
    fun unauthorized(e: UnauthorizedException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to (e.message ?: "로그인이 필요해요.")))
}
