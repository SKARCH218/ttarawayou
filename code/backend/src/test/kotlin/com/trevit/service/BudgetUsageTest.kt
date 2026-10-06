package com.trevit.service

import com.trevit.dto.PlanDtos.PlanRequest
import com.trevit.entity.Wallet
import com.trevit.repository.WalletRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.Optional
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** 예산 대비 실제 사용액 측정 (시드 장소, 외부 API 없음) */
class BudgetUsageTest {

    private fun service(): PlanService {
        val wallets = Mockito.mock(WalletRepository::class.java)
        Mockito.`when`(wallets.findById(1L)).thenAnswer { Optional.of(Wallet(1L, 5_000_000)) }
        val tmap = TmapService(null)
        val route = RouteService(null, "http://127.0.0.1:1", PublicBusService(null), IntercityBusService(null), tmap)
        val ai = AiPlanService("http://127.0.0.1:1/v1", "none", false, 1000, "", 4000)
        return PlanService(route, ai, wallets, PlaceProviderService(tmap, SeedPlaceService()), RegionService(tmap))
    }

    /** 실제 TMAP 가격 등급처럼 저렴~고급이 섞인 후보 (기준점 반경 4km) */
    private fun tieredPool(lat: Double, lng: Double): PlaceProviderService.Pool {
        val rnd = Random(7)
        var id = 8_000_000L
        fun make(type: com.trevit.entity.Place.PlaceType, n: Int, price: () -> Int) = List(n) {
            val r = 4_000.0 * sqrt(rnd.nextDouble()); val a = rnd.nextDouble() * 2 * PI
            com.trevit.entity.Place("${type.name}-$it", type, "서울", lat + r * cos(a) / 111_320.0,
                lng + r * sin(a) / (111_320.0 * cos(lat * PI / 180)), price(), (38 + rnd.nextInt(12)) / 10.0, "")
                .also { p -> p.id = id++ }
        }
        fun pick(vararg bands: IntRange): Int {
            val b = bands[rnd.nextInt(bands.size)]
            return (b.first + rnd.nextInt(b.last - b.first + 1)) / 1000 * 1000
        }
        return PlaceProviderService.Pool(
            make(com.trevit.entity.Place.PlaceType.LODGING, 40) { pick(35_000..70_000, 60_000..170_000, 150_000..350_000) },
            make(com.trevit.entity.Place.PlaceType.RESTAURANT, 80) { pick(7_000..11_000, 10_000..22_000, 25_000..45_000, 50_000..120_000) },
            make(com.trevit.entity.Place.PlaceType.ATTRACTION, 80) { pick(0..0, 1_000..6_000, 12_000..30_000, 35_000..60_000) },
            "TEST",
        )
    }

    @Test
    fun `가격대가 다양한 후보에서 예산 사용률`() {
        val wallets = Mockito.mock(WalletRepository::class.java)
        Mockito.`when`(wallets.findById(1L)).thenAnswer { Optional.of(Wallet(1L, 5_000_000)) }
        val tmap = TmapService(null)
        val route = RouteService(null, "http://127.0.0.1:1", PublicBusService(null), IntercityBusService(null), tmap)
        val ai = AiPlanService("http://127.0.0.1:1/v1", "none", false, 1000, "", 4000)
        val provider = Mockito.mock(PlaceProviderService::class.java)
        Mockito.`when`(provider.places(Mockito.anyDouble(), Mockito.anyDouble())).thenReturn(tieredPool(37.5665, 126.9780))
        val s = PlanService(route, ai, wallets, provider, RegionService(tmap))
        for (days in listOf(1, 2, 3)) for (budget in listOf(100_000L, 300_000L, 600_000L, 1_000_000L)) {
            val plan = runCatching { s.createPlan(PlanRequest(budget = budget, days = days, people = 2, region = "서울")) }
                .getOrElse { println("TIERED days=$days budget=$budget → 거절: ${it.message}"); continue }
            println("TIERED days=$days budget=$budget total=${plan.totalCost} (${plan.totalCost * 100 / budget}%)")
            org.junit.jupiter.api.Assertions.assertTrue(plan.totalCost <= budget, "예산 초과: ${plan.totalCost} > $budget")
        }
    }

    @Test
    fun `긴 여행·많은 인원도 날마다 일정이 채워진다`() {
        val wallets = Mockito.mock(WalletRepository::class.java)
        Mockito.`when`(wallets.findById(1L)).thenAnswer { Optional.of(Wallet(1L, 50_000_000)) }
        val tmap = TmapService(null)
        val route = RouteService(null, "http://127.0.0.1:1", PublicBusService(null), IntercityBusService(null), tmap)
        val ai = AiPlanService("http://127.0.0.1:1/v1", "none", false, 1000, "", 4000)
        val provider = Mockito.mock(PlaceProviderService::class.java)
        Mockito.`when`(provider.places(Mockito.anyDouble(), Mockito.anyDouble())).thenReturn(tieredPool(37.5665, 126.9780))
        val s = PlanService(route, ai, wallets, provider, RegionService(tmap))
        for ((days, people) in listOf(7 to 2, 10 to 6, 14 to 10)) {
            val budget = days * people * 150_000L
            val plan = s.createPlan(PlanRequest(budget = budget, days = days, people = people, region = "서울"))
            val counts = plan.dayPlans.map { d ->
                d.stops.count { it.type == "ATTRACTION" } to d.stops.count { it.type == "RESTAURANT" }
            }
            println("LONG days=$days people=$people budget=$budget total=${plan.totalCost} " +
                "(${plan.totalCost * 100 / budget}%) 일자별(관광,식당)=$counts")
            org.junit.jupiter.api.Assertions.assertEquals(days, plan.dayPlans.size)
            org.junit.jupiter.api.Assertions.assertTrue(counts.all { (a, r) -> a + r > 0 }, "빈 날이 있음: $counts")
        }
    }

    @Test
    fun `예산 사용률 측정`() {
        val s = service()
        for (days in listOf(1, 2, 3)) for (budget in listOf(100_000L, 300_000L, 600_000L, 1_000_000L)) {
            val plan = runCatching { s.createPlan(PlanRequest(budget = budget, days = days, people = 2, region = "서울")) }
                .getOrElse { println("USAGE days=$days budget=$budget → 거절: ${it.message}"); continue }
            org.junit.jupiter.api.Assertions.assertTrue(plan.totalCost <= budget, "예산 초과: ${plan.totalCost} > $budget")
            val b = plan.breakdown
            println("USAGE days=$days budget=$budget total=${plan.totalCost} (${plan.totalCost * 100 / budget}%) " +
                "숙박=${b.lodgingSpent} 관광=${b.attractionSpent} 식비=${b.foodSpent} 교통=${b.transportSpent}")
        }
    }
}
