package com.trevit.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.R

/**
 * 로그인·회원가입 화면 전용 조각들.
 * 웹 `style.css` 의 `.auth-input` `.pw-toggle` `.field-hint` `.error-box` `.verify-btn` 대응.
 */

/** 웹 `.field input.auth-input` — 15px 세로 패딩, radius 12, 포커스 시 민트 테두리 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    isError: Boolean = false,
    verified: Boolean = false,
    letterSpacingSp: Float = 0f,
    trailing: @Composable (() -> Unit)? = null,
) {
    val border = when {
        isError -> WebError
        verified -> WebMint
        else -> webBorderStrong()
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (verified) webChipOnFill() else webSurface(),
        border = BorderStroke(1.5.dp, border),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = webTextDecor(),
                        letterSpacing = letterSpacingSp.sp,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) webText() else webTextMuted(),
                        letterSpacing = letterSpacingSp.sp,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(WebMint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 15.dp),
                )
            }
            if (trailing != null) trailing() else Box(Modifier.width(8.dp))
        }
    }
}

/** 웹 `.pw-toggle` — 디자인 시스템 Show·Hide 아이콘 토글 */
@Composable
fun PasswordToggle(visible: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(if (visible) R.drawable.ic_hide else R.drawable.ic_show),
                contentDescription = if (visible) "비밀번호 숨기기" else "비밀번호 표시",
                tint = webTextDim(),
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

/** 웹 `.verify-btn` — 입력칸 오른쪽에 붙는 민트 보조 버튼 */
@Composable
fun VerifyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) webChipOnFill() else webFill(),
        border = BorderStroke(1.dp, if (enabled) webChipOnBorder() else webBorder()),
    ) {
        Box(
            Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) webChipOnText() else webTextDim(),
            )
        }
    }
}

/** 오류 글자색 — 다크 모드에서는 밝은 산호빛으로 바꾼다 (SetupScreen ErrorBox와 같은 값) */
@Composable
fun errorTone(): Color = if (isSystemInDarkTheme()) Color(0xFFFF9DA6) else WebError

/** 웹 `.field-hint` — 입력칸 아래 안내. 오류면 빨강, 통과면 민트 */
@Composable
fun FieldHint(text: String, isError: Boolean = false, isOk: Boolean = false) {
    Text(
        text,
        fontSize = 12.5.sp,
        lineHeight = 12.5.sp * 1.5f,
        fontWeight = FontWeight.SemiBold,
        color = when {
            isError -> errorTone()
            isOk -> webChipOnText()
            else -> webTextFaint()
        },
        modifier = Modifier.padding(top = 7.dp),
    )
}

/** 웹 `.pw-meter` — 3칸짜리 비밀번호 강도 막대 */
@Composable
fun PasswordMeter(score: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) { index ->
            val filled = index < score
            val color = when {
                !filled -> webBorderStrong()
                score <= 1 -> errorTone()
                score == 2 -> WebWarning
                else -> WebMint
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(color, RoundedCornerShape(99.dp)),
            )
        }
    }
}

/** 웹 `.auth-switch` — "이미 계정이 있나요? 로그인" 줄 */
@Composable
fun AuthSwitchRow(question: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(question, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = webTextFaint())
        Surface(onClick = onClick, color = Color.Transparent) {
            Text(
                action,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = webChipOnText(),
            )
        }
    }
}

/** 웹 `.auth-terms` 체크박스 — Material 기본 대신 웹 스타일에 맞춘 네모 */
@Composable
fun TermsCheckbox(checked: Boolean, onToggle: () -> Unit, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            onClick = onToggle,
            shape = RoundedCornerShape(6.dp),
            color = if (checked) WebMint else Color.Transparent,
            border = BorderStroke(1.5.dp, if (checked) WebMint else webBorderStrong()),
            modifier = Modifier.size(20.dp),
        ) {
            if (checked) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        Text(
            text,
            modifier = Modifier.padding(start = 9.dp),
            fontSize = 12.5.sp,
            lineHeight = 12.5.sp * 1.55f,
            color = webTextMuted(),
        )
    }
}

/** 비밀번호 가리기 */
val passwordMask = PasswordVisualTransformation()
