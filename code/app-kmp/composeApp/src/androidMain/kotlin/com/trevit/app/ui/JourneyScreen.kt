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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
            // ---- 상단 안내 ----
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "DAY ${dayPlan.day} · ${legIndex + 1}/${geoms.size} 구간",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "???까지 " +
                                    formatDistance(remainMeters) +
                                    " · 약 ${remainMinutes}분",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        IconButton(onClick = { state.screen = Screen.Result }) {
                            Icon(painterResource(R.drawable.ic_close_md), contentDescription = "나가기")
                        }
                    }
                    if (currentLeg.mode == "TRANSIT") {
                        Spacer(Modifier.height(8.dp))
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(10.dp),
                        ) {
                            Text(
                                (currentLeg.summary
                                    ?: "${currentLeg.boardStop ?: "정류장"} 승차 → ${currentLeg.alightStop ?: "정류장"} 하차"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            currentLeg.steps?.takeIf { it.isNotEmpty() }?.let { steps ->
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    steps.joinToString("  →  ") { it.description ?: "" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                        .copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ---- 하단 컨트롤 ----
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { playing = !playing },
                        enabled = revealStop == null && !completed,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            if (playing) "일시정지" else "시뮬레이션",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    SPEED_OPTIONS.forEachIndexed { i, (_, label) ->
                        FilterChip(
                            selected = speedIdx == i,
                            onClick = { speedIdx = i },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
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
// 도착 리빌 오버레이 (컨페티 + 카드)
// ---------------------------------------------------------------------------
@Composable
private fun RevealOverlay(stop: StopDto, isLast: Boolean, onContinue: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        ConfettiCanvas(key = stop)
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(initialScale = 0.7f) + fadeIn(),
        ) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "장소 공개",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stopEmoji(stop.type),
                        fontSize = 54.sp,
                        fontFamily = TossFaceFontFamily, // 이모지만 토스페이스로
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stop.name ?: "미스터리 장소",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stopTypeLabel(stop.type) +
                            (if (stop.rating > 0) "  ·  ★ %.1f".format(stop.rating) else ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (stop.cost > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            won(stop.cost),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    stop.address?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    stop.description?.let {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(10.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            if (isLast) "여행 요약" else "다음",
                            fontWeight = FontWeight.Bold,
                        )
                    }
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
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        ConfettiCanvas(key = "done-$dayIndex")
        Card(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (hasNextDay) "DAY ${dayPlan.day} 완료" else "여행 완료",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "오늘 쓴 돈 ${won(dayPlan.dayCost)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                dayPlan.stops.forEach { stop ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stop.name ?: "???",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (stop.cost > 0) {
                            Text(
                                won(stop.cost),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
                if (!hasNextDay) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "총 비용 ${won(plan.totalCost)} · 남은 예산 ${won(plan.remainingBudget)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        state.screen = if (hasNextDay) {
                            Screen.Journey(dayIndex + 1)
                        } else {
                            Screen.Result
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        if (hasNextDay) "DAY ${dayPlan.day + 1} 시작" else "플랜으로 돌아가기",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
