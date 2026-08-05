package com.trevit.app

import com.trevit.shared.BudgetBreakdown
import com.trevit.shared.DayPlanDto
import com.trevit.shared.LegDto
import com.trevit.shared.PlanResponse
import com.trevit.shared.StepDto
import com.trevit.shared.StopDto

/**
 * 백엔드 없이도 시연 가능한 내장 데모 플랜 (대전 1일 코스).
 * 서버 연결 실패 시 폴백으로 사용한다.
 */
fun buildDemoPlan(budget: Long, days: Int, people: Int, region: String? = null): PlanResponse {
    val start = StopDto(
        placeId = 0, name = "대전 시청", type = "START", address = "대전 서구 둔산로 100",
        latitude = 36.3504, longitude = 127.3845, cost = 0, rating = 0.0,
        description = "여행의 시작점",
    )
    val stop1 = StopDto(
        placeId = 1, name = "성심당 본점", type = "RESTAURANT", address = "대전 중구 대종로480번길 15",
        latitude = 36.3277, longitude = 127.4272, cost = 12_000L * people, rating = 4.8,
        description = "대전의 자부심, 튀김소보로의 원조 빵집",
    )
    val stop2 = StopDto(
        placeId = 2, name = "한밭수목원", type = "ATTRACTION", address = "대전 서구 둔산대로 169",
        latitude = 36.3672, longitude = 127.3881, cost = 0, rating = 4.5,
        description = "도심 속 전국 최대 인공 수목원",
    )
    val stop3 = StopDto(
        placeId = 3, name = "엑스포 과학공원", type = "ATTRACTION", address = "대전 유성구 대덕대로 480",
        latitude = 36.3763, longitude = 127.3907, cost = 5_000L * people, rating = 4.3,
        description = "한빛탑과 다리 야경이 아름다운 과학 도시의 상징",
    )

    fun lerpPath(a: StopDto, b: StopDto, n: Int): List<List<Double>> =
        (0..n).map { i ->
            val t = i.toDouble() / n
            listOf(
                a.latitude + (b.latitude - a.latitude) * t + if (i % 2 == 1) 0.0006 else 0.0,
                a.longitude + (b.longitude - a.longitude) * t + if (i % 3 == 1) 0.0008 else 0.0,
            )
        }

    val leg1 = LegDto(
        mode = "TRANSIT", distanceMeters = 5200.0, durationMinutes = 22, fare = 1500L * people,
        summary = "버스 618번 (시청 승차 → 은행동 하차, 9개 정류장)",
        path = lerpPath(start, stop1, 14),
        boardStop = "시청", alightStop = "은행동",
        departAt = "10:00", arriveAt = "10:22",
        boardLat = 36.3499, boardLng = 127.3856, alightLat = 36.3281, alightLng = 127.4260,
        steps = listOf(
            StepDto("WALK", "도보 120m → 시청 정류장", 120.0, 2),
            StepDto("BUS", "618번 버스 · 시청 승차 → 은행동 하차", 4900.0, 17),
            StepDto("WALK", "도보 180m → 목적지", 180.0, 3),
        ),
    )
    val leg2 = LegDto(
        mode = "TRANSIT", distanceMeters = 5600.0, durationMinutes = 25, fare = 1500L * people,
        summary = "버스 911번 (은행동 승차 → 정부청사 하차, 10개 정류장)",
        path = lerpPath(stop1, stop2, 14),
        boardStop = "은행동", alightStop = "정부청사",
        departAt = "12:10", arriveAt = "12:35",
        steps = listOf(
            StepDto("WALK", "도보 150m → 은행동 정류장", 150.0, 2),
            StepDto("BUS", "911번 버스 · 은행동 승차 → 정부청사 하차", 5200.0, 20),
            StepDto("WALK", "도보 250m → 목적지", 250.0, 3),
        ),
    )
    val leg3 = LegDto(
        mode = "WALK", distanceMeters = 1300.0, durationMinutes = 18, fare = 0,
        summary = "도보 1.3km",
        path = lerpPath(stop2, stop3, 10),
        departAt = "15:00", arriveAt = "15:18",
    )

    val dayStops = listOf(start, stop1, stop2, stop3)
    val dayLegs = listOf(leg1, leg2, leg3)
    val dayCost = dayStops.sumOf { it.cost } + dayLegs.sumOf { it.fare }

    val dayPlans = (1..days.coerceIn(1, 3)).map { d ->
        DayPlanDto(day = d, stops = dayStops, legs = dayLegs, dayCost = dayCost)
    }
    val totalCost = dayPlans.sumOf { it.dayCost }
    val food = dayPlans.sumOf { p -> p.stops.filter { it.type == "RESTAURANT" }.sumOf { it.cost } }
    val attraction = dayPlans.sumOf { p -> p.stops.filter { it.type == "ATTRACTION" }.sumOf { it.cost } }
    val transport = dayPlans.sumOf { p -> p.legs.sumOf { it.fare } }

    return PlanResponse(
        budget = budget,
        days = days,
        people = people,
        totalCost = totalCost,
        remainingBudget = budget - totalCost,
        breakdown = BudgetBreakdown(
            lodgingBudget = budget * 35 / 100, attractionBudget = budget * 25 / 100,
            foodBudget = budget * 25 / 100, transportBudget = budget * 15 / 100,
            lodgingSpent = 0, attractionSpent = attraction,
            foodSpent = food, transportSpent = transport,
        ),
        dayPlans = dayPlans,
        plannedBy = "DEMO",
        tokenBalance = 0,
        aiReason = "예산과 입력한 취향을 반영해 이동 동선을 최소화한 데모 플랜입니다.",
    )
}
