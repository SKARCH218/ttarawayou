package com.trevit.service

import com.trevit.entity.Place
import com.trevit.entity.Place.PlaceType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 플랜에 쓸 장소 후보 공급자 — 전국 대응 + 시드 폴백.
 * 1순위: 기준 좌표 주변의 관광명소·숙박·음식점을 TMAP POI로 실시간 조회.
 * 폴백(시드): (a) TMAP 키 없음 (b) 429/QUOTA_EXCEEDED (c) 결과 부족 →
 *            시작 좌표에서 가장 가까운 충남·대전 시드 클러스터(SeedPlaceService) 사용.
 * TMAP POI에는 가격·평점이 없어 이름 해시 기반의 결정적 추정값을 부여한다(테스트용).
 */
@Service
class PlaceProviderService(
    private val tmapService: TmapService,
    private val seedPlaceService: SeedPlaceService,
) {

    private val log = LoggerFactory.getLogger(PlaceProviderService::class.java)

    data class Pool(
        val lodgings: List<Place>,
        val restaurants: List<Place>,
        val attractions: List<Place>,
        val source: String,
    ) {
        fun all(): List<Place> = lodgings + restaurants + attractions
    }

    private val cache = ConcurrentHashMap<String, Pool>()
    private val prefCache = ConcurrentHashMap<String, List<TmapService.Poi>>()
    private val idSeq = AtomicLong(1_000_000)

    fun places(lat: Double, lng: Double): Pool {
        val key = String.format(Locale.US, "%.2f,%.2f", lat, lng)
        cache[key]?.let { return it }
        val pool = fetch(lat, lng)
        cache[key] = pool
        return pool
    }

    /**
     * "꼭 가고 싶은 곳" 이름을 실제 장소로 찾는다.
     * 후보 목록에 이미 있으면 그걸 쓰고, 없으면 TMAP 이름 검색(기준점 50km 이내)으로 찾는다.
     */
    fun findByName(name: String, lat: Double, lng: Double, pool: Pool): Place? {
        val q = name.trim()
        if (q.length < 2) return null
        pool.all()
            .filter { it.type != PlaceType.LODGING && it.name.length >= 2 }
            .firstOrNull { it.name.contains(q) || q.contains(it.name) }
            ?.let { return it }

        val poi = tmapService.searchPois(q, lat, lng, 5, radiusKm = 0)
            .firstOrNull { GeoUtil.distanceMeters(lat, lng, it.lat, it.lng) <= MUST_VISIT_RADIUS_M }
            ?: return null
        val type = if (Regex("음식|식당|카페|주점|베이커리").containsMatchIn(poi.category)) PlaceType.RESTAURANT
            else PlaceType.ATTRACTION
        return toPlaces(listOf(poi), type).firstOrNull()
    }

    private fun fetch(lat: Double, lng: Double): Pool {
        // (a) 키 없음 / (b) 쿼터 초과 → 즉시 시드 폴백 (재시도 금지)
        if (tmapService.usable()) {
            val fromTmap = fetchFromTmap(lat, lng)
            if (fromTmap != null) return fromTmap
            // (c) TMAP 결과 부족 또는 호출 중 쿼터 초과 → 시드 폴백
        } else {
            log.info("TMAP 사용 불가(키 없음 또는 쿼터 초과) → 시드 장소 폴백")
        }
        return fetchFromSeed(lat, lng)
    }

    private fun fetchFromTmap(lat: Double, lng: Double): Pool? {
        val lodgings = toPlaces(tmapService.poisAround(lat, lng, "숙박", FETCH_COUNT), PlaceType.LODGING)
        val restaurants = toPlaces(tmapService.poisAround(lat, lng, "음식점", FETCH_COUNT), PlaceType.RESTAURANT)
        val attractions = toPlaces(tmapService.poisAround(lat, lng, "관광명소", FETCH_COUNT), PlaceType.ATTRACTION)
        if (restaurants.size < MIN_USABLE || attractions.size < MIN_USABLE) {
            log.warn("TMAP 장소 부족(식당 {}, 관광 {}) → 시드 폴백", restaurants.size, attractions.size)
            return null
        }
        val region = restaurants[0].address.takeIf { it.split(" ").size > 1 } ?: "현재 지역"
        log.info("TMAP 지역 장소 조회 완료 [{}]: 숙박 {}, 식당 {}, 관광 {}",
            region, lodgings.size, restaurants.size, attractions.size)
        return Pool(lodgings, restaurants, attractions, "TMAP:$region")
    }

    private fun fetchFromSeed(lat: Double, lng: Double): Pool {
        val seed = seedPlaceService.poolNear(lat, lng)
        // 시드는 수도권 몇 곳뿐이다. 멀리 떨어진 지역(예: 부산)을 서울 시드로 짜면 엉뚱한 일정이 되므로 거절한다.
        val spots = seed.attractions + seed.restaurants
        if (spots.isNotEmpty()) {
            val cLat = spots.map { it.latitude }.average()
            val cLng = spots.map { it.longitude }.average()
            require(GeoUtil.distanceMeters(lat, lng, cLat, cLng) <= SEED_MAX_DISTANCE_M) {
                "이 지역은 실시간 장소 검색이 켜져 있어야 플랜을 만들 수 있어요. 잠시 후 다시 시도해 주세요."
            }
        }
        return Pool(seed.lodgings, seed.restaurants, seed.attractions, "SEED:${seed.region}")
    }

    /** 키워드 하나로 장소 검색 (AI 도구 search_places 용). 찾은 장소에는 검색어를 태그로 붙인다 */
    fun searchKeyword(query: String, type: PlaceType, lat: Double, lng: Double, radiusM: Double): List<Place> =
        preferencePlaces(lat, lng, listOf(Triple(query, type, query)), radiusM)

    /**
     * 취향에 맞는 장소를 따로 찾아온다 (검색어마다 기준점에서 가까운 순, [radiusM] 이내).
     * 찾은 장소에는 취향 라벨을 태그로 붙여 점수 계산에서 알아볼 수 있게 한다.
     */
    fun preferencePlaces(
        lat: Double, lng: Double, searches: List<Triple<String, PlaceType, String>>, radiusM: Double,
    ): List<Place> {
        if (!tmapService.usable()) return emptyList()
        return searches.flatMap { (keyword, type, label) ->
            val key = String.format(Locale.US, "%.2f,%.2f,%s", lat, lng, keyword)
            val pois = prefCache.getOrPut(key) { tmapService.searchPois(keyword, lat, lng, PREF_FETCH_COUNT, radiusKm = Math.ceil(radiusM / 1000).toInt().coerceIn(1, 33)) }
            toPlaces(pois.filter { GeoUtil.distanceMeters(lat, lng, it.lat, it.lng) <= radiusM }, type,
                "취향 검색: $label").onEach { it.tags = setOf(label) }
        }
    }

    /**
     * TMAP에는 가격이 없어 이름·업종으로 등급을 나눠 추정한다 (같은 이름은 늘 같은 값).
     * 등급이 없으면 예산이 커도 쓸 곳이 없어 플랜 금액이 예산에 한참 못 미친다.
     */
    private fun estimatePrice(text: String, type: PlaceType, h: Int): Int {
        fun band(min: Int, max: Int, step: Int) = min + (h % ((max - min) / step + 1)) * step
        return when (type) {
            PlaceType.LODGING -> when {
                Regex("호텔|리조트|스위트").containsMatchIn(text) -> band(150_000, 350_000, 10_000)
                Regex("게스트하우스|호스텔|게하|민박").containsMatchIn(text) -> band(35_000, 70_000, 5_000)
                Regex("한옥|풀빌라|글램핑").containsMatchIn(text) -> band(120_000, 280_000, 10_000)
                else -> band(60_000, 170_000, 10_000)                                   // 모텔·펜션 등
            }
            PlaceType.RESTAURANT -> when {
                Regex("오마카세|파인다이닝|코스|한우|스테이크하우스|호텔 ?뷔페").containsMatchIn(text) -> band(50_000, 120_000, 5_000)
                Regex("횟집|대게|장어|소고기|갈비|한정식|스시|초밥|뷔페|와인|이자카야").containsMatchIn(text) -> band(25_000, 45_000, 5_000)
                Regex("카페|커피|베이커리|디저트|빙수|제과").containsMatchIn(text) -> band(6_000, 14_000, 1_000)
                Regex("분식|국밥|김밥|떡볶이|칼국수|백반").containsMatchIn(text) -> band(7_000, 11_000, 1_000)
                else -> band(10_000, 22_000, 2_000)
            }
            PlaceType.ATTRACTION -> when {
                Regex("테마파크|놀이공원|워터파크|아쿠아리움|랜드$|월드$").containsMatchIn(text) -> band(35_000, 60_000, 5_000)
                Regex("케이블카|스카이|전망대|루지|짚라인|체험|공방|클래스|스파|온천|찜질|요트|카약|서핑").containsMatchIn(text) ->
                    band(12_000, 30_000, 2_000)
                Regex("박물관|미술관|궁$|성$|기념관|과학관|전시").containsMatchIn(text) -> if (h % 3 == 0) 0 else band(2_000, 8_000, 1_000)
                Regex("공원|해변|해수욕장|산$|시장|거리|골목|마을|강변|호수").containsMatchIn(text) -> 0
                else -> if (h % 3 == 0) 0 else band(1_000, 6_000, 1_000)
            }
        }
    }

    private fun toPlaces(
        pois: List<TmapService.Poi>, type: PlaceType,
        description: String = "TMAP 검색 결과 (가격은 추정)",
    ): List<Place> {
        val out = ArrayList<Place>()
        for (poi in pois) {
            if (poi.name.isBlank()) continue
            // 주차장·입구 등 부속 POI 제외
            val n = poi.name
            if (n.contains("주차장") || n.endsWith("입구") || n.contains("화장실")) continue
            val h = Math.abs(n.hashCode())
            val price = estimatePrice("$n ${poi.category}", type, h)
            val rating = Math.round((3.8 + (h % 12) * 0.1) * 10) / 10.0  // 3.8~4.9
            val p = Place(
                n, type, poi.address.ifBlank { "주소 정보 없음" },
                poi.lat, poi.lng, price, rating, description,
            )
            p.id = idSeq.incrementAndGet()
            out.add(p)
        }
        return out
    }

    companion object {
        private const val FETCH_COUNT = 100 // 카테고리당 후보 수 (긴 여행도 날마다 다른 곳을 고를 수 있게, TMAP 최대 200)
        private const val MIN_USABLE = 6   // 이보다 적으면 시드 폴백
        private const val MUST_VISIT_RADIUS_M = 50_000.0 // 이보다 먼 동명 장소는 다른 지역으로 보고 제외
        private const val SEED_MAX_DISTANCE_M = 60_000.0 // 시드 장소가 이보다 멀면 그 지역 시드가 없는 것
        private const val PREF_FETCH_COUNT = 10          // 취향 검색어당 가져올 장소 수
    }
}
