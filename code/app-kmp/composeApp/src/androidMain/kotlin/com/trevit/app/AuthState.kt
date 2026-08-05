package com.trevit.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.trevit.shared.AuthRepository
import com.trevit.shared.LoginRequest
import com.trevit.shared.SignupRequest
import com.trevit.shared.UserDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 로그인·회원가입 상태 (웹 `js/auth.js` + `common.js` 의 인증 부분과 같은 규칙).
 *
 * 검증 기준은 백엔드 AuthService·EmailVerificationService 와 맞춰 두고,
 * 최종 판단은 항상 서버가 한다 — 여기서는 사용자가 헛걸음하지 않도록 미리 걸러줄 뿐이다.
 */
class AuthState(
    private val baseUrlProvider: () -> String,
    initialToken: String?,
    private val onTokenSaved: (String?) -> Unit,
) {
    private val repository = AuthRepository()

    /** 저장된 로그인 토큰. null이면 로그인 화면부터 시작한다 */
    var token by mutableStateOf(initialToken)
        private set

    var user by mutableStateOf<UserDto?>(null)
        private set

    val isLoggedIn: Boolean get() = token != null

    // ---- 로그인 폼 ----
    var loginEmail by mutableStateOf("")
    var loginPassword by mutableStateOf("")

    // ---- 회원가입 폼 ----
    var signupEmail by mutableStateOf("")
    var signupNickname by mutableStateOf("")
    var signupPassword by mutableStateOf("")
    var signupPasswordConfirm by mutableStateOf("")
    var signupCode by mutableStateOf("")
    var agreedToTerms by mutableStateOf(false)

    /** 인증코드를 보낸 뒤에만 코드 입력칸이 나타난다 */
    var codeSent by mutableStateOf(false)
        private set

    /** 인증을 마친 주소. 이메일을 고치면 처음으로 되돌린다 */
    var verifiedEmail by mutableStateOf<String?>(null)
        private set

    val emailVerified: Boolean
        get() = verifiedEmail != null && verifiedEmail == signupEmail.trim().lowercase()

    /** 인증코드 남은 시간(초) — 0이면 만료 */
    var codeSecondsLeft by mutableIntStateOf(0)
        private set

    /** 재발송까지 남은 시간(초) */
    var resendSecondsLeft by mutableIntStateOf(0)
        private set

    var busy by mutableStateOf(false)
        private set

    /** 화면 하단 오류 상자 (웹 `.error-box`) */
    var errorMessage by mutableStateOf<String?>(null)

    /** 인증코드 입력칸 아래 안내 문구 */
    var codeMessage by mutableStateOf<String?>(null)
        private set

    var codeMessageIsError by mutableStateOf(false)
        private set

    // ─────────────────────────────────────────────
    // 세션
    // ─────────────────────────────────────────────

    /** 앱을 켤 때 저장된 토큰이 아직 살아 있는지 확인한다 */
    suspend fun restoreSession(): Boolean {
        val saved = token ?: return false
        return try {
            val me = withContext(Dispatchers.IO) { repository.me(baseUrlProvider(), saved) }
            if (me == null) {
                clearSession()
                false
            } else {
                user = me
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 서버에 못 닿는 상황에서 토큰까지 지우면 오프라인일 때 로그아웃돼 버린다
            true
        }
    }

    suspend fun logout() {
        val saved = token
        if (saved != null) {
            withContext(Dispatchers.IO) { repository.logout(baseUrlProvider(), saved) }
        }
        clearSession()
    }

    private fun clearSession() {
        token = null
        user = null
        onTokenSaved(null)
    }

    private fun saveSession(newToken: String, newUser: UserDto) {
        token = newToken
        user = newUser
        onTokenSaved(newToken)
        resetForms()
    }

    // ─────────────────────────────────────────────
    // 로그인
    // ─────────────────────────────────────────────

    /** 성공하면 true. 실패 사유는 [errorMessage] 에 담긴다 */
    suspend fun login(): Boolean {
        val email = loginEmail.trim()
        val password = loginPassword
        if (email.isEmpty() || password.isEmpty()) {
            errorMessage = "이메일과 비밀번호를 모두 입력해 주세요."
            return false
        }
        return run("로그인") {
            val auth = repository.login(baseUrlProvider(), LoginRequest(email, password))
            saveSession(auth.token, auth.user)
        }
    }

    // ─────────────────────────────────────────────
    // 메일 인증
    // ─────────────────────────────────────────────

    suspend fun sendCode(): Boolean {
        val email = signupEmail.trim()
        if (!EMAIL_REGEX.matches(email)) {
            errorMessage = "이메일 형식이 올바르지 않아요."
            return false
        }
        return run("인증코드 발송") {
            val seconds = repository.sendCode(baseUrlProvider(), email)
            codeSent = true
            codeSecondsLeft = seconds.toInt()
            resendSecondsLeft = RESEND_COOLDOWN_SECONDS
            signupCode = ""
            setCodeMessage("메일로 받은 6자리 숫자를 입력해 주세요", isError = false)
        }
    }

    suspend fun verifyCode(): Boolean {
        val email = signupEmail.trim()
        if (signupCode.length != 6) {
            setCodeMessage("인증번호 6자리를 입력해 주세요", isError = true)
            return false
        }
        return try {
            busy = true
            withContext(Dispatchers.IO) { repository.verifyCode(baseUrlProvider(), email, signupCode) }
            verifiedEmail = email.lowercase()
            codeSecondsLeft = 0
            resendSecondsLeft = 0
            setCodeMessage("이메일 인증이 끝났어요", isError = false)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 코드 오류는 화면 하단이 아니라 입력칸 바로 아래에 보여준다
            setCodeMessage(e.message ?: "인증번호를 확인하지 못했어요", isError = true)
            false
        } finally {
            busy = false
        }
    }

    /** 이메일을 고치면 인증을 처음부터 다시 받아야 한다 */
    fun onSignupEmailChanged(value: String) {
        signupEmail = value
        if (verifiedEmail != null && verifiedEmail != value.trim().lowercase()) {
            verifiedEmail = null
            codeSent = false
            signupCode = ""
            codeSecondsLeft = 0
            resendSecondsLeft = 0
            setCodeMessage(null, isError = false)
        }
    }

    /** 1초마다 화면에서 호출 — 남은 시간 카운트다운 */
    fun tickTimers() {
        if (codeSecondsLeft > 0) {
            codeSecondsLeft--
            if (codeSecondsLeft == 0 && verifiedEmail == null) {
                setCodeMessage("인증번호가 만료됐어요. 다시 받아 주세요.", isError = true)
            }
        }
        if (resendSecondsLeft > 0) resendSecondsLeft--
    }

    // ─────────────────────────────────────────────
    // 회원가입
    // ─────────────────────────────────────────────

    suspend fun signup(): Boolean {
        val email = signupEmail.trim()
        val nickname = signupNickname.trim()

        val problem = when {
            !EMAIL_REGEX.matches(email) -> "이메일 형식이 올바르지 않아요."
            !emailVerified -> "이메일 인증을 먼저 해주세요."
            nickname.length !in 2..12 -> "닉네임은 2~12자로 입력해 주세요."
            !isPasswordStrong(signupPassword) -> "비밀번호는 영문과 숫자를 섞어 8자 이상이어야 해요."
            signupPassword != signupPasswordConfirm -> "비밀번호가 서로 달라요."
            !agreedToTerms -> "약관에 동의해야 가입할 수 있어요."
            else -> null
        }
        if (problem != null) {
            errorMessage = problem
            return false
        }

        return run("회원가입") {
            val auth = repository.signup(
                baseUrlProvider(),
                SignupRequest(
                    email = email,
                    password = signupPassword,
                    passwordConfirm = signupPasswordConfirm,
                    nickname = nickname,
                ),
            )
            saveSession(auth.token, auth.user)
        }
    }

    // ─────────────────────────────────────────────

    /** 네트워크 호출 공통 처리 — busy 토글과 오류 메시지를 한곳에서 다룬다 */
    private suspend fun run(what: String, block: suspend () -> Unit): Boolean = try {
        busy = true
        errorMessage = null
        withContext(Dispatchers.IO) { block() }
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        errorMessage = e.message ?: "${what}에 실패했어요 — 서버 연결을 확인해 주세요."
        false
    } finally {
        busy = false
    }

    private fun setCodeMessage(message: String?, isError: Boolean) {
        codeMessage = message
        codeMessageIsError = isError
    }

    private fun resetForms() {
        loginEmail = ""
        loginPassword = ""
        signupEmail = ""
        signupNickname = ""
        signupPassword = ""
        signupPasswordConfirm = ""
        signupCode = ""
        agreedToTerms = false
        codeSent = false
        verifiedEmail = null
        codeSecondsLeft = 0
        resendSecondsLeft = 0
        errorMessage = null
        setCodeMessage(null, isError = false)
    }

    companion object {
        /** 백엔드 EmailVerification.RESEND_COOLDOWN_SECONDS 와 동일 */
        const val RESEND_COOLDOWN_SECONDS = 60

        val EMAIL_REGEX = Regex("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")

        fun isPasswordStrong(value: String): Boolean =
            value.length >= 8 && value.any { it.isDigit() } && value.any { it.isLetter() }

        /** 비밀번호 강도 0~3 (웹 `.pw-meter` 와 같은 기준) */
        fun passwordScore(value: String): Int {
            if (value.isEmpty()) return 0
            var score = 0
            if (value.length >= 8) score++
            if (value.any { it.isLetter() }) score++
            if (value.any { it.isDigit() }) score++
            return score
        }
    }
}
