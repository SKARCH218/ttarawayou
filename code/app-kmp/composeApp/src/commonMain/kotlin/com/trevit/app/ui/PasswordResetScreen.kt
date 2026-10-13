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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.AppState
import com.trevit.app.AuthState
import com.trevit.app.Screen
import com.trevit.app.i18n.tr
import com.trevit.app.resources.*
import com.trevit.app.twoDigits
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * 비밀번호 찾기 — 가입한 이메일로 인증번호를 받아 새 비밀번호를 정한다.
 * 비밀번호는 해시로만 저장돼 "원래 비밀번호"는 누구도 알 수 없으므로, 새로 정하는 방식뿐이다.
 */
@Composable
fun PasswordResetScreen(state: AppState) {
    val auth = state.auth
    val scope = rememberCoroutineScope()
    var passwordVisible by remember { mutableStateOf(false) }

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
        GradientTitle(tr("auth.reset.title"))
        WebSubtitle(tr("auth.reset.subtitle"), modifier = Modifier.padding(top = 10.dp))

        TrevitCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            // 1) 이메일 + 인증번호 받기
            FieldLabel(tr("auth.field.email"))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    AuthTextField(
                        value = auth.resetEmail,
                        onValueChange = { auth.resetEmail = it },
                        placeholder = "travit@example.com",
                        keyboardType = KeyboardType.Email,
                    )
                }
                VerifyButton(
                    text = when {
                        auth.sendingCode -> tr("auth.signup.sending")
                        auth.resendSecondsLeft > 0 -> tr("auth.signup.resendIn", auth.resendSecondsLeft)
                        auth.resetCodeSent -> tr("auth.signup.resend")
                        else -> tr("auth.signup.requestCode")
                    },
                    enabled = !auth.busy && auth.resendSecondsLeft == 0,
                    onClick = { scope.launch { auth.sendResetCode() } },
                )
            }
            FieldHint(tr("auth.reset.emailHint"))

            if (auth.resetCodeSent) {
                // 2) 인증번호
                Spacer(Modifier.height(16.dp))
                FieldLabel(tr("auth.signup.codeLabel"))
                Spacer(Modifier.height(8.dp))
                AuthTextField(
                    value = auth.resetCode,
                    onValueChange = { input -> auth.resetCode = input.filter { it.isDigit() }.take(6) },
                    placeholder = "000000",
                    keyboardType = KeyboardType.NumberPassword,
                    letterSpacingSp = 6f,
                    trailing = {
                        if (auth.codeSecondsLeft > 0) {
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
                auth.codeMessage?.let { FieldHint(it, isError = auth.codeMessageIsError, isOk = !auth.codeMessageIsError) }

                // 3) 새 비밀번호
                Spacer(Modifier.height(16.dp))
                NewPasswordFields(auth, passwordVisible) { passwordVisible = !passwordVisible }
            }
        }

        auth.errorMessage?.let { ErrorBox(it) }

        if (auth.resetCodeSent) {
            Spacer(Modifier.height(16.dp))
            PrimaryCta(
                text = if (auth.busy) tr("auth.reset.busy") else tr("auth.reset.button"),
                enabled = !auth.busy,
                onClick = {
                    scope.launch {
                        if (auth.resetPasswordSubmit()) state.screen = Screen.Login
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AuthSwitchRow(tr("auth.reset.remembered"), tr("auth.login.button")) {
            auth.clearResetForm()
            state.screen = Screen.Login
        }
    }
}

@Composable
private fun NewPasswordFields(auth: AuthState, visible: Boolean, onToggle: () -> Unit) {
    val password = auth.resetPassword
    val missing = buildList {
        if (password.length < 8) add(tr("auth.signup.pwMin8"))
        if (password.none { it.isLetter() }) add(tr("auth.signup.pwLetter"))
        if (password.none { it.isDigit() }) add(tr("auth.signup.pwDigit"))
    }
    FieldLabel(tr("auth.reset.newPassword"))
    Spacer(Modifier.height(8.dp))
    AuthTextField(
        value = password,
        onValueChange = { auth.resetPassword = it },
        placeholder = tr("auth.signup.passwordPlaceholder"),
        keyboardType = KeyboardType.Password,
        visualTransformation = if (visible) VisualTransformation.None else passwordMask,
        isError = password.isNotEmpty() && missing.isNotEmpty(),
        trailing = { PasswordToggle(visible, onToggle) },
    )
    PasswordMeter(AuthState.passwordScore(password))
    FieldHint(
        text = if (missing.isEmpty()) tr("auth.signup.passwordStrong") else tr("auth.signup.passwordMissing", missing.joinToString(" · ")),
        isError = password.isNotEmpty() && missing.isNotEmpty(),
        isOk = password.isNotEmpty() && missing.isEmpty(),
    )

    Spacer(Modifier.height(16.dp))
    val confirm = auth.resetPasswordConfirm
    val same = confirm.isNotEmpty() && confirm == password
    FieldLabel(tr("auth.signup.confirmLabel"))
    Spacer(Modifier.height(8.dp))
    AuthTextField(
        value = confirm,
        onValueChange = { auth.resetPasswordConfirm = it },
        placeholder = tr("auth.signup.confirmPlaceholder"),
        keyboardType = KeyboardType.Password,
        visualTransformation = if (visible) VisualTransformation.None else passwordMask,
        isError = confirm.isNotEmpty() && !same,
    )
    if (confirm.isNotEmpty()) {
        FieldHint(
            text = if (same) tr("auth.signup.passwordMatch") else tr("auth.signup.passwordMismatch"),
            isError = !same,
            isOk = same,
        )
    }
}
