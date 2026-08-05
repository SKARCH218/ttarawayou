package com.trevit.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.trevit.app.resources.*

/**
 * 웹 데모(frontend/public/css/style.css)의 시각 규칙을 Compose로 옮긴 공용 컴포넌트.
 *
 * 수치·색은 전부 웹 CSS와 ogq-tokens.css 에서 그대로 가져왔다.
 * 화면 코드에서 새 값을 만들지 말고 여기 있는 것을 쓴다 — 두 쪽이 어긋나는 걸 막는 유일한 장치다.
 * 다크 모드 대응도 여기서 끝내므로 화면 코드에 다크 분기를 다시 쓸 필요가 없다.
 */

// ─────────────────────────────────────────────────────────────
// OGQ 색 토큰 (css/ogq-tokens.css)
// ─────────────────────────────────────────────────────────────
/** primary-500 — 브랜드 민트 */
val WebMint = Color(0xFF00C389)

/** primary-700 — CTA 그라데이션의 끝 */
val WebMintDeep = Color(0xFF00B57F)

/** primary-050 — 선택된 칩 배경 */
val WebMintPale = Color(0xFFDAF5EC)


/** 선택된 칩 글자 (primary-700 계열) */
private val WebMintText = Color(0xFF00805C)

/** secondary-600 — 타이틀 그라데이션의 시작 */
val WebPurple = Color(0xFF703FE4)

/** secondary-300 — 다크 모드에서 secondary-600 대신 쓰는 밝은 보라 */
private val WebPurpleLight = Color(0xFFA78FEC)

/** warning-default — 토큰 잔액 */
val WebOrange = Color(0xFFFF9800)

/** warning-dark — 보유 토큰 강조 */
val WebOrangeDark = Color(0xFFE65100)

/** accent-blue-500 — 교통비 바 */
val WebBlue = Color(0xFF1490EB)

/** error-default — 입력 오류·인증 실패 (웹 `.error-box`, `.field-hint.error`) */
val WebError = Color(0xFFE21235)

/** warning-default — 비밀번호 강도 중간 단계 */
val WebWarning = Color(0xFFFF9800)

// ── 무채색 (라이트 기준값. 다크는 아래 web*() 함수가 바꿔 준다) ──
private val Mono030 = Color(0xFFF9FBFB)
private val Mono050 = Color(0xFFF3F6F6)
private val Mono080 = Color(0xFFEDF1F1)
private val Mono100 = Color(0xFFD8DFDF)
private val Mono300 = Color(0xFF92A5A8)
private val Mono400 = Color(0xFF899C9F)
private val Mono500 = Color(0xFF7C9295)
private val Mono600 = Color(0xFF6C7E84)
private val Mono700 = Color(0xFF57676B)
private val Mono800 = Color(0xFF465357)
private val Mono990 = Color(0xFF262D2E)

/** 웹 `body` 배경 (mono-030) */
@Composable
@ReadOnlyComposable
fun webBg(): Color = if (isSystemInDarkTheme()) Color(0xFF181D1E) else Mono030

/** 카드·시트의 흰 면 (mono-000) */
@Composable
@ReadOnlyComposable
fun webSurface(): Color = if (isSystemInDarkTheme()) Color(0xFF202627) else Color.White

/** 옅은 채움 (mono-050) — 칩·스테퍼 버튼 배경 */
@Composable
@ReadOnlyComposable
fun webFill(): Color = if (isSystemInDarkTheme()) Color(0xFF2C3435) else Mono050

/** 얇은 경계선 (mono-080) — 카드 테두리·점선 구분선 */
@Composable
@ReadOnlyComposable
fun webBorder(): Color = if (isSystemInDarkTheme()) Color(0xFF394344) else Mono080

/** 또렷한 경계선 (mono-100) — 입력·칩 테두리 */
@Composable
@ReadOnlyComposable
fun webBorderStrong(): Color = if (isSystemInDarkTheme()) Color(0xFF44504F) else Mono100

/** 본문 진한 글자 (mono-990) */
@Composable
@ReadOnlyComposable
fun webText(): Color = if (isSystemInDarkTheme()) Color(0xFFEDF1F1) else Mono990

/** 칩 글자 (mono-800) */
@Composable
@ReadOnlyComposable
fun webTextChip(): Color = if (isSystemInDarkTheme()) Color(0xFFD3DCDC) else Mono800

/** 라벨 (mono-700) */
@Composable
@ReadOnlyComposable
fun webTextLabel(): Color = if (isSystemInDarkTheme()) Color(0xFFB6C2C2) else Mono700

/** 보조 설명 (mono-600) */
@Composable
@ReadOnlyComposable
fun webTextMuted(): Color = if (isSystemInDarkTheme()) Color(0xFFA7B6B9) else Mono600

/** 흐린 설명 (mono-500) */
@Composable
@ReadOnlyComposable
fun webTextFaint(): Color = if (isSystemInDarkTheme()) Color(0xFF92A5A8) else Mono500

/** 가장 흐린 글자·단위 (mono-400) */
@Composable
@ReadOnlyComposable
fun webTextDim(): Color = if (isSystemInDarkTheme()) Color(0xFF8A9C9F) else Mono400

/** 화살표 등 장식 (mono-300) */
@Composable
@ReadOnlyComposable
fun webTextDecor(): Color = if (isSystemInDarkTheme()) Color(0xFF6E7E80) else Mono300

/** 선택된 칩 배경 (primary-050) */
@Composable
@ReadOnlyComposable
fun webChipOnFill(): Color = if (isSystemInDarkTheme()) Color(0xFF10453A) else WebMintPale

/** 선택된 칩 글자 */
@Composable
@ReadOnlyComposable
fun webChipOnText(): Color = if (isSystemInDarkTheme()) Color(0xFF7FDEC1) else WebMintText

/** 선택된 칩 테두리 (primary-300/400) */
@Composable
@ReadOnlyComposable
fun webChipOnBorder(): Color = if (isSystemInDarkTheme()) Color(0xFF2E7C66) else Color(0xFF28C799)

// ─────────────────────────────────────────────────────────────
// 모양 (ogq-radius-*)
// ─────────────────────────────────────────────────────────────
private val ShapeLg = RoundedCornerShape(12.dp)
private val CtaShape = RoundedCornerShape(16.dp)      // radius-xl
private val CardShape = RoundedCornerShape(20.dp)     // radius-2xl
private val PillShape = RoundedCornerShape(50)        // radius-full

// ─────────────────────────────────────────────────────────────
// 화면 골격
// ─────────────────────────────────────────────────────────────

/**
 * 웹 `.screen` — `padding: calc(24px + safe-area) 22px 32px`.
 * 웹은 CTA까지 이 흐름 안에서 함께 스크롤되므로 앱도 bottomBar를 쓰지 않는다.
 */
@Composable
fun WebScreen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier
            .fillMaxSize()
            .background(webBg())
            .statusBarsPadding()
            .thenIf(scrollable) { Modifier.verticalScroll(scroll) }
            .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 32.dp),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/**
 * 웹 `.title` 의 `linear-gradient(120deg, #703fe4, #00c389)` — 30px bold, line-height 1.35.
 * 각 화면의 가장 큰 제목 하나에만 쓴다.
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

@Composable
fun GradientTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 30.sp,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            brush = titleBrush(),
            fontSize = fontSize,
            lineHeight = fontSize * 1.35f,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.01f).em,
        ),
        textAlign = textAlign,
    )
}

/** 웹 `.subtitle` — 14px, mono-600, 가운데, line-height 1.6 */
@Composable
fun WebSubtitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = webTextMuted(),
    fontSize: TextUnit = 14.sp,
) {
    Text(
        text,
        modifier = modifier.fillMaxWidth(),
        fontSize = fontSize,
        lineHeight = fontSize * 1.6f,
        color = color,
        textAlign = TextAlign.Center,
    )
}

/** 웹 `.card` — 흰 배경 + 1px mono-080 테두리 + radius 20 + shadow-sm */
@Composable
fun TrevitCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = webSurface(),
        border = BorderStroke(1.dp, webBorder()),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

/** 웹 `.field label` — 14px semibold mono-700, 아래 8dp */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = webTextLabel(),
    )
}

// ─────────────────────────────────────────────────────────────
// 버튼
// ─────────────────────────────────────────────────────────────

/** 웹 `.btn-primary` 의 `linear-gradient(120deg,#00c389,#00b57f)` */
private fun ctaBrush(): Brush = Brush.linearGradient(listOf(WebMint, WebMintDeep))

/**
 * 웹 `.btn-primary` — 민트 그라데이션 + 아래로 번지는 민트 글로우
 * (`box-shadow: 0 8px 24px rgba(0,195,137,.30)`), 세로 패딩 17, radius 16, 16.5px bold.
 */
@Composable
fun PrimaryCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val disabledFill = webFill()
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
        contentColor = if (enabled) Color.White else webTextDim(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .thenIf(enabled) { Modifier.background(ctaBrush(), CtaShape) }
                .thenIf(!enabled) { Modifier.background(disabledFill, CtaShape) }
                .padding(vertical = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 웹 `.btn-ghost` — 투명 배경 + 1.5px mono-100 테두리, 세로 패딩 13, 14px bold */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ShapeLg,
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, webBorderStrong()),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = webTextMuted())
        }
    }
}

/** 웹 `.ask-back` — 투명 + 1.5px 테두리, 패딩 13/18, 14px bold mono-500 */
@Composable
fun WebBackButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ShapeLg,
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, webBorderStrong()),
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 13.dp)) {
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = webTextFaint())
        }
    }
}

/**
 * 웹 `.ask-opt` / `.chip` — 라운드 999 알약 선택지.
 * [compact] 는 설정 화면의 작은 `.chip`(8/14, 13px), 기본은 질문 화면의 `.ask-opt`(13/20, 15px).
 */
@Composable
fun WebChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val border = if (selected) webChipOnBorder() else webBorderStrong()
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = PillShape,
        color = if (selected) webChipOnFill() else webFill(),
        border = BorderStroke(if (compact) 1.dp else 1.5.dp, border),
    ) {
        Box(
            Modifier.padding(
                horizontal = if (compact) 14.dp else 20.dp,
                vertical = if (compact) 8.dp else 13.dp,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                fontSize = if (compact) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) webChipOnText() else if (compact) webTextLabel() else webTextChip(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 진행 표시 · 바
// ─────────────────────────────────────────────────────────────

/**
 * 웹 `.ask-progress` — 질문 수만큼 잘린 3dp 막대. 현재 질문까지 민트로 채운다.
 */
@Composable
fun SegmentedProgress(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(PillShape)
                    .background(if (i <= current) WebMint else webBorderStrong()),
            )
        }
    }
}

/**
 * 웹 `.bar-track` / `.bar-fill` — 예산 대비 사용액을 보여주는 8dp 진행 바.
 * 항목마다 색이 다르므로(숙박 보라·관광 민트·식비 오렌지·교통 파랑) 색을 받는다.
 */
@Composable
fun BudgetBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val fill = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(PillShape)
            .background(webBorder()),
    ) {
        if (fill > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .background(color, PillShape),
            )
        }
    }
}

/**
 * 웹 `.budget-slider` — 10dp 트랙(채움 민트 / 남은 곳 mono-080)에
 * 흰 속을 민트 테두리로 두른 26dp 손잡이.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val track = webBorder()
    val thumbFill = webSurface()
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        track = { sliderState ->
            val span = sliderState.valueRange.endInclusive - sliderState.valueRange.start
            val fraction =
                if (span > 0f) (sliderState.value - sliderState.valueRange.start) / span else 0f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(PillShape)
                    .background(track),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(WebMint, PillShape),
                )
            }
        },
        thumb = {
            Box(
                Modifier
                    .size(26.dp)
                    .shadow(4.dp, CircleShape, spotColor = WebMint, ambientColor = WebMint)
                    .background(thumbFill, CircleShape)
                    .border(4.dp, WebMint, CircleShape),
            )
        },
    )
}

/** 웹 `.cost-row + .cost-row` 의 `border-top: 1px dashed mono-080` */
@Composable
fun DashedDivider(modifier: Modifier = Modifier) {
    val color = webBorder()
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                val dash = 3.dp.toPx()
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                )
            },
    )
}

/**
 * 웹 인트로·플랜 배지의 `box-shadow: 0 0 40px rgba(0,195,137,.35)` — 방사형 민트 글로우.
 * [size] 는 글로우 원의 지름.
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

// ─────────────────────────────────────────────────────────────
// 스테퍼 (기간 · 인원)
// ─────────────────────────────────────────────────────────────

/**
 * 웹 `.stepper` — [−] [값/단위] [＋].
 * 버튼은 46dp 정사각 radius 12, 값은 20px bold + 12px 단위.
 */
@Composable
fun WebStepper(
    value: Int,
    unit: String,
    onChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepperButton(Res.drawable.ic_remove_minus, "감소", value > range.first) { onChange(value - 1) }
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("$value", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = webText())
            Spacer(Modifier.height(2.dp))
            Text(
                unit,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = webTextDim(),
            )
        }
        StepperButton(Res.drawable.ic_add_plus, "증가", value < range.last) { onChange(value + 1) }
    }
}

/** 웹 `.stepper button` — 46dp 정사각, mono-050 배경, 안에는 디자인 시스템 PNG 아이콘 */
@Composable
private fun StepperButton(
    icon: org.jetbrains.compose.resources.DrawableResource,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(46.dp),
        shape = ShapeLg,
        color = webFill(),
        border = BorderStroke(1.dp, webBorder()),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painterResource(icon),
                contentDescription = description,
                tint = if (enabled) webTextChip() else webTextDecor(),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 플랜 화면 조각
// ─────────────────────────────────────────────────────────────

/** 웹 `.cost-row` — 왼쪽 라벨(14px mono-600) / 오른쪽 값 */
@Composable
fun CostRow(
    label: String,
    value: String,
    valueColor: Color,
    valueSize: TextUnit = 19.sp,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = webTextMuted())
        Text(value, fontSize = valueSize, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

/**
 * 웹 `.day-btn` — 46dp 그라데이션 배지 + 제목/설명 + 화살표.
 * [locked] 면 웹처럼 반투명으로 눌리지 않는다.
 */
@Composable
fun DayButton(
    badge: String,
    title: String,
    info: String,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .thenIf(!locked) {
                Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
            },
        shape = CtaShape,
        color = webSurface(),
        border = BorderStroke(1.5.dp, webBorder()),
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(ShapeLg)
                    .background(
                        if (locked) {
                            Brush.linearGradient(
                                listOf(
                                    WebMint.copy(alpha = 0.35f),
                                    WebMintDeep.copy(alpha = 0.35f),
                                ),
                            )
                        } else {
                            ctaBrush()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = webText().copy(alpha = if (locked) 0.5f else 1f),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    info,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = webTextFaint().copy(alpha = if (locked) 0.6f else 1f),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("›", fontSize = 20.sp, color = webTextDecor())
        }
    }
}

/** 조건부 Modifier 연결 — `if (x) Modifier.a() else Modifier` 반복을 줄인다 */
inline fun Modifier.thenIf(condition: Boolean, block: () -> Modifier): Modifier =
    if (condition) this.then(block()) else this
