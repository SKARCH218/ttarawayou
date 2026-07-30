package com.trevit.shared

import kotlinx.serialization.Serializable

/**
 * 백엔드 PlanDtos.java 와 1:1 대응하는 직렬화 모델.
 * (backend-kotlin/src/main/kotlin/com/ttarawayou/dto/PlanDtos.kt)
 */

/** POST /api/plan 요청 본문 (v2 — 프로필/취향 필드는 전부 선택) */
@Serializable
data class PlanRequest(
    val budget: Long,
    val days: Int,
    val people: Int,
    val region: String? = null,          // 예: "태안"
    val gender: String? = null,          // MALE | FEMALE
    val ageGroup: String? = null,        // 예: "10대"
    val mbti: String? = null,            // 예: "INFP"
    val purpose: String? = null,         // 휴양 | 관광 | 미식 | 액티비티
    val foodPreference: String? = null,  // 한식 | 양식 | 일식 | 중식
    val avoidWalking: Boolean? = null,
    val keywords: List<String>? = null,  // 예: ["바다","공원"]
    val preferenceNote: String? = null,  // 자유 서술 취향 메모
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
)

/** 예산 배분 내역 */
@Serializable
data class BudgetBreakdown(
    val lodgingBudget: Long = 0,
    val attractionBudget: Long = 0,
    val foodBudget: Long = 0,
    val transportBudget: Long = 0,
    val lodgingSpent: Long = 0,
    val attractionSpent: Long = 0,
    val foodSpent: Long = 0,
    val transportSpent: Long = 0,
)

/** 일정의 한 지점 (도착 전까지 이름을 숨긴다) */
@Serializable
data class StopDto(
    val placeId: Long? = null,
    val name: String? = null,
    val type: String? = null,          // LODGING | RESTAURANT | ATTRACTION | START
    val address: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val cost: Long = 0,                // 인원수 반영 비용(원)
    val rating: Double = 0.0,
    val description: String? = null,
)

/** 이동 구간의 세부 단계 (도보 → 승차 → 하차 → 도보) */
@Serializable
data class StepDto(
    val kind: String? = null,          // WALK | BUS
    val description: String? = null,
    val distanceMeters: Double = 0.0,
    val durationMinutes: Int = 0,
)

/** 두 지점 사이 이동 구간 */
@Serializable
data class LegDto(
    val mode: String = "WALK",         // WALK | TRANSIT
    val distanceMeters: Double = 0.0,
    val durationMinutes: Int = 0,
    val fare: Long = 0,                // 인원수 반영 요금(원)
    val summary: String? = null,       // 예: "버스 700번 (A 승차 → B 하차, 8개 정류장)"
    val path: List<List<Double>> = emptyList(),   // [lat, lng] 목록
    val boardStop: String? = null,     // 승차 정류장명
    val alightStop: String? = null,    // 하차 정류장명
    val departAt: String? = null,      // HH:mm
    val arriveAt: String? = null,      // HH:mm
    val boardLat: Double? = null,
    val boardLng: Double? = null,
    val alightLat: Double? = null,
    val alightLng: Double? = null,
    val stations: List<List<Double>>? = null,     // 경유 정류장 좌표
    val steps: List<StepDto>? = null,
)

@Serializable
data class DayPlanDto(
    val day: Int,
    val stops: List<StopDto> = emptyList(),
    val legs: List<LegDto> = emptyList(),
    val dayCost: Long = 0,
)

/**
 * POST /api/plan 응답.
 * plannedBy: AI(LM Studio) 또는 ALGORITHM(휴리스틱 폴백)
 */
@Serializable
data class PlanResponse(
    val budget: Long = 0,
    val days: Int = 0,
    val people: Int = 0,
    val totalCost: Long = 0,
    val remainingBudget: Long = 0,
    val breakdown: BudgetBreakdown = BudgetBreakdown(),
    val dayPlans: List<DayPlanDto> = emptyList(),
    val plannedBy: String? = null,
    val tokenBalance: Long = 0,
    val aiReason: String? = null,        // AI 추천 이유 (v2)
)
