package com.trevit.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
 * 대한민국 공공데이터포털 — 국토교통부(TAGO) 전국 버스 정보.
 *  - 버스정류소정보: 좌표 → 근접 정류소·도시코드
 *  - 버스노선정보: 노선번호 → 노선ID → 경유 정류소 시퀀스
 *  - 버스도착정보: 정류소별 실시간 도착 예정
 * 키(DATA_GO_KR_API_KEY)가 없거나 호출이 실패하면 null/빈 결과로 폴백한다.
 */
@Service
class PublicBusService(@Value("\${datago.api-key}") apiKey: String?) {

    private val log = LoggerFactory.getLogger(PublicBusService::class.java)

    /** 정류소 (근접 조회 결과) */
    data class Station(val nodeId: String, val name: String, val cityCode: String, val lat: Double, val lng: Double)

    /** 도착 예정 정보 */
    data class Arrival(
        val routeNo: String, val routeType: String, val arrTimeSec: Int,
        val prevStationCount: Int, val vehicleType: String,
    )

    private val encodedKey: String = apiKey?.trim().orEmpty().let {
        if (it.isEmpty() || it.contains("%")) it else URLEncoder.encode(it, StandardCharsets.UTF_8)
    }

    private val mapper = ObjectMapper()
    private val http: RestClient = RestClient.builder()
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(3000)
            setReadTimeout(3000)
        })
        .build()

    @Volatile
    private var dead = false // 키가 TAGO에 등록되지 않은 경우 재시도 중단

    /** "cityCode:busNo" → 노선 후보, routeId → 정류소 시퀀스, 좌표 → 근접 정류소 목록 */
    private val routeCache = ConcurrentHashMap<String, List<JsonNode>>()
    private val stationCache = ConcurrentHashMap<String, List<DoubleArray>>()
    private val nearCache = ConcurrentHashMap<String, List<Station>>()

    @Volatile
    private var cityNames: MutableMap<String, String>? = null // citycode → cityname

    fun enabled(): Boolean = encodedKey.isNotEmpty() && !dead

    // ---------- ① 좌표 → 근접 정류소 (도시코드 자동 판별) ----------

    /** 좌표 주변 정류소 목록 (가까운 순). */
    fun nearestStations(lat: Double, lng: Double): List<Station> {
        if (!enabled()) return emptyList()
        val key = String.format(Locale.US, "%.3f,%.3f", lat, lng)
        nearCache[key]?.let { return it }
        val out = ArrayList<Station>()
        try {
            val items = call(
                "/BusSttnInfoInqireService/getCrdntPrxmtSttnList",
                String.format(Locale.US, "&gpsLati=%f&gpsLong=%f&numOfRows=10", lat, lng),
            )
            for (it in asArray(items)) {
                val st = Station(
                    it.path("nodeid").asText(), it.path("nodenm").asText(),
                    it.path("citycode").asText(),
                    it.path("gpslati").asDouble(), it.path("gpslong").asDouble(),
                )
                if (st.nodeId.isNotEmpty() && st.cityCode.isNotEmpty()) out.add(st)
            }
            nearCache[key] = out
        } catch (e: Exception) {
            handleFailure("근접 정류소 조회", e)
        }
        return out
    }

    /** 좌표에서 가장 가까운 정류소. 실패 시 null */
    fun nearestStation(lat: Double, lng: Double): Station? = nearestStations(lat, lng).firstOrNull()

    /** 도시코드 → 도시명. 실패/미확인 시 빈 문자열 */
    fun cityName(cityCode: String?): String {
        if (!enabled() || cityCode == null) return ""
        return try {
            if (cityNames == null) {
                val map = ConcurrentHashMap<String, String>()
                val items = call("/BusSttnInfoInqireService/getCtyCodeList", "")
                for (it in asArray(items)) {
                    map[it.path("citycode").asText()] = it.path("cityname").asText()
                }
                cityNames = map
            }
            cityNames?.getOrDefault(cityCode, "") ?: ""
        } catch (e: Exception) {
            handleFailure("도시코드 목록 조회", e)
            ""
        }
    }

    // ---------- ② 버스 경로 (노선의 경유 정류소 구간) ----------

    /**
     * 버스번호와 승차/하차 좌표로 실제 노선의 경유 정류소 구간을 찾는다.
     * 반환: [lat, lng] 목록, 실패 시 null
     */
    fun stationsBetween(
        rawBusNo: String,
        boardLat: Double, boardLng: Double,
        alightLat: Double, alightLng: Double,
    ): List<DoubleArray>? {
        if (!enabled()) return null
        try {
            val busNo = rawBusNo.replace(Regex("\\(.*$"), "").trim()
            if (busNo.isEmpty()) return null
            // 시 경계에서는 인접 도시 정류소가 섞이므로 주변 정류소들의 도시코드를 모두 시도
            val cityCodes = nearestStations(boardLat, boardLng).map { it.cityCode }.distinct()

            for (cityCode in cityCodes) {
                for (route in findRoutes(cityCode, busNo)) {
                    val routeId = route.path("routeid").asText()
                    if (routeId.isEmpty()) continue
                    val stations = findStations(cityCode, routeId)
                    if (stations.size < 2) continue
                    val bi = nearestIndex(stations, boardLat, boardLng)
                    val ai = nearestIndex(stations, alightLat, alightLng)
                    if (bi < 0 || ai < 0 || bi >= ai) continue // 방향 불일치(반대 방향 노선 등)
                    if (GeoUtil.distanceMeters(stations[bi][0], stations[bi][1], boardLat, boardLng) > STATION_MATCH_M ||
                        GeoUtil.distanceMeters(stations[ai][0], stations[ai][1], alightLat, alightLng) > STATION_MATCH_M
                    ) continue
                    log.info("TAGO 버스 노선 매칭: {}번 (도시 {}, routeId {}, 정류소 {}→{})",
                        busNo, cityCode, routeId, bi, ai)
                    return ArrayList(stations.subList(bi, ai + 1))
                }
            }
        } catch (e: Exception) {
            handleFailure("버스노선 조회", e)
        }
        return null
    }

    // ---------- ③ 실시간 도착 정보 ----------

    /**
     * 좌표에서 가장 가까운 정류소의 실시간 버스 도착 예정.
     * busNo가 주어지면 해당 노선만 필터링한다. 반환: (정류소, 도착목록), 실패 시 null
     */
    fun arrivalsNear(lat: Double, lng: Double, busNoFilter: String?): Pair<Station, List<Arrival>>? {
        if (!enabled()) return null
        val st = nearestStation(lat, lng) ?: return null
        val out = ArrayList<Arrival>()
        try {
            val items = call(
                "/ArvlInfoInqireService/getSttnAcctoArvlPrearngeInfoList",
                String.format(
                    Locale.US, "&cityCode=%s&nodeId=%s&numOfRows=30",
                    st.cityCode, URLEncoder.encode(st.nodeId, StandardCharsets.UTF_8),
                ),
            )
            val filter = busNoFilter?.replace(Regex("\\(.*$"), "")?.trim().orEmpty()
            for (it in asArray(items)) {
                val routeNo = it.path("routeno").asText()
                if (filter.isNotEmpty() && !routeNo.equals(filter, ignoreCase = true)) continue
                out.add(Arrival(
                    routeNo, it.path("routetp").asText(),
                    it.path("arrtime").asInt(), it.path("arrprevstationcnt").asInt(),
                    it.path("vehicletp").asText(),
                ))
            }
            out.sortBy { it.arrTimeSec }
        } catch (e: Exception) {
            handleFailure("도착정보 조회", e)
        }
        return st to out
    }

    // ---------- TAGO API 공통 ----------

    private fun findRoutes(cityCode: String, busNo: String): List<JsonNode> {
        val cacheKey = "$cityCode:$busNo"
        routeCache[cacheKey]?.let { return it }
        val items = call(
            "/BusRouteInfoInqireService/getRouteNoList",
            String.format(
                Locale.US, "&cityCode=%s&routeNo=%s&numOfRows=50",
                cityCode, URLEncoder.encode(busNo, StandardCharsets.UTF_8),
            ),
        )
        // 조회가 부분일치라(예: '10' → 100, 710…) 번호가 정확히 같은 노선을 앞에 둔다
        val exact = ArrayList<JsonNode>()
        val others = ArrayList<JsonNode>()
        for (it in asArray(items)) {
            if (it.path("routeno").asText().equals(busNo, ignoreCase = true)) exact.add(it)
            else others.add(it)
        }
        exact.addAll(others)
        routeCache[cacheKey] = exact
        return exact
    }

    private fun findStations(cityCode: String, routeId: String): List<DoubleArray> {
        val cacheKey = "$cityCode:$routeId"
        stationCache[cacheKey]?.let { return it }
        val items = call(
            "/BusRouteInfoInqireService/getRouteAcctoThrghSttnList",
            String.format(
                Locale.US, "&cityCode=%s&routeId=%s&numOfRows=500",
                cityCode, URLEncoder.encode(routeId, StandardCharsets.UTF_8),
            ),
        )
        val list = asArray(items).sortedBy { it.path("nodeord").asInt() }
        val out = ArrayList<DoubleArray>()
        for (it in list) {
            val lat = it.path("gpslati").asDouble()
            val lng = it.path("gpslong").asDouble()
            if (lat != 0.0 && lng != 0.0) out.add(doubleArrayOf(lat, lng))
        }
        stationCache[cacheKey] = out
        return out
    }

    private fun call(path: String, params: String): JsonNode {
        val url = "$BASE$path?serviceKey=$encodedKey&_type=json&pageNo=1$params"
        val body = http.get().uri(URI.create(url)).retrieve().body(String::class.java).orEmpty()
        val root = try {
            mapper.readTree(body)
        } catch (e: Exception) {
            throw IllegalStateException("JSON 아님: " + body.take(160))
        }
        val code = root.path("response").path("header").path("resultCode").asText()
        if (code != "00") {
            throw IllegalStateException(
                "resultCode=$code " + root.path("response").path("header").path("resultMsg").asText()
            )
        }
        return root.path("response").path("body").path("items").path("item")
    }

    private fun handleFailure(what: String, e: Exception) {
        val msg = e.message.orEmpty()
        if (msg.contains("403")) {
            dead = true
            log.warn("TAGO API 인증 거부(403) — 키 활용신청 상태 확인 필요. 이번 실행에서는 비활성화")
        } else {
            log.warn("TAGO {} 실패: {}", what, msg)
        }
    }

    companion object {
        private const val BASE = "https://apis.data.go.kr/1613000"
        private const val STATION_MATCH_M = 700.0 // 승/하차 정류장 매칭 허용 거리

        /** items.item이 단건(객체)일 수도, 배열일 수도 있어 통일한다 */
        fun asArray(item: JsonNode?): MutableList<JsonNode> {
            val out = ArrayList<JsonNode>()
            if (item == null || item.isMissingNode || item.isNull) return out
            if (item.isArray) item.forEach { out.add(it) } else out.add(item)
            return out
        }

        private fun nearestIndex(stations: List<DoubleArray>, lat: Double, lng: Double): Int {
            var best = -1
            var bestDist = Double.MAX_VALUE
            for (i in stations.indices) {
                val d = GeoUtil.distanceMeters(stations[i][0], stations[i][1], lat, lng)
                if (d < bestDist) {
                    bestDist = d
                    best = i
                }
            }
            return best
        }
    }
}
