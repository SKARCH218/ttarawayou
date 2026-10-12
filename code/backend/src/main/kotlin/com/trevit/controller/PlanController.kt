package com.trevit.controller

import com.trevit.dto.PlanDtos.PlanRequest
import com.trevit.dto.PlanDtos.PlanResponse
import com.trevit.entity.UserEvent
import com.trevit.service.AuthService
import com.trevit.service.MetricsService
import com.trevit.service.PlanService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/plan")
class PlanController(
    private val planService: PlanService,
    private val authService: AuthService,
    private val metrics: MetricsService,
) {

    /** POST /api/plan — 예산 기반 미스터리 플랜 생성 (성공 시 토큰 차감). 로그인은 선택 — 했으면 지표에 회원을 남긴다 */
    @PostMapping
    fun createPlan(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @RequestBody request: PlanRequest,
    ): PlanResponse {
        val plan = planService.createPlan(request)
        metrics.record(UserEvent.PLAN_CREATED, authService.userIdOrNull(authorization))
        return plan
    }

    /** 토큰 부족 등 잘못된 요청 → 400 + 메시지 */
    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "잘못된 요청")))
}
