package com.trevit.app.map

import kotlin.math.abs
import kotlin.math.floor

/** 대중교통 구간에서 지금 해야 할 일 */
sealed interface TransitPhase {
    /** 승차 정류장으로 걸어가는 중 — 버스 번호·하차 정류장은 정류장에 도착할 때까지 숨긴다 */
    data class WalkToStop(val stopName: String?, val transfer: Boolean) : TransitPhase

    /**
     * 정류장에 도착해 타고 가는 중.
     * [stopsLeft] 하차까지 앞으로 지날 정류장 수 (하차 정류장 포함). 1이면 다음 정류장에서 내린다. -1 = 모름
     */
    data class Riding(val description: String, val index: Int, val total: Int, val stopsLeft: Int) : TransitPhase

    /** 마지막 하차 후 목적지까지 걷는 중 */
    data object WalkToDestination : TransitPhase
}

/**
 * 대중교통 구간(도보 → 승차 → 하차 → 도보, 환승 포함)에서 경로 위 위치(m)가
 * 어느 단계에 있는지 계산한다.
 *
 * 경로(path)에는 도보·탑승 구간이 모두 이어져 있으므로, 탑승 구간의 시작·끝을 경로 위 거리로 찾아야 한다.
 * - 단계(step)별 거리를 경로 길이에 맞게 늘려 대략의 경계를 잡고
 * - 첫 승차·마지막 하차는 실제 정류장 좌표를 경로에 투영해 맞춘다
 * - 남은 정거장은 경유 정류장 좌표를 경로에 투영해, 현재 위치보다 앞에 있는 것만 센다
 */
class TransitGuide(private val geom: LegGeometry) {

    private class Ride(val description: String, val boardName: String?, var start: Double, var end: Double, val stopCount: Int)

    private val rides: List<Ride>
    private val stationAlongs: List<Double>

    init {
        val leg = geom.leg
        // 시외버스 안내 단계는 구간 전체 거리를 담은 정보성 단계라 경계 계산에서 뺀다
        val steps = leg.steps.orEmpty().filterNot { it.description?.startsWith("시외버스") == true }
        val stepTotal = steps.sumOf { it.distanceMeters }
        val scale = if (stepTotal > 0) geom.lengthMeters / stepTotal else 0.0

        val list = ArrayList<Ride>()
        var acc = 0.0
        for (s in steps) {
            val from = acc * scale
            acc += s.distanceMeters
            val desc = s.description
            if (s.kind == "BUS" && !desc.isNullOrBlank()) {
                list += Ride(
                    description = desc,
                    boardName = BOARD_NAME.find(desc)?.groupValues?.get(1)?.trim(),
                    start = from,
                    end = acc * scale,
                    stopCount = STOP_COUNT.find(desc)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                )
            }
        }
        // 단계 정보가 없는 옛 데이터: 승차·하차 좌표만 있으면 탑승 구간 하나로 본다
        if (list.isEmpty() && leg.boardLat != null && leg.alightLat != null) {
            val desc = leg.summary?.takeIf { it.isNotBlank() }
                ?: "${leg.boardStop ?: "정류장"} 승차 → ${leg.alightStop ?: "정류장"} 하차"
            list += Ride(desc, leg.boardStop, 0.0, 0.0, STOP_COUNT.find(desc)?.groupValues?.get(1)?.toIntOrNull() ?: 0)
        }
        // 첫 승차·마지막 하차 지점은 실제 정류장 좌표로 맞춘다 (경로 위에 있을 때만)
        if (list.isNotEmpty()) {
            val first = list.first()
            val last = list.last()
            snap(leg.boardLat, leg.boardLng)?.let { first.start = it }
            snap(leg.alightLat, leg.alightLng, minAlong = first.start)?.let { last.end = it }
            if (first.end < first.start) first.end = first.start
            if (last.start > last.end) last.start = last.end
        }
        rides = list

        // 경유 정류장을 순서대로 경로에 투영 (앞으로만 진행), 붙어 있는 중복(환승 정류장 등)은 하나로
        val alongs = ArrayList<Double>()
        var prev = 0.0
        for (st in leg.stations.orEmpty()) {
            if (st.size < 2) continue
            val a = geom.project(st[0], st[1], minAlong = (prev - 30.0).coerceAtLeast(0.0))
            if (alongs.isEmpty() || abs(a - alongs.last()) > SAME_STOP_M) alongs += a
            prev = a
        }
        stationAlongs = alongs
    }

    /**
     * 정류장 좌표를 경로 위 거리로. 경로에서 너무 멀면(좌표가 어긋난 데이터) null —
     * 억지로 붙이면 경로 끝에 붙어 "하차 후 걷기" 단계가 사라진다.
     */
    private fun snap(lat: Double?, lng: Double?, minAlong: Double = 0.0): Double? {
        if (lat == null || lng == null) return null
        val along = geom.project(lat, lng, minAlong)
        val (pLat, pLng) = geom.positionAt(along)
        return along.takeIf { haversineMeters(lat, lng, pLat, pLng) <= SNAP_MAX_M }
    }

    /** 경로 위 [pos](m)에서의 단계. 탑승 구간 정보가 없으면 null */
    fun phaseAt(pos: Double): TransitPhase? {
        if (rides.isEmpty()) return null
        for ((i, ride) in rides.withIndex()) {
            // 정류장 반경 안에 들어오기 전까지는 걸어가는 중
            if (pos < ride.start - AT_STOP_M) {
                return TransitPhase.WalkToStop(ride.boardName ?: if (i == 0) geom.leg.boardStop else null, transfer = i > 0)
            }
            if (pos <= ride.end + AT_STOP_M) {
                return TransitPhase.Riding(ride.description, i, rides.size, stopsLeft(ride, pos))
            }
        }
        return TransitPhase.WalkToDestination
    }

    private fun stopsLeft(ride: Ride, pos: Double): Int {
        val inRide = stationAlongs.filter { it >= ride.start - AT_STOP_M && it <= ride.end + AT_STOP_M }
        if (inRide.size >= 2) {
            // 지금 위치보다 앞에 있는 정류장 (막 지나친 정류장, 승차 정류장 자체는 제외)
            val after = maxOf(pos + PASSED_M, ride.start + AT_STOP_M)
            return inRide.count { it > after }
        }
        // 좌표가 없으면 "N개 정류장"과 구간 내 진행률로 추정 — 하차 정류장에 닿기 전까지는 최소 1
        if (ride.stopCount <= 0) return -1
        val len = (ride.end - ride.start).coerceAtLeast(1.0)
        val progress = ((pos - ride.start) / len).coerceIn(0.0, 1.0)
        return (ride.stopCount - floor(progress * ride.stopCount).toInt()).coerceAtLeast(if (progress >= 1.0) 0 else 1)
    }

    private companion object {
        /** 이 거리 안이면 정류장에 도착한 것으로 본다 */
        const val AT_STOP_M = 25.0
        /** 정류장을 이만큼 지나야 "지나간 정류장"으로 친다 */
        const val PASSED_M = 15.0
        /** 이보다 가까운 연속 정류장은 같은 정류장(환승 지점 중복)으로 본다 */
        const val SAME_STOP_M = 20.0
        /** 정류장 좌표가 경로에서 이보다 멀면 경로 위 정류장으로 보지 않는다 */
        const val SNAP_MAX_M = 150.0
        /** "700번 버스 · 시청 승차 → 서울역 하차 (8개 정류장)" 에서 승차 정류장명 */
        val BOARD_NAME = Regex("·\\s*(.+?)\\s*승차")
        val STOP_COUNT = Regex("(\\d+)개 정류장")
    }
}
