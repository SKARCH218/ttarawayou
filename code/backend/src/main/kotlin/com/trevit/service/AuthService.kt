package com.trevit.service

import com.trevit.dto.AuthDtos.AuthResponse
import com.trevit.dto.AuthDtos.LoginRequest
import com.trevit.dto.AuthDtos.SignupRequest
import com.trevit.dto.AuthDtos.UserResponse
import com.trevit.entity.AuthToken
import com.trevit.entity.User
import com.trevit.repository.AuthTokenRepository
import com.trevit.repository.UserRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.HexFormat

/** 인증 실패(토큰 없음·만료·비밀번호 불일치) → 401 */
class UnauthorizedException(message: String) : RuntimeException(message)

/**
 * 이메일 + 비밀번호 회원가입/로그인.
 * 비밀번호는 BCrypt로 해싱해 저장하고, 로그인하면 랜덤 토큰을 발급한다.
 * (외부 인증 서버 없이 단일 jar로 굴러가야 해서 Spring Security 필터 대신
 *  spring-security-crypto의 해시 + 자체 토큰 테이블만 쓴다)
 */
@Service
class AuthService(
    private val users: UserRepository,
    private val tokens: AuthTokenRepository,
    private val emailVerification: EmailVerificationService,
    private val google: GoogleAuthService,
) {
    private val encoder = BCryptPasswordEncoder()
    private val random = SecureRandom()

    private val emailRegex = EmailVerificationService.EMAIL_REGEX

    @Transactional
    fun signup(req: SignupRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        val nickname = req.nickname.trim()

        require(email.isNotEmpty()) { "이메일을 입력해 주세요." }
        require(emailRegex.matches(email)) { "이메일 형식이 올바르지 않아요." }
        require(nickname.length in 2..12) { "닉네임은 2~12자로 입력해 주세요." }
        validatePassword(req.password)
        require(req.passwordConfirm == null || req.passwordConfirm == req.password) {
            "비밀번호가 서로 달라요."
        }
        require(!users.existsByEmail(email)) { "이미 가입된 이메일이에요." }
        require(!users.existsByNickname(nickname)) { "이미 사용 중인 닉네임이에요." }

        // 메일 인증을 마친 이메일만 가입시킨다 (기록은 여기서 소비된다)
        emailVerification.consumeVerified(email)

        val user = users.save(
            User(
                email = email,
                passwordHash = encoder.encode(req.password),
                nickname = nickname,
                provider = User.Provider.LOCAL,
            )
        )
        return AuthResponse(issueToken(user), user.toResponse())
    }

    /**
     * 구글 로그인 — 처음이면 가입까지 한 번에 처리한다.
     * 같은 이메일로 이미 이메일·비밀번호 가입을 했다면 그 계정에 구글을 연결해 준다.
     */
    @Transactional
    fun loginWithGoogle(credential: String): AuthResponse {
        val profile = google.verify(credential)

        val existing = users.findByGoogleSub(profile.sub) ?: users.findByEmail(profile.email)
        val user = if (existing != null) {
            if (existing.googleSub == null) {
                existing.googleSub = profile.sub
                users.save(existing)
            }
            existing
        } else {
            users.save(
                User(
                    email = profile.email,
                    passwordHash = null,
                    nickname = availableNickname(profile.name, profile.email),
                    provider = User.Provider.GOOGLE,
                    googleSub = profile.sub,
                )
            )
        }
        return AuthResponse(issueToken(user), user.toResponse())
    }

    /** 구글 이름이나 이메일 앞부분으로 닉네임을 만들되, 겹치면 숫자를 붙인다 */
    private fun availableNickname(name: String?, email: String): String {
        val base = (name?.trim()?.takeIf { it.isNotEmpty() } ?: email.substringBefore('@'))
            .take(10)
            .ifEmpty { "여행자" }
        if (!users.existsByNickname(base)) return base
        for (i in 2..9999) {
            val candidate = "$base$i"
            if (!users.existsByNickname(candidate)) return candidate
        }
        // 여기까지 오는 일은 사실상 없지만, 실패보다는 임의값이 낫다
        return "여행자${random.nextInt(1_000_000)}"
    }

    @Transactional
    fun login(req: LoginRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        val user = users.findByEmail(email)
        // 가입 여부를 알려주지 않기 위해 아이디·비밀번호 오류 메시지를 하나로 합친다
        if (user == null || !encoder.matches(req.password, user.passwordHash)) {
            throw UnauthorizedException("이메일 또는 비밀번호가 올바르지 않아요.")
        }
        return AuthResponse(issueToken(user), user.toResponse())
    }

    /** Authorization 헤더의 토큰으로 회원을 찾는다. 실패하면 401 */
    @Transactional(readOnly = true)
    fun requireUser(authorization: String?): UserResponse {
        val raw = authorization?.removePrefix("Bearer ")?.trim().orEmpty()
        if (raw.isEmpty()) throw UnauthorizedException("로그인이 필요해요.")

        val token = tokens.findById(raw).orElse(null)
            ?: throw UnauthorizedException("로그인이 필요해요.")
        if (token.isExpired()) throw UnauthorizedException("로그인이 만료됐어요. 다시 로그인해 주세요.")

        val user = users.findById(token.userId).orElse(null)
            ?: throw UnauthorizedException("탈퇴했거나 없는 계정이에요.")
        return user.toResponse()
    }

    /** 해당 토큰만 폐기 — 다른 기기의 로그인은 유지된다 */
    @Transactional
    fun logout(authorization: String?) {
        val raw = authorization?.removePrefix("Bearer ")?.trim().orEmpty()
        if (raw.isNotEmpty()) tokens.deleteById(raw)
    }

    private fun validatePassword(password: String) {
        require(password.length >= 8) { "비밀번호는 8자 이상이어야 해요." }
        require(password.length <= 64) { "비밀번호는 64자 이하로 입력해 주세요." }
        require(password.any { it.isDigit() }) { "비밀번호에 숫자를 하나 이상 넣어 주세요." }
        require(password.any { it.isLetter() }) { "비밀번호에 영문자를 하나 이상 넣어 주세요." }
    }

    private fun issueToken(user: User): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        val value = HexFormat.of().formatHex(bytes)
        tokens.save(
            AuthToken(
                token = value,
                userId = user.id!!,
                expiresAt = Instant.now().plus(AuthToken.TTL_DAYS, ChronoUnit.DAYS),
            )
        )
        return value
    }

    private fun User.toResponse() = UserResponse(id = id!!, email = email, nickname = nickname)
}
