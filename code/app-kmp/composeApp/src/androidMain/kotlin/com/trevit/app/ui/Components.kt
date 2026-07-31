package com.trevit.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 웹 데모(frontend/public/css/style.css)의 시각 언어를 Compose로 옮긴 공용 컴포넌트.
 *
 * 웹에서 반복되는 네 가지 — 그라데이션 타이틀(.title), 글로우 CTA(.btn-primary),
 * 카드(.card), 민트 선택 칩(.chip.on) — 을 여기서 한 번만 정의해 화면들이 공유한다.
 * 라이트/다크 각각의 값을 담고 있으므로 화면 코드에서 다크 분기를 다시 쓸 필요가 없다.
 */

// ── 웹에서 그대로 가져온 색 토큰 ──
/** primary-500 — CTA·바 그라데이션의 시작 */
val WebMint = Color(0xFF00C389)

/** primary-700 — CTA·바 그라데이션의 끝 */
val WebMintDeep = Color(0xFF00B57F)

/** primary-050 — 선택된 칩 배경 */
val WebMintPale = Color(0xFFDAF5EC)

/** 선택된 칩 글자 */
private val WebMintText = Color(0xFF00805C)

/** secondary-600 — 타이틀 그라데이션의 시작 */
val WebPurple = Color(0xFF703FE4)

/** secondary-300 — 다크 모드에서 secondary-600 대신 쓰는 밝은 보라 */
private val WebPurpleLight = Color(0xFFA78FEC)

/** mono-080 — 카드 테두리 */
private val WebCardBorder = Color(0xFFEDF1F1)

private val CtaShape = RoundedCornerShape(16.dp)
private val CardShape = RoundedCornerShape(20.dp)
private val BarShape = RoundedCornerShape(50)

/**
 * 웹 `.title` 의 `linear-gradient(120deg, #703fe4, #00c389)`.
 * 다크 모드에서는 보라가 배경에 묻히므로 한 단계 밝은 값으로 바꾼다.
 */
@Composable
@ReadOnlyComposable
fun titleBrush(): Brush {
    val dark = isSystemInDarkTheme()
    return Brush.linearGradient(
        listOf(
            if (dark) WebPurpleLight else WebPurple,
            if (dark) Color(0xFF32D29D) else WebMint,
        ),
    )
}

/** 웹 `.btn-primary` 의 `linear-gradient(120deg,#00c389,#00b57f)` */
private fun ctaBrush(): Brush = Brush.linearGradient(listOf(WebMint, WebMintDeep))

/** 웹 `.card` 의 `1px solid #edf1f1` — 다크에서는 표면보다 살짝 밝은 선 */
@Composable
@ReadOnlyComposable
private fun cardBorderColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF394344) else WebCardBorder

/**
 * 웹 `.title` 을 그대로 옮긴 그라데이션 제목.
 * 각 화면의 가장 큰 제목 하나에만 쓴다 — 여러 번 쓰면 웹보다 요란해진다.
 */
@Composable
fun GradientTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    textAlign: TextAlign = TextAlign.Center,
) {
    val brush = titleBrush()
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(brush = brush, fontWeight = FontWeight.Bold),
        textAlign = textAlign,
    )
}

/**
 * 웹 `.btn-primary` — 민트 그라데이션 + 아래로 번지는 민트 글로우.
 * Button 대신 Surface 를 쓴 건 컨테이너를 투명하게 두고 그라데이션을 직접 칠하기 위해서다.
 */
@Composable
fun PrimaryCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val disabledFill = MaterialTheme.colorScheme.surfaceVariant
    val glow = modifier.thenIf(enabled) {
        Modifier.shadow(
            elevation = 16.dp,
            shape = CtaShape,
            spotColor = WebMint,
            ambientColor = WebMint,
        )
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = glow,
        shape = CtaShape,
        color = Color.Transparent,
        contentColor = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 54.dp)
                .thenIf(enabled) { Modifier.background(ctaBrush(), CtaShape) }
                .thenIf(!enabled) { Modifier.background(disabledFill, CtaShape) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** 웹 `.card` — 흰 배경 + 1px 테두리 + radius 20 + 아주 옅은 그림자 */
@Composable
fun TrevitCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, cardBorderColor()),
        content = content,
    )
}

/**
 * 웹 `.chip.on` — 선택 시 민트 계열로 물든다.
 * Material 기본값은 선택 상태가 보라(secondaryContainer)라 브랜드와 어긋난다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun trevitChipColors(): SelectableChipColors {
    val dark = isSystemInDarkTheme()
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = if (dark) Color(0xFF10453A) else WebMintPale,
        selectedLabelColor = if (dark) Color(0xFF7FDEC1) else WebMintText,
    )
}

/** 세그먼트 버튼(기간·MBTI)의 선택 상태도 칩과 같은 민트로 맞춘다 */
@Composable
fun trevitSegmentedColors(): SegmentedButtonColors {
    val dark = isSystemInDarkTheme()
    return SegmentedButtonDefaults.colors(
        activeContainerColor = if (dark) Color(0xFF10453A) else WebMintPale,
        activeContentColor = if (dark) Color(0xFF7FDEC1) else WebMintText,
        activeBorderColor = if (dark) Color(0xFF2E7C66) else Color(0xFF66DCB5),
    )
}

/** 선택 시 민트 테두리 — `.chip.on` 의 `border-color: primary-300` */
@Composable
fun trevitChipBorder(selected: Boolean): BorderStroke {
    val dark = isSystemInDarkTheme()
    return if (selected) {
        BorderStroke(1.5.dp, if (dark) Color(0xFF2E7C66) else Color(0xFF66DCB5))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * 웹 `.bar-track` / `.bar-fill` — 예산 대비 사용액을 보여주는 가는 진행 바.
 * 예산을 넘으면 민트 대신 경고색으로 칠해 한눈에 구분되게 한다.
 */
@Composable
fun BudgetBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    overBudget: Boolean = false,
) {
    val fill = fraction.coerceIn(0f, 1f)
    val brush = if (overBudget) {
        Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFE65100)))
    } else {
        ctaBrush()
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(BarShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (fill > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .shadow(
                        elevation = 6.dp,
                        shape = BarShape,
                        spotColor = if (overBudget) Color(0xFFFF9800) else WebMint,
                        ambientColor = if (overBudget) Color(0xFFFF9800) else WebMint,
                    )
                    .background(brush, BarShape),
            )
        }
    }
}

/**
 * 웹 인트로의 `box-shadow: 0 0 40px rgba(0,195,137,.35)` — 로고 뒤에 깔리는 방사형 글로우.
 * [size] 는 글로우 원의 지름이며 로고보다 넉넉히 크게 잡는다.
 */
@Composable
fun MintGlow(size: Dp, modifier: Modifier = Modifier, alpha: Float = 0.30f) {
    Box(
        modifier
            .size(size)
            .background(
                Brush.radialGradient(
                    listOf(WebMint.copy(alpha = alpha), WebMint.copy(alpha = 0f)),
                ),
            ),
    )
}

/**
 * 질문 화면 이모지 뒤에 까는 옅은 민트 후광.
 * 이모지 위아래 여백이 넓어 휑해 보이던 곳에 시선을 모아 준다.
 */
@Composable
fun EmojiHalo(
    size: Dp = 180.dp,
    content: @Composable () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        val dark = isSystemInDarkTheme()
        Box(
            Modifier
                .size(size)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (dark) WebMint.copy(alpha = 0.14f) else WebMintPale,
                            if (dark) WebMint.copy(alpha = 0f) else WebMintPale.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
        content()
    }
}

/** 조건부 Modifier 연결 — `if (x) Modifier.a() else Modifier` 반복을 줄인다 */
inline fun Modifier.thenIf(condition: Boolean, block: () -> Modifier): Modifier =
    if (condition) this.then(block()) else this
