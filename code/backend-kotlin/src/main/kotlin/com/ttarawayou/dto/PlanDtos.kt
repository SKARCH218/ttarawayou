package com.ttarawayou.dto

/** 플랜 API 요청/응답 DTO 모음 */
object PlanDtos {

    /** POST /api/plan 요청 본문 (v2: 프로필/취향 필드는 전부 선택 — 구버전 요청도 그대로 동작) */
    data class PlanRequest(
        val budget: Long = 0,
        val days: Int = 1,
        val people: Int = 1,
        val startLatitude: Double? = null,
        val startLongitude: Double? = null,
        val region: String? = null,           // 지역명 (예: "태안") — 있으면 이 지역 중심으로 플랜
        val gender: String? = null,           // MALE | FEMALE | NONE
        val ageGroup: String? = null,         // 10대 | 20대 | 30대 | 40대 | 50대+
        val mbti: String? = null,             // 16 types 또는 null
        val purpose: String? = null,          // 휴양 | 관광 | 미식 | 액티비티
        val foodPreference: String? = null,   // 한식 | 양식 | 일식 | 중식 | 상관없음
        val avoidWalking: Boolean = false,    // 걷기 기피 → 도보 최소화 + 산/등산 장소 회피
        val keywords: List<String>? = null,   // 선호 키워드: 산 | 바다 | 공원 | 강
        val preferenceNote: String? = null,   // 자유 서술 취향 ("매운 음식 좋아요, 조용한 카페 위주로")
    )

    /** 플랜 생성에 쓰는 사용자 프로필/취향 (요청에서 추출, 내부 전달용) */
    data class TravelProfile(
        val regionName: String? = null,
        val gender: String? = null,
        val ageGroup: String? = null,
        val mbti: String? = null,
        val purpose: String? = null,
        val foodPreference: String? = null,
        val avoidWalking: Boolean = false,
        val keywords: List<String> = emptyList(),
        val preferenceNote: String? = null,
    ) {
        fun isEmpty(): Boolean =
            gender == null && ageGroup == null && mbti == null && purpose == null &&
                foodPreference == null && !avoidWalking && keywords.isEmpty() &&
                preferenceNote.isNullOrBlank()
    }

    /** 예산 배분 내역 */
    data class BudgetBreakdown(
        val lodgingBudget: Long,
        val attractionBudget: Long,
        val foodBudget: Long,
        val transportBudget: Long,
        val lodgingSpent: Long,
        val attractionSpent: Long,
        val foodSpent: Long,
        val transportSpent: Long,
    )

    /** 일정의 한 지점 (프론트에서는 도착 전까지 이름을 숨긴다) */
    data class StopDto(
        val placeId: Long?,
        val name: String,
        val type: String,          // LODGING | RESTAURANT | ATTRACTION | START
        val address: String,
        val latitude: Double,
        val longitude: Double,
        val cost: Long,            // 인원수 반영한 비용(원)
        val rating: Double,
        val description: String?,
    )

    /** 이동 구간의 세부 단계 (도보 → 승차 → 하차 → 도보) */
    data class StepDto(
        val kind: String,          // WALK | BUS
        val description: String,   // 예: "도보 215m → 팔우정 정류장"
        val distanceMeters: Double,
        val durationMinutes: Int,
    )

    /** 두 지점 사이 이동 구간 */
    data class LegDto(
        val mode: String,               // WALK | TRANSIT
        val distanceMeters: Double,
        val durationMinutes: Int,
        val fare: Long,                 // 인원수 반영 요금(원)
        val summary: String,            // 예: "버스 700번 (A 승차 → B 하차, 8개 정류장)"
        val path: List<DoubleArray>,    // [lat, lng] 목록
        val boardStop: String? = null,  // 승차 정류장명 (도보/추정이면 null)
        val alightStop: String? = null, // 하차 정류장명
        val departAt: String? = null,   // 출발 시각 HH:mm (일정표 기준)
        val arriveAt: String? = null,   // 도착 시각 HH:mm
        val boardLat: Double? = null,   // 승차 정류장 좌표 (지도 마커용)
        val boardLng: Double? = null,
        val alightLat: Double? = null,  // 하차 정류장 좌표
        val alightLng: Double? = null,
        val stations: List<DoubleArray>? = null, // 승차→하차 경유 정류장 좌표
        val steps: List<StepDto>? = null,        // 세부 단계, 없으면 null
    ) {
        /** 일정표 시각을 채운 사본 */
        fun withTimes(depart: String, arrive: String): LegDto =
            copy(departAt = depart, arriveAt = arrive)

        /** 요금만 바꾼 사본 (인원수 반영) */
        fun withFare(newFare: Long): LegDto = copy(fare = newFare)
    }

    data class DayPlanDto(
        val day: Int,
        val stops: List<StopDto>,
        val legs: List<LegDto>,
        val dayCost: Long,
    )

    /**
     * POST /api/plan 응답.
     * plannedBy: AI(LM Studio) 또는 ALGORITHM(휴리스틱 폴백)
     * tokenBalance: 플랜 비용 차감 후 남은 토큰
     * aiReason: 이 프로필에 왜 이런 계획을 세웠는지 (AI 생성, 폴백 시 규칙 기반 요약)
     */
    data class PlanResponse(
        val budget: Long,
        val days: Int,
        val people: Int,
        val totalCost: Long,
        val remainingBudget: Long,
        val breakdown: BudgetBreakdown,
        val dayPlans: List<DayPlanDto>,
        val plannedBy: String,
        val tokenBalance: Long,
        val aiReason: String? = null,
    )
}
