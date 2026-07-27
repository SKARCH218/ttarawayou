package com.mysterytrip.controller;

import com.mysterytrip.dto.PlanDtos.PlanRequest;
import com.mysterytrip.dto.PlanDtos.PlanResponse;
import com.mysterytrip.service.PlanService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    /** POST /api/plan — 예산 기반 미스터리 플랜 생성 */
    @PostMapping
    public PlanResponse createPlan(@RequestBody PlanRequest request) {
        return planService.createPlan(request);
    }
}
