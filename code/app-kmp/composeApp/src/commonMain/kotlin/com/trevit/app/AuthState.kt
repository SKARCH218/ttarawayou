package com.trevit.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.trevit.app.i18n.AppLanguage
import com.trevit.app.i18n.serverMessage
import com.trevit.app.i18n.translate
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
    /** 현재 화면 언어 — 오류 문구 번역용 */
    private val languageProvider: () -> AppLanguage = { AppLanguage.KO },
) {
    private val repository = AuthRepository()

    /** 상태 클래스 안에서 쓰는 번역 */
    private fun t(key: String, vararg args: Any?): String = translate(languageProvider(), key, *args)

    /** 서버가 준 한국어 문구를 현재 언어로 (모르는 문구는 그대로) */
    private fun srv(message: String?): String? = serverMessage(languageProvider(), message)

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

    /** 메일 인증이 막혔을 때 쓰는 임시 가입 초대코드 — 입력하면 이메일 인증 없이 가입된다 */
    var signupInviteCode by mutableStateOf("")
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

    /**
     * [sendCode] 가 실제로 서버에 요청을 보내고 있는 동안만 true.
     * "인증요청" 버튼이 눌러도 텍스트가 안 바뀌어 멈춘 것처럼 보인다는 신고가 있어 추가했다 —
     * [busy] 는 로그인·가입·인증확인까지 공유하는 값이라 이 버튼만 콕 집어 "발송 중…"을 보여줄 수 없었다.
     */
    var sendingCode by mutableStateOf(false)
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
            val me = withContext(Dispatchers.Default) { repository.me(baseUrlProvider(), saved) }
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
            withContext(Dispatchers.Default) { repository.logout(baseUrlProvider(), saved) }
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
            errorMessage = t("auth.err.loginEmpty")
            return false
        }
        return run(t("auth.action.login")) {
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
            errorMessage = t("auth.err.emailFormat")
            return false
        }
        sendingCode = true
        try {
            return run(t("auth.action.sendCode")) {
                val seconds = repository.sendCode(baseUrlProvider(), email)
                codeSent = true
                codeSecondsLeft = seconds.toInt()
                resendSecondsLeft = RESEND_COOLDOWN_SECONDS
                signupCode = ""
                setCodeMessage(t("auth.code.enterHint"), isError = false)
            }
        } finally {
            sendingCode = false
        }
    }

    suspend fun verifyCode(): Boolean {
        val email = signupEmail.trim()
        if (signupCode.length != 6) {
            setCodeMessage(t("auth.code.need6"), isError = true)
            return false
        }
        return try {
            busy = true
            withContext(Dispatchers.Default) { repository.verifyCode(baseUrlProvider(), email, signupCode) }
            verifiedEmail = email.lowercase()
            codeSecondsLeft = 0
            resendSecondsLeft = 0
            setCodeMessage(t("auth.code.verified"), isError = false)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 코드 오류는 화면 하단이 아니라 입력칸 바로 아래에 보여준다
            setCodeMessage(srv(e.message) ?: t("auth.code.verifyFailed"), isError = true)
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
                setCodeMessage(t("auth.code.expired"), isError = true)
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
            !EMAIL_REGEX.matches(email) -> t("auth.err.emailFormat")
            !emailVerified && signupInviteCode.isBlank() -> t("auth.err.verifyOrInvite")
            nickname.length !in 2..12 -> t("auth.err.nicknameLength")
            !isPasswordStrong(signupPassword) -> t("auth.err.passwordWeak")
            signupPassword != signupPasswordConfirm -> t("auth.err.passwordMismatch")
            !agreedToTerms -> t("auth.err.terms")
            else -> null
        }
        if (problem != null) {
            errorMessage = problem
            return false
        }

        return run(t("auth.action.signup")) {
            val auth = repository.signup(
                baseUrlProvider(),
                SignupRequest(
                    email = email,
                    password = signupPassword,
                    passwordConfirm = signupPasswordConfirm,
                    nickname = nickname,
                    // 메일 인증을 마쳤다면 초대코드는 보내지 않는다 (서버는 코드가 있으면 인증을 건너뛰기 때문)
                    inviteCode = if (emailVerified) null else signupInviteCode.trim().ifBlank { null },
                ),
            )
            saveSession(auth.token, auth.user)
        }
    }

    // ─────────────────────────────────────────────
    // 계정 설정 (설정 화면) — 성공하면 null, 실패하면 보여줄 오류 문구
    // ─────────────────────────────────────────────

    /** 구글로 가입한 계정은 비밀번호가 없어 변경할 수 없다 */
    val hasPassword: Boolean get() = user?.provider != "GOOGLE"

    suspend fun changePassword(current: String, new: String, confirm: String): String? {
        val saved = token ?: return t("settings.err.loginRequired")
        when {
            current.isEmpty() -> return t("settings.err.currentPasswordEmpty")
            !isPasswordStrong(new) -> return t("settings.err.passwordWeak")
            new != confirm -> return t("settings.err.passwordMismatch")
            new == current -> return t("settings.err.passwordSame")
        }
        return account { repository.changePassword(baseUrlProvider(), saved, current, new) }
    }

    suspend fun changeNickname(nickname: String): String? {
        val saved = token ?: return t("settings.err.loginRequired")
        val value = nickname.trim()
        if (value.length !in 2..12) return t("settings.err.nicknameLength")
        if (value == user?.nickname) return null
        return account { user = repository.changeNickname(baseUrlProvider(), saved, value) }
    }

    /** 회원 탈퇴 — 이메일 가입자는 비밀번호 확인이 필요하다. 성공하면 로그아웃 상태가 된다 */
    suspend fun deleteAccount(password: String?): String? {
        val saved = token ?: return t("settings.err.loginRequired")
        if (hasPassword && password.isNullOrEmpty()) return t("settings.err.currentPasswordEmpty")
        val error = account { repository.deleteAccount(baseUrlProvider(), saved, password) }
        if (error == null) clearSession()
        return error
    }

    private suspend fun account(block: suspend () -> Unit): String? = try {
        busy = true
        withContext(Dispatchers.Default) { block() }
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        srv(e.message) ?: t("settings.err.network")
    } finally {
        busy = false
    }

    // ─────────────────────────────────────────────

    /** 네트워크 호출 공통 처리 — busy 토글과 오류 메시지를 한곳에서 다룬다 */
    private suspend fun run(what: String, block: suspend () -> Unit): Boolean = try {
        busy = true
        errorMessage = null
        withContext(Dispatchers.Default) { block() }
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        errorMessage = srv(e.message) ?: t("auth.err.failed", what)
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
        signupInviteCode = ""
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
