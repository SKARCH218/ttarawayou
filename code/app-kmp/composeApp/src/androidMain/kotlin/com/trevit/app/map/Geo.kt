package com.trevit.app.map

import androidx.compose.ui.geometry.Offset
import com.trevit.shared.LegDto
import com.trevit.shared.StopDto
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** 두 좌표 사이 거리(m) — 하버사인 */
fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
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
        cosLat = cos(Math.toRadians((minLat + maxLat) / 2))
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

/** 장소 유형 → 한글 라벨 */
fun stopTypeLabel(type: String?): String = when (type) {
    "LODGING" -> "숙소"
    "RESTAURANT" -> "맛집"
    "ATTRACTION" -> "관광지"
    "START" -> "출발지"
    else -> "미스터리"
}
