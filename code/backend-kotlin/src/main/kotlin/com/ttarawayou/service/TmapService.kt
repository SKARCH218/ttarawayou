package com.ttarawayou.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ttarawayou.dto.PlanDtos.LegDto
import com.ttarawayou.dto.PlanDtos.StepDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * SK open API (TMAP) 통합 클라이언트.
 *  - 보행자 경로안내: /tmap/routes/pedestrian  (실제 보행로)
 *  - 대중교통 길찾기: /transit/routes          (버스·지하철·기차, 경로좌표·요금 포함)
 *  - 주변 POI 검색:  /tmap/pois/search/around  (전국 관광지·숙소·음식점)
 * 앱키(TMAP_APP_KEY)가 없거나 실패하면 null을 반환해 기존 방식으로 폴백한다.
 * 429/QUOTA_EXCEEDED가 오면 즉시 쿼터 소진으로 표시하고 재시도하지 않는다.
 */
@Service
class TmapService(@Value("\${tmap.app-key}") appKey: String?) {

    private val log = LoggerFactory.getLogger(TmapService::class.java)

    data class WalkRoute(val path: List<DoubleArray>, val distanceMeters: Double, val minutes: Int)

    data class Poi(val name: String, val lat: Double, val lng: Double, val address: String)

    private val appKey: String = appKey?.trim().orEmpty()
    private val mapper = ObjectMapper()
    private val http: RestClient = RestClient.builder()
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(3000)
            setReadTimeout(3000)
        })
        .build()

    /** 429/쿼터 초과 발생 시각 이후 10분간 TMAP 호출을 중단한다 (즉시 폴백, 재시도 금지) */
    @Volatile
    private var quotaBlockedUntil: Long = 0

    fun enabled(): Boolean = appKey.isNotEmpty()

    fun quotaExhausted(): Boolean = System.currentTimeMillis() < quotaBlockedUntil

    fun usable(): Boolean = enabled() && !quotaExhausted()

    private fun markQuotaExhausted(where: String) {
        quotaBlockedUntil = System.currentTimeMillis() + 10 * 60 * 1000L
        log.warn("TMAP 쿼터 초과(429) [{}] — 10분간 호출 중단, 시드/폴백 사용", where)
    }

    private fun isQuotaError(e: Exception): Boolean {
        if (e is HttpStatusCodeException && e.statusCode.value() == 429) return true
        val msg = e.message.orEmpty()
        return msg.contains("429") || msg.contains("QUOTA_EXCEEDED", ignoreCase = true)
    }

    // ---------- 보행자 경로 ----------

    fun walkRoute(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): WalkRoute? {
        if (!usable()) return null
        return try {
            val body = mapOf(
                "startX" to String.format(Locale.US, "%f", fromLng),
                "startY" to String.format(Locale.US, "%f", fromLat),
                "endX" to String.format(Locale.US, "%f", toLng),
                "endY" to String.format(Locale.US, "%f", toLat),
                "startName" to "S", "endName" to "E",
            )
            val root = post("/tmap/routes/pedestrian?version=1", body)
            val path = ArrayList<DoubleArray>()
            var distance = 0.0
            var seconds = 0
            for (feat in root.path("features")) {
                val props = feat.path("properties")
                if (props.has("totalDistance")) {
                    distance = props.path("totalDistance").asDouble()
                    seconds = props.path("totalTime").asInt()
                }
                val geom = feat.path("geometry")
                if (geom.path("type").asText() == "LineString") {
                    for (c in geom.path("coordinates")) {
                        path.add(doubleArrayOf(c.get(1).asDouble(), c.get(0).asDouble()))
                    }
                }
            }
            if (path.size < 2) return null
            WalkRoute(path, distance, maxOf(1, seconds / 60))
        } catch (e: Exception) {
            if (isQuotaError(e)) markQuotaExhausted("보행자 경로")
            else log.warn("TMAP 보행자 경로 실패: {}", e.message)
            null
        }
    }

    // ---------- 대중교통 길찾기 ----------

    /** 1인 기준 대중교통 구간. 실패 시 null (→ ODsay/추정 폴백) */
    fun transitLeg(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): LegDto? {
        if (!usable()) return null
        try {
            val body = mapOf(
                "startX" to String.format(Locale.US, "%f", fromLng),
                "startY" to String.format(Locale.US, "%f", fromLat),
                "endX" to String.format(Locale.US, "%f", toLng),
                "endY" to String.format(Locale.US, "%f", toLat),
                "count" to 1, "lang" to 0, "format" to "json",
            )
            val root = post("/transit/routes", body)
            val itin = root.path("metaData").path("plan").path("itineraries").get(0) ?: return null

            val fare = itin.path("fare").path("regular").path("totalFare").asLong(0)
            val minutes = maxOf(1, itin.path("totalTime").asInt(0) / 60)

            val path = ArrayList<DoubleArray>()
            val stations = ArrayList<DoubleArray>()
            val steps = ArrayList<StepDto>()
            val parts = ArrayList<String>()
            var distance = 0.0
            var boardStop: String? = null
            var alightStop: String? = null
            var boardLat: Double? = null
            var boardLng: Double? = null
            var alightLat: Double? = null
            var alightLng: Double? = null

            for (leg in itin.path("legs")) {
                val mode = leg.path("mode").asText()
                val legDist = leg.path("distance").asDouble(0.0)
                val legMin = maxOf(1, leg.path("sectionTime").asInt(0) / 60)
                distance += legDist
                if (mode == "WALK") {
                    for (st in leg.path("steps")) {
                        appendLinestring(path, st.path("linestring").asText(""))
                    }
                    if (legDist >= 20) {
                        steps.add(StepDto(
                            "WALK",
                            "도보 ${Math.round(legDist)}m → ${leg.path("end").path("name").asText("목적지")}",
                            legDist, legMin,
                        ))
                    }
                } else {
                    val route = leg.path("route").asText(modeLabel(mode))
                    val sName = leg.path("start").path("name").asText("")
                    val eName = leg.path("end").path("name").asText("")
                    if (boardStop == null) {
                        boardStop = sName
                        boardLat = leg.path("start").path("lat").asDouble()
                        boardLng = leg.path("start").path("lon").asDouble()
                    }
                    alightStop = eName
                    alightLat = leg.path("end").path("lat").asDouble()
                    alightLng = leg.path("end").path("lon").asDouble()

                    var stCount = 0
                    for (st in leg.path("passStopList").path("stationList")) {
                        val la = st.path("lat").asDouble()
                        val lo = st.path("lon").asDouble()
                        if (la != 0.0 && lo != 0.0) {
                            stations.add(doubleArrayOf(la, lo))
                            stCount++
                        }
                    }
                    appendLinestring(path, leg.path("passShape").path("linestring").asText(""))
                    val label = cleanRoute(route, mode)
                    parts.add(
                        "$label ($sName 승차 → $eName 하차" +
                            (if (stCount > 1) ", ${stCount - 1}개 정류장" else "") + ")"
                    )
                    steps.add(StepDto(
                        "BUS",
                        "$label · $sName 승차 → $eName 하차" +
                            (if (stCount > 1) " (${stCount - 1}개 정류장)" else ""),
                        legDist, legMin,
                    ))
                }
            }
            if (path.size < 2) return null
            val summary = if (parts.isEmpty()) "대중교통" else parts.joinToString(" → 환승 → ")
            log.info("TMAP 대중교통 경로: {}분, {}원, {}", minutes, fare, summary)
            return LegDto(
                "TRANSIT", distance, minutes, fare, summary, path,
                boardStop, alightStop, null, null,
                boardLat, boardLng, alightLat, alightLng, stations, steps,
            )
        } catch (e: Exception) {
            if (isQuotaError(e)) markQuotaExhausted("대중교통")
            else log.warn("TMAP 대중교통 실패: {}", e.message)
            return null
        }
    }

    // ---------- 주변 POI (전국 장소 공급) ----------

    fun poisAround(lat: Double, lng: Double, category: String, count: Int): List<Poi> {
        if (!usable()) return emptyList()
        val out = ArrayList<Poi>()
        try {
            val url = String.format(
                Locale.US,
                "%s/tmap/pois/search/around?version=1&centerLon=%f&centerLat=%f&categories=%s&page=1&count=%d&radius=15",
                BASE, lng, lat, URLEncoder.encode(category, StandardCharsets.UTF_8),
                minOf(200, count),
            )
            val res = http.get().uri(URI.create(url))
                .header("appKey", appKey).retrieve().body(String::class.java)
            val pois = mapper.readTree(res).path("searchPoiInfo").path("pois").path("poi")
            for (p in pois) {
                val la = p.path("frontLat").asDouble(p.path("noorLat").asDouble(0.0))
                val lo = p.path("frontLon").asDouble(p.path("noorLon").asDouble(0.0))
                if (la == 0.0 || lo == 0.0) continue
                val addr = (p.path("upperAddrName").asText("") + " " +
                    p.path("middleAddrName").asText("") + " " +
                    p.path("lowerAddrName").asText("")).trim()
                out.add(Poi(p.path("name").asText(""), la, lo, addr))
            }
        } catch (e: Exception) {
            if (isQuotaError(e)) markQuotaExhausted("POI 검색($category)")
            else log.warn("TMAP POI 검색 실패({}): {}", category, e.message)
        }
        return out
    }

    // ---------- 공통 ----------

    private fun post(path: String, body: Map<String, Any>): JsonNode {
        val res = http.post().uri(URI.create(BASE + path))
            .header("appKey", appKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapper.writeValueAsString(body))
            .retrieve().body(String::class.java)
        return mapper.readTree(res)
    }

    companion object {
        private const val BASE = "https://apis.openapi.sk.com"

        /** "lng,lat lng,lat …" 문자열을 [lat,lng] 목록으로 이어붙인다 */
        private fun appendLinestring(path: MutableList<DoubleArray>, linestring: String?) {
            if (linestring.isNullOrBlank()) return
            for (pair in linestring.trim().split(Regex("\\s+"))) {
                val xy = pair.split(",")
                if (xy.size == 2) {
                    val x = xy[0].toDoubleOrNull()
                    val y = xy[1].toDoubleOrNull()
                    if (x != null && y != null) path.add(doubleArrayOf(y, x))
                }
            }
        }

        private fun modeLabel(mode: String): String = when (mode) {
            "SUBWAY" -> "지하철"
            "TRAIN" -> "기차"
            "EXPRESSBUS" -> "고속버스"
            "INTERCITYBUS" -> "시외버스"
            "AIRPLANE" -> "항공"
            "FERRY" -> "여객선"
            else -> "버스"
        }

        /** TMAP route 표기("좌석:11" 등)를 읽기 좋게 다듬는다 */
        private fun cleanRoute(route: String?, mode: String): String {
            if (route.isNullOrBlank()) return modeLabel(mode)
            val r = route.replace(":", " ")
            return if (mode == "BUS" && !r.contains("버스")) "버스 $r" else r
        }
    }
}
