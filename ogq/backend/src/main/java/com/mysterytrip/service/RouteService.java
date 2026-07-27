package com.mysterytrip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysterytrip.dto.PlanDtos.LegDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 구간 이동 경로 생성.
 * - 도보: OSRM 공개 라우팅 서버(키 불필요)로 실제 도로를 따르는 경로
 * - 대중교통: ODsay API (키 없거나 실패 시 거리 기반 추정으로 자동 폴백)
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private static final double WALK_SPEED_M_PER_MIN = 67;   // 약 4km/h
    private static final int BASE_BUS_FARE = 1500;           // 경주 시내버스 성인 요금(카드) 추정

    private final RestClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String odsayKey;
    private final String osrmBaseUrl;
    private final Map<String, LegDto> cache = new ConcurrentHashMap<>();

    public RouteService(@Value("${odsay.api-key}") String odsayKey,
                        @Value("${osrm.base-url}") String osrmBaseUrl) {
        this.odsayKey = odsayKey == null ? "" : odsayKey.trim();
        this.osrmBaseUrl = osrmBaseUrl;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(5000);
        this.http = RestClient.builder().requestFactory(f).build();
    }

    /** 1인 기준 도보 구간 생성 (요금 0) */
    public LegDto walkLeg(double fromLat, double fromLng, double toLat, double toLng) {
        String key = "W:" + round(fromLat) + "," + round(fromLng) + ">" + round(toLat) + "," + round(toLng);
        return cache.computeIfAbsent(key, k -> fetchWalk(fromLat, fromLng, toLat, toLng));
    }

    /** 1인 기준 대중교통 구간 생성. fare는 1인 요금 */
    public LegDto transitLeg(double fromLat, double fromLng, double toLat, double toLng) {
        String key = "T:" + round(fromLat) + "," + round(fromLng) + ">" + round(toLat) + "," + round(toLng);
        return cache.computeIfAbsent(key, k -> fetchTransit(fromLat, fromLng, toLat, toLng));
    }

    private static double round(double v) {
        return Math.round(v * 1e5) / 1e5;
    }

    // ---------- OSRM 도보 ----------

    private LegDto fetchWalk(double fromLat, double fromLng, double toLat, double toLng) {
        try {
            String url = String.format(Locale.US,
                    "%s/route/v1/foot/%f,%f;%f,%f?overview=full&geometries=geojson",
                    osrmBaseUrl, fromLng, fromLat, toLng, toLat);
            String body = http.get().uri(URI.create(url)).retrieve().body(String.class);
            JsonNode root = mapper.readTree(body);
            if (!"Ok".equals(root.path("code").asText())) throw new IllegalStateException("OSRM code != Ok");
            JsonNode route = root.path("routes").get(0);
            double distance = route.path("distance").asDouble();
            List<double[]> path = new ArrayList<>();
            for (JsonNode c : route.path("geometry").path("coordinates")) {
                path.add(new double[]{c.get(1).asDouble(), c.get(0).asDouble()});
            }
            int minutes = (int) Math.max(1, Math.round(distance / WALK_SPEED_M_PER_MIN));
            return new LegDto("WALK", distance, minutes, 0, "도보 " + fmtKm(distance), path);
        } catch (Exception e) {
            log.warn("OSRM 도보 경로 실패, 직선 폴백: {}", e.getMessage());
            double d = GeoUtil.distanceMeters(fromLat, fromLng, toLat, toLng) * 1.3;
            int minutes = (int) Math.max(1, Math.round(d / WALK_SPEED_M_PER_MIN));
            return new LegDto("WALK", d, minutes, 0, "도보 " + fmtKm(d) + " (추정)",
                    straightPath(fromLat, fromLng, toLat, toLng));
        }
    }

    // ---------- ODsay 대중교통 ----------

    private LegDto fetchTransit(double fromLat, double fromLng, double toLat, double toLng) {
        if (!odsayKey.isEmpty()) {
            try {
                LegDto leg = fetchOdsay(fromLat, fromLng, toLat, toLng);
                if (leg != null) return leg;
            } catch (Exception e) {
                log.warn("ODsay 호출 실패, 거리 기반 추정 폴백: {}", e.getMessage());
            }
        }
        return estimateTransit(fromLat, fromLng, toLat, toLng);
    }

    private LegDto fetchOdsay(double fromLat, double fromLng, double toLat, double toLng) throws Exception {
        String encodedKey = URLEncoder.encode(odsayKey, StandardCharsets.UTF_8);
        String url = String.format(Locale.US,
                "https://api.odsay.com/v1/api/searchPubTransPathT?SX=%f&SY=%f&EX=%f&EY=%f&apiKey=%s",
                fromLng, fromLat, toLng, toLat, encodedKey);
        // 문자열로 넘기면 URI 템플릿 처리로 %2B가 %252B로 이중 인코딩되므로 URI 객체로 전달
        String body = http.get().uri(URI.create(url)).retrieve().body(String.class);
        JsonNode root = mapper.readTree(body);
        if (root.has("error")) {
            throw new IllegalStateException("ODsay error: " + root.path("error").toString());
        }
        JsonNode best = root.path("result").path("path").get(0);
        if (best == null) return null;

        JsonNode info = best.path("info");
        long fare = info.path("payment").asLong(BASE_BUS_FARE);
        int minutes = info.path("totalTime").asInt(30);
        double distance = info.path("totalDistance").asDouble(
                GeoUtil.distanceMeters(fromLat, fromLng, toLat, toLng));

        List<double[]> path = new ArrayList<>();
        List<String> parts = new ArrayList<>();
        path.add(new double[]{fromLat, fromLng});
        for (JsonNode sub : best.path("subPath")) {
            int trafficType = sub.path("trafficType").asInt(); // 1 지하철, 2 버스, 3 도보
            if (trafficType == 3) continue;
            JsonNode lane = sub.path("lane").get(0);
            if (lane != null) {
                String busNo = lane.path("busNo").asText(lane.path("name").asText(""));
                int stationCount = sub.path("stationCount").asInt(0);
                parts.add((trafficType == 2 ? "버스 " : "") + busNo + "번 · " + stationCount + "개 정류장");
            }
            for (JsonNode st : sub.path("passStopList").path("stations")) {
                double x = st.path("x").asDouble(); // 경도
                double y = st.path("y").asDouble(); // 위도
                if (x != 0 && y != 0) path.add(new double[]{y, x});
            }
        }
        path.add(new double[]{toLat, toLng});
        String summary = parts.isEmpty() ? "대중교통" : String.join(" → ", parts);
        return new LegDto("TRANSIT", distance, minutes, fare, summary, path);
    }

    /** ODsay 불가 시 거리 기반 대중교통 추정: 도로 경로는 OSRM driving으로 시도, 실패 시 직선 */
    private LegDto estimateTransit(double fromLat, double fromLng, double toLat, double toLng) {
        double straight = GeoUtil.distanceMeters(fromLat, fromLng, toLat, toLng);
        double distance = straight * 1.35;
        long fare = BASE_BUS_FARE + (straight > 15000 ? 700 : 0); // 좌석/시계외 할증 추정
        int rideMinutes = (int) Math.round(distance / 1000 / 26 * 60); // 평균 26km/h
        int minutes = rideMinutes + 9; // 대기 시간 포함
        List<double[]> path;
        try {
            String url = String.format(Locale.US,
                    "%s/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                    osrmBaseUrl, fromLng, fromLat, toLng, toLat);
            String body = http.get().uri(URI.create(url)).retrieve().body(String.class);
            JsonNode root = mapper.readTree(body);
            JsonNode route = root.path("routes").get(0);
            distance = route.path("distance").asDouble(distance);
            path = new ArrayList<>();
            for (JsonNode c : route.path("geometry").path("coordinates")) {
                path.add(new double[]{c.get(1).asDouble(), c.get(0).asDouble()});
            }
        } catch (Exception e) {
            path = straightPath(fromLat, fromLng, toLat, toLng);
        }
        return new LegDto("TRANSIT", distance, minutes, fare,
                "버스 이동 (요금·시간 추정)", path);
    }

    private static List<double[]> straightPath(double fromLat, double fromLng, double toLat, double toLng) {
        List<double[]> path = new ArrayList<>();
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            path.add(new double[]{fromLat + (toLat - fromLat) * t, fromLng + (toLng - fromLng) * t});
        }
        return path;
    }

    private static String fmtKm(double meters) {
        return meters >= 1000
                ? String.format(Locale.US, "%.1fkm", meters / 1000)
                : Math.round(meters) + "m";
    }
}
