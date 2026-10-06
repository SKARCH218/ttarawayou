package com.trevit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.AppState
import com.trevit.app.AuthState
import com.trevit.app.twoDigits
import com.trevit.app.resources.*
import com.trevit.app.Screen
import com.trevit.app.i18n.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 웹 `signup.html` 대응 — 메일 인증(6자리 코드)을 마쳐야 가입된다.
 * 인증코드 유효시간·재발송 쿨다운은 백엔드와 같은 값을 쓴다.
 */
@Composable
fun SignupScreen(state: AppState) {
    val auth = state.auth
    val scope = rememberCoroutineScope()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    // 인증코드 남은 시간·재발송 쿨다운을 1초마다 깎는다
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            auth.tickTimers()
        }
    }

    WebScreen {
        Spacer(Modifier.height(20.dp))

        Icon(
            painter = painterResource(Res.drawable.ic_travit_symbol),
            contentDescription = tr("auth.brand"),
            tint = BrandMint,
            modifier = Modifier.width(104.dp).height(64.dp),
        )

        Spacer(Modifier.height(12.dp))
        GradientTitle(tr("auth.signup.title"))
        WebSubtitle(tr("auth.signup.subtitle"), modifier = Modifier.padding(top = 10.dp))

        TrevitCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            EmailWithVerify(auth, scope)
            if (auth.codeSent) {
                Spacer(Modifier.height(16.dp))
                CodeField(auth, scope)
            }

            if (!auth.emailVerified) {
                Spacer(Modifier.height(16.dp))
                InviteCodeField(auth)
            }

            Spacer(Modifier.height(16.dp))
            NicknameField(auth)

            Spacer(Modifier.height(16.dp))
            PasswordFields(
                auth = auth,
                passwordVisible = passwordVisible,
                confirmVisible = confirmVisible,
                onTogglePassword = { passwordVisible = !passwordVisible },
                onToggleConfirm = { confirmVisible = !confirmVisible },
            )

            TermsCheckbox(
                checked = auth.agreedToTerms,
                onToggle = { auth.agreedToTerms = !auth.agreedToTerms },
                text = tr("auth.signup.terms"),
            )
        }

        auth.errorMessage?.let { ErrorBox(it) }

        Spacer(Modifier.height(16.dp))
        PrimaryCta(
            text = if (auth.busy) tr("auth.signup.busy") else tr("auth.signup.button"),
            enabled = !auth.busy,
            onClick = {
                scope.launch {
                    if (auth.signup()) state.screen = Screen.Setup
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        AuthSwitchRow(tr("auth.signup.hasAccount"), tr("auth.login.button")) {
            auth.errorMessage = null
            state.screen = Screen.Login
        }
    }
}

/** 이메일 + 인증요청 버튼 (웹 `.verify-row`) */
@Composable
private fun EmailWithVerify(auth: AuthState, scope: kotlinx.coroutines.CoroutineScope) {
    FieldLabel(tr("auth.field.email"))
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            AuthTextField(
                value = auth.signupEmail,
                onValueChange = auth::onSignupEmailChanged,
                placeholder = "travit@example.com",
                keyboardType = KeyboardType.Email,
                enabled = !auth.emailVerified,
                verified = auth.emailVerified,
            )
        }
        VerifyButton(
            text = when {
                auth.emailVerified -> tr("auth.signup.verified")
                auth.sendingCode -> tr("auth.signup.sending")
                auth.resendSecondsLeft > 0 -> tr("auth.signup.resendIn", auth.resendSecondsLeft)
                auth.codeSent -> tr("auth.signup.resend")
                else -> tr("auth.signup.requestCode")
            },
            enabled = !auth.busy && !auth.emailVerified && auth.resendSecondsLeft == 0,
            onClick = { scope.launch { auth.sendCode() } },
        )
    }
    FieldHint(
        text = if (auth.emailVerified) tr("auth.signup.emailVerifiedHint") else tr("auth.signup.emailHint"),
        isOk = auth.emailVerified,
    )
}

/** 인증번호 입력 + 남은 시간 (웹 `.code-input` + `.code-timer`) */
@Composable
private fun CodeField(auth: AuthState, scope: kotlinx.coroutines.CoroutineScope) {
    FieldLabel(tr("auth.signup.codeLabel"))
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            AuthTextField(
                value = auth.signupCode,
                onValueChange = { input -> auth.signupCode = input.filter { it.isDigit() }.take(6) },
                placeholder = "000000",
                keyboardType = KeyboardType.NumberPassword,
                enabled = !auth.emailVerified,
                letterSpacingSp = 6f,
                trailing = {
                    if (!auth.emailVerified && auth.codeSecondsLeft > 0) {
                        Text(
                            text = "${auth.codeSecondsLeft / 60}:${twoDigits(auth.codeSecondsLeft % 60)}",
                            modifier = Modifier.padding(end = 8.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WebError,
                        )
                    }
                },
            )
        }
        VerifyButton(
            text = tr("common.ok"),
            enabled = !auth.busy && !auth.emailVerified,
            onClick = { scope.launch { auth.verifyCode() } },
        )
    }
    auth.codeMessage?.let {
        FieldHint(it, isError = auth.codeMessageIsError, isOk = !auth.codeMessageIsError)
    }
}

/** 메일 인증이 안 될 때 쓰는 임시 경로 — 초대코드를 받은 사람은 이메일 인증을 건너뛴다 */
@Composable
private fun InviteCodeField(auth: AuthState) {
    FieldLabel(tr("auth.signup.inviteLabel"))
    Spacer(Modifier.height(8.dp))
    AuthTextField(
        value = auth.signupInviteCode,
        onValueChange = { auth.signupInviteCode = it.trim().take(32) },
        placeholder = tr("auth.signup.invitePlaceholder"),
    )
    FieldHint(
        text = if (auth.signupInviteCode.isBlank()) tr("auth.signup.inviteHint")
        else tr("auth.signup.inviteActive"),
        isOk = auth.signupInviteCode.isNotBlank(),
    )
}

@Composable
private fun NicknameField(auth: AuthState) {
    val length = auth.signupNickname.trim().length
    val valid = length in 2..12
    FieldLabel(tr("auth.signup.nicknameLabel"))
    Spacer(Modifier.height(8.dp))
    AuthTextField(
        value = auth.signupNickname,
        onValueChange = { auth.signupNickname = it.take(12) },
        placeholder = tr("auth.signup.nicknamePlaceholder"),
        isError = length > 0 && !valid,
    )
    FieldHint(
        text = when {
            length == 0 -> tr("auth.signup.nicknameHint")
            valid -> tr("auth.signup.nicknameOk", length)
            else -> tr("auth.signup.nicknameInvalid")
        },
        isError = length > 0 && !valid,
        isOk = valid,
    )
}

@Composable
private fun PasswordFields(
    auth: AuthState,
    passwordVisible: Boolean,
    confirmVisible: Boolean,
    onTogglePassword: () -> Unit,
    onToggleConfirm: () -> Unit,
) {
    val password = auth.signupPassword
    val needMin8 = tr("auth.signup.pwMin8")
    val needLetter = tr("auth.signup.pwLetter")
    val needDigit = tr("auth.signup.pwDigit")
    val missing = buildList {
        if (password.length < 8) add(needMin8)
        if (password.none { it.isLetter() }) add(needLetter)
        if (password.none { it.isDigit() }) add(needDigit)
    }

    FieldLabel(tr("auth.field.password"))
    Spacer(Modifier.height(8.dp))
    AuthTextField(
        value = password,
        onValueChange = { auth.signupPassword = it },
        placeholder = tr("auth.signup.passwordPlaceholder"),
        keyboardType = KeyboardType.Password,
        visualTransformation = if (passwordVisible) VisualTransformation.None else passwordMask,
        isError = password.isNotEmpty() && missing.isNotEmpty(),
        trailing = { PasswordToggle(passwordVisible, onTogglePassword) },
    )
    PasswordMeter(AuthState.passwordScore(password))
    FieldHint(
        text = if (missing.isEmpty()) tr("auth.signup.passwordStrong") else tr("auth.signup.passwordMissing", missing.joinToString(" · ")),
        isError = password.isNotEmpty() && missing.isNotEmpty(),
        isOk = password.isNotEmpty() && missing.isEmpty(),
    )

    Spacer(Modifier.height(16.dp))

    val confirm = auth.signupPasswordConfirm
    val same = confirm.isNotEmpty() && confirm == password
    FieldLabel(tr("auth.signup.confirmLabel"))
    Spacer(Modifier.height(8.dp))
    AuthTextField(
        value = confirm,
        onValueChange = { auth.signupPasswordConfirm = it },
        placeholder = tr("auth.signup.confirmPlaceholder"),
        keyboardType = KeyboardType.Password,
        visualTransformation = if (confirmVisible) VisualTransformation.None else passwordMask,
        isError = confirm.isNotEmpty() && !same,
        trailing = { PasswordToggle(confirmVisible, onToggleConfirm) },
    )
    if (confirm.isNotEmpty()) {
        FieldHint(
            text = if (same) tr("auth.signup.passwordMatch") else tr("auth.signup.passwordMismatch"),
            isError = !same,
            isOk = same,
        )
    }
}
