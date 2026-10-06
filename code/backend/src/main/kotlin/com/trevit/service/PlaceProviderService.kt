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

        val poi = tmapService.searchPois(q, lat, lng, 5)
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
        return Pool(seed.lodgings, seed.restaurants, seed.attractions, "SEED:${seed.region}")
    }

    private fun toPlaces(pois: List<TmapService.Poi>, type: PlaceType): List<Place> {
        val out = ArrayList<Place>()
        for (poi in pois) {
            if (poi.name.isBlank()) continue
            // 주차장·입구 등 부속 POI 제외
            val n = poi.name
            if (n.contains("주차장") || n.endsWith("입구") || n.contains("화장실")) continue
            val h = Math.abs(n.hashCode())
            val price = when (type) {
                PlaceType.LODGING -> 60_000 + (h % 12) * 10_000          // 6만~17만/박
                PlaceType.RESTAURANT -> 8_000 + (h % 5) * 2_000          // 8천~1.6만/인
                PlaceType.ATTRACTION -> if (h % 3 == 0) 0 else 1_000 + (h % 5) * 1_000 // 무료~5천
            }
            val rating = Math.round((3.8 + (h % 12) * 0.1) * 10) / 10.0  // 3.8~4.9
            val p = Place(
                n, type, poi.address.ifBlank { "주소 정보 없음" },
                poi.lat, poi.lng, price, rating, "TMAP 검색 결과 (가격은 추정)",
            )
            p.id = idSeq.incrementAndGet()
            out.add(p)
        }
        return out
    }

    companion object {
        private const val FETCH_COUNT = 60 // 카테고리당 후보 수
        private const val MIN_USABLE = 6   // 이보다 적으면 시드 폴백
        private const val MUST_VISIT_RADIUS_M = 50_000.0 // 이보다 먼 동명 장소는 다른 지역으로 보고 제외
    }
}
