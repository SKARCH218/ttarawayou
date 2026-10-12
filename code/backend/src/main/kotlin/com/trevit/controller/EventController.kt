package com.trevit.controller

import com.trevit.entity.UserEvent
import com.trevit.service.AuthService
import com.trevit.service.MetricsService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 앱에서만 알 수 있는 행동(여정 시작·하루 완주)을 받는다 — 퍼널의 뒷단계.
 * 정해진 이벤트 이름만 받고, 회원 번호는 로그인 토큰에서만 꺼낸다(요청 본문의 값은 믿지 않는다).
 */
@RestController
@RequestMapping("/api/events")
class EventController(
    private val authService: AuthService,
    private val metrics: MetricsService,
) {
    data class EventRequest(val type: String = "")

    @PostMapping
    fun record(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @RequestBody request: EventRequest,
    ): ResponseEntity<Map<String, Boolean>> {
        val type = request.type.trim().uppercase()
        if (type !in UserEvent.CLIENT_TYPES) return ResponseEntity.badRequest().body(mapOf("ok" to false))
        metrics.record(type, authService.userIdOrNull(authorization))
        return ResponseEntity.ok(mapOf("ok" to true))
    }
}
