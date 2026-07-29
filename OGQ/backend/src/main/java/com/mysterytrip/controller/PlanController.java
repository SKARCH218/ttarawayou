package com.mysterytrip.controller;

import com.mysterytrip.dto.PlanDtos.PlanRequest;
import com.mysterytrip.dto.PlanDtos.PlanResponse;
import com.mysterytrip.service.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    /** POST /api/plan — 예산 기반 미스터리 플랜 생성 (성공 시 토큰 차감) */
    @PostMapping
    public PlanResponse createPlan(@RequestBody PlanRequest request) {
        return planService.createPlan(request);
    }

    /** 토큰 부족 등 잘못된 요청 → 400 + 메시지 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
