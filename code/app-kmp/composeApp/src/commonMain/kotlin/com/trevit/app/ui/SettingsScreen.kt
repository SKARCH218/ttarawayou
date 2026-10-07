package com.trevit.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.AppState
import com.trevit.app.Screen
import com.trevit.app.i18n.AppLanguage
import com.trevit.app.i18n.tr
import com.trevit.app.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/** 설정 화면에서 띄우는 다이얼로그 종류 */
private enum class SettingsDialog { Nickname, Password, Logout, Withdraw, Language, Theme, Server }

/**
 * 설정 — 계정(닉네임·비밀번호·로그아웃·탈퇴), 앱 설정(언어·테마), 정보(버전), 개발자 옵션(서버 주소).
 * 메인 화면 우상단 톱니바퀴로 들어온다.
 */
@Composable
fun SettingsScreen(state: AppState) {
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    /** 잠깐 보여줬다가 사라지는 완료 안내 */
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2_500)
            notice = null
        }
    }

    val user = state.auth.user
    val hasPassword = state.auth.hasPassword

    Column(
        Modifier
            .fillMaxSize()
            .background(webBg())
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ---- 상단 바: 뒤로 + 제목 ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = { state.screen = Screen.Setup },
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = webSurface(),
                border = BorderStroke(1.dp, webBorderStrong()),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(Res.drawable.ic_chevron_left),
                        contentDescription = tr("settings.back"),
                        tint = WebMintDeep,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(tr("settings.title"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = webText())
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        ) {
            notice?.let { NoticeBanner(it) }

            // ---- 프로필 카드 ----
            TrevitCard(Modifier.fillMaxWidth(), padding = 18.dp) {
                if (user != null) {
                    Text(user.nickname, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = webText())
                    Spacer(Modifier.height(4.dp))
                    Text(user.email, fontSize = 13.5.sp, color = webTextMuted())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr(if (user.provider == "GOOGLE") "settings.googleAccount" else "settings.emailAccount"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = webChipOnText(),
                        modifier = Modifier
                            .background(webChipOnFill(), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                } else {
                    Text(tr("settings.notLoaded"), fontSize = 14.sp, color = webTextMuted())
                }
            }

            // ---- 계정 ----
            SectionTitle(tr("settings.section.account"))
            SettingsGroup {
                SettingsRow(
                    title = tr("settings.nickname"),
                    value = user?.nickname,
                    enabled = user != null,
                    onClick = { dialog = SettingsDialog.Nickname },
                )
                RowDivider()
                SettingsRow(
                    title = tr("settings.password"),
                    subtitle = if (!hasPassword) tr("settings.password.googleNote") else null,
                    enabled = user != null && hasPassword,
                    onClick = { dialog = SettingsDialog.Password },
                )
                RowDivider()
                SettingsRow(title = tr("settings.logout"), onClick = { dialog = SettingsDialog.Logout })
                RowDivider()
                SettingsRow(
                    title = tr("settings.withdraw"),
                    danger = true,
                    enabled = user != null,
                    onClick = { dialog = SettingsDialog.Withdraw },
                )
            }

            // ---- 앱 설정 ----
            SectionTitle(tr("settings.section.app"))
            SettingsGroup {
                SettingsRow(
                    title = tr("settings.language"),
                    // 웹은 일본어·중국어 폰트를 받는 동안 기존 언어를 유지한다
                    value = state.pendingLanguage?.let { "${it.nativeName} …" } ?: state.language.nativeName,
                    onClick = { dialog = SettingsDialog.Language },
                )
                RowDivider()
                SettingsRow(
                    title = tr("settings.theme"),
                    value = themeLabel(state.themeMode),
                    onClick = { dialog = SettingsDialog.Theme },
                )
            }

            // ---- 정보 ----
            SectionTitle(tr("settings.section.info"))
            SettingsGroup {
                SettingsRow(title = tr("settings.version"), value = AppState.APP_VERSION, showChevron = false)
            }

            // ---- 개발자 옵션 ----
            SectionTitle(tr("settings.section.developer"))
            SettingsGroup {
                SettingsRow(
                    title = tr("settings.server"),
                    value = state.baseUrl,
                    onClick = { dialog = SettingsDialog.Server },
                )
            }
        }
    }

    // ---- 다이얼로그 ----
    val close = { dialog = null }
    when (dialog) {
        SettingsDialog.Nickname -> NicknameDialog(state, close) { notice = it }
        SettingsDialog.Password -> PasswordDialog(state, close) { notice = it }
        SettingsDialog.Logout -> LogoutDialog(state, close)
        SettingsDialog.Withdraw -> WithdrawDialog(state, close)
        SettingsDialog.Language -> ChoiceDialog(
            title = tr("settings.language"),
            options = AppLanguage.entries.map { it to it.nativeName },
            selected = state.language,
            onSelect = { state.changeLanguage(it); close() },
            onDismiss = close,
        )
        SettingsDialog.Theme -> ChoiceDialog(
            title = tr("settings.theme"),
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = state.themeMode,
            onSelect = { state.changeTheme(it); close() },
            onDismiss = close,
        )
        SettingsDialog.Server -> ServerDialog(state, close) { notice = it }
        null -> Unit
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> tr("settings.theme.system")
    ThemeMode.LIGHT -> tr("settings.theme.light")
    ThemeMode.DARK -> tr("settings.theme.dark")
}

// ─────────────────────────────────────────────────────────────
// 화면 조각
// ─────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = webTextFaint(),
    )
}

/** 여러 행을 묶는 카드 */
@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = webSurface(),
        border = BorderStroke(1.dp, webBorder()),
    ) {
        Column { content() }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = webBorder())
}

/** 설정 한 줄 — 제목(+설명) 왼쪽, 현재 값 + › 오른쪽 */
@Composable
private fun SettingsRow(
    title: String,
    value: String? = null,
    subtitle: String? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val clickable = enabled && onClick != null
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable { onClick?.invoke() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    !enabled -> webTextDim()
                    danger -> errorTone()
                    else -> webText()
                },
            )
            subtitle?.let {
                Spacer(Modifier.height(3.dp))
                Text(it, fontSize = 12.5.sp, color = webTextFaint())
            }
        }
        value?.let {
            Spacer(Modifier.width(12.dp))
            // 값은 오른쪽 끝(› 바로 앞)에 붙인다. 길면(서버 주소 등) 말줄임
            Text(
                it,
                fontSize = 13.5.sp,
                color = webTextMuted(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        if (showChevron && clickable) {
            Spacer(Modifier.width(6.dp))
            Text("›", fontSize = 20.sp, color = webTextDecor())
        }
    }
}

@Composable
private fun NoticeBanner(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(webChipOnFill(), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        fontSize = 13.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = webChipOnText(),
    )
}

// ─────────────────────────────────────────────────────────────
// 다이얼로그
// ─────────────────────────────────────────────────────────────

/** 다이얼로그 하단 오류 문구 */
@Composable
private fun DialogError(message: String?) {
    message?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, fontSize = 13.sp, color = errorTone())
    }
}

@Composable
private fun NicknameDialog(state: AppState, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var value by remember { mutableStateOf(state.auth.user?.nickname.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val doneText = tr("settings.done.nickname")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("settings.nickname")) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.length <= 12) value = it },
                    label = { Text(tr("settings.nickname.label")) },
                    supportingText = { Text(tr("settings.nickname.hint")) },
                    singleLine = true,
                )
                DialogError(error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !state.auth.busy,
                onClick = {
                    scope.launch {
                        error = state.auth.changeNickname(value)
                        if (error == null) {
                            onDismiss()
                            onDone(doneText)
                        }
                    }
                },
            ) { Text(if (state.auth.busy) tr("settings.processing") else tr("common.save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("common.cancel")) } },
    )
}

@Composable
private fun PasswordDialog(state: AppState, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val doneText = tr("settings.done.password")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("settings.password")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PasswordField(current, { current = it }, tr("settings.password.current"))
                PasswordField(new, { new = it }, tr("settings.password.new"), supporting = tr("settings.password.rule"))
                PasswordField(confirm, { confirm = it }, tr("settings.password.confirm"))
                DialogError(error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !state.auth.busy,
                onClick = {
                    scope.launch {
                        error = state.auth.changePassword(current, new, confirm)
                        if (error == null) {
                            onDismiss()
                            onDone(doneText)
                        }
                    }
                },
            ) { Text(if (state.auth.busy) tr("settings.processing") else tr("common.change")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("common.cancel")) } },
    )
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, supporting: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 64) onChange(it) },
        label = { Text(label) },
        supportingText = supporting?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}

@Composable
private fun LogoutDialog(state: AppState, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("settings.logout")) },
        text = { Text(tr("settings.logout.confirm")) },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    state.auth.logout()
                    onDismiss()
                    state.screen = Screen.Login
                }
            }) { Text(tr("settings.logout")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("common.cancel")) } },
    )
}

@Composable
private fun WithdrawDialog(state: AppState, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val needPassword = state.auth.hasPassword
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("settings.withdraw")) },
        text = {
            Column {
                Text(tr("settings.withdraw.warning"), fontSize = 14.sp, lineHeight = 21.sp)
                if (needPassword) {
                    Spacer(Modifier.height(12.dp))
                    PasswordField(password, { password = it }, tr("settings.withdraw.passwordLabel"))
                }
                DialogError(error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !state.auth.busy,
                onClick = {
                    scope.launch {
                        error = state.auth.deleteAccount(if (needPassword) password else null)
                        if (error == null) {
                            onDismiss()
                            state.screen = Screen.Login
                        }
                    }
                },
            ) {
                Text(
                    if (state.auth.busy) tr("settings.processing") else tr("settings.withdraw.confirm"),
                    color = errorTone(),
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("common.cancel")) } },
    )
}

@Composable
private fun ServerDialog(state: AppState, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var url by remember { mutableStateOf(state.baseUrl) }
    val doneText = tr("settings.done.server")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("settings.server")) },
        text = {
            Column {
                Text(tr("settings.server.hint"), fontSize = 13.sp, lineHeight = 19.sp, color = webTextMuted())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                state.saveBaseUrl(url)
                onDismiss()
                onDone(doneText)
            }) { Text(tr("common.save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("common.cancel")) } },
    )
}

/** 언어·테마처럼 하나를 고르는 목록 다이얼로그 */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == selected,
                                onClick = { onSelect(value) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = WebMint),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(label, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("common.close")) } },
    )
}
