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
            contentDescription = "트레빗",
            tint = BrandMint,
            modifier = Modifier.width(104.dp).height(64.dp),
        )

        Spacer(Modifier.height(12.dp))
        GradientTitle("떠날 준비 되셨나요?")
        WebSubtitle("30초면 가입 끝 — 바로 여행을 만들 수 있어요", modifier = Modifier.padding(top = 10.dp))

        TrevitCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            EmailWithVerify(auth, scope)
            if (auth.codeSent) {
                Spacer(Modifier.height(16.dp))
                CodeField(auth, scope)
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
                text = "서비스 이용약관과 개인정보 처리방침에 동의합니다. 위치정보는 여행 안내 중에만 쓰이고 저장하지 않아요.",
            )
        }

        auth.errorMessage?.let { ErrorBox(it) }

        Spacer(Modifier.height(16.dp))
        PrimaryCta(
            text = if (auth.busy) "가입하는 중…" else "가입하고 시작하기",
            enabled = !auth.busy,
            onClick = {
                scope.launch {
                    if (auth.signup()) state.screen = Screen.Setup
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        AuthSwitchRow("이미 계정이 있나요?", "로그인") {
            auth.errorMessage = null
            state.screen = Screen.Login
        }
    }
}

/** 이메일 + 인증요청 버튼 (웹 `.verify-row`) */
@Composable
private fun EmailWithVerify(auth: AuthState, scope: kotlinx.coroutines.CoroutineScope) {
    FieldLabel("이메일")
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
                auth.emailVerified -> "인증완료"
                auth.resendSecondsLeft > 0 -> "재발송 ${auth.resendSecondsLeft}"
                auth.codeSent -> "재발송"
                else -> "인증요청"
            },
            enabled = !auth.busy && !auth.emailVerified && auth.resendSecondsLeft == 0,
            onClick = { scope.launch { auth.sendCode() } },
        )
    }
    FieldHint(
        text = if (auth.emailVerified) "인증된 주소예요" else "인증번호를 받을 주소예요",
        isOk = auth.emailVerified,
    )
}

/** 인증번호 입력 + 남은 시간 (웹 `.code-input` + `.code-timer`) */
@Composable
private fun CodeField(auth: AuthState, scope: kotlinx.coroutines.CoroutineScope) {
    FieldLabel("인증번호")
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
            text = "확인",
            enabled = !auth.busy && !auth.emailVerified,
            onClick = { scope.launch { auth.verifyCode() } },
        )
    }
    auth.codeMessage?.let {
        FieldHint(it, isError = auth.codeMessageIsError, isOk = !auth.codeMessageIsError)
    }
}

@Composable
private fun NicknameField(auth: AuthState) {
    val length = auth.signupNickname.trim().length
    val valid = length in 2..12
    FieldLabel("닉네임")
    Spacer(Modifier.height(8.dp))
    AuthTextField(
        value = auth.signupNickname,
        onValueChange = { auth.signupNickname = it.take(12) },
        placeholder = "여행자",
        isError = length > 0 && !valid,
    )
    FieldHint(
        text = when {
            length == 0 -> "2~12자 — 여행 화면에서 이렇게 불러 드려요"
            valid -> "${length}자 — 좋아요"
            else -> "닉네임은 2~12자로 입력해 주세요"
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
    val missing = buildList {
        if (password.length < 8) add("8자 이상")
        if (password.none { it.isLetter() }) add("영문자")
        if (password.none { it.isDigit() }) add("숫자")
    }

    FieldLabel("비밀번호")
    Spacer(Modifier.height(8.dp))
    AuthTextField(
        value = password,
        onValueChange = { auth.signupPassword = it },
        placeholder = "영문+숫자 8자 이상",
        keyboardType = KeyboardType.Password,
        visualTransformation = if (passwordVisible) VisualTransformation.None else passwordMask,
        isError = password.isNotEmpty() && missing.isNotEmpty(),
        trailing = { PasswordToggle(passwordVisible, onTogglePassword) },
    )
    PasswordMeter(AuthState.passwordScore(password))
    FieldHint(
        text = if (missing.isEmpty()) "안전한 비밀번호예요" else "${missing.joinToString(" · ")}가 더 필요해요",
        isError = password.isNotEmpty() && missing.isNotEmpty(),
        isOk = password.isNotEmpty() && missing.isEmpty(),
    )

    Spacer(Modifier.height(16.dp))

    val confirm = auth.signupPasswordConfirm
    val same = confirm.isNotEmpty() && confirm == password
    FieldLabel("비밀번호 확인")
    Spacer(Modifier.height(8.dp))
    AuthTextField(
        value = confirm,
        onValueChange = { auth.signupPasswordConfirm = it },
        placeholder = "한 번 더 입력",
        keyboardType = KeyboardType.Password,
        visualTransformation = if (confirmVisible) VisualTransformation.None else passwordMask,
        isError = confirm.isNotEmpty() && !same,
        trailing = { PasswordToggle(confirmVisible, onToggleConfirm) },
    )
    if (confirm.isNotEmpty()) {
        FieldHint(
            text = if (same) "비밀번호가 일치해요" else "비밀번호가 서로 달라요",
            isError = !same,
            isOk = same,
        )
    }
}
