package com.trevit.service

import com.trevit.dto.PlanDtos.PlanRequest
import com.trevit.dto.PlanDtos.PlanResponse
import com.trevit.entity.Place
import com.trevit.entity.Wallet
import com.trevit.repository.WalletRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.Optional
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 하루 = 동네 하나: 하루 일정의 장소들이 그날 동네 안에 모여 있어야 한다.
 * (외부 API 키 없이 시드 장소로 돌리고, 길찾기 서버는 꺼진 주소로 둬 직선거리로만 계산한다)
 */
class PlanRouteCompactnessTest {

    private fun planService(): PlanService {
        val wallets = Mockito.mock(WalletRepository::class.java)
        Mockito.`when`(wallets.findById(1L)).thenReturn(Optional.of(Wallet(1L, Wallet.INITIAL_BALANCE)))
        val tmap = TmapService(null)
        val route = RouteService(null, "http://127.0.0.1:1", PublicBusService(null), IntercityBusService(null), tmap)
        val ai = AiPlanService("http://127.0.0.1:1/v1", "none", false, 1000, "", 4000)
        return PlanService(route, ai, wallets, PlaceProviderService(tmap, SeedPlaceService()), RegionService())
    }

    /** TMAP처럼 기준점 반경 15km에 흩어진 장소 후보 (평점·가격은 무작위, 시드 고정) */
    private fun scatteredPool(lat: Double, lng: Double): PlaceProviderService.Pool {
        val rnd = Random(42)
        var id = 9_000_000L
        fun make(type: Place.PlaceType, n: Int, price: () -> Int) = List(n) {
            val r = 15_000.0 * sqrt(rnd.nextDouble())
            val a = rnd.nextDouble() * 2 * PI
            val pLat = lat + r * cos(a) / 111_320.0
            val pLng = lng + r * sin(a) / (111_320.0 * cos(lat * PI / 180))
            Place("${type.name}-$it", type, "서울", pLat, pLng, price(),
                (38 + rnd.nextInt(12)) / 10.0, "테스트").also { p -> p.id = id++ }
        }
        return PlaceProviderService.Pool(
            make(Place.PlaceType.LODGING, 60) { 60_000 + rnd.nextInt(12) * 10_000 },
            make(Place.PlaceType.RESTAURANT, 60) { 8_000 + rnd.nextInt(5) * 2_000 },
            make(Place.PlaceType.ATTRACTION, 60) { if (rnd.nextInt(3) == 0) 0 else 1_000 + rnd.nextInt(5) * 1_000 },
            "TEST",
        )
    }

    @Test
    fun `흩어진 후보에서도 하루 장소들은 한 동네 안에 모인다`() {
        val wallets = Mockito.mock(WalletRepository::class.java)
        Mockito.`when`(wallets.findById(1L)).thenReturn(Optional.of(Wallet(1L, Wallet.INITIAL_BALANCE)))
        val tmap = TmapService(null)
        val route = RouteService(null, "http://127.0.0.1:1", PublicBusService(null), IntercityBusService(null), tmap)
        val ai = AiPlanService("http://127.0.0.1:1/v1", "none", false, 1000, "", 4000)
        val provider = Mockito.mock(PlaceProviderService::class.java)
        Mockito.`when`(provider.places(Mockito.anyDouble(), Mockito.anyDouble())).thenReturn(scatteredPool(37.5665, 126.9780))
        val plan = PlanService(route, ai, wallets, provider, RegionService()).createPlan(
            PlanRequest(budget = 300_000, days = 2, people = 1, region = "서울",
                startLatitude = 37.5563, startLongitude = 126.9236),
        )
        assertCompact(plan)
    }

    @Test
    fun `하루 장소들은 한 동네 안에 모인다`() {
        val plan = planService().createPlan(
            PlanRequest(budget = 300_000, days = 2, people = 1, region = "서울",
                startLatitude = 37.5563, startLongitude = 126.9236), // 홍대입구역
        )
        assertCompact(plan)
    }

    private fun assertCompact(plan: PlanResponse) {
        for (day in plan.dayPlans) {
            val spots = day.stops.filter { it.type == "ATTRACTION" || it.type == "RESTAURANT" }
            if (spots.isEmpty()) continue
            val cLat = spots.map { it.latitude }.average()
            val cLng = spots.map { it.longitude }.average()
            val maxFromCenter = spots.maxOf { GeoUtil.distanceMeters(cLat, cLng, it.latitude, it.longitude) }
            val legsBetweenSpots = day.legs.drop(1).dropLast(1) // 출발·숙소 복귀 구간 제외
            val maxLeg = legsBetweenSpots.maxOfOrNull {
                GeoUtil.distanceMeters(it.path.first()[0], it.path.first()[1], it.path.last()[0], it.path.last()[1])
            } ?: 0.0
            println("Day ${day.day}: 장소 ${spots.size}곳, 중심에서 최대 ${maxFromCenter.toInt()}m, " +
                "장소 간 최대 직선 ${maxLeg.toInt()}m — ${spots.joinToString { it.name }}")
            assertTrue(maxFromCenter <= 4_000.0, "Day ${day.day} 장소가 동네 밖으로 흩어짐: ${maxFromCenter.toInt()}m")
        }
    }
}
