package com.trevit.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 지역명 → 대표 좌표 내장 테이블.
 * 서비스 범위는 **수도권(서울·경기·인천)** 으로 제한한다 — 좌표는 각 지역의 대표 여행 거점.
 * "가평군"·"인천광역시"처럼 행정 접미사가 붙어도 부분 일치로 찾는다.
 * 수도권 밖 좌표로 요청이 들어오면 서울로 폴백한다.
 */
@Service
class RegionService {

    private val log = LoggerFactory.getLogger(RegionService::class.java)

    data class Region(
        val name: String,
        val lat: Double,
        val lng: Double,
        val aliases: List<String> = emptyList(),
    )

    /** 서비스 기본 지역 — 지역 미지정이거나 서비스 범위 밖일 때 */
    val default = Region("서울", 37.5665, 126.9780, listOf("서울특별시"))

    private val regions: List<Region> = listOf(
        // ── 서울 ──
        default,
        // ── 인천 ──
        Region("인천", 37.4563, 126.7052, listOf("인천광역시", "월미도", "차이나타운")),
        Region("강화", 37.7473, 126.4878, listOf("강화도")),
        Region("송도", 37.3826, 126.6430, listOf("송도국제도시")),
        // ── 경기 ──
        Region("수원", 37.2852, 127.0146, listOf("화성행궁", "수원화성", "행궁동")),
        Region("가평", 37.7909, 127.5210, listOf("남이섬", "자라섬", "쁘띠프랑스")),
        Region("양평", 37.5299, 127.3100, listOf("두물머리", "세미원")),
        Region("파주", 37.8550, 126.7800, listOf("헤이리", "임진각", "프로방스")),
        Region("포천", 37.8949, 127.2003, listOf("아트밸리", "허브아일랜드")),
        Region("용인", 37.2941, 127.2026, listOf("에버랜드", "한국민속촌")),
        Region("남양주", 37.6360, 127.2165, listOf("다산", "물의정원")),
        Region("이천", 37.2721, 127.4350, listOf("도자예술마을", "이천온천")),
        Region("여주", 37.2982, 127.6370, listOf("신륵사", "여주프리미엄아울렛")),
        Region("화성", 37.1780, 126.6180, listOf("제부도", "궁평항")),
        Region("안산", 37.2400, 126.5820, listOf("대부도", "시화나래")),
        Region("시흥", 37.3410, 126.7350, listOf("오이도", "갯골생태공원")),
        Region("김포", 37.6152, 126.7156, listOf("애기봉", "라베니체")),
        Region("과천", 37.4291, 127.0079, listOf("서울대공원", "국립현대미술관")),
        Region("광주", 37.4295, 127.2550, listOf("경기광주", "남한산성", "곤지암")),
        Region("고양", 37.6584, 126.8320, listOf("일산", "호수공원")),
        Region("연천", 38.0966, 127.0748, listOf("한탄강", "재인폭포")),
    )

    /** 수도권 대략 경계 — 이 밖의 좌표는 서비스 범위 밖으로 본다 */
    private val latRange = 36.90..38.30
    private val lngRange = 126.10..127.80

    fun inServiceArea(lat: Double, lng: Double): Boolean = lat in latRange && lng in lngRange

    /** 선택 가능한 지역 목록 (클라이언트 노출용) */
    fun all(): List<Region> = regions

    /**
     * 지역명 해석. 접미사(시/군/구/도/특별시 등) 제거 후 정확 일치 → 별칭 → 부분 일치 순.
     * 못 찾으면 null (호출부에서 startLatitude 또는 default 사용).
     */
    fun resolve(raw: String?): Region? {
        val q = raw?.trim().orEmpty()
        if (q.isEmpty()) return null
        val norm = q
            .replace(Regex("(특별자치시|특별자치도|특별시|광역시)$"), "")
            .replace(Regex("(시|군|구|읍|면|도)$"), "")
            .trim()
            .ifEmpty { q }
        val found = regions.firstOrNull { it.name == norm }
            ?: regions.firstOrNull { norm in it.aliases || q in it.aliases }
            ?: regions.firstOrNull { it.name == q }
            ?: regions.firstOrNull { it.name.contains(norm) || norm.contains(it.name) }
            ?: regions.firstOrNull { r -> r.aliases.any { norm.contains(it) || it.contains(norm) } }
        if (found != null) {
            log.info("지역명 해석: '{}' → {} ({}, {})", raw, found.name, found.lat, found.lng)
        } else {
            log.warn("지역명 해석 실패: '{}' — 서비스 지역(수도권)이 아닐 수 있음", raw)
        }
        return found
    }
}
