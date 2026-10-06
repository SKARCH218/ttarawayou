package com.trevit.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 지역명 → 대표 좌표 내장 테이블.
 * 서비스 범위는 **전국** — 좌표는 각 지역의 대표 여행 거점.
 * "가평군"·"부산광역시"처럼 행정 접미사가 붙어도 부분 일치로 찾는다.
 * 국내 밖 좌표로 요청이 들어오면 서울로 폴백한다.
 */
@Service
class RegionService(private val tmapService: TmapService) {

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
        Region("경기광주", 37.4295, 127.2550, listOf("경기 광주", "남한산성", "곤지암")),
        Region("고양", 37.6584, 126.8320, listOf("일산", "호수공원")),
        Region("연천", 38.0966, 127.0748, listOf("한탄강", "재인폭포")),
        // ── 강원 ──
        Region("춘천", 37.8813, 127.7298, listOf("소양강", "닭갈비골목", "레고랜드")),
        Region("강릉", 37.7519, 128.8761, listOf("경포대", "안목해변", "주문진")),
        Region("속초", 38.2070, 128.5918, listOf("설악산", "속초해수욕장", "아바이마을")),
        Region("양양", 38.0754, 128.6189, listOf("서피비치", "낙산사")),
        Region("평창", 37.3705, 128.3903, listOf("대관령", "오대산")),
        Region("원주", 37.3422, 127.9202, listOf("소금산", "뮤지엄산")),
        Region("정선", 37.3807, 128.6608, listOf("정선아리랑시장", "하이원")),
        // ── 충청 ──
        Region("대전", 36.3504, 127.3845, listOf("대전광역시", "성심당", "엑스포")),
        Region("세종", 36.4800, 127.2890, listOf("세종특별자치시", "세종호수공원")),
        Region("청주", 36.6424, 127.4890, listOf("수암골", "상당산성")),
        Region("충주", 36.9910, 127.9259, listOf("충주호", "탄금대")),
        Region("단양", 36.9846, 128.3655, listOf("도담삼봉", "만천하스카이워크")),
        Region("천안", 36.8151, 127.1139, listOf("독립기념관", "각원사")),
        Region("공주", 36.4466, 127.1190, listOf("공산성", "무령왕릉")),
        Region("부여", 36.2757, 126.9098, listOf("궁남지", "부소산성")),
        Region("태안", 36.7456, 126.2980, listOf("만리포", "안면도", "꽃지해수욕장")),
        Region("보령", 36.3333, 126.6127, listOf("대천해수욕장", "머드축제")),
        // ── 전라 ──
        Region("광주", 35.1595, 126.8526, listOf("광주광역시", "양림동", "무등산")),
        Region("전주", 35.8150, 127.1530, listOf("전주한옥마을", "남부시장")),
        Region("군산", 35.9676, 126.7366, listOf("경암동철길마을", "근대역사박물관")),
        Region("여수", 34.7604, 127.6622, listOf("오동도", "여수밤바다", "돌산")),
        Region("순천", 34.9507, 127.4872, listOf("순천만", "낙안읍성")),
        Region("목포", 34.8118, 126.3922, listOf("유달산", "목포해상케이블카")),
        Region("담양", 35.3211, 126.9882, listOf("죽녹원", "메타세쿼이아길")),
        // ── 경상 ──
        Region("부산", 35.1580, 129.0600, listOf("부산광역시", "해운대", "광안리", "남포동", "서면")),
        Region("대구", 35.8714, 128.6014, listOf("대구광역시", "동성로", "김광석길")),
        Region("울산", 35.5384, 129.3114, listOf("울산광역시", "태화강", "간절곶")),
        Region("경주", 35.8562, 129.2247, listOf("불국사", "황리단길", "첨성대", "보문단지")),
        Region("안동", 36.5684, 128.7294, listOf("하회마을", "월영교")),
        Region("포항", 36.0190, 129.3435, listOf("호미곶", "영일대", "구룡포")),
        Region("통영", 34.8544, 128.4331, listOf("동피랑", "미륵산", "통영케이블카")),
        Region("거제", 34.8806, 128.6211, listOf("바람의언덕", "외도", "학동몽돌해변")),
        Region("남해", 34.8375, 127.8925, listOf("독일마을", "다랭이마을")),
        Region("진주", 35.1800, 128.1076, listOf("진주성", "남강")),
        Region("창원", 35.2280, 128.6811, listOf("마산", "진해", "경화역")),
        // ── 제주 ──
        Region("제주", 33.4996, 126.5312, listOf("제주시", "제주도", "애월", "협재", "함덕")),
        Region("서귀포", 33.2541, 126.5600, listOf("중문", "성산일출봉", "올레시장")),
    )

    /** 국내 대략 경계(제주·울릉 포함) — 이 밖의 좌표는 서비스 범위 밖으로 본다 */
    private val latRange = 33.0..38.7
    private val lngRange = 124.5..131.0

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
        // 순서: 거점 표 정확 일치 → 별칭 → (표에 없는 시·군) TMAP으로 시청·군청 좌표 → 부분 일치.
        // 부분 일치를 마지막에 두는 이유: "양주"가 "남양주"에 잘못 걸리지 않게.
        val found = regions.firstOrNull { it.name == norm }
            ?: regions.firstOrNull { norm in it.aliases || q in it.aliases }
            ?: regions.firstOrNull { it.name == q }
            ?: geocode(q)
            ?: regions.firstOrNull { it.name.contains(norm) || norm.contains(it.name) }
            ?: regions.firstOrNull { r -> r.aliases.any { norm.contains(it) || it.contains(norm) } }
        if (found != null) {
            log.info("지역명 해석: '{}' → {} ({}, {})", raw, found.name, found.lat, found.lng)
        } else {
            log.warn("지역명 해석 실패: '{}'", raw)
        }
        return found
    }

    private val geocoded = HashMap<String, Region?>()

    /**
     * 거점 표에 없는 시·군은 TMAP에서 "○○시청"·"○○군청"을 찾아 그 좌표를 쓴다 (결과는 캐시).
     * 이름이 겹치는 곳은 도 이름을 앞에 붙여 보낸다 (예: "강원 고성", "경남 고성") —
     * 그 도의 중심에서 가까운 순으로 찾아 맞는 쪽을 고른다.
     */
    @Synchronized
    private fun geocode(q: String): Region? {
        if (geocoded.containsKey(q)) return geocoded[q]
        if (!tmapService.usable()) return null
        val parts = q.split(" ").filter { it.isNotBlank() }
        val province = parts.firstOrNull()?.let { PROVINCE_CENTERS[it] }
        val base = (if (province != null) parts.drop(1).joinToString(" ") else q)
            .replace(Regex("(시|군)$"), "")
        val center = province ?: (36.5 to 127.8)
        val region = listOf("${base}시청", "${base}군청", base).firstNotNullOfOrNull { query ->
            tmapService.searchPois(query, center.first, center.second, 3)
                .firstOrNull { it.name.startsWith(base) }
                ?.let { Region(q, it.lat, it.lng) }
        }
        geocoded[q] = region
        return region
    }

    companion object {
        /** 이름이 겹치는 시·군을 구분할 때 쓰는 도 중심 좌표 */
        private val PROVINCE_CENTERS = mapOf(
            "경기" to (37.4 to 127.2), "강원" to (37.8 to 128.2), "충북" to (36.8 to 127.7),
            "충남" to (36.5 to 126.8), "전북" to (35.7 to 127.1), "전남" to (34.9 to 126.9),
            "경북" to (36.4 to 128.9), "경남" to (35.3 to 128.3), "제주" to (33.4 to 126.6),
        )
    }
}
