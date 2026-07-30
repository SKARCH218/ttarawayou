package com.ttarawayou.controller

import com.ttarawayou.service.PublicBusService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** TAGO 전국 버스 도착정보 프록시 — 프론트 지도에서 실시간 도착 안내용 */
@RestController
@RequestMapping("/api/bus")
class BusController(private val busService: PublicBusService) {

    /**
     * GET /api/bus/arrivals?lat=..&lng=..&busNo=10
     * 좌표에서 가장 가까운 정류소의 실시간 버스 도착 예정 (busNo 지정 시 해당 노선만)
     */
    @GetMapping("/arrivals")
    fun arrivals(
        @RequestParam lat: Double,
        @RequestParam lng: Double,
        @RequestParam(required = false) busNo: String?,
    ): Map<String, Any> {
        if (!busService.enabled()) {
            return mapOf("enabled" to false, "arrivals" to emptyList<Any>())
        }
        val result = busService.arrivalsNear(lat, lng, busNo)
            ?: return mapOf("enabled" to true, "arrivals" to emptyList<Any>())
        return mapOf(
            "enabled" to true,
            "station" to mapOf("name" to result.first.name, "nodeId" to result.first.nodeId),
            "arrivals" to result.second,
        )
    }
}
