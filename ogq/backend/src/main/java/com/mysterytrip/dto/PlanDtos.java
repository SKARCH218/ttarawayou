package com.mysterytrip.dto;

import java.util.List;

/** 플랜 API 요청/응답 DTO 모음 */
public class PlanDtos {

    /** POST /api/plan 요청 본문 */
    public record PlanRequest(
            long budget,
            int days,
            int people,
            Double startLatitude,
            Double startLongitude
    ) {}

    /** 예산 배분 내역 */
    public record BudgetBreakdown(
            long lodgingBudget, long attractionBudget, long foodBudget, long transportBudget,
            long lodgingSpent, long attractionSpent, long foodSpent, long transportSpent
    ) {}

    /** 일정의 한 지점 (프론트에서는 도착 전까지 이름을 숨긴다) */
    public record StopDto(
            Long placeId,
            String name,
            String type,          // LODGING | RESTAURANT | ATTRACTION | START
            String address,
            double latitude,
            double longitude,
            long cost,            // 인원수 반영한 비용(원)
            double rating,
            String description
    ) {}

    /** 두 지점 사이 이동 구간 */
    public record LegDto(
            String mode,          // WALK | TRANSIT
            double distanceMeters,
            int durationMinutes,
            long fare,            // 인원수 반영 요금(원)
            String summary,       // 예: "버스 700번 · 8개 정류장"
            List<double[]> path   // [lat, lng] 목록
    ) {}

    public record DayPlanDto(
            int day,
            List<StopDto> stops,
            List<LegDto> legs,
            long dayCost
    ) {}

    /** POST /api/plan 응답 */
    public record PlanResponse(
            long budget,
            int days,
            int people,
            long totalCost,
            long remainingBudget,
            BudgetBreakdown breakdown,
            List<DayPlanDto> dayPlans
    ) {}
}
