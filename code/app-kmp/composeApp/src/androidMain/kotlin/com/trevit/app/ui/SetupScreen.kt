package com.trevit.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.AppState
import com.trevit.app.BUDGET_STEP
import com.trevit.app.MIN_BUDGET
import com.trevit.app.R
import com.trevit.app.REGIONS
import com.trevit.app.Screen
import kotlinx.coroutines.launch

/**
 * 여행 설정 — 웹 `index.html` 의 `.screen` 을 그대로 옮긴 화면.
 * 로고 → 그라데이션 제목 → 부제 → 카드(지역·예산·기간·인원) → 다음.
 *
 * 지역만 웹의 `<select>` 대신 검색 + 칩을 유지한다 (터치 환경에서 더 낫다).
 */
@Composable
fun SetupScreen(state: AppState) {
    var regionQuery by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.baseUrl) { state.loadWallet() }

    Box(Modifier.fillMaxWidth()) {
        WebScreen {
            // 웹 `.brand-logo { width: 92px; margin: 0 auto 6px }` — 원본 비율 134:113
            Icon(
                painter = painterResource(R.drawable.ic_travit_logo),
                contentDescription = "트레빗",
                tint = BrandMint,
                modifier = Modifier
                    .width(92.dp)
                    .height(78.dp),
            )
            Spacer(Modifier.height(6.dp))
            GradientTitle("어디로 떠나볼까요?")
            Spacer(Modifier.height(10.dp))
            WebSubtitle("장소는 도착 전까지 비밀")

            Spacer(Modifier.height(16.dp))
            TrevitCard(Modifier.fillMaxWidth()) {
                RegionField(
                    query = regionQuery,
                    onQueryChange = { regionQuery = it },
                    selected = state.region,
                    onSelect = { state.region = it },
                )
                Spacer(Modifier.height(16.dp))
                BudgetField(state) { scope.launch { state.chargeWallet() } }
                Spacer(Modifier.height(16.dp))
                // 웹 `.field-row { display:flex; gap:10px }` — 기간·인원을 나란히
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("기간")
                        Spacer(Modifier.height(8.dp))
                        WebStepper(state.days, "일", { state.days = it }, 1..3)
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("인원")
                        Spacer(Modifier.height(8.dp))
                        WebStepper(state.people, "명", { state.people = it }, 1..4)
                    }
                }
            }

            state.setupError?.let { ErrorBox(it) }

            Spacer(Modifier.height(16.dp))
            PrimaryCta(
                text = "다음",
                onClick = {
                    val balance = state.walletBalance ?: 0
                    if (state.budget > balance) {
                        state.setupError =
                            "보유 토큰(${"%,d".format(balance)})이 부족해요. 충전 버튼을 눌러 주세요."
                    } else {
                        state.setupError = null
                        state.startProfile()
                    }
                },
                enabled = state.region != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 서버 주소 설정 — 웹에는 없지만 실제 폰에서 백엔드 주소를 바꾸려면 필요하다.
        // 웹 화면을 흐트러뜨리지 않도록 모서리에 흐리게 둔다.
        IconButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 4.dp)
                .size(32.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_settings),
                contentDescription = "서버 주소",
                tint = webTextDecor().copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (showSettings) {
        var urlInput by remember { mutableStateOf(state.baseUrl) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("서버 주소") },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    state.saveBaseUrl(urlInput)
                    showSettings = false
                }) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) { Text("취소") }
            },
        )
    }
}

/** 웹 `.field` 의 지역 칸. select 대신 검색어 + 칩(`.chip`)으로 고른다. */
@Composable
private fun RegionField(
    query: String,
    onQueryChange: (String) -> Unit,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Column {
        FieldLabel("지역")
        Spacer(Modifier.height(8.dp))
        // 웹 `.ds-select` — 패딩 11/12, radius 12, 1px mono-100, 배경 mono-050
        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, webBorderStrong(), RoundedCornerShape(12.dp))
                .background(webFill(), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = webTextDim(),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("지역 검색", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = webTextDim())
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = webText(),
                    ),
                    cursorBrush = SolidColor(WebMint),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // 웹 `.chips { gap: 8px }`.
        // 웹의 select 는 한 줄만 차지하므로, 칩도 가로 스크롤 한 줄로 두어 화면 높이를 맞춘다.
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            REGIONS.filter { query.isBlank() || it.contains(query.trim()) }.forEach { region ->
                WebChip(region, selected == region, { onSelect(region) }, compact = true)
            }
        }
    }
}

/**
 * 웹 `.field` 의 예산 칸 — 큰 숫자(누르면 직접 입력) + 슬라이더 + 보유 토큰/충전.
 */
@Composable
private fun BudgetField(state: AppState, onCharge: () -> Unit) {
    var editing by remember { mutableStateOf(false) }

    Column {
        FieldLabel("예산")
        Spacer(Modifier.height(8.dp))
        if (editing) {
            BudgetInlineEditor(
                initial = state.budget,
                onCommit = { value ->
                    state.budget = state.clampBudget(
                        ((value + BUDGET_STEP / 2) / BUDGET_STEP) * BUDGET_STEP,
                    )
                    editing = false
                },
                onCancel = { editing = false },
            )
        } else {
            // 웹 `.budget-display` — 숫자(32px, primary-700, primary-200 점선 밑줄) + "토큰"
            val underline = Color(0xFF9BDFCC)
            val interaction = remember { MutableInteractionSource() }
            Row(
                Modifier.clickable(interactionSource = interaction, indication = null) {
                    editing = true
                },
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    "%,d".format(state.budget),
                    fontSize = 32.sp,
                    lineHeight = 37.sp,
                    fontWeight = FontWeight.Bold,
                    color = WebMintDeep,
                    modifier = Modifier.drawBehind {
                        drawLine(
                            color = underline,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(3.dp.toPx(), 3.dp.toPx()),
                            ),
                        )
                    },
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "토큰",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = webTextDim(),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        // 웹 `.field input.budget-slider` — 46dp 높이의 테두리 상자 안에 트랙이 들어간다
        Box(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .border(1.5.dp, webBorderStrong(), RoundedCornerShape(12.dp))
                .background(webSurface(), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            val max = state.maxBudget.coerceAtLeast(MIN_BUDGET + BUDGET_STEP)
            WebSlider(
                value = state.budget.coerceIn(MIN_BUDGET, max).toFloat(),
                onValueChange = {
                    state.budget = state.clampBudget(
                        ((it.toLong() + BUDGET_STEP / 2) / BUDGET_STEP) * BUDGET_STEP,
                    )
                },
                valueRange = MIN_BUDGET.toFloat()..max.toFloat(),
            )
        }

        // 웹 `.wallet-row` — 점선 위에 "보유 N 토큰"과 충전 버튼
        Spacer(Modifier.height(14.dp))
        DashedDivider()
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("보유 ", fontSize = 14.sp, color = webTextMuted())
                Text(
                    state.walletBalance?.let { "%,d 토큰".format(it) } ?: "…",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WebOrangeDark,
                )
            }
            ChargeButton(onCharge)
        }
    }
}

/** 웹 `.budget-edit` — 숫자를 누르면 나타나는 직접 입력 칸 */
@Composable
private fun BudgetInlineEditor(
    initial: Long,
    onCommit: (Long) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf("%,d".format(initial)) }
    val focus = remember { FocusRequester() }
    var touched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val commit = {
        val digits = text.filter { it.isDigit() }
        if (digits.isEmpty()) onCancel() else onCommit(digits.toLong())
    }
    BasicTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }
            text = if (digits.isEmpty()) "" else "%,d".format(digits.toLong())
        },
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = webText(),
        ),
        cursorBrush = SolidColor(WebMint),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        modifier = Modifier
            .width(200.dp)
            .heightIn(min = 44.dp)
            .border(1.5.dp, WebMint, RoundedCornerShape(8.dp))
            .background(webSurface(), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .focusRequester(focus)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) touched = true else if (touched) commit()
            },
    )
}

/** 웹 `.charge-btn` — 패딩 7/13, radius 8, primary-050 배경 */
@Composable
private fun ChargeButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isDark()) Color(0xFF10453A) else Color(0xFFDAF5EC),
        border = BorderStroke(1.dp, if (isDark()) Color(0xFF2E7C66) else Color(0xFFB5E5D7)),
    ) {
        Box(Modifier.padding(horizontal = 13.dp, vertical = 7.dp)) {
            Text(
                "충전",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark()) Color(0xFF7FDEC1) else Color(0xFF009969),
            )
        }
    }
}

/** 웹 `.error-box` — 붉은 배경의 안내 상자 */
@Composable
fun ErrorBox(message: String) {
    Spacer(Modifier.height(16.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.dp, if (isDark()) Color(0xFF6E2833) else Color(0xFFFF8A93), RoundedCornerShape(12.dp))
            .background(
                if (isDark()) Color(0xFF3A1C20) else Color(0xFFFFE2E4),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            message,
            fontSize = 13.5.sp,
            lineHeight = 21.sp,
            color = if (isDark()) Color(0xFFFF9DA6) else Color(0xFFC90E2E),
        )
    }
}

@Composable
private fun isDark(): Boolean = androidx.compose.foundation.isSystemInDarkTheme()
