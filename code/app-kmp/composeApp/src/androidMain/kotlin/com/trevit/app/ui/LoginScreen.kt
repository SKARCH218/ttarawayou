package com.trevit.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.trevit.app.AppState
import com.trevit.app.R
import com.trevit.app.Screen
import kotlinx.coroutines.launch

/**
 * 웹 `login.html` 대응 — 이메일 + 비밀번호 로그인.
 * 구글 로그인은 안드로이드용 OAuth 클라이언트 ID가 필요해 아직 넣지 않았다.
 */
@Composable
fun LoginScreen(state: AppState) {
    val auth = state.auth
    val scope = rememberCoroutineScope()
    var passwordVisible by remember { mutableStateOf(false) }

    WebScreen {
        Spacer(Modifier.height(28.dp))

        // 웹 `.auth-screen .brand-logo { width: 104px }` — 원본 비율 94:58
        Icon(
            painter = painterResource(R.drawable.ic_travit_symbol),
            contentDescription = "트레빗",
            tint = BrandMint,
            modifier = Modifier.width(104.dp).height(64.dp),
        )

        Spacer(Modifier.height(12.dp))
        GradientTitle("다시 만나서 반가워요")
        WebSubtitle("오늘은 어디로 떠나볼까요?", modifier = Modifier.padding(top = 10.dp))

        LoginCard(state, passwordVisible) { passwordVisible = !passwordVisible }

        auth.errorMessage?.let { ErrorBox(it) }

        Spacer(Modifier.height(16.dp))
        PrimaryCta(
            text = if (auth.busy) "로그인 중…" else "로그인",
            enabled = !auth.busy,
            onClick = {
                scope.launch {
                    if (auth.login()) state.screen = Screen.Setup
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        AuthSwitchRow("아직 계정이 없나요?", "회원가입") {
            auth.errorMessage = null
            state.screen = Screen.Signup
        }
    }
}

@Composable
private fun LoginCard(state: AppState, passwordVisible: Boolean, onToggleVisible: () -> Unit) {
    val auth = state.auth
    TrevitCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        FieldLabel("이메일")
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = auth.loginEmail,
            onValueChange = { auth.loginEmail = it },
            placeholder = "travit@example.com",
            keyboardType = KeyboardType.Email,
        )

        Spacer(Modifier.height(16.dp))

        FieldLabel("비밀번호")
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = auth.loginPassword,
            onValueChange = { auth.loginPassword = it },
            placeholder = "비밀번호",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) VisualTransformation.None else passwordMask,
            trailing = { PasswordToggle(passwordVisible, onToggleVisible) },
        )
    }
}
