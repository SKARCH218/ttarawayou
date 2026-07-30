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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * 공공데이터포털 — 국토교통부(TAGO) 시외버스정보.
 * 시외 이동(도시 간 30km 이상) 구간에서 실제 시외버스 터미널·시간표·요금을 조회한다.
 * 키가 이 API에 활용신청되지 않았으면(403) 자동 비활성화되고 기존 방식으로 폴백한다.
 */
@Service
class IntercityBusService(@Value("\${datago.api-key}") apiKey: String?) {

    private val log = LoggerFactory.getLogger(IntercityBusService::class.java)

    /** 다음 시외버스 배차 정보 */
    data class Departure(
        val depTerminalNm: String, val arrTerminalNm: String,
        val depTime: String, val arrTime: String, val charge: Long, val grade: String,
    )

    private data class Terminal(val id: String, val name: String)

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
    private var dead = false

    private val terminalCache = ConcurrentHashMap<String, List<Terminal>>()
    private val timetableCache = ConcurrentHashMap<String, List<JsonNode>>()

    fun enabled(): Boolean = encodedKey.isNotEmpty() && !dead

    /**
     * 두 도시 사이의 다음 시외버스 배차 (지금 이후 첫 출발).
     * 시내버스 도시코드의 앞 2자리(광역 코드)로 변환하고, 도시명으로 터미널을 거른다.
     * 터미널 조합을 순서대로 시도하고, 배차가 없으면 null.
     */
    fun nextDeparture(
        depCityCode: String?, depCityName: String?,
        arrCityCode: String?, arrCityName: String?,
    ): Departure? {
        if (!enabled() || depCityCode == null || arrCityCode == null || depCityCode == arrCityCode) {
            return null
        }
        try {
            val deps = terminalsForCity(depCityCode, depCityName)
            val arrs = terminalsForCity(arrCityCode, arrCityName)
            val today = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.BASIC_ISO_DATE)
            val nowStamp = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")).toLong()

            for (dep in deps.take(3)) {
                for (arr in arrs.take(3)) {
                    for (t in timetable(dep.id, arr.id, today)) {
                        val depPland = t.path("depplandtime").asLong(t.path("depPlandTime").asLong(0))
                        if (depPland < nowStamp) continue // 이미 지난 배차
                        val arrPland = t.path("arrplandtime").asLong(t.path("arrPlandTime").asLong(0))
                        val d = Departure(
                            dep.name, arr.name,
                            hhmm(depPland), hhmm(arrPland),
                            t.path("charge").asLong(0),
                            t.path("gradeNm").asText(t.path("gradenm").asText("일반")),
                        )
                        log.info("시외버스 배차 매칭: {} → {} · {} 출발 · {}원",
                            d.depTerminalNm, d.arrTerminalNm, d.depTime, d.charge)
                        return d
                    }
                }
            }
        } catch (e: Exception) {
            handleFailure(e)
        }
        return null
    }

    // ---------- API ----------

    /** 시내버스 도시코드 + 도시명 → 해당 도시의 시외버스 터미널 후보 */
    private fun terminalsForCity(cityCode: String, cityName: String?): List<Terminal> {
        val province = if (cityCode.length >= 2) cityCode.substring(0, 2) else cityCode
        val all = terminals(province)
        val nameKey = cityName.orEmpty()
            .replace(Regex("(특별자치시|특별자치도|특별시|광역시|시|군)$"), "").trim()
        if (nameKey.isNotEmpty()) {
            val filtered = all.filter { it.name.contains(nameKey) }
            if (filtered.isNotEmpty()) return filtered
        }
        // 광역시(도시코드 2자리)는 광역 전체가 곧 그 도시라 전체 목록을 그대로 쓴다
        return if (cityCode.length <= 2) all else emptyList()
    }

    private fun terminals(provinceCode: String): List<Terminal> {
        terminalCache[provinceCode]?.let { return it }
        val items = call(
            "/GetSuberbsBusTrminlList",
            "&cityCode=" + URLEncoder.encode(provinceCode, StandardCharsets.UTF_8) + "&numOfRows=300",
        )
        val out = ArrayList<Terminal>()
        for (it in PublicBusService.asArray(items)) {
            val id = it.path("terminalId").asText(it.path("terminalid").asText(""))
            val nm = it.path("terminalNm").asText(it.path("terminalnm").asText(""))
            if (id.isNotEmpty()) out.add(Terminal(id, nm))
        }
        // 대표 터미널(이름에 '시외'가 들어가거나 이름이 짧은 곳)을 앞으로
        out.sortBy { (if (it.name.contains("시외")) 0 else 1) * 100 + it.name.length }
        terminalCache[provinceCode] = out
        return out
    }

    private fun timetable(depId: String, arrId: String, date: String): List<JsonNode> {
        val key = "$depId>$arrId@$date"
        timetableCache[key]?.let { return it }
        val items = call(
            "/GetStrtpntAlocFndSuberbsBusInfo",
            "&depTerminalId=" + URLEncoder.encode(depId, StandardCharsets.UTF_8) +
                "&arrTerminalId=" + URLEncoder.encode(arrId, StandardCharsets.UTF_8) +
                "&depPlandTime=" + date + "&numOfRows=50",
        )
        val out = PublicBusService.asArray(items)
            .sortedBy { it.path("depplandtime").asLong(it.path("depPlandTime").asLong(0)) }
        timetableCache[key] = out
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

    private fun handleFailure(e: Exception) {
        val msg = e.message.orEmpty()
        if (msg.contains("403")) {
            dead = true
            log.warn("시외버스정보 API 인증 거부(403) — 활용신청 필요. 이번 실행에서는 비활성화")
        } else {
            log.warn("시외버스정보 조회 실패: {}", msg)
        }
    }

    companion object {
        private const val BASE = "https://apis.data.go.kr/1613000/SuburbsBusInfo"

        private fun hhmm(plandTime: Long): String {
            val s = plandTime.toString()
            return if (s.length >= 12) s.substring(8, 10) + ":" + s.substring(10, 12) else ""
        }
    }
}
