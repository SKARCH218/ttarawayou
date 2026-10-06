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
import androidx.compose.foundation.shape.CircleShape
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
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.AppState
import com.trevit.app.BUDGET_STEP
import com.trevit.app.comma
import com.trevit.app.MIN_BUDGET
import com.trevit.app.resources.*
import com.trevit.app.REGIONS
import com.trevit.app.Screen
import com.trevit.app.i18n.LocalLanguage
import com.trevit.app.i18n.optLabel
import com.trevit.app.i18n.tokens
import com.trevit.app.i18n.tr
import com.trevit.shared.WalletProductDto
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

    LaunchedEffect(state.baseUrl) { state.loadWallet() }

    WebScreen {
        // 상단 바 — 닉네임(로그인 시) + 설정(톱니바퀴).
        // 로그아웃은 설정 화면에, 토큰 구매는 보유 토큰 옆 + 버튼에 있다.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.auth.user?.let { user ->
                    Text(
                        tr("setup.nickname", user.nickname),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = webTextFaint(),
                    )
                }
                IconButton(
                    onClick = { state.screen = Screen.Settings },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_settings),
                        contentDescription = tr("setup.settings"),
                        tint = webTextDecor().copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // 웹 `.brand-logo { width: 92px; margin: 0 auto 6px }` — 원본 비율 134:113
            Icon(
                painter = painterResource(Res.drawable.ic_travit_logo),
                contentDescription = tr("setup.logo"),
                tint = BrandMint,
                modifier = Modifier
                    .width(92.dp)
                    .height(78.dp),
            )
            Spacer(Modifier.height(6.dp))
            GradientTitle(tr("setup.title"))
            Spacer(Modifier.height(10.dp))
            WebSubtitle(tr("setup.subtitle"))

            Spacer(Modifier.height(16.dp))
            TrevitCard(Modifier.fillMaxWidth()) {
                RegionField(
                    query = regionQuery,
                    onQueryChange = { regionQuery = it },
                    selected = state.region,
                    onSelect = { state.region = it },
                )
                Spacer(Modifier.height(16.dp))
                BudgetField(state)
                Spacer(Modifier.height(16.dp))
                // 웹 `.field-row { display:flex; gap:10px }` — 기간·인원을 나란히
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel(tr("setup.days"))
                        Spacer(Modifier.height(8.dp))
                        WebStepper(state.days, tr("setup.daysUnit"), { state.days = it }, 1..3)
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel(tr("setup.people"))
                        Spacer(Modifier.height(8.dp))
                        WebStepper(state.people, tr("setup.peopleUnit"), { state.people = it }, 1..4)
                    }
                }
            }

            state.setupError?.let { ErrorBox(it) }

            Spacer(Modifier.height(16.dp))
            PrimaryCta(
                text = tr("common.next"),
                onClick = {
                    val balance = state.walletBalance ?: 0
                    if (state.budget > balance) {
                        state.setupError =
                            state.t("setup.notEnoughTokens", comma(balance))
                    } else {
                        state.setupError = null
                        state.startProfile()
                    }
                },
                enabled = state.region != null,
                modifier = Modifier.fillMaxWidth(),
            )
    }

    if (state.showStore) {
        StoreDialog(state)
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
    val lang = LocalLanguage.current
    Column {
        FieldLabel(tr("setup.region"))
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
                painterResource(Res.drawable.ic_search),
                contentDescription = null,
                tint = webTextDim(),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(tr("setup.regionSearch"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = webTextDim())
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
            // 검색어는 한국어 값 또는 현재 언어의 표시명 어느 쪽과 맞아도 된다
            val q = query.trim()
            REGIONS.filter {
                q.isBlank() || it.contains(q) || optLabel(lang, it).contains(q, ignoreCase = true)
            }.forEach { region ->
                WebChip(optLabel(region), selected == region, { onSelect(region) }, compact = true)
            }
        }
    }
}

/**
 * 웹 `.field` 의 예산 칸 — 큰 숫자(누르면 직접 입력) + 슬라이더 + 보유 토큰.
 */
@Composable
private fun BudgetField(state: AppState) {
    var editing by remember { mutableStateOf(false) }

    Column {
        FieldLabel(tr("setup.budget"))
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
                    comma(state.budget),
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
                    tr("setup.tokenUnit"),
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

        // 웹 `.wallet-row` — 점선 위에 "보유 N 토큰 (+)". + 를 누르면 토큰 구매 창이 열린다.
        Spacer(Modifier.height(14.dp))
        DashedDivider()
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("setup.balance"), fontSize = 14.sp, color = webTextMuted())
            Text(
                state.walletBalance?.let { tokens(it) } ?: "…",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                // warning-dark 는 어두운 배경에서 묻히므로 다크에서는 한 단계 밝게
                color = if (isDark()) WebOrange else WebOrangeDark,
            )
            Spacer(Modifier.width(8.dp))
            AddTokensButton(onClick = { state.openStore() })
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
    var text by remember { mutableStateOf(comma(initial)) }
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
            text = if (digits.isEmpty()) "" else comma(digits.toLong())
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

/** 보유 토큰 옆 민트색 동그란 + 버튼 — 누르면 토큰 구매 창 */
@Composable
private fun AddTokensButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = WebMint,
        modifier = Modifier.size(24.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painterResource(Res.drawable.ic_add_plus),
                contentDescription = tr("setup.buyTokens"),
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * 토큰 구매 다이얼로그 — 고정환율제(1토큰 = 1원) 상품 목록.
 * 실제 결제(PG) 연동 전이라 누르면 곧바로 보유 토큰에 더해진다(결제 시뮬레이션).
 */
@Composable
private fun StoreDialog(state: AppState) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { state.loadStoreProducts() }

    AlertDialog(
        onDismissRequest = { state.showStore = false },
        title = { Text(tr("setup.buyTokens")) },
        text = {
            Column {
                Text(
                    tr("setup.storeRate"),
                    fontSize = 12.sp,
                    color = webTextMuted(),
                )
                Spacer(Modifier.height(14.dp))
                when {
                    state.storeLoading -> Text(tr("setup.loading"), fontSize = 13.sp, color = webTextMuted())
                    state.storeError != null -> Text(
                        state.storeError!!,
                        fontSize = 13.sp,
                        color = Color(0xFFC90E2E),
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.storeProducts.forEach { product ->
                            ProductRow(
                                product = product,
                                pending = state.storePendingId == product.id,
                                justPurchased = state.storeJustPurchasedId == product.id,
                                onBuy = { scope.launch { state.purchase(product.id) } },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { state.showStore = false }) { Text(tr("common.close")) }
        },
    )
}

@Composable
private fun ProductRow(
    product: WalletProductDto,
    pending: Boolean,
    justPurchased: Boolean,
    onBuy: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, webBorder(), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tokens(product.tokens), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = webText())
                product.badge?.let {
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFFDAF5EC)) {
                        Text(
                            it,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF009969),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text("₩${comma(product.tokens)}", fontSize = 12.sp, color = webTextMuted())
        }
        Surface(
            onClick = onBuy,
            shape = RoundedCornerShape(8.dp),
            color = if (justPurchased) Color(0xFFDAF5EC) else WebMint,
        ) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 9.dp)) {
                Text(
                    if (pending) "…" else if (justPurchased) tr("setup.purchased") else tr("setup.buy"),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (justPurchased) Color(0xFF009969) else Color.White,
                )
            }
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
private fun isDark(): Boolean = isAppDark()
