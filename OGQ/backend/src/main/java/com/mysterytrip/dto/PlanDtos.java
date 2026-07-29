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

    /** 이동 구간의 세부 단계 (도보 → 승차 → 하차 → 도보) */
    public record StepDto(
            String kind,          // WALK | BUS
            String description,   // 예: "도보 215m → 팔우정 정류장", "10번 버스 · A 승차 → B 하차"
            double distanceMeters,
            int durationMinutes
    ) {}

    /** 두 지점 사이 이동 구간 */
    public record LegDto(
            String mode,            // WALK | TRANSIT
            double distanceMeters,
            int durationMinutes,
            long fare,              // 인원수 반영 요금(원)
            String summary,         // 예: "버스 700번 (A 승차 → B 하차, 8개 정류장)"
            List<double[]> path,    // [lat, lng] 목록
            String boardStop,       // 승차 정류장명 (도보/추정이면 null)
            String alightStop,      // 하차 정류장명
            String departAt,        // 출발 시각 HH:mm (일정표 기준)
            String arriveAt,        // 도착 시각 HH:mm
            Double boardLat,        // 승차 정류장 좌표 (지도 마커용)
            Double boardLng,
            Double alightLat,       // 하차 정류장 좌표
            Double alightLng,
            List<double[]> stations, // 승차→하차 경유 정류장 좌표 (하차까지 남은 정거장 계산용)
            List<StepDto> steps      // 세부 단계 (도보→승차→하차→도보), 없으면 null
    ) {
        /** 시각·정류장 정보 미정 구간 생성용 (도보 등) */
        public LegDto(String mode, double distanceMeters, int durationMinutes, long fare,
                      String summary, List<double[]> path) {
            this(mode, distanceMeters, durationMinutes, fare, summary, path,
                    null, null, null, null, null, null, null, null, null, null);
        }

        /** 일정표 시각을 채운 사본 */
        public LegDto withTimes(String depart, String arrive) {
            return new LegDto(mode, distanceMeters, durationMinutes, fare, summary, path,
                    boardStop, alightStop, depart, arrive,
                    boardLat, boardLng, alightLat, alightLng, stations, steps);
        }

        /** 요금만 바꾼 사본 (인원수 반영) */
        public LegDto withFare(long newFare) {
            return new LegDto(mode, distanceMeters, durationMinutes, newFare, summary, path,
                    boardStop, alightStop, departAt, arriveAt,
                    boardLat, boardLng, alightLat, alightLng, stations, steps);
        }
    }

    public record DayPlanDto(
            int day,
            List<StopDto> stops,
            List<LegDto> legs,
            long dayCost
    ) {}

    /**
     * POST /api/plan 응답.
     * plannedBy: AI(LM Studio) 또는 ALGORITHM(휴리스틱 폴백)
     * tokenBalance: 플랜 비용 차감 후 남은 토큰
     */
    public record PlanResponse(
            long budget,
            int days,
            int people,
            long totalCost,
            long remainingBudget,
            BudgetBreakdown breakdown,
            List<DayPlanDto> dayPlans,
            String plannedBy,
            long tokenBalance
    ) {}
}
