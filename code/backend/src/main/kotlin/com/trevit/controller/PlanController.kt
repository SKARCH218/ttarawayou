package com.trevit.controller

import com.trevit.dto.PlanDtos.PlanRequest
import com.trevit.dto.PlanDtos.PlanResponse
import com.trevit.service.PlanService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/plan")
class PlanController(private val planService: PlanService) {

    /** POST /api/plan — 예산 기반 미스터리 플랜 생성 (성공 시 토큰 차감) */
    @PostMapping
    fun createPlan(@RequestBody request: PlanRequest): PlanResponse = planService.createPlan(request)

    /** 토큰 부족 등 잘못된 요청 → 400 + 메시지 */
    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "잘못된 요청")))
}
