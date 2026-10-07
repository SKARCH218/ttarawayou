package com.trevit.app.map

import androidx.compose.ui.geometry.Offset
import com.trevit.app.i18n.AppLanguage
import com.trevit.app.i18n.translate
import com.trevit.shared.LegDto
import com.trevit.shared.StopDto
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// java.lang.Math.toRadians 는 wasm 타깃에 없다
private fun toRadians(deg: Double): Double = deg * PI / 180.0

/** 두 좌표 사이 거리(m) — 하버사인 */
fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6_371_000.0
    val dLat = toRadians(lat2 - lat1)
    val dLng = toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(toRadians(lat1)) * cos(toRadians(lat2)) *
        sin(dLng / 2) * sin(dLng / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

/** 한 구간의 경로 기하: 포인트 목록 + 누적 거리 */
class LegGeometry(val leg: LegDto, from: StopDto, to: StopDto) {
    val points: List<Pair<Double, Double>> =
        leg.path.filter { it.size >= 2 }.map { it[0] to it[1] }
            .takeIf { it.size >= 2 }
            ?: listOf(from.latitude to from.longitude, to.latitude to to.longitude)

    val cumulative: DoubleArray = DoubleArray(points.size).also { acc ->
        for (i in 1 until points.size) {
            val (aLat, aLng) = points[i - 1]
            val (bLat, bLng) = points[i]
            acc[i] = acc[i - 1] + haversineMeters(aLat, aLng, bLat, bLng)
        }
    }

    val lengthMeters: Double = cumulative.last().coerceAtLeast(1.0)

    /** 시작으로부터 dist(m) 지점의 좌표 (선형 보간) */
    fun positionAt(dist: Double): Pair<Double, Double> {
        if (dist <= 0) return points.first()
        if (dist >= lengthMeters) return points.last()
        var i = 1
        while (i < cumulative.size && cumulative[i] < dist) i++
        val segStart = cumulative[i - 1]
        val segLen = (cumulative[i] - segStart).coerceAtLeast(0.0001)
        val t = ((dist - segStart) / segLen).coerceIn(0.0, 1.0)
        val (aLat, aLng) = points[i - 1]
        val (bLat, bLng) = points[i]
        return (aLat + (bLat - aLat) * t) to (aLng + (bLng - aLng) * t)
    }

    /**
     * 좌표를 경로 위 가장 가까운 지점으로 투영해, 시작으로부터의 거리(m)를 돌려준다.
     * [minAlong] 보다 앞쪽 구간은 보지 않는다 — 정류장을 순서대로 투영할 때 되돌아오는 길에 붙지 않게.
     */
    fun project(lat: Double, lng: Double, minAlong: Double = 0.0): Double {
        val mPerDegLat = 111_320.0
        val mPerDegLng = 111_320.0 * cos(toRadians(lat))
        var bestDist = Double.MAX_VALUE
        var bestAlong = minAlong.coerceIn(0.0, lengthMeters)
        for (i in 1 until points.size) {
            if (cumulative[i] < minAlong) continue
            val (aLat, aLng) = points[i - 1]
            val (bLat, bLng) = points[i]
            val ax = (aLng - lng) * mPerDegLng; val ay = (aLat - lat) * mPerDegLat
            val bx = (bLng - lng) * mPerDegLng; val by = (bLat - lat) * mPerDegLat
            val dx = bx - ax; val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 < 1e-9) 0.0 else (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0)
            val px = ax + dx * t; val py = ay + dy * t
            val d = px * px + py * py
            if (d < bestDist) {
                bestDist = d
                bestAlong = cumulative[i - 1] + (cumulative[i] - cumulative[i - 1]) * t
            }
        }
        return bestAlong
    }

    /** dist 지점까지의 부분 경로 (지나온 경로 그리기용) */
    fun subPathTo(dist: Double): List<Pair<Double, Double>> {
        if (dist <= 0) return listOf(points.first())
        if (dist >= lengthMeters) return points
        val result = mutableListOf<Pair<Double, Double>>()
        var i = 0
        while (i < cumulative.size && cumulative[i] <= dist) {
            result.add(points[i]); i++
        }
        result.add(positionAt(dist))
        return result
    }
}

/** 위경도 → 캔버스 좌표 투영 (등장방형 + 위도 보정, 종횡비 유지) */
class GeoProjector(
    allPoints: List<Pair<Double, Double>>,
    private val width: Float,
    private val height: Float,
    private val padding: Float,
) {
    private val minLat: Double
    private val maxLat: Double
    private val minLng: Double
    private val maxLng: Double
    private val cosLat: Double
    private val scale: Float
    private val offsetX: Float
    private val offsetY: Float

    init {
        var loLat = Double.MAX_VALUE; var hiLat = -Double.MAX_VALUE
        var loLng = Double.MAX_VALUE; var hiLng = -Double.MAX_VALUE
        for ((lat, lng) in allPoints) {
            loLat = min(loLat, lat); hiLat = max(hiLat, lat)
            loLng = min(loLng, lng); hiLng = max(hiLng, lng)
        }
        if (allPoints.isEmpty()) { loLat = 0.0; hiLat = 1.0; loLng = 0.0; hiLng = 1.0 }
        minLat = loLat; maxLat = hiLat; minLng = loLng; maxLng = hiLng
        cosLat = cos(toRadians((minLat + maxLat) / 2))
        val spanX = ((maxLng - minLng) * cosLat).coerceAtLeast(1e-6)
        val spanY = (maxLat - minLat).coerceAtLeast(1e-6)
        val sx = (width - padding * 2) / spanX.toFloat()
        val sy = (height - padding * 2) / spanY.toFloat()
        scale = min(sx, sy)
        offsetX = (width - spanX.toFloat() * scale) / 2f
        offsetY = (height - spanY.toFloat() * scale) / 2f
    }

    fun toOffset(lat: Double, lng: Double): Offset {
        val x = offsetX + (((lng - minLng) * cosLat).toFloat()) * scale
        val y = offsetY + ((maxLat - lat).toFloat()) * scale
        return Offset(x, y)
    }
}

/** 장소 유형 → 이모지 아이콘 */
fun stopEmoji(type: String?): String = when (type) {
    "LODGING" -> "🏨"
    "RESTAURANT" -> "🍜"
    "ATTRACTION" -> "🎡"
    "START" -> "📍"
    else -> "✨"
}

/** 장소 유형 → 화면 언어 라벨 */
fun stopTypeLabel(type: String?, lang: AppLanguage = AppLanguage.KO): String = translate(
    lang,
    when (type) {
        "LODGING" -> "geo.lodging"
        "RESTAURANT" -> "geo.restaurant"
        "ATTRACTION" -> "geo.attraction"
        "START" -> "geo.start"
        else -> "geo.mystery"
    },
)
