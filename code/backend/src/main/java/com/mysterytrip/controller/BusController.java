package com.mysterytrip.controller;

import com.mysterytrip.service.PublicBusService;
import com.mysterytrip.service.PublicBusService.Arrival;
import com.mysterytrip.service.PublicBusService.Station;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** TAGO 전국 버스 도착정보 프록시 — 프론트 지도에서 실시간 도착 안내용 */
@RestController
@RequestMapping("/api/bus")
public class BusController {

    private final PublicBusService busService;

    public BusController(PublicBusService busService) {
        this.busService = busService;
    }

    /**
     * GET /api/bus/arrivals?lat=..&lng=..&busNo=10
     * 좌표에서 가장 가까운 정류소의 실시간 버스 도착 예정 (busNo 지정 시 해당 노선만)
     */
    @GetMapping("/arrivals")
    public Map<String, Object> arrivals(@RequestParam double lat,
                                        @RequestParam double lng,
                                        @RequestParam(required = false) String busNo) {
        if (!busService.enabled()) {
            return Map.of("enabled", false, "arrivals", List.of());
        }
        Map.Entry<Station, List<Arrival>> result = busService.arrivalsNear(lat, lng, busNo);
        if (result == null) {
            return Map.of("enabled", true, "arrivals", List.of());
        }
        return Map.of(
                "enabled", true,
                "station", Map.of("name", result.getKey().name(), "nodeId", result.getKey().nodeId()),
                "arrivals", result.getValue()
        );
    }
}
