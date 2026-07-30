package com.trevit.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.trevit.dto.PlanDtos.LegDto
import com.trevit.dto.PlanDtos.StepDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 구간 이동 경로 생성.
 * - 도보: TMAP 보행자 → OSRM 공개 서버 → 직선 보간 폴백
 * - 대중교통: TMAP → ODsay → 거리 기반 추정 폴백
 * 모든 외부 호출은 connect/read 3초 타임아웃.
 * OSRM이 한 번 실패하면 60초간 호출을 건너뛰어(서킷 브레이커) 전체 응답 시간을 보장한다.
 */
@Service
class RouteService(
    @Value("\${odsay.api-key}") odsayKey: String?,
    @Value("\${osrm.base-url}") private val osrmBaseUrl: String,
    private val publicBusService: PublicBusService,
    private val intercityBusService: IntercityBusService,
    private val tmapService: TmapService,
) {

    private val log = LoggerFactory.getLogger(RouteService::class.java)

    private val odsayKey: String = odsayKey?.trim().orEmpty()
    private val mapper = ObjectMapper()
    private val http: RestClient = RestClient.builder()
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(3000)
            setReadTimeout(3000)
        })
        .build()
    private val cache = ConcurrentHashMap<String, LegDto>()

    /** OSRM 실패 시 이 시각까지 호출을 건너뛴다 (응답 시간 보장) */
    @Volatile
    private var osrmBlockedUntil: Long = 0

    private fun osrmUsable(): Boolean = System.currentTimeMillis() >= osrmBlockedUntil

    private fun markOsrmFailed(reason: String?) {
        osrmBlockedUntil = System.currentTimeMillis() + 60_000L
        log.warn("OSRM 실패({}) — 60초간 직선 보간 폴백", reason)
    }

    /** 1인 기준 도보 구간 생성 (요금 0) */
    fun walkLeg(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): LegDto {
        val key = "W:${round(fromLat)},${round(fromLng)}>${round(toLat)},${round(toLng)}"
        return cache.computeIfAbsent(key) { fetchWalk(fromLat, fromLng, toLat, toLng) }
    }

    /** 1인 기준 대중교통 구간 생성. fare는 1인 요금 */
    fun transitLeg(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): LegDto {
        val key = "T:${round(fromLat)},${round(fromLng)}>${round(toLat)},${round(toLng)}"
        return cache.computeIfAbsent(key) { fetchTransit(fromLat, fromLng, toLat, toLng) }
    }

    // ---------- 도보 ----------

    private fun fetchWalk(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): LegDto {
        // 1순위: TMAP 보행자 경로 (한국 보행로 데이터)
        tmapService.walkRoute(fromLat, fromLng, toLat, toLng)?.let { tw ->
            return LegDto("WALK", tw.distanceMeters, tw.minutes, 0, "도보 " + fmtKm(tw.distanceMeters), tw.path)
        }
        // 2순위: OSRM
        if (osrmUsable()) {
            try {
                val url = String.format(
                    Locale.US,
                    "%s/route/v1/foot/%f,%f;%f,%f?overview=full&geometries=geojson",
                    osrmBaseUrl, fromLng, fromLat, toLng, toLat,
                )
                val body = http.get().uri(URI.create(url)).retrieve().body(String::class.java)
                val root = mapper.readTree(body)
                check(root.path("code").asText() == "Ok") { "OSRM code != Ok" }
                val route = root.path("routes").get(0)
                val distance = route.path("distance").asDouble()
                val path = ArrayList<DoubleArray>()
                for (c in route.path("geometry").path("coordinates")) {
                    path.add(doubleArrayOf(c.get(1).asDouble(), c.get(0).asDouble()))
                }
                val minutes = maxOf(1L, Math.round(distance / WALK_SPEED_M_PER_MIN)).toInt()
                return LegDto("WALK", distance, minutes, 0, "도보 " + fmtKm(distance), path)
            } catch (e: Exception) {
                markOsrmFailed(e.message)
            }
        }
        // 3순위: 직선 보간
        val d = GeoUtil.distanceMeters(fromLat, fromLng, toLat, toLng) * 1.3
        val minutes = maxOf(1L, Math.round(d / WALK_SPEED_M_PER_MIN)).toInt()
        return LegDto("WALK", d, minutes, 0, "도보 " + fmtKm(d) + " (추정)",
            straightPath(fromLat, fromLng, toLat, toLng))
    }

    // ---------- 대중교통 ----------

    private fun fetchTransit(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): LegDto {
        // 1순위: TMAP 대중교통 (경로좌표·요금·환승 포함)
        var leg: LegDto? = tmapService.transitLeg(fromLat, fromLng, toLat, toLng)
        // 2순위: ODsay
        if (leg == null && odsayKey.isNotEmpty()) {
            try {
                leg = fetchOdsay(fromLat, fromLng, toLat, toLng)
            } catch (e: Exception) {
                log.warn("ODsay 호출 실패, 거리 기반 추정 폴백: {}", e.message)
            }
        }
        var result = leg ?: estimateTransit(fromLat, fromLng, toLat, toLng)
        // 시외 이동이면 시외버스 터미널·시간표·요금 정보를 결합
        val straight = GeoUtil.distanceMeters(fromLat, fromLng, toLat, toLng)
        if (straight > INTERCITY_THRESHOLD_M) {
            result = enrichIntercity(result, fromLat, fromLng, toLat, toLng)
            result = ensureIntercityFare(result, straight)
        }
        return result
    }

    /**
     * 시외 구간인데 요금이 시내버스 수준(명백히 비현실적)이면 거리 기반으로 추정한다.
     * 기준: 시외버스 대략 km당 85원, 최소 5,000원.
     */
    private fun ensureIntercityFare(leg: LegDto, straightM: Double): LegDto {
        val km = straightM / 1000.0
        val minReasonable = Math.round(km * 30) // 이보다 낮으면 시내요금 폴백으로 판단
        if (leg.fare >= minReasonable) return leg
        val estimated = maxOf(5000L, Math.round(km * 85 / 100) * 100)
        log.info("시외 구간 요금 보정: {}km, {}원 → {}원(추정)", Math.round(km), leg.fare, estimated)
        return leg.copy(fare = estimated, summary = leg.summary + " · 요금 " + estimated + "원(거리 기반 추정)")
    }

    /** 시외 구간: TAGO 시외버스정보로 다음 배차·터미널·실제 요금을 붙인다 */
    private fun enrichIntercity(
        leg: LegDto, fromLat: Double, fromLng: Double, toLat: Double, toLng: Double,
    ): LegDto {
        if (!intercityBusService.enabled()) return leg
        return try {
            val depSt = publicBusService.nearestStations(fromLat, fromLng)
            val arrSt = publicBusService.nearestStations(toLat, toLng)
            if (depSt.isEmpty() || arrSt.isEmpty()) return leg
            val depCity = depSt[0].cityCode
            val arrCity = arrSt[0].cityCode
            val d = intercityBusService.nextDeparture(
                depCity, publicBusService.cityName(depCity),
                arrCity, publicBusService.cityName(arrCity),
            ) ?: return leg

            val steps = ArrayList(leg.steps ?: emptyList())
            steps.add(0, StepDto(
                "BUS",
                "시외버스 ${d.depTerminalNm} → ${d.arrTerminalNm}" +
                    " · 다음 출발 ${d.depTime}" +
                    (if (d.arrTime.isEmpty()) "" else " (도착 ${d.arrTime})") +
                    " · ${d.grade} ${d.charge}원",
                leg.distanceMeters, leg.durationMinutes,
            ))
            // 추정 요금(시내버스 수준)이면 실제 시외버스 요금으로 교체
            val fare = if (leg.fare < d.charge / 2) d.charge else leg.fare
            leg.copy(fare = fare, summary = leg.summary + " · 시외버스 " + d.depTime + " 출발", steps = steps)
        } catch (e: Exception) {
            log.warn("시외버스 정보 결합 실패: {}", e.message)
            leg
        }
    }

    private fun fetchOdsay(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): LegDto? {
        val encodedKey = URLEncoder.encode(odsayKey, StandardCharsets.UTF_8)
        val url = String.format(
            Locale.US,
            "https://api.odsay.com/v1/api/searchPubTransPathT?SX=%f&SY=%f&EX=%f&EY=%f&apiKey=%s",
            fromLng, fromLat, toLng, toLat, encodedKey,
        )
        // 문자열로 넘기면 URI 템플릿 처리로 %2B가 %252B로 이중 인코딩되므로 URI 객체로 전달
        val body = http.get().uri(URI.create(url)).retrieve().body(String::class.java)
        val root = mapper.readTree(body)
        if (root.has("error")) {
            throw IllegalStateException("ODsay error: " + root.path("error").toString())
        }
        val best = root.path("result").path("path").get(0) ?: return null

        val info = best.path("info")
        val fare = info.path("payment").asLong(BASE_BUS_FARE.toLong())
        val minutes = info.path("totalTime").asInt(30)
        val distance = info.path("totalDistance").asDouble(
            GeoUtil.distanceMeters(fromLat, fromLng, toLat, toLng),
        )

        val path = ArrayList<DoubleArray>()
        val stations = ArrayList<DoubleArray>() // 정류장 좌표만 (하차 카운트다운용)
        val parts = ArrayList<String>()
        val steps = ArrayList<StepDto>()
        var boardStop: String? = null
        var alightStop: String? = null
        var boardLat: Double? = null
        var boardLng: Double? = null
        var alightLat: Double? = null
        var alightLng: Double? = null
        var pendingWalkM = -1.0   // 버스 구간 사이의 도보(정류장까지 걷기)
        var pendingWalkMin = 0
        path.add(doubleArrayOf(fromLat, fromLng))
        for (sub in best.path("subPath")) {
            val trafficType = sub.path("trafficType").asInt() // 1 지하철, 2 버스, 3 도보
            if (trafficType == 3) {
                val dm = sub.path("distance").asDouble(0.0)
                if (dm > 0) {
                    pendingWalkM = if (pendingWalkM < 0) dm else pendingWalkM + dm
                    pendingWalkMin += sub.path("sectionTime").asInt(0)
                }
                continue
            }
            val startName = sub.path("startName").asText("")
            val endName = sub.path("endName").asText("")
            if (boardStop == null && startName.isNotEmpty()) {
                boardStop = startName
                boardLat = sub.path("startY").asDouble()
                boardLng = sub.path("startX").asDouble()
            }
            if (endName.isNotEmpty()) {
                alightStop = endName
                alightLat = sub.path("endY").asDouble()
                alightLng = sub.path("endX").asDouble()
            }
            val lane = sub.path("lane").get(0)
            var busNo = ""
            val stationCount = sub.path("stationCount").asInt(0)
            if (lane != null) {
                busNo = lane.path("busNo").asText(lane.path("name").asText("")).trim()
            }
            // 시외 이동에서는 기차 등 번호 없는 구간이 올 수 있다
            val rideName = if (busNo.isEmpty()) {
                if (trafficType == 1) "지하철" else "시외 이동(열차/버스)"
            } else "${busNo}번 버스"
            parts.add(
                "$rideName ($startName 승차 → $endName 하차" +
                    (if (stationCount > 0) ", ${stationCount}개 정류장" else "") + ")"
            )
            // 세부 단계: (도보 → 정류장) + (승차 → 하차)
            if (pendingWalkM >= 0) {
                steps.add(StepDto(
                    "WALK",
                    "도보 ${Math.round(pendingWalkM)}m → $startName 정류장",
                    pendingWalkM, maxOf(1, pendingWalkMin),
                ))
                pendingWalkM = -1.0
                pendingWalkMin = 0
            }
            steps.add(StepDto(
                "BUS",
                "$rideName · $startName 승차 → $endName 하차" +
                    (if (stationCount > 0) " (${stationCount}개 정류장)" else ""),
                sub.path("distance").asDouble(0.0), sub.path("sectionTime").asInt(0),
            ))
            // 1순위: 공공데이터포털(TAGO) 버스노선의 실제 경유 정류소 구간
            var segment: List<DoubleArray>? = null
            if (trafficType == 2 && busNo.isNotEmpty()) {
                segment = publicBusService.stationsBetween(
                    busNo,
                    sub.path("startY").asDouble(), sub.path("startX").asDouble(),
                    sub.path("endY").asDouble(), sub.path("endX").asDouble(),
                )
            }
            // 2순위: ODsay가 준 경유 정류장 목록
            if (segment == null) {
                val seg = ArrayList<DoubleArray>()
                for (st in sub.path("passStopList").path("stations")) {
                    val x = st.path("x").asDouble() // 경도
                    val y = st.path("y").asDouble() // 위도
                    if (x != 0.0 && y != 0.0) seg.add(doubleArrayOf(y, x))
                }
                segment = seg
            }
            path.addAll(segment)
            stations.addAll(segment)
        }
        path.add(doubleArrayOf(toLat, toLng))
        // 마지막 도보 (하차 후 목적지까지)
        if (pendingWalkM >= 0) {
            steps.add(StepDto(
                "WALK",
                "하차 후 도보 ${Math.round(pendingWalkM)}m → 목적지",
                pendingWalkM, maxOf(1, pendingWalkMin),
            ))
        }
        // 정류장 좌표만 직선으로 이으면 건물을 관통해 보이므로,
        // 정류장들을 경유지로 실도로(OSRM driving) 경로를 만들어 지도용 경로로 쓴다
        val roadPath = osrmRouteThrough(path)
        val summary = if (parts.isEmpty()) "대중교통" else parts.joinToString(" → 환승 → ")
        return LegDto(
            "TRANSIT", distance, minutes, fare, summary,
            roadPath ?: path, boardStop, alightStop, null, null,
            boardLat, boardLng, alightLat, alightLng, stations, steps,
        )
    }

    /** 여러 지점을 순서대로 경유하는 실도로 경로 (OSRM driving). 실패 시 null */
    private fun osrmRouteThrough(points: List<DoubleArray>): List<DoubleArray>? {
        if (!osrmUsable()) return null
        return try {
            val waypoints = sampleWaypoints(points, 24)
            val coords = waypoints.joinToString(";") {
                String.format(Locale.US, "%f,%f", it[1], it[0]) // lng,lat
            }
            val url = "$osrmBaseUrl/route/v1/driving/$coords?overview=full&geometries=geojson"
            val body = http.get().uri(URI.create(url)).retrieve().body(String::class.java)
            val root = mapper.readTree(body)
            if (root.path("code").asText() != "Ok") return null
            val out = ArrayList<DoubleArray>()
            for (c in root.path("routes").get(0).path("geometry").path("coordinates")) {
                out.add(doubleArrayOf(c.get(1).asDouble(), c.get(0).asDouble()))
            }
            if (out.size >= 2) out else null
        } catch (e: Exception) {
            markOsrmFailed(e.message)
            null
        }
    }

    /** ODsay/TMAP 불가 시 거리 기반 대중교통 추정: 도로 경로는 OSRM driving으로 시도, 실패 시 직선 보간 */
    private fun estimateTransit(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): LegDto {
        val straight = GeoUtil.distanceMeters(fromLat, fromLng, toLat, toLng)
        var distance = straight * 1.35
        val fare = BASE_BUS_FARE + (if (straight > 15000) 700L else 0L) // 좌석/시계외 할증 추정
        val rideMinutes = Math.round(distance / 1000 / 26 * 60).toInt() // 평균 26km/h
        val minutes = rideMinutes + 9 // 대기 시간 포함
        var path: List<DoubleArray>
        if (osrmUsable()) {
            try {
                val url = String.format(
                    Locale.US,
                    "%s/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                    osrmBaseUrl, fromLng, fromLat, toLng, toLat,
                )
                val body = http.get().uri(URI.create(url)).retrieve().body(String::class.java)
                val root = mapper.readTree(body)
                val route = root.path("routes").get(0)
                distance = route.path("distance").asDouble(distance)
                val p = ArrayList<DoubleArray>()
                for (c in route.path("geometry").path("coordinates")) {
                    p.add(doubleArrayOf(c.get(1).asDouble(), c.get(0).asDouble()))
                }
                path = p
            } catch (e: Exception) {
                markOsrmFailed(e.message)
                path = straightPath(fromLat, fromLng, toLat, toLng)
            }
        } else {
            path = straightPath(fromLat, fromLng, toLat, toLng)
        }
        return LegDto("TRANSIT", distance, minutes, fare, "버스 이동 (요금·시간 추정)", path)
    }

    companion object {
        private const val WALK_SPEED_M_PER_MIN = 67.0   // 약 4km/h
        private const val BASE_BUS_FARE = 1500L         // 시내버스 성인 요금(카드) 추정

        /** 이 직선거리(m)를 넘는 이동은 시외로 보고 시외버스 정보를 결합한다 */
        private const val INTERCITY_THRESHOLD_M = 30_000.0

        private fun round(v: Double): Double = Math.round(v * 1e5) / 1e5

        /** 경유지가 너무 많으면 시작·끝을 보존하며 균등 샘플링 */
        private fun sampleWaypoints(points: List<DoubleArray>, maxCount: Int): List<DoubleArray> {
            if (points.size <= maxCount) return points
            val out = ArrayList<DoubleArray>()
            for (i in 0 until maxCount) {
                out.add(points[Math.round(i.toDouble() * (points.size - 1) / (maxCount - 1)).toInt()])
            }
            return out
        }

        /** 직선 보간 경로 (외부 API 전부 실패 시 즉시 대체) */
        fun straightPath(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): List<DoubleArray> {
            val path = ArrayList<DoubleArray>()
            val steps = 20
            for (i in 0..steps) {
                val t = i.toDouble() / steps
                path.add(doubleArrayOf(fromLat + (toLat - fromLat) * t, fromLng + (toLng - fromLng) * t))
            }
            return path
        }

        private fun fmtKm(meters: Double): String =
            if (meters >= 1000) String.format(Locale.US, "%.1fkm", meters / 1000)
            else "${Math.round(meters)}m"
    }
}
