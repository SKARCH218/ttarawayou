package com.trevit.app.ui

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.AppState
import com.trevit.app.R
import com.trevit.app.Screen
import com.trevit.app.map.GeoProjector
import com.trevit.app.map.LegGeometry
import com.trevit.app.map.stopEmoji
import com.trevit.app.map.stopTypeLabel
import com.trevit.app.won
import com.trevit.shared.StopDto
import kotlinx.coroutines.isActive
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val BASE_SPEED_MPS = 30.0   // 시뮬레이션 기본 속도: 초당 약 30m
private const val ARRIVE_RADIUS_M = 20.0  // 도착 판정 반경
private val SPEED_OPTIONS = listOf(1f to "1×", 3f to "3×", 10f to "10×")

@Composable
fun JourneyScreen(state: AppState, dayIndex: Int) {
    val plan = state.plan ?: run { state.screen = Screen.Setup; return }
    val dayPlan = plan.dayPlans.getOrNull(dayIndex) ?: run { state.screen = Screen.Result; return }
    val stops = dayPlan.stops
    val geoms = remember(dayIndex) {
        dayPlan.legs.mapIndexed { i, leg ->
            LegGeometry(
                leg,
                stops.getOrElse(i) { stops.first() },
                stops.getOrElse(i + 1) { stops.last() },
            )
        }
    }

    if (geoms.isEmpty()) {
        // 이동 구간이 없는 비정상 플랜 — 바로 완료 처리
        LaunchedEffect(Unit) {
            state.onDayCompleted(dayIndex)
            state.screen = Screen.Result
        }
        return
    }

    var legIndex by remember(dayIndex) { mutableIntStateOf(0) }
    var distOnLeg by remember(dayIndex) { mutableDoubleStateOf(0.0) }
    var playing by remember(dayIndex) { mutableStateOf(false) }
    var speedIdx by remember(dayIndex) { mutableIntStateOf(0) }
    var revealStop by remember(dayIndex) { mutableStateOf<StopDto?>(null) }
    var revealedCount by remember(dayIndex) { mutableIntStateOf(1) } // 출발지는 공개
    var completed by remember(dayIndex) { mutableStateOf(false) }

    // ---- 시뮬레이션 틱 ----
    LaunchedEffect(playing, speedIdx, legIndex) {
        if (!playing) return@LaunchedEffect
        var last = -1L
        while (isActive && playing) {
            kotlinx.coroutines.delay(16)
            val now = System.nanoTime()
            if (last < 0) { last = now; continue }
            val dt = (now - last) / 1e9
            last = now
            val geom = geoms[legIndex]
            val speed = BASE_SPEED_MPS * SPEED_OPTIONS[speedIdx].first
            val next = min(distOnLeg + speed * dt, geom.lengthMeters)
            distOnLeg = next
            if (geom.lengthMeters - next <= ARRIVE_RADIUS_M) {
                distOnLeg = geom.lengthMeters
                playing = false
                revealStop = stops.getOrNull(legIndex + 1)
                if (revealStop == null) completed = true
            }
        }
    }

    LaunchedEffect(completed) {
        if (completed) state.onDayCompleted(dayIndex)
    }

    val currentGeom = geoms[legIndex]
    val currentLeg = currentGeom.leg
    val remainMeters = (currentGeom.lengthMeters - distOnLeg).coerceAtLeast(0.0)
    val remainMinutes = ceil(currentLeg.durationMinutes * (remainMeters / currentGeom.lengthMeters))
        .toInt().coerceAtLeast(if (remainMeters > 30) 1 else 0)
    val nextStop = stops.getOrNull(legIndex + 1)

    Box(Modifier.fillMaxSize()) {
        // ================= Canvas 지도 =================
        JourneyMap(
            geoms = geoms,
            stops = stops,
            legIndex = legIndex,
            distOnLeg = distOnLeg,
            revealedCount = revealedCount,
        )

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            // ---- 웹 `.map-topbar` — 뒤로 버튼 + 상태 두 줄 ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapBackButton { state.screen = Screen.Result }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Day ${dayPlan.day} 여정",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = webText(),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "다음 비밀 장소까지 ${formatDistance(remainMeters)} · 약 ${remainMinutes}분",
                        fontSize = 12.sp,
                        color = webTextMuted(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ---- 웹 `.progress-pill` ----
            MapPill(
                "비밀 장소 $revealedCount / ${geoms.size}",
                Modifier.align(Alignment.CenterHorizontally),
            )

            if (currentLeg.mode == "TRANSIT") {
                Spacer(Modifier.height(8.dp))
                MapPill(
                    currentLeg.summary
                        ?: "${currentLeg.boardStop ?: "정류장"} 승차 → ${currentLeg.alightStop ?: "정류장"} 하차",
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                    color = WebOrangeDark,
                )
            }

            Spacer(Modifier.weight(1f))

            // ---- 웹 `.mystery-hint` — 지도 위에 뜨는 안내 ----
            MysteryHint(
                if (playing) "보라색 길을 따라가는 중" else "시뮬레이션을 눌러 길을 따라가세요",
                Modifier.padding(horizontal = 16.dp),
            )

            // ---- 웹 `.map-bottombar` + `.sim-btn` ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SimButton(
                    text = if (playing) "일시정지" else "시뮬레이션",
                    primary = true,
                    enabled = revealStop == null && !completed,
                    modifier = Modifier.weight(1f),
                ) { playing = !playing }
                SPEED_OPTIONS.forEachIndexed { i, (_, label) ->
                    SimButton(label, primary = false, selected = speedIdx == i) { speedIdx = i }
                }
            }
        }

        // ================= 도착 리빌 오버레이 =================
        revealStop?.let { stop ->
            RevealOverlay(
                stop = stop,
                isLast = legIndex == geoms.lastIndex,
                onContinue = {
                    revealStop = null
                    revealedCount++
                    if (legIndex == geoms.lastIndex) {
                        completed = true
                    } else {
                        legIndex++
                        distOnLeg = 0.0
                        playing = true
                    }
                },
            )
        }

        // ================= 완료 오버레이 =================
        if (completed && revealStop == null) {
            CompletionOverlay(state, dayIndex)
        }
    }
}

// ---------------------------------------------------------------------------
// Canvas 지도
// ---------------------------------------------------------------------------
@Composable
private fun JourneyMap(
    geoms: List<LegGeometry>,
    stops: List<StopDto>,
    legIndex: Int,
    distOnLeg: Double,
    revealedCount: Int,
) {
    val colorScheme = MaterialTheme.colorScheme
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "pulseValue",
    )
    val context = androidx.compose.ui.platform.LocalContext.current
    val textPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            // 지도 마커 이모지도 토스페이스 폰트로 (미보유 글리프는 시스템 폴백)
            typeface = runCatching {
                context.resources.getFont(com.trevit.app.R.font.tossface)
            }.getOrNull()
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val allPoints = buildList {
            geoms.forEach { addAll(it.points) }
            stops.forEach { add(it.latitude to it.longitude) }
        }
        val projector = GeoProjector(allPoints, size.width, size.height, 90f)

        // 배경 그리드 (지도 느낌)
        val gridColor = colorScheme.onSurface.copy(alpha = 0.05f)
        var gx = 0f
        while (gx < size.width) {
            drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), 2f)
            gx += 56f
        }
        var gy = 0f
        while (gy < size.height) {
            drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), 2f)
            gy += 56f
        }

        fun drawGeoPath(
            pts: List<Pair<Double, Double>>,
            color: Color,
            widthPx: Float,
            dashed: Boolean = false,
        ) {
            if (pts.size < 2) return
            val path = Path()
            pts.forEachIndexed { i, (lat, lng) ->
                val o = projector.toOffset(lat, lng)
                if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            drawPath(
                path,
                color = color,
                style = Stroke(
                    width = widthPx,
                    cap = StrokeCap.Round,
                    pathEffect = if (dashed) {
                        PathEffect.dashPathEffect(floatArrayOf(16f, 14f))
                    } else null,
                ),
            )
        }

        // 남은 경로 (선명한 브랜드 컬러)
        for (i in legIndex + 1 until geoms.size) {
            drawGeoPath(geoms[i].points, colorScheme.primary, 9f)
        }
        val current = geoms[legIndex]
        val traveledPts = current.subPathTo(distOnLeg)
        val remainingPts = buildList {
            add(current.positionAt(distOnLeg))
            var idx = 0
            while (idx < current.cumulative.size && current.cumulative[idx] <= distOnLeg) idx++
            for (j in idx until current.points.size) add(current.points[j])
        }
        drawGeoPath(remainingPts, colorScheme.primary, 9f)

        // 지나온 경로 (회색 점선)
        for (i in 0 until legIndex) {
            drawGeoPath(geoms[i].points, colorScheme.outline.copy(alpha = 0.65f), 6f, dashed = true)
        }
        drawGeoPath(traveledPts, colorScheme.outline.copy(alpha = 0.65f), 6f, dashed = true)

        // 승차/하차 정류장 마커 (대중교통 구간)
        val leg = current.leg
        if (leg.mode == "TRANSIT") {
            listOf(
                leg.boardLat to leg.boardLng,
                leg.alightLat to leg.alightLng,
            ).forEach { (la, ln) ->
                if (la != null && ln != null) {
                    val o = projector.toOffset(la, ln)
                    drawCircle(colorScheme.secondary, 11f, o)
                    drawCircle(colorScheme.surface, 5f, o)
                }
            }
        }

        // 정차 지점 마커
        stops.forEachIndexed { i, stop ->
            val o = projector.toOffset(stop.latitude, stop.longitude)
            val revealed = i < revealedCount
            if (revealed) {
                drawCircle(colorScheme.secondaryContainer, 26f, o)
                drawCircle(colorScheme.secondary, 26f, o, style = Stroke(4f))
                drawContext.canvas.nativeCanvas.drawText(
                    stopEmoji(stop.type),
                    o.x,
                    o.y + 10f,
                    textPaint.apply { textSize = 28f },
                )
            } else {
                drawCircle(colorScheme.surfaceVariant, 24f, o)
                drawCircle(colorScheme.outline, 24f, o, style = Stroke(3f))
                drawContext.canvas.nativeCanvas.drawText(
                    "?",
                    o.x,
                    o.y + 11f,
                    textPaint.apply {
                        textSize = 30f
                        color = android.graphics.Color.argb(
                            255,
                            (colorScheme.onSurfaceVariant.red * 255).toInt(),
                            (colorScheme.onSurfaceVariant.green * 255).toInt(),
                            (colorScheme.onSurfaceVariant.blue * 255).toInt(),
                        )
                        isFakeBoldText = true
                    },
                )
            }
        }

        // 현재 위치 마커 (펄스)
        val (curLat, curLng) = current.positionAt(distOnLeg)
        val curOffset = projector.toOffset(curLat, curLng)
        drawCircle(
            colorScheme.tertiary.copy(alpha = (1f - pulse) * 0.35f),
            18f + pulse * 30f,
            curOffset,
        )
        drawCircle(colorScheme.tertiary, 14f, curOffset)
        drawCircle(Color.White, 14f, curOffset, style = Stroke(4f))
    }
}

// ---------------------------------------------------------------------------
// 지도 화면 조각 (웹 `.map-back` / `.progress-pill` / `.mystery-hint` / `.sim-btn`)
// ---------------------------------------------------------------------------

/** 웹 `.map-back` — 40dp 흰 사각 버튼 */
@Composable
private fun MapBackButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = webSurface(),
        border = BorderStroke(1.dp, webBorderStrong()),
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_chevron_left),
                contentDescription = "뒤로",
                tint = WebMintDeep,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 웹 `.progress-pill` — 지도 위에 떠 있는 흰 알약 */
@Composable
private fun MapPill(text: String, modifier: Modifier = Modifier, color: Color = WebMintDeep) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = webSurface(),
        border = BorderStroke(1.dp, webBorderStrong()),
        shadowElevation = 2.dp,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}

/** 웹 `.mystery-hint` — 지도 하단의 반투명 안내 상자 */
@Composable
private fun MysteryHint(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = webSurface().copy(alpha = 0.96f),
        border = BorderStroke(1.dp, webBorderStrong()),
        shadowElevation = 4.dp,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = webTextLabel(),
        )
    }
}

/** 웹 `.sim-btn` / `.sim-btn.primary` */
@Composable
private fun SimButton(
    text: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = when {
            primary && enabled -> Color.Transparent
            selected -> webChipOnFill()
            else -> webSurface()
        },
        border = if (primary) null else BorderStroke(
            1.5.dp,
            if (selected) webChipOnBorder() else webBorderStrong(),
        ),
    ) {
        Box(
            Modifier
                .thenIf(primary) {
                    Modifier.background(
                        if (enabled) {
                            Brush.linearGradient(listOf(WebMint, WebMintDeep))
                        } else {
                            Brush.linearGradient(listOf(webFill(), webFill()))
                        },
                        RoundedCornerShape(12.dp),
                    )
                }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    primary && enabled -> Color.White
                    primary -> webTextDim()
                    selected -> webChipOnText()
                    else -> webTextChip()
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 도착 리빌 오버레이 (컨페티 + 카드) — 웹 `.reveal-overlay` / `.reveal-card`
// ---------------------------------------------------------------------------
@Composable
private fun RevealOverlay(stop: StopDto, isLast: Boolean, onContinue: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF262D2E).copy(alpha = 0.55f))
            .padding(26.dp),
        contentAlignment = Alignment.Center,
    ) {
        ConfettiCanvas(key = stop)
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(initialScale = 0.7f) + fadeIn(),
        ) {
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = webSurface(),
                border = BorderStroke(1.dp, webBorder()),
                shadowElevation = 16.dp,
            ) {
                Column(
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 30.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 웹 `.reveal-card .emoji { font-size: 52px }` — 이모지만 토스페이스로
                    Text(
                        stopEmoji(stop.type),
                        fontSize = 52.sp,
                        lineHeight = 60.sp,
                        fontFamily = TossFaceFontFamily,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (isLast) "오늘의 여정 완료! 마지막 장소는…" else "도착! 이곳은…",
                        fontSize = 12.sp,
                        letterSpacing = 3.sp,
                        color = webTextFaint(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stop.name ?: "미스터리 장소",
                        fontSize = 24.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = webText(),
                        textAlign = TextAlign.Center,
                    )
                    stop.address?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, fontSize = 13.sp, color = webTextMuted(), textAlign = TextAlign.Center)
                    }
                    stop.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            it,
                            fontSize = 13.5.sp,
                            lineHeight = 22.sp,
                            color = webTextLabel(),
                            textAlign = TextAlign.Center,
                        )
                    }
                    // 웹 `.reveal-card .meta` — 가운데 정렬 민트 굵은 글씨
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(
                            stopTypeLabel(stop.type),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WebMint,
                        )
                        if (stop.rating > 0) {
                            Text(
                                "★ %.1f".format(stop.rating),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WebMint,
                            )
                        }
                        if (stop.cost > 0) {
                            Text(
                                won(stop.cost),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WebMint,
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    PrimaryCta(
                        text = if (isLast) "여정 마치기" else "다음 비밀 장소로 →",
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val xFrac: Float,
    val delay: Float,
    val speed: Float,
    val wobble: Float,
    val rotSpeed: Float,
    val colorIndex: Int,
    val size: Float,
)

@Composable
private fun ConfettiCanvas(key: Any) {
    val colors = listOf(MysteryPurple, MysteryPurpleLight, MintAccent, SunsetOrange, Color(0xFFFFD54F))
    val particles = remember(key) {
        List(70) {
            ConfettiParticle(
                xFrac = Random.nextFloat(),
                delay = Random.nextFloat() * 0.3f,
                speed = 0.7f + Random.nextFloat() * 0.6f,
                wobble = 3f + Random.nextFloat() * 7f,
                rotSpeed = 180f + Random.nextFloat() * 540f,
                colorIndex = Random.nextInt(colors.size),
                size = 10f + Random.nextFloat() * 14f,
            )
        }
    }
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(3000, easing = LinearEasing))
    }

    Canvas(Modifier.fillMaxSize()) {
        val p = progress.value
        particles.forEach { particle ->
            val t = ((p - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
            if (t <= 0f || t >= 1f) return@forEach
            val x = particle.xFrac * size.width +
                sin(t * particle.wobble * Math.PI).toFloat() * 46f
            val y = -40f + t * particle.speed * (size.height + 80f)
            rotate(t * particle.rotSpeed, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[particle.colorIndex].copy(alpha = 1f - t * 0.4f),
                    topLeft = Offset(x - particle.size / 2, y - particle.size / 3),
                    size = androidx.compose.ui.geometry.Size(particle.size, particle.size * 0.65f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 일차 완료 / 여행 완료 오버레이
// ---------------------------------------------------------------------------
@Composable
private fun CompletionOverlay(state: AppState, dayIndex: Int) {
    val plan = state.plan ?: return
    val dayPlan = plan.dayPlans[dayIndex]
    val hasNextDay = dayIndex + 1 < plan.dayPlans.size

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF262D2E).copy(alpha = 0.55f))
            .padding(26.dp),
        contentAlignment = Alignment.Center,
    ) {
        ConfettiCanvas(key = "done-$dayIndex")
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = webSurface(),
            border = BorderStroke(1.dp, webBorder()),
            shadowElevation = 16.dp,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp, vertical = 30.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (hasNextDay) "Day ${dayPlan.day} 완료" else "여행 완료",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = webText(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "오늘 쓴 비용 ${won(dayPlan.dayCost)}",
                    fontSize = 13.sp,
                    color = webTextMuted(),
                )
                Spacer(Modifier.height(14.dp))
                DashedDivider()
                Spacer(Modifier.height(6.dp))
                dayPlan.stops.forEach { stop ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stop.name ?: "???",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = webTextLabel(),
                            modifier = Modifier.weight(1f),
                        )
                        if (stop.cost > 0) {
                            Text(
                                won(stop.cost),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WebMint,
                            )
                        }
                    }
                }
                if (!hasNextDay) {
                    Spacer(Modifier.height(6.dp))
                    DashedDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "총 비용 ${won(plan.totalCost)} · 남는 예산 ${won(plan.remainingBudget)}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = webText(),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(22.dp))
                PrimaryCta(
                    text = if (hasNextDay) "Day ${dayPlan.day + 1} 시작" else "플랜으로 돌아가기",
                    onClick = {
                        state.screen = if (hasNextDay) {
                            Screen.Journey(dayIndex + 1)
                        } else {
                            Screen.Result
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
