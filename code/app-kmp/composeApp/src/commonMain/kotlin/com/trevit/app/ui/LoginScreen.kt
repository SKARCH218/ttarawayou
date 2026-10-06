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
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.trevit.app.AppState
import com.trevit.app.resources.*
import com.trevit.app.Screen
import com.trevit.app.i18n.tr
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
            painter = painterResource(Res.drawable.ic_travit_symbol),
            contentDescription = tr("auth.brand"),
            tint = BrandMint,
            modifier = Modifier.width(104.dp).height(64.dp),
        )

        Spacer(Modifier.height(12.dp))
        GradientTitle(tr("auth.login.title"))
        WebSubtitle(tr("auth.login.subtitle"), modifier = Modifier.padding(top = 10.dp))

        LoginCard(state, passwordVisible) { passwordVisible = !passwordVisible }

        auth.errorMessage?.let { ErrorBox(it) }

        Spacer(Modifier.height(16.dp))
        PrimaryCta(
            text = if (auth.busy) tr("auth.login.busy") else tr("auth.login.button"),
            enabled = !auth.busy,
            onClick = {
                scope.launch {
                    if (auth.login()) state.screen = Screen.Setup
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        AuthSwitchRow(tr("auth.login.noAccount"), tr("auth.login.toSignup")) {
            auth.errorMessage = null
            state.screen = Screen.Signup
        }
    }
}

@Composable
private fun LoginCard(state: AppState, passwordVisible: Boolean, onToggleVisible: () -> Unit) {
    val auth = state.auth
    TrevitCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        FieldLabel(tr("auth.field.email"))
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = auth.loginEmail,
            onValueChange = { auth.loginEmail = it },
            placeholder = "travit@example.com",
            keyboardType = KeyboardType.Email,
        )

        Spacer(Modifier.height(16.dp))

        FieldLabel(tr("auth.field.password"))
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = auth.loginPassword,
            onValueChange = { auth.loginPassword = it },
            placeholder = tr("auth.field.password"),
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) VisualTransformation.None else passwordMask,
            trailing = { PasswordToggle(passwordVisible, onToggleVisible) },
        )
    }
}
