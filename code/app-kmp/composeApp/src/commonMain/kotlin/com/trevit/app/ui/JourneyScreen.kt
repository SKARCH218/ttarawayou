package com.trevit.app.ui

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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.AppState
import com.trevit.app.resources.*
import com.trevit.app.Screen
import com.trevit.app.map.LegGeometry
import com.trevit.app.map.MapCamera
import com.trevit.app.map.TILE_SIZE
import com.trevit.app.map.TileKey
import com.trevit.app.map.TransitGuide
import com.trevit.app.map.TransitPhase
import com.trevit.app.map.haversineMeters
import com.trevit.app.map.locationUpdates
import com.trevit.app.map.stopEmoji
import com.trevit.app.map.stopTypeLabel
import com.trevit.app.oneDecimal
import com.trevit.app.i18n.AppLanguage
import com.trevit.app.i18n.LocalLanguage
import com.trevit.app.i18n.tokens
import com.trevit.app.i18n.tr
import com.trevit.app.i18n.translate
import com.trevit.app.voice.speak
import com.trevit.app.voice.spokenDistance
import com.trevit.app.voice.stopSpeaking
import com.trevit.shared.StopDto
import com.trevit.shared.TileFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

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
    var liveMode by remember(dayIndex) { mutableStateOf(false) }
    var liveFix by remember(dayIndex) { mutableStateOf<Pair<Double, Double>?>(null) }

    // ---- 실시간 모드: 실제 GPS 위치를 경로에 투영해 진행 ----
    LaunchedEffect(liveMode, legIndex, revealStop, completed) {
        if (!liveMode || revealStop != null || completed) return@LaunchedEffect
        locationUpdates().collect { (lat, lng) ->
            liveFix = lat to lng
            val geom = geoms[legIndex]
            // GPS 흔들림으로 뒤로 튀지 않도록 앞으로만 진행
            distOnLeg = max(distOnLeg, geom.project(lat, lng))
            val (destLat, destLng) = geom.points.last()
            if (revealStop == null && haversineMeters(lat, lng, destLat, destLng) <= ARRIVE_RADIUS_M) {
                distOnLeg = geom.lengthMeters
                revealStop = stops.getOrNull(legIndex + 1)
                if (revealStop == null) completed = true
            }
        }
    }

    // ---- 시뮬레이션 틱 ----
    LaunchedEffect(playing, speedIdx, legIndex, liveMode) {
        if (!playing || liveMode) return@LaunchedEffect
        var last: TimeSource.Monotonic.ValueTimeMark? = null
        while (isActive && playing) {
            kotlinx.coroutines.delay(16)
            val now = TimeSource.Monotonic.markNow()
            val prev = last
            last = now
            if (prev == null) continue
            val dt = (now - prev).toDouble(DurationUnit.SECONDS)
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

    // 대중교통 구간: 지금 정류장으로 걷는 중인지, 타고 가는 중인지, 내려서 걷는 중인지.
    // 버스 번호·하차 정류장은 승차 정류장에 도착해야 보여 준다 (그 전엔 정류장 이름만).
    val transitGuide = remember(dayIndex, legIndex) {
        if (currentLeg.mode == "TRANSIT") TransitGuide(currentGeom) else null
    }
    val transitPhase = transitGuide?.phaseAt(distOnLeg)
    val riding = transitPhase as? TransitPhase.Riding
    val currentSegIndex = riding?.index ?: -1
    // 하차까지 앞으로 지날 정류장 수 (1 = 다음 정류장에서 하차)
    val remainingStops = riding?.stopsLeft ?: -1

    // ---- 다음 장소 힌트: 가까워질수록 하나씩 열린다 ----
    val lang = LocalLanguage.current
    val hints = remember(dayIndex, legIndex, lang) { nextStop?.let { buildHints(it, lang) }.orEmpty() }
    val legProgress = (distOnLeg / currentGeom.lengthMeters).coerceIn(0.0, 1.0)
    val unlockedHints = hints.count { legProgress >= it.at }
    var showHints by remember(dayIndex) { mutableStateOf(false) }

    // ---- 음성 안내 (화면 언어로 읽는다) ----
    val say: (String) -> Unit = { if (state.voiceEnabled) speak(it, lang) }
    fun t(key: String, vararg args: Any?) = translate(lang, key, *args)
    LaunchedEffect(legIndex, unlockedHints) {
        // 첫 힌트(종류)는 구간 시작 안내와 겹치므로 두 번째부터 읽는다
        if (unlockedHints >= 2) say(t("voice.newHint", hints[unlockedHints - 1].text))
    }
    DisposableEffect(Unit) { onDispose { stopSpeaking() } }
    LaunchedEffect(legIndex) {
        val key = if (currentLeg.mode == "TRANSIT") "voice.legTransit" else "voice.legWalk"
        say(t(key, spokenDistance(currentGeom.lengthMeters, lang), currentLeg.durationMinutes))
    }
    LaunchedEffect(legIndex, currentSegIndex) {
        // 정류장에 도착한 순간 탈 버스와 하차 정류장을 알려 준다.
        // 구간 설명은 서버가 한국어로 주므로 한국어 화면에서만 그대로 읽고, 다른 언어는 화면을 보라고 안내
        val r = riding ?: return@LaunchedEffect
        say(if (lang == AppLanguage.KO) r.description else t("voice.atStop"))
    }
    LaunchedEffect(legIndex, currentSegIndex, remainingStops) {
        when (remainingStops) {
            2 -> say(t("voice.alightNext"))   // 다음다음 정류장에서 내린다
            1 -> say(t("voice.alightNow"))    // 이번(다가오는) 정류장에서 내린다
        }
    }
    val near = remainMeters <= 100.0 && currentGeom.lengthMeters > 200.0
    LaunchedEffect(legIndex, near) {
        if (near) say(t("voice.near"))
    }
    LaunchedEffect(revealStop) {
        revealStop?.let { s ->
            say(t("voice.arrived", stopTypeLabel(s.type, lang), s.name ?: t("journey.mysteryPlace")))
        }
    }
    LaunchedEffect(completed) {
        if (completed) say(t("voice.dayDone"))
    }

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
                        tr("journey.title", dayPlan.day),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = webText(),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tr("journey.toNext", formatDistance(remainMeters), remainMinutes),
                        fontSize = 12.sp,
                        color = webTextMuted(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                VoiceToggleButton(state.voiceEnabled) {
                    state.voiceEnabled = !state.voiceEnabled
                    if (!state.voiceEnabled) stopSpeaking()
                }
            }

            // ---- 웹 `.progress-pill` ----
            MapPill(
                tr("journey.progress", revealedCount, geoms.size),
                Modifier.align(Alignment.CenterHorizontally),
            )

            if (hints.isNotEmpty() && revealStop == null && !completed) {
                Spacer(Modifier.height(8.dp))
                HintButton(
                    unlocked = unlockedHints,
                    total = hints.size,
                    expanded = showHints,
                    onClick = { showHints = !showHints },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                if (showHints) {
                    Spacer(Modifier.height(8.dp))
                    HintCard(
                        hints, legProgress, currentGeom.lengthMeters,
                        Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            // 도착해 장소가 공개되면 버스 안내는 치운다
            val showTransit = currentLeg.mode == "TRANSIT" && revealStop == null && !completed
            if (showTransit && transitPhase != null) {
                Spacer(Modifier.height(8.dp))
                // 지금 단계 하나만 안내한다. 버스 번호·하차 정류장은 승차 정류장에 도착한 뒤에만 보인다
                val segLabel = when (transitPhase) {
                    is TransitPhase.WalkToStop -> tr(
                        if (transitPhase.transfer) "journey.walkToTransfer" else "journey.walkToStop",
                        transitPhase.stopName ?: tr("journey.stopFallback"),
                    )
                    is TransitPhase.Riding ->
                        (if (transitPhase.total > 1) "[${transitPhase.index + 1}/${transitPhase.total}] " else "") +
                            transitPhase.description
                    TransitPhase.WalkToDestination -> tr("journey.walkAfterAlight")
                }
                MapPill(
                    segLabel,
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                    color = WebOrangeDark,
                )
                // 하차까지 남은 정거장 수 — 1이면 다가오는 정류장이 하차 정류장
                if (remainingStops >= 0) {
                    Spacer(Modifier.height(6.dp))
                    MapPill(
                        if (remainingStops <= 1) tr("journey.alightNow")
                        else tr("journey.stopsLeft", remainingStops),
                        Modifier.align(Alignment.CenterHorizontally),
                        color = WebOrangeDark,
                    )
                }
            } else if (showTransit) {
                // 버스 정보가 없는 구간 — 경로 API를 못 써서 거리로 추정했거나 옛 데이터
                Spacer(Modifier.height(8.dp))
                val estimated = currentLeg.estimated || currentLeg.summary?.contains("추정") == true
                MapPill(
                    if (estimated) tr("journey.transitEstimated")
                    else currentLeg.summary
                        ?: tr(
                            "journey.boardAlight",
                            currentLeg.boardStop ?: tr("journey.stopFallback"),
                            currentLeg.alightStop ?: tr("journey.stopFallback"),
                        ),
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                    color = WebOrangeDark,
                )
            }

            Spacer(Modifier.weight(1f))

            // ---- 웹 `.mystery-hint` — 지도 위에 뜨는 안내 ----
            MysteryHint(
                when {
                    liveMode && liveFix == null -> tr("journey.hintLiveWaiting")
                    liveMode -> tr("journey.hintLive")
                    playing -> tr("journey.hintPlaying")
                    else -> tr("journey.hintIdle")
                },
                Modifier.padding(horizontal = 16.dp),
            )

            // ---- 모드 선택: 시뮬레이션 / 실시간(GPS) ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SimButton(
                    tr("journey.modeSim"),
                    primary = false,
                    selected = !liveMode,
                    modifier = Modifier.weight(1f),
                ) { liveMode = false }
                SimButton(
                    tr("journey.modeLive"),
                    primary = false,
                    selected = liveMode,
                    modifier = Modifier.weight(1f),
                ) {
                    liveMode = true
                    playing = false
                }
            }

            // ---- 웹 `.map-bottombar` + `.sim-btn` ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (liveMode) {
                    MapPill(
                        if (liveFix == null) tr("journey.gpsWaiting") else tr("journey.gpsTracking", ARRIVE_RADIUS_M.toInt()),
                        Modifier.weight(1f),
                    )
                } else {
                    SimButton(
                        text = if (playing) tr("journey.pause") else tr("journey.simulate"),
                        primary = true,
                        enabled = revealStop == null && !completed,
                        modifier = Modifier.weight(1f),
                    ) { playing = !playing }
                    SPEED_OPTIONS.forEachIndexed { i, (_, label) ->
                        SimButton(label, primary = false, selected = speedIdx == i) { speedIdx = i }
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
                        playing = !liveMode
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
private const val MAP_ZOOM = 19       // 고정 줌 — 확대/축소 제스처 없음 (미스터리 지도 규칙)
private const val MAP_TILE_SCALE = 2.6f // 타일 렌더 배율 (클수록 더 확대되어 보임)

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun JourneyMap(
    geoms: List<LegGeometry>,
    stops: List<StopDto>,
    legIndex: Int,
    distOnLeg: Double,
    revealedCount: Int,
) {
    val colorScheme = MaterialTheme.colorScheme
    val darkMap = isAppDark()
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "pulseValue",
    )
    // 멀티플랫폼 캔버스 텍스트 — 지도 마커 이모지는 토스페이스 폰트로
    val textMeasurer = rememberTextMeasurer()
    val tossFace = TossFaceFontFamily

    // ---- 내비게이션 카메라: 사용자 현재 위치가 항상 화면 중앙 ----
    val (curLat, curLng) = geoms[legIndex].positionAt(distOnLeg)

    // ---- OSM 타일 로딩 (실패한 타일은 다음 이동 때 재시도, 그동안은 격자 배경) ----
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val tiles = remember { mutableStateMapOf<TileKey, ImageBitmap>() }
    val pendingTiles = remember { mutableSetOf<TileKey>() }
    val visibleTiles = remember(canvasSize, curLat, curLng) {
        if (canvasSize == IntSize.Zero) emptyList()
        else MapCamera(
            curLat, curLng, MAP_ZOOM, MAP_TILE_SCALE,
            canvasSize.width.toFloat(), canvasSize.height.toFloat(),
        ).visibleTiles()
    }
    LaunchedEffect(visibleTiles) {
        visibleTiles.forEach { key ->
            if (key !in tiles && pendingTiles.add(key)) {
                launch(Dispatchers.Default) {
                    runCatching { TileFetcher.fetch(key.z, key.x, key.y, darkMap) }
                        .onSuccess { bytes ->
                            runCatching { tiles[key] = bytes.decodeToImageBitmap() }
                        }
                    pendingTiles.remove(key)
                }
            }
        }
        // 캐시 상한 — 화면 밖 타일부터 정리
        if (tiles.size > 140) {
            val keep = visibleTiles.toSet()
            tiles.keys.filterNot { it in keep }.take(tiles.size - 100)
                .forEach { tiles.remove(it) }
        }
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it },
    ) {
        val camera = MapCamera(curLat, curLng, MAP_ZOOM, MAP_TILE_SCALE, size.width, size.height)

        // 배경 그리드 — 타일이 아직 없을 때의 폴백 (오프라인에서도 경로는 보이게)
        drawRect(colorScheme.background)
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

        // OSM 타일 (+1px 겹침으로 반올림 이음새 제거)
        val tilePx = (TILE_SIZE * MAP_TILE_SCALE).roundToInt() + 1
        camera.visibleTiles().forEach { key ->
            val img = tiles[key] ?: return@forEach
            val tl = camera.tileTopLeft(key.x, key.y)
            drawImage(
                img,
                dstOffset = IntOffset(tl.x.roundToInt(), tl.y.roundToInt()),
                dstSize = IntSize(tilePx, tilePx),
            )
        }
        // 지도 간략화 — 타일 위에 옅은 막을 씌워 상점·POI 색을 가라앉힌다 (경로·정류장 마커 강조)
        if (tiles.isNotEmpty()) {
            drawRect(
                if (darkMap) Color(0xFF10191B).copy(alpha = 0.68f)
                else Color.White.copy(alpha = 0.72f),
            )
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
                val o = camera.toOffset(lat, lng)
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

        // 현재 향하는 장소로 가는 길만 그린다 (미래·과거 구간은 숨김).
        // 현재 구간의 남은 길(보라색) + 지나온 길(회색 점선)만 표시.
        val current = geoms[legIndex]
        val traveledPts = current.subPathTo(distOnLeg)
        val remainingPts = buildList {
            add(current.positionAt(distOnLeg))
            var idx = 0
            while (idx < current.cumulative.size && current.cumulative[idx] <= distOnLeg) idx++
            for (j in idx until current.points.size) add(current.points[j])
        }
        drawGeoPath(remainingPts, MysteryPurple, 9f)
        drawGeoPath(traveledPts, colorScheme.outline.copy(alpha = 0.65f), 6f, dashed = true)

        // 버스 정류장 마커 — 현재 구간이 대중교통일 때만, 그 구간의 정류장만 표시
        run {
            val lg = current.leg
            if (lg.mode != "TRANSIT") return@run
            // 경유 정류장 (작은 주황 점)
            lg.stations?.forEach { st ->
                if (st.size >= 2) {
                    val o = camera.toOffset(st[0], st[1])
                    drawCircle(WebOrangeDark, 5f, o)
                    drawCircle(Color.White, 5f, o, style = Stroke(2f))
                }
            }
            // 승차·하차 정류장 (흰 원 + 주황 테두리 + 🚏)
            listOf(
                lg.boardLat to lg.boardLng,
                lg.alightLat to lg.alightLng,
            ).forEach { (la, ln) ->
                if (la != null && ln != null) {
                    val o = camera.toOffset(la, ln)
                    drawCircle(Color.White, 15f, o)
                    drawCircle(WebOrangeDark, 15f, o, style = Stroke(3.5f))
                    val layout = textMeasurer.measure(
                        "🚏",
                        style = TextStyle(fontSize = 17f.toSp(), fontFamily = tossFace),
                    )
                    drawText(
                        layout,
                        topLeft = Offset(o.x - layout.size.width / 2f, o.y - layout.size.height / 2f),
                    )
                }
            }
        }

        // 정차 지점 마커 — 현재 구간의 출발지(legIndex)와 목표 장소(legIndex+1)만 표시
        stops.forEachIndexed { i, stop ->
            if (i != legIndex && i != legIndex + 1) return@forEachIndexed
            val o = camera.toOffset(stop.latitude, stop.longitude)
            val revealed = i < revealedCount
            if (revealed) {
                drawCircle(colorScheme.secondaryContainer, 26f, o)
                drawCircle(colorScheme.secondary, 26f, o, style = Stroke(4f))
                val layout = textMeasurer.measure(
                    stopEmoji(stop.type),
                    style = TextStyle(fontSize = 28f.toSp(), fontFamily = tossFace),
                )
                drawText(
                    layout,
                    topLeft = Offset(o.x - layout.size.width / 2f, o.y - layout.size.height / 2f),
                )
            } else {
                drawCircle(colorScheme.surfaceVariant, 24f, o)
                drawCircle(colorScheme.outline, 24f, o, style = Stroke(3f))
                val layout = textMeasurer.measure(
                    "?",
                    style = TextStyle(
                        fontSize = 30f.toSp(),
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant,
                    ),
                )
                drawText(
                    layout,
                    topLeft = Offset(o.x - layout.size.width / 2f, o.y - layout.size.height / 2f),
                )
            }
        }

        // 현재 위치 마커 (펄스) — 카메라가 사용자를 따라가므로 항상 화면 정중앙
        val curOffset = camera.toOffset(curLat, curLng)
        // 웹 `.user-marker` — 민트 점에 흰 테두리와 옅은 후광
        drawCircle(
            WebMint.copy(alpha = (1f - pulse) * 0.35f),
            18f + pulse * 30f,
            curOffset,
        )
        drawCircle(WebMint, 14f, curOffset)
        drawCircle(Color.White, 14f, curOffset, style = Stroke(4f))

        // OSM 저작자 표시 (타일 사용 요건)
        val attribution = textMeasurer.measure(
            "© OpenStreetMap",
            style = TextStyle(fontSize = 9.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f)),
        )
        drawText(
            attribution,
            topLeft = Offset(
                size.width - attribution.size.width - 10f,
                size.height - attribution.size.height - 8f,
            ),
        )
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
                painterResource(Res.drawable.ic_chevron_left),
                contentDescription = tr("journey.backCd"),
                tint = WebMintDeep,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 음성 안내 켜기/끄기 — 뒤로 버튼과 같은 모양 */
@Composable
private fun VoiceToggleButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = webSurface(),
        border = BorderStroke(1.dp, webBorderStrong()),
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (enabled) "🔊" else "🔇",
                fontSize = 18.sp,
                fontFamily = TossFaceFontFamily,
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
                        if (isLast) tr("journey.revealLast") else tr("journey.revealArrived"),
                        fontSize = 12.sp,
                        letterSpacing = 3.sp,
                        color = webTextFaint(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stop.name ?: tr("journey.mysteryPlace"),
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
                            stopTypeLabel(stop.type, LocalLanguage.current),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WebMint,
                        )
                        if (stop.rating > 0) {
                            Text(
                                "★ ${oneDecimal(stop.rating)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WebMint,
                            )
                        }
                        if (stop.cost > 0) {
                            Text(
                                tokens(stop.cost),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WebMint,
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    PrimaryCta(
                        text = if (isLast) tr("journey.finish") else tr("journey.nextPlace"),
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
                sin(t * particle.wobble * PI).toFloat() * 46f
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
                    if (hasNextDay) tr("journey.dayDone", dayPlan.day) else tr("journey.tripDone"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = webText(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("journey.todayCost", tokens(dayPlan.dayCost)),
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
                                tokens(stop.cost),
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
                        tr("journey.totalSummary", tokens(plan.totalCost), tokens(plan.remainingBudget)),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = webText(),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(22.dp))
                PrimaryCta(
                    text = if (hasNextDay) tr("journey.startDay", dayPlan.day + 1) else tr("journey.backToPlan"),
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
