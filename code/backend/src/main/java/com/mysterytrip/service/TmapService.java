package com.mysterytrip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysterytrip.dto.PlanDtos.LegDto;
import com.mysterytrip.dto.PlanDtos.StepDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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

/**
 * SK open API (TMAP) 통합 클라이언트.
 *  - 보행자 경로안내: /tmap/routes/pedestrian  (실제 보행로)
 *  - 대중교통 길찾기: /transit/routes          (버스·지하철·기차, 경로좌표·요금 포함)
 *  - 주변 POI 검색:  /tmap/pois/search/around  (전국 관광지·숙소·음식점)
 * 앱키(TMAP_APP_KEY)가 없거나 실패하면 null을 반환해 기존 방식으로 폴백한다.
 */
@Service
public class TmapService {

    private static final Logger log = LoggerFactory.getLogger(TmapService.class);
    private static final String BASE = "https://apis.openapi.sk.com";

    public record WalkRoute(List<double[]> path, double distanceMeters, int minutes) {}

    public record Poi(String name, double lat, double lng, String address) {}

    private final RestClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String appKey;

    public TmapService(@Value("${tmap.app-key}") String appKey) {
        this.appKey = appKey == null ? "" : appKey.trim();
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(10000);
        this.http = RestClient.builder().requestFactory(f).build();
    }

    public boolean enabled() {
        return !appKey.isEmpty();
    }

    // ---------- 보행자 경로 ----------

    public WalkRoute walkRoute(double fromLat, double fromLng, double toLat, double toLng) {
        if (!enabled()) return null;
        try {
            Map<String, Object> body = Map.of(
                    "startX", String.format(Locale.US, "%f", fromLng),
                    "startY", String.format(Locale.US, "%f", fromLat),
                    "endX", String.format(Locale.US, "%f", toLng),
                    "endY", String.format(Locale.US, "%f", toLat),
                    "startName", "S", "endName", "E");
            JsonNode root = post("/tmap/routes/pedestrian?version=1", body);
            List<double[]> path = new ArrayList<>();
            double distance = 0;
            int seconds = 0;
            for (JsonNode feat : root.path("features")) {
                JsonNode props = feat.path("properties");
                if (props.has("totalDistance")) {
                    distance = props.path("totalDistance").asDouble();
                    seconds = props.path("totalTime").asInt();
                }
                JsonNode geom = feat.path("geometry");
                if ("LineString".equals(geom.path("type").asText())) {
                    for (JsonNode c : geom.path("coordinates")) {
                        path.add(new double[]{c.get(1).asDouble(), c.get(0).asDouble()});
                    }
                }
            }
            if (path.size() < 2) return null;
            return new WalkRoute(path, distance, Math.max(1, seconds / 60));
        } catch (Exception e) {
            log.warn("TMAP 보행자 경로 실패: {}", e.getMessage());
            return null;
        }
    }

    // ---------- 대중교통 길찾기 ----------

    /** 1인 기준 대중교통 구간. 실패 시 null (→ ODsay 폴백) */
    public LegDto transitLeg(double fromLat, double fromLng, double toLat, double toLng) {
        if (!enabled()) return null;
        try {
            Map<String, Object> body = Map.of(
                    "startX", String.format(Locale.US, "%f", fromLng),
                    "startY", String.format(Locale.US, "%f", fromLat),
                    "endX", String.format(Locale.US, "%f", toLng),
                    "endY", String.format(Locale.US, "%f", toLat),
                    "count", 1, "lang", 0, "format", "json");
            JsonNode root = post("/transit/routes", body);
            JsonNode itin = root.path("metaData").path("plan").path("itineraries").get(0);
            if (itin == null) return null;

            long fare = itin.path("fare").path("regular").path("totalFare").asLong(0);
            int minutes = Math.max(1, itin.path("totalTime").asInt(0) / 60);

            List<double[]> path = new ArrayList<>();
            List<double[]> stations = new ArrayList<>();
            List<StepDto> steps = new ArrayList<>();
            List<String> parts = new ArrayList<>();
            double distance = 0;
            String boardStop = null, alightStop = null;
            Double boardLat = null, boardLng = null, alightLat = null, alightLng = null;

            for (JsonNode leg : itin.path("legs")) {
                String mode = leg.path("mode").asText();
                double legDist = leg.path("distance").asDouble(0);
                int legMin = Math.max(1, leg.path("sectionTime").asInt(0) / 60);
                distance += legDist;
                if ("WALK".equals(mode)) {
                    for (JsonNode st : leg.path("steps")) {
                        appendLinestring(path, st.path("linestring").asText(""));
                    }
                    if (legDist >= 20) {
                        steps.add(new StepDto("WALK",
                                "도보 " + Math.round(legDist) + "m → " + leg.path("end").path("name").asText("목적지"),
                                legDist, legMin));
                    }
                } else {
                    String route = leg.path("route").asText(modeLabel(mode));
                    String sName = leg.path("start").path("name").asText("");
                    String eName = leg.path("end").path("name").asText("");
                    if (boardStop == null) {
                        boardStop = sName;
                        boardLat = leg.path("start").path("lat").asDouble();
                        boardLng = leg.path("start").path("lon").asDouble();
                    }
                    alightStop = eName;
                    alightLat = leg.path("end").path("lat").asDouble();
                    alightLng = leg.path("end").path("lon").asDouble();

                    int stCount = 0;
                    for (JsonNode st : leg.path("passStopList").path("stationList")) {
                        double la = st.path("lat").asDouble();
                        double lo = st.path("lon").asDouble();
                        if (la != 0 && lo != 0) {
                            stations.add(new double[]{la, lo});
                            stCount++;
                        }
                    }
                    appendLinestring(path, leg.path("passShape").path("linestring").asText(""));
                    String label = cleanRoute(route, mode);
                    parts.add(label + " (" + sName + " 승차 → " + eName + " 하차"
                            + (stCount > 1 ? ", " + (stCount - 1) + "개 정류장" : "") + ")");
                    steps.add(new StepDto("BUS",
                            label + " · " + sName + " 승차 → " + eName + " 하차"
                                    + (stCount > 1 ? " (" + (stCount - 1) + "개 정류장)" : ""),
                            legDist, legMin));
                }
            }
            if (path.size() < 2) return null;
            String summary = parts.isEmpty() ? "대중교통" : String.join(" → 환승 → ", parts);
            log.info("TMAP 대중교통 경로: {}분, {}원, {}", minutes, fare, summary);
            return new LegDto("TRANSIT", distance, minutes, fare, summary, path,
                    boardStop, alightStop, null, null,
                    boardLat, boardLng, alightLat, alightLng, stations, steps);
        } catch (Exception e) {
            log.warn("TMAP 대중교통 실패: {}", e.getMessage());
            return null;
        }
    }

    // ---------- 주변 POI (전국 장소 공급) ----------

    public List<Poi> poisAround(double lat, double lng, String category, int count) {
        if (!enabled()) return List.of();
        List<Poi> out = new ArrayList<>();
        try {
            String url = String.format(Locale.US,
                    "%s/tmap/pois/search/around?version=1&centerLon=%f&centerLat=%f&categories=%s&page=1&count=%d&radius=15",
                    BASE, lng, lat, URLEncoder.encode(category, StandardCharsets.UTF_8),
                    Math.min(200, count));
            String res = http.get().uri(URI.create(url))
                    .header("appKey", appKey).retrieve().body(String.class);
            JsonNode pois = mapper.readTree(res).path("searchPoiInfo").path("pois").path("poi");
            for (JsonNode p : pois) {
                double la = p.path("frontLat").asDouble(p.path("noorLat").asDouble(0));
                double lo = p.path("frontLon").asDouble(p.path("noorLon").asDouble(0));
                if (la == 0 || lo == 0) continue;
                String addr = (p.path("upperAddrName").asText("") + " "
                        + p.path("middleAddrName").asText("") + " "
                        + p.path("lowerAddrName").asText("")).trim();
                out.add(new Poi(p.path("name").asText(""), la, lo, addr));
            }
        } catch (Exception e) {
            log.warn("TMAP POI 검색 실패({}): {}", category, e.getMessage());
        }
        return out;
    }

    // ---------- 공통 ----------

    private JsonNode post(String path, Map<String, Object> body) throws Exception {
        String res = http.post().uri(URI.create(BASE + path))
                .header("appKey", appKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapper.writeValueAsString(body))
                .retrieve().body(String.class);
        return mapper.readTree(res);
    }

    /** "lng,lat lng,lat …" 문자열을 [lat,lng] 목록으로 이어붙인다 */
    private static void appendLinestring(List<double[]> path, String linestring) {
        if (linestring == null || linestring.isBlank()) return;
        for (String pair : linestring.trim().split("\\s+")) {
            String[] xy = pair.split(",");
            if (xy.length == 2) {
                try {
                    path.add(new double[]{Double.parseDouble(xy[1]), Double.parseDouble(xy[0])});
                } catch (NumberFormatException ignored) { }
            }
        }
    }

    private static String modeLabel(String mode) {
        return switch (mode) {
            case "SUBWAY" -> "지하철";
            case "TRAIN" -> "기차";
            case "EXPRESSBUS" -> "고속버스";
            case "INTERCITYBUS" -> "시외버스";
            case "AIRPLANE" -> "항공";
            case "FERRY" -> "여객선";
            default -> "버스";
        };
    }

    /** TMAP route 표기("좌석:11" 등)를 읽기 좋게 다듬는다 */
    private static String cleanRoute(String route, String mode) {
        if (route == null || route.isBlank()) return modeLabel(mode);
        String r = route.replace(":", " ");
        return "BUS".equals(mode) && !r.contains("버스") ? "버스 " + r : r;
    }
}
