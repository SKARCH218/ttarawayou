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
    private final PublicBusService publicBusService;
    private final IntercityBusService intercityBusService;
    private final TmapService tmapService;
    private final Map<String, LegDto> cache = new ConcurrentHashMap<>();

    /** 이 직선거리(m)를 넘는 이동은 시외로 보고 시외버스 정보를 결합한다 */
    private static final double INTERCITY_THRESHOLD_M = 30_000;

    public RouteService(@Value("${odsay.api-key}") String odsayKey,
                        @Value("${osrm.base-url}") String osrmBaseUrl,
                        PublicBusService publicBusService,
                        IntercityBusService intercityBusService,
                        TmapService tmapService) {
        this.odsayKey = odsayKey == null ? "" : odsayKey.trim();
        this.osrmBaseUrl = osrmBaseUrl;
        this.publicBusService = publicBusService;
        this.intercityBusService = intercityBusService;
        this.tmapService = tmapService;
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
        // 1순위: TMAP 보행자 경로 (한국 보행로 데이터)
        TmapService.WalkRoute tw = tmapService.walkRoute(fromLat, fromLng, toLat, toLng);
        if (tw != null) {
            return new LegDto("WALK", tw.distanceMeters(), tw.minutes(), 0,
                    "도보 " + fmtKm(tw.distanceMeters()), tw.path());
        }
        // 2순위: OSRM
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
        // 1순위: TMAP 대중교통 (경로좌표·요금·환승 포함)
        LegDto leg = tmapService.transitLeg(fromLat, fromLng, toLat, toLng);
        // 2순위: ODsay
        if (leg == null && !odsayKey.isEmpty()) {
            try {
                leg = fetchOdsay(fromLat, fromLng, toLat, toLng);
            } catch (Exception e) {
                log.warn("ODsay 호출 실패, 거리 기반 추정 폴백: {}", e.getMessage());
            }
        }
        if (leg == null) leg = estimateTransit(fromLat, fromLng, toLat, toLng);
        // 시외 이동이면 시외버스 터미널·시간표·요금 정보를 결합
        double straight = GeoUtil.distanceMeters(fromLat, fromLng, toLat, toLng);
        if (straight > INTERCITY_THRESHOLD_M) {
            leg = enrichIntercity(leg, fromLat, fromLng, toLat, toLng);
            leg = ensureIntercityFare(leg, straight);
        }
        return leg;
    }

    /**
     * 시외 구간인데 요금이 시내버스 수준(명백히 비현실적)이면 거리 기반으로 추정한다.
     * ODsay는 시외/열차 요금을 안 주는 경우가 많고, 시외버스 API에 배차 데이터가
     * 없는 노선도 있어서(예: 경주행 다수) 마지막 안전망이 필요하다.
     * 기준: 시외버스 대략 km당 85원, 최소 5,000원.
     */
    private LegDto ensureIntercityFare(LegDto leg, double straightM) {
        double km = straightM / 1000.0;
        long minReasonable = Math.round(km * 30); // 이보다 낮으면 시내요금 폴백으로 판단
        if (leg.fare() >= minReasonable) return leg;
        long estimated = Math.max(5000, Math.round(km * 85 / 100) * 100);
        log.info("시외 구간 요금 보정: {}km, {}원 → {}원(추정)", Math.round(km), leg.fare(), estimated);
        return new LegDto(leg.mode(), leg.distanceMeters(), leg.durationMinutes(), estimated,
                leg.summary() + " · 요금 " + estimated + "원(거리 기반 추정)", leg.path(),
                leg.boardStop(), leg.alightStop(), leg.departAt(), leg.arriveAt(),
                leg.boardLat(), leg.boardLng(), leg.alightLat(), leg.alightLng(),
                leg.stations(), leg.steps());
    }

    /** 시외 구간: TAGO 시외버스정보로 다음 배차·터미널·실제 요금을 붙인다 */
    private LegDto enrichIntercity(LegDto leg, double fromLat, double fromLng,
                                   double toLat, double toLng) {
        if (!intercityBusService.enabled()) return leg;
        try {
            List<PublicBusService.Station> depSt = publicBusService.nearestStations(fromLat, fromLng);
            List<PublicBusService.Station> arrSt = publicBusService.nearestStations(toLat, toLng);
            if (depSt.isEmpty() || arrSt.isEmpty()) return leg;
            String depCity = depSt.get(0).cityCode();
            String arrCity = arrSt.get(0).cityCode();
            IntercityBusService.Departure d = intercityBusService.nextDeparture(
                    depCity, publicBusService.cityName(depCity),
                    arrCity, publicBusService.cityName(arrCity));
            if (d == null) return leg;

            List<com.mysterytrip.dto.PlanDtos.StepDto> steps =
                    new ArrayList<>(leg.steps() == null ? List.of() : leg.steps());
            steps.add(0, new com.mysterytrip.dto.PlanDtos.StepDto("BUS",
                    "시외버스 " + d.depTerminalNm() + " → " + d.arrTerminalNm()
                            + " · 다음 출발 " + d.depTime()
                            + (d.arrTime().isEmpty() ? "" : " (도착 " + d.arrTime() + ")")
                            + " · " + d.grade() + " " + d.charge() + "원",
                    leg.distanceMeters(), leg.durationMinutes()));
            // 추정 요금(시내버스 수준)이면 실제 시외버스 요금으로 교체
            long fare = leg.fare() < d.charge() / 2 ? d.charge() : leg.fare();
            String summary = leg.summary() + " · 시외버스 " + d.depTime() + " 출발";
            return new LegDto(leg.mode(), leg.distanceMeters(), leg.durationMinutes(), fare,
                    summary, leg.path(), leg.boardStop(), leg.alightStop(),
                    leg.departAt(), leg.arriveAt(), leg.boardLat(), leg.boardLng(),
                    leg.alightLat(), leg.alightLng(), leg.stations(), steps);
        } catch (Exception e) {
            log.warn("시외버스 정보 결합 실패: {}", e.getMessage());
            return leg;
        }
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
        List<double[]> stations = new ArrayList<>(); // 정류장 좌표만 (하차 카운트다운용)
        List<String> parts = new ArrayList<>();
        List<com.mysterytrip.dto.PlanDtos.StepDto> steps = new ArrayList<>();
        String boardStop = null;
        String alightStop = null;
        Double boardLat = null, boardLng = null, alightLat = null, alightLng = null;
        double pendingWalkM = -1;   // 버스 구간 사이의 도보(정류장까지 걷기)
        int pendingWalkMin = 0;
        path.add(new double[]{fromLat, fromLng});
        for (JsonNode sub : best.path("subPath")) {
            int trafficType = sub.path("trafficType").asInt(); // 1 지하철, 2 버스, 3 도보
            if (trafficType == 3) {
                // 도보 구간은 다음 버스 정류장 이름과 붙여서 스텝으로 만든다
                double dm = sub.path("distance").asDouble(0);
                if (dm > 0) {
                    pendingWalkM = pendingWalkM < 0 ? dm : pendingWalkM + dm;
                    pendingWalkMin += sub.path("sectionTime").asInt(0);
                }
                continue;
            }
            String startName = sub.path("startName").asText("");
            String endName = sub.path("endName").asText("");
            if (boardStop == null && !startName.isEmpty()) {
                boardStop = startName;
                boardLat = sub.path("startY").asDouble();
                boardLng = sub.path("startX").asDouble();
            }
            if (!endName.isEmpty()) {
                alightStop = endName;
                alightLat = sub.path("endY").asDouble();
                alightLng = sub.path("endX").asDouble();
            }
            JsonNode lane = sub.path("lane").get(0);
            String busNo = "";
            int stationCount = sub.path("stationCount").asInt(0);
            if (lane != null) {
                busNo = lane.path("busNo").asText(lane.path("name").asText("")).trim();
            }
            // 시외 이동에서는 기차 등 번호 없는 구간이 올 수 있다
            String rideName = busNo.isEmpty()
                    ? (trafficType == 1 ? "지하철" : "시외 이동(열차/버스)")
                    : busNo + "번 버스";
            parts.add(rideName + " (" + startName + " 승차 → " + endName + " 하차"
                    + (stationCount > 0 ? ", " + stationCount + "개 정류장" : "") + ")");
            // 세부 단계: (도보 → 정류장) + (승차 → 하차)
            if (pendingWalkM >= 0) {
                steps.add(new com.mysterytrip.dto.PlanDtos.StepDto("WALK",
                        "도보 " + Math.round(pendingWalkM) + "m → " + startName + " 정류장",
                        pendingWalkM, Math.max(1, pendingWalkMin)));
                pendingWalkM = -1;
                pendingWalkMin = 0;
            }
            steps.add(new com.mysterytrip.dto.PlanDtos.StepDto("BUS",
                    rideName + " · " + startName + " 승차 → " + endName + " 하차"
                            + (stationCount > 0 ? " (" + stationCount + "개 정류장)" : ""),
                    sub.path("distance").asDouble(0), sub.path("sectionTime").asInt(0)));
            // 1순위: 공공데이터포털(TAGO) 버스노선의 실제 경유 정류소 구간
            List<double[]> segment = null;
            if (trafficType == 2 && !busNo.isEmpty()) {
                segment = publicBusService.stationsBetween(busNo,
                        sub.path("startY").asDouble(), sub.path("startX").asDouble(),
                        sub.path("endY").asDouble(), sub.path("endX").asDouble());
            }
            // 2순위: ODsay가 준 경유 정류장 목록
            if (segment == null) {
                segment = new ArrayList<>();
                for (JsonNode st : sub.path("passStopList").path("stations")) {
                    double x = st.path("x").asDouble(); // 경도
                    double y = st.path("y").asDouble(); // 위도
                    if (x != 0 && y != 0) segment.add(new double[]{y, x});
                }
            }
            path.addAll(segment);
            stations.addAll(segment);
        }
        path.add(new double[]{toLat, toLng});
        // 마지막 도보 (하차 후 목적지까지)
        if (pendingWalkM >= 0) {
            steps.add(new com.mysterytrip.dto.PlanDtos.StepDto("WALK",
                    "하차 후 도보 " + Math.round(pendingWalkM) + "m → 목적지",
                    pendingWalkM, Math.max(1, pendingWalkMin)));
        }
        // 정류장 좌표만 직선으로 이으면 건물을 관통해 보이므로,
        // 정류장들을 경유지로 실도로(OSRM driving) 경로를 만들어 지도용 경로로 쓴다
        List<double[]> roadPath = osrmRouteThrough(path);
        String summary = parts.isEmpty() ? "대중교통" : String.join(" → 환승 → ", parts);
        return new LegDto("TRANSIT", distance, minutes, fare, summary,
                roadPath != null ? roadPath : path, boardStop, alightStop, null, null,
                boardLat, boardLng, alightLat, alightLng, stations, steps);
    }

    /** 여러 지점을 순서대로 경유하는 실도로 경로 (OSRM driving). 실패 시 null */
    private List<double[]> osrmRouteThrough(List<double[]> points) {
        try {
            List<double[]> waypoints = sampleWaypoints(points, 24);
            StringBuilder coords = new StringBuilder();
            for (double[] p : waypoints) {
                if (coords.length() > 0) coords.append(';');
                coords.append(String.format(Locale.US, "%f,%f", p[1], p[0])); // lng,lat
            }
            String url = String.format("%s/route/v1/driving/%s?overview=full&geometries=geojson",
                    osrmBaseUrl, coords);
            String body = http.get().uri(URI.create(url)).retrieve().body(String.class);
            JsonNode root = mapper.readTree(body);
            if (!"Ok".equals(root.path("code").asText())) return null;
            List<double[]> out = new ArrayList<>();
            for (JsonNode c : root.path("routes").get(0).path("geometry").path("coordinates")) {
                out.add(new double[]{c.get(1).asDouble(), c.get(0).asDouble()});
            }
            return out.size() >= 2 ? out : null;
        } catch (Exception e) {
            log.warn("경유지 실도로 경로 실패, 정류장 직선 연결 사용: {}", e.getMessage());
            return null;
        }
    }

    /** 경유지가 너무 많으면 시작·끝을 보존하며 균등 샘플링 */
    private static List<double[]> sampleWaypoints(List<double[]> points, int maxCount) {
        if (points.size() <= maxCount) return points;
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < maxCount; i++) {
            out.add(points.get((int) Math.round((double) i * (points.size() - 1) / (maxCount - 1))));
        }
        return out;
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
